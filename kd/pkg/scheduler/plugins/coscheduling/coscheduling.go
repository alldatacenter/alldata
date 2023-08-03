/*
Copyright 2022 The Koordinator Authors.
Copyright 2020 The Kubernetes Authors.

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

package coscheduling

import (
	"context"
	"fmt"
	"time"

	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	corev1helpers "k8s.io/component-helpers/scheduling/corev1"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling"
	pgclientset "sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned"
	pgformers "sigs.k8s.io/scheduler-plugins/pkg/generated/informers/externalversions"
	schedinformers "sigs.k8s.io/scheduler-plugins/pkg/generated/informers/externalversions/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/validation"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/core"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/util"
)

// Coscheduling is a plugin that schedules pods in a group.
type Coscheduling struct {
	args             *config.CoschedulingArgs
	frameworkHandler framework.Handle
	pgClient         pgclientset.Interface
	pgInformer       schedinformers.PodGroupInformer
	pgMgr            core.Manager
}

var _ framework.QueueSortPlugin = &Coscheduling{}
var _ framework.PreFilterPlugin = &Coscheduling{}
var _ framework.PostFilterPlugin = &Coscheduling{}
var _ framework.PermitPlugin = &Coscheduling{}
var _ framework.ReservePlugin = &Coscheduling{}
var _ framework.PostBindPlugin = &Coscheduling{}
var _ framework.EnqueueExtensions = &Coscheduling{}

const (
	// Name is the name of the plugin used in Registry and configurations.
	Name = "Coscheduling"
)

// New initializes and returns a new Coscheduling plugin.
func New(obj runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	args, ok := obj.(*config.CoschedulingArgs)
	if !ok {
		return nil, fmt.Errorf("want args to be of type CoschedulingArgs, got %T", obj)
	}
	if err := validation.ValidateCoschedulingArgs(args); err != nil {
		return nil, err
	}
	pgClient, ok := handle.(pgclientset.Interface)
	if !ok {
		kubeConfig := *handle.KubeConfig()
		kubeConfig.ContentType = runtime.ContentTypeJSON
		kubeConfig.AcceptContentTypes = runtime.ContentTypeJSON
		pgClient = pgclientset.NewForConfigOrDie(&kubeConfig)
	}
	pgInformerFactory := pgformers.NewSharedInformerFactory(pgClient, 0)
	pgInformer := pgInformerFactory.Scheduling().V1alpha1().PodGroups()

	pgMgr := core.NewPodGroupManager(pgClient, pgInformerFactory, handle.SharedInformerFactory(), args)
	plugin := &Coscheduling{
		args:             args,
		frameworkHandler: handle,
		pgClient:         pgClient,
		pgInformer:       pgInformer,
		pgMgr:            pgMgr,
	}
	return plugin, nil
}

func (cs *Coscheduling) EventsToRegister() []framework.ClusterEvent {
	// To register a custom event, follow the naming convention at:
	// https://git.k8s.io/kubernetes/pkg/scheduler/eventhandlers.go#L403-L410
	pgGVK := fmt.Sprintf("podgroups.v1alpha1.%v", scheduling.GroupName)
	return []framework.ClusterEvent{
		{Resource: framework.Pod, ActionType: framework.Add},
		{Resource: framework.GVK(pgGVK), ActionType: framework.Add | framework.Update},
	}
}

// Name returns name of the plugin. It is used in logs, etc.
func (cs *Coscheduling) Name() string {
	return Name
}

// Less is sorting pods in the scheduling queue in the following order.
// Firstly, compare the priorities of the two pods, the higher priority (if pod's priority is equal,then compare their KoordinatorPriority at labels )is at the front of the queue,
// Secondly, compare creationTimestamp of two pods, if pod belongs to a Gang, then we compare creationTimestamp of the Gang, the one created first will be at the front of the queue.
// Finally, compare pod's namespace, if pod belongs to a Gang, then we compare Gang name.
func (cs *Coscheduling) Less(podInfo1, podInfo2 *framework.QueuedPodInfo) bool {
	prio1 := corev1helpers.PodPriority(podInfo1.Pod)
	prio2 := corev1helpers.PodPriority(podInfo2.Pod)
	if prio1 != prio2 {
		return prio1 > prio2
	}
	subPrio1, err := extension.GetPodSubPriority(podInfo1.Pod.Labels)
	if err != nil {
		klog.ErrorS(err, "GetSubPriority of the pod error", "pod", klog.KObj(podInfo1.Pod))
	}
	subPrio2, err := extension.GetPodSubPriority(podInfo2.Pod.Labels)
	if err != nil {
		klog.ErrorS(err, "GetSubPriority of the pod error", "pod", klog.KObj(podInfo2.Pod))
	}
	if subPrio1 != subPrio2 {
		return subPrio1 > subPrio2
	}

	group1, _ := cs.pgMgr.GetGroupId(podInfo1.Pod)
	group2, _ := cs.pgMgr.GetGroupId(podInfo2.Pod)
	if group1 != group2 {
		return group1 < group2
	}

	isgang1satisfied := cs.pgMgr.IsGangMinSatisfied(podInfo1.Pod)
	isgang2satisfied := cs.pgMgr.IsGangMinSatisfied(podInfo2.Pod)
	if isgang1satisfied != isgang2satisfied {
		return !isgang1satisfied
	}

	creationTime1 := cs.pgMgr.GetCreatTime(podInfo1)
	creationTime2 := cs.pgMgr.GetCreatTime(podInfo2)
	if creationTime1.Equal(creationTime2) {
		return util.GetId(podInfo1.Pod.Namespace, podInfo1.Pod.Name) < util.GetId(podInfo2.Pod.Namespace, podInfo2.Pod.Name)
	}
	return creationTime1.Before(creationTime2)
}

// PreFilter
// if non-strict-mode, we only do step1 and step2:
// i.Check whether childes in Gang has met the requirements of minimum number under each Gang, and reject the pod if negative.
// ii.Check whether the Gang has been timeout(check the pod's annotation,later introduced at Permit section) or is inited, and reject the pod if positive.
// iii.Check whether the Gang has met the scheduleCycleValid check, and reject the pod if negative.
// iv.Try update scheduleCycle, scheduleCycleValid, childrenScheduleRoundMap as mentioned above.
func (cs *Coscheduling) PreFilter(ctx context.Context, state *framework.CycleState, pod *v1.Pod) *framework.Status {
	// If PreFilter fails, return framework.Error to avoid
	// any preemption attempts.
	if err := cs.pgMgr.PreFilter(ctx, pod); err != nil {
		klog.ErrorS(err, "PreFilter failed", "pod", klog.KObj(pod))
		return framework.AsStatus(err)
	}
	return framework.NewStatus(framework.Success, "")
}

// PostFilter
// i. If strict-mode, we will set scheduleCycleValid to false and release all assumed pods.
// ii. If non-strict mode, we will do nothing.
func (cs *Coscheduling) PostFilter(ctx context.Context, state *framework.CycleState, pod *v1.Pod,
	filteredNodeStatusMap framework.NodeToStatusMap) (*framework.PostFilterResult, *framework.Status) {
	return cs.pgMgr.PostFilter(ctx, pod, cs.frameworkHandler, Name)
}

// PreFilterExtensions returns a PreFilterExtensions interface if the plugin implements one.
func (cs *Coscheduling) PreFilterExtensions() framework.PreFilterExtensions {
	return nil
}

// Permit
// we will calculate all Gangs in GangGroup whether the current number of assumed-pods in each Gang meets the Gang's minimum requirement.
// and decide whether we should let the pod wait in Permit stage or let the whole gangGroup go binding
func (cs *Coscheduling) Permit(ctx context.Context, state *framework.CycleState, pod *v1.Pod, nodeName string) (*framework.Status, time.Duration) {
	waitTime, s := cs.pgMgr.Permit(ctx, pod)
	var retStatus *framework.Status
	switch s {
	case core.PodGroupNotSpecified:
		return framework.NewStatus(framework.Success, ""), 0
	case core.PodGroupNotFound:
		return framework.NewStatus(framework.Unschedulable, "Gang not found"), 0
	case core.Wait:
		klog.InfoS("Pod is waiting to be scheduled at Permit stage", "gang",
			util.GetId(pod.Namespace, util.GetGangNameByPod(pod)), "pod", klog.KObj(pod))
		retStatus = framework.NewStatus(framework.Wait)
		// We will also request to move the sibling pods back to activeQ.
		cs.pgMgr.ActivateSiblings(pod, state)
	case core.Success:
		cs.pgMgr.AllowGangGroup(pod, cs.frameworkHandler, Name)
		retStatus = framework.NewStatus(framework.Success)
		waitTime = 0
	}
	return retStatus, waitTime
}

// Reserve is the functions invoked by the framework at "reserve" extension point.
func (cs *Coscheduling) Reserve(ctx context.Context, state *framework.CycleState, pod *v1.Pod, nodeName string) *framework.Status {
	return nil
}

// Unreserve
// i. handle the timeout gang
// ii. do nothing when bound failed
func (cs *Coscheduling) Unreserve(ctx context.Context, state *framework.CycleState, pod *v1.Pod, nodeName string) {
	cs.pgMgr.Unreserve(ctx, state, pod, nodeName, cs.frameworkHandler, Name)
}

// PostBind is called after a pod is successfully bound. These plugins are used update PodGroup when pod is bound.
func (cs *Coscheduling) PostBind(ctx context.Context, _ *framework.CycleState, pod *v1.Pod, nodeName string) {
	cs.pgMgr.PostBind(ctx, pod, nodeName)
}
