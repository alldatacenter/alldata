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
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	koordinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
)

func TestGC(t *testing.T) {
	fakeClientSet := kubefake.NewSimpleClientset()
	fakeKoordClientSet := koordfake.NewSimpleClientset()
	sharedInformerFactory := informers.NewSharedInformerFactory(fakeClientSet, 0)
	koordSharedInformerFactory := koordinformers.NewSharedInformerFactory(fakeKoordClientSet, 0)

	shouldExpireReservation := &schedulingv1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			UID:  uuid.NewUUID(),
			Name: "shouldExpireReservation",
			CreationTimestamp: metav1.Time{
				Time: time.Now().Add(-48 * time.Hour),
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
	time.Sleep(1 * time.Second)
	controller.gcReservations()

	reservationList, err := fakeKoordClientSet.SchedulingV1alpha1().Reservations().List(context.TODO(), metav1.ListOptions{})
	assert.NoError(t, err)
	assert.Len(t, reservationList.Items, 2)

	expiredReservation, err := fakeKoordClientSet.SchedulingV1alpha1().Reservations().Get(context.TODO(), shouldExpireReservation.Name, metav1.GetOptions{})
	assert.NoError(t, err)
	expiredReservation = expiredReservation.DeepCopy()
	for i := range expiredReservation.Status.Conditions {
		cond := &expiredReservation.Status.Conditions[i]
		if cond.Reason == schedulingv1alpha1.ReasonReservationExpired {
			cond.LastProbeTime = metav1.Time{Time: metav1.Now().Add(-48 * time.Hour)}
			cond.LastTransitionTime = metav1.Time{Time: metav1.Now().Add(-48 * time.Hour)}
		}
	}
	_, err = fakeKoordClientSet.SchedulingV1alpha1().Reservations().UpdateStatus(context.TODO(), expiredReservation, metav1.UpdateOptions{})
	assert.NoError(t, err)

	time.Sleep(1 * time.Second)
	controller.gcReservations()
	reservationList, err = fakeKoordClientSet.SchedulingV1alpha1().Reservations().List(context.TODO(), metav1.ListOptions{})
	assert.NoError(t, err)
	assert.Len(t, reservationList.Items, 1)
	assert.Equal(t, normalReservation, &reservationList.Items[0])
}
