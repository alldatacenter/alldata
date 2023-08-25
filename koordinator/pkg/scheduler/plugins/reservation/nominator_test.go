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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

func TestNominateReservation(t *testing.T) {
	reservation4C8G := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reservation4C8G",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	reservation2C4G := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "reservation2C4G",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Template: &corev1.PodTemplateSpec{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("2"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	tests := []struct {
		name            string
		pod             *corev1.Pod
		reservations    []*schedulingv1alpha1.Reservation
		allocated       map[types.UID]corev1.ResourceList
		wantReservation *schedulingv1alpha1.Reservation
		wantStatus      bool
	}{
		{
			name: "reserve pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Annotations: map[string]string{
						reservationutil.AnnotationReservePod: "true",
					},
				},
			},
			wantStatus: true,
		},
		{
			name:       "node without reservations",
			pod:        &corev1.Pod{},
			wantStatus: true,
		},
		{
			name: "preferred reservation",
			pod:  &corev1.Pod{},
			reservations: []*schedulingv1alpha1.Reservation{
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "preferred-reservation",
						Labels: map[string]string{
							apiext.LabelReservationOrder: "100",
						},
					},
					Spec: schedulingv1alpha1.ReservationSpec{
						Template: &corev1.PodTemplateSpec{},
					},
					Status: schedulingv1alpha1.ReservationStatus{
						NodeName: "test-node",
					},
				},
				{
					ObjectMeta: metav1.ObjectMeta{
						Name: "normal-reservation",
					},
					Spec: schedulingv1alpha1.ReservationSpec{
						Template: &corev1.PodTemplateSpec{},
					},
					Status: schedulingv1alpha1.ReservationStatus{
						NodeName: "test-node",
					},
				},
			},
			wantReservation: &schedulingv1alpha1.Reservation{
				ObjectMeta: metav1.ObjectMeta{
					Name: "preferred-reservation",
					Labels: map[string]string{
						apiext.LabelReservationOrder: "100",
					},
				},
				Spec: schedulingv1alpha1.ReservationSpec{
					Template: &corev1.PodTemplateSpec{},
				},
				Status: schedulingv1alpha1.ReservationStatus{
					NodeName: "test-node",
				},
			},
			wantStatus: true,
		},
		{
			name: "allocated reservation",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("2"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			},
			reservations: []*schedulingv1alpha1.Reservation{
				reservation4C8G,
				reservation2C4G,
			},
			allocated: map[types.UID]corev1.ResourceList{
				reservation2C4G.UID: {
					corev1.ResourceCPU:    resource.MustParse("2"),
					corev1.ResourceMemory: resource.MustParse("4Gi"),
				},
			},
			wantStatus:      true,
			wantReservation: reservation4C8G,
		},
		{
			name: "matched reservations",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("2"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
			},
			reservations: []*schedulingv1alpha1.Reservation{
				reservation4C8G,
				reservation2C4G,
			},
			wantStatus:      true,
			wantReservation: reservation2C4G,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			suit := newPluginTestSuit(t)
			plugin, err := suit.pluginFactory()
			assert.NoError(t, err)
			pl := plugin.(*Plugin)
			cycleState := framework.NewCycleState()
			state := &stateData{
				matched: map[string][]*reservationInfo{},
			}
			for _, reservation := range tt.reservations {
				rInfo := newReservationInfo(reservation)
				if allocated := tt.allocated[reservation.UID]; len(allocated) > 0 {
					rInfo.allocated = allocated
				}
				state.matched[reservation.Status.NodeName] = append(state.matched[reservation.Status.NodeName], rInfo)
				pl.reservationCache.updateReservation(reservation)
			}
			cycleState.Write(stateKey, state)
			reservation, status := pl.NominateReservation(context.TODO(), cycleState, tt.pod, "test-node")
			assert.Equal(t, tt.wantReservation, reservation)
			assert.Equal(t, tt.wantStatus, status.IsSuccess())
		})
	}
}
