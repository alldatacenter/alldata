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
	"sort"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/uuid"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func TestCacheUpdateReservation(t *testing.T) {
	cache := newReservationCache(nil)
	reservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "test-reservation",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			NodeName: "test-node-1",
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("4"),
				corev1.ResourceMemory: resource.MustParse("4Gi"),
			},
			Allocated: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("2"),
				corev1.ResourceMemory: resource.MustParse("2Gi"),
			},
		},
	}
	cache.updateReservation(reservation)
	reservationInfos := cache.listReservationInfosOnNode(reservation.Status.NodeName)
	assert.Len(t, reservationInfos, 1)
	rInfo := reservationInfos[0]
	expectReservationInfo := &reservationInfo{
		reservation: reservation,
		resourceNames: []corev1.ResourceName{
			corev1.ResourceCPU,
			corev1.ResourceMemory,
		},
		allocatable: corev1.ResourceList{
			corev1.ResourceCPU:    resource.MustParse("4"),
			corev1.ResourceMemory: resource.MustParse("4Gi"),
		},
		allocated: nil,
		pods:      map[types.UID]*podRequirement{},
	}
	sort.Slice(rInfo.resourceNames, func(i, j int) bool {
		return rInfo.resourceNames[i] < rInfo.resourceNames[j]
	})
	assert.Equal(t, expectReservationInfo, rInfo)

	cache.updateReservation(reservation)
	reservationInfos = cache.listReservationInfosOnNode(reservation.Status.NodeName)
	assert.Len(t, reservationInfos, 1)
	rInfo = reservationInfos[0]
	expectReservationInfo.allocated = corev1.ResourceList{}
	sort.Slice(rInfo.resourceNames, func(i, j int) bool {
		return rInfo.resourceNames[i] < rInfo.resourceNames[j]
	})
	assert.Equal(t, expectReservationInfo, rInfo)
}

func TestCacheDeleteReservation(t *testing.T) {
	cache := newReservationCache(nil)
	reservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "test-reservation",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			NodeName: "test-node-1",
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("4"),
				corev1.ResourceMemory: resource.MustParse("4Gi"),
			},
			Allocated: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("2"),
				corev1.ResourceMemory: resource.MustParse("2Gi"),
			},
		},
	}
	cache.updateReservation(reservation)

	rInfo := cache.getReservationInfoByUID(reservation.UID)
	assert.NotNil(t, rInfo)

	expectReservationInfo := &reservationInfo{
		reservation: reservation,
		resourceNames: []corev1.ResourceName{
			corev1.ResourceCPU,
			corev1.ResourceMemory,
		},
		allocatable: corev1.ResourceList{
			corev1.ResourceCPU:    resource.MustParse("4"),
			corev1.ResourceMemory: resource.MustParse("4Gi"),
		},
		allocated: nil,
		pods:      map[types.UID]*podRequirement{},
	}
	sort.Slice(rInfo.resourceNames, func(i, j int) bool {
		return rInfo.resourceNames[i] < rInfo.resourceNames[j]
	})
	assert.Equal(t, expectReservationInfo, rInfo)

	cache.deleteReservation(reservation)
	rInfo = cache.getReservationInfoByUID(reservation.UID)
	assert.Nil(t, rInfo)
}

func TestCacheAddOrUpdateOrDeletePod(t *testing.T) {
	cache := newReservationCache(nil)
	reservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "test-reservation",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("4000m"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			NodeName: "test-node-1",
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("4000m"),
				corev1.ResourceMemory: resource.MustParse("4Gi"),
			},
			Allocated: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("2000m"),
				corev1.ResourceMemory: resource.MustParse("2Gi"),
			},
		},
	}
	cache.updateReservation(reservation)

	rInfo := cache.getReservationInfoByUID(reservation.UID)
	assert.NotNil(t, rInfo)

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			UID:       uuid.NewUUID(),
			Namespace: "default",
			Name:      "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("2000m"),
							corev1.ResourceMemory: resource.MustParse("2Gi"),
						},
					},
				},
			},
		},
	}

	cache.addPod(reservation.UID, pod)

	rInfo = cache.getReservationInfoByUID(reservation.UID)
	sort.Slice(rInfo.resourceNames, func(i, j int) bool {
		return rInfo.resourceNames[i] < rInfo.resourceNames[j]
	})
	expectReservationInfo := &reservationInfo{
		reservation: reservation,
		resourceNames: []corev1.ResourceName{
			corev1.ResourceCPU,
			corev1.ResourceMemory,
		},
		allocatable: corev1.ResourceList{
			corev1.ResourceCPU:    resource.MustParse("4000m"),
			corev1.ResourceMemory: resource.MustParse("4Gi"),
		},
		allocated: corev1.ResourceList{
			corev1.ResourceCPU:    resource.MustParse("2000m"),
			corev1.ResourceMemory: resource.MustParse("2Gi"),
		},
		pods: map[types.UID]*podRequirement{
			pod.UID: {
				namespace: pod.Namespace,
				name:      pod.Name,
				uid:       pod.UID,
				requests: corev1.ResourceList{
					corev1.ResourceCPU:    resource.MustParse("2000m"),
					corev1.ResourceMemory: resource.MustParse("2Gi"),
				},
			},
		},
	}
	assert.Equal(t, expectReservationInfo, rInfo)

	cache.updatePod(reservation.UID, pod, pod)
	rInfo = cache.getReservationInfoByUID(reservation.UID)
	sort.Slice(rInfo.resourceNames, func(i, j int) bool {
		return rInfo.resourceNames[i] < rInfo.resourceNames[j]
	})
	expectReservationInfo.allocated = quotav1.SubtractWithNonNegativeResult(expectReservationInfo.allocated, corev1.ResourceList{
		corev1.ResourceCPU:    resource.MustParse("0"),
		corev1.ResourceMemory: resource.MustParse("0"),
	})
	assert.Equal(t, expectReservationInfo, rInfo)

	cache.deletePod(reservation.UID, pod)
	rInfo = cache.getReservationInfoByUID(reservation.UID)
	sort.Slice(rInfo.resourceNames, func(i, j int) bool {
		return rInfo.resourceNames[i] < rInfo.resourceNames[j]
	})
	expectReservationInfo.allocated = corev1.ResourceList{
		corev1.ResourceCPU:    resource.MustParse("0"),
		corev1.ResourceMemory: resource.MustParse("0"),
	}
	expectReservationInfo.pods = map[types.UID]*podRequirement{}
	assert.Equal(t, expectReservationInfo, rInfo)
}
