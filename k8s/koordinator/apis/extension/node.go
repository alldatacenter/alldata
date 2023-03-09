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

	"k8s.io/apimachinery/pkg/types"
)

const (
	// AnnotationNodeCPUTopology describes the detailed CPU topology.
	AnnotationNodeCPUTopology = NodeDomainPrefix + "/cpu-topology"
	// AnnotationNodeCPUAllocs describes K8s Guaranteed Pods.
	AnnotationNodeCPUAllocs = NodeDomainPrefix + "/pod-cpu-allocs"
	// AnnotationNodeCPUSharedPools describes the CPU Shared Pool defined by Koordinator.
	// The shared pool is mainly used by Koordinator LS Pods or K8s Burstable Pods.
	AnnotationNodeCPUSharedPools = NodeDomainPrefix + "/cpu-shared-pools"

	// LabelNodeCPUBindPolicy constrains how to bind CPU logical CPUs when scheduling.
	LabelNodeCPUBindPolicy = NodeDomainPrefix + "/cpu-bind-policy"
	// LabelNodeNUMAAllocateStrategy indicates how to choose satisfied NUMA Nodes when scheduling.
	LabelNodeNUMAAllocateStrategy = NodeDomainPrefix + "/numa-allocate-strategy"
)

const (
	// NodeCPUBindPolicyNone does not perform any bind policy
	NodeCPUBindPolicyNone = "None"
	// NodeCPUBindPolicyFullPCPUsOnly requires that the scheduler must allocate full physical cores.
	// Equivalent to kubelet CPU manager policy option full-pcpus-only=true.
	NodeCPUBindPolicyFullPCPUsOnly = "FullPCPUsOnly"
	// NodeCPUBindPolicySpreadByPCPUs requires that the scheduler must evenly allocate logical cpus across physical cores
	NodeCPUBindPolicySpreadByPCPUs = "SpreadByPCPUs"
)

const (
	NodeNUMAAllocateStrategyLeastAllocated = string(NUMALeastAllocated)
	NodeNUMAAllocateStrategyMostAllocated  = string(NUMAMostAllocated)
)

const (
	// AnnotationKubeletCPUManagerPolicy describes the cpu manager policy options of kubelet
	AnnotationKubeletCPUManagerPolicy = "kubelet.koordinator.sh/cpu-manager-policy"

	KubeletCPUManagerPolicyStatic                         = "static"
	KubeletCPUManagerPolicyNone                           = "none"
	KubeletCPUManagerPolicyFullPCPUsOnlyOption            = "full-pcpus-only"
	KubeletCPUManagerPolicyDistributeCPUsAcrossNUMAOption = "distribute-cpus-across-numa"
)

type CPUTopology struct {
	Detail []CPUInfo `json:"detail,omitempty"`
}

type CPUInfo struct {
	ID     int32 `json:"id"`
	Core   int32 `json:"core"`
	Socket int32 `json:"socket"`
	Node   int32 `json:"node"`
}

type PodCPUAlloc struct {
	Namespace        string    `json:"namespace,omitempty"`
	Name             string    `json:"name,omitempty"`
	UID              types.UID `json:"uid,omitempty"`
	CPUSet           string    `json:"cpuset,omitempty"`
	ManagedByKubelet bool      `json:"managedByKubelet,omitempty"`
}

type PodCPUAllocs []PodCPUAlloc

type KubeletCPUManagerPolicy struct {
	Policy       string            `json:"policy,omitempty"`
	Options      map[string]string `json:"options,omitempty"`
	ReservedCPUs string            `json:"reservedCPUs,omitempty"`
}

func GetCPUTopology(annotations map[string]string) (*CPUTopology, error) {
	topology := &CPUTopology{}
	data, ok := annotations[AnnotationNodeCPUTopology]
	if !ok {
		return topology, nil
	}
	err := json.Unmarshal([]byte(data), topology)
	if err != nil {
		return nil, err
	}
	return topology, nil
}

func GetPodCPUAllocs(annotations map[string]string) (PodCPUAllocs, error) {
	var allocs PodCPUAllocs
	data, ok := annotations[AnnotationNodeCPUAllocs]
	if !ok {
		return allocs, nil
	}
	err := json.Unmarshal([]byte(data), &allocs)
	if err != nil {
		return nil, err
	}
	return allocs, nil
}

func GetNodeCPUSharePools(nodeTopoAnnotations map[string]string) ([]CPUSharedPool, error) {
	var cpuSharePools []CPUSharedPool
	data, ok := nodeTopoAnnotations[AnnotationNodeCPUSharedPools]
	if !ok {
		return cpuSharePools, nil
	}
	err := json.Unmarshal([]byte(data), &cpuSharePools)
	if err != nil {
		return nil, err
	}
	return cpuSharePools, nil
}

func GetKubeletCPUManagerPolicy(annotations map[string]string) (*KubeletCPUManagerPolicy, error) {
	cpuManagerPolicy := &KubeletCPUManagerPolicy{}
	data, ok := annotations[AnnotationKubeletCPUManagerPolicy]
	if !ok {
		return cpuManagerPolicy, nil
	}
	err := json.Unmarshal([]byte(data), cpuManagerPolicy)
	if err != nil {
		return nil, err
	}
	return cpuManagerPolicy, nil
}

func GetNodeCPUBindPolicy(nodeLabels map[string]string, kubeletCPUPolicy *KubeletCPUManagerPolicy) string {
	nodeCPUBindPolicy := nodeLabels[LabelNodeCPUBindPolicy]
	if nodeCPUBindPolicy == NodeCPUBindPolicyFullPCPUsOnly ||
		(kubeletCPUPolicy != nil && kubeletCPUPolicy.Policy == KubeletCPUManagerPolicyStatic &&
			kubeletCPUPolicy.Options[KubeletCPUManagerPolicyFullPCPUsOnlyOption] == "true") {
		return NodeCPUBindPolicyFullPCPUsOnly
	}
	if nodeCPUBindPolicy == NodeCPUBindPolicySpreadByPCPUs {
		return nodeCPUBindPolicy
	}
	return NodeCPUBindPolicyNone
}
