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
	"sync"

	"k8s.io/apimachinery/pkg/types"

	schedulingconfig "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

type cpuAllocation struct {
	lock          sync.Mutex
	nodeName      string
	allocatedPods map[types.UID]cpuset.CPUSet
	allocatedCPUs CPUDetails
}

func newCPUAllocation(nodeName string) *cpuAllocation {
	return &cpuAllocation{
		nodeName:      nodeName,
		allocatedPods: map[types.UID]cpuset.CPUSet{},
		allocatedCPUs: NewCPUDetails(),
	}
}

func (n *cpuAllocation) updateAllocatedCPUSet(cpuTopology *CPUTopology, podUID types.UID, cpuset cpuset.CPUSet, cpuExclusivePolicy schedulingconfig.CPUExclusivePolicy) {
	n.releaseCPUs(podUID)
	n.addCPUs(cpuTopology, podUID, cpuset, cpuExclusivePolicy)
}

func (n *cpuAllocation) addCPUs(cpuTopology *CPUTopology, podUID types.UID, cpuset cpuset.CPUSet, exclusivePolicy schedulingconfig.CPUExclusivePolicy) {
	if _, ok := n.allocatedPods[podUID]; ok {
		return
	}
	n.allocatedPods[podUID] = cpuset

	for _, cpuID := range cpuset.ToSliceNoSort() {
		cpuInfo, ok := n.allocatedCPUs[cpuID]
		if !ok {
			cpuInfo = cpuTopology.CPUDetails[cpuID]
		}
		cpuInfo.ExclusivePolicy = exclusivePolicy
		cpuInfo.RefCount++
		n.allocatedCPUs[cpuID] = cpuInfo
	}
}

func (n *cpuAllocation) releaseCPUs(podUID types.UID) {
	cpuset, ok := n.allocatedPods[podUID]
	if !ok {
		return
	}
	delete(n.allocatedPods, podUID)

	for _, cpuID := range cpuset.ToSliceNoSort() {
		cpuInfo, ok := n.allocatedCPUs[cpuID]
		if !ok {
			continue
		}
		cpuInfo.RefCount--
		if cpuInfo.RefCount == 0 {
			delete(n.allocatedCPUs, cpuID)
		} else {
			n.allocatedCPUs[cpuID] = cpuInfo
		}
	}
}

func (n *cpuAllocation) getAvailableCPUs(cpuTopology *CPUTopology, maxRefCount int, reservedCPUs cpuset.CPUSet) (availableCPUs cpuset.CPUSet, allocateInfo CPUDetails) {
	allocateInfo = n.allocatedCPUs.Clone()
	allocated := allocateInfo.CPUs().Filter(func(cpuID int) bool {
		return allocateInfo[cpuID].RefCount >= maxRefCount
	})
	availableCPUs = cpuTopology.CPUDetails.CPUs().Difference(allocated).Difference(reservedCPUs)
	return
}
