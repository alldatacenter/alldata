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

package elasticquota

import (
	"context"
	"fmt"
	"sync"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/util/wait"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	v1 "k8s.io/client-go/listers/core/v1"
	policylisters "k8s.io/client-go/listers/policy/v1"
	"k8s.io/client-go/tools/cache"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned"
	"sigs.k8s.io/scheduler-plugins/pkg/generated/informers/externalversions"
	"sigs.k8s.io/scheduler-plugins/pkg/generated/listers/scheduling/v1alpha1"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/validation"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	frameworkexthelper "github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext/helper"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/elasticquota/core"
)

const (
	Name                              = "ElasticQuota"
	MigrateDefaultQuotaGroupsPodCycle = 1 * time.Second
	postFilterKey                     = "PostFilter" + Name
)

type PostFilterState struct {
	quotaInfo *core.QuotaInfo
}

func (p *PostFilterState) Clone() framework.StateData {
	return &PostFilterState{
		quotaInfo: p.quotaInfo.DeepCopy(),
	}
}

type Plugin struct {
	handle      framework.Handle
	client      versioned.Interface
	pluginArgs  *config.ElasticQuotaArgs
	quotaLister v1alpha1.ElasticQuotaLister
	podLister   v1.PodLister
	pdbLister   policylisters.PodDisruptionBudgetLister
	nodeLister  v1.NodeLister
	// only used in OnNodeAdd,in case Recover and normal Watch double call OnNodeAdd
	nodeResourceMapLock sync.Mutex
	nodeResourceMap     map[string]struct{}
	groupQuotaManager   *core.GroupQuotaManager
}

var (
	_ framework.PreFilterPlugin  = &Plugin{}
	_ framework.PostFilterPlugin = &Plugin{}
	_ framework.ReservePlugin    = &Plugin{}
)

func New(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	pluginArgs, ok := args.(*config.ElasticQuotaArgs)
	if !ok {
		return nil, fmt.Errorf("want args to be of type GangSchedulingArgs, got %T", args)
	}
	if err := validation.ValidateElasticQuotaArgs(pluginArgs); err != nil {
		return nil, err
	}

	client, ok := handle.(versioned.Interface)
	if !ok {
		kubeConfig := *handle.KubeConfig()
		kubeConfig.ContentType = runtime.ContentTypeJSON
		kubeConfig.AcceptContentTypes = runtime.ContentTypeJSON
		client = versioned.NewForConfigOrDie(&kubeConfig)
	}
	scheSharedInformerFactory := externalversions.NewSharedInformerFactory(client, 0)
	elasticQuotaInformer := scheSharedInformerFactory.Scheduling().V1alpha1().ElasticQuotas()

	elasticQuota := &Plugin{
		handle:            handle,
		client:            client,
		pluginArgs:        pluginArgs,
		podLister:         handle.SharedInformerFactory().Core().V1().Pods().Lister(),
		quotaLister:       elasticQuotaInformer.Lister(),
		pdbLister:         getPDBLister(handle),
		nodeLister:        handle.SharedInformerFactory().Core().V1().Nodes().Lister(),
		groupQuotaManager: core.NewGroupQuotaManager(pluginArgs.SystemQuotaGroupMax, pluginArgs.DefaultQuotaGroupMax),
		nodeResourceMap:   make(map[string]struct{}),
	}
	if err := core.RunDecorateInit(handle); err != nil {
		return nil, err
	}

	ctx := context.TODO()

	elasticQuota.createSystemQuotaIfNotPresent()
	elasticQuota.createDefaultQuotaIfNotPresent()
	frameworkexthelper.ForceSyncFromInformer(ctx.Done(), scheSharedInformerFactory, elasticQuotaInformer.Informer(), cache.ResourceEventHandlerFuncs{
		AddFunc:    elasticQuota.OnQuotaAdd,
		UpdateFunc: elasticQuota.OnQuotaUpdate,
		DeleteFunc: elasticQuota.OnQuotaDelete,
	})

	nodeInformer := handle.SharedInformerFactory().Core().V1().Nodes().Informer()
	frameworkexthelper.ForceSyncFromInformer(ctx.Done(), handle.SharedInformerFactory(), nodeInformer, cache.ResourceEventHandlerFuncs{
		AddFunc:    elasticQuota.OnNodeAdd,
		UpdateFunc: elasticQuota.OnNodeUpdate,
		DeleteFunc: elasticQuota.OnNodeDelete,
	})

	podInformer := handle.SharedInformerFactory().Core().V1().Pods().Informer()
	frameworkexthelper.ForceSyncFromInformer(ctx.Done(), handle.SharedInformerFactory(), podInformer, cache.ResourceEventHandlerFuncs{
		AddFunc:    elasticQuota.OnPodAdd,
		UpdateFunc: elasticQuota.OnPodUpdate,
		DeleteFunc: elasticQuota.OnPodDelete,
	})

	elasticQuota.migrateDefaultQuotaGroupsPod()

	return elasticQuota, nil
}

func (g *Plugin) Start() {
	go wait.Until(g.migrateDefaultQuotaGroupsPod, MigrateDefaultQuotaGroupsPodCycle, nil)
	klog.Infof("start migrate pod from defaultQuotaGroup")
}

func (g *Plugin) NewControllers() ([]frameworkext.Controller, error) {
	quotaOverUsedRevokeController := NewQuotaOverUsedRevokeController(g.handle.ClientSet(), g.pluginArgs.DelayEvictTime.Duration,
		g.pluginArgs.RevokePodInterval.Duration, g.groupQuotaManager, *g.pluginArgs.MonitorAllQuotas)
	elasticQuotaController := NewElasticQuotaController(g.client, g.quotaLister, g.groupQuotaManager)
	return []frameworkext.Controller{g, quotaOverUsedRevokeController, elasticQuotaController}, nil
}

func (g *Plugin) Name() string {
	return Name
}

func (g *Plugin) PreFilter(ctx context.Context, state *framework.CycleState, pod *corev1.Pod) *framework.Status {
	quotaName := g.getPodAssociateQuotaName(pod)
	quotaInfo := g.groupQuotaManager.GetQuotaInfoByName(quotaName)
	if quotaInfo == nil {
		return framework.NewStatus(framework.Error, fmt.Sprintf("Could not find the specified ElasticQuota"))
	}
	g.snapshotPostFilterState(quotaName, state)
	quotaUsed := quotaInfo.GetUsed()
	quotaRuntime := quotaInfo.GetRuntime()

	pod = core.RunDecoratePod(pod)
	podRequest, _ := resource.PodRequestsAndLimits(pod)
	newUsed := quotav1.Add(podRequest, quotaUsed)

	if isLessEqual, exceedDimensions := quotav1.LessThanOrEqual(newUsed, quotaRuntime); !isLessEqual {
		return framework.NewStatus(framework.Unschedulable, fmt.Sprintf("Scheduling refused due to insufficient quotas, "+
			"quotaName: %v, runtime: %v, used: %v, pod's request: %v, exceedDimensions: %v",
			quotaName, printResourceList(quotaRuntime), printResourceList(quotaUsed), printResourceList(podRequest), exceedDimensions))
	}

	if *g.pluginArgs.EnableCheckParentQuota {
		return g.checkQuotaRecursive(quotaName, []string{quotaName}, podRequest)
	}

	return framework.NewStatus(framework.Success, "")
}

func (g *Plugin) PreFilterExtensions() framework.PreFilterExtensions {
	return g
}

// AddPod is called by the framework while trying to evaluate the impact
// of adding podToAdd to the node while scheduling podToSchedule.
func (g *Plugin) AddPod(ctx context.Context, state *framework.CycleState, podToSchedule *corev1.Pod,
	podInfoToAdd *framework.PodInfo, nodeInfo *framework.NodeInfo) *framework.Status {
	postFilterState, err := getPostFilterState(state)
	if err != nil {
		klog.ErrorS(err, "Failed to read postFilterState from cycleState", "elasticQuotaSnapshotKey", postFilterState)
		return framework.NewStatus(framework.Error, err.Error())
	}
	quotaInfo := postFilterState.quotaInfo
	if err = quotaInfo.UpdatePodIsAssigned(podInfoToAdd.Pod, true); err != nil {
		return framework.NewStatus(framework.Error, err.Error())
	}
	pod := core.RunDecoratePod(podInfoToAdd.Pod)
	podReq, _ := resource.PodRequestsAndLimits(pod)
	quotaInfo.CalculateInfo.Used = quotav1.Add(quotaInfo.CalculateInfo.Used, podReq)
	return framework.NewStatus(framework.Success, "")
}

// RemovePod is called by the framework while trying to evaluate the impact
// of removing podToRemove from the node while scheduling podToSchedule.
func (g *Plugin) RemovePod(ctx context.Context, state *framework.CycleState, podToSchedule *corev1.Pod,
	podInfoToRemove *framework.PodInfo, nodeInfo *framework.NodeInfo) *framework.Status {
	postFilterState, err := getPostFilterState(state)
	if err != nil {
		klog.ErrorS(err, "Failed to read postFilterState from cycleState", "elasticQuotaSnapshotKey", postFilterState)
		return framework.NewStatus(framework.Error, err.Error())
	}
	quotaInfo := postFilterState.quotaInfo
	if err = quotaInfo.UpdatePodIsAssigned(podInfoToRemove.Pod, false); err != nil {
		return framework.NewStatus(framework.Error, err.Error())
	}
	pod := core.RunDecoratePod(podInfoToRemove.Pod)
	podReq, _ := resource.PodRequestsAndLimits(pod)
	quotaInfo.CalculateInfo.Used = quotav1.SubtractWithNonNegativeResult(quotaInfo.CalculateInfo.Used, podReq)
	return framework.NewStatus(framework.Success, "")
}

// PostFilter modify the defaultPreemption, only allow pods in the same quota can preempt others.
func (g *Plugin) PostFilter(ctx context.Context, state *framework.CycleState, pod *corev1.Pod,
	filteredNodeStatusMap framework.NodeToStatusMap) (*framework.PostFilterResult, *framework.Status) {
	nnn, status := g.preempt(ctx, state, pod, filteredNodeStatusMap)
	if !status.IsSuccess() {
		return nil, status
	}
	// This happens when the pod is not eligible for preemption or extenders filtered all candidates.
	if nnn == "" {
		return nil, framework.NewStatus(framework.Unschedulable)
	}

	return &framework.PostFilterResult{NominatedNodeName: nnn}, framework.NewStatus(framework.Success)
}

func (g *Plugin) Reserve(ctx context.Context, state *framework.CycleState, p *corev1.Pod, nodeName string) *framework.Status {
	quotaName := g.getPodAssociateQuotaName(p)
	g.groupQuotaManager.ReservePod(quotaName, p)
	return framework.NewStatus(framework.Success, "")
}

func (g *Plugin) Unreserve(ctx context.Context, state *framework.CycleState, p *corev1.Pod, nodeName string) {
	quotaName := g.getPodAssociateQuotaName(p)
	g.groupQuotaManager.UnreservePod(quotaName, p)
}
