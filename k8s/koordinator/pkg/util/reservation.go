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

package util

import (
	"fmt"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

var (
	// AnnotationReservePod indicates whether the pod is a reserved pod.
	AnnotationReservePod = apiext.SchedulingDomainPrefix + "/reserve-pod"
	// AnnotationReservationName indicates the name of the reservation.
	AnnotationReservationName = apiext.SchedulingDomainPrefix + "/reservation-name"
	// AnnotationReservationNode indicates the node name if the reservation specifies a node.
	AnnotationReservationNode = apiext.SchedulingDomainPrefix + "/reservation-node"
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
	if len(reservePod.Namespace) <= 0 {
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

	return reservePod
}

func ValidateReservation(r *schedulingv1alpha1.Reservation) error {
	if r == nil {
		return fmt.Errorf("the reservation is nil")
	}
	if r.Spec.Template == nil {
		return fmt.Errorf("the reservation misses the template spec")
	}
	if len(r.Spec.Owners) <= 0 {
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

func GetReservePodKey(pod *corev1.Pod) string {
	return string(pod.UID)
}

func GetReservePodNodeName(pod *corev1.Pod) string {
	return pod.Annotations[AnnotationReservationNode]
}

func GetReservationNameFromReservePod(pod *corev1.Pod) string {
	return pod.Annotations[AnnotationReservationName]
}

func GetReservationSchedulerName(r *schedulingv1alpha1.Reservation) string {
	if r == nil || r.Spec.Template == nil || len(r.Spec.Template.Spec.SchedulerName) <= 0 {
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

func SetReservationNodeName(r *schedulingv1alpha1.Reservation, nodeName string) {
	r.Status.NodeName = nodeName
}

func SetReservationUnschedulable(r *schedulingv1alpha1.Reservation, msg string) {
	// unschedule reservations can try scheduling in next cycles, so we does not update its phase
	// not duplicate condition info
	idx := -1
	isScheduled := false
	for i, condition := range r.Status.Conditions {
		if condition.Type == schedulingv1alpha1.ReservationConditionScheduled {
			idx = i
			isScheduled = condition.Status == schedulingv1alpha1.ConditionStatusTrue
		}
	}
	if idx < 0 { // if not set condition
		condition := schedulingv1alpha1.ReservationCondition{
			Type:               schedulingv1alpha1.ReservationConditionScheduled,
			Status:             schedulingv1alpha1.ConditionStatusFalse,
			Reason:             schedulingv1alpha1.ReasonReservationUnschedulable,
			Message:            msg,
			LastProbeTime:      metav1.Now(),
			LastTransitionTime: metav1.Now(),
		}
		r.Status.Conditions = append(r.Status.Conditions, condition)
	} else if isScheduled { // if is scheduled, keep the condition status
		r.Status.Conditions[idx].LastProbeTime = metav1.Now()
	} else { // if already unschedulable, update the message
		r.Status.Conditions[idx].Reason = schedulingv1alpha1.ReasonReservationUnschedulable
		r.Status.Conditions[idx].Message = msg
		r.Status.Conditions[idx].LastProbeTime = metav1.Now()
	}
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

// ReservationToPodEventHandlerFuncs can be used to handle reservation events with a pod event handler, which converts
// each reservation object into the corresponding reserve pod object.
//
//	e.g.
//	func registerReservationEventHandler(handle framework.Handle, podHandler podHandler) {
//	  extendedHandle, ok := handle.(frameworkext.ExtendedHandle)
//	  if !ok { // if not implement extendedHandle, ignore reservation events
//	    klog.V(3).Infof("registerReservationEventHandler aborted, cannot convert handle to frameworkext.ExtendedHandle, got %T", handle)
//	    return
//	  }
//	  extendedHandle.KoordinatorSharedInformerFactory().Scheduling().V1alpha1().Reservations().Informer().AddEventHandler(&util.ReservationToPodEventHandlerFuncs{
//	    FilterFunc: util.IsObjValidActiveReservation,
//	    PodHandler: &podHandler,
//	  })
//	}
type ReservationToPodEventHandlerFuncs struct {
	FilterFunc func(obj interface{}) bool
	PodHandler cache.ResourceEventHandler
}

var _ cache.ResourceEventHandler = &ReservationToPodEventHandlerFuncs{}

func (r ReservationToPodEventHandlerFuncs) OnAdd(obj interface{}) {
	reservation, ok := obj.(*schedulingv1alpha1.Reservation)
	if !ok {
		return
	}
	if !r.FilterFunc(reservation) {
		return
	}

	pod := NewReservePod(reservation)
	r.PodHandler.OnAdd(pod)
}

// OnUpdate calls UpdateFunc if it's not nil.
func (r ReservationToPodEventHandlerFuncs) OnUpdate(oldObj, newObj interface{}) {
	oldR, oldOK := oldObj.(*schedulingv1alpha1.Reservation)
	newR, newOK := newObj.(*schedulingv1alpha1.Reservation)
	if !oldOK || !newOK {
		return
	}

	oldOK = r.FilterFunc(oldR)
	newOK = r.FilterFunc(newR)
	switch {
	case oldOK && newOK:
		oldPod := NewReservePod(oldR)
		newPod := NewReservePod(newR)
		r.PodHandler.OnUpdate(oldPod, newPod)
	case !oldOK && newOK:
		newPod := NewReservePod(newR)
		r.PodHandler.OnAdd(newPod)
	case oldOK && !newOK:
		oldPod := NewReservePod(oldR)
		r.PodHandler.OnDelete(oldPod)
	default:
		// do nothing
	}
}

// OnDelete calls DeleteFunc if it's not nil.
func (r ReservationToPodEventHandlerFuncs) OnDelete(obj interface{}) {
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
	if !r.FilterFunc(reservation) {
		return
	}

	pod := NewReservePod(reservation)
	r.PodHandler.OnDelete(pod)
}
