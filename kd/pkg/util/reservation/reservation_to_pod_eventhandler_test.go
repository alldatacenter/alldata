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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/tools/cache"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func TestIsObjValidActiveReservation(t *testing.T) {
	tests := []struct {
		name string
		arg  interface{}
		want bool
	}{
		{
			name: "valid and active",
			arg: &schedulingv1alpha1.Reservation{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-0",
				},
				Spec: schedulingv1alpha1.ReservationSpec{
					Template: &corev1.PodTemplateSpec{
						ObjectMeta: metav1.ObjectMeta{
							Name: "reserve-pod-0",
						},
					},
					Owners: []schedulingv1alpha1.ReservationOwner{
						{
							Object: &corev1.ObjectReference{
								Kind: "Pod",
								Name: "test-pod-0",
							},
						},
					},
					TTL: &metav1.Duration{Duration: 30 * time.Minute},
				},
				Status: schedulingv1alpha1.ReservationStatus{
					Phase:    schedulingv1alpha1.ReservationAvailable,
					NodeName: "test-node-0",
				},
			},
			want: true,
		},
		{
			name: "valid but not active",
			arg: &schedulingv1alpha1.Reservation{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-0",
				},
				Spec: schedulingv1alpha1.ReservationSpec{
					Template: &corev1.PodTemplateSpec{
						ObjectMeta: metav1.ObjectMeta{
							Name: "reserve-pod-0",
						},
					},
					Owners: []schedulingv1alpha1.ReservationOwner{
						{
							Object: &corev1.ObjectReference{
								Kind: "Pod",
								Name: "test-pod-0",
							},
						},
					},
					TTL: &metav1.Duration{Duration: 30 * time.Minute},
				},
				Status: schedulingv1alpha1.ReservationStatus{
					Phase:    schedulingv1alpha1.ReservationSucceeded,
					NodeName: "test-node-0",
				},
			},
			want: false,
		},
		{
			name: "invalid",
			arg: &schedulingv1alpha1.Reservation{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-0",
				},
				Spec: schedulingv1alpha1.ReservationSpec{
					Owners: []schedulingv1alpha1.ReservationOwner{
						{
							Object: &corev1.ObjectReference{
								Kind: "Pod",
								Name: "test-pod-0",
							},
						},
					},
					TTL: &metav1.Duration{Duration: 30 * time.Minute},
				},
				Status: schedulingv1alpha1.ReservationStatus{
					Phase: schedulingv1alpha1.ReservationPending,
				},
			},
			want: false,
		},
		{
			name: "invalid 1",
			arg: &schedulingv1alpha1.Reservation{
				ObjectMeta: metav1.ObjectMeta{
					Name: "reserve-pod-0",
				},
				Spec: schedulingv1alpha1.ReservationSpec{
					Template: &corev1.PodTemplateSpec{
						ObjectMeta: metav1.ObjectMeta{
							Name: "reserve-pod-0",
						},
					},
					TTL: &metav1.Duration{Duration: 30 * time.Minute},
				},
				Status: schedulingv1alpha1.ReservationStatus{
					Phase: schedulingv1alpha1.ReservationPending,
				},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := IsObjValidActiveReservation(tt.arg)
			assert.Equal(t, tt.want, got)
		})
	}
}

var _ cache.ResourceEventHandler = &fakePodHandler{}

type fakePodHandler struct {
	t *testing.T
}

func (f *fakePodHandler) OnAdd(obj interface{}) {
	_, ok := obj.(*corev1.Pod)
	if !ok {
		f.t.Errorf("OnAdd got object %T, but not a pod", obj)
	}
}

func (f *fakePodHandler) OnUpdate(oldObj, newObj interface{}) {
	_, ok := oldObj.(*corev1.Pod)
	if !ok {
		f.t.Errorf("OnUpdate got old object %T, but not a pod", oldObj)
	}
	_, ok = newObj.(*corev1.Pod)
	if !ok {
		f.t.Errorf("OnUpdate got new object %T, but not a pod", newObj)
	}
}

func (f *fakePodHandler) OnDelete(obj interface{}) {
	_, ok := obj.(*corev1.Pod)
	if !ok {
		f.t.Errorf("OnDelete got object %T, but not a pod", obj)
	}
}

func TestReservationToPodEventHandler(t *testing.T) {
	testReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "reserve-0",
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			Owners: []schedulingv1alpha1.ReservationOwner{
				{
					Object: &corev1.ObjectReference{
						Kind: "Pod",
						Name: "test-pod-0",
					},
				},
			},
			TTL: &metav1.Duration{Duration: 30 * time.Minute},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase: schedulingv1alpha1.ReservationPending,
			Conditions: []schedulingv1alpha1.ReservationCondition{
				{
					Type:               schedulingv1alpha1.ReservationConditionScheduled,
					Status:             schedulingv1alpha1.ConditionStatusTrue,
					Reason:             schedulingv1alpha1.ReasonReservationScheduled,
					LastProbeTime:      metav1.Now(),
					LastTransitionTime: metav1.Now(),
				},
			},
		},
	}
	testDeletedFinalStateUnknown := cache.DeletedFinalStateUnknown{
		Key: "reserve-0",
		Obj: testReservation,
	}
	t.Run("test not panic", func(t *testing.T) {
		h := NewReservationToPodEventHandler(
			&fakePodHandler{t: t},
			func(obj interface{}) bool {
				return true
			},
		)

		h.OnAdd(testReservation)

		h.OnUpdate(testReservation, testReservation)

		h.OnDelete(testReservation)
		h.OnDelete(testDeletedFinalStateUnknown)

		h = NewReservationToPodEventHandler(
			&fakePodHandler{t: t},
			func(obj interface{}) bool {
				return false
			},
		)

		h.OnAdd(testReservation)
		h.OnUpdate(testReservation, testReservation)
		h.OnDelete(testReservation)
	})
}
