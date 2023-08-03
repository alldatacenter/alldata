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

package core

import (
	"context"
	"fmt"
	"sync"
	"time"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/client-go/informers"
	listerv1 "k8s.io/client-go/listers/core/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	pgformers "sigs.k8s.io/scheduler-plugins/pkg/generated/informers/externalversions"

	"sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
	pgclientset "sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned"
	pglister "sigs.k8s.io/scheduler-plugins/pkg/generated/listers/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	frameworkexthelper "github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/helper"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/coscheduling/util"
)

type Status string

const (
	// PodGroupNotSpecified denotes no PodGroup is specified in the Pod spec.
	PodGroupNotSpecified Status = "PodGroup not specified"
	// PodGroupNotFound denotes the specified PodGroup in the Pod spec is
	// not found in API server.
	PodGroupNotFound Status = "PodGroup not found"
	Success          Status = "Success"
	Wait             Status = "Wait"
)

// Manager defines the interfaces for PodGroup management.
type Manager interface {
	PreFilter(context.Context, *corev1.Pod) error
	Permit(context.Context, *corev1.Pod) (time.Duration, Status)
	PostBind(context.Context, *corev1.Pod, string)
	PostFilter(context.Context, *corev1.Pod, framework.Handle, string) (*framework.PostFilterResult, *framework.Status)
	GetCreatTime(*framework.QueuedPodInfo) time.Time
	GetGroupId(*corev1.Pod) (string, error)
	GetAllPodsFromGang(string) []*corev1.Pod
	ActivateSiblings(*corev1.Pod, *framework.CycleState)
	AllowGangGroup(*corev1.Pod, framework.Handle, string)
	Unreserve(context.Context, *framework.CycleState, *corev1.Pod, string, framework.Handle, string)
	GetGangSummary(gangId string) (*GangSummary, bool)
	GetGangSummaries() map[string]*GangSummary
	IsGangMinSatisfied(*corev1.Pod) bool
}

// PodGroupManager defines the scheduling operation called
type PodGroupManager struct {
	// pgClient is a podGroup client
	pgClient pgclientset.Interface
	// scheduleTimeout is the default timeout for podgroup scheduling.
	// If podgroup's scheduleTimeoutSeconds is set, it will be used.
	scheduleTimeout *time.Duration
	// pgLister is podgroup lister
	pgLister pglister.PodGroupLister
	// podLister is pod lister
	podLister listerv1.PodLister
	// reserveResourcePercentage is the reserved resource for the max finished group, range (0,100]
	reserveResourcePercentage int32
	// cache stores gang info
	cache *GangCache
	sync.RWMutex
}

// NewPodGroupManager creates a new operation object.
func NewPodGroupManager(
	pgClient pgclientset.Interface,
	pgSharedInformerFactory pgformers.SharedInformerFactory,
	sharedInformerFactory informers.SharedInformerFactory,
	args *config.CoschedulingArgs,
) *PodGroupManager {
	pgInformer := pgSharedInformerFactory.Scheduling().V1alpha1().PodGroups()
	podInformer := sharedInformerFactory.Core().V1().Pods()
	gangCache := NewGangCache(args, podInformer.Lister(), pgInformer.Lister(), pgClient)
	pgMgr := &PodGroupManager{
		pgClient:  pgClient,
		pgLister:  pgInformer.Lister(),
		podLister: podInformer.Lister(),
		cache:     gangCache,
	}

	podGroupEventHandler := cache.ResourceEventHandlerFuncs{
		AddFunc:    gangCache.onPodGroupAdd,
		DeleteFunc: gangCache.onPodGroupDelete,
	}
	frameworkexthelper.ForceSyncFromInformer(context.TODO().Done(), pgSharedInformerFactory, pgInformer.Informer(), podGroupEventHandler)

	podEventHandler := cache.ResourceEventHandlerFuncs{
		AddFunc:    gangCache.onPodAdd,
		DeleteFunc: gangCache.onPodDelete,
	}
	frameworkexthelper.ForceSyncFromInformer(context.TODO().Done(), sharedInformerFactory, podInformer.Informer(), podEventHandler)
	return pgMgr
}

func (pgMgr *PodGroupManager) OnPodAdd(obj interface{}) {
	pgMgr.cache.onPodAdd(obj)
}

func (pgMgr *PodGroupManager) OnPodDelete(obj interface{}) {
	pgMgr.cache.onPodDelete(obj)
}

func (pgMgr *PodGroupManager) OnPodGroupAdd(obj interface{}) {
	pgMgr.cache.onPodGroupAdd(obj)
}

func (pgMgr *PodGroupManager) OnPodGroupDelete(obj interface{}) {
	pgMgr.cache.onPodGroupDelete(obj)
}

func (pgMgr *PodGroupManager) GetGroupId(pod *corev1.Pod) (string, error) {
	gang := pgMgr.GetGangByPod(pod)
	if gang == nil {
		return "", fmt.Errorf("gang doesn't exist in cache")
	}

	return gang.GangGroupId, nil
}

func (pgMgr *PodGroupManager) IsGangMinSatisfied(pod *corev1.Pod) bool {
	gang := pgMgr.GetGangByPod(pod)
	if gang == nil {
		return false
	}

	if gang.isGangOnceResourceSatisfied() {
		return true
	}

	return gang.MinRequiredNumber <= gang.getGangAssumedPods()
}

// ActivateSiblings stashes the pods belonging to the same PodGroup of the given pod
// in the given state, with a reserved key "kubernetes.io/pods-to-activate".
func (pgMgr *PodGroupManager) ActivateSiblings(pod *corev1.Pod, state *framework.CycleState) {
	gang := pgMgr.GetGangByPod(pod)
	if gang == nil {
		return
	}
	// iterate over each gangGroup, get all the pods
	gangGroup := gang.getGangGroup()
	toActivePods := make([]*corev1.Pod, 0)
	for _, groupGangId := range gangGroup {
		groupGang := pgMgr.cache.getGangFromCacheByGangId(groupGangId, false)
		if groupGang == nil {
			continue
		}
		toActivePods = append(toActivePods, groupGang.getChildrenFromGang()...)
	}

	if len(toActivePods) != 0 {
		if c, err := state.Read(framework.PodsToActivateKey); err == nil {
			if s, ok := c.(*framework.PodsToActivate); ok {
				s.Lock()
				for _, pod := range toActivePods {
					namespacedName := util.GetId(pod.Namespace, pod.Name)
					s.Map[namespacedName] = pod
					klog.InfoS("ActivateSiblings add pod's key to PodsToActivate map", "pod", namespacedName)
				}
				s.Unlock()
			}
		}
	}
}

// PreFilter
// i.Check whether children in Gang has met the requirements of minimum number under each Gang, and reject the pod if negative.
// ii.Check whether the Gang is inited, and reject the pod if positive.
// iii.Check whether the Gang is OnceResourceSatisfied
// iv.Check whether the Gang has met the scheduleCycleValid check, and reject the pod if negative(only Strict mode ).
// v.Try update scheduleCycle, scheduleCycleValid, childrenScheduleRoundMap as mentioned above.
func (pgMgr *PodGroupManager) PreFilter(ctx context.Context, pod *corev1.Pod) error {
	if !util.IsPodNeedGang(pod) {
		return nil
	}
	gang := pgMgr.GetGangByPod(pod)
	if gang == nil {
		return fmt.Errorf("can't find gang, gangName: %v, podName: %v", util.GetId(pod.Namespace, util.GetGangNameByPod(pod)),
			util.GetId(pod.Namespace, pod.Name))
	}

	// check if gang is initialized
	if !gang.HasGangInit {
		return fmt.Errorf("gang has not init, gangName: %v, podName: %v", gang.Name,
			util.GetId(pod.Namespace, pod.Name))
	}
	// resourceSatisfied means pod will directly pass the PreFilter
	if gang.OnceResourceSatisfied {
		return nil
	}

	// check minNum
	if gang.getChildrenNum() < gang.getGangMinNum() {
		return fmt.Errorf("gang child pod not collect enough, gangName: %v, podName: %v", gang.Name,
			util.GetId(pod.Namespace, pod.Name))
	}

	// first try update the global cycle of gang
	gang.trySetScheduleCycleValid()
	gangScheduleCycle := gang.getScheduleCycle()
	defer gang.setChildScheduleCycle(pod, gangScheduleCycle)

	gangMode := gang.getGangMode()
	if gangMode == extension.GangModeStrict {
		podScheduleCycle := gang.getChildScheduleCycle(pod)
		if !gang.isScheduleCycleValid() {
			return fmt.Errorf("gang scheduleCycle not valid, gangName: %v, podName: %v",
				gang.Name, util.GetId(pod.Namespace, pod.Name))
		}
		if podScheduleCycle >= gangScheduleCycle {
			return fmt.Errorf("pod's schedule cycle too large, gangName: %v, podName: %v, podCycle: %v, gangCycle: %v",
				gang.Name, util.GetId(pod.Namespace, pod.Name), podScheduleCycle, gangScheduleCycle)
		}
	}
	return nil
}

// PostFilter
// i. If strict-mode, we will set scheduleCycleValid to false and release all assumed pods.
// ii. If non-strict mode, we will do nothing.
func (pgMgr *PodGroupManager) PostFilter(ctx context.Context, pod *corev1.Pod, handle framework.Handle, pluginName string) (*framework.PostFilterResult, *framework.Status) {
	if !util.IsPodNeedGang(pod) {
		return &framework.PostFilterResult{}, framework.NewStatus(framework.Unschedulable, "")
	}
	gang := pgMgr.GetGangByPod(pod)
	if gang == nil {
		klog.InfoS("Pod does not belong to any gang", "pod", klog.KObj(pod))
		return &framework.PostFilterResult{}, framework.NewStatus(framework.Unschedulable, "can not find gang")
	}
	if gang.isGangOnceResourceSatisfied() {
		return &framework.PostFilterResult{}, framework.NewStatus(framework.Unschedulable, "")
	}

	if gang.getGangMode() == extension.GangModeStrict {
		pgMgr.rejectGangGroupById(pluginName, gang.Name, handle)
		return &framework.PostFilterResult{}, framework.NewStatus(framework.Unschedulable,
			fmt.Sprintf("Gang: %v gets rejected this cycle due to Pod: %v is unschedulable even after "+
				"PostFilter in StrictMode", gang.Name, pod.Name))
	}

	return &framework.PostFilterResult{}, framework.NewStatus(framework.Unschedulable, "")
}

// Permit
// we will calculate all Gangs in GangGroup whether the current number of assumed-pods in each Gang meets the Gang's minimum requirement.
// and decide whether we should let the pod wait in Permit stage or let the whole gangGroup go binding
func (pgMgr *PodGroupManager) Permit(ctx context.Context, pod *corev1.Pod) (time.Duration, Status) {
	if !util.IsPodNeedGang(pod) {
		return 0, PodGroupNotSpecified
	}
	gang := pgMgr.GetGangByPod(pod)

	if gang == nil {
		klog.InfoS("Pod does not belong to any gang", "pod", klog.KObj(pod))
		return 0, PodGroupNotFound
	}
	// first add pod to the gang's WaitingPodsMap
	gang.addAssumedPod(pod)

	gangGroup := gang.getGangGroup()
	allGangGroupAssumed := true
	// check each gang group
	for _, groupName := range gangGroup {
		gangTmp := pgMgr.cache.getGangFromCacheByGangId(groupName, false)
		if gangTmp == nil || !gangTmp.isGangValidForPermit() {
			allGangGroupAssumed = false
			break
		}
	}
	if !allGangGroupAssumed {
		return gang.WaitTime, Wait
	}
	return 0, Success
}

// Unreserve
// if gang is resourceSatisfied, we only delAssumedPod
// if gang is not resourceSatisfied and is in StrictMode, we release all the assumed pods
func (pgMgr *PodGroupManager) Unreserve(ctx context.Context, state *framework.CycleState, pod *corev1.Pod, nodeName string, handle framework.Handle, pluginName string) {
	if !util.IsPodNeedGang(pod) {
		return
	}
	gang := pgMgr.GetGangByPod(pod)
	if gang == nil {
		klog.InfoS("Pod does not belong to any gang", "pod", klog.KObj(pod))
		return
	}
	// first delete the pod from gang's waitingFroBindChildren map
	gang.delAssumedPod(pod)

	if !gang.isGangOnceResourceSatisfied() && gang.getGangMode() == extension.GangModeStrict {
		pgMgr.rejectGangGroupById(pluginName, gang.Name, handle)
	}
}

func (pgMgr *PodGroupManager) rejectGangGroupById(pluginName, gangId string, handle framework.Handle) {
	gang := pgMgr.cache.getGangFromCacheByGangId(gangId, false)
	if gang == nil {
		return
	}

	// iterate over each gangGroup, get all the pods
	gangGroup := gang.getGangGroup()
	gangSet := sets.NewString(gangGroup...)

	if handle != nil {
		handle.IterateOverWaitingPods(func(waitingPod framework.WaitingPod) {
			waitingGangId := util.GetId(waitingPod.GetPod().Namespace,
				util.GetGangNameByPod(waitingPod.GetPod()))
			if gangSet.Has(waitingGangId) {
				klog.V(1).InfoS("ganggroup rejects the pod", "gang", waitingGangId, "pod", klog.KObj(waitingPod.GetPod()))
				waitingPod.Reject(pluginName, "gang rejection by another ganggroup")
			}
		})
	}
	for gang := range gangSet {
		gangIns := pgMgr.cache.getGangFromCacheByGangId(gang, false)
		if gangIns != nil {
			gangIns.setScheduleCycleValid(false)
		}
	}
}

// PostBind updates a PodGroup's status.
func (pgMgr *PodGroupManager) PostBind(ctx context.Context, pod *corev1.Pod, nodeName string) {
	if !util.IsPodNeedGang(pod) {
		return
	}
	gang := pgMgr.GetGangByPod(pod)
	if gang == nil {
		klog.InfoS("Pod does not belong to any gang", "pod", klog.KObj(pod))
		return
	}
	// first update gang in cache
	gang.addBoundPod(pod)

	//  update PodGroup
	_, pg := pgMgr.GetPodGroup(pod)
	if pg == nil {
		return
	}
	pgCopy := pg.DeepCopy()

	pgCopy.Status.Scheduled = int32(gang.getBoundPodNum())

	if pgCopy.Status.Scheduled >= pgCopy.Spec.MinMember {
		pgCopy.Status.Phase = v1alpha1.PodGroupScheduled
		klog.InfoS("PostBind has got enough bound child for gang", "gang", gang.Name, "pod", klog.KObj(pod))
	} else {
		pgCopy.Status.Phase = v1alpha1.PodGroupScheduling
		klog.InfoS("PostBind has not got enough bound child for gang", "gang", gang.Name, "pod", klog.KObj(pod))
		if pgCopy.Status.ScheduleStartTime.IsZero() {
			pgCopy.Status.ScheduleStartTime = metav1.Time{Time: time.Now()}
		}
	}
	if pgCopy.Status.Phase != pg.Status.Phase {
		pg, err := pgMgr.pgLister.PodGroups(pgCopy.Namespace).Get(pgCopy.Name)
		if err != nil {
			klog.ErrorS(err, "PosFilter failed to get PodGroup", "podGroup", klog.KObj(pgCopy))
			return
		}
		patch, err := util.CreateMergePatch(pg, pgCopy)
		if err != nil {
			klog.ErrorS(err, "PostFilter failed to create merge patch", "podGroup", klog.KObj(pg), "podGroup", klog.KObj(pgCopy))
			return
		}
		if err := pgMgr.PatchPodGroup(pg.Name, pg.Namespace, patch); err != nil {
			klog.ErrorS(err, "PostFilter Failed to patch", "podGroup", klog.KObj(pg))
			return
		} else {
			klog.InfoS("PostFilter success to patch podGroup", "podGroup", klog.KObj(pgCopy))
		}
	}

}

func (pgMgr *PodGroupManager) GetCreatTime(podInfo *framework.QueuedPodInfo) time.Time {
	// first check if the pod belongs to the Gang
	// it doesn't belong to the gang,we get the creation time of the pod
	if !util.IsPodNeedGang(podInfo.Pod) {
		return podInfo.InitialAttemptTimestamp
	}
	gang := pgMgr.GetGangByPod(podInfo.Pod)
	// it belongs to a gang,we get the creation time of the Gang
	if gang != nil {
		return gang.CreateTime
	}
	klog.Errorf("getCreatTime didn't find gang: %v in gangCache, pod name: %v",
		util.GetId(podInfo.Pod.Namespace, util.GetGangNameByPod(podInfo.Pod)), podInfo.Pod.Name)
	return time.Now()
}

// PatchPodGroup patches a podGroup.
func (pgMgr *PodGroupManager) PatchPodGroup(pgName string, namespace string, patch []byte) error {
	if len(patch) == 0 {
		return nil
	}
	_, err := pgMgr.pgClient.SchedulingV1alpha1().PodGroups(namespace).Patch(context.TODO(), pgName,
		types.MergePatchType, patch, metav1.PatchOptions{})
	return err
}

// GetPodGroup returns the PodGroup that a Pod belongs to in cache.
func (pgMgr *PodGroupManager) GetPodGroup(pod *corev1.Pod) (string, *v1alpha1.PodGroup) {
	pgName := util.GetGangNameByPod(pod)
	if len(pgName) == 0 {
		return "", nil
	}
	pg, err := pgMgr.pgLister.PodGroups(pod.Namespace).Get(pgName)
	if err != nil {
		return fmt.Sprintf("%v/%v", pod.Namespace, pgName), nil
	}
	return fmt.Sprintf("%v/%v", pod.Namespace, pgName), pg
}

func (pgMgr *PodGroupManager) AllowGangGroup(pod *corev1.Pod, handle framework.Handle, pluginName string) {
	gang := pgMgr.GetGangByPod(pod)
	if gang == nil {
		klog.InfoS("Pod does not belong to any gang", "pod", klog.KObj(pod))
		return
	}

	gangSlices := gang.getGangGroup()

	handle.IterateOverWaitingPods(func(waitingPod framework.WaitingPod) {
		podGangId := util.GetId(waitingPod.GetPod().Namespace,
			util.GetGangNameByPod(waitingPod.GetPod()))
		for _, gangIdTmp := range gangSlices {
			if podGangId == gangIdTmp {
				klog.InfoS("Permit allows pod from gang", "gang", podGangId, "pod", klog.KObj(waitingPod.GetPod()))
				waitingPod.Allow(pluginName)
				break
			}
		}
	})

}

func (pgMgr *PodGroupManager) GetGangByPod(pod *corev1.Pod) *Gang {
	gangName := util.GetGangNameByPod(pod)
	if gangName == "" {
		return nil
	}
	gangId := util.GetId(pod.Namespace, gangName)
	gang := pgMgr.cache.getGangFromCacheByGangId(gangId, false)
	return gang
}

func (pgMgr *PodGroupManager) GetAllPodsFromGang(gangId string) []*corev1.Pod {
	pods := make([]*corev1.Pod, 0)
	gang := pgMgr.cache.getGangFromCacheByGangId(gangId, false)
	if gang == nil {
		return pods
	}
	pods = gang.getChildrenFromGang()
	return pods
}

func (pgMgr *PodGroupManager) GetGangSummary(gangId string) (*GangSummary, bool) {
	gang := pgMgr.cache.getGangFromCacheByGangId(gangId, false)
	if gang == nil {
		return nil, false
	}
	return gang.GetGangSummary(), true
}

func (pgMgr *PodGroupManager) GetGangSummaries() map[string]*GangSummary {
	result := make(map[string]*GangSummary)
	allGangs := pgMgr.cache.getAllGangsFromCache()
	for gangName, gang := range allGangs {
		result[gangName] = gang.GetGangSummary()
	}

	return result
}
