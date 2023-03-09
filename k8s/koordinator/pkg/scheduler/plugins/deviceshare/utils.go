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
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/klog/v2"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

const (
	NvidiaGPUExist = 1 << iota
	KoordGPUExist
	GPUCoreExist
	GPUMemoryExist
	GPUMemoryRatioExist
)

var DeviceResourceNames = map[schedulingv1alpha1.DeviceType][]corev1.ResourceName{
	schedulingv1alpha1.GPU:  {apiext.NvidiaGPU, apiext.KoordGPU, apiext.GPUCore, apiext.GPUMemory, apiext.GPUMemoryRatio},
	schedulingv1alpha1.RDMA: {apiext.KoordRDMA},
	schedulingv1alpha1.FPGA: {apiext.KoordFPGA},
}

func hasDeviceResource(podRequest corev1.ResourceList, deviceType schedulingv1alpha1.DeviceType) bool {
	if podRequest == nil || len(podRequest) == 0 {
		klog.Warningf("skip checking hasDeviceResource, because pod request is empty")
		return false
	}
	for _, resourceName := range DeviceResourceNames[deviceType] {
		if _, ok := podRequest[resourceName]; ok {
			return true
		}
	}
	klog.V(5).Infof("pod does not request %v resource", deviceType)
	return false
}

func validateCommonDeviceRequest(podRequest corev1.ResourceList, deviceType schedulingv1alpha1.DeviceType) error {
	if podRequest == nil || len(podRequest) == 0 {
		return fmt.Errorf("pod request should not be empty")
	}
	var commonDevice resource.Quantity
	switch deviceType {
	case schedulingv1alpha1.FPGA:
		commonDevice = podRequest[apiext.KoordFPGA]
	case schedulingv1alpha1.RDMA:
		commonDevice = podRequest[apiext.KoordRDMA]
	default:
		return fmt.Errorf("device type %v is not supported yet", deviceType)
	}
	if commonDevice.Value() > 100 && commonDevice.Value()%100 != 0 {
		return fmt.Errorf("failed to validate %v%v: %v", apiext.ResourceDomainPrefix, deviceType, commonDevice.Value())
	}
	return nil
}

// ValidateGPURequest uses binary to store each request status.
// For example, 00010 stands for koordinator.sh/gpu exists, and vice versa.
// only 00001 || 00010 || 10100 || 01100 are valid GPU request combination
var ValidateGPURequest = func(podRequest corev1.ResourceList) (uint, error) {
	var gpuCombination uint

	if podRequest == nil || len(podRequest) == 0 {
		return gpuCombination, fmt.Errorf("pod request should not be empty")
	}

	if _, exist := podRequest[apiext.NvidiaGPU]; exist {
		gpuCombination |= NvidiaGPUExist
	}
	if koordGPU, exist := podRequest[apiext.KoordGPU]; exist {
		if koordGPU.Value() > 100 && koordGPU.Value()%100 != 0 {
			return gpuCombination, fmt.Errorf("failed to validate %v: %v", apiext.KoordGPU, koordGPU.Value())
		}
		gpuCombination |= KoordGPUExist
	}
	if gpuCore, exist := podRequest[apiext.GPUCore]; exist {
		// koordinator.sh/gpu-core should be something like: 25, 50, 75, 100, 200, 300
		if gpuCore.Value() > 100 && gpuCore.Value()%100 != 0 {
			return gpuCombination, fmt.Errorf("failed to validate %v: %v", apiext.GPUCore, gpuCore.Value())
		}
		gpuCombination |= GPUCoreExist
	}
	if _, exist := podRequest[apiext.GPUMemory]; exist {
		gpuCombination |= GPUMemoryExist
	}
	if gpuMemRatio, exist := podRequest[apiext.GPUMemoryRatio]; exist {
		if gpuMemRatio.Value() > 100 && gpuMemRatio.Value()%100 != 0 {
			return gpuCombination, fmt.Errorf("failed to validate %v: %v", apiext.GPUMemoryRatio, gpuMemRatio.Value())
		}
		gpuCombination |= GPUMemoryRatioExist
	}

	if gpuCombination == (NvidiaGPUExist) ||
		gpuCombination == (KoordGPUExist) ||
		gpuCombination == (GPUCoreExist|GPUMemoryExist) ||
		gpuCombination == (GPUCoreExist|GPUMemoryRatioExist) {
		return gpuCombination, nil
	}

	return gpuCombination, fmt.Errorf("request is not valid, current combination: %b", gpuCombination)
}

func convertCommonDeviceResource(podRequest corev1.ResourceList, deviceType schedulingv1alpha1.DeviceType) corev1.ResourceList {
	if podRequest == nil || len(podRequest) == 0 {
		klog.Warningf("pod request should not be empty")
		return nil
	}
	var resources corev1.ResourceList
	switch deviceType {
	case schedulingv1alpha1.RDMA:
		if value, ok := podRequest[apiext.KoordRDMA]; ok {
			resources = corev1.ResourceList{
				apiext.KoordRDMA: value,
			}
		}
	case schedulingv1alpha1.FPGA:
		if value, ok := podRequest[apiext.KoordFPGA]; ok {
			resources = corev1.ResourceList{
				apiext.KoordFPGA: value,
			}
		}
	default:
		klog.Warningf("device type %v is not supported yet", deviceType)
		return nil
	}
	return resources
}

// nvidia.com/gpu means applying for full-card
// koordinator.sh/gpu means applying for cards in percentile
// ConvertGPUResource will convert either nvidia.com/gpu or koordinator.sh/gpu to koordinator.sh/gpu-core and koordinator.sh/gpu-memory-ratio
var ConvertGPUResource = func(podRequest corev1.ResourceList, combination uint) corev1.ResourceList {
	if podRequest == nil || len(podRequest) == 0 {
		klog.Warningf("pod request should not be empty")
		return nil
	}
	switch combination {
	case GPUCoreExist | GPUMemoryExist:
		return corev1.ResourceList{
			apiext.GPUCore:   podRequest[apiext.GPUCore],
			apiext.GPUMemory: podRequest[apiext.GPUMemory],
		}
	case GPUCoreExist | GPUMemoryRatioExist:
		return corev1.ResourceList{
			apiext.GPUCore:        podRequest[apiext.GPUCore],
			apiext.GPUMemoryRatio: podRequest[apiext.GPUMemoryRatio],
		}
	case KoordGPUExist:
		return corev1.ResourceList{
			apiext.GPUCore:        podRequest[apiext.KoordGPU],
			apiext.GPUMemoryRatio: podRequest[apiext.KoordGPU],
		}
	case NvidiaGPUExist:
		nvidiaGpu := podRequest[apiext.NvidiaGPU]
		return corev1.ResourceList{
			apiext.GPUCore:        *resource.NewQuantity(nvidiaGpu.Value()*100, resource.DecimalSI),
			apiext.GPUMemoryRatio: *resource.NewQuantity(nvidiaGpu.Value()*100, resource.DecimalSI),
		}
	}
	return nil
}

func isMultipleCommonDevicePod(podRequest corev1.ResourceList, deviceType schedulingv1alpha1.DeviceType) bool {
	if podRequest == nil || len(podRequest) == 0 {
		klog.Warningf("pod request should not be empty")
		return false
	}
	switch deviceType {
	case schedulingv1alpha1.RDMA:
		rdma := podRequest[apiext.KoordRDMA]
		return rdma.Value() > 100 && rdma.Value()%100 == 0
	case schedulingv1alpha1.FPGA:
		fpga := podRequest[apiext.KoordFPGA]
		return fpga.Value() > 100 && fpga.Value()%100 == 0
	default:
		return false
	}
}

func isMultipleGPUPod(podRequest corev1.ResourceList) bool {
	if podRequest == nil || len(podRequest) == 0 {
		klog.Warningf("pod request should not be empty")
		return false
	}
	gpuCore := podRequest[apiext.GPUCore]
	return gpuCore.Value() > 100 && gpuCore.Value()%100 == 0
}

func memRatioToBytes(ratio, totalMemory resource.Quantity) resource.Quantity {
	return *resource.NewQuantity(ratio.Value()*totalMemory.Value()/100, resource.BinarySI)
}

func memBytesToRatio(bytes, totalMemory resource.Quantity) resource.Quantity {
	return *resource.NewQuantity(int64(float64(bytes.Value())/float64(totalMemory.Value())*100), resource.DecimalSI)
}

func patchContainerGPUResource(pod *corev1.Pod, podRequest corev1.ResourceList) {
	// we assume only one container in Pod would request GPU resource
	for _, container := range pod.Spec.Containers {
		var needPatch bool
		reqs := container.Resources.Requests
		if reqs == nil {
			continue
		}
		for _, v := range DeviceResourceNames[schedulingv1alpha1.GPU] {
			if _, ok := reqs[v]; ok {
				needPatch = true
				break
			}
		}
		if needPatch {
			for _, v := range []corev1.ResourceName{apiext.GPUCore, apiext.GPUMemory, apiext.GPUMemoryRatio} {
				reqs[v] = podRequest[v]
			}
			break
		}
	}
}

func fillGPUTotalMem(nodeDeviceTotal deviceResources, podRequest corev1.ResourceList) {
	// nodeDeviceTotal uses the minor of GPU as key. However, under certain circumstances,
	// minor 0 might not exist. We need to iterate the cache once to find the active minor.
	var activeMinor int
	for i := range nodeDeviceTotal {
		activeMinor = i
		break
	}

	// a node can only contain one type of GPU, so each of them has the same total memory.
	if gpuMem, ok := podRequest[apiext.GPUMemory]; ok {
		podRequest[apiext.GPUMemoryRatio] = memBytesToRatio(gpuMem, nodeDeviceTotal[activeMinor][apiext.GPUMemory])
	} else {
		gpuMemRatio := podRequest[apiext.GPUMemoryRatio]
		podRequest[apiext.GPUMemory] = memRatioToBytes(gpuMemRatio, nodeDeviceTotal[activeMinor][apiext.GPUMemory])
	}
}
