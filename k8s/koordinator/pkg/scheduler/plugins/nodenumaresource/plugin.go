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

package nodenumaresource

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/klog/v2"
	resourceapi "k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingconfig "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/util"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

const (
	Name     = "NodeNUMAResource"
	stateKey = Name
)

const (
	// socketScoreWeight controls the range of the final score when scoring according to the NUMA Socket dimension.
	// the NUMA Socket dimension score formulas: nodeFinalScore = math.Log(numaSocketFinalScore) * socketScoreWeight
	// use the prime number 7, we can get the range of the final score = [0, 32.23]
	socketScoreWeight = 7
)

const (
	ErrNotFoundCPUTopology     = "node(s) CPU Topology not found"
	ErrInvalidCPUTopology      = "node(s) invalid CPU Topology"
	ErrSMTAlignmentError       = "node(s) requested cpus not multiple cpus per core"
	ErrRequiredFullPCPUsPolicy = "node(s) required FullPCPUs policy"
)

var (
	GetResourceSpec   = extension.GetResourceSpec
	GetResourceStatus = extension.GetResourceStatus
	SetResourceStatus = extension.SetResourceStatus
	GetPodQoSClass    = extension.GetPodQoSClass
	GetPriorityClass  = extension.GetPriorityClass
	AllowUseCPUSet    = func(pod *corev1.Pod) bool {
		if pod == nil {
			return false
		}
		qosClass := GetPodQoSClass(pod)
		priorityClass := GetPriorityClass(pod)
		return (qosClass == extension.QoSLSE || qosClass == extension.QoSLSR) && priorityClass == extension.PriorityProd
	}
)

var (
	_ framework.PreFilterPlugin = &Plugin{}
	_ framework.FilterPlugin    = &Plugin{}
	_ framework.ScorePlugin     = &Plugin{}
	_ framework.ReservePlugin   = &Plugin{}
	_ framework.PreBindPlugin   = &Plugin{}
)

type Plugin struct {
	handle          framework.Handle
	pluginArgs      *schedulingconfig.NodeNUMAResourceArgs
	topologyManager CPUTopologyManager
	cpuManager      CPUManager
}

type Option func(*pluginOptions)

type pluginOptions struct {
	topologyManager    CPUTopologyManager
	customSyncTopology bool
	cpuManager         CPUManager
}

func WithCPUTopologyManager(topologyManager CPUTopologyManager) Option {
	return func(opts *pluginOptions) {
		opts.topologyManager = topologyManager
	}
}

func WithCustomSyncTopology(customSyncTopology bool) Option {
	return func(options *pluginOptions) {
		options.customSyncTopology = customSyncTopology
	}
}

func WithCPUManager(cpuManager CPUManager) Option {
	return func(opts *pluginOptions) {
		opts.cpuManager = cpuManager
	}
}

func NewWithOptions(args runtime.Object, handle framework.Handle, opts ...Option) (framework.Plugin, error) {
	pluginArgs, ok := args.(*schedulingconfig.NodeNUMAResourceArgs)
	if !ok {
		return nil, fmt.Errorf("want args to be of type NodeNUMAResourceArgs, got %T", args)
	}

	options := &pluginOptions{}
	for _, optFnc := range opts {
		optFnc(options)
	}

	if options.topologyManager == nil {
		options.topologyManager = NewCPUTopologyManager()
	}

	if options.cpuManager == nil {
		defaultNUMAAllocateStrategy := GetDefaultNUMAAllocateStrategy(pluginArgs)
		options.cpuManager = NewCPUManager(handle, defaultNUMAAllocateStrategy, options.topologyManager)
	}

	if !options.customSyncTopology {
		if err := registerNodeResourceTopologyEventHandler(handle, options.topologyManager); err != nil {
			return nil, err
		}
	}
	registerPodEventHandler(handle, options.cpuManager)

	return &Plugin{
		handle:          handle,
		pluginArgs:      pluginArgs,
		topologyManager: options.topologyManager,
		cpuManager:      options.cpuManager,
	}, nil
}

func New(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	return NewWithOptions(args, handle)
}

func GetDefaultNUMAAllocateStrategy(pluginArgs *schedulingconfig.NodeNUMAResourceArgs) schedulingconfig.NUMAAllocateStrategy {
	numaAllocateStrategy := schedulingconfig.NUMAMostAllocated
	if pluginArgs != nil && pluginArgs.ScoringStrategy != nil && pluginArgs.ScoringStrategy.Type == schedulingconfig.LeastAllocated {
		numaAllocateStrategy = schedulingconfig.NUMALeastAllocated
	}
	return numaAllocateStrategy
}

func (p *Plugin) Name() string { return Name }

func (p *Plugin) GetCPUManager() CPUManager {
	return p.cpuManager
}

func (p *Plugin) GetCPUTopologyManager() CPUTopologyManager {
	return p.topologyManager
}

type preFilterState struct {
	skip                        bool
	resourceSpec                *extension.ResourceSpec
	preferredCPUBindPolicy      schedulingconfig.CPUBindPolicy
	preferredCPUExclusivePolicy schedulingconfig.CPUExclusivePolicy
	numCPUsNeeded               int
	allocatedCPUs               cpuset.CPUSet
}

func (s *preFilterState) Clone() framework.StateData {
	return &preFilterState{
		skip:          s.skip,
		resourceSpec:  s.resourceSpec,
		allocatedCPUs: s.allocatedCPUs.Clone(),
	}
}

func (p *Plugin) PreFilter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod) *framework.Status {
	resourceSpec, err := GetResourceSpec(pod.Annotations)
	if err != nil {
		return framework.NewStatus(framework.Error, err.Error())
	}

	state := &preFilterState{
		skip: true,
	}
	if AllowUseCPUSet(pod) {
		preferredCPUBindPolicy := resourceSpec.PreferredCPUBindPolicy
		if preferredCPUBindPolicy == "" || preferredCPUBindPolicy == schedulingconfig.CPUBindPolicyDefault {
			preferredCPUBindPolicy = p.pluginArgs.DefaultCPUBindPolicy
		}
		if preferredCPUBindPolicy == schedulingconfig.CPUBindPolicyFullPCPUs ||
			preferredCPUBindPolicy == schedulingconfig.CPUBindPolicySpreadByPCPUs {
			requests, _ := resourceapi.PodRequestsAndLimits(pod)
			requestedCPU := requests.Cpu().MilliValue()
			if requestedCPU%1000 != 0 {
				return framework.NewStatus(framework.Error, "the requested CPUs must be integer")
			}

			if requestedCPU > 0 {
				state.skip = false
				state.resourceSpec = resourceSpec
				state.preferredCPUBindPolicy = preferredCPUBindPolicy
				state.preferredCPUExclusivePolicy = resourceSpec.PreferredCPUExclusivePolicy
				state.numCPUsNeeded = int(requestedCPU / 1000)
			}
		}
	}

	cycleState.Write(stateKey, state)
	return nil
}

func (p *Plugin) PreFilterExtensions() framework.PreFilterExtensions {
	return nil
}

func getPreFilterState(cycleState *framework.CycleState) (*preFilterState, *framework.Status) {
	value, err := cycleState.Read(stateKey)
	if err != nil {
		return nil, framework.AsStatus(err)
	}
	state := value.(*preFilterState)
	return state, nil
}

func (p *Plugin) Filter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) *framework.Status {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return status
	}
	if state.skip {
		return nil
	}

	node := nodeInfo.Node()
	if node == nil {
		return framework.NewStatus(framework.Error, "node not found")
	}

	cpuTopologyOptions := p.topologyManager.GetCPUTopologyOptions(node.Name)
	if cpuTopologyOptions.CPUTopology == nil {
		return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrNotFoundCPUTopology)
	}

	// It's necessary to force node to have NodeResourceTopology and CPUTopology
	// We must satisfy the user's CPUSet request. Even if some nodes in the cluster have resources,
	// they cannot be allocated without valid CPU topology.
	if !cpuTopologyOptions.CPUTopology.IsValid() {
		return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrInvalidCPUTopology)
	}

	kubeletCPUPolicy := cpuTopologyOptions.Policy
	if extension.GetNodeCPUBindPolicy(node.Labels, kubeletCPUPolicy) == extension.NodeCPUBindPolicyFullPCPUsOnly {
		if state.numCPUsNeeded%cpuTopologyOptions.CPUTopology.CPUsPerCore() != 0 {
			return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrSMTAlignmentError)
		}
		if state.preferredCPUBindPolicy != schedulingconfig.CPUBindPolicyFullPCPUs {
			return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrRequiredFullPCPUsPolicy)
		}
	}

	return nil
}

func (p *Plugin) Score(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) (int64, *framework.Status) {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return 0, status
	}
	if state.skip {
		return 0, nil
	}

	nodeInfo, err := p.handle.SnapshotSharedLister().NodeInfos().Get(nodeName)
	if err != nil {
		return 0, framework.NewStatus(framework.Error, fmt.Sprintf("getting node %q from Snapshot: %v", nodeName, err))
	}
	node := nodeInfo.Node()
	if node == nil {
		return 0, framework.NewStatus(framework.Error, "node not found")
	}

	preferredCPUBindPolicy, err := p.getPreferredCPUBindPolicy(node, state.preferredCPUBindPolicy)
	if err != nil {
		return 0, nil
	}

	score := p.cpuManager.Score(node, state.numCPUsNeeded, preferredCPUBindPolicy, state.preferredCPUExclusivePolicy)
	return score, nil
}

func (p *Plugin) ScoreExtensions() framework.ScoreExtensions {
	return nil
}

func (p *Plugin) Reserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return status
	}
	if state.skip {
		return nil
	}

	nodeInfo, err := p.handle.SnapshotSharedLister().NodeInfos().Get(nodeName)
	if err != nil {
		return framework.NewStatus(framework.Error, fmt.Sprintf("getting node %q from Snapshot: %v", nodeName, err))
	}
	node := nodeInfo.Node()
	if node == nil {
		return framework.NewStatus(framework.Error, "node not found")
	}

	preferredCPUBindPolicy, err := p.getPreferredCPUBindPolicy(node, state.preferredCPUBindPolicy)
	if err != nil {
		return framework.AsStatus(err)
	}
	result, err := p.cpuManager.Allocate(node, state.numCPUsNeeded, preferredCPUBindPolicy, state.preferredCPUExclusivePolicy)
	if err != nil {
		return framework.AsStatus(err)
	}
	p.cpuManager.UpdateAllocatedCPUSet(nodeName, pod.UID, result, state.preferredCPUExclusivePolicy)
	state.allocatedCPUs = result
	state.preferredCPUBindPolicy = preferredCPUBindPolicy
	return nil
}

func (p *Plugin) Unreserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return
	}
	if state.skip || state.allocatedCPUs.IsEmpty() {
		return
	}
	p.cpuManager.Free(nodeName, pod.UID)
}

func (p *Plugin) PreBind(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return status
	}
	if state.skip {
		return nil
	}

	if state.allocatedCPUs.IsEmpty() {
		return nil
	}

	podOriginal := pod
	pod = pod.DeepCopy()

	// Write back ResourceSpec annotation if LSR Pod hasn't specified CPUBindPolicy
	if state.resourceSpec.PreferredCPUBindPolicy == "" ||
		state.resourceSpec.PreferredCPUBindPolicy == schedulingconfig.CPUBindPolicyDefault ||
		state.resourceSpec.PreferredCPUBindPolicy != state.preferredCPUBindPolicy {
		resourceSpec := &extension.ResourceSpec{
			PreferredCPUBindPolicy: state.preferredCPUBindPolicy,
		}
		resourceSpecData, err := json.Marshal(resourceSpec)
		if err != nil {
			return framework.NewStatus(framework.Error, err.Error())
		}
		if pod.Annotations == nil {
			pod.Annotations = make(map[string]string)
		}
		pod.Annotations[extension.AnnotationResourceSpec] = string(resourceSpecData)
	}

	resourceStatus := &extension.ResourceStatus{CPUSet: state.allocatedCPUs.String()}
	err := SetResourceStatus(pod, resourceStatus)
	if err != nil {
		return framework.NewStatus(framework.Error, err.Error())
	}

	// patch pod or reservation (if the pod is a reserve pod) with new annotations
	err = util.RetryOnConflictOrTooManyRequests(func() error {
		_, err1 := util.NewPatch().WithHandle(p.handle).AddAnnotations(pod.Annotations).PatchPodOrReservation(podOriginal)
		return err1
	})
	if err != nil {
		klog.V(3).ErrorS(err, "Failed to preBind Pod with CPUSet",
			"pod", klog.KObj(pod), "CPUSet", state.allocatedCPUs, "node", nodeName)
		return framework.NewStatus(framework.Error, err.Error())
	}

	klog.V(4).Infof("Successfully preBind Pod %s/%s with CPUSet %s", pod.Namespace, pod.Name, state.allocatedCPUs)
	return nil
}

func (p *Plugin) getPreferredCPUBindPolicy(node *corev1.Node, preferredCPUBindPolicy schedulingconfig.CPUBindPolicy) (schedulingconfig.CPUBindPolicy, error) {
	cpuTopologyOptions := p.topologyManager.GetCPUTopologyOptions(node.Name)
	if cpuTopologyOptions.CPUTopology == nil {
		return preferredCPUBindPolicy, errors.New(ErrNotFoundCPUTopology)
	}
	if !cpuTopologyOptions.CPUTopology.IsValid() {
		return preferredCPUBindPolicy, errors.New(ErrInvalidCPUTopology)
	}

	kubeletCPUPolicy := cpuTopologyOptions.Policy
	nodeCPUBindPolicy := extension.GetNodeCPUBindPolicy(node.Labels, kubeletCPUPolicy)
	switch nodeCPUBindPolicy {
	default:
	case extension.NodeCPUBindPolicyNone:
	case extension.NodeCPUBindPolicySpreadByPCPUs:
		preferredCPUBindPolicy = schedulingconfig.CPUBindPolicySpreadByPCPUs
	case extension.NodeCPUBindPolicyFullPCPUsOnly:
		preferredCPUBindPolicy = schedulingconfig.CPUBindPolicyFullPCPUs
	}
	return preferredCPUBindPolicy, nil
}
