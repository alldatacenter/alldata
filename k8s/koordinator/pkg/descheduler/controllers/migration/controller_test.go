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

package migration

import (
	"context"
	"fmt"
	"testing"
	"time"

	appsv1alpha1 "github.com/openkruise/kruise-api/apps/v1alpha1"
	appsv1beta1 "github.com/openkruise/kruise-api/apps/v1beta1"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/labels"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/clock"
	"k8s.io/apimachinery/pkg/util/intstr"
	"k8s.io/apimachinery/pkg/util/uuid"
	clientgoscheme "k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/tools/record"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"

	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config/v1alpha2"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/migration/controllerfinder"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/migration/reservation"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/controllers/migration/util"
	evictionsutil "github.com/koordinator-sh/koordinator/pkg/descheduler/evictions"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	podutil "github.com/koordinator-sh/koordinator/pkg/descheduler/pod"
)

type fakeEvictionInterpreter struct {
	err error
}

func (f fakeEvictionInterpreter) Evict(ctx context.Context, job *sev1alpha1.PodMigrationJob, pod *corev1.Pod) error {
	return f.err
}

type fakeReservationInterpreter struct {
	createErr   error
	getErr      error
	deleteErr   error
	reservation *sev1alpha1.Reservation
}

func (f fakeReservationInterpreter) GetReservationType() client.Object {
	return &sev1alpha1.Reservation{}
}

func (f fakeReservationInterpreter) Preemption() reservation.Preemption {
	return nil
}

func (f fakeReservationInterpreter) CreateReservation(ctx context.Context, job *sev1alpha1.PodMigrationJob) (reservation.Object, error) {
	if f.createErr != nil {
		return nil, f.createErr
	}
	return reservation.NewReservation(f.reservation), nil
}

func (f fakeReservationInterpreter) GetReservation(ctx context.Context, reservationRef *corev1.ObjectReference) (reservation.Object, error) {
	if f.getErr != nil {
		return nil, f.getErr
	}
	return reservation.NewReservation(f.reservation), nil
}

func (f fakeReservationInterpreter) DeleteReservation(ctx context.Context, reservationRef *corev1.ObjectReference) error {
	return f.deleteErr
}

type fakeControllerFinder struct {
	pods     []*corev1.Pod
	replicas int32
	err      error
}

func (f *fakeControllerFinder) GetPodsForRef(apiVersion, kind, name, ns string, labelSelector *metav1.LabelSelector, active bool) ([]*corev1.Pod, int32, error) {
	return f.pods, f.replicas, f.err
}

func (f *fakeControllerFinder) GetExpectedScaleForPods(pods []*corev1.Pod) (int32, error) {
	return f.replicas, f.err
}

func newTestReconciler() *Reconciler {
	scheme := runtime.NewScheme()
	_ = sev1alpha1.AddToScheme(scheme)
	_ = clientgoscheme.AddToScheme(scheme)
	_ = appsv1alpha1.AddToScheme(scheme)
	_ = appsv1beta1.AddToScheme(scheme)

	var v1beta2args v1alpha2.MigrationControllerArgs
	v1alpha2.SetDefaults_MigrationControllerArgs(&v1beta2args)
	var args deschedulerconfig.MigrationControllerArgs
	err := v1alpha2.Convert_v1alpha2_MigrationControllerArgs_To_config_MigrationControllerArgs(&v1beta2args, &args, nil)
	if err != nil {
		panic(err)
	}

	runtimeClient := fake.NewClientBuilder().WithScheme(scheme).Build()
	eventBroadcaster := record.NewBroadcaster()
	recorder := eventBroadcaster.NewRecorder(scheme, corev1.EventSource{Component: Name})

	nodesGetter := func() ([]*corev1.Node, error) {
		var nodeList corev1.NodeList
		err := runtimeClient.List(context.TODO(), &nodeList)
		if err != nil {
			return nil, err
		}
		r := make([]*corev1.Node, 0, len(nodeList.Items))
		for i := range nodeList.Items {
			r = append(r, &nodeList.Items[i])
		}
		return r, nil
	}

	evictorFilter := evictionsutil.NewEvictorFilter(
		nodesGetter,
		func(s string, filterFunc framework.FilterFunc) ([]*corev1.Pod, error) {
			return nil, fmt.Errorf("implement it")
		},
		false, false, false, false,
	)

	podFilter, err := podutil.NewOptions().
		WithFilter(evictorFilter.Filter).
		BuildFilterFunc()
	if err != nil {
		panic(err)
	}

	controllerFinder := &controllerfinder.ControllerFinder{Client: runtimeClient}
	r := &Reconciler{
		Client:                 runtimeClient,
		args:                   &args,
		eventRecorder:          record.NewEventRecorderAdapter(recorder),
		reservationInterpreter: nil,
		evictorInterpreter:     nil,
		controllerFinder:       controllerFinder,
		unretriablePodFilter:   podFilter,
		assumedCache:           newAssumedCache(),
		clock:                  clock.RealClock{},
	}

	return r
}

func TestAbortJobIfTimeout(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))

	timeout, err := reconciler.abortJobIfTimeout(context.TODO(), job)
	assert.False(t, timeout)
	assert.Nil(t, err)

	job.Spec.TTL = &metav1.Duration{Duration: 30 * time.Minute}
	timeout, err = reconciler.abortJobIfTimeout(context.TODO(), job)
	assert.False(t, timeout)
	assert.Nil(t, err)

	reconciler.clock = clock.NewFakeClock(time.Now().Add(60 * time.Minute))
	timeout, err = reconciler.abortJobIfTimeout(context.TODO(), job)
	assert.True(t, timeout)
	assert.Nil(t, err)
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonTimeout, job.Status.Reason)
}

func TestAbortJobByMissingPod(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))

	assert.Nil(t, reconciler.abortJobByMissingPod(context.TODO(), job, types.NamespacedName{Namespace: "default", Name: "test-pod"}))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonMissingPod, job.Status.Reason)
}

func TestAbortJobByMissingReservation(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
			ReservationOptions: &sev1alpha1.PodMigrateReservationOptions{
				ReservationRef: &corev1.ObjectReference{
					Name: "test-reservation",
				},
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))

	assert.Nil(t, reconciler.abortJobByMissingReservation(context.TODO(), job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonMissingReservation, job.Status.Reason)
}

func TestAbortJobByInvalidReservation(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))
	assert.Nil(t, reconciler.abortJobByReservationBound(context.TODO(), job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonForbiddenMigratePod, job.Status.Reason)
}

func TestAbortJobByReservationUnschedulable(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))

	reservationObj := reservation.NewReservation(&sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Status: sev1alpha1.ReservationStatus{
			Conditions: []sev1alpha1.ReservationCondition{
				{
					Reason:  string(corev1.PodScheduled),
					Message: "Reservation is unschedulable",
				},
			},
		},
	})
	assert.Nil(t, reconciler.abortJobByReservationUnschedulable(context.TODO(), job, reservationObj))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonUnschedulable, job.Status.Reason)
}

func TestHandleScheduleFailed(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))

	reservationObj := reservation.NewReservation(&sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Status: sev1alpha1.ReservationStatus{
			Phase: sev1alpha1.ReservationFailed,
			Conditions: []sev1alpha1.ReservationCondition{
				{
					Type:    sev1alpha1.ReservationConditionScheduled,
					Reason:  sev1alpha1.ReasonReservationUnschedulable,
					Message: "Reservation is unschedulable",
				},
			},
		},
	})
	assert.Nil(t, reconciler.syncReservationScheduleFailed(context.TODO(), job, reservationObj))
	_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionReservationScheduled)
	assert.NotNil(t, cond)
	expectCond := &sev1alpha1.PodMigrationJobCondition{
		Type:               sev1alpha1.PodMigrationJobConditionReservationScheduled,
		Status:             sev1alpha1.PodMigrationJobConditionStatusFalse,
		Reason:             sev1alpha1.PodMigrationJobReasonUnschedulable,
		Message:            "Reservation is unschedulable",
		LastTransitionTime: cond.LastTransitionTime,
	}
	assert.Equal(t, expectCond, cond)
}

func TestHandleScheduleSuccess(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))

	reservationObj := reservation.NewReservation(&sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Status: sev1alpha1.ReservationStatus{
			NodeName: "test-node",
		},
	})
	assert.Nil(t, reconciler.prepareJobWithReservationScheduleSuccess(context.TODO(), job, reservationObj))
	_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionReservationScheduled)
	assert.NotNil(t, cond)
	expectCond := &sev1alpha1.PodMigrationJobCondition{
		Type:               sev1alpha1.PodMigrationJobConditionReservationScheduled,
		Status:             sev1alpha1.PodMigrationJobConditionStatusTrue,
		LastTransitionTime: cond.LastTransitionTime,
	}
	assert.Equal(t, expectCond, cond)
	assert.Equal(t, "test-node", job.Status.NodeName)
}

func TestWaitForPodBindReservation(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
		Status: sev1alpha1.PodMigrationJobStatus{
			Conditions: []sev1alpha1.PodMigrationJobCondition{
				{
					Type:   sev1alpha1.PodMigrationJobConditionReservationPodBoundReservation,
					Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
				},
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))

	reservationObj := reservation.NewReservation(&sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
	})

	bound, result, err := reconciler.waitForPodBindReservation(context.TODO(), job, reservationObj)
	assert.True(t, bound)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Nil(t, err)

	job.Status.Conditions = nil
	bound, result, err = reconciler.waitForPodBindReservation(context.TODO(), job, reservationObj)
	assert.False(t, bound)
	assert.Equal(t, reconcile.Result{RequeueAfter: defaultRequeueAfter}, result)
	assert.Nil(t, err)

	_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionReservationPodBoundReservation)
	assert.NotNil(t, cond)
	expectCond := &sev1alpha1.PodMigrationJobCondition{
		Type:               sev1alpha1.PodMigrationJobConditionReservationPodBoundReservation,
		Status:             sev1alpha1.PodMigrationJobConditionStatusFalse,
		Reason:             sev1alpha1.PodMigrationJobReasonWaitForPodBindReservation,
		LastTransitionTime: cond.LastTransitionTime,
	}
	assert.Equal(t, expectCond, cond)

	reservationObj = reservation.NewReservation(&sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Status: sev1alpha1.ReservationStatus{
			CurrentOwners: []corev1.ObjectReference{
				{
					Namespace: "default",
					Name:      "newly-create-test-pod",
				},
			},
		},
	})
	bound, result, err = reconciler.waitForPodBindReservation(context.TODO(), job, reservationObj)
	assert.True(t, bound)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Nil(t, err)
}

func TestEvictPodDirectly(t *testing.T) {
	reconciler := newTestReconciler()

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
		Status: sev1alpha1.PodMigrationJobStatus{
			Conditions: []sev1alpha1.PodMigrationJobCondition{
				{
					Type:   sev1alpha1.PodMigrationJobConditionEviction,
					Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
				},
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))
	result, err := reconciler.evictPodDirectly(context.TODO(), job)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Nil(t, err)
	assert.Equal(t, sev1alpha1.PodMigrationJobSucceeded, job.Status.Phase)
	assert.Equal(t, "Complete", job.Status.Status)
	assert.Equal(t, "", job.Status.Reason)
}

func TestEvictPod(t *testing.T) {
	reconciler := newTestReconciler()

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
		Status: sev1alpha1.PodMigrationJobStatus{
			Conditions: []sev1alpha1.PodMigrationJobCondition{
				{
					Type:   sev1alpha1.PodMigrationJobConditionEviction,
					Status: sev1alpha1.PodMigrationJobConditionStatusTrue,
				},
			},
		},
	}
	assert.Nil(t, reconciler.Create(context.TODO(), job))

	evicted, result, err := reconciler.evictPod(context.TODO(), job)
	assert.True(t, evicted)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Nil(t, err)

	job.Status.Conditions = nil
	job.Status.Status = string(sev1alpha1.PodMigrationJobConditionReservationScheduled)
	evicted, result, err = reconciler.evictPod(context.TODO(), job)
	assert.False(t, evicted)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Nil(t, err)
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonMissingPod, job.Status.Reason)

	job.Status = sev1alpha1.PodMigrationJobStatus{}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	expectErr := fmt.Errorf("must return error")
	reconciler.evictorInterpreter = fakeEvictionInterpreter{expectErr}
	evicted, result, err = reconciler.evictPod(context.TODO(), job)
	assert.False(t, evicted)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Equal(t, expectErr, err)

	reconciler.evictorInterpreter = fakeEvictionInterpreter{}
	evicted, result, err = reconciler.evictPod(context.TODO(), job)
	assert.False(t, evicted)
	assert.Equal(t, reconcile.Result{RequeueAfter: defaultRequeueAfter}, result)
	assert.Nil(t, err)

	_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionEviction)
	assert.NotNil(t, cond)

	expectCond := &sev1alpha1.PodMigrationJobCondition{
		Type:               sev1alpha1.PodMigrationJobConditionEviction,
		Status:             sev1alpha1.PodMigrationJobConditionStatusFalse,
		Reason:             sev1alpha1.PodMigrationJobReasonEvicting,
		Message:            cond.Message,
		LastTransitionTime: cond.LastTransitionTime,
	}
	assert.Equal(t, expectCond, cond)

	assert.Nil(t, reconciler.Client.Delete(context.TODO(), pod))
	evicted, result, err = reconciler.evictPod(context.TODO(), job)
	assert.True(t, evicted)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Nil(t, err)

	_, cond = util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionEviction)
	assert.NotNil(t, cond)

	expectCond = &sev1alpha1.PodMigrationJobCondition{
		Type:               sev1alpha1.PodMigrationJobConditionEviction,
		Status:             sev1alpha1.PodMigrationJobConditionStatusTrue,
		Reason:             sev1alpha1.PodMigrationJobReasonEvictComplete,
		LastTransitionTime: cond.LastTransitionTime,
	}
	assert.Equal(t, expectCond, cond)
}

func TestDeleteReservation(t *testing.T) {
	reconciler := newTestReconciler()
	assert.Nil(t, reconciler.deleteReservation(context.TODO(), &sev1alpha1.PodMigrationJob{}))

	reconciler.reservationInterpreter = fakeReservationInterpreter{
		deleteErr: fmt.Errorf("must return delete error"),
	}
	assert.NotNil(t, reconciler.deleteReservation(context.TODO(), &sev1alpha1.PodMigrationJob{
		Spec: sev1alpha1.PodMigrationJobSpec{
			ReservationOptions: &sev1alpha1.PodMigrateReservationOptions{
				ReservationRef: &corev1.ObjectReference{
					Name: "test-reservation",
				},
			},
		},
	}))
}

func TestCreateReservation(t *testing.T) {
	reconciler := newTestReconciler()

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	assert.NotNil(t, reconciler.createReservation(context.TODO(), job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonMissingPod, job.Status.Reason)

	job.Status = sev1alpha1.PodMigrationJobStatus{}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	reconciler.reservationInterpreter = fakeReservationInterpreter{
		createErr: fmt.Errorf("must return create error"),
	}
	assert.NotNil(t, reconciler.createReservation(context.TODO(), job))
	_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionReservationCreated)
	assert.NotNil(t, cond)

	expectCond := &sev1alpha1.PodMigrationJobCondition{
		Type:               sev1alpha1.PodMigrationJobConditionReservationCreated,
		Status:             sev1alpha1.PodMigrationJobConditionStatusFalse,
		Reason:             sev1alpha1.PodMigrationJobReasonFailedCreateReservation,
		Message:            cond.Message,
		LastTransitionTime: cond.LastTransitionTime,
	}
	assert.Equal(t, expectCond, cond)

	expectReservation := &sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
			UID:  uuid.NewUUID(),
		},
	}
	reconciler.reservationInterpreter = fakeReservationInterpreter{
		reservation: expectReservation,
	}
	assert.Nil(t, reconciler.createReservation(context.TODO(), job))
	expectReservationRef := &corev1.ObjectReference{
		Name: "test-reservation",
		UID:  expectReservation.UID,
	}
	assert.Equal(t, expectReservationRef, job.Spec.ReservationOptions.ReservationRef)
}

func TestWaitForPendingPodScheduled(t *testing.T) {
	reconciler := newTestReconciler()

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))

	result, err := reconciler.waitForPendingPodScheduled(context.TODO(), job)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Nil(t, err)
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonMissingPod, job.Status.Reason)

	job.Status = sev1alpha1.PodMigrationJobStatus{}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	result, err = reconciler.waitForPendingPodScheduled(context.TODO(), job)
	assert.Equal(t, reconcile.Result{RequeueAfter: defaultRequeueAfter}, result)
	assert.Nil(t, err)

	_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionPodScheduled)
	assert.NotNil(t, cond)

	expectCond := &sev1alpha1.PodMigrationJobCondition{
		Type:               sev1alpha1.PodMigrationJobConditionPodScheduled,
		Status:             sev1alpha1.PodMigrationJobConditionStatusFalse,
		Reason:             sev1alpha1.PodMigrationJobReasonUnschedulable,
		Message:            cond.Message,
		LastTransitionTime: cond.LastTransitionTime,
	}
	assert.Equal(t, expectCond, cond)

	assert.Nil(t, reconciler.Client.Delete(context.TODO(), pod))
	pod = &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
		},
		Status: corev1.PodStatus{
			Conditions: []corev1.PodCondition{
				{
					Type:   corev1.PodScheduled,
					Status: corev1.ConditionTrue,
				},
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))
	result, err = reconciler.waitForPendingPodScheduled(context.TODO(), job)
	assert.Equal(t, reconcile.Result{}, result)
	assert.Nil(t, err)
	assert.Equal(t, sev1alpha1.PodMigrationJobSucceeded, job.Status.Phase)
	assert.Equal(t, "Complete", job.Status.Status)

	_, cond = util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionPodScheduled)
	assert.NotNil(t, cond)

	expectCond = &sev1alpha1.PodMigrationJobCondition{
		Type:               sev1alpha1.PodMigrationJobConditionPodScheduled,
		Status:             sev1alpha1.PodMigrationJobConditionStatusTrue,
		LastTransitionTime: cond.LastTransitionTime,
	}
	assert.Equal(t, expectCond, cond)
}

func TestMigrate(t *testing.T) {
	reconciler := newTestReconciler()
	reconciler.evictorInterpreter = fakeEvictionInterpreter{}

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			Paused: true,
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion: "apps/v1",
					Controller: pointer.Bool(true),
					Kind:       "StatefulSet",
					Name:       "test",
					UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
				},
			},
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	r := &sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Spec: sev1alpha1.ReservationSpec{
			Owners: []sev1alpha1.ReservationOwner{
				{
					Controller: &sev1alpha1.ReservationControllerReference{
						Namespace: "default",
						OwnerReference: metav1.OwnerReference{
							APIVersion: "apps/v1",
							Controller: pointer.Bool(true),
							Kind:       "StatefulSet",
							Name:       "test",
							UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
						},
					},
				},
			},
		},
		Status: sev1alpha1.ReservationStatus{
			Phase: sev1alpha1.ReservationAvailable,
			Conditions: []sev1alpha1.ReservationCondition{
				{
					Type:   sev1alpha1.ReservationConditionScheduled,
					Reason: sev1alpha1.ReasonReservationScheduled,
					Status: sev1alpha1.ConditionStatusTrue,
				},
			},
			CurrentOwners: []corev1.ObjectReference{
				{
					Namespace: "default",
					Name:      "test-pod-1",
				},
			},
			NodeName: "test-node-1",
		},
	}
	assert.NoError(t, reconciler.Client.Create(context.TODO(), r))

	reconciler.reservationInterpreter = fakeReservationInterpreter{
		reservation: r,
	}
	for {
		_, err := reconciler.Reconcile(context.TODO(), reconcile.Request{NamespacedName: types.NamespacedName{Name: job.Name}})
		if err != nil {
			t.Fatal(err)
		}
		assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))

		if job.Spec.Paused {
			job.Spec.Paused = false
			assert.Nil(t, reconciler.Client.Update(context.TODO(), job))
		}

		if job.Status.Phase != "" && job.Status.Phase != sev1alpha1.PodMigrationJobRunning {
			break
		}
		_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionEviction)
		if cond != nil && cond.Status == sev1alpha1.PodMigrationJobConditionStatusFalse {
			assert.Nil(t, reconciler.Client.Delete(context.TODO(), pod))
		}
	}
	assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobSucceeded, job.Status.Phase)
	assert.Equal(t, "Complete", job.Status.Status)
}

func TestMigrateWithSamePodName(t *testing.T) {
	reconciler := newTestReconciler()
	reconciler.evictorInterpreter = fakeEvictionInterpreter{}

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			Paused: true,
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			UID:       uuid.NewUUID(),
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion: "apps/v1",
					Controller: pointer.Bool(true),
					Kind:       "StatefulSet",
					Name:       "test",
					UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
				},
			},
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	podCopy := pod.DeepCopy()
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	r := &sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Spec: sev1alpha1.ReservationSpec{
			Owners: []sev1alpha1.ReservationOwner{
				{
					Controller: &sev1alpha1.ReservationControllerReference{
						Namespace: "default",
						OwnerReference: metav1.OwnerReference{
							APIVersion: "apps/v1",
							Controller: pointer.Bool(true),
							Kind:       "StatefulSet",
							Name:       "test",
							UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
						},
					},
				},
			},
		},
		Status: sev1alpha1.ReservationStatus{
			Phase: sev1alpha1.ReservationAvailable,
			Conditions: []sev1alpha1.ReservationCondition{
				{
					Type:   sev1alpha1.ReservationConditionScheduled,
					Reason: sev1alpha1.ReasonReservationScheduled,
					Status: sev1alpha1.ConditionStatusTrue,
				},
			},
			CurrentOwners: []corev1.ObjectReference{
				{
					Namespace: "default",
					Name:      "test-pod-1",
				},
			},
			NodeName: "test-node-1",
		},
	}
	assert.NoError(t, reconciler.Client.Create(context.TODO(), r))

	reconciler.reservationInterpreter = fakeReservationInterpreter{
		reservation: r,
	}
	for {
		_, err := reconciler.Reconcile(context.TODO(), reconcile.Request{NamespacedName: types.NamespacedName{Name: job.Name}})
		if err != nil {
			t.Fatal(err)
		}
		assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))

		if job.Spec.Paused {
			job.Spec.Paused = false
			assert.Nil(t, reconciler.Client.Update(context.TODO(), job))
		}

		if job.Status.Phase != "" && job.Status.Phase != sev1alpha1.PodMigrationJobRunning {
			break
		}
		_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionEviction)
		if cond != nil && cond.Status == sev1alpha1.PodMigrationJobConditionStatusFalse {
			assert.Nil(t, reconciler.Client.Delete(context.TODO(), pod))
			podCopy.UID = uuid.NewUUID()
			assert.NoError(t, reconciler.Client.Create(context.TODO(), podCopy))
		}
	}
	assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobSucceeded, job.Status.Phase)
	assert.Equal(t, "Complete", job.Status.Status)
}

func TestMigrateWhenEvictingWithSucceededReservation(t *testing.T) {
	reconciler := newTestReconciler()
	reconciler.evictorInterpreter = fakeEvictionInterpreter{}

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			Paused: true,
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion: "apps/v1",
					Controller: pointer.Bool(true),
					Kind:       "StatefulSet",
					Name:       "test",
					UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
				},
			},
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	r := &sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Spec: sev1alpha1.ReservationSpec{
			Owners: []sev1alpha1.ReservationOwner{
				{
					Controller: &sev1alpha1.ReservationControllerReference{
						Namespace: "default",
						OwnerReference: metav1.OwnerReference{
							APIVersion: "apps/v1",
							Controller: pointer.Bool(true),
							Kind:       "StatefulSet",
							Name:       "test",
							UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
						},
					},
				},
			},
		},
		Status: sev1alpha1.ReservationStatus{
			Phase: sev1alpha1.ReservationSucceeded,
			Conditions: []sev1alpha1.ReservationCondition{
				{
					Type:   sev1alpha1.ReservationConditionScheduled,
					Reason: sev1alpha1.ReasonReservationScheduled,
					Status: sev1alpha1.ConditionStatusTrue,
				},
			},
			CurrentOwners: []corev1.ObjectReference{
				{
					Namespace: "default",
					Name:      "test-pod-1",
				},
			},
			NodeName: "test-node-1",
		},
	}
	assert.NoError(t, reconciler.Create(context.TODO(), r))
	reconciler.reservationInterpreter = fakeReservationInterpreter{
		reservation: r,
	}
	for {
		_, err := reconciler.Reconcile(context.TODO(), reconcile.Request{NamespacedName: types.NamespacedName{Name: job.Name}})
		if err != nil {
			t.Fatal(err)
		}
		assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))

		if job.Spec.Paused {
			job.Spec.Paused = false
			assert.Nil(t, reconciler.Client.Update(context.TODO(), job))
		}

		if job.Status.Phase != "" && job.Status.Phase != sev1alpha1.PodMigrationJobRunning {
			break
		}
	}
	assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, string(sev1alpha1.PodMigrationJobConditionReservationScheduled), job.Status.Status)

	_, cond := util.GetCondition(&job.Status, sev1alpha1.PodMigrationJobConditionEviction)
	assert.Nil(t, cond)
}

func TestMigrateWithReservationScheduleFailed(t *testing.T) {
	reconciler := newTestReconciler()
	reconciler.evictorInterpreter = fakeEvictionInterpreter{}

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion: "apps/v1",
					Controller: pointer.Bool(true),
					Kind:       "StatefulSet",
					Name:       "test",
					UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
				},
			},
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	r := &sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Spec: sev1alpha1.ReservationSpec{
			Owners: []sev1alpha1.ReservationOwner{
				{
					Controller: &sev1alpha1.ReservationControllerReference{
						Namespace: "default",
						OwnerReference: metav1.OwnerReference{
							APIVersion: "apps/v1",
							Controller: pointer.Bool(true),
							Kind:       "StatefulSet",
							Name:       "test",
							UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
						},
					},
				},
			},
		},
		Status: sev1alpha1.ReservationStatus{
			Phase: sev1alpha1.ReservationFailed,
			Conditions: []sev1alpha1.ReservationCondition{
				{
					Type:    sev1alpha1.ReservationConditionScheduled,
					Reason:  sev1alpha1.ReasonReservationUnschedulable,
					Message: "expired reservation",
				},
			},
		},
	}
	assert.NoError(t, reconciler.Create(context.TODO(), r))
	reconciler.reservationInterpreter = fakeReservationInterpreter{
		reservation: r,
	}
	for {
		_, err := reconciler.Reconcile(context.TODO(), reconcile.Request{NamespacedName: types.NamespacedName{Name: job.Name}})
		if err != nil {
			t.Fatal(err)
		}
		assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))

		if job.Status.Phase != "" && job.Status.Phase != sev1alpha1.PodMigrationJobRunning {
			break
		}
	}
	assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonUnschedulable, job.Status.Reason)
}

func TestMigrateWithReservationSucceeded(t *testing.T) {
	reconciler := newTestReconciler()
	reconciler.evictorInterpreter = fakeEvictionInterpreter{}

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion: "apps/v1",
					Controller: pointer.Bool(true),
					Kind:       "StatefulSet",
					Name:       "test",
					UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
				},
			},
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	r := &sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Spec: sev1alpha1.ReservationSpec{
			Owners: []sev1alpha1.ReservationOwner{
				{
					Controller: &sev1alpha1.ReservationControllerReference{
						Namespace: "default",
						OwnerReference: metav1.OwnerReference{
							APIVersion: "apps/v1",
							Controller: pointer.Bool(true),
							Kind:       "StatefulSet",
							Name:       "test",
							UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
						},
					},
				},
			},
		},
		Status: sev1alpha1.ReservationStatus{
			NodeName: "test-node-1",
			Phase:    sev1alpha1.ReservationSucceeded,
			Conditions: []sev1alpha1.ReservationCondition{
				{
					Type:   sev1alpha1.ReservationConditionScheduled,
					Reason: sev1alpha1.ReasonReservationScheduled,
					Status: sev1alpha1.ConditionStatusTrue,
				},
			},
			CurrentOwners: []corev1.ObjectReference{
				{
					Namespace: "test",
					Name:      "other-pod",
					UID:       uuid.NewUUID(),
				},
			},
		},
	}
	assert.NoError(t, reconciler.Client.Create(context.TODO(), r))
	reconciler.reservationInterpreter = fakeReservationInterpreter{
		reservation: r,
	}
	for {
		_, err := reconciler.Reconcile(context.TODO(), reconcile.Request{NamespacedName: types.NamespacedName{Name: job.Name}})
		if err != nil {
			t.Fatal(err)
		}
		assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))

		if job.Status.Phase != "" && job.Status.Phase != sev1alpha1.PodMigrationJobRunning {
			break
		}
	}
	assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonForbiddenMigratePod, job.Status.Reason)
}

func TestMigrateWithReservationExpired(t *testing.T) {
	reconciler := newTestReconciler()
	reconciler.evictorInterpreter = fakeEvictionInterpreter{}

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion: "apps/v1",
					Controller: pointer.Bool(true),
					Kind:       "StatefulSet",
					Name:       "test",
					UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
				},
			},
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	r := &sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation",
		},
		Spec: sev1alpha1.ReservationSpec{
			Owners: []sev1alpha1.ReservationOwner{
				{
					Controller: &sev1alpha1.ReservationControllerReference{
						Namespace: "default",
						OwnerReference: metav1.OwnerReference{
							APIVersion: "apps/v1",
							Controller: pointer.Bool(true),
							Kind:       "StatefulSet",
							Name:       "test",
							UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
						},
					},
				},
			},
		},
		Status: sev1alpha1.ReservationStatus{
			NodeName: "test-node-1",
			Phase:    sev1alpha1.ReservationFailed,
			Conditions: []sev1alpha1.ReservationCondition{
				{
					Type:   sev1alpha1.ReservationConditionReady,
					Reason: sev1alpha1.ReasonReservationExpired,
				},
			},
		},
	}
	assert.NoError(t, reconciler.Client.Create(context.TODO(), r))
	reconciler.reservationInterpreter = fakeReservationInterpreter{
		reservation: r,
	}
	for {
		_, err := reconciler.Reconcile(context.TODO(), reconcile.Request{NamespacedName: types.NamespacedName{Name: job.Name}})
		if err != nil {
			t.Fatal(err)
		}
		assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))

		if job.Status.Phase != "" && job.Status.Phase != sev1alpha1.PodMigrationJobRunning {
			break
		}
	}
	assert.Nil(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonReservationExpired, job.Status.Reason)
}

func TestDoScavenge(t *testing.T) {
	reconciler := newTestReconciler()
	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	job.CreationTimestamp = metav1.Time{Time: job.CreationTimestamp.Add(1 * time.Hour)}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	for i := 0; i < 10; i++ {
		mustScavengeJob := &sev1alpha1.PodMigrationJob{
			ObjectMeta: metav1.ObjectMeta{
				Name:              fmt.Sprintf("test-%d", i),
				CreationTimestamp: metav1.Time{Time: time.Now()},
			},
			Spec: sev1alpha1.PodMigrationJobSpec{
				PodRef: &corev1.ObjectReference{
					Namespace: "default",
					Name:      "test-pod",
				},
				TTL: &metav1.Duration{Duration: 15 * time.Minute},
			},
		}
		assert.Nil(t, reconciler.Client.Create(context.TODO(), mustScavengeJob))
	}
	reconciler.clock = clock.NewFakeClock(time.Now().Add(20 * time.Minute))
	stopCh := make(chan struct{})
	close(stopCh)
	reconciler.scavenger(stopCh)
	jobList := &sev1alpha1.PodMigrationJobList{}
	opts := &client.ListOptions{
		LabelSelector: labels.Everything(),
	}
	assert.Nil(t, reconciler.Client.List(context.TODO(), jobList, opts))
	assert.Len(t, jobList.Items, 1)
	job.CreationTimestamp = jobList.Items[0].CreationTimestamp
	assert.Equal(t, job, &jobList.Items[0])
}

func TestEvict(t *testing.T) {
	reconciler := newTestReconciler()
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "test",
			Name:      "test-pod",
			OwnerReferences: []metav1.OwnerReference{
				{
					Controller: pointer.Bool(true),
					Kind:       "Deployment",
					Name:       "test",
				},
			},
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node-1",
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.True(t, reconciler.Filter(pod))

	assert.True(t, reconciler.Evict(context.TODO(), pod, framework.EvictOptions{}))
	var jobList sev1alpha1.PodMigrationJobList
	assert.NoError(t, reconciler.Client.List(context.TODO(), &jobList))
	assert.Equal(t, 1, len(jobList.Items))
	expectPodRef := &corev1.ObjectReference{
		Namespace: "test",
		Name:      "test-pod",
	}
	assert.Equal(t, expectPodRef, jobList.Items[0].Spec.PodRef)
}

func TestAbortJobIfReserveOnSameNode(t *testing.T) {
	reconciler := newTestReconciler()
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test",
			UID:       uuid.NewUUID(),
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node-1",
		},
	}
	assert.NoError(t, reconciler.Client.Create(context.TODO(), pod))

	testReservation := &sev1alpha1.Reservation{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-reservation-1",
			UID:  uuid.NewUUID(),
		},
		Status: sev1alpha1.ReservationStatus{
			NodeName: "test-node-1",
		},
	}
	assert.NoError(t, reconciler.Client.Create(context.TODO(), testReservation))

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-job-1",
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: pod.Namespace,
				Name:      pod.Name,
				UID:       pod.UID,
			},
			ReservationOptions: &sev1alpha1.PodMigrateReservationOptions{
				ReservationRef: &corev1.ObjectReference{
					Name: testReservation.Name,
					UID:  testReservation.UID,
				},
			},
		},
	}
	assert.NoError(t, reconciler.Client.Create(context.TODO(), job))

	reservationObj := reservation.NewReservation(testReservation)
	aborted, err := reconciler.abortJobIfReserveOnSameNode(context.TODO(), job, reservationObj)
	assert.NoError(t, err)
	assert.True(t, aborted)

	assert.NoError(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonForbiddenMigratePod, job.Status.Reason)
}

func TestRequeueJobIfRetriablePodFilterFailed(t *testing.T) {
	reconciler := newTestReconciler()
	enter := false
	reconciler.retriablePodFilter = func(pod *corev1.Pod) bool {
		enter = true
		return false
	}

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion: "apps/v1",
					Controller: pointer.Bool(true),
					Kind:       "StatefulSet",
					Name:       "test",
					UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
				},
			},
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	result, err := reconciler.doMigrate(context.TODO(), job)
	assert.True(t, enter)
	assert.NoError(t, err)
	assert.True(t, result.RequeueAfter != 0)
	assert.NoError(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobPhase(""), job.Status.Phase)
	assert.Equal(t, "", job.Status.Reason)
}

func TestAbortJobIfUnretriablePodFilterFailed(t *testing.T) {
	reconciler := newTestReconciler()
	enter := false
	reconciler.unretriablePodFilter = func(pod *corev1.Pod) bool {
		enter = true
		return false
	}

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			OwnerReferences: []metav1.OwnerReference{
				{
					APIVersion: "apps/v1",
					Controller: pointer.Bool(true),
					Kind:       "StatefulSet",
					Name:       "test",
					UID:        "2f96233d-a6b9-4981-b594-7c90c987aed9",
				},
			},
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	result, err := reconciler.doMigrate(context.TODO(), job)
	assert.True(t, enter)
	assert.NotNil(t, err)
	assert.Equal(t, reconcile.Result{}, result)

	assert.NoError(t, reconciler.Client.Get(context.TODO(), types.NamespacedName{Name: job.Name}, job))
	assert.Equal(t, sev1alpha1.PodMigrationJobFailed, job.Status.Phase)
	assert.Equal(t, sev1alpha1.PodMigrationJobReasonForbiddenMigratePod, job.Status.Reason)
}

func TestFilterExistingMigrationJob(t *testing.T) {
	reconciler := newTestReconciler()

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod",
			UID:       uuid.NewUUID(),
		},
		Spec: corev1.PodSpec{
			NodeName: "test-node",
		},
		Status: corev1.PodStatus{
			Phase: corev1.PodRunning,
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

	job := &sev1alpha1.PodMigrationJob{
		ObjectMeta: metav1.ObjectMeta{
			Name:              "test",
			CreationTimestamp: metav1.Time{Time: time.Now()},
		},
		Spec: sev1alpha1.PodMigrationJobSpec{
			PodRef: &corev1.ObjectReference{
				Namespace: "default",
				Name:      "test-pod",
				UID:       pod.UID,
			},
		},
	}
	assert.Nil(t, reconciler.Client.Create(context.TODO(), job))

	assert.False(t, reconciler.filterExistingPodMigrationJob(pod))
}

func TestFilterMaxMigratingPerNode(t *testing.T) {
	tests := []struct {
		name             string
		numMigratingPods int
		samePod          bool
		sameNode         bool
		maxMigrating     int32
		want             bool
	}{
		{
			name: "maxMigrating=0",
			want: true,
		},
		{
			name:         "maxMigrating=1 no migrating Pods",
			maxMigrating: 1,
			want:         true,
		},
		{
			name:             "maxMigrating=1 one migrating Pod with same Pod and Node",
			numMigratingPods: 1,
			samePod:          true,
			sameNode:         true,
			maxMigrating:     1,
			want:             true,
		},
		{
			name:             "maxMigrating=1 one migrating Pod with diff Pod and same Node",
			numMigratingPods: 1,
			samePod:          false,
			sameNode:         true,
			maxMigrating:     1,
			want:             false,
		},
		{
			name:             "maxMigrating=1 one migrating Pod with diff Pod and Node",
			numMigratingPods: 1,
			samePod:          false,
			sameNode:         false,
			maxMigrating:     1,
			want:             true,
		},
		{
			name:             "maxMigrating=2 two migrating Pod with same Pod and Node",
			numMigratingPods: 2,
			samePod:          true,
			sameNode:         true,
			maxMigrating:     2,
			want:             true,
		},
		{
			name:             "maxMigrating=2 two migrating Pod with diff Pod and Node",
			numMigratingPods: 2,
			samePod:          false,
			sameNode:         false,
			maxMigrating:     2,
			want:             true,
		},
		{
			name:             "maxMigrating=2 two migrating Pod with diff Pod and same Node",
			numMigratingPods: 2,
			samePod:          false,
			sameNode:         true,
			maxMigrating:     2,
			want:             false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			reconciler := newTestReconciler()
			reconciler.args.MaxMigratingPerNode = pointer.Int32(tt.maxMigrating)

			var migratingPods []*corev1.Pod
			for i := 0; i < tt.numMigratingPods; i++ {
				pod := &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "default",
						Name:      fmt.Sprintf("test-migrating-pod-%d", i),
						UID:       uuid.NewUUID(),
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node",
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodRunning,
					},
				}
				migratingPods = append(migratingPods, pod)

				assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

				job := &sev1alpha1.PodMigrationJob{
					ObjectMeta: metav1.ObjectMeta{
						Name:              fmt.Sprintf("test-%d", i),
						CreationTimestamp: metav1.Time{Time: time.Now()},
					},
					Spec: sev1alpha1.PodMigrationJobSpec{
						PodRef: &corev1.ObjectReference{
							Namespace: pod.Namespace,
							Name:      pod.Name,
							UID:       pod.UID,
						},
					},
				}
				assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
			}

			var filterPod *corev1.Pod
			if tt.samePod && len(migratingPods) > 0 {
				filterPod = migratingPods[0]
			}
			if filterPod == nil {
				filterPod = &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "default",
						Name:      fmt.Sprintf("test-pod-%s", uuid.NewUUID()),
						UID:       uuid.NewUUID(),
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodRunning,
					},
				}
			}
			if tt.sameNode {
				filterPod.Spec.NodeName = "test-node"
			} else {
				filterPod.Spec.NodeName = "test-other-node"
			}

			got := reconciler.filterMaxMigratingPerNode(filterPod)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestFilterMaxMigratingPerNamespace(t *testing.T) {
	tests := []struct {
		name             string
		numMigratingPods int
		samePod          bool
		sameNamespace    bool
		maxMigrating     int32
		want             bool
	}{
		{
			name: "maxMigrating=0",
			want: true,
		},
		{
			name:         "maxMigrating=1 no migrating Pods",
			maxMigrating: 1,
			want:         true,
		},
		{
			name:             "maxMigrating=1 one migrating Pod with same Pod and Namespace",
			numMigratingPods: 1,
			samePod:          true,
			sameNamespace:    true,
			maxMigrating:     1,
			want:             true,
		},
		{
			name:             "maxMigrating=1 one migrating Pod with diff Pod and same Namespace",
			numMigratingPods: 1,
			samePod:          false,
			sameNamespace:    true,
			maxMigrating:     1,
			want:             false,
		},
		{
			name:             "maxMigrating=1 one migrating Pod with diff Pod and Namespace",
			numMigratingPods: 1,
			samePod:          false,
			sameNamespace:    false,
			maxMigrating:     1,
			want:             true,
		},
		{
			name:             "maxMigrating=2 two migrating Pod with same Pod and Namespace",
			numMigratingPods: 2,
			samePod:          true,
			sameNamespace:    true,
			maxMigrating:     2,
			want:             true,
		},
		{
			name:             "maxMigrating=2 two migrating Pod with diff Pod and Namespace",
			numMigratingPods: 2,
			samePod:          false,
			sameNamespace:    false,
			maxMigrating:     2,
			want:             true,
		},
		{
			name:             "maxMigrating=2 two migrating Pod with diff Pod and same Namespace",
			numMigratingPods: 2,
			samePod:          false,
			sameNamespace:    true,
			maxMigrating:     2,
			want:             false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			reconciler := newTestReconciler()
			reconciler.args.MaxMigratingPerNamespace = pointer.Int32(tt.maxMigrating)

			var migratingPods []*corev1.Pod
			for i := 0; i < tt.numMigratingPods; i++ {
				pod := &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "default",
						Name:      fmt.Sprintf("test-migrating-pod-%d", i),
						UID:       uuid.NewUUID(),
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node",
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodRunning,
					},
				}
				migratingPods = append(migratingPods, pod)

				assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

				job := &sev1alpha1.PodMigrationJob{
					ObjectMeta: metav1.ObjectMeta{
						Name:              fmt.Sprintf("test-%d", i),
						CreationTimestamp: metav1.Time{Time: time.Now()},
					},
					Spec: sev1alpha1.PodMigrationJobSpec{
						PodRef: &corev1.ObjectReference{
							Namespace: pod.Namespace,
							Name:      pod.Name,
							UID:       pod.UID,
						},
					},
				}
				assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
			}

			var filterPod *corev1.Pod
			if tt.samePod && len(migratingPods) > 0 {
				filterPod = migratingPods[0]
			}
			if filterPod == nil {
				filterPod = &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "default",
						Name:      fmt.Sprintf("test-pod-%s", uuid.NewUUID()),
						UID:       uuid.NewUUID(),
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node",
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodRunning,
					},
				}
			}
			if !tt.sameNamespace {
				filterPod.Namespace = "other-namespace"
			}

			got := reconciler.filterMaxMigratingPerNamespace(filterPod)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestFilterMaxMigratingPerWorkload(t *testing.T) {
	ownerReferences1 := []metav1.OwnerReference{
		{
			APIVersion: "apps/v1",
			Controller: pointer.Bool(true),
			Kind:       "StatefulSet",
			Name:       "test-1",
			UID:        uuid.NewUUID(),
		},
	}

	ownerReferences2 := []metav1.OwnerReference{
		{
			APIVersion: "apps/v1",
			Controller: pointer.Bool(true),
			Kind:       "StatefulSet",
			Name:       "test-2",
			UID:        uuid.NewUUID(),
		},
	}
	tests := []struct {
		name             string
		totalReplicas    int32
		numMigratingPods int
		samePod          bool
		sameWorkload     bool
		maxMigrating     int
		want             bool
	}{
		{
			name:             "totalReplicas=1 and maxMigrating=1",
			totalReplicas:    1,
			numMigratingPods: 0,
			maxMigrating:     1,
			samePod:          true,
			sameWorkload:     true,
			want:             false,
		},
		{
			name:             "totalReplicas=100 and maxMigrating=100",
			totalReplicas:    100,
			numMigratingPods: 0,
			maxMigrating:     100,
			samePod:          true,
			sameWorkload:     true,
			want:             false,
		},
		{
			name:             "totalReplicas=10 and maxMigrating=1 no migrating Pod",
			totalReplicas:    10,
			numMigratingPods: 0,
			maxMigrating:     1,
			samePod:          false,
			sameWorkload:     false,
			want:             true,
		},
		{
			name:             "totalReplicas=10 and maxMigrating=1 one migrating Pod with same Pod and Workload",
			totalReplicas:    10,
			numMigratingPods: 1,
			maxMigrating:     1,
			samePod:          true,
			sameWorkload:     true,
			want:             true,
		},
		{
			name:             "totalReplicas=10 and maxMigrating=1 one migrating Pod with diff Pod and same Workload",
			totalReplicas:    10,
			numMigratingPods: 1,
			maxMigrating:     1,
			samePod:          false,
			sameWorkload:     true,
			want:             false,
		},
		{
			name:             "totalReplicas=10 and maxMigrating=1 one migrating Pod with diff Pod and diff Workload",
			totalReplicas:    10,
			numMigratingPods: 1,
			maxMigrating:     1,
			samePod:          false,
			sameWorkload:     false,
			want:             true,
		},
		{
			name:             "totalReplicas=10 and maxMigrating=2 two migrating Pod with same Pod and Workload",
			totalReplicas:    10,
			numMigratingPods: 2,
			maxMigrating:     2,
			samePod:          true,
			sameWorkload:     true,
			want:             true,
		},
		{
			name:             "totalReplicas=10 and maxMigrating=2 two migrating Pod with diff Pod and same Workload",
			totalReplicas:    10,
			numMigratingPods: 2,
			maxMigrating:     2,
			samePod:          false,
			sameWorkload:     true,
			want:             false,
		},
		{
			name:             "totalReplicas=10 and maxMigrating=2 two migrating Pod with diff Pod and diff Workload",
			totalReplicas:    10,
			numMigratingPods: 2,
			maxMigrating:     2,
			samePod:          false,
			sameWorkload:     false,
			want:             true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			reconciler := newTestReconciler()
			intOrString := intstr.FromInt(tt.maxMigrating)
			reconciler.args.MaxMigratingPerWorkload = &intOrString
			maxUnavailable := intstr.FromInt(int(tt.totalReplicas - 1))
			reconciler.args.MaxUnavailablePerWorkload = &maxUnavailable

			var migratingPods []*corev1.Pod
			for i := 0; i < tt.numMigratingPods; i++ {
				pod := &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace:       "default",
						Name:            fmt.Sprintf("test-migrating-pod-%d", i),
						UID:             uuid.NewUUID(),
						OwnerReferences: ownerReferences1,
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node",
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodRunning,
						Conditions: []corev1.PodCondition{
							{
								Type:   corev1.PodReady,
								Status: corev1.ConditionTrue,
							},
						},
					},
				}
				migratingPods = append(migratingPods, pod)

				assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

				job := &sev1alpha1.PodMigrationJob{
					ObjectMeta: metav1.ObjectMeta{
						Name:              fmt.Sprintf("test-%d", i),
						CreationTimestamp: metav1.Time{Time: time.Now()},
					},
					Spec: sev1alpha1.PodMigrationJobSpec{
						PodRef: &corev1.ObjectReference{
							Namespace: pod.Namespace,
							Name:      pod.Name,
							UID:       pod.UID,
						},
					},
				}
				assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
			}

			var filterPod *corev1.Pod
			if tt.samePod && len(migratingPods) > 0 {
				filterPod = migratingPods[0]
			}
			if filterPod == nil {
				filterPod = &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace:       "default",
						Name:            fmt.Sprintf("test-pod-%s", uuid.NewUUID()),
						UID:             uuid.NewUUID(),
						OwnerReferences: ownerReferences1,
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node",
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodRunning,
					},
				}
			}
			if !tt.sameWorkload {
				filterPod.OwnerReferences = ownerReferences2
			}

			reconciler.controllerFinder = &fakeControllerFinder{
				replicas: tt.totalReplicas,
			}

			got := reconciler.filterMaxMigratingOrUnavailablePerWorkload(filterPod)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestFilterMaxUnavailablePerWorkload(t *testing.T) {
	ownerReferences1 := []metav1.OwnerReference{
		{
			APIVersion: "apps/v1",
			Controller: pointer.Bool(true),
			Kind:       "StatefulSet",
			Name:       "test-1",
			UID:        uuid.NewUUID(),
		},
	}

	ownerReferences2 := []metav1.OwnerReference{
		{
			APIVersion: "apps/v1",
			Controller: pointer.Bool(true),
			Kind:       "StatefulSet",
			Name:       "test-2",
			UID:        uuid.NewUUID(),
		},
	}
	tests := []struct {
		name               string
		totalReplicas      int32
		numUnavailablePods int
		numMigratingPods   int
		maxUnavailable     int
		sameWorkload       bool
		want               bool
	}{
		{
			name:               "totalReplicas=1 and maxUnavailable=1",
			totalReplicas:      1,
			numUnavailablePods: 0,
			maxUnavailable:     1,
			sameWorkload:       true,
			want:               false,
		},
		{
			name:               "totalReplicas=100 and maxUnavailable=100",
			totalReplicas:      100,
			numUnavailablePods: 0,
			maxUnavailable:     100,
			sameWorkload:       true,
			want:               false,
		},
		{
			name:               "totalReplicas=10 and maxUnavailable=1 no migrating Pod and no unavailable Pod",
			totalReplicas:      10,
			numUnavailablePods: 0,
			numMigratingPods:   0,
			maxUnavailable:     1,
			sameWorkload:       true,
			want:               true,
		},
		{
			name:               "totalReplicas=10 and maxUnavailable=1 one unavailable Pod with same Workload",
			totalReplicas:      10,
			numUnavailablePods: 1,
			numMigratingPods:   0,
			maxUnavailable:     1,
			sameWorkload:       true,
			want:               false,
		},
		{
			name:               "totalReplicas=10 and maxUnavailable=1 one migrating Pod with same Workload",
			totalReplicas:      10,
			numUnavailablePods: 0,
			numMigratingPods:   1,
			maxUnavailable:     1,
			sameWorkload:       true,
			want:               false,
		},
		{
			name:               "totalReplicas=10 and maxUnavailable=1 one unavailable Pod and one migrating Pod with same Workload",
			totalReplicas:      10,
			numUnavailablePods: 1,
			maxUnavailable:     1,
			sameWorkload:       true,
			want:               false,
		},

		{
			name:               "totalReplicas=10 and maxUnavailable=2 no migrating Pod and no unavailable Pod",
			totalReplicas:      10,
			numUnavailablePods: 0,
			numMigratingPods:   0,
			maxUnavailable:     2,
			sameWorkload:       true,
			want:               true,
		},
		{
			name:               "totalReplicas=10 and maxUnavailable=2 one unavailable Pod with same Workload",
			totalReplicas:      10,
			numUnavailablePods: 1,
			numMigratingPods:   0,
			maxUnavailable:     2,
			sameWorkload:       true,
			want:               true,
		},
		{
			name:               "totalReplicas=10 and maxUnavailable=2 one migrating Pod with same Workload",
			totalReplicas:      10,
			numUnavailablePods: 0,
			numMigratingPods:   1,
			maxUnavailable:     2,
			sameWorkload:       true,
			want:               true,
		},
		{
			name:               "totalReplicas=10 and maxUnavailable=2 one unavailable Pod and one migrating Pod with same Workload",
			totalReplicas:      10,
			numUnavailablePods: 1,
			numMigratingPods:   1,
			maxUnavailable:     2,
			sameWorkload:       true,
			want:               false,
		},
		{
			name:               "totalReplicas=10 and maxUnavailable=2 one unavailable Pod and one migrating Pod with diff Workload",
			totalReplicas:      10,
			numUnavailablePods: 1,
			numMigratingPods:   1,
			maxUnavailable:     2,
			sameWorkload:       false,
			want:               true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			reconciler := newTestReconciler()
			intOrString := intstr.FromInt(int(tt.totalReplicas - 1))
			reconciler.args.MaxMigratingPerWorkload = &intOrString
			maxUnavailable := intstr.FromInt(tt.maxUnavailable)
			reconciler.args.MaxUnavailablePerWorkload = &maxUnavailable

			var totalPods []*corev1.Pod
			for i := 0; i < tt.numUnavailablePods; i++ {
				pod := &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace:       "default",
						Name:            fmt.Sprintf("test-unavailable-pod-%d", i),
						UID:             uuid.NewUUID(),
						OwnerReferences: ownerReferences1,
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node",
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodPending,
					},
				}
				totalPods = append(totalPods, pod)

				assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))
			}

			for i := 0; i < tt.numMigratingPods; i++ {
				pod := &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace:       "default",
						Name:            fmt.Sprintf("test-migrating-pod-%d", i),
						UID:             uuid.NewUUID(),
						OwnerReferences: ownerReferences1,
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node",
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodRunning,
						Conditions: []corev1.PodCondition{
							{
								Type:   corev1.PodReady,
								Status: corev1.ConditionTrue,
							},
						},
					},
				}
				totalPods = append(totalPods, pod)

				assert.Nil(t, reconciler.Client.Create(context.TODO(), pod))

				job := &sev1alpha1.PodMigrationJob{
					ObjectMeta: metav1.ObjectMeta{
						Name:              fmt.Sprintf("test-%d", i),
						CreationTimestamp: metav1.Time{Time: time.Now()},
					},
					Spec: sev1alpha1.PodMigrationJobSpec{
						PodRef: &corev1.ObjectReference{
							Namespace: pod.Namespace,
							Name:      pod.Name,
							UID:       pod.UID,
						},
					},
				}
				assert.Nil(t, reconciler.Client.Create(context.TODO(), job))
			}

			for i := 0; i < int(tt.totalReplicas)-len(totalPods); i++ {
				pod := &corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace:       "default",
						Name:            fmt.Sprintf("test-available-pod-%d", i),
						UID:             uuid.NewUUID(),
						OwnerReferences: ownerReferences1,
					},
					Spec: corev1.PodSpec{
						NodeName: "test-node",
					},
					Status: corev1.PodStatus{
						Phase: corev1.PodRunning,
						Conditions: []corev1.PodCondition{
							{
								Type:   corev1.PodReady,
								Status: corev1.ConditionTrue,
							},
						},
					},
				}
				totalPods = append(totalPods, pod)
			}

			filterPod := &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace:       "default",
					Name:            fmt.Sprintf("test-pod-%s", uuid.NewUUID()),
					UID:             uuid.NewUUID(),
					OwnerReferences: ownerReferences1,
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			}
			if !tt.sameWorkload {
				filterPod.OwnerReferences = ownerReferences2
			}

			reconciler.controllerFinder = &fakeControllerFinder{
				pods:     totalPods,
				replicas: tt.totalReplicas,
			}

			got := reconciler.filterMaxMigratingOrUnavailablePerWorkload(filterPod)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestFilterObjectLimiter(t *testing.T) {
	ownerReferences1 := []metav1.OwnerReference{
		{
			APIVersion: "apps/v1",
			Controller: pointer.Bool(true),
			Kind:       "StatefulSet",
			Name:       "test-1",
			UID:        uuid.NewUUID(),
		},
	}
	otherOwnerReferences := metav1.OwnerReference{
		APIVersion: "apps/v1",
		Controller: pointer.Bool(true),
		Kind:       "StatefulSet",
		Name:       "test-2",
		UID:        uuid.NewUUID(),
	}
	testObjectLimiters := deschedulerconfig.ObjectLimiterMap{
		deschedulerconfig.MigrationLimitObjectWorkload: {
			Duration:     metav1.Duration{Duration: 1 * time.Second},
			MaxMigrating: &intstr.IntOrString{Type: intstr.Int, IntVal: 10},
		},
	}

	tests := []struct {
		name             string
		objectLimiters   deschedulerconfig.ObjectLimiterMap
		totalReplicas    int32
		sleepDuration    time.Duration
		pod              *corev1.Pod
		evictedPodsCount int
		evictedWorkload  *metav1.OwnerReference
		want             bool
	}{
		{
			name:           "less than default maxMigrating",
			totalReplicas:  100,
			objectLimiters: testObjectLimiters,
			sleepDuration:  100 * time.Millisecond,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					OwnerReferences: ownerReferences1,
				},
			},
			evictedPodsCount: 6,
			want:             true,
		},
		{
			name:           "exceeded default maxMigrating",
			totalReplicas:  100,
			objectLimiters: testObjectLimiters,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					OwnerReferences: ownerReferences1,
				},
			},
			evictedPodsCount: 11,
			want:             false,
		},
		{
			name:           "other than workload",
			totalReplicas:  100,
			objectLimiters: testObjectLimiters,
			sleepDuration:  100 * time.Millisecond,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					OwnerReferences: ownerReferences1,
				},
			},
			evictedPodsCount: 11,
			evictedWorkload:  &otherOwnerReferences,
			want:             true,
		},
		{
			name:          "disable objectLimiters",
			totalReplicas: 100,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					OwnerReferences: ownerReferences1,
				},
			},
			evictedPodsCount: 11,
			objectLimiters: deschedulerconfig.ObjectLimiterMap{
				deschedulerconfig.MigrationLimitObjectWorkload: deschedulerconfig.MigrationObjectLimiter{
					Duration: metav1.Duration{Duration: 0},
				},
			},
			want: true,
		},
		{
			name:          "default limiter",
			totalReplicas: 100,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					OwnerReferences: ownerReferences1,
				},
			},
			evictedPodsCount: 1,
			want:             false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			reconciler := newTestReconciler()
			controllerFinder := &fakeControllerFinder{}
			if tt.objectLimiters != nil {
				reconciler.args.ObjectLimiters = tt.objectLimiters
			}

			reconciler.initObjectLimiters()
			if tt.totalReplicas > 0 {
				controllerFinder.replicas = tt.totalReplicas
			}
			reconciler.controllerFinder = controllerFinder
			if tt.evictedPodsCount > 0 {
				for i := 0; i < tt.evictedPodsCount; i++ {
					pod := tt.pod.DeepCopy()
					if tt.evictedWorkload != nil {
						pod.OwnerReferences = []metav1.OwnerReference{
							*tt.evictedWorkload,
						}
					}
					reconciler.trackEvictedPod(pod)
					if tt.sleepDuration > 0 {
						time.Sleep(tt.sleepDuration)
					}
				}
			}
			got := reconciler.filterLimitedObject(tt.pod)
			assert.Equal(t, tt.want, got)
		})
	}
}
