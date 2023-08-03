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

package reservation

import (
	"context"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/tools/cache"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	frameworkexthelper "github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/helper"
)

type podEventHandler struct {
	cache *reservationCache
}

func registerPodEventHandler(cache *reservationCache, factory informers.SharedInformerFactory) {
	eventHandler := &podEventHandler{
		cache: cache,
	}
	informer := factory.Core().V1().Pods().Informer()
	frameworkexthelper.ForceSyncFromInformer(context.TODO().Done(), factory, informer, eventHandler)
}

func (h *podEventHandler) OnAdd(obj interface{}) {
	pod, _ := obj.(*corev1.Pod)
	if pod == nil {
		return
	}

	reservationAllocated, err := apiext.GetReservationAllocated(pod)
	if err != nil || reservationAllocated == nil || reservationAllocated.UID == "" {
		return
	}
	h.cache.addPod(reservationAllocated.UID, pod)
}

func (h *podEventHandler) OnUpdate(oldObj, newObj interface{}) {
}

func (h *podEventHandler) OnDelete(obj interface{}) {
	var pod *corev1.Pod
	switch t := obj.(type) {
	case *corev1.Pod:
		pod = t
	case cache.DeletedFinalStateUnknown:
		pod, _ = t.Obj.(*corev1.Pod)
	}
	if pod == nil {
		return
	}

	reservationAllocated, err := apiext.GetReservationAllocated(pod)
	if err != nil || reservationAllocated == nil || reservationAllocated.UID == "" {
		return
	}
	h.cache.deletePod(reservationAllocated.UID, pod)
}
