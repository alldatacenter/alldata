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
	corev1 "k8s.io/api/core/v1"
	"sigs.k8s.io/controller-runtime/pkg/client"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

var _ Object = &Reservation{}

type Reservation struct {
	*sev1alpha1.Reservation
}

func NewReservation(reservation *sev1alpha1.Reservation) Object {
	return &Reservation{Reservation: reservation}
}

func (r *Reservation) String() string {
	return r.Reservation.Name
}

func (r *Reservation) OriginObject() client.Object {
	return r.Reservation
}

func (r *Reservation) GetReservationConditions() []sev1alpha1.ReservationCondition {
	if len(r.Status.Conditions) == 0 {
		return nil
	}
	conditions := make([]sev1alpha1.ReservationCondition, 0, len(r.Status.Conditions))
	for i := range r.Status.Conditions {
		conditions = append(conditions, *r.Status.Conditions[i].DeepCopy())
	}
	return conditions
}

func (r *Reservation) QueryPreemptedPodsRefs() []corev1.ObjectReference {
	return nil
}

func (r *Reservation) GetBoundPod() *corev1.ObjectReference {
	if len(r.Status.CurrentOwners) == 0 {
		return nil
	}
	return &r.Status.CurrentOwners[0]
}

func (r *Reservation) GetReservationOwners() []sev1alpha1.ReservationOwner {
	return r.Spec.Owners
}

func (r *Reservation) GetScheduledNodeName() string {
	return r.Status.NodeName
}

func (r *Reservation) GetPhase() sev1alpha1.ReservationPhase {
	return r.Status.Phase
}

func (r *Reservation) NeedPreemption() bool {
	return false
}

func GetReservationCondition(r Object, conditionType sev1alpha1.ReservationConditionType, reason string) *sev1alpha1.ReservationCondition {
	conditions := r.GetReservationConditions()
	if len(conditions) == 0 {
		return nil
	}
	for i := range conditions {
		cond := &conditions[i]
		if cond.Type == conditionType && (reason == "" || reason == cond.Reason) {
			return cond
		}
	}
	return nil
}

func GetUnschedulableCondition(r Object) *sev1alpha1.ReservationCondition {
	return GetReservationCondition(r, sev1alpha1.ReservationConditionScheduled, sev1alpha1.ReasonReservationUnschedulable)
}

func IsReservationScheduled(r Object) bool {
	if r.GetScheduledNodeName() == "" {
		return false
	}
	cond := GetReservationCondition(r, sev1alpha1.ReservationConditionScheduled, sev1alpha1.ReasonReservationScheduled)
	return cond != nil && cond.Status == sev1alpha1.ConditionStatusTrue
}

func IsReservationPending(r Object) bool {
	return r != nil && (r.GetPhase() == "" || r.GetPhase() == sev1alpha1.ReservationPending)
}

// IsReservationAvailable checks if the reservation is scheduled on a node and its status is Available.
func IsReservationAvailable(r Object) bool {
	return r != nil && r.GetScheduledNodeName() != "" && r.GetPhase() == sev1alpha1.ReservationAvailable
}

func IsReservationSucceeded(r Object) bool {
	return r != nil && r.GetPhase() == sev1alpha1.ReservationSucceeded
}

func IsReservationFailed(r Object) bool {
	return r != nil && r.GetPhase() == sev1alpha1.ReservationFailed
}

func IsReservationExpired(r Object) bool {
	if !IsReservationFailed(r) {
		return false
	}
	condition := GetReservationCondition(r, sev1alpha1.ReservationConditionReady, sev1alpha1.ReasonReservationExpired)
	return condition != nil
}
