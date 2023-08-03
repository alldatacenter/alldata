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
	"fmt"
	"sort"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	koordinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

func TestFailedOrSucceededReservation(t *testing.T) {
	fakeClientSet := kubefake.NewSimpleClientset()
	fakeKoordClientSet := koordfake.NewSimpleClientset()
	sharedInformerFactory := informers.NewSharedInformerFactory(fakeClientSet, 0)
	koordSharedInformerFactory := koordinformers.NewSharedInformerFactory(fakeKoordClientSet, 0)

	failedReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "failedReservation",
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase: schedulingv1alpha1.ReservationFailed,
		},
	}
	succededReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "succededReservation",
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase: schedulingv1alpha1.ReservationFailed,
		},
	}
	_, err := fakeKoordClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), failedReservation, metav1.CreateOptions{})
	assert.NoError(t, err)
	_, err = fakeKoordClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), succededReservation, metav1.CreateOptions{})
	assert.NoError(t, err)

	controller := New(sharedInformerFactory, koordSharedInformerFactory, fakeKoordClientSet, 0)

	sharedInformerFactory.Start(nil)
	koordSharedInformerFactory.Start(nil)
	sharedInformerFactory.WaitForCacheSync(nil)
	koordSharedInformerFactory.WaitForCacheSync(nil)

	_, err = controller.sync(failedReservation.Name)
	assert.NoError(t, err)
	_, err = controller.sync(succededReservation.Name)
	assert.NoError(t, err)

	got, err := fakeKoordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), failedReservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	assert.Equal(t, failedReservation, got)

	got, err = fakeKoordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), succededReservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	assert.Equal(t, succededReservation, got)
}

func TestExpireActiveReservation(t *testing.T) {
	fakeClientSet := kubefake.NewSimpleClientset()
	fakeKoordClientSet := koordfake.NewSimpleClientset()
	sharedInformerFactory := informers.NewSharedInformerFactory(fakeClientSet, 0)
	koordSharedInformerFactory := koordinformers.NewSharedInformerFactory(fakeKoordClientSet, 0)

	shouldExpireReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "shouldExpireReservation",
			CreationTimestamp: metav1.Time{
				Time: time.Now().Add(-5 * time.Minute),
			},
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			TTL: &metav1.Duration{
				Duration: 1 * time.Minute,
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	pendingReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "pendingReservation",
			CreationTimestamp: metav1.Time{
				Time: time.Now().Add(-5 * time.Minute),
			},
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			TTL: &metav1.Duration{
				Duration: 1 * time.Minute,
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationPending,
			NodeName: "test-node",
		},
	}
	normalReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:               uuid.NewUUID(),
			Name:              "normalReservation",
			CreationTimestamp: metav1.Now(),
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			TTL: &metav1.Duration{
				Duration: 1 * time.Minute,
			},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
		},
	}
	missingNodeReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "missingNodeReservation",
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "missing-node",
		},
	}

	reservations := []*schedulingv1alpha1.Reservation{
		shouldExpireReservation,
		pendingReservation,
		normalReservation,
		missingNodeReservation,
	}
	for _, v := range reservations {
		_, err := fakeKoordClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), v, metav1.CreateOptions{})
		assert.NoError(t, err)
	}

	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node",
		},
	}
	_, err := fakeClientSet.CoreV1().Nodes().Create(context.TODO(), node, metav1.CreateOptions{})
	assert.NoError(t, err)

	controller := New(sharedInformerFactory, koordSharedInformerFactory, fakeKoordClientSet, 0)

	sharedInformerFactory.Start(nil)
	koordSharedInformerFactory.Start(nil)
	sharedInformerFactory.WaitForCacheSync(nil)
	koordSharedInformerFactory.WaitForCacheSync(nil)

	for _, v := range reservations {
		_, err := controller.sync(v.Name)
		assert.NoError(t, err)
	}

	got, err := fakeKoordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), shouldExpireReservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	assert.True(t, reservationutil.IsReservationExpired(got))

	got, err = fakeKoordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), pendingReservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	assert.True(t, reservationutil.IsReservationExpired(got))

	r, err := controller.sync(normalReservation.Name)
	assert.NoError(t, err)
	assert.Equal(t, maxRetryAfterTime, r.requeueAfter)

	got, err = fakeKoordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), normalReservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	assert.Equal(t, normalReservation, got)

	got, err = fakeKoordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), missingNodeReservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	assert.True(t, reservationutil.IsReservationExpired(got))
}

func TestSyncStatus(t *testing.T) {
	fakeClientSet := kubefake.NewSimpleClientset()
	fakeKoordClientSet := koordfake.NewSimpleClientset()
	sharedInformerFactory := informers.NewSharedInformerFactory(fakeClientSet, 0)
	koordSharedInformerFactory := koordinformers.NewSharedInformerFactory(fakeKoordClientSet, 0)

	reservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:               uuid.NewUUID(),
			Name:              "normalReservation",
			CreationTimestamp: metav1.Now(),
		},
		Spec: schedulingv1alpha1.ReservationSpec{
			TTL: &metav1.Duration{
				Duration: 1 * time.Minute,
			},
			AllocateOnce: true,
			Template:     &corev1.PodTemplateSpec{},
		},
		Status: schedulingv1alpha1.ReservationStatus{
			Phase:    schedulingv1alpha1.ReservationAvailable,
			NodeName: "test-node",
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("10000m"),
				corev1.ResourceMemory: resource.MustParse("10Gi"),
			},
		},
	}

	_, err := fakeKoordClientSet.SchedulingV1alpha1().Reservations().Create(context.TODO(), reservation, metav1.CreateOptions{})
	assert.NoError(t, err)

	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node",
		},
	}
	_, err = fakeClientSet.CoreV1().Nodes().Create(context.TODO(), node, metav1.CreateOptions{})
	assert.NoError(t, err)

	var owners []corev1.ObjectReference
	for i := 0; i < 4; i++ {
		owners = append(owners, corev1.ObjectReference{
			UID:       types.UID(fmt.Sprintf("%d", i)),
			Namespace: "default",
			Name:      fmt.Sprintf("pod-%d", i),
		})
		pod := &corev1.Pod{
			ObjectMeta: metav1.ObjectMeta{
				UID:       types.UID(fmt.Sprintf("%d", i)),
				Namespace: "default",
				Name:      fmt.Sprintf("pod-%d", i),
			},
			Spec: corev1.PodSpec{
				NodeName: node.Name,
				Containers: []corev1.Container{
					{
						Resources: corev1.ResourceRequirements{
							Requests: corev1.ResourceList{
								corev1.ResourceCPU:              resource.MustParse("2000m"),
								corev1.ResourceMemory:           resource.MustParse("2Gi"),
								corev1.ResourceEphemeralStorage: resource.MustParse("10Gi"),
							},
						},
					},
				},
			},
		}
		apiext.SetReservationAllocated(pod, reservation)
		_, err := fakeClientSet.CoreV1().Pods(pod.Namespace).Create(context.TODO(), pod, metav1.CreateOptions{})
		assert.NoError(t, err)
	}

	controller := New(sharedInformerFactory, koordSharedInformerFactory, fakeKoordClientSet, 0)
	controller.Start()

	time.Sleep(1 * time.Second)

	r, err := controller.sync(reservation.Name)
	assert.NoError(t, err)
	assert.Equal(t, time.Duration(0), r.requeueAfter)

	got, err := fakeKoordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), reservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)

	expectReservation := reservation.DeepCopy()
	reservationutil.SetReservationSucceeded(expectReservation)
	expectReservation.Status.Allocated = corev1.ResourceList{
		corev1.ResourceCPU:    resource.MustParse("8000m"),
		corev1.ResourceMemory: resource.MustParse("8Gi"),
	}
	sort.Slice(owners, func(i, j int) bool {
		return owners[i].UID < owners[j].UID
	})
	expectReservation.Status.CurrentOwners = owners
	for i := range expectReservation.Status.Conditions {
		cond := &expectReservation.Status.Conditions[i]
		cond.LastProbeTime = metav1.Time{}
		cond.LastTransitionTime = metav1.Time{}
	}
	for i := range got.Status.Conditions {
		cond := &got.Status.Conditions[i]
		cond.LastProbeTime = metav1.Time{}
		cond.LastTransitionTime = metav1.Time{}
	}
	assert.Equal(t, expectReservation, got)
}
