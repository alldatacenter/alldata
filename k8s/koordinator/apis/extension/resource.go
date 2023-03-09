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

import (
	"encoding/json"

	corev1 "k8s.io/api/core/v1"
)

const (
	// Deprecated: because of the limitation of extended resource naming
	KoordBatchCPU corev1.ResourceName = DomainPrefix + "batch-cpu"
	// Deprecated: because of the limitation of extended resource naming
	KoordBatchMemory corev1.ResourceName = DomainPrefix + "batch-memory"

	BatchCPU    corev1.ResourceName = ResourceDomainPrefix + "batch-cpu"
	BatchMemory corev1.ResourceName = ResourceDomainPrefix + "batch-memory"

	KoordRDMA corev1.ResourceName = ResourceDomainPrefix + "rdma"
	KoordFPGA corev1.ResourceName = ResourceDomainPrefix + "fpga"

	KoordGPU  corev1.ResourceName = ResourceDomainPrefix + "gpu"
	NvidiaGPU corev1.ResourceName = "nvidia.com/gpu"

	GPUCore        corev1.ResourceName = ResourceDomainPrefix + "gpu-core"
	GPUMemory      corev1.ResourceName = ResourceDomainPrefix + "gpu-memory"
	GPUMemoryRatio corev1.ResourceName = ResourceDomainPrefix + "gpu-memory-ratio"

	GPUDriver string = ResourceDomainPrefix + "gpu-driver"
	GPUModel  string = ResourceDomainPrefix + "gpu-model"
)

const (
	// AnnotationResourceSpec represents resource allocation API defined by Koordinator.
	// The user specifies the desired CPU orchestration policy by setting the annotation.
	AnnotationResourceSpec = SchedulingDomainPrefix + "/resource-spec"
	// AnnotationResourceStatus represents resource allocation result.
	// koord-scheduler patch Pod with the annotation before binding to node.
	AnnotationResourceStatus = SchedulingDomainPrefix + "/resource-status"

	// AnnotationExtendedResourceSpec specifies the resource requirements of extended resources for internal usage.
	// It annotates the requests/limits of extended resources and can be used by runtime proxy and koordlet that
	// cannot get the original pod spec in CRI requests.
	AnnotationExtendedResourceSpec = NodeDomainPrefix + "/extended-resource-spec"
)

var (
	ResourceNameMap = map[PriorityClass]map[corev1.ResourceName]corev1.ResourceName{
		PriorityBatch: {
			corev1.ResourceCPU:    BatchCPU,
			corev1.ResourceMemory: BatchMemory,
		},
	}
)

// ResourceSpec describes extra attributes of the resource requirements.
type ResourceSpec struct {
	// PreferredCPUBindPolicy represents best-effort CPU bind policy.
	PreferredCPUBindPolicy CPUBindPolicy `json:"preferredCPUBindPolicy,omitempty"`
	// PreferredCPUExclusivePolicy represents best-effort CPU exclusive policy.
	PreferredCPUExclusivePolicy CPUExclusivePolicy `json:"preferredCPUExclusivePolicy,omitempty"`
}

// ResourceStatus describes resource allocation result, such as how to bind CPU.
type ResourceStatus struct {
	// CPUSet represents the allocated CPUs. It is Linux CPU list formatted string.
	// When LSE/LSR Pod requested, koord-scheduler will update the field.
	CPUSet string `json:"cpuset,omitempty"`
	// CPUSharedPools represents the desired CPU Shared Pools used by LS Pods.
	CPUSharedPools []CPUSharedPool `json:"cpuSharedPools,omitempty"`
}

// CPUBindPolicy defines the CPU binding policy
type CPUBindPolicy string

const (
	// CPUBindPolicyDefault performs the default bind policy that specified in koord-scheduler configuration
	CPUBindPolicyDefault CPUBindPolicy = "Default"
	// CPUBindPolicyFullPCPUs favor cpuset allocation that pack in few physical cores
	CPUBindPolicyFullPCPUs CPUBindPolicy = "FullPCPUs"
	// CPUBindPolicySpreadByPCPUs favor cpuset allocation that evenly allocate logical cpus across physical cores
	CPUBindPolicySpreadByPCPUs CPUBindPolicy = "SpreadByPCPUs"
	// CPUBindPolicyConstrainedBurst constrains the CPU Shared Pool range of the Burstable Pod
	CPUBindPolicyConstrainedBurst CPUBindPolicy = "ConstrainedBurst"
)

type CPUExclusivePolicy string

const (
	// CPUExclusivePolicyNone does not perform any exclusive policy
	CPUExclusivePolicyNone CPUExclusivePolicy = "None"
	// CPUExclusivePolicyPCPULevel represents mutual exclusion in the physical core dimension
	CPUExclusivePolicyPCPULevel CPUExclusivePolicy = "PCPULevel"
	// CPUExclusivePolicyNUMANodeLevel indicates mutual exclusion in the NUMA topology dimension
	CPUExclusivePolicyNUMANodeLevel CPUExclusivePolicy = "NUMANodeLevel"
)

// NUMAAllocateStrategy indicates how to choose satisfied NUMA Nodes
type NUMAAllocateStrategy string

const (
	// NUMAMostAllocated indicates that allocates from the NUMA Node with the least amount of available resource.
	NUMAMostAllocated NUMAAllocateStrategy = "MostAllocated"
	// NUMALeastAllocated indicates that allocates from the NUMA Node with the most amount of available resource.
	NUMALeastAllocated NUMAAllocateStrategy = "LeastAllocated"
	// NUMADistributeEvenly indicates that evenly distribute CPUs across NUMA Nodes.
	NUMADistributeEvenly NUMAAllocateStrategy = "DistributeEvenly"
)

type NUMACPUSharedPools []CPUSharedPool

type CPUSharedPool struct {
	Socket int32  `json:"socket"`
	Node   int32  `json:"node"`
	CPUSet string `json:"cpuset,omitempty"`
}

// GetResourceSpec parses ResourceSpec from annotations
func GetResourceSpec(annotations map[string]string) (*ResourceSpec, error) {
	resourceSpec := &ResourceSpec{
		PreferredCPUBindPolicy: CPUBindPolicyDefault,
	}
	data, ok := annotations[AnnotationResourceSpec]
	if !ok {
		return resourceSpec, nil
	}
	err := json.Unmarshal([]byte(data), resourceSpec)
	if err != nil {
		return nil, err
	}
	return resourceSpec, nil
}

// GetResourceStatus parses ResourceStatus from annotations
func GetResourceStatus(annotations map[string]string) (*ResourceStatus, error) {
	resourceStatus := &ResourceStatus{}
	data, ok := annotations[AnnotationResourceStatus]
	if !ok {
		return resourceStatus, nil
	}
	err := json.Unmarshal([]byte(data), resourceStatus)
	if err != nil {
		return nil, err
	}
	return resourceStatus, nil
}

func SetResourceStatus(pod *corev1.Pod, status *ResourceStatus) error {
	if pod == nil {
		return nil
	}
	if pod.Annotations == nil {
		pod.Annotations = map[string]string{}
	}
	data, err := json.Marshal(status)
	if err != nil {
		return err
	}
	pod.Annotations[AnnotationResourceStatus] = string(data)
	return nil
}

// TranslateResourceNameByPriorityClass translates defaultResourceName to extend resourceName by PriorityClass
func TranslateResourceNameByPriorityClass(priorityClass PriorityClass, defaultResourceName corev1.ResourceName) corev1.ResourceName {
	if priorityClass == PriorityProd || priorityClass == PriorityNone {
		return defaultResourceName
	}
	return ResourceNameMap[priorityClass][defaultResourceName]
}

type ExtendedResourceSpec struct {
	Containers map[string]ExtendedResourceContainerSpec `json:"containers,omitempty"`
}

type ExtendedResourceContainerSpec struct {
	Limits   corev1.ResourceList `json:"limits,omitempty"`
	Requests corev1.ResourceList `json:"requests,omitempty"`
}

// GetExtendedResourceSpec parses ExtendedResourceSpec from annotations
func GetExtendedResourceSpec(annotations map[string]string) (*ExtendedResourceSpec, error) {
	spec := &ExtendedResourceSpec{}
	if annotations == nil {
		return spec, nil
	}
	data, ok := annotations[AnnotationExtendedResourceSpec]
	if !ok {
		return spec, nil
	}
	err := json.Unmarshal([]byte(data), spec)
	if err != nil {
		return nil, err
	}
	return spec, nil
}

func SetExtendedResourceSpec(pod *corev1.Pod, spec *ExtendedResourceSpec) error {
	if pod == nil {
		return nil
	}
	if pod.Annotations == nil {
		pod.Annotations = map[string]string{}
	}
	data, err := json.Marshal(spec)
	if err != nil {
		return err
	}
	pod.Annotations[AnnotationExtendedResourceSpec] = string(data)
	return nil
}
