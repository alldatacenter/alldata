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

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

var (
	// AnnotationReservePod indicates whether the pod is a reserved pod.
	AnnotationReservePod = extension.SchedulingDomainPrefix + "/reserve-pod"
	// AnnotationReservationName indicates the name of the reservation.
	AnnotationReservationName = extension.SchedulingDomainPrefix + "/reservation-name"
	// AnnotationReservationNode indicates the node name if the reservation specifies a node.
	AnnotationReservationNode = extension.SchedulingDomainPrefix + "/reservation-node"
)

// NewReservePod returns a fake pod set as the reservation's specifications.
// The reserve pod is only visible for the scheduler and does not make actual creation on nodes.
func NewReservePod(r *schedulingv1alpha1.Reservation) *corev1.Pod {
	reservePod := &corev1.Pod{}
	if r.Spec.Template != nil {
		reservePod.ObjectMeta = *r.Spec.Template.ObjectMeta.DeepCopy()
		reservePod.Spec = *r.Spec.Template.Spec.DeepCopy()
	} else {
		klog.V(4).InfoS("failed to set valid spec for new reserve pod, template is nil", "spec", r.Spec)
	}
	// name, uid: reservation uid
	reservePod.Name = GetReservationKey(r)
	reservePod.UID = r.UID
	if len(reservePod.Namespace) == 0 {
		reservePod.Namespace = corev1.NamespaceDefault
	}

	// labels, annotations: `objectMeta` overwrites `template.objectMeta`
	if reservePod.Labels == nil {
		reservePod.Labels = map[string]string{}
	}
	for k, v := range r.Labels {
		reservePod.Labels[k] = v
	}
	if reservePod.Annotations == nil {
		reservePod.Annotations = map[string]string{}
	}
	for k, v := range r.Annotations {
		reservePod.Annotations[k] = v
	}
	// annotate the reservePod
	reservePod.Annotations[AnnotationReservePod] = "true"
	reservePod.Annotations[AnnotationReservationName] = r.Name // for search inversely

	// annotate node name specified
	if len(reservePod.Spec.NodeName) > 0 {
		// if the reservation specifies a nodeName, annotate it and cleanup spec.nodeName for other plugins not
		// processing the nodeName before binding
		reservePod.Annotations[AnnotationReservationNode] = reservePod.Spec.NodeName
		reservePod.Spec.NodeName = ""
	}
	// use reservation status.nodeName as the real scheduled result
	if nodeName := GetReservationNodeName(r); len(nodeName) > 0 {
		reservePod.Spec.NodeName = nodeName
	}

	if IsReservationSucceeded(r) {
		reservePod.Status.Phase = corev1.PodSucceeded
	} else if IsReservationExpired(r) || IsReservationFailed(r) {
		reservePod.Status.Phase = corev1.PodFailed
	}

	reservePod.Spec.SchedulerName = GetReservationSchedulerName(r)
	return reservePod
}

func ValidateReservation(r *schedulingv1alpha1.Reservation) error {
	if r == nil {
		return fmt.Errorf("the reservation is nil")
	}
	if r.Spec.Template == nil {
		return fmt.Errorf("the reservation misses the template spec")
	}
	if len(r.Spec.Owners) == 0 {
		return fmt.Errorf("the reservation misses the owner spec")
	}
	if r.Spec.TTL == nil && r.Spec.Expires == nil {
		return fmt.Errorf("the reservation misses the expiration spec")
	}
	return nil
}

func IsReservePod(pod *corev1.Pod) bool {
	return pod != nil && pod.Annotations != nil && pod.Annotations[AnnotationReservePod] == "true"
}

func GetReservationKey(r *schedulingv1alpha1.Reservation) string {
	return string(r.UID)
}

func GetReservePodNodeName(pod *corev1.Pod) string {
	return pod.Annotations[AnnotationReservationNode]
}

func GetReservationNameFromReservePod(pod *corev1.Pod) string {
	return pod.Annotations[AnnotationReservationName]
}

func GetReservationSchedulerName(r *schedulingv1alpha1.Reservation) string {
	if r == nil || r.Spec.Template == nil || len(r.Spec.Template.Spec.SchedulerName) == 0 {
		return corev1.DefaultSchedulerName
	}
	return r.Spec.Template.Spec.SchedulerName
}

// IsReservationActive checks if the reservation is scheduled and its status is Available/Waiting (active to use).
func IsReservationActive(r *schedulingv1alpha1.Reservation) bool {
	return r != nil && len(GetReservationNodeName(r)) > 0 &&
		(r.Status.Phase == schedulingv1alpha1.ReservationAvailable || r.Status.Phase == schedulingv1alpha1.ReservationWaiting)
}

// IsReservationAvailable checks if the reservation is scheduled on a node and its status is Available.
func IsReservationAvailable(r *schedulingv1alpha1.Reservation) bool {
	return r != nil && len(GetReservationNodeName(r)) > 0 && r.Status.Phase == schedulingv1alpha1.ReservationAvailable
}

func IsReservationSucceeded(r *schedulingv1alpha1.Reservation) bool {
	return r != nil && r.Status.Phase == schedulingv1alpha1.ReservationSucceeded
}

func IsReservationFailed(r *schedulingv1alpha1.Reservation) bool {
	return r != nil && r.Status.Phase == schedulingv1alpha1.ReservationFailed
}

func IsReservationExpired(r *schedulingv1alpha1.Reservation) bool {
	if r == nil || r.Status.Phase != schedulingv1alpha1.ReservationFailed {
		return false
	}
	for _, condition := range r.Status.Conditions {
		if condition.Type == schedulingv1alpha1.ReservationConditionReady {
			return condition.Status == schedulingv1alpha1.ConditionStatusFalse &&
				condition.Reason == schedulingv1alpha1.ReasonReservationExpired
		}
	}
	return false
}

func GetReservationNodeName(r *schedulingv1alpha1.Reservation) string {
	return r.Status.NodeName
}

func SetReservationExpired(r *schedulingv1alpha1.Reservation) {
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

func SetReservationSucceeded(r *schedulingv1alpha1.Reservation) {
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

func SetReservationAvailable(r *schedulingv1alpha1.Reservation, nodeName string) {
	r.Status.NodeName = nodeName
	r.Status.Phase = schedulingv1alpha1.ReservationAvailable
	r.Status.CurrentOwners = make([]corev1.ObjectReference, 0)

	requests := ReservationRequests(r)
	r.Status.Allocatable = requests
	r.Status.Allocated = nil

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

func ReservationRequests(r *schedulingv1alpha1.Reservation) corev1.ResourceList {
	requests, _ := resource.PodRequestsAndLimits(&corev1.Pod{
		Spec: r.Spec.Template.Spec,
	})
	return requests
}

func ReservePorts(r *schedulingv1alpha1.Reservation) framework.HostPortInfo {
	portInfo := framework.HostPortInfo{}
	for _, container := range r.Spec.Template.Spec.Containers {
		for _, podPort := range container.Ports {
			portInfo.Add(podPort.HostIP, string(podPort.Protocol), podPort.HostPort)
		}
	}
	return portInfo
}

// MatchReservationOwners checks if the scheduling pod matches the reservation's owner spec.
// `reservation.spec.owners` defines the DNF (disjunctive normal form) of ObjectReference, ControllerReference
// (extended), LabelSelector, which means multiple selectors are firstly ANDed and secondly ORed.
func MatchReservationOwners(pod *corev1.Pod, r *schedulingv1alpha1.Reservation) bool {
	// assert pod != nil && r != nil
	// Owners == nil matches nothing, while Owners = [{}] matches everything
	for _, owner := range r.Spec.Owners {
		if MatchObjectRef(pod, owner.Object) &&
			MatchReservationControllerReference(pod, owner.Controller) &&
			matchLabelSelector(pod, owner.LabelSelector) {
			return true
		}
	}
	return false
}

func MatchObjectRef(pod *corev1.Pod, objRef *corev1.ObjectReference) bool {
	// `ResourceVersion`, `FieldPath` are ignored.
	// since only pod type are compared, `Kind` field is also ignored.
	return objRef == nil ||
		(len(objRef.UID) == 0 || pod.UID == objRef.UID) &&
			(len(objRef.Name) == 0 || pod.Name == objRef.Name) &&
			(len(objRef.Namespace) == 0 || pod.Namespace == objRef.Namespace) &&
			(len(objRef.APIVersion) == 0 || pod.APIVersion == objRef.APIVersion)
}

func MatchReservationControllerReference(pod *corev1.Pod, controllerRef *schedulingv1alpha1.ReservationControllerReference) bool {
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
			(len(controllerRef.UID) == 0 || controllerRef.UID == podOwner.UID) &&
			(len(controllerRef.Name) == 0 || controllerRef.Name == podOwner.Name) &&
			(len(controllerRef.Kind) == 0 || controllerRef.Kind == podOwner.Kind) &&
			(len(controllerRef.APIVersion) == 0 || controllerRef.APIVersion == podOwner.APIVersion) {
			return true
		}
	}
	return false
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
