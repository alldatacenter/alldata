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

package frameworkext

import (
	"context"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordinatorclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
)

// ExtendedHandle extends the k8s scheduling framework Handle interface
// to facilitate plugins to access Koordinator's resources and states.
type ExtendedHandle interface {
	framework.Handle
	KoordinatorClientSet() koordinatorclientset.Interface
	KoordinatorSharedInformerFactory() koordinatorinformers.SharedInformerFactory
}

// FrameworkExtender extends the K8s Scheduling Framework interface to provide more extension methods to support Koordinator.
type FrameworkExtender interface {
	framework.Framework
	ExtendedHandle

	RunReservationPreFilterExtensionRemoveReservation(ctx context.Context, cycleState *framework.CycleState, podToSchedule *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeInfo *framework.NodeInfo) *framework.Status
	RunReservationPreFilterExtensionAddPodInReservation(ctx context.Context, cycleState *framework.CycleState, podToSchedule *corev1.Pod, podToAdd *framework.PodInfo, reservation *schedulingv1alpha1.Reservation, nodeInfo *framework.NodeInfo) *framework.Status

	RunReservationFilterPlugins(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeName string) *framework.Status
	RunReservationScorePlugins(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, reservations []*schedulingv1alpha1.Reservation, nodeName string) (PluginToReservationScores, *framework.Status)
}

// SharedListerAdapter intercepts the incoming ShareLister and
// modifies the corresponding data to support some advanced scheduling scenarios.
type SharedListerAdapter func(lister framework.SharedLister) framework.SharedLister

// SchedulingTransformer is the parent type for all the custom transformer plugins.
type SchedulingTransformer interface {
	Name() string
}

// PreFilterTransformer is executed before and after PreFilter.
type PreFilterTransformer interface {
	SchedulingTransformer
	// BeforePreFilter If there is a change to the incoming Pod, it needs to be modified after DeepCopy and returned.
	BeforePreFilter(handle ExtendedHandle, state *framework.CycleState, pod *corev1.Pod) (*corev1.Pod, bool)
	// AfterPreFilter is executed after PreFilter.
	// There is a chance to trigger the correction of the State data of each plugin after the PreFilter.
	AfterPreFilter(handle ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod) error
}

// FilterTransformer is executed before Filter.
type FilterTransformer interface {
	SchedulingTransformer
	BeforeFilter(handle ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) (*corev1.Pod, *framework.NodeInfo, bool)
}

// ScoreTransformer is executed before Score.
type ScoreTransformer interface {
	SchedulingTransformer
	BeforeScore(handle ExtendedHandle, cycleState *framework.CycleState, pod *corev1.Pod, nodes []*corev1.Node) (*corev1.Pod, []*corev1.Node, bool)
}

// ReservationPreFilterExtension is used to support the return of fine-grained resources
// held by Reservation, such as CPU Cores, GPU Devices, etc. During Pod scheduling, resources
// held by these reservations need to be allocated first, otherwise resources will be wasted.
// First, RemoveReservation will be called to return the resources held by the Reservation, and
// then AddPodInReservation will be called to indicate that the Pod has used some resources of
// the Reservation, and these resources can no longer be allocated.
type ReservationPreFilterExtension interface {
	framework.Plugin
	RemoveReservation(ctx context.Context, cycleState *framework.CycleState, podToSchedule *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeInfo *framework.NodeInfo) *framework.Status
	AddPodInReservation(ctx context.Context, cycleState *framework.CycleState, podToSchedule *corev1.Pod, podInfoToAdd *framework.PodInfo, reservation *schedulingv1alpha1.Reservation, nodeInfo *framework.NodeInfo) *framework.Status
}

// ReservationFilterPlugin is an interface for Filter Reservation plugins.
// These plugins will be called during the Reserve phase to determine whether the Reservation can participate in the Reserve
type ReservationFilterPlugin interface {
	framework.Plugin
	FilterReservation(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeName string) *framework.Status
}

// ReservationNominator nominates a more suitable Reservation in the Reserve stage and Pod will bind this Reservation.
// The Reservation will be recorded in CycleState through SetNominatedReservation.
// When executing Reserve, each plugin will obtain the currently used Reservation through GetNominatedReservation,
// and locate the previously returned reusable resources for Pod allocation.
type ReservationNominator interface {
	framework.Plugin
	NominateReservation(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) (*schedulingv1alpha1.Reservation, *framework.Status)
}

const (
	// MaxReservationScore is the maximum score a ReservationScorePlugin plugin is expected to return.
	MaxReservationScore int64 = 100

	// MinReservationScore is the minimum score a ReservationScorePlugin plugin is expected to return.
	MinReservationScore int64 = 0
)

// ReservationScoreList declares a list of reservations and their scores.
type ReservationScoreList []ReservationScore

// ReservationScore is a struct with reservation name and score.
type ReservationScore struct {
	Name  string
	Score int64
}

// PluginToReservationScores declares a map from plugin name to its ReservationScoreList.
type PluginToReservationScores map[string]ReservationScoreList

// ReservationScorePlugin is an interface that must be implemented by "ScoreReservation" plugins to rank
// reservations that passed the reserve phase.
type ReservationScorePlugin interface {
	framework.Plugin
	ScoreReservation(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeName string) (int64, *framework.Status)
}

var (
	nominatedReservationKey framework.StateKey = "koordinator.sh/nominated-reservation"
)

// nominatedReservationState saves the reservation nominated by ReservationNominator
type nominatedReservationState struct {
	reservation *schedulingv1alpha1.Reservation
}

func (r *nominatedReservationState) Clone() framework.StateData {
	return r
}

func SetNominatedReservation(cycleState *framework.CycleState, reservation *schedulingv1alpha1.Reservation) {
	if reservation != nil {
		cycleState.Write(nominatedReservationKey, &nominatedReservationState{reservation: reservation})
	}
}

func GetNominatedReservation(cycleState *framework.CycleState) *schedulingv1alpha1.Reservation {
	state, err := cycleState.Read(nominatedReservationKey)
	if err != nil {
		return nil
	}
	return state.(*nominatedReservationState).reservation
}

// ReservationPreBindPlugin performs special binding logic specifically for Reservation in the PreBind phase.
// Similar to the built-in VolumeBinding plugin of kube-scheduler, it does not support Reservation,
// and how Reservation itself uses PVC reserved resources also needs special handling.
// In addition, implementing this interface can clearly indicate that the plugin supports Reservation.
type ReservationPreBindPlugin interface {
	framework.Plugin
	PreBindReservation(ctx context.Context, state *framework.CycleState, reservation *schedulingv1alpha1.Reservation, nodeName string) *framework.Status
}
