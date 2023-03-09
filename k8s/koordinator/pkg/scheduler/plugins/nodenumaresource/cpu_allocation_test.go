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
	"testing"

	"github.com/stretchr/testify/assert"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/uuid"

	schedulingconfig "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

func TestNodeAllocationStateAddCPUs(t *testing.T) {
	cpuTopology := buildCPUTopologyForTest(2, 1, 4, 2)
	for _, v := range cpuTopology.CPUDetails {
		v.CoreID = v.SocketID<<16 | v.CoreID
		cpuTopology.CPUDetails[v.CPUID] = v
	}

	allocationState := newCPUAllocation("test-node-1")
	assert.NotNil(t, allocationState)
	podUID := uuid.NewUUID()
	allocationState.addCPUs(cpuTopology, podUID, cpuset.MustParse("1-4"), schedulingconfig.CPUExclusivePolicyPCPULevel)

	cpus := cpuset.MustParse("1-4")
	expectAllocatedPods := map[types.UID]cpuset.CPUSet{
		podUID: cpus,
	}
	expectAllocatedCPUs := CPUDetails{}
	for _, cpuID := range cpus.ToSliceNoSort() {
		cpuInfo := cpuTopology.CPUDetails[cpuID]
		cpuInfo.ExclusivePolicy = schedulingconfig.CPUExclusivePolicyPCPULevel
		cpuInfo.RefCount++
		expectAllocatedCPUs[cpuID] = cpuInfo
	}

	assert.Equal(t, expectAllocatedPods, allocationState.allocatedPods)
	assert.Equal(t, expectAllocatedCPUs, allocationState.allocatedCPUs)

	availableCPUs, _ := allocationState.getAvailableCPUs(cpuTopology, 2, cpuset.NewCPUSet())
	expectAvailableCPUs := cpuset.MustParse("0-15")
	assert.Equal(t, expectAvailableCPUs, availableCPUs)

	// test with add already allocated Pod
	allocationState.addCPUs(cpuTopology, podUID, cpuset.MustParse("1-4"), schedulingconfig.CPUExclusivePolicyPCPULevel)
	assert.Equal(t, expectAllocatedPods, allocationState.allocatedPods)
	assert.Equal(t, expectAllocatedCPUs, allocationState.allocatedCPUs)

	availableCPUs, _ = allocationState.getAvailableCPUs(cpuTopology, 2, cpuset.NewCPUSet())
	cpuset.MustParse("0-15")
	assert.Equal(t, expectAvailableCPUs, availableCPUs)

	// test with add already allocated cpu(refCount > 1 but less than maxRefCount) and another pod
	anotherPodUID := uuid.NewUUID()
	allocationState.addCPUs(cpuTopology, anotherPodUID, cpuset.MustParse("2-5"), schedulingconfig.CPUExclusivePolicyPCPULevel)
	anotherCPUSet := cpuset.MustParse("2-5")
	expectAllocatedPods[anotherPodUID] = anotherCPUSet
	for _, cpuID := range anotherCPUSet.ToSliceNoSort() {
		cpuInfo, ok := expectAllocatedCPUs[cpuID]
		if !ok {
			cpuInfo = cpuTopology.CPUDetails[cpuID]
		}
		cpuInfo.ExclusivePolicy = schedulingconfig.CPUExclusivePolicyPCPULevel
		cpuInfo.RefCount++
		expectAllocatedCPUs[cpuID] = cpuInfo
	}
	assert.Equal(t, expectAllocatedPods, allocationState.allocatedPods)
	assert.Equal(t, expectAllocatedCPUs, allocationState.allocatedCPUs)
}

func TestNodeAllocationStateReleaseCPUs(t *testing.T) {
	cpuTopology := buildCPUTopologyForTest(2, 1, 4, 2)
	for _, v := range cpuTopology.CPUDetails {
		v.CoreID = v.SocketID<<16 | v.CoreID
		cpuTopology.CPUDetails[v.CPUID] = v
	}

	allocationState := newCPUAllocation("test-node-1")
	assert.NotNil(t, allocationState)
	podUID := uuid.NewUUID()
	allocationState.addCPUs(cpuTopology, podUID, cpuset.MustParse("1-4"), schedulingconfig.CPUExclusivePolicyPCPULevel)

	allocationState.releaseCPUs(podUID)

	expectAllocatedPods := map[types.UID]cpuset.CPUSet{}
	expectAllocatedCPUs := CPUDetails{}
	assert.Equal(t, expectAllocatedPods, allocationState.allocatedPods)
	assert.Equal(t, expectAllocatedCPUs, allocationState.allocatedCPUs)
	for i := 0; i < 16; i++ {
		assert.Equal(t, 0, allocationState.allocatedCPUs[i].RefCount)
	}
}

func Test_cpuAllocation_getAvailableCPUs(t *testing.T) {
	cpuTopology := buildCPUTopologyForTest(2, 1, 4, 2)
	for _, v := range cpuTopology.CPUDetails {
		v.CoreID = v.SocketID<<16 | v.CoreID
		cpuTopology.CPUDetails[v.CPUID] = v
	}

	allocationState := newCPUAllocation("test-node-1")
	assert.NotNil(t, allocationState)
	podUID := uuid.NewUUID()
	allocationState.addCPUs(cpuTopology, podUID, cpuset.MustParse("1-4"), schedulingconfig.CPUExclusivePolicyPCPULevel)

	availableCPUs, _ := allocationState.getAvailableCPUs(cpuTopology, 2, cpuset.NewCPUSet())
	expectAvailableCPUs := cpuset.MustParse("0-15")
	assert.Equal(t, expectAvailableCPUs, availableCPUs)

	// test with add already allocated cpu(refCount > 1 but less than maxRefCount) and another pod
	anotherPodUID := uuid.NewUUID()
	allocationState.addCPUs(cpuTopology, anotherPodUID, cpuset.MustParse("2-5"), schedulingconfig.CPUExclusivePolicyPCPULevel)
	availableCPUs, _ = allocationState.getAvailableCPUs(cpuTopology, 2, cpuset.NewCPUSet())
	expectAvailableCPUs = cpuset.MustParse("0-1,5-15")
	assert.Equal(t, expectAvailableCPUs, availableCPUs)

	allocationState.releaseCPUs(podUID)
	availableCPUs, _ = allocationState.getAvailableCPUs(cpuTopology, 1, cpuset.NewCPUSet())
	expectAvailableCPUs = cpuset.MustParse("0-1,6-15")
	assert.Equal(t, expectAvailableCPUs, availableCPUs)
}
