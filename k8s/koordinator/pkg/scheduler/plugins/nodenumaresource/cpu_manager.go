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
	"errors"
	"fmt"
	"math"
	"sync"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/tools/cache"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingconfig "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

type CPUManager interface {
	Allocate(
		node *corev1.Node,
		numCPUsNeeded int,
		cpuBindPolicy schedulingconfig.CPUBindPolicy,
		cpuExclusivePolicy schedulingconfig.CPUExclusivePolicy) (cpuset.CPUSet, error)

	UpdateAllocatedCPUSet(nodeName string, podUID types.UID, cpuset cpuset.CPUSet, cpuExclusivePolicy schedulingconfig.CPUExclusivePolicy)

	Free(nodeName string, podUID types.UID)

	Score(
		node *corev1.Node,
		numCPUsNeeded int,
		cpuBindPolicy schedulingconfig.CPUBindPolicy,
		cpuExclusivePolicy schedulingconfig.CPUExclusivePolicy) int64

	GetAvailableCPUs(nodeName string) (availableCPUs cpuset.CPUSet, allocated CPUDetails, err error)
}

type cpuManagerImpl struct {
	numaAllocateStrategy schedulingconfig.NUMAAllocateStrategy
	topologyManager      CPUTopologyManager
	lock                 sync.Mutex
	allocationStates     map[string]*cpuAllocation
}

func NewCPUManager(
	handle framework.Handle,
	defaultNUMAAllocateStrategy schedulingconfig.NUMAAllocateStrategy,
	topologyManager CPUTopologyManager,
) CPUManager {
	manager := &cpuManagerImpl{
		numaAllocateStrategy: defaultNUMAAllocateStrategy,
		topologyManager:      topologyManager,
		allocationStates:     map[string]*cpuAllocation{},
	}
	handle.SharedInformerFactory().Core().V1().Nodes().Informer().AddEventHandler(cache.ResourceEventHandlerFuncs{DeleteFunc: manager.onNodeDelete})
	return manager
}

func (c *cpuManagerImpl) onNodeDelete(obj interface{}) {
	var node *corev1.Node
	switch t := obj.(type) {
	case *corev1.Node:
		node = t
	case cache.DeletedFinalStateUnknown:
		var ok bool
		node, ok = t.Obj.(*corev1.Node)
		if !ok {
			return
		}
	default:
		break
	}

	if node == nil {
		return
	}
	c.lock.Lock()
	defer c.lock.Unlock()
	delete(c.allocationStates, node.Name)
}

func (c *cpuManagerImpl) getOrCreateAllocation(nodeName string) *cpuAllocation {
	c.lock.Lock()
	defer c.lock.Unlock()
	v := c.allocationStates[nodeName]
	if v == nil {
		v = newCPUAllocation(nodeName)
		c.allocationStates[nodeName] = v
	}
	return v
}

func (c *cpuManagerImpl) Allocate(
	node *corev1.Node,
	numCPUsNeeded int,
	cpuBindPolicy schedulingconfig.CPUBindPolicy,
	cpuExclusivePolicy schedulingconfig.CPUExclusivePolicy,
) (cpuset.CPUSet, error) {
	result := cpuset.CPUSet{}
	// The Pod requires the CPU to be allocated according to CPUBindPolicy,
	// but the current node does not have a NodeResourceTopology or a valid CPUTopology,
	// so this error should be exposed to the user
	cpuTopologyOptions := c.topologyManager.GetCPUTopologyOptions(node.Name)
	if cpuTopologyOptions.CPUTopology == nil {
		return result, errors.New(ErrNotFoundCPUTopology)
	}
	if !cpuTopologyOptions.CPUTopology.IsValid() {
		return result, errors.New(ErrInvalidCPUTopology)
	}

	reservedCPUs := cpuTopologyOptions.ReservedCPUs

	allocation := c.getOrCreateAllocation(node.Name)
	allocation.lock.Lock()
	defer allocation.lock.Unlock()

	availableCPUs, allocated := allocation.getAvailableCPUs(cpuTopologyOptions.CPUTopology, cpuTopologyOptions.MaxRefCount, reservedCPUs)
	numaAllocateStrategy := c.getNUMAAllocateStrategy(node)
	result, err := takeCPUs(
		cpuTopologyOptions.CPUTopology,
		cpuTopologyOptions.MaxRefCount,
		availableCPUs,
		allocated,
		numCPUsNeeded,
		cpuBindPolicy,
		cpuExclusivePolicy,
		numaAllocateStrategy,
	)
	return result, err
}

func (c *cpuManagerImpl) UpdateAllocatedCPUSet(nodeName string, podUID types.UID, cpuset cpuset.CPUSet, cpuExclusivePolicy schedulingconfig.CPUExclusivePolicy) {
	cpuTopologyOptions := c.topologyManager.GetCPUTopologyOptions(nodeName)
	if cpuTopologyOptions.CPUTopology == nil || !cpuTopologyOptions.CPUTopology.IsValid() {
		return
	}

	allocation := c.getOrCreateAllocation(nodeName)
	allocation.lock.Lock()
	defer allocation.lock.Unlock()

	allocation.updateAllocatedCPUSet(cpuTopologyOptions.CPUTopology, podUID, cpuset, cpuExclusivePolicy)
}

func (c *cpuManagerImpl) Free(nodeName string, podUID types.UID) {
	allocation := c.getOrCreateAllocation(nodeName)
	allocation.lock.Lock()
	defer allocation.lock.Unlock()
	allocation.releaseCPUs(podUID)
}

func (c *cpuManagerImpl) Score(
	node *corev1.Node,
	numCPUsNeeded int,
	cpuBindPolicy schedulingconfig.CPUBindPolicy,
	cpuExclusivePolicy schedulingconfig.CPUExclusivePolicy,
) int64 {
	cpuTopologyOptions := c.topologyManager.GetCPUTopologyOptions(node.Name)
	if cpuTopologyOptions.CPUTopology == nil || !cpuTopologyOptions.CPUTopology.IsValid() {
		return 0
	}

	numaAllocateStrategy := c.getNUMAAllocateStrategy(node)
	reservedCPUs := cpuTopologyOptions.ReservedCPUs

	allocation := c.getOrCreateAllocation(node.Name)
	allocation.lock.Lock()
	defer allocation.lock.Unlock()

	cpuTopology := cpuTopologyOptions.CPUTopology
	availableCPUs, allocated := allocation.getAvailableCPUs(cpuTopology, cpuTopologyOptions.MaxRefCount, reservedCPUs)
	acc := newCPUAccumulator(
		cpuTopology,
		cpuTopologyOptions.MaxRefCount,
		availableCPUs,
		allocated,
		numCPUsNeeded,
		cpuExclusivePolicy,
		numaAllocateStrategy,
	)

	var freeCPUs [][]int
	if cpuBindPolicy == schedulingconfig.CPUBindPolicyFullPCPUs {
		if numCPUsNeeded <= cpuTopology.CPUsPerNode() {
			freeCPUs = acc.freeCoresInNode(true, true)
		} else if numCPUsNeeded <= cpuTopology.CPUsPerSocket() {
			freeCPUs = acc.freeCoresInSocket(true)
		}
	} else {
		if numCPUsNeeded <= cpuTopology.CPUsPerNode() {
			freeCPUs = acc.freeCPUsInNode(true)
		} else if numCPUsNeeded <= cpuTopology.CPUsPerSocket() {
			freeCPUs = acc.freeCPUsInSocket(true)
		}
	}

	scoreFn := mostRequestedScore
	if numaAllocateStrategy == schedulingconfig.NUMALeastAllocated {
		scoreFn = leastRequestedScore
	}

	var maxScore int64
	for _, cpus := range freeCPUs {
		if len(cpus) < numCPUsNeeded {
			continue
		}

		numaScore := scoreFn(int64(numCPUsNeeded), int64(len(cpus)))
		if numaScore > maxScore {
			maxScore = numaScore
		}
	}

	// If the requested CPUs can be aligned according to NUMA Socket, it should be scored,
	// but in order to avoid the situation where the number of CPUs in the NUMA Socket of
	// some special models in the cluster is equal to the number of CPUs in the NUMA Node
	// of other models, it is necessary to reduce the weight of the score of such machines.
	if numCPUsNeeded > cpuTopology.CPUsPerNode() && numCPUsNeeded <= cpuTopology.CPUsPerSocket() {
		maxScore = int64(math.Ceil(math.Log(float64(maxScore)) * socketScoreWeight))
	}

	return maxScore
}

// The used capacity is calculated on a scale of 0-MaxNodeScore (MaxNodeScore is
// constant with value set to 100).
// 0 being the lowest priority and 100 being the highest.
// The more resources are used the higher the score is. This function
// is almost a reversed version of leastRequestedScore.
func mostRequestedScore(requested, capacity int64) int64 {
	if capacity == 0 {
		return 0
	}
	if requested > capacity {
		return 0
	}

	return (requested * framework.MaxNodeScore) / capacity
}

// The unused capacity is calculated on a scale of 0-MaxNodeScore
// 0 being the lowest priority and `MaxNodeScore` being the highest.
// The more unused resources the higher the score is.
func leastRequestedScore(requested, capacity int64) int64 {
	if capacity == 0 {
		return 0
	}
	if requested > capacity {
		return 0
	}

	return ((capacity - requested) * framework.MaxNodeScore) / capacity
}

func (c *cpuManagerImpl) getNUMAAllocateStrategy(node *corev1.Node) schedulingconfig.NUMAAllocateStrategy {
	numaAllocateStrategy := c.numaAllocateStrategy
	if val := schedulingconfig.NUMAAllocateStrategy(node.Labels[extension.LabelNodeNUMAAllocateStrategy]); val != "" {
		numaAllocateStrategy = val
	}
	return numaAllocateStrategy
}

func (c *cpuManagerImpl) GetAvailableCPUs(nodeName string) (availableCPUs cpuset.CPUSet, allocated CPUDetails, err error) {
	cpuTopologyOptions := c.topologyManager.GetCPUTopologyOptions(nodeName)
	if cpuTopologyOptions.CPUTopology == nil {
		return cpuset.NewCPUSet(), nil, errors.New(ErrNotFoundCPUTopology)
	}
	if !cpuTopologyOptions.CPUTopology.IsValid() {
		return cpuset.NewCPUSet(), nil, fmt.Errorf("cpuTopology is invalid")
	}

	allocation := c.getOrCreateAllocation(nodeName)
	allocation.lock.Lock()
	defer allocation.lock.Unlock()
	availableCPUs, allocated = allocation.getAvailableCPUs(cpuTopologyOptions.CPUTopology, cpuTopologyOptions.MaxRefCount, cpuTopologyOptions.ReservedCPUs)
	return availableCPUs, allocated, nil
}
