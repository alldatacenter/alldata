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
	"k8s.io/apimachinery/pkg/types"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/kubernetes/pkg/api/v1/resource"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

type reservationInfo struct {
	reservation   *schedulingv1alpha1.Reservation
	resourceNames []corev1.ResourceName
	allocatable   corev1.ResourceList
	allocated     corev1.ResourceList
	pods          map[types.UID]*podRequirement
}

type podRequirement struct {
	namespace string
	name      string
	uid       types.UID
	requests  corev1.ResourceList
}

func newReservationInfo(r *schedulingv1alpha1.Reservation) *reservationInfo {
	allocatable := reservationutil.ReservationRequests(r)
	resourceNames := quotav1.ResourceNames(allocatable)

	return &reservationInfo{
		reservation:   r.DeepCopy(),
		resourceNames: resourceNames,
		allocatable:   allocatable,
		pods:          map[types.UID]*podRequirement{},
	}
}

func (ri *reservationInfo) Clone() *reservationInfo {
	resourceNames := make([]corev1.ResourceName, 0, len(ri.resourceNames))
	for _, v := range ri.resourceNames {
		resourceNames = append(resourceNames, v)
	}

	pods := map[types.UID]*podRequirement{}
	for k, v := range ri.pods {
		pods[k] = &podRequirement{
			namespace: v.namespace,
			name:      v.name,
			uid:       v.uid,
			requests:  v.requests.DeepCopy(),
		}
	}

	return &reservationInfo{
		reservation:   ri.reservation.DeepCopy(),
		resourceNames: resourceNames,
		allocatable:   ri.allocatable.DeepCopy(),
		allocated:     ri.allocated.DeepCopy(),
		pods:          pods,
	}
}

func (ri *reservationInfo) updateReservation(r *schedulingv1alpha1.Reservation) {
	ri.reservation = r.DeepCopy()
	ri.allocatable = reservationutil.ReservationRequests(r)
	ri.resourceNames = quotav1.ResourceNames(ri.allocatable)
	ri.allocated = quotav1.Mask(ri.allocated, ri.resourceNames)
}

func (ri *reservationInfo) addPod(pod *corev1.Pod) {
	requests, _ := resource.PodRequestsAndLimits(pod)
	ri.allocated = quotav1.Add(ri.allocated, quotav1.Mask(requests, ri.resourceNames))
	ri.pods[pod.UID] = &podRequirement{
		namespace: pod.Namespace,
		name:      pod.Name,
		uid:       pod.UID,
		requests:  requests,
	}
}

func (ri *reservationInfo) removePod(pod *corev1.Pod) {
	if requirement, ok := ri.pods[pod.UID]; ok {
		if len(requirement.requests) > 0 {
			ri.allocated = quotav1.SubtractWithNonNegativeResult(ri.allocated, quotav1.Mask(requirement.requests, ri.resourceNames))
		}
		delete(ri.pods, pod.UID)
	}
}
