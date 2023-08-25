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

package extension

import corev1 "k8s.io/api/core/v1"

const (
	// Deprecated: because of the limitation of extended resource naming
	KoordBatchCPU corev1.ResourceName = DomainPrefix + "batch-cpu"
	// Deprecated: because of the limitation of extended resource naming
	KoordBatchMemory corev1.ResourceName = DomainPrefix + "batch-memory"

	// Deprecated: Device extension resource names should use the prefix `koordinator.sh`
	DeprecatedKoordRDMA corev1.ResourceName = ResourceDomainPrefix + "rdma"
	// Deprecated: Device extension resource names should use the prefix `koordinator.sh`
	DeprecatedKoordFPGA corev1.ResourceName = ResourceDomainPrefix + "fpga"
	// Deprecated: Device extension resource names should use the prefix `koordinator.sh`
	DeprecatedKoordGPU corev1.ResourceName = ResourceDomainPrefix + "gpu"
	// Deprecated: Device extension resource names should use the prefix `koordinator.sh`
	DeprecatedGPUCore corev1.ResourceName = ResourceDomainPrefix + "gpu-core"
	// Deprecated: Device extension resource names should use the prefix `koordinator.sh`
	DeprecatedGPUMemory corev1.ResourceName = ResourceDomainPrefix + "gpu-memory"
	// Deprecated: Device extension resource names should use the prefix `koordinator.sh`
	DeprecatedGPUMemoryRatio corev1.ResourceName = ResourceDomainPrefix + "gpu-memory-ratio"
)

const (
	// Deprecated: Device extension resource names should use the prefix `koordinator.sh`
	DeprecatedGPUDriver string = ResourceDomainPrefix + "gpu-driver"
	// Deprecated: Device extension resource names should use the prefix `koordinator.sh`
	DeprecatedGPUModel string = ResourceDomainPrefix + "gpu-model"
)

var deprecatedDeviceResourceNameMapper = map[corev1.ResourceName]corev1.ResourceName{
	DeprecatedKoordRDMA:      ResourceRDMA,
	DeprecatedKoordFPGA:      ResourceFPGA,
	DeprecatedKoordGPU:       ResourceGPU,
	DeprecatedGPUCore:        ResourceGPUCore,
	DeprecatedGPUMemory:      ResourceGPUMemory,
	DeprecatedGPUMemoryRatio: ResourceGPUMemoryRatio,
}

func TransformDeprecatedDeviceResources(resList corev1.ResourceList) corev1.ResourceList {
	r := make(corev1.ResourceList, len(resList))
	for k, v := range resList {
		newResName := deprecatedDeviceResourceNameMapper[k]
		if newResName != "" {
			r[newResName] = v
		} else {
			r[k] = v
		}
	}
	return r
}
