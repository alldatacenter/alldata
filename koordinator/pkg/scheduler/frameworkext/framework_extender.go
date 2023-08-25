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
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordinatorclientset "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	reservationutil "github.com/koordinator-sh/koordinator/pkg/util/reservation"
)

var _ FrameworkExtender = &frameworkExtenderImpl{}

type frameworkExtenderImpl struct {
	framework.Framework

	temporarySnapshot                *TemporarySnapshot
	sharedListerAdapter              SharedListerAdapter
	koordinatorClientSet             koordinatorclientset.Interface
	koordinatorSharedInformerFactory koordinatorinformers.SharedInformerFactory

	preFilterTransformers []PreFilterTransformer
	filterTransformers    []FilterTransformer
	scoreTransformers     []ScoreTransformer

	reservationFilterPlugins       []ReservationFilterPlugin
	reservationScorePlugins        []ReservationScorePlugin
	reservationNominatorPlugins    []ReservationNominator
	reservationPreBindPlugins      []ReservationPreBindPlugin
	reservationPreFilterExtensions []ReservationPreFilterExtension
}

func NewFrameworkExtender(f *FrameworkExtenderFactory, fw framework.Framework) FrameworkExtender {
	frameworkExtender := &frameworkExtenderImpl{
		Framework:                        fw,
		sharedListerAdapter:              f.SharedListerAdapter(),
		koordinatorClientSet:             f.KoordinatorClientSet(),
		koordinatorSharedInformerFactory: f.koordinatorSharedInformerFactory,
	}
	frameworkExtender.updateTransformer(f.defaultTransformers...)
	return frameworkExtender
}

func (ext *frameworkExtenderImpl) updateTransformer(transformers ...SchedulingTransformer) {
	for _, transformer := range transformers {
		preFilterTransformer, ok := transformer.(PreFilterTransformer)
		if ok {
			ext.preFilterTransformers = append(ext.preFilterTransformers, preFilterTransformer)
			klog.V(4).InfoS("framework extender got scheduling transformer registered", "preFilter", preFilterTransformer.Name())
		}
		filterTransformer, ok := transformer.(FilterTransformer)
		if ok {
			ext.filterTransformers = append(ext.filterTransformers, filterTransformer)
			klog.V(4).InfoS("framework extender got scheduling transformer registered", "filter", filterTransformer.Name())
		}
		scoreTransformer, ok := transformer.(ScoreTransformer)
		if ok {
			ext.scoreTransformers = append(ext.scoreTransformers, scoreTransformer)
			klog.V(4).InfoS("framework extender got scheduling transformer registered", "score", scoreTransformer.Name())
		}
	}
}

func (ext *frameworkExtenderImpl) updatePlugins(pl framework.Plugin) {
	if transformer, ok := pl.(SchedulingTransformer); ok {
		ext.updateTransformer(transformer)
	}
	if r, ok := pl.(ReservationFilterPlugin); ok {
		ext.reservationFilterPlugins = append(ext.reservationFilterPlugins, r)
	}
	if r, ok := pl.(ReservationScorePlugin); ok {
		ext.reservationScorePlugins = append(ext.reservationScorePlugins, r)
	}
	if r, ok := pl.(ReservationNominator); ok {
		ext.reservationNominatorPlugins = append(ext.reservationNominatorPlugins, r)
	}
	if r, ok := pl.(ReservationPreBindPlugin); ok {
		ext.reservationPreBindPlugins = append(ext.reservationPreBindPlugins, r)
	}
	if r, ok := pl.(ReservationPreFilterExtension); ok {
		ext.reservationPreFilterExtensions = append(ext.reservationPreFilterExtensions, r)
	}
}

func (ext *frameworkExtenderImpl) KoordinatorClientSet() koordinatorclientset.Interface {
	return ext.koordinatorClientSet
}

func (ext *frameworkExtenderImpl) KoordinatorSharedInformerFactory() koordinatorinformers.SharedInformerFactory {
	return ext.koordinatorSharedInformerFactory
}

func (ext *frameworkExtenderImpl) SnapshotSharedLister() framework.SharedLister {
	if ext.temporarySnapshot == nil {
		// Only test scenarios are encountered here.
		sharedLister := ext.Framework.SnapshotSharedLister()
		if ext.sharedListerAdapter != nil {
			sharedLister = ext.sharedListerAdapter(sharedLister)
		}
		return sharedLister
	}
	return ext.temporarySnapshot
}

func (ext *frameworkExtenderImpl) takeTemporarySnapshot() error {
	sharedLister := ext.Framework.SnapshotSharedLister()
	if ext.sharedListerAdapter != nil {
		sharedLister = ext.sharedListerAdapter(sharedLister)
	}
	if ext.temporarySnapshot == nil {
		ext.temporarySnapshot = NewTemporarySnapshot()
	}
	return ext.temporarySnapshot.UpdateSnapshot(sharedLister)
}

// RunPreFilterPlugins transforms the PreFilter phase of framework with pre-filter transformers.
func (ext *frameworkExtenderImpl) RunPreFilterPlugins(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod) *framework.Status {
	// Custom Transformers may need to modify NodeInfo, but modifying the snapshot data returned by framework.SnapshotSharedLister
	// may cause abnormal scheduling behavior, for example, errors such as "no corresponding pod" may be encountered
	// when Reservation returns data. The reason for this error is that these snapshot data may not be updated in full every time,
	// so we need to clone a copy of the full snapshot data to ensure that this round of scheduling requirements are met.
	err := ext.takeTemporarySnapshot()
	if err != nil {
		return framework.AsStatus(err)
	}

	for _, transformer := range ext.preFilterTransformers {
		newPod, transformed := transformer.BeforePreFilter(ext, cycleState, pod)
		if transformed {
			klog.V(5).InfoS("RunPreFilterPlugins transformed", "transformer", transformer.Name(), "pod", klog.KObj(pod))
			pod = newPod
		}
	}

	status := ext.Framework.RunPreFilterPlugins(ctx, cycleState, pod)
	if !status.IsSuccess() {
		return status
	}

	for _, transformer := range ext.preFilterTransformers {
		if err := transformer.AfterPreFilter(ext, cycleState, pod); err != nil {
			return framework.AsStatus(err)
		}
	}
	return nil
}

// RunFilterPluginsWithNominatedPods transforms the Filter phase of framework with filter transformers.
// We don't transform RunFilterPlugins since framework's RunFilterPluginsWithNominatedPods just calls its RunFilterPlugins.
func (ext *frameworkExtenderImpl) RunFilterPluginsWithNominatedPods(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) *framework.Status {
	for _, transformer := range ext.filterTransformers {
		newPod, newNodeInfo, transformed := transformer.BeforeFilter(ext, cycleState, pod, nodeInfo)
		if transformed {
			klog.V(5).InfoS("RunFilterPluginsWithNominatedPods transformed", "transformer", transformer.Name(), "pod", klog.KObj(pod))
			pod = newPod
			nodeInfo = newNodeInfo
		}
	}
	status := ext.Framework.RunFilterPluginsWithNominatedPods(ctx, cycleState, pod, nodeInfo)
	if !status.IsSuccess() && debugFilterFailure {
		klog.Infof("Failed to filter for Pod %q on Node %q, failedPlugin: %s, reason: %s", klog.KObj(pod), klog.KObj(nodeInfo.Node()), status.FailedPlugin(), status.Message())
	}
	return status
}

func (ext *frameworkExtenderImpl) RunScorePlugins(ctx context.Context, state *framework.CycleState, pod *corev1.Pod, nodes []*corev1.Node) (framework.PluginToNodeScores, *framework.Status) {
	for _, transformer := range ext.scoreTransformers {
		newPod, newNodes, transformed := transformer.BeforeScore(ext, state, pod, nodes)
		if transformed {
			klog.V(5).InfoS("RunScorePlugins transformed", "transformer", transformer.Name(), "pod", klog.KObj(pod))
			pod = newPod
			nodes = newNodes
		}
	}
	pluginToNodeScores, status := ext.Framework.RunScorePlugins(ctx, state, pod, nodes)
	if status.IsSuccess() && debugTopNScores > 0 {
		debugScores(debugTopNScores, pod, pluginToNodeScores, nodes)
	}
	return pluginToNodeScores, status
}

// RunReservePluginsReserve supports trying to obtain the most suitable Reservation on the current node during the Reserve phase
func (ext *frameworkExtenderImpl) RunReservePluginsReserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	if reservationutil.IsReservePod(pod) {
		return ext.Framework.RunReservePluginsReserve(ctx, cycleState, pod, nodeName)
	}

	for _, pl := range ext.reservationNominatorPlugins {
		reservation, status := pl.NominateReservation(ctx, cycleState, pod, nodeName)
		if !status.IsSuccess() {
			return status
		}
		if reservation != nil {
			SetNominatedReservation(cycleState, reservation)
			break
		}
	}
	return ext.Framework.RunReservePluginsReserve(ctx, cycleState, pod, nodeName)
}

// RunPreBindPlugins supports PreBindReservation for Reservation
func (ext *frameworkExtenderImpl) RunPreBindPlugins(ctx context.Context, state *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	if !reservationutil.IsReservePod(pod) {
		return ext.Framework.RunPreBindPlugins(ctx, state, pod, nodeName)
	}

	reservationLister := ext.koordinatorSharedInformerFactory.Scheduling().V1alpha1().Reservations().Lister()
	rName := reservationutil.GetReservationNameFromReservePod(pod)
	reservation, err := reservationLister.Get(rName)
	if err != nil {
		return framework.AsStatus(err)
	}

	reservation = reservation.DeepCopy()
	reservation.Status.NodeName = nodeName

	for _, pl := range ext.reservationPreBindPlugins {
		status := pl.PreBindReservation(ctx, state, reservation, nodeName)
		if !status.IsSuccess() {
			err := status.AsError()
			klog.ErrorS(err, "Failed running ReservationPreBindPlugin plugin", "plugin", pl.Name(), "reservation", klog.KObj(reservation))
			return framework.AsStatus(fmt.Errorf("running ReservationPreBindPlugin plugin %q: %w", pl.Name(), err))
		}
	}
	return nil
}

// RunReservationPreFilterExtensionRemoveReservation restores the Reservation during PreFilter phase
func (ext *frameworkExtenderImpl) RunReservationPreFilterExtensionRemoveReservation(ctx context.Context, cycleState *framework.CycleState, podToSchedule *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeInfo *framework.NodeInfo) *framework.Status {
	for _, pl := range ext.reservationPreFilterExtensions {
		status := pl.RemoveReservation(ctx, cycleState, podToSchedule, reservation, nodeInfo)
		if !status.IsSuccess() {
			err := status.AsError()
			klog.ErrorS(err, "Failed running RemoveReservation on plugin", "plugin", pl.Name(), "pod", klog.KObj(podToSchedule))
			return framework.AsStatus(fmt.Errorf("running RemoveReservation on plugin %q: %w", pl.Name(), err))
		}
	}
	return nil
}

// RunReservationPreFilterExtensionAddPodInReservation restores Pod of the Reservation during PreFilter phase
func (ext *frameworkExtenderImpl) RunReservationPreFilterExtensionAddPodInReservation(ctx context.Context, cycleState *framework.CycleState, podToSchedule *corev1.Pod, podToAdd *framework.PodInfo, reservation *schedulingv1alpha1.Reservation, nodeInfo *framework.NodeInfo) *framework.Status {
	for _, pl := range ext.reservationPreFilterExtensions {
		status := pl.AddPodInReservation(ctx, cycleState, podToSchedule, podToAdd, reservation, nodeInfo)
		if !status.IsSuccess() {
			err := status.AsError()
			klog.ErrorS(err, "Failed running AddPodInReservation on plugin", "plugin", pl.Name(), "pod", klog.KObj(podToSchedule))
			return framework.AsStatus(fmt.Errorf("running AddPodInReservation on plugin %q: %w", pl.Name(), err))
		}
	}
	return nil
}

// RunReservationFilterPlugins determines whether the Reservation can participate in the Reserve
func (ext *frameworkExtenderImpl) RunReservationFilterPlugins(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, reservation *schedulingv1alpha1.Reservation, nodeName string) *framework.Status {
	for _, pl := range ext.reservationFilterPlugins {
		status := pl.FilterReservation(ctx, cycleState, pod, reservation, nodeName)
		if !status.IsSuccess() {
			klog.Infof("Failed to FilterReservation for Pod %q with Reservation %q on Node %q, failedPlugin: %s, reason: %s", klog.KObj(pod), klog.KObj(reservation), nodeName, status.FailedPlugin(), status.Message())
			return status
		}
	}
	return nil
}

// RunReservationScorePlugins ranks the Reservations
func (ext *frameworkExtenderImpl) RunReservationScorePlugins(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, reservations []*schedulingv1alpha1.Reservation, nodeName string) (ps PluginToReservationScores, status *framework.Status) {
	if len(reservations) == 0 {
		return
	}
	pluginToReservationScores := make(PluginToReservationScores, len(ext.reservationScorePlugins))
	for _, pl := range ext.reservationScorePlugins {
		pluginToReservationScores[pl.Name()] = make(ReservationScoreList, len(reservations))
	}

	for _, pl := range ext.reservationScorePlugins {
		for index, reservation := range reservations {
			s, status := pl.ScoreReservation(ctx, cycleState, pod, reservation, nodeName)
			if !status.IsSuccess() {
				err := fmt.Errorf("plugin %q failed with: %w", pl.Name(), status.AsError())
				return nil, framework.AsStatus(err)
			}
			pluginToReservationScores[pl.Name()][index] = ReservationScore{
				Name:  reservation.Name,
				Score: s,
			}
		}
	}

	// TODO: Should support score normalization
	// TODO: Should support configure weight

	for _, pl := range ext.reservationScorePlugins {
		weight := 1
		reservationScoreList := pluginToReservationScores[pl.Name()]

		for i, reservationScore := range reservationScoreList {
			// return error if score plugin returns invalid score.
			if reservationScore.Score > MaxReservationScore || reservationScore.Score < MinReservationScore {
				err := fmt.Errorf("plugin %q returns an invalid score %v, it should in the range of [%v, %v]", pl.Name(), reservationScore.Score, MinReservationScore, MaxReservationScore)
				return nil, framework.AsStatus(err)
			}
			reservationScoreList[i].Score = reservationScore.Score * int64(weight)
		}
	}

	return pluginToReservationScores, nil
}
