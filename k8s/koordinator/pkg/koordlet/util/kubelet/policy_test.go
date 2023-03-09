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
	"k8s.io/kubernetes/pkg/kubelet/cm/cpumanager/topology"
)

var (
	topoSingleSocketHT = &topology.CPUTopology{
		NumCPUs:    8,
		NumSockets: 1,
		NumCores:   4,
		CPUDetails: map[int]topology.CPUInfo{
			0: {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			1: {CoreID: 1, SocketID: 0, NUMANodeID: 0},
			2: {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			3: {CoreID: 3, SocketID: 0, NUMANodeID: 0},
			4: {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			5: {CoreID: 1, SocketID: 0, NUMANodeID: 0},
			6: {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			7: {CoreID: 3, SocketID: 0, NUMANodeID: 0},
		},
	}

	topoDualSocketHT = &topology.CPUTopology{
		NumCPUs:    12,
		NumSockets: 2,
		NumCores:   6,
		CPUDetails: map[int]topology.CPUInfo{
			0:  {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			1:  {CoreID: 1, SocketID: 1, NUMANodeID: 1},
			2:  {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			3:  {CoreID: 3, SocketID: 1, NUMANodeID: 1},
			4:  {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			5:  {CoreID: 5, SocketID: 1, NUMANodeID: 1},
			6:  {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			7:  {CoreID: 1, SocketID: 1, NUMANodeID: 1},
			8:  {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			9:  {CoreID: 3, SocketID: 1, NUMANodeID: 1},
			10: {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			11: {CoreID: 5, SocketID: 1, NUMANodeID: 1},
		},
	}

	// fake topology for testing purposes only
	topoTripleSocketHT = &topology.CPUTopology{
		NumCPUs:    18,
		NumSockets: 3,
		NumCores:   9,
		CPUDetails: map[int]topology.CPUInfo{
			0:  {CoreID: 0, SocketID: 1, NUMANodeID: 1},
			1:  {CoreID: 0, SocketID: 1, NUMANodeID: 1},
			2:  {CoreID: 1, SocketID: 1, NUMANodeID: 1},
			3:  {CoreID: 1, SocketID: 1, NUMANodeID: 1},
			4:  {CoreID: 2, SocketID: 1, NUMANodeID: 1},
			5:  {CoreID: 2, SocketID: 1, NUMANodeID: 1},
			6:  {CoreID: 3, SocketID: 0, NUMANodeID: 0},
			7:  {CoreID: 3, SocketID: 0, NUMANodeID: 0},
			8:  {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			9:  {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			10: {CoreID: 5, SocketID: 0, NUMANodeID: 0},
			11: {CoreID: 5, SocketID: 0, NUMANodeID: 0},
			12: {CoreID: 6, SocketID: 2, NUMANodeID: 2},
			13: {CoreID: 6, SocketID: 2, NUMANodeID: 2},
			14: {CoreID: 7, SocketID: 2, NUMANodeID: 2},
			15: {CoreID: 7, SocketID: 2, NUMANodeID: 2},
			16: {CoreID: 8, SocketID: 2, NUMANodeID: 2},
			17: {CoreID: 8, SocketID: 2, NUMANodeID: 2},
		},
	}

	/*
		Topology from dual xeon gold 6230; lscpu excerpt
		CPU(s):              80
		On-line CPU(s) list: 0-79
		Thread(s) per core:  2
		Core(s) per socket:  20
		Socket(s):           2
		NUMA node(s):        4
		NUMA node0 CPU(s):   0-9,40-49
		NUMA node1 CPU(s):   10-19,50-59
		NUMA node2 CPU(s):   20-29,60-69
		NUMA node3 CPU(s):   30-39,70-79
	*/
	topoDualSocketMultiNumaPerSocketHT = &topology.CPUTopology{
		NumCPUs:    80,
		NumSockets: 2,
		NumCores:   40,
		CPUDetails: map[int]topology.CPUInfo{
			0:  {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			1:  {CoreID: 1, SocketID: 0, NUMANodeID: 0},
			2:  {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			3:  {CoreID: 3, SocketID: 0, NUMANodeID: 0},
			4:  {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			5:  {CoreID: 5, SocketID: 0, NUMANodeID: 0},
			6:  {CoreID: 6, SocketID: 0, NUMANodeID: 0},
			7:  {CoreID: 7, SocketID: 0, NUMANodeID: 0},
			8:  {CoreID: 8, SocketID: 0, NUMANodeID: 0},
			9:  {CoreID: 9, SocketID: 0, NUMANodeID: 0},
			10: {CoreID: 10, SocketID: 0, NUMANodeID: 1},
			11: {CoreID: 11, SocketID: 0, NUMANodeID: 1},
			12: {CoreID: 12, SocketID: 0, NUMANodeID: 1},
			13: {CoreID: 13, SocketID: 0, NUMANodeID: 1},
			14: {CoreID: 14, SocketID: 0, NUMANodeID: 1},
			15: {CoreID: 15, SocketID: 0, NUMANodeID: 1},
			16: {CoreID: 16, SocketID: 0, NUMANodeID: 1},
			17: {CoreID: 17, SocketID: 0, NUMANodeID: 1},
			18: {CoreID: 18, SocketID: 0, NUMANodeID: 1},
			19: {CoreID: 19, SocketID: 0, NUMANodeID: 1},
			20: {CoreID: 20, SocketID: 1, NUMANodeID: 2},
			21: {CoreID: 21, SocketID: 1, NUMANodeID: 2},
			22: {CoreID: 22, SocketID: 1, NUMANodeID: 2},
			23: {CoreID: 23, SocketID: 1, NUMANodeID: 2},
			24: {CoreID: 24, SocketID: 1, NUMANodeID: 2},
			25: {CoreID: 25, SocketID: 1, NUMANodeID: 2},
			26: {CoreID: 26, SocketID: 1, NUMANodeID: 2},
			27: {CoreID: 27, SocketID: 1, NUMANodeID: 2},
			28: {CoreID: 28, SocketID: 1, NUMANodeID: 2},
			29: {CoreID: 29, SocketID: 1, NUMANodeID: 2},
			30: {CoreID: 30, SocketID: 1, NUMANodeID: 3},
			31: {CoreID: 31, SocketID: 1, NUMANodeID: 3},
			32: {CoreID: 32, SocketID: 1, NUMANodeID: 3},
			33: {CoreID: 33, SocketID: 1, NUMANodeID: 3},
			34: {CoreID: 34, SocketID: 1, NUMANodeID: 3},
			35: {CoreID: 35, SocketID: 1, NUMANodeID: 3},
			36: {CoreID: 36, SocketID: 1, NUMANodeID: 3},
			37: {CoreID: 37, SocketID: 1, NUMANodeID: 3},
			38: {CoreID: 38, SocketID: 1, NUMANodeID: 3},
			39: {CoreID: 39, SocketID: 1, NUMANodeID: 3},
			40: {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			41: {CoreID: 1, SocketID: 0, NUMANodeID: 0},
			42: {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			43: {CoreID: 3, SocketID: 0, NUMANodeID: 0},
			44: {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			45: {CoreID: 5, SocketID: 0, NUMANodeID: 0},
			46: {CoreID: 6, SocketID: 0, NUMANodeID: 0},
			47: {CoreID: 7, SocketID: 0, NUMANodeID: 0},
			48: {CoreID: 8, SocketID: 0, NUMANodeID: 0},
			49: {CoreID: 9, SocketID: 0, NUMANodeID: 0},
			50: {CoreID: 10, SocketID: 0, NUMANodeID: 1},
			51: {CoreID: 11, SocketID: 0, NUMANodeID: 1},
			52: {CoreID: 12, SocketID: 0, NUMANodeID: 1},
			53: {CoreID: 13, SocketID: 0, NUMANodeID: 1},
			54: {CoreID: 14, SocketID: 0, NUMANodeID: 1},
			55: {CoreID: 15, SocketID: 0, NUMANodeID: 1},
			56: {CoreID: 16, SocketID: 0, NUMANodeID: 1},
			57: {CoreID: 17, SocketID: 0, NUMANodeID: 1},
			58: {CoreID: 18, SocketID: 0, NUMANodeID: 1},
			59: {CoreID: 19, SocketID: 0, NUMANodeID: 1},
			60: {CoreID: 20, SocketID: 1, NUMANodeID: 2},
			61: {CoreID: 21, SocketID: 1, NUMANodeID: 2},
			62: {CoreID: 22, SocketID: 1, NUMANodeID: 2},
			63: {CoreID: 23, SocketID: 1, NUMANodeID: 2},
			64: {CoreID: 24, SocketID: 1, NUMANodeID: 2},
			65: {CoreID: 25, SocketID: 1, NUMANodeID: 2},
			66: {CoreID: 26, SocketID: 1, NUMANodeID: 2},
			67: {CoreID: 27, SocketID: 1, NUMANodeID: 2},
			68: {CoreID: 28, SocketID: 1, NUMANodeID: 2},
			69: {CoreID: 29, SocketID: 1, NUMANodeID: 2},
			70: {CoreID: 30, SocketID: 1, NUMANodeID: 3},
			71: {CoreID: 31, SocketID: 1, NUMANodeID: 3},
			72: {CoreID: 32, SocketID: 1, NUMANodeID: 3},
			73: {CoreID: 33, SocketID: 1, NUMANodeID: 3},
			74: {CoreID: 34, SocketID: 1, NUMANodeID: 3},
			75: {CoreID: 35, SocketID: 1, NUMANodeID: 3},
			76: {CoreID: 36, SocketID: 1, NUMANodeID: 3},
			77: {CoreID: 37, SocketID: 1, NUMANodeID: 3},
			78: {CoreID: 38, SocketID: 1, NUMANodeID: 3},
			79: {CoreID: 39, SocketID: 1, NUMANodeID: 3},
		},
	}
	/*
		FAKE Topology from dual xeon gold 6230
		(see: topoDualSocketMultiNumaPerSocketHT).
		We flip NUMA cells and Sockets to exercise the code.
		TODO(fromanirh): replace with a real-world topology
		once we find a suitable one.
	*/
	fakeTopoMultiSocketDualSocketPerNumaHT = &topology.CPUTopology{
		NumCPUs:    80,
		NumSockets: 4,
		NumCores:   40,
		CPUDetails: map[int]topology.CPUInfo{
			0:  {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			1:  {CoreID: 1, SocketID: 0, NUMANodeID: 0},
			2:  {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			3:  {CoreID: 3, SocketID: 0, NUMANodeID: 0},
			4:  {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			5:  {CoreID: 5, SocketID: 0, NUMANodeID: 0},
			6:  {CoreID: 6, SocketID: 0, NUMANodeID: 0},
			7:  {CoreID: 7, SocketID: 0, NUMANodeID: 0},
			8:  {CoreID: 8, SocketID: 0, NUMANodeID: 0},
			9:  {CoreID: 9, SocketID: 0, NUMANodeID: 0},
			10: {CoreID: 10, SocketID: 1, NUMANodeID: 0},
			11: {CoreID: 11, SocketID: 1, NUMANodeID: 0},
			12: {CoreID: 12, SocketID: 1, NUMANodeID: 0},
			13: {CoreID: 13, SocketID: 1, NUMANodeID: 0},
			14: {CoreID: 14, SocketID: 1, NUMANodeID: 0},
			15: {CoreID: 15, SocketID: 1, NUMANodeID: 0},
			16: {CoreID: 16, SocketID: 1, NUMANodeID: 0},
			17: {CoreID: 17, SocketID: 1, NUMANodeID: 0},
			18: {CoreID: 18, SocketID: 1, NUMANodeID: 0},
			19: {CoreID: 19, SocketID: 1, NUMANodeID: 0},
			20: {CoreID: 20, SocketID: 2, NUMANodeID: 1},
			21: {CoreID: 21, SocketID: 2, NUMANodeID: 1},
			22: {CoreID: 22, SocketID: 2, NUMANodeID: 1},
			23: {CoreID: 23, SocketID: 2, NUMANodeID: 1},
			24: {CoreID: 24, SocketID: 2, NUMANodeID: 1},
			25: {CoreID: 25, SocketID: 2, NUMANodeID: 1},
			26: {CoreID: 26, SocketID: 2, NUMANodeID: 1},
			27: {CoreID: 27, SocketID: 2, NUMANodeID: 1},
			28: {CoreID: 28, SocketID: 2, NUMANodeID: 1},
			29: {CoreID: 29, SocketID: 2, NUMANodeID: 1},
			30: {CoreID: 30, SocketID: 3, NUMANodeID: 1},
			31: {CoreID: 31, SocketID: 3, NUMANodeID: 1},
			32: {CoreID: 32, SocketID: 3, NUMANodeID: 1},
			33: {CoreID: 33, SocketID: 3, NUMANodeID: 1},
			34: {CoreID: 34, SocketID: 3, NUMANodeID: 1},
			35: {CoreID: 35, SocketID: 3, NUMANodeID: 1},
			36: {CoreID: 36, SocketID: 3, NUMANodeID: 1},
			37: {CoreID: 37, SocketID: 3, NUMANodeID: 1},
			38: {CoreID: 38, SocketID: 3, NUMANodeID: 1},
			39: {CoreID: 39, SocketID: 3, NUMANodeID: 1},
			40: {CoreID: 0, SocketID: 0, NUMANodeID: 0},
			41: {CoreID: 1, SocketID: 0, NUMANodeID: 0},
			42: {CoreID: 2, SocketID: 0, NUMANodeID: 0},
			43: {CoreID: 3, SocketID: 0, NUMANodeID: 0},
			44: {CoreID: 4, SocketID: 0, NUMANodeID: 0},
			45: {CoreID: 5, SocketID: 0, NUMANodeID: 0},
			46: {CoreID: 6, SocketID: 0, NUMANodeID: 0},
			47: {CoreID: 7, SocketID: 0, NUMANodeID: 0},
			48: {CoreID: 8, SocketID: 0, NUMANodeID: 0},
			49: {CoreID: 9, SocketID: 0, NUMANodeID: 0},
			50: {CoreID: 10, SocketID: 1, NUMANodeID: 0},
			51: {CoreID: 11, SocketID: 1, NUMANodeID: 0},
			52: {CoreID: 12, SocketID: 1, NUMANodeID: 0},
			53: {CoreID: 13, SocketID: 1, NUMANodeID: 0},
			54: {CoreID: 14, SocketID: 1, NUMANodeID: 0},
			55: {CoreID: 15, SocketID: 1, NUMANodeID: 0},
			56: {CoreID: 16, SocketID: 1, NUMANodeID: 0},
			57: {CoreID: 17, SocketID: 1, NUMANodeID: 0},
			58: {CoreID: 18, SocketID: 1, NUMANodeID: 0},
			59: {CoreID: 19, SocketID: 1, NUMANodeID: 0},
			60: {CoreID: 20, SocketID: 2, NUMANodeID: 1},
			61: {CoreID: 21, SocketID: 2, NUMANodeID: 1},
			62: {CoreID: 22, SocketID: 2, NUMANodeID: 1},
			63: {CoreID: 23, SocketID: 2, NUMANodeID: 1},
			64: {CoreID: 24, SocketID: 2, NUMANodeID: 1},
			65: {CoreID: 25, SocketID: 2, NUMANodeID: 1},
			66: {CoreID: 26, SocketID: 2, NUMANodeID: 1},
			67: {CoreID: 27, SocketID: 2, NUMANodeID: 1},
			68: {CoreID: 28, SocketID: 2, NUMANodeID: 1},
			69: {CoreID: 29, SocketID: 2, NUMANodeID: 1},
			70: {CoreID: 30, SocketID: 3, NUMANodeID: 1},
			71: {CoreID: 31, SocketID: 3, NUMANodeID: 1},
			72: {CoreID: 32, SocketID: 3, NUMANodeID: 1},
			73: {CoreID: 33, SocketID: 3, NUMANodeID: 1},
			74: {CoreID: 34, SocketID: 3, NUMANodeID: 1},
			75: {CoreID: 35, SocketID: 3, NUMANodeID: 1},
			76: {CoreID: 36, SocketID: 3, NUMANodeID: 1},
			77: {CoreID: 37, SocketID: 3, NUMANodeID: 1},
			78: {CoreID: 38, SocketID: 3, NUMANodeID: 1},
			79: {CoreID: 39, SocketID: 3, NUMANodeID: 1},
		},
	}
)
