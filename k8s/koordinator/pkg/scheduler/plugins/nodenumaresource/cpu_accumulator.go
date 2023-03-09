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
	"fmt"
	"sort"

	"k8s.io/apimachinery/pkg/util/sets"

	schedulingconfig "github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

func takeCPUs(
	topology *CPUTopology,
	maxRefCount int,
	availableCPUs cpuset.CPUSet,
	allocatedCPUs CPUDetails,
	numCPUsNeeded int,
	cpuBindPolicy schedulingconfig.CPUBindPolicy,
	cpuExclusivePolicy schedulingconfig.CPUExclusivePolicy,
	numaAllocatedStrategy schedulingconfig.NUMAAllocateStrategy,
) (cpuset.CPUSet, error) {
	acc := newCPUAccumulator(topology, maxRefCount, availableCPUs, allocatedCPUs, numCPUsNeeded, cpuExclusivePolicy, numaAllocatedStrategy)
	if acc.isSatisfied() {
		return acc.result, nil
	}
	if acc.isFailed() {
		return cpuset.NewCPUSet(), fmt.Errorf("not enough cpus available to satisfy request")
	}

	fullPCPUs := cpuBindPolicy == schedulingconfig.CPUBindPolicyFullPCPUs
	if fullPCPUs || acc.topology.CPUsPerCore() == 1 {
		// According to the NUMA allocation strategy,
		// select the NUMA Node with the most remaining amount or the least amount remaining
		// and the total amount of available CPUs in the NUMA Node is greater than or equal to the number of CPUs needed
		filterExclusiveArgs := []bool{true, false}
		if acc.numCPUsNeeded <= acc.topology.CPUsPerNode() {
			for _, filterExclusive := range filterExclusiveArgs {
				freeCPUs := acc.freeCoresInNode(true, filterExclusive)
				for _, cpus := range freeCPUs {
					if len(cpus) >= acc.numCPUsNeeded {
						acc.take(cpus[:acc.numCPUsNeeded]...)
						return acc.result, nil
					}
				}
			}
		}

		// According to the NUMA allocation strategy,
		// select the NUMA Socket with the most remaining amount or the least amount remaining
		// and the total amount of available CPUs in the NUMA Socket is greater than or equal to the number of CPUs needed
		if acc.numCPUsNeeded <= acc.topology.CPUsPerSocket() {
			freeCPUs := acc.freeCoresInSocket(true)
			for _, cpus := range freeCPUs {
				if len(cpus) >= acc.numCPUsNeeded {
					acc.take(cpus[:acc.numCPUsNeeded]...)
					return acc.result, nil
				}
			}
		}

		// There are some scenarios at this time,
		// - the amount of CPUs needed exceeds the total amount of a NUMA Socket,
		// - or each NUMA Node/Socket is allocated a part and the remaining number of CPUs does not meet the needed.
		// For better performance, allocate from the NUMA Socket with the most remaining physical cores as much as possible
		// and no longer follow the NUMA Allocate Strategy.
		freeCPUs := acc.freeCoresInSocket(true)
		sort.Slice(freeCPUs, func(i, j int) bool {
			return len(freeCPUs[i]) > len(freeCPUs[j])
		})
		var unsatisfiedFreeCPus [][]int
		for _, cpus := range freeCPUs {
			if !acc.needs(len(cpus)) {
				unsatisfiedFreeCPus = append(unsatisfiedFreeCPus, cpus)
			} else {
				acc.take(cpus...)
				if acc.isSatisfied() {
					return acc.result, nil
				}
			}
		}

		// There are still some unsatisfied physical core requests,
		// which need to be allocated from the NUMA Socket with the fewest remaining physical cores as much as possible
		if acc.needs(acc.topology.CPUsPerCore()) {
			freeCPUs = unsatisfiedFreeCPus
			sort.Slice(freeCPUs, func(i, j int) bool {
				return len(freeCPUs[i]) < len(freeCPUs[j])
			})
			cpusPerCore := acc.topology.CPUsPerCore()
			for _, cpus := range freeCPUs {
				for i := 0; i < len(cpus); i += cpusPerCore {
					acc.take(cpus[i : i+cpusPerCore]...)
					if acc.isSatisfied() {
						return acc.result, nil
					}
					if !acc.needs(cpusPerCore) {
						break
					}
				}
			}
		}
	}

	// The following scenarios need to be allocated through the SpreadByPCPUs policy
	// - The application requires CPU allocation according to the SpreadByPCPUs policy
	// - Not enough physical cores to satisfy FullPCPUs policy
	// - It is required to be allocated in the FullPCPUs policy, but the number of CPUs requested is
	//   not enough to monopolize a complete physical core
	if !fullPCPUs {
		// Try to allocate CPUs in the same NUMA Node according NUMA Allocate Strategy,
		// even if the SpreadByPCPUs policy cannot be satisfied
		filterExclusiveArgs := []bool{true, false}
		if acc.numCPUsNeeded <= acc.topology.CPUsPerNode() {
			for _, filterExclusive := range filterExclusiveArgs {
				cpusInNode := acc.freeCPUsInNode(filterExclusive)
				for _, cpus := range cpusInNode {
					if len(cpus) >= acc.numCPUsNeeded {
						cpus = acc.spreadCPUs(cpus)
						acc.take(cpus[:acc.numCPUsNeeded]...)
						return acc.result, nil
					}
				}
			}
		}

		// Try to allocate CPUs in the same NUMA Socket according NUMA Allocate Strategy,
		// even if the SpreadByPCPUs policy cannot be satisfied
		if acc.numCPUsNeeded <= acc.topology.CPUsPerSocket() {
			for _, filterExclusive := range filterExclusiveArgs {
				cpusInSocket := acc.freeCPUsInSocket(filterExclusive)
				for _, cpus := range cpusInSocket {
					if len(cpus) >= acc.numCPUsNeeded {
						cpus = acc.spreadCPUs(cpus)
						acc.take(cpus[:acc.numCPUsNeeded]...)
						return acc.result, nil
					}
				}
			}
		}
	}

	// Try to allocate CPUs on the NUMA Node/Socket which the allocated CPU belongs
	filterExclusiveArgs := []bool{true, false}
	for _, filterExclusive := range filterExclusiveArgs {
		cpus := acc.spreadCPUs(acc.freeCPUs(filterExclusive))
		for _, c := range cpus {
			if acc.needs(1) {
				acc.take(c)
			}
			if acc.isSatisfied() {
				return acc.result, nil
			}
		}
	}

	return cpuset.NewCPUSet(), fmt.Errorf("failed to allocate cpus")
}

type cpuAccumulator struct {
	topology             *CPUTopology
	maxRefCount          int
	allocatableCPUs      CPUDetails
	numCPUsNeeded        int
	exclusive            bool
	exclusiveInCores     sets.Int
	exclusiveInNUMANodes sets.Int
	exclusivePolicy      schedulingconfig.CPUExclusivePolicy
	numaAllocateStrategy schedulingconfig.NUMAAllocateStrategy
	result               cpuset.CPUSet
}

func newCPUAccumulator(
	topology *CPUTopology,
	maxRefCount int,
	availableCPUs cpuset.CPUSet,
	allocatedCPUs CPUDetails,
	numCPUsNeeded int,
	exclusivePolicy schedulingconfig.CPUExclusivePolicy,
	numaAllocateStrategy schedulingconfig.NUMAAllocateStrategy,
) *cpuAccumulator {
	exclusiveInCores := sets.NewInt()
	exclusiveInNUMANodes := sets.NewInt()
	for _, v := range allocatedCPUs {
		if v.ExclusivePolicy == schedulingconfig.CPUExclusivePolicyPCPULevel {
			exclusiveInCores.Insert(v.CoreID)
		} else if v.ExclusivePolicy == schedulingconfig.CPUExclusivePolicyNUMANodeLevel {
			exclusiveInNUMANodes.Insert(v.NodeID)
		}
	}
	exclusive := exclusivePolicy == schedulingconfig.CPUExclusivePolicyPCPULevel ||
		exclusivePolicy == schedulingconfig.CPUExclusivePolicyNUMANodeLevel

	allocatableCPUs := topology.CPUDetails.KeepOnly(availableCPUs)
	if maxRefCount > 1 {
		for _, v := range allocatableCPUs {
			v.RefCount = allocatedCPUs[v.CPUID].RefCount
			allocatableCPUs[v.CPUID] = v
		}
	}

	return &cpuAccumulator{
		topology:             topology,
		maxRefCount:          maxRefCount,
		allocatableCPUs:      allocatableCPUs,
		exclusiveInCores:     exclusiveInCores,
		exclusiveInNUMANodes: exclusiveInNUMANodes,
		exclusive:            exclusive,
		exclusivePolicy:      exclusivePolicy,
		numCPUsNeeded:        numCPUsNeeded,
		numaAllocateStrategy: numaAllocateStrategy,
		result:               cpuset.NewCPUSet(),
	}
}

func (a *cpuAccumulator) take(cpus ...int) {
	a.result = a.result.UnionSlice(cpus...)
	for _, cpu := range cpus {
		delete(a.allocatableCPUs, cpu)
		if a.exclusive {
			cpuInfo := a.topology.CPUDetails[cpu]
			if a.exclusivePolicy == schedulingconfig.CPUExclusivePolicyPCPULevel {
				a.exclusiveInCores.Insert(cpuInfo.CoreID)
			} else if a.exclusivePolicy == schedulingconfig.CPUExclusivePolicyNUMANodeLevel {
				a.exclusiveInNUMANodes.Insert(cpuInfo.NodeID)
			}
		}
	}
	a.numCPUsNeeded -= len(cpus)
}

func (a *cpuAccumulator) needs(n int) bool {
	return a.numCPUsNeeded >= n
}

func (a *cpuAccumulator) isSatisfied() bool {
	return a.numCPUsNeeded < 1
}

func (a *cpuAccumulator) isFailed() bool {
	return a.numCPUsNeeded > len(a.allocatableCPUs)
}

func (a *cpuAccumulator) isCPUExclusivePCPULevel(cpuInfo *CPUInfo) bool {
	if a.exclusivePolicy != schedulingconfig.CPUExclusivePolicyPCPULevel {
		return false
	}
	return a.exclusiveInCores.Has(cpuInfo.CoreID)
}

func (a *cpuAccumulator) isCPUExclusiveNUMANodeLevel(cpuInfo *CPUInfo) bool {
	if a.exclusivePolicy != schedulingconfig.CPUExclusivePolicyNUMANodeLevel {
		return false
	}
	return a.exclusiveInNUMANodes.Has(cpuInfo.NodeID)
}

func (a *cpuAccumulator) extractCPU(cpus []int) []int {
	selected := make([]int, 0, len(cpus))
	cores := make(map[int]struct{})
	for _, c := range cpus {
		coreID := a.topology.CPUDetails[c].CoreID
		if _, ok := cores[coreID]; !ok {
			cores[coreID] = struct{}{}
			selected = append(selected, c)
		}
	}
	return selected
}

func (a *cpuAccumulator) sortCores(details CPUDetails, cores []int, cpusInCores map[int][]int) {
	if len(cores) <= 1 {
		return
	}

	sort.Slice(cores, func(i, j int) bool {
		iCore := cores[i]
		jCore := cores[j]

		iCPUsCount := len(cpusInCores[iCore])
		jCPUsCount := len(cpusInCores[jCore])
		if iCPUsCount != jCPUsCount {
			return iCPUsCount > jCPUsCount
		}
		if a.maxRefCount > 1 {
			iRefCount := getCoreRefCount(details, iCore)
			jRefCount := getCoreRefCount(details, jCore)
			if iRefCount != jRefCount {
				return iRefCount < jRefCount
			}
		}
		return cores[i] < cores[j]
	})
}

// freeCoresInNode returns the logical cpus of the free cores in nodes that sorted
func (a *cpuAccumulator) freeCoresInNode(filterFullFreeCore bool, filterExclusive bool) [][]int {
	allocatableCPUs := a.allocatableCPUs

	socketFreeScores := make(map[int]int)
	cpusInCores := make(map[int][]int)
	for _, cpuInfo := range allocatableCPUs {
		if filterExclusive && a.isCPUExclusiveNUMANodeLevel(&cpuInfo) {
			continue
		}
		cpus := cpusInCores[cpuInfo.CoreID]
		if len(cpus) == 0 {
			cpus = make([]int, 0, a.topology.CPUsPerCore())
		}
		cpus = append(cpus, cpuInfo.CPUID)
		cpusInCores[cpuInfo.CoreID] = cpus
		socketFreeScores[cpuInfo.SocketID]++
	}

	coresInNodes := make(map[int][]int)
	numCPUs := 0
	for core, cpus := range cpusInCores {
		if filterFullFreeCore && len(cpus) != a.topology.CPUsPerCore() {
			continue
		}
		info := allocatableCPUs[cpus[0]]
		cores := coresInNodes[info.NodeID]
		if len(cores) == 0 {
			cores = make([]int, 0, a.topology.CPUsPerNode()/a.topology.CPUsPerCore())
		}
		cores = append(cores, core)
		coresInNodes[info.NodeID] = cores
		numCPUs += len(cpus)
	}

	nodeIDs := make([]int, 0, len(coresInNodes))
	cpusInNodes := make(map[int][]int)
	for node, cores := range coresInNodes {
		nodeIDs = append(nodeIDs, node)
		a.sortCores(allocatableCPUs, cores, cpusInCores)
		cpusInCore := make([]int, 0, numCPUs)
		for _, c := range cores {
			cpus := cpusInCores[c]
			sort.Ints(cpus)
			cpusInCore = append(cpusInCore, cpus...)
		}
		cpusInNodes[node] = cpusInCore
	}

	sort.Slice(nodeIDs, func(i, j int) bool {
		iCPUs := cpusInNodes[nodeIDs[i]]
		jCPUs := cpusInNodes[nodeIDs[j]]

		// each cpu's socketId and nodeId in same node are same
		iCPUInfo := allocatableCPUs[iCPUs[0]]
		jCPUInfo := allocatableCPUs[jCPUs[0]]

		iSocket := iCPUInfo.SocketID
		jSocket := jCPUInfo.SocketID

		// Compute the number of available CPUs available on the same node as each core.
		iNodeFreeScore := len(cpusInNodes[nodeIDs[i]])
		jNodeFreeScore := len(cpusInNodes[nodeIDs[j]])
		if iNodeFreeScore != jNodeFreeScore {
			if a.numaAllocateStrategy == schedulingconfig.NUMAMostAllocated {
				return iNodeFreeScore < jNodeFreeScore
			} else {
				return iNodeFreeScore > jNodeFreeScore
			}
		}

		// Compute the number of available CPUs available on the same socket as each core.
		iSocketFreeScore := socketFreeScores[iSocket]
		jSocketFreeScore := socketFreeScores[jSocket]
		if iSocketFreeScore != jSocketFreeScore {
			if a.numaAllocateStrategy == schedulingconfig.NUMAMostAllocated {
				return iSocketFreeScore < jSocketFreeScore
			} else {
				return iSocketFreeScore > jSocketFreeScore
			}
		}

		return nodeIDs[i] < nodeIDs[j]
	})

	var result [][]int
	for _, node := range nodeIDs {
		result = append(result, cpusInNodes[node])
	}

	return result
}

// freeCoresInSocket returns the logical cpus of the free cores in sockets that sorted
func (a *cpuAccumulator) freeCoresInSocket(filterFullFreeCore bool) [][]int {
	allocatableCPUs := a.allocatableCPUs

	cpusInCores := make(map[int][]int)
	for _, cpuInfo := range allocatableCPUs {
		cpus := cpusInCores[cpuInfo.CoreID]
		if len(cpus) == 0 {
			cpus = make([]int, 0, a.topology.CPUsPerCore())
		}
		cpus = append(cpus, cpuInfo.CPUID)
		cpusInCores[cpuInfo.CoreID] = cpus
	}

	coresInSockets := make(map[int][]int)
	numCPUs := 0
	for core, cpus := range cpusInCores {
		if filterFullFreeCore && len(cpus) != a.topology.CPUsPerCore() {
			continue
		}
		info := allocatableCPUs[cpus[0]]
		cores := coresInSockets[info.SocketID]
		if len(cores) == 0 {
			cores = make([]int, 0, a.topology.CPUsPerSocket()/a.topology.CPUsPerCore())
		}
		cores = append(cores, core)
		coresInSockets[info.SocketID] = cores
		numCPUs += len(cpus)
	}

	socketIDs := make([]int, 0, len(coresInSockets))
	cpusInSockets := make(map[int][]int)
	for socket, cores := range coresInSockets {
		socketIDs = append(socketIDs, socket)
		a.sortCores(allocatableCPUs, cores, cpusInCores)
		cpusInCore := make([]int, 0, numCPUs)
		for _, c := range cores {
			cpus := cpusInCores[c]
			sort.Ints(cpus)
			cpusInCore = append(cpusInCore, cpus...)
		}
		cpusInSockets[socket] = cpusInCore
	}

	sort.Slice(socketIDs, func(i, j int) bool {
		iSocketFreeScore := len(cpusInSockets[socketIDs[i]])
		jSocketFreeScore := len(cpusInSockets[socketIDs[j]])

		if iSocketFreeScore != jSocketFreeScore {
			if a.numaAllocateStrategy == schedulingconfig.NUMAMostAllocated {
				return iSocketFreeScore < jSocketFreeScore
			} else {
				return iSocketFreeScore > jSocketFreeScore
			}
		}
		return socketIDs[i] < socketIDs[j]
	})

	var result [][]int
	for _, s := range socketIDs {
		result = append(result, cpusInSockets[s])
	}

	return result
}

// freeCPUsInNode returns free logical cpus in nodes that sorted in ascending order.
func (a *cpuAccumulator) freeCPUsInNode(filterExclusive bool) [][]int {
	cpusInNodes := make(map[int][]int)
	nodeFreeScores := make(map[int]int)
	socketFreeScores := make(map[int]int)
	for _, cpuInfo := range a.allocatableCPUs {
		if filterExclusive && (a.isCPUExclusivePCPULevel(&cpuInfo) || a.isCPUExclusiveNUMANodeLevel(&cpuInfo)) {
			continue
		}
		cpus := cpusInNodes[cpuInfo.NodeID]
		if len(cpus) == 0 {
			cpus = make([]int, 0, a.topology.CPUsPerNode())
		}
		cpus = append(cpus, cpuInfo.CPUID)
		cpusInNodes[cpuInfo.NodeID] = cpus
		nodeFreeScores[cpuInfo.NodeID]++
		socketFreeScores[cpuInfo.SocketID]++
	}

	nodeIDs := make([]int, 0, len(cpusInNodes))
	for nodeID, cpus := range cpusInNodes {
		nodeIDs = append(nodeIDs, nodeID)
		sort.Ints(cpus)
		if a.maxRefCount > 1 {
			a.sortCPUsByRefCount(cpus)
		}
		if filterExclusive {
			cpus = a.extractCPU(cpus)
		}
		cpusInNodes[nodeID] = cpus
	}

	sort.Slice(nodeIDs, func(i, j int) bool {
		iCPUs := cpusInNodes[nodeIDs[i]]
		jCPUs := cpusInNodes[nodeIDs[j]]

		iCPUInfo := a.allocatableCPUs[iCPUs[0]]
		jCPUInfo := a.allocatableCPUs[jCPUs[0]]

		iNode := iCPUInfo.NodeID
		jNode := jCPUInfo.NodeID

		iSocket := iCPUInfo.SocketID
		jSocket := jCPUInfo.SocketID

		// Compute the number of available CPUs available on the same node as each core.
		iNodeFreeScore := nodeFreeScores[iNode]
		jNodeFreeScore := nodeFreeScores[jNode]

		// Compute the number of available CPUs available on the same socket as each core.
		iSocketFreeScore := socketFreeScores[iSocket]
		jSocketFreeScore := socketFreeScores[jSocket]

		if iNodeFreeScore != jNodeFreeScore {
			if a.numaAllocateStrategy == schedulingconfig.NUMAMostAllocated {
				return iNodeFreeScore < jNodeFreeScore
			} else {
				return iNodeFreeScore > jNodeFreeScore
			}
		}
		if iSocketFreeScore != jSocketFreeScore {
			if a.numaAllocateStrategy == schedulingconfig.NUMAMostAllocated {
				return iSocketFreeScore < jSocketFreeScore
			} else {
				return iSocketFreeScore > jSocketFreeScore
			}
		}
		return nodeIDs[i] < nodeIDs[j]
	})

	var result [][]int
	for _, node := range nodeIDs {
		result = append(result, cpusInNodes[node])
	}

	return result
}

// freeCPUsInSocket returns free logical cpus in sockets that sorted in ascending order.
func (a *cpuAccumulator) freeCPUsInSocket(filterExclusive bool) [][]int {
	allocatableCPUs := a.allocatableCPUs
	cpusInSockets := make(map[int][]int)
	for _, cpuInfo := range allocatableCPUs {
		if filterExclusive && a.isCPUExclusivePCPULevel(&cpuInfo) {
			continue
		}
		cpus := cpusInSockets[cpuInfo.SocketID]
		if len(cpus) == 0 {
			cpus = make([]int, 0, a.topology.CPUsPerSocket())
		}
		cpus = append(cpus, cpuInfo.CPUID)
		cpusInSockets[cpuInfo.SocketID] = cpus
	}

	socketIDs := make([]int, 0, len(cpusInSockets))
	for socketID, cpus := range cpusInSockets {
		socketIDs = append(socketIDs, socketID)
		sort.Ints(cpus)
		if a.maxRefCount > 1 {
			a.sortCPUsByRefCount(cpus)
		}
		if filterExclusive {
			cpus = a.extractCPU(cpus)
		}
		cpusInSockets[socketID] = cpus
	}

	sort.Slice(socketIDs, func(i, j int) bool {
		iSocketFreeScore := len(cpusInSockets[socketIDs[i]])
		jSocketFreeScore := len(cpusInSockets[socketIDs[j]])

		if iSocketFreeScore != jSocketFreeScore {
			if a.numaAllocateStrategy == schedulingconfig.NUMAMostAllocated {
				return iSocketFreeScore < jSocketFreeScore
			} else {
				return iSocketFreeScore > jSocketFreeScore
			}
		}
		return socketIDs[i] < socketIDs[j]
	})

	var result [][]int
	for _, s := range socketIDs {
		result = append(result, cpusInSockets[s])
	}

	return result
}

// Returns CPU IDs as a slice sorted in ascending order by:
// - socket affinity with result
// - number of CPUs available on the same socket
// - number of CPUs available on the same core
// - socket ID
// - node ID
// - reference count if maxRefCount > 1
// - core ID
func (a *cpuAccumulator) freeCPUs(filterExclusive bool) []int {
	allocatableCPUs := a.allocatableCPUs
	cpusInCores := make(map[int][]int)
	coresToSocket := make(map[int]int)
	coresToNode := make(map[int]int)
	nodeFreeScores := make(map[int]int)
	socketFreeScores := make(map[int]int)
	for _, cpuInfo := range allocatableCPUs {
		if filterExclusive && (a.isCPUExclusivePCPULevel(&cpuInfo) || a.isCPUExclusiveNUMANodeLevel(&cpuInfo)) {
			continue
		}

		cpus := cpusInCores[cpuInfo.CoreID]
		if len(cpus) == 0 {
			cpus = make([]int, 0, a.topology.CPUsPerCore())
		}
		cpus = append(cpus, cpuInfo.CPUID)
		cpusInCores[cpuInfo.CoreID] = cpus
		coresToSocket[cpuInfo.CoreID] = cpuInfo.SocketID
		coresToNode[cpuInfo.CoreID] = cpuInfo.NodeID
		nodeFreeScores[cpuInfo.NodeID]++
		socketFreeScores[cpuInfo.SocketID]++
	}

	socketColoScores := make(map[int]int)
	for socketID := range socketFreeScores {
		socketColoScore := a.topology.CPUDetails.CPUsInSockets(socketID).Intersection(a.result).Size()
		socketColoScores[socketID] = socketColoScore
	}

	cores := make([]int, 0, len(cpusInCores))
	for coreID := range cpusInCores {
		cores = append(cores, coreID)
	}

	sort.Slice(
		cores,
		func(i, j int) bool {
			iCore := cores[i]
			jCore := cores[j]

			iSocket := coresToSocket[iCore]
			jSocket := coresToSocket[jCore]

			// Compute the number of CPUs in the result reside on the same socket as each core.
			iSocketColoScore := socketColoScores[iSocket]
			jSocketColoScore := socketColoScores[jSocket]
			if iSocketColoScore != jSocketColoScore {
				return iSocketColoScore > jSocketColoScore
			}

			// Compute the number of available CPUs available on the same socket as each core.
			iSocketFreeScore := socketFreeScores[iSocket]
			jSocketFreeScore := socketFreeScores[jSocket]
			if iSocketFreeScore != jSocketFreeScore {
				if a.numaAllocateStrategy == schedulingconfig.NUMAMostAllocated {
					return iSocketFreeScore < jSocketFreeScore
				}
				return iSocketFreeScore > jSocketFreeScore
			}

			iNode := coresToNode[iCore]
			jNode := coresToNode[jCore]
			// Compute the number of available CPUs available on the same node as each core.
			iNodeFreeScore := nodeFreeScores[iNode]
			jNodeFreeScore := nodeFreeScores[jNode]
			if iNodeFreeScore != jNodeFreeScore {
				if a.numaAllocateStrategy == schedulingconfig.NUMAMostAllocated {
					return iNodeFreeScore < jNodeFreeScore
				} else {
					return iNodeFreeScore > jNodeFreeScore
				}
			}

			// Compute the number of available CPUs on each core.
			iCoreFreeScore := len(cpusInCores[iCore])
			jCoreFreeScore := len(cpusInCores[jCore])
			if iCoreFreeScore != jCoreFreeScore {
				return iCoreFreeScore < jCoreFreeScore
			}

			if iSocket != jSocket {
				return iSocket < jSocket
			}

			if a.maxRefCount > 1 {
				iRefCount := getCoreRefCount(allocatableCPUs, iCore)
				jRefCount := getCoreRefCount(allocatableCPUs, jCore)
				if iRefCount != jRefCount {
					return iRefCount < jRefCount
				}
			}

			return iCore < jCore
		})

	// For each core, append sorted CPU IDs to result.
	var result []int
	for _, core := range cores {
		cpus := cpusInCores[core]
		sort.Ints(cpus)
		if a.maxRefCount > 1 {
			a.sortCPUsByRefCount(cpus)
		}
		result = append(result, cpus...)
	}

	return result
}

func getCoreRefCount(details CPUDetails, core int) int {
	cpus := details.CPUsInCores(core).ToSliceNoSort()
	refCount := 0
	for _, cpu := range cpus {
		refCount += details[cpu].RefCount
	}
	return refCount
}

func (a *cpuAccumulator) sortCPUsByRefCount(cpus []int) {
	sort.Slice(cpus, func(i, j int) bool {
		iCPU := cpus[i]
		jCPU := cpus[j]
		iRefCount := a.allocatableCPUs[iCPU].RefCount
		jRefCount := a.allocatableCPUs[jCPU].RefCount
		if iRefCount != jRefCount {
			return iRefCount < jRefCount
		}
		return iCPU < jCPU
	})
}

func (a *cpuAccumulator) spreadCPUs(cpus []int) []int {
	if len(cpus) <= a.topology.CPUsPerCore() {
		return cpus
	}

	preparedCPUs := make([]int, len(cpus))
	copy(preparedCPUs, cpus)
	cpus = cpus[:0]
	for len(preparedCPUs) > 0 {
		var reservedCPUs []int
		coresFilter := make(map[int]struct{})
		for _, cpu := range preparedCPUs {
			cpuInfo := a.topology.CPUDetails[cpu]
			core := cpuInfo.CoreID
			if _, ok := coresFilter[core]; ok {
				reservedCPUs = append(reservedCPUs, cpu)
				continue
			}
			cpus = append(cpus, cpu)
			coresFilter[core] = struct{}{}
		}
		preparedCPUs = reservedCPUs
	}
	return cpus
}
