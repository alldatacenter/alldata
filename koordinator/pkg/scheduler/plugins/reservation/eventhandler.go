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

	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	frameworkexthelper "github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/helper"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

type reservationEventHandler struct {
	cache *reservationCache
}

func registerReservationEventHandler(cache *reservationCache, koordinatorInformerFactory koordinatorinformers.SharedInformerFactory) {
	eventHandler := &reservationEventHandler{
		cache: cache,
	}
	reservationInformer := koordinatorInformerFactory.Scheduling().V1alpha1().Reservations().Informer()
	frameworkexthelper.ForceSyncFromInformer(context.TODO().Done(), koordinatorInformerFactory, reservationInformer, eventHandler)
}

func (h *reservationEventHandler) OnAdd(obj interface{}) {
	r, ok := obj.(*schedulingv1alpha1.Reservation)
	if !ok {
		return
	}
	if reservationutil.IsReservationActive(r) {
		h.cache.updateReservation(r)
	}
	klog.V(5).InfoS("reservation cache add", "reservation", klog.KObj(r))
}

func (h *reservationEventHandler) OnUpdate(oldObj, newObj interface{}) {
	oldR, oldOK := oldObj.(*schedulingv1alpha1.Reservation)
	newR, newOK := newObj.(*schedulingv1alpha1.Reservation)
	if !oldOK || !newOK {
		return
	}
	if oldR == nil || newR == nil {
		return
	}

	if reservationutil.IsReservationActive(newR) {
		h.cache.updateReservation(newR)
	} else if reservationutil.IsReservationFailed(newR) || reservationutil.IsReservationSucceeded(newR) {
		h.cache.deleteReservation(newR)
	}
	klog.V(5).InfoS("reservation cache update", "reservation", klog.KObj(newR))
}

func (h *reservationEventHandler) OnDelete(obj interface{}) {
	var r *schedulingv1alpha1.Reservation
	switch t := obj.(type) {
	case *schedulingv1alpha1.Reservation:
		r = t
	case cache.DeletedFinalStateUnknown:
		deletedReservation, ok := t.Obj.(*schedulingv1alpha1.Reservation)
		if ok {
			r = deletedReservation
		}
	}
	if r == nil {
		klog.V(4).InfoS("reservation cache delete failed to parse, obj %T", obj)
		return
	}
	h.cache.deleteReservation(r)
	klog.V(5).InfoS("reservation cache delete", "reservation", klog.KObj(r))
}
