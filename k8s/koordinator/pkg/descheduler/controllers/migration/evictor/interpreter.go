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

package evictor

import (
	"context"
	"errors"
	"fmt"
	"strconv"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/tools/events"
	"k8s.io/client-go/util/flowcontrol"
	"k8s.io/klog/v2"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/metrics"
)

const (
	LabelEvictPolicy = "koordinator.sh/evict-policy"

	AnnotationEvictReason  = "koordinator.sh/evict-reason"
	AnnotationEvictTrigger = "koordinator.sh/evict-trigger"
)

var (
	ErrTooManyEvictions = errors.New("TooManyEvictions")
)

type FactoryFn func(client kubernetes.Interface) (Interface, error)

type Interface interface {
	Evict(ctx context.Context, job *sev1alpha1.PodMigrationJob, pod *corev1.Pod) error
}

var registry = map[string]FactoryFn{}

func RegisterEvictor(name string, factoryFn FactoryFn) {
	registry[name] = factoryFn
}

type Interpreter interface {
	Interface
}

type interpreterImpl struct {
	evictors       map[string]Interface
	defaultEvictor Interface
	rateLimiter    flowcontrol.RateLimiter
	eventRecorder  events.EventRecorder
}

func NewInterpreter(handle framework.Handle, defaultEvictionPolicy string, evictQPS string, evictBurst int) (Interpreter, error) {
	var qps float32
	if val, err := strconv.ParseFloat(evictQPS, 64); err == nil && val > 0 {
		qps = float32(val)
	}
	rateLimiter := flowcontrol.NewTokenBucketRateLimiter(qps, evictBurst)

	evictors := map[string]Interface{}
	for k, v := range registry {
		evictor, err := v(handle.ClientSet())
		if err != nil {
			return nil, err
		}
		evictors[k] = evictor
	}
	defaultEvictor := evictors[defaultEvictionPolicy]
	if defaultEvictor == nil {
		return nil, fmt.Errorf("unsupported evicition policy")
	}
	return &interpreterImpl{
		evictors:       evictors,
		defaultEvictor: defaultEvictor,
		rateLimiter:    rateLimiter,
		eventRecorder:  handle.EventRecorder(),
	}, nil
}

func (p *interpreterImpl) Evict(ctx context.Context, job *sev1alpha1.PodMigrationJob, pod *corev1.Pod) error {
	if p.rateLimiter != nil {
		if !p.rateLimiter.TryAccept() {
			return ErrTooManyEvictions
		}
	}
	evictionPolicy := getCustomEvictionPolicy(pod.Labels)
	if evictionPolicy == "" {
		evictionPolicy = getCustomEvictionPolicy(job.Labels)
	}

	var evictor Interface
	if evictionPolicy != "" {
		evictor = p.evictors[evictionPolicy]
	}
	if evictor == nil {
		evictor = p.defaultEvictor
	}

	trigger, reason := GetEvictionTriggerAndReason(job.Annotations)
	err := evictor.Evict(ctx, job, pod)
	if err != nil {
		metrics.PodsEvicted.With(map[string]string{"result": "error", "strategy": trigger, "namespace": pod.Namespace, "node": pod.Spec.NodeName}).Inc()
		return err
	}

	metrics.PodsEvicted.With(map[string]string{"result": "success", "strategy": trigger, "namespace": pod.Namespace, "node": pod.Spec.NodeName}).Inc()

	klog.V(1).InfoS("Evicted pod", "pod", klog.KObj(pod), "reason", reason, "trigger", trigger, "node", pod.Spec.NodeName)
	p.eventRecorder.Eventf(pod, nil, corev1.EventTypeNormal, "Descheduled", "Migrating", "Pod evicted from node %q by the reason %q", pod.Spec.NodeName, reason)
	return nil
}

func getCustomEvictionPolicy(labels map[string]string) string {
	value, ok := labels[LabelEvictPolicy]
	if ok && value != "" {
		return value
	}
	return ""
}

func GetEvictionTriggerAndReason(annotations map[string]string) (string, string) {
	reason := annotations[AnnotationEvictReason]
	trigger := annotations[AnnotationEvictTrigger]
	if len(reason) == 0 {
		reason = trigger
		if len(reason) == 0 {
			reason = "NotSet"
		}
	}
	return trigger, reason
}
