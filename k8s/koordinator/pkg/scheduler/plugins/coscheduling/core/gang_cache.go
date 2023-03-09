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

package core

import (
	"context"
	"sync"
	"time"

	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/validation"
	listerv1 "k8s.io/client-go/listers/core/v1"
	"k8s.io/client-go/util/retry"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
	pgclientset "sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned"

	pglister "sigs.k8s.io/scheduler-plugins/pkg/generated/listers/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/util"
)

type GangCache struct {
	lock       *sync.RWMutex
	gangItems  map[string]*Gang
	pluginArgs *config.CoschedulingArgs
	podLister  listerv1.PodLister
	pgLister   pglister.PodGroupLister
	pgClient   pgclientset.Interface
}

func NewGangCache(args *config.CoschedulingArgs, podLister listerv1.PodLister, pgLister pglister.PodGroupLister, client pgclientset.Interface) *GangCache {
	return &GangCache{
		gangItems:  make(map[string]*Gang),
		lock:       new(sync.RWMutex),
		pluginArgs: args,
		podLister:  podLister,
		pgLister:   pgLister,
		pgClient:   client,
	}
}

func (gangCache *GangCache) getGangFromCacheByGangId(gangId string, createIfNotExist bool) *Gang {
	gangCache.lock.Lock()
	defer gangCache.lock.Unlock()
	gang := gangCache.gangItems[gangId]
	if gang == nil && createIfNotExist {
		gang = NewGang(gangId)
		gangCache.gangItems[gangId] = gang
		klog.Infof("getGangFromCache create new gang, gang: %v", gangId)
	}
	return gang
}

func (gangCache *GangCache) getAllGangsFromCache() map[string]*Gang {
	gangCache.lock.RLock()
	defer gangCache.lock.RUnlock()

	result := make(map[string]*Gang)
	for gangId, gang := range gangCache.gangItems {
		result[gangId] = gang
	}

	return result
}

func (gangCache *GangCache) deleteGangFromCacheByGangId(gangId string) {
	gangCache.lock.Lock()
	defer gangCache.lock.Unlock()

	delete(gangCache.gangItems, gangId)
	klog.Infof("delete gang from cache, gang: %v", gangId)
}

func (gangCache *GangCache) onPodAdd(obj interface{}) {
	pod, ok := obj.(*v1.Pod)
	if !ok {
		return
	}

	gangName := util.GetGangNameByPod(pod)
	if gangName == "" {
		return
	}
	// gangName has to match DNS (RFC 1123) check
	errMSg := validation.IsDNS1123Subdomain(gangName)
	if len(errMSg) != 0 {
		klog.Errorf("gangName is invalid,it has to match RFC 1123 check, gangName: %v", gangName)
		return
	}

	gangNamespace := pod.Namespace
	gangId := util.GetId(gangNamespace, gangName)
	gang := gangCache.getGangFromCacheByGangId(gangId, true)

	// the gang is created in Annotation way
	shouldCreatePg := false
	if _, exist := pod.Labels[v1alpha1.PodGroupLabel]; !exist {
		shouldCreatePg = gang.tryInitByPodConfig(pod, gangCache.pluginArgs)
	}
	gang.setChild(pod)
	if pod.Spec.NodeName != "" {
		gang.addBoundPod(pod)
		gang.setResourceSatisfied()
	}

	if shouldCreatePg {
		pg, _ := gangCache.pgLister.PodGroups(gangNamespace).Get(gangName)
		// cluster doesn't have the podGroup
		if pg == nil {
			pgFromAnnotation := generateNewPodGroup(gang, pod)
			err := retry.OnError(
				retry.DefaultRetry,
				errors.IsTooManyRequests,
				func() error {
					_, err := gangCache.pgClient.SchedulingV1alpha1().PodGroups(pod.Namespace).Create(context.TODO(), pgFromAnnotation, metav1.CreateOptions{})
					return err
				})
			if err != nil {
				klog.Errorf("Create podGroup by pod's annotations error, pod: %v, err: %v ", util.GetId(pod.Namespace, pod.Name), err)
			} else {
				klog.Infof("Create podGroup by pod's annotations success, pg: %v, pod: %v", gangId, util.GetId(pod.Namespace, pod.Name))
			}
		}
	}
}

func (gangCache *GangCache) onPodUpdate(oldObj interface{}, newObj interface{}) {
}

func (gangCache *GangCache) onPodDelete(obj interface{}) {
	pod, ok := obj.(*v1.Pod)
	if !ok {
		return
	}
	gangName := util.GetGangNameByPod(pod)
	if gangName == "" {
		return
	}

	gangNamespace := pod.Namespace
	gangId := util.GetId(gangNamespace, gangName)
	gang := gangCache.getGangFromCacheByGangId(gangId, false)
	if gang == nil {
		return
	}

	shouldDeleteGang := gang.deletePod(pod)
	if shouldDeleteGang {
		gangCache.deleteGangFromCacheByGangId(gangId)
		// delete podGroup
		err := retry.OnError(
			retry.DefaultRetry,
			errors.IsTooManyRequests,
			func() error {
				err := gangCache.pgClient.SchedulingV1alpha1().PodGroups(pod.Namespace).Delete(context.TODO(), gangName, metav1.DeleteOptions{})
				return err
			})
		if err != nil {
			klog.Errorf("Delete podGroup by gang's deletion error, gang: %v, error: %v", gangId, err)
		} else {
			klog.Infof("Delete podGroup by gang's deletion , gang: %v", gangId)
		}
	}
}

func (gangCache *GangCache) onPodGroupAdd(obj interface{}) {
	pg, ok := obj.(*v1alpha1.PodGroup)
	if !ok {
		return
	}
	// if podGroup is created from pod's annotation, we ignore it
	if pg.Annotations[PodGroupFromPodAnnotation] == "true" {
		return
	}
	gangNamespace := pg.Namespace
	gangName := pg.Name

	gangId := util.GetId(gangNamespace, gangName)
	gang := gangCache.getGangFromCacheByGangId(gangId, false)
	if gang == nil {
		gang = gangCache.getGangFromCacheByGangId(gangId, true)
		klog.Infof("Create gang by podGroup on add, gangName: %v", gangId)
	}

	gang.tryInitByPodGroup(pg, gangCache.pluginArgs)
}

func (gangCache *GangCache) onPodGroupUpdate(oldObj interface{}, newObj interface{}) {
}

func (gangCache *GangCache) onPodGroupDelete(obj interface{}) {
	pg, ok := obj.(*v1alpha1.PodGroup)
	if !ok {
		return
	}
	if pg.Annotations[PodGroupFromPodAnnotation] == "true" {
		return
	}
	gangNamespace := pg.Namespace
	gangName := pg.Name

	gangId := util.GetId(gangNamespace, gangName)
	gang := gangCache.getGangFromCacheByGangId(gangId, false)
	if gang == nil {
		return
	}
	gangCache.deleteGangFromCacheByGangId(gangId)
}

func generateNewPodGroup(gang *Gang, pod *v1.Pod) *v1alpha1.PodGroup {
	gangName := extension.GetGangName(pod)
	pg := &v1alpha1.PodGroup{
		ObjectMeta: metav1.ObjectMeta{
			Name:              gangName,
			Namespace:         pod.Namespace,
			CreationTimestamp: metav1.Time{Time: gang.CreateTime},
			Annotations: map[string]string{
				PodGroupFromPodAnnotation: "true",
			},
		},
		Spec: v1alpha1.PodGroupSpec{
			ScheduleTimeoutSeconds: pointer.Int32(int32(gang.getGangWaitTime() / time.Second)),
			MinMember:              int32(gang.getGangMinNum()),
		},
		Status: v1alpha1.PodGroupStatus{
			ScheduleStartTime: metav1.Now(),
		},
	}
	return pg
}
