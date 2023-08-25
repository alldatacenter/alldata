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

package batchresource

import (
	"context"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	resschedplug "k8s.io/kubernetes/pkg/scheduler/framework/plugins/noderesources"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

const (
	Name = "BatchResourceFit"
)

type batchResource struct {
	MilliCPU int64
	Memory   int64
}

var (
	_ framework.FilterPlugin = &Plugin{}
)

type Plugin struct {
}

func New(args runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	return &Plugin{}, nil
}

func (p *Plugin) Name() string {
	return Name
}

func (p *Plugin) Filter(ctx context.Context, state *framework.CycleState, pod *corev1.Pod, nodeInfo *framework.NodeInfo) *framework.Status {
	insufficientResources := fitsRequest(pod, nodeInfo)

	if len(insufficientResources) != 0 {
		// We will keep all failure reasons.
		failureReasons := make([]string, 0, len(insufficientResources))
		for _, r := range insufficientResources {
			failureReasons = append(failureReasons, r.Reason)
		}
		return framework.NewStatus(framework.Unschedulable, failureReasons...)
	}
	return nil
}

func fitsRequest(pod *corev1.Pod, nodeInfo *framework.NodeInfo) []resschedplug.InsufficientResource {
	podBatchRequest := computePodBatchRequest(pod)
	if podBatchRequest.MilliCPU == 0 && podBatchRequest.Memory == 0 {
		return nil
	}

	insufficientResources := make([]resschedplug.InsufficientResource, 0, 2)
	nodeRequested := computeNodeBatchRequested(nodeInfo)
	nodeAllocatable := computeNodeBatchAllocatable(nodeInfo)
	if podBatchRequest.MilliCPU > (nodeAllocatable.MilliCPU - nodeRequested.MilliCPU) {
		insufficientResources = append(insufficientResources, resschedplug.InsufficientResource{
			ResourceName: apiext.BatchCPU,
			Reason:       "Insufficient batch cpu",
			Requested:    podBatchRequest.MilliCPU,
			Used:         nodeRequested.MilliCPU,
			Capacity:     nodeAllocatable.MilliCPU,
		})
	}
	if podBatchRequest.Memory > (nodeAllocatable.Memory - nodeRequested.Memory) {
		insufficientResources = append(insufficientResources, resschedplug.InsufficientResource{
			ResourceName: apiext.BatchMemory,
			Reason:       "Insufficient batch memory",
			Requested:    podBatchRequest.Memory,
			Used:         nodeRequested.Memory,
			Capacity:     nodeAllocatable.Memory,
		})
	}
	return insufficientResources
}

func computeNodeBatchAllocatable(nodeInfo *framework.NodeInfo) *batchResource {
	nodeAllocatable := &batchResource{
		MilliCPU: 0,
		Memory:   0,
	}
	// compatible with old format, overwrite BatchCPU, BatchMemory if exist
	// nolint:staticcheck // SA1019: apiext.KoordBatchCPU is deprecated: because of the limitation of extended resource naming
	if koordBatchCPU, exist := nodeInfo.Allocatable.ScalarResources[apiext.KoordBatchCPU]; exist {
		nodeAllocatable.MilliCPU = koordBatchCPU
	}
	// nolint:staticcheck // SA1019: apiext.KoordBatchMemory is deprecated: because of the limitation of extended resource naming
	if koordBatchMemory, exist := nodeInfo.Allocatable.ScalarResources[apiext.KoordBatchMemory]; exist {
		nodeAllocatable.Memory = koordBatchMemory
	}
	if batchCPU, exist := nodeInfo.Allocatable.ScalarResources[apiext.BatchCPU]; exist {
		nodeAllocatable.MilliCPU = batchCPU
	}
	if batchMemory, exist := nodeInfo.Allocatable.ScalarResources[apiext.BatchMemory]; exist {
		nodeAllocatable.Memory = batchMemory
	}
	return nodeAllocatable
}

func computeNodeBatchRequested(nodeInfo *framework.NodeInfo) *batchResource {
	nodeRequested := &batchResource{
		MilliCPU: 0,
		Memory:   0,
	}
	// compatible with old format, accumulate
	// with KoordBatchCPU, KoordBatchCPU if exist
	if batchCPU, exist := nodeInfo.Requested.ScalarResources[apiext.BatchCPU]; exist {
		nodeRequested.MilliCPU += batchCPU
	}
	// nolint:staticcheck // SA1019: apiext.KoordBatchCPU is deprecated: because of the limitation of extended resource naming
	if koordBatchCPU, exist := nodeInfo.Requested.ScalarResources[apiext.KoordBatchCPU]; exist {
		nodeRequested.MilliCPU += koordBatchCPU
	}
	if batchMemory, exist := nodeInfo.Requested.ScalarResources[apiext.BatchMemory]; exist {
		nodeRequested.Memory += batchMemory
	}
	// nolint:staticcheck // SA1019: apiext.KoordBatchMemory is deprecated: because of the limitation of extended resource naming
	if koordBatchMemory, exist := nodeInfo.Requested.ScalarResources[apiext.KoordBatchMemory]; exist {
		nodeRequested.Memory += koordBatchMemory
	}
	return nodeRequested
}

// computePodBERequest returns the total non-zero best-effort requests. If Overhead is defined for the pod and
// the PodOverhead feature is enabled, the Overhead is added to the result.
// podBERequest = max(sum(podSpec.Containers), podSpec.InitContainers) + overHead
func computePodBatchRequest(pod *corev1.Pod) *batchResource {
	podRequest := &framework.Resource{}
	for _, container := range pod.Spec.Containers {
		podRequest.Add(container.Resources.Requests)
	}

	// take max_resource(sum_pod, any_init_container)
	for _, container := range pod.Spec.InitContainers {
		podRequest.SetMaxResource(container.Resources.Requests)
	}

	// If Overhead is being utilized, add to the total requests for the pod
	if pod.Spec.Overhead != nil {
		podRequest.Add(pod.Spec.Overhead)
	}

	result := &batchResource{
		MilliCPU: 0,
		Memory:   0,
	}
	// compatible with old format, overwrite BatchCPU, BatchMemory if exist
	// nolint:staticcheck // SA1019: apiext.KoordBatchCPU is deprecated: because of the limitation of extended resource naming
	if koordBatchCPU, exist := podRequest.ScalarResources[apiext.KoordBatchCPU]; exist {
		result.MilliCPU = koordBatchCPU
	}
	// nolint:staticcheck // SA1019: apiext.KoordBatchMemory is deprecated: because of the limitation of extended resource naming
	if koordBatchMemory, exist := podRequest.ScalarResources[apiext.KoordBatchMemory]; exist {
		result.Memory = koordBatchMemory
	}
	if batchCPU, exist := podRequest.ScalarResources[apiext.BatchCPU]; exist {
		result.MilliCPU = batchCPU
	}
	if batchMemory, exist := podRequest.ScalarResources[apiext.BatchMemory]; exist {
		result.Memory = batchMemory
	}
	return result
}
