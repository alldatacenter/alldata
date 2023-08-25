/*
Copyright 2017 The Kubernetes Authors.

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

package kubelet

import (
	"fmt"
	"sort"

	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/kubelet/cm/cpumanager/topology"
	"k8s.io/kubernetes/pkg/kubelet/cm/cpuset"
)

type LoopControl int

const (
	Continue LoopControl = iota
	Break
)

type mapIntInt map[int]int

func (m mapIntInt) Clone() mapIntInt {
	cp := make(mapIntInt, len(m))
	for k, v := range m {
		cp[k] = v
	}
	return cp
}

func (m mapIntInt) Keys() []int {
	var keys []int
	for k := range m {
		keys = append(keys, k)
	}
	return keys
}

func (m mapIntInt) Values(keys ...int) []int {
	if keys == nil {
		keys = m.Keys()
	}
	var values []int
	for _, k := range keys {
		values = append(values, m[k])
	}
	return values
}

func min(x, y int) int {
	if x < y {
		return x
	}
	return y
}

type numaOrSocketsFirstFuncs interface {
	takeFullFirstLevel()
	takeFullSecondLevel()
	sortAvailableNUMANodes() []int
	sortAvailableSockets() []int
	sortAvailableCores() []int
}

type numaFirst struct{ acc *cpuAccumulator }
type socketsFirst struct{ acc *cpuAccumulator }

var _ numaOrSocketsFirstFuncs = (*numaFirst)(nil)
var _ numaOrSocketsFirstFuncs = (*socketsFirst)(nil)

// If NUMA nodes are higher in the memory hierarchy than sockets, then we take
// from the set of NUMA Nodes as the first level.
func (n *numaFirst) takeFullFirstLevel() {
	n.acc.takeFullNUMANodes()
}

// If NUMA nodes are higher in the memory hierarchy than sockets, then we take
// from the set of sockets as the second level.
func (n *numaFirst) takeFullSecondLevel() {
	n.acc.takeFullSockets()
}

// If NUMA nodes are higher in the memory hierarchy than sockets, then just
// sort the NUMA nodes directly, and return them.
func (n *numaFirst) sortAvailableNUMANodes() []int {
	numas := n.acc.details.NUMANodes().ToSliceNoSort()
	n.acc.sort(numas, n.acc.details.CPUsInNUMANodes)
	return numas
}

// If NUMA nodes are higher in the memory hierarchy than sockets, then we need
// to pull the set of sockets out of each sorted NUMA node, and accumulate the
// partial order across them.
func (n *numaFirst) sortAvailableSockets() []int {
	var result []int
	for _, numa := range n.sortAvailableNUMANodes() {
		sockets := n.acc.details.SocketsInNUMANodes(numa).ToSliceNoSort()
		n.acc.sort(sockets, n.acc.details.CPUsInSockets)
		result = append(result, sockets...)
	}
	return result
}

// If NUMA nodes are higher in the memory hierarchy than sockets, then
// cores sit directly below sockets in the memory hierarchy.
func (n *numaFirst) sortAvailableCores() []int {
	var result []int
	for _, socket := range n.acc.sortAvailableSockets() {
		cores := n.acc.details.CoresInSockets(socket).ToSliceNoSort()
		n.acc.sort(cores, n.acc.details.CPUsInCores)
		result = append(result, cores...)
	}
	return result
}

// If sockets are higher in the memory hierarchy than NUMA nodes, then we take
// from the set of sockets as the first level.
func (s *socketsFirst) takeFullFirstLevel() {
	s.acc.takeFullSockets()
}

// If sockets are higher in the memory hierarchy than NUMA nodes, then we take
// from the set of NUMA Nodes as the second level.
func (s *socketsFirst) takeFullSecondLevel() {
	s.acc.takeFullNUMANodes()
}

// If sockets are higher in the memory hierarchy than NUMA nodes, then we need
// to pull the set of NUMA nodes out of each sorted Socket, and accumulate the
// partial order across them.
func (s *socketsFirst) sortAvailableNUMANodes() []int {
	var result []int
	for _, socket := range s.sortAvailableSockets() {
		numas := s.acc.details.NUMANodesInSockets(socket).ToSliceNoSort()
		s.acc.sort(numas, s.acc.details.CPUsInNUMANodes)
		result = append(result, numas...)
	}
	return result
}

// If sockets are higher in the memory hierarchy than NUMA nodes, then just
// sort the sockets directly, and return them.
func (s *socketsFirst) sortAvailableSockets() []int {
	sockets := s.acc.details.Sockets().ToSliceNoSort()
	s.acc.sort(sockets, s.acc.details.CPUsInSockets)
	return sockets
}

// If sockets are higher in the memory hierarchy than NUMA nodes, then cores
// sit directly below NUMA Nodes in the memory hierarchy.
func (s *socketsFirst) sortAvailableCores() []int {
	var result []int
	for _, numa := range s.acc.sortAvailableNUMANodes() {
		cores := s.acc.details.CoresInNUMANodes(numa).ToSliceNoSort()
		s.acc.sort(cores, s.acc.details.CPUsInCores)
		result = append(result, cores...)
	}
	return result
}

type cpuAccumulator struct {
	topo               *topology.CPUTopology
	details            topology.CPUDetails
	numCPUsNeeded      int
	result             cpuset.CPUSet
	numaOrSocketsFirst numaOrSocketsFirstFuncs
}

func newCPUAccumulator(topo *topology.CPUTopology, availableCPUs cpuset.CPUSet, numCPUs int) *cpuAccumulator {
	acc := &cpuAccumulator{
		topo:          topo,
		details:       topo.CPUDetails.KeepOnly(availableCPUs),
		numCPUsNeeded: numCPUs,
		result:        cpuset.NewCPUSet(),
	}

	// if topo.NumSockets >= topo.NumNUMANodes {
	// 	acc.numaOrSocketsFirst = &numaFirst{acc}
	// } else {
	// 	acc.numaOrSocketsFirst = &socketsFirst{acc}
	// }
	acc.numaOrSocketsFirst = &socketsFirst{acc}

	return acc
}

// Returns true if the supplied NUMANode is fully available in `topoDetails`.
func (a *cpuAccumulator) isNUMANodeFree(numaID int) bool {
	return a.details.CPUsInNUMANodes(numaID).Size() == a.topo.CPUDetails.CPUsInNUMANodes(numaID).Size()
}

// Returns true if the supplied socket is fully available in `topoDetails`.
func (a *cpuAccumulator) isSocketFree(socketID int) bool {
	return a.details.CPUsInSockets(socketID).Size() == a.topo.CPUsPerSocket()
}

// Returns true if the supplied core is fully available in `topoDetails`.
func (a *cpuAccumulator) isCoreFree(coreID int) bool {
	return a.details.CPUsInCores(coreID).Size() == a.topo.CPUsPerCore()
}

// Returns free NUMA Node IDs as a slice sorted by sortAvailableNUMANodes().
func (a *cpuAccumulator) freeNUMANodes() []int {
	free := []int{}
	for _, numa := range a.sortAvailableNUMANodes() {
		if a.isNUMANodeFree(numa) {
			free = append(free, numa)
		}
	}
	return free
}

// Returns free socket IDs as a slice sorted by sortAvailableSockets().
func (a *cpuAccumulator) freeSockets() []int {
	free := []int{}
	for _, socket := range a.sortAvailableSockets() {
		if a.isSocketFree(socket) {
			free = append(free, socket)
		}
	}
	return free
}

// Returns free core IDs as a slice sorted by sortAvailableCores().
func (a *cpuAccumulator) freeCores() []int {
	free := []int{}
	for _, core := range a.sortAvailableCores() {
		if a.isCoreFree(core) {
			free = append(free, core)
		}
	}
	return free
}

// Returns free CPU IDs as a slice sorted by sortAvailableCPUs().
func (a *cpuAccumulator) freeCPUs() []int {
	return a.sortAvailableCPUs()
}

// Sorts the provided list of NUMA nodes/sockets/cores/cpus referenced in 'ids'
// by the number of available CPUs contained within them (smallest to largest).
// The 'getCPU()' paramater defines the function that should be called to
// retrieve the list of available CPUs for the type being referenced. If two
// NUMA nodes/sockets/cores/cpus have the same number of available CPUs, they
// are sorted in ascending order by their id.
func (a *cpuAccumulator) sort(ids []int, getCPUs func(ids ...int) cpuset.CPUSet) {
	sort.Slice(ids,
		func(i, j int) bool {
			iCPUs := getCPUs(ids[i])
			jCPUs := getCPUs(ids[j])
			if iCPUs.Size() < jCPUs.Size() {
				return true
			}
			if iCPUs.Size() > jCPUs.Size() {
				return false
			}
			return ids[i] < ids[j]
		})
}

// Sort all NUMA nodes with free CPUs.
func (a *cpuAccumulator) sortAvailableNUMANodes() []int {
	return a.numaOrSocketsFirst.sortAvailableNUMANodes()
}

// Sort all sockets with free CPUs.
func (a *cpuAccumulator) sortAvailableSockets() []int {
	return a.numaOrSocketsFirst.sortAvailableSockets()
}

// Sort all cores with free CPUs:
func (a *cpuAccumulator) sortAvailableCores() []int {
	return a.numaOrSocketsFirst.sortAvailableCores()
}

// Sort all available CPUs:
// - First by core using sortAvailableCores().
// - Then within each core, using the sort() algorithm defined above.
func (a *cpuAccumulator) sortAvailableCPUs() []int {
	var result []int
	for _, core := range a.sortAvailableCores() {
		cpus := a.details.CPUsInCores(core).ToSliceNoSort()
		sort.Ints(cpus)
		result = append(result, cpus...)
	}
	return result
}

func (a *cpuAccumulator) take(cpus cpuset.CPUSet) {
	a.result = a.result.Union(cpus)
	a.details = a.details.KeepOnly(a.details.CPUs().Difference(a.result))
	a.numCPUsNeeded -= cpus.Size()
}

func (a *cpuAccumulator) takeFullNUMANodes() {
	for _, numa := range a.freeNUMANodes() {
		cpusInNUMANode := a.topo.CPUDetails.CPUsInNUMANodes(numa)
		if !a.needs(cpusInNUMANode.Size()) {
			continue
		}
		klog.V(4).InfoS("takeFullNUMANodes: claiming NUMA node", "numa", numa)
		a.take(cpusInNUMANode)
	}
}

func (a *cpuAccumulator) takeFullSockets() {
	for _, socket := range a.freeSockets() {
		cpusInSocket := a.topo.CPUDetails.CPUsInSockets(socket)
		if !a.needs(cpusInSocket.Size()) {
			continue
		}
		klog.V(4).InfoS("takeFullSockets: claiming socket", "socket", socket)
		a.take(cpusInSocket)
	}
}

func (a *cpuAccumulator) takeFullCores() {
	for _, core := range a.freeCores() {
		cpusInCore := a.topo.CPUDetails.CPUsInCores(core)
		if !a.needs(cpusInCore.Size()) {
			continue
		}
		klog.V(4).InfoS("takeFullCores: claiming core", "core", core)
		a.take(cpusInCore)
	}
}

func (a *cpuAccumulator) takeRemainingCPUs() {
	for _, cpu := range a.sortAvailableCPUs() {
		klog.V(4).InfoS("takeRemainingCPUs: claiming CPU", "cpu", cpu)
		a.take(cpuset.NewCPUSet(cpu))
		if a.isSatisfied() {
			return
		}
	}
}

func (a *cpuAccumulator) rangeNUMANodesNeededToSatisfy(cpuGroupSize int) (int, int) {
	// Get the total number of NUMA nodes in the system.
	numNUMANodes := a.topo.CPUDetails.NUMANodes().Size()

	// Get the total number of NUMA nodes that have CPUs available on them.
	numNUMANodesAvailable := a.details.NUMANodes().Size()

	// Get the total number of CPUs in the system.
	numCPUs := a.topo.CPUDetails.CPUs().Size()

	// Get the total number of 'cpuGroups' in the system.
	numCPUGroups := (numCPUs-1)/cpuGroupSize + 1

	// Calculate the number of 'cpuGroups' per NUMA Node in the system (rounding up).
	numCPUGroupsPerNUMANode := (numCPUGroups-1)/numNUMANodes + 1

	// Calculate the number of available 'cpuGroups' across all NUMA nodes as
	// well as the number of 'cpuGroups' that need to be allocated (rounding up).
	numCPUGroupsNeeded := (a.numCPUsNeeded-1)/cpuGroupSize + 1

	// Calculate the minimum number of numa nodes required to satisfy the
	// allocation (rounding up).
	minNUMAs := (numCPUGroupsNeeded-1)/numCPUGroupsPerNUMANode + 1

	// Calculate the maximum number of numa nodes required to satisfy the allocation.
	maxNUMAs := min(numCPUGroupsNeeded, numNUMANodesAvailable)

	return minNUMAs, maxNUMAs
}

func (a *cpuAccumulator) needs(n int) bool {
	return a.numCPUsNeeded >= n
}

func (a *cpuAccumulator) isSatisfied() bool {
	return a.numCPUsNeeded < 1
}

func (a *cpuAccumulator) isFailed() bool {
	return a.numCPUsNeeded > a.details.CPUs().Size()
}

// iterateCombinations walks through all n-choose-k subsets of size k in n and
// calls function 'f()' on each subset. For example, if n={0,1,2}, and k=2,
// then f() will be called on the subsets {0,1}, {0,2}. and {1,2}. If f() ever
// returns 'Break', we break early and exit the loop.
func (a *cpuAccumulator) iterateCombinations(n []int, k int, f func([]int) LoopControl) {
	if k < 1 {
		return
	}

	var helper func(n []int, k int, start int, accum []int, f func([]int) LoopControl) LoopControl
	helper = func(n []int, k int, start int, accum []int, f func([]int) LoopControl) LoopControl {
		if k == 0 {
			return f(accum)
		}
		for i := start; i <= len(n)-k; i++ {
			control := helper(n, k-1, i+1, append(accum, n[i]), f)
			if control == Break {
				return Break
			}
		}
		return Continue
	}

	helper(n, k, 0, []int{}, f)
}

func takeByTopologyNUMAPacked(topo *topology.CPUTopology, availableCPUs cpuset.CPUSet, numCPUs int) (cpuset.CPUSet, error) {
	acc := newCPUAccumulator(topo, availableCPUs, numCPUs)
	if acc.isSatisfied() {
		return acc.result, nil
	}
	if acc.isFailed() {
		return cpuset.NewCPUSet(), fmt.Errorf("not enough cpus available to satisfy request")
	}

	// Algorithm: topology-aware best-fit
	// 1. Acquire whole NUMA nodes and sockets, if available and the container
	//    requires at least a NUMA node or socket's-worth of CPUs. If NUMA
	//    Nodes map to 1 or more sockets, pull from NUMA nodes first.
	//    Otherwise pull from sockets first.
	acc.numaOrSocketsFirst.takeFullFirstLevel()
	if acc.isSatisfied() {
		return acc.result, nil
	}
	acc.numaOrSocketsFirst.takeFullSecondLevel()
	if acc.isSatisfied() {
		return acc.result, nil
	}

	// 2. Acquire whole cores, if available and the container requires at least
	//    a core's-worth of CPUs.
	acc.takeFullCores()
	if acc.isSatisfied() {
		return acc.result, nil
	}

	// 3. Acquire single threads, preferring to fill partially-allocated cores
	//    on the same sockets as the whole cores we have already taken in this
	//    allocation.
	acc.takeRemainingCPUs()
	if acc.isSatisfied() {
		return acc.result, nil
	}

	return cpuset.NewCPUSet(), fmt.Errorf("failed to allocate cpus")
}

func TakeByTopology(availableCPUs cpuset.CPUSet, numCPUs int, cpuTopology *topology.CPUTopology) (cpuset.CPUSet, error) {
	return takeByTopologyNUMAPacked(cpuTopology, availableCPUs, numCPUs)
}
