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

package deviceshare

import (
	"context"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/api/v1/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const (
	// Name is the name of the plugin used in the plugin registry and configurations.
	Name = "DeviceShare"

	// stateKey is the key in CycleState to pre-computed data.
	stateKey = Name

	// ErrMissingDevice when node does not have Device.
	ErrMissingDevice = "node(s) missing Device"

	// ErrInsufficientDevices when node can't satisfy Pod's requested resource.
	ErrInsufficientDevices = "Insufficient Devices"
)

type Plugin struct {
	handle          framework.Handle
	nodeDeviceCache *nodeDeviceCache
}

var (
	_ framework.PreFilterPlugin = &Plugin{}
	_ framework.FilterPlugin    = &Plugin{}
	_ framework.ReservePlugin   = &Plugin{}
	_ framework.PreBindPlugin   = &Plugin{}
)

type preFilterState struct {
	skip                    bool
	allocationResult        apiext.DeviceAllocations
	convertedDeviceResource corev1.ResourceList
}

func (s *preFilterState) Clone() framework.StateData {
	return s
}

func (g *Plugin) Name() string {
	return Name
}

func (g *Plugin) PreFilter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod) *framework.Status {
	state := &preFilterState{
		skip:                    true,
		convertedDeviceResource: make(corev1.ResourceList),
	}

	podRequest, _ := resource.PodRequestsAndLimits(pod)

	for deviceType := range DeviceResourceNames {
		switch deviceType {
		case schedulingv1alpha1.GPU:
			if !hasDeviceResource(podRequest, deviceType) {
				break
			}
			combination, err := ValidateGPURequest(podRequest)
			if err != nil {
				return framework.NewStatus(framework.Error, err.Error())
			}
			state.convertedDeviceResource = quotav1.Add(
				state.convertedDeviceResource,
				ConvertGPUResource(podRequest, combination),
			)
			state.skip = false
		case schedulingv1alpha1.RDMA, schedulingv1alpha1.FPGA:
			if !hasDeviceResource(podRequest, deviceType) {
				break
			}
			if err := validateCommonDeviceRequest(podRequest, deviceType); err != nil {
				return framework.NewStatus(framework.Error, err.Error())
			}
			state.convertedDeviceResource = quotav1.Add(
				state.convertedDeviceResource,
				convertCommonDeviceResource(podRequest, deviceType),
			)
			state.skip = false
		default:
			klog.Warningf("device type %v is not supported yet", deviceType)
		}
	}

	cycleState.Write(stateKey, state)
	return nil
}

func (g *Plugin) PreFilterExtensions() framework.PreFilterExtensions {
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

func (g *Plugin) Filter(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) *framework.Status {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return status
	}
	if state.skip {
		return nil
	}

	if nodeInfo.Node() == nil {
		return framework.NewStatus(framework.Error, "node not found")
	}

	nodeDeviceInfo := g.nodeDeviceCache.getNodeDevice(nodeInfo.Node().Name)
	if nodeDeviceInfo == nil {
		return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrMissingDevice)
	}

	podRequest := state.convertedDeviceResource

	nodeDeviceInfo.lock.RLock()
	defer nodeDeviceInfo.lock.RUnlock()

	allocateResult, err := nodeDeviceInfo.tryAllocateDevice(podRequest)
	if len(allocateResult) != 0 && err == nil {
		return nil
	}

	return framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices)
}

func (g *Plugin) Reserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return status
	}
	if state.skip {
		return nil
	}

	nodeDeviceInfo := g.nodeDeviceCache.getNodeDevice(nodeName)
	if nodeDeviceInfo == nil {
		return framework.NewStatus(framework.UnschedulableAndUnresolvable, ErrMissingDevice)
	}

	podRequest := state.convertedDeviceResource

	nodeDeviceInfo.lock.Lock()
	defer nodeDeviceInfo.lock.Unlock()

	allocateResult, err := nodeDeviceInfo.tryAllocateDevice(podRequest)
	if err != nil || len(allocateResult) == 0 {
		return framework.NewStatus(framework.Unschedulable, ErrInsufficientDevices)
	}

	nodeDeviceInfo.updateCacheUsed(allocateResult, pod, true)

	state.allocationResult = allocateResult
	return nil
}

func (g *Plugin) Unreserve(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return
	}
	if state.skip {
		return
	}

	nodeDeviceInfo := g.nodeDeviceCache.getNodeDevice(nodeName)
	if nodeDeviceInfo == nil {
		return
	}

	nodeDeviceInfo.lock.Lock()
	defer nodeDeviceInfo.lock.Unlock()

	nodeDeviceInfo.updateCacheUsed(state.allocationResult, pod, false)

	state.allocationResult = nil
}

func (g *Plugin) PreBind(ctx context.Context, cycleState *framework.CycleState, pod *corev1.Pod, nodeName string) *framework.Status {
	state, status := getPreFilterState(cycleState)
	if !status.IsSuccess() {
		return status
	}
	if state.skip {
		return nil
	}

	allocResult := state.allocationResult
	newPod := pod.DeepCopy()
	if err := apiext.SetDeviceAllocations(newPod, allocResult); err != nil {
		return framework.NewStatus(framework.Error, err.Error())
	}

	// NOTE: APIServer won't allow the following modification. Error: pod updates may not change fields other than
	// `spec.containers[*].image`, `spec.initContainers[*].image`, `spec.activeDeadlineSeconds`,
	// `spec.tolerations` (only additions to existing tolerations) or `spec.terminationGracePeriodSeconds`

	// podRequest := state.convertedDeviceResource
	// if _, ok := allocResult[schedulingv1alpha1.GPU]; ok {
	// 	patchContainerGPUResource(newPod, podRequest)
	// }

	patchBytes, err := util.GeneratePodPatch(pod, newPod)
	if err != nil {
		return framework.NewStatus(framework.Error, err.Error())
	}
	err = util.RetryOnConflictOrTooManyRequests(func() error {
		_, podErr := g.handle.ClientSet().CoreV1().Pods(pod.Namespace).
			Patch(ctx, pod.Name, types.StrategicMergePatchType, patchBytes, metav1.PatchOptions{})
		return podErr
	})
	if err != nil {
		return framework.NewStatus(framework.Error, err.Error())
	}

	return nil
}

func (g *Plugin) getNodeDeviceSummary(nodeName string) (*NodeDeviceSummary, bool) {
	return g.nodeDeviceCache.getNodeDeviceSummary(nodeName)
}

func (g *Plugin) getAllNodeDeviceSummary() map[string]*NodeDeviceSummary {
	return g.nodeDeviceCache.getAllNodeDeviceSummary()
}

func New(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	extendedHandle, ok := handle.(frameworkext.ExtendedHandle)
	if !ok {
		return nil, fmt.Errorf("expect handle to be type frameworkext.ExtendedHandle, got %T", handle)
	}

	deviceCache := newNodeDeviceCache()
	registerDeviceEventHandler(deviceCache, extendedHandle.KoordinatorSharedInformerFactory())
	registerPodEventHandler(deviceCache, handle.SharedInformerFactory())

	return &Plugin{
		handle:          handle,
		nodeDeviceCache: deviceCache,
	}, nil
}
