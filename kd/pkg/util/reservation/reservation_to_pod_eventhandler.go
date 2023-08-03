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
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

// ReservationToPodEventHandler can be used to handle reservation events with a pod event handler, which converts
// each reservation object into the corresponding reserve pod object.
//
//	e.g.
//	func registerReservationEventHandler(handle framework.Handle, podHandler podHandler) {
//	  extendedHandle, ok := handle.(frameworkext.ExtendedHandle)
//	  if !ok { // if not implement extendedHandle, ignore reservation events
//	    klog.V(3).Infof("registerReservationEventHandler aborted, cannot convert handle to frameworkext.ExtendedHandle, got %T", handle)
//	    return
//	  }
//	  extendedHandle.KoordinatorSharedInformerFactory().Scheduling().V1alpha1().Reservations().Informer().
//	 	 AddEventHandler(util.NewReservationToPodEventHandler(&podHandler, IsObjValidActiveReservation))
//	}
type ReservationToPodEventHandler struct {
	handler cache.ResourceEventHandler
}

var _ cache.ResourceEventHandler = &ReservationToPodEventHandler{}

func NewReservationToPodEventHandler(handler cache.ResourceEventHandler, filters ...func(obj interface{}) bool) cache.ResourceEventHandler {
	return cache.FilteringResourceEventHandler{
		FilterFunc: func(obj interface{}) bool {
			for _, fn := range filters {
				if !fn(obj) {
					return false
				}
			}
			return true
		},
		Handler: &ReservationToPodEventHandler{
			handler: handler,
		},
	}
}

func (r ReservationToPodEventHandler) OnAdd(obj interface{}) {
	reservation, ok := obj.(*schedulingv1alpha1.Reservation)
	if !ok {
		return
	}
	pod := NewReservePod(reservation)
	r.handler.OnAdd(pod)
}

// OnUpdate calls UpdateFunc if it's not nil.
func (r ReservationToPodEventHandler) OnUpdate(oldObj, newObj interface{}) {
	oldR, oldOK := oldObj.(*schedulingv1alpha1.Reservation)
	newR, newOK := newObj.(*schedulingv1alpha1.Reservation)
	if !oldOK || !newOK {
		return
	}

	oldPod := NewReservePod(oldR)
	newPod := NewReservePod(newR)
	r.handler.OnUpdate(oldPod, newPod)
}

// OnDelete calls DeleteFunc if it's not nil.
func (r ReservationToPodEventHandler) OnDelete(obj interface{}) {
	var reservation *schedulingv1alpha1.Reservation
	switch t := obj.(type) {
	case *schedulingv1alpha1.Reservation:
		reservation = t
	case cache.DeletedFinalStateUnknown:
		var ok bool
		reservation, ok = t.Obj.(*schedulingv1alpha1.Reservation)
		if !ok {
			return
		}
	default:
		return
	}

	pod := NewReservePod(reservation)
	r.handler.OnDelete(pod)
}

func IsObjValidActiveReservation(obj interface{}) bool {
	reservation, _ := obj.(*schedulingv1alpha1.Reservation)
	err := ValidateReservation(reservation)
	if err != nil {
		klog.ErrorS(err, "failed to validate reservation obj", "reservation", klog.KObj(reservation))
		return false
	}
	if !IsReservationActive(reservation) {
		klog.V(6).InfoS("ignore reservation obj since it is not active",
			"reservation", klog.KObj(reservation), "phase", reservation.Status.Phase)
		return false
	}
	return true
}
