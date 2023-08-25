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

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

const (
	DefaultCreator = "koord-descheduler"
	LabelCreatedBy = "app.kubernetes.io/created-by"
)

var NewInterpreter = newInterpreter

type Interpreter interface {
	GetReservationType() client.Object
	Preemption() Preemption
	CreateReservation(ctx context.Context, job *sev1alpha1.PodMigrationJob) (Object, error)
	GetReservation(ctx context.Context, reservationRef *corev1.ObjectReference) (Object, error)
	DeleteReservation(ctx context.Context, reservationRef *corev1.ObjectReference) error
}

type Preemption interface {
	Preempt(ctx context.Context, job *sev1alpha1.PodMigrationJob, reservationObj Object) (bool, reconcile.Result, error)
}

type Object interface {
	metav1.Object
	runtime.Object
	String() string
	OriginObject() client.Object
	GetReservationConditions() []sev1alpha1.ReservationCondition
	QueryPreemptedPodsRefs() []corev1.ObjectReference
	GetBoundPod() *corev1.ObjectReference
	GetReservationOwners() []sev1alpha1.ReservationOwner
	GetScheduledNodeName() string
	GetPhase() sev1alpha1.ReservationPhase
	NeedPreemption() bool
}
