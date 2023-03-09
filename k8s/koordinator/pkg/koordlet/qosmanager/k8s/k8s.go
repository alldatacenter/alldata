/*
Copyright 2022 The Koordinator Authors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package k8s

import (
	"context"
	"fmt"
	"sync"

	corev1 "k8s.io/api/core/v1"
	policyv1 "k8s.io/api/policy/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/record"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/audit"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/common/reason"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metrics"
	expireCache "github.com/koordinator-sh/koordinator/pkg/util/cache"
)

type K8sClient interface {
	Run(<-chan struct{}) error
	EvictPods(evictPods []*corev1.Pod, node *corev1.Node, reason string, message string)
}

var _ K8sClient = &k8sClient{}

func NewK8sClient(kubeClinet kubernetes.Interface, eventRecorder record.EventRecorder) K8sClient {
	return &k8sClient{
		kubeClient:    kubeClinet,
		eventRecorder: eventRecorder,
		podsEvicted:   expireCache.NewCacheDefault(),
	}
}

type k8sClient struct {
	kubeClient    kubernetes.Interface
	eventRecorder record.EventRecorder
	podsEvicted   *expireCache.Cache

	started bool
	lock    sync.RWMutex
}

// Run impl interface K8sClient.
func (r *k8sClient) Run(stop <-chan struct{}) error {
	r.lock.Lock()
	defer r.lock.Unlock()

	if r.started {
		return nil
	}

	return r.podsEvicted.Run(stop)
}

// EvictPods impl interface K8sClient.
func (r *k8sClient) EvictPods(evictPods []*corev1.Pod, node *corev1.Node, reason string, message string) {
	for _, evictPod := range evictPods {
		r.evictPodIfNotEvicted(evictPod, node, reason, message)
	}
}

func (r *k8sClient) evictPodIfNotEvicted(evictPod *corev1.Pod, node *corev1.Node, reason string, message string) {
	_, evicted := r.podsEvicted.Get(string(evictPod.UID))
	if evicted {
		klog.V(5).Infof("Pod has been evicted! podID: %v, evict reason: %s", evictPod.UID, reason)
		return
	}
	success := r.evictPod(evictPod, node, reason, message)
	if success {
		_ = r.podsEvicted.SetDefault(string(evictPod.UID), evictPod.UID)
	}
}

func (r *k8sClient) evictPod(evictPod *corev1.Pod, node *corev1.Node, reasonMsg string, message string) bool {
	podEvictMessage := fmt.Sprintf("evict Pod:%s, reason: %s, message: %v", evictPod.Name, reasonMsg, message)
	_ = audit.V(0).Pod(evictPod.Namespace, evictPod.Name).Reason(reasonMsg).Message(message).Do()
	podEvict := policyv1.Eviction{
		ObjectMeta: metav1.ObjectMeta{
			Name:      evictPod.Name,
			Namespace: evictPod.Namespace,
		},
	}

	if err := r.kubeClient.CoreV1().Pods(evictPod.Namespace).EvictV1(context.TODO(), &podEvict); err == nil {
		r.eventRecorder.Eventf(node, corev1.EventTypeWarning, reason.EvictPodSuccess, podEvictMessage)
		metrics.RecordPodEviction(reasonMsg)
		klog.Infof("evict pod %v/%v success, reason: %v", evictPod.Namespace, evictPod.Name, reasonMsg)
		return true
	} else if !errors.IsNotFound(err) {
		r.eventRecorder.Eventf(node, corev1.EventTypeWarning, reason.EvictPodFail, podEvictMessage)
		klog.Errorf("evict pod %v/%v failed, reason: %v, error: %v", evictPod.Namespace, evictPod.Name, reasonMsg, err)
		return false
	}
	return true
}
