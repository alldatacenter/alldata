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
	"fmt"
	"strings"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"
	resourceapi "k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	// NodeNameIndex is the lookup name for the index function, which is to index by the status.nodeName field.
	NodeNameIndex string = "status.nodeName"
)

// StatusNodeNameIndexFunc is an index function that indexes based on a reservation's status.nodeName
func StatusNodeNameIndexFunc(obj interface{}) ([]string, error) {
	r, ok := obj.(*schedulingv1alpha1.Reservation)
	if !ok {
		return []string{}, nil
	}
	if len(r.Status.NodeName) <= 0 {
		return []string{}, nil
	}
	return []string{r.Status.NodeName}, nil
}

func isReservationNeedExpiration(r *schedulingv1alpha1.Reservation) bool {
	// 1. failed or succeeded reservations does not need to expire
	if r.Status.Phase == schedulingv1alpha1.ReservationFailed || r.Status.Phase == schedulingv1alpha1.ReservationSucceeded {
		return false
	}
	// 2. disable expiration if TTL is set as 0
	if r.Spec.TTL != nil && r.Spec.TTL.Duration == 0 {
		return false
	}
	// 3. if both TTL and Expires are set, firstly check Expires
	return r.Spec.Expires != nil && time.Now().After(r.Spec.Expires.Time) ||
		r.Spec.TTL != nil && time.Since(r.CreationTimestamp.Time) > r.Spec.TTL.Duration
}

func isReservationNeedCleanup(r *schedulingv1alpha1.Reservation) bool {
	if r == nil {
		return true
	}
	if util.IsReservationExpired(r) {
		for _, condition := range r.Status.Conditions {
			if condition.Reason == schedulingv1alpha1.ReasonReservationExpired {
				return time.Since(condition.LastTransitionTime.Time) > defaultGCDuration
			}
		}
	} else if util.IsReservationSucceeded(r) {
		for _, condition := range r.Status.Conditions {
			if condition.Reason == schedulingv1alpha1.ReasonReservationSucceeded {
				return time.Since(condition.LastProbeTime.Time) > defaultGCDuration
			}
		}
	}
	return false
}

func setReservationAvailable(r *schedulingv1alpha1.Reservation, nodeName string) {
	// just annotate scheduled node at status
	util.SetReservationNodeName(r, nodeName)
	r.Status.Phase = schedulingv1alpha1.ReservationAvailable
	r.Status.CurrentOwners = make([]corev1.ObjectReference, 0)

	requests := getReservationRequests(r)
	r.Status.Allocatable = requests
	r.Status.Allocated = util.NewZeroResourceList()

	// initialize the conditions
	r.Status.Conditions = []schedulingv1alpha1.ReservationCondition{
		{
			Type:               schedulingv1alpha1.ReservationConditionScheduled,
			Status:             schedulingv1alpha1.ConditionStatusTrue,
			Reason:             schedulingv1alpha1.ReasonReservationScheduled,
			LastProbeTime:      metav1.Now(),
			LastTransitionTime: metav1.Now(),
		},
		{
			Type:               schedulingv1alpha1.ReservationConditionReady,
			Status:             schedulingv1alpha1.ConditionStatusTrue,
			Reason:             schedulingv1alpha1.ReasonReservationAvailable,
			LastProbeTime:      metav1.Now(),
			LastTransitionTime: metav1.Now(),
		},
	}
}

func setReservationExpired(r *schedulingv1alpha1.Reservation) {
	r.Status.Phase = schedulingv1alpha1.ReservationFailed
	// not duplicate expired info
	idx := -1
	isReady := false
	for i, condition := range r.Status.Conditions {
		if condition.Type == schedulingv1alpha1.ReservationConditionReady {
			idx = i
			isReady = condition.Status == schedulingv1alpha1.ConditionStatusTrue
		}
	}
	if idx < 0 { // if not set condition
		condition := schedulingv1alpha1.ReservationCondition{
			Type:               schedulingv1alpha1.ReservationConditionReady,
			Status:             schedulingv1alpha1.ConditionStatusFalse,
			Reason:             schedulingv1alpha1.ReasonReservationExpired,
			LastProbeTime:      metav1.Now(),
			LastTransitionTime: metav1.Now(),
		}
		r.Status.Conditions = append(r.Status.Conditions, condition)
	} else if isReady { // if was ready
		condition := schedulingv1alpha1.ReservationCondition{
			Type:               schedulingv1alpha1.ReservationConditionReady,
			Status:             schedulingv1alpha1.ConditionStatusFalse,
			Reason:             schedulingv1alpha1.ReasonReservationExpired,
			LastProbeTime:      metav1.Now(),
			LastTransitionTime: metav1.Now(),
		}
		r.Status.Conditions[idx] = condition
	} else { // if already not ready
		r.Status.Conditions[idx].Reason = schedulingv1alpha1.ReasonReservationExpired
		r.Status.Conditions[idx].LastProbeTime = metav1.Now()
	}
}

func setReservationAllocated(r *schedulingv1alpha1.Reservation, pod *corev1.Pod) {
	owner := getPodOwner(pod)
	requests, _ := resourceapi.PodRequestsAndLimits(pod)
	// avoid duplication (it happens if pod allocated annotation was missing)
	idx := -1
	for i, current := range r.Status.CurrentOwners {
		if matchObjectRef(pod, &current) {
			idx = i
		}
	}
	if idx < 0 {
		r.Status.CurrentOwners = append(r.Status.CurrentOwners, owner)
		if r.Status.Allocated == nil {
			r.Status.Allocated = requests
		} else {
			r.Status.Allocated = quotav1.Add(r.Status.Allocated, requests)
		}
	} else {
		// keep old allocated
		r.Status.CurrentOwners[idx] = owner
	}
	if r.Spec.AllocateOnce {
		setReservationSucceeded(r)
	}
}

func setReservationSucceeded(r *schedulingv1alpha1.Reservation) {
	r.Status.Phase = schedulingv1alpha1.ReservationSucceeded
	idx := -1
	for i, condition := range r.Status.Conditions {
		if condition.Type == schedulingv1alpha1.ReservationConditionReady {
			idx = i
		}
	}
	if idx < 0 { // if not set condition
		condition := schedulingv1alpha1.ReservationCondition{
			Type:               schedulingv1alpha1.ReservationConditionReady,
			Status:             schedulingv1alpha1.ConditionStatusFalse,
			Reason:             schedulingv1alpha1.ReasonReservationSucceeded,
			LastProbeTime:      metav1.Now(),
			LastTransitionTime: metav1.Now(),
		}
		r.Status.Conditions = append(r.Status.Conditions, condition)
	} else {
		r.Status.Conditions[idx].Status = schedulingv1alpha1.ConditionStatusFalse
		r.Status.Conditions[idx].Reason = schedulingv1alpha1.ReasonReservationSucceeded
		r.Status.Conditions[idx].LastProbeTime = metav1.Now()
	}
}

func removeReservationAllocated(r *schedulingv1alpha1.Reservation, pod *corev1.Pod) error {
	// remove matched owner info
	idx := -1
	for i, owner := range r.Status.CurrentOwners {
		if matchObjectRef(pod, &owner) {
			idx = i
		}
	}
	if idx < 0 {
		return fmt.Errorf("current owner not matched")
	}
	r.Status.CurrentOwners = append(r.Status.CurrentOwners[:idx], r.Status.CurrentOwners[idx+1:]...)

	// decrease resources allocated
	requests, _ := resourceapi.PodRequestsAndLimits(pod)
	if r.Status.Allocated != nil {
		r.Status.Allocated = quotav1.Subtract(r.Status.Allocated, requests)
	} else {
		klog.V(5).InfoS("failed to remove pod from reservation allocated, err: allocated is nil")
	}

	if r.Spec.AllocateOnce {
		removeReservationSucceeded(r)
	}

	return nil
}

func removeReservationSucceeded(r *schedulingv1alpha1.Reservation) {
	// only available reservation can trans to succeeded
	r.Status.Phase = schedulingv1alpha1.ReservationAvailable
	idx := -1
	for i, condition := range r.Status.Conditions {
		if condition.Type == schedulingv1alpha1.ReservationConditionReady {
			idx = i
		}
	}
	if idx >= 0 {
		r.Status.Conditions[idx].Status = schedulingv1alpha1.ConditionStatusTrue
		r.Status.Conditions[idx].Reason = schedulingv1alpha1.ReasonReservationAvailable
		r.Status.Conditions[idx].LastProbeTime = r.Status.Conditions[idx].LastTransitionTime
	}
}

func getReservationRequests(r *schedulingv1alpha1.Reservation) corev1.ResourceList {
	requests, _ := resourceapi.PodRequestsAndLimits(&corev1.Pod{
		Spec: r.Spec.Template.Spec,
	})
	return requests
}

func matchReservation(pod *corev1.Pod, rMeta *reservationInfo) bool {
	return matchReservationOwners(pod, rMeta.Reservation) && matchReservationResources(pod, rMeta.Reservation, rMeta.Resources) && matchReservationPort(pod, rMeta)
}

func matchReservationPort(pod *corev1.Pod, rMeta *reservationInfo) bool {
	for _, container := range pod.Spec.Containers {
		for _, podPort := range container.Ports {
			if podPort.HostPort > 0 && !rMeta.Port.CheckConflict(podPort.HostIP, string(podPort.Protocol), podPort.HostPort) {
				return false
			}
		}
	}
	return true
}

func matchReservationResources(pod *corev1.Pod, r *schedulingv1alpha1.Reservation, reservedResources corev1.ResourceList) bool {
	if r.Status.Allocated != nil {
		// multi owners can share one reservation when reserved resources are sufficient
		reservedResources = quotav1.Subtract(reservedResources, r.Status.Allocated)
	}
	podRequests, _ := resourceapi.PodRequestsAndLimits(pod)
	for resource, quantity := range podRequests {
		q := reservedResources[resource]
		if quantity.Cmp(q) > 0 { // not match if any pod request is larger than reserved resources
			return false
		}
	}
	return true
}

// matchReservationOwners checks if the scheduling pod matches the reservation's owner spec.
// `reservation.spec.owners` defines the DNF (disjunctive normal form) of ObjectReference, ControllerReference
// (extended), LabelSelector, which means multiple selectors are firstly ANDed and secondly ORed.
func matchReservationOwners(pod *corev1.Pod, r *schedulingv1alpha1.Reservation) bool {
	// assert pod != nil && r != nil
	// Owners == nil matches nothing, while Owners = [{}] matches everything
	for _, owner := range r.Spec.Owners {
		if matchObjectRef(pod, owner.Object) &&
			matchReservationControllerReference(pod, owner.Controller) &&
			matchLabelSelector(pod, owner.LabelSelector) {
			return true
		}
	}
	return false
}

func matchObjectRef(pod *corev1.Pod, objRef *corev1.ObjectReference) bool {
	// `ResourceVersion`, `FieldPath` are ignored.
	// since only pod type are compared, `Kind` field is also ignored.
	return objRef == nil ||
		(len(objRef.UID) <= 0 || pod.UID == objRef.UID) &&
			(len(objRef.Name) <= 0 || pod.Name == objRef.Name) &&
			(len(objRef.Namespace) <= 0 || pod.Namespace == objRef.Namespace) &&
			(len(objRef.APIVersion) <= 0 || pod.APIVersion == objRef.APIVersion)
}

func matchReservationControllerReference(pod *corev1.Pod, controllerRef *schedulingv1alpha1.ReservationControllerReference) bool {
	// controllerRef matched if any of pod owner references matches the controllerRef;
	// typically a pod has only one controllerRef
	if controllerRef == nil {
		return true
	}
	if len(controllerRef.Namespace) > 0 && controllerRef.Namespace != pod.Namespace { // namespace field is extended
		return false
	}
	// currently `BlockOwnerDeletion` is ignored
	for _, podOwner := range pod.OwnerReferences {
		if (controllerRef.Controller == nil || podOwner.Controller != nil && *controllerRef.Controller == *podOwner.Controller) &&
			(len(controllerRef.UID) <= 0 || controllerRef.UID == podOwner.UID) &&
			(len(controllerRef.Name) <= 0 || controllerRef.Name == podOwner.Name) &&
			(len(controllerRef.Kind) <= 0 || controllerRef.Kind == podOwner.Kind) &&
			(len(controllerRef.APIVersion) <= 0 || controllerRef.APIVersion == podOwner.APIVersion) {
			return true
		}
	}
	return false
}

func dumpMatchReservationReason(pod *corev1.Pod, rMeta *reservationInfo) string {
	var msg strings.Builder
	if !matchReservationOwners(pod, rMeta.Reservation) {
		msg.WriteString("owner specs not matched;")
	}
	if !matchReservationResources(pod, rMeta.Reservation, rMeta.Resources) {
		msg.WriteString("resources not matched;")
	}
	if !matchReservationPort(pod, rMeta) {
		msg.WriteString("port not matched;")
	}
	return msg.String()
}

func matchLabelSelector(pod *corev1.Pod, labelSelector *metav1.LabelSelector) bool {
	if labelSelector == nil {
		return true
	}
	selector, err := metav1.LabelSelectorAsSelector(labelSelector)
	if err != nil {
		return false
	}
	return selector.Matches(labels.Set(pod.Labels))
}

func getPodOwner(pod *corev1.Pod) corev1.ObjectReference {
	return corev1.ObjectReference{
		Namespace: pod.Namespace,
		Name:      pod.Name,
		UID:       pod.UID,
		// currently `Kind`, `APIVersion`m `ResourceVersion`, `FieldPath` are ignored
	}
}

func getOwnerKey(owner *corev1.ObjectReference) string {
	return string(owner.UID)
}

func getPreFilterState(cycleState *framework.CycleState) *stateData {
	v, err := cycleState.Read(preFilterStateKey)
	if err != nil {
		return nil
	}
	cache, ok := v.(*stateData)
	if !ok || cache == nil {
		return nil
	}
	return cache
}
