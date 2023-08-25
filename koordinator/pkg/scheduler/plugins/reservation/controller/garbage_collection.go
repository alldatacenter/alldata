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

package controller

import (
	"context"
	"time"

	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	corelister "k8s.io/client-go/listers/core/v1"
	"k8s.io/klog/v2"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

const (
	defaultGCCheckInterval = 60 * time.Second
	defaultGCDuration      = 24 * time.Hour
)

func (c *Controller) gcReservations() {
	reservations, err := c.reservationLister.List(labels.Everything())
	if err != nil {
		klog.Errorf("failed to list reservations, abort the GC turn, err: %s", err)
		return
	}
	for _, reservation := range reservations {
		if reservationutil.IsReservationExpired(reservation) || reservationutil.IsReservationSucceeded(reservation) {
			if isReservationNeedCleanup(reservation) || missingNode(reservation, c.nodeLister) {
				if err = c.koordClientSet.SchedulingV1alpha1().Reservations().Delete(context.TODO(), reservation.Name, metav1.DeleteOptions{}); err != nil {
					klog.V(3).InfoS("failed to delete reservation", "reservation", klog.KObj(reservation), "err", err)
				} else {
					klog.V(4).InfoS("Reservation has been garbage collected", "reservation", klog.KObj(reservation))
				}
			}
		}
	}
}

func missingNode(reservation *schedulingv1alpha1.Reservation, nodeLister corelister.NodeLister) bool {
	if reservation.Status.NodeName != "" {
		if _, err := nodeLister.Get(reservation.Status.NodeName); err != nil {
			if errors.IsNotFound(err) {
				return true
			}
		}
	}
	return false
}

func isReservationNeedCleanup(r *schedulingv1alpha1.Reservation) bool {
	if r == nil {
		return true
	}
	if reservationutil.IsReservationExpired(r) {
		for _, condition := range r.Status.Conditions {
			if condition.Reason == schedulingv1alpha1.ReasonReservationExpired {
				return time.Since(condition.LastTransitionTime.Time) > defaultGCDuration
			}
		}
	} else if reservationutil.IsReservationSucceeded(r) {
		for _, condition := range r.Status.Conditions {
			if condition.Reason == schedulingv1alpha1.ReasonReservationSucceeded {
				return time.Since(condition.LastProbeTime.Time) > defaultGCDuration
			}
		}
	}
	return false
}
