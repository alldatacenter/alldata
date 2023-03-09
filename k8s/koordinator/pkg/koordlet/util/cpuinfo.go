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

package util

import (
	"context"
	"fmt"
	"os"
	"os/exec"
	"sort"
	"strconv"
	"strings"
	"time"

	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

const cpuCmdTimeout = 3 * time.Second

// CPUBasicInfo describes the cpu basic features and status
type CPUBasicInfo struct {
	HyperThreadEnabled bool   `json:"hyperThreadEnabled,omitempty"`
	CatL3CbmMask       string `json:"catL3CbmMask,omitempty"`
}

// ProcessorInfo describes the processor topology information of a single logic cpu, including the core, socket and numa
// node it belongs to
type ProcessorInfo struct {
	// logic CPU/ processor ID
	CPUID int32 `json:"cpu"`
	// physical CPU core ID
	CoreID int32 `json:"core"`
	// cpu socket ID
	SocketID int32 `json:"socket"`
	// numa node ID
	NodeID int32 `json:"node"`
	// L1 L2 cache ID
	L1dl1il2 string `json:"l1dl1il2"`
	// L3 cache ID
	L3 int32 `json:"l3"`
	// online
	Online string `json:"online"`
}

// CPUTotalInfo describes the total number infos of the local cpu, e.g. the number of cores, the number of numa nodes
type CPUTotalInfo struct {
	NumberCPUs    int32 `json:"numberCPUs"`
	NumberCores   int32 `json:"numberCores"`
	NumberSockets int32 `json:"numberSockets"`
	NumberNodes   int32 `json:"numberNodes"`
	NumberL3s     int32 `json:"numberL3s"`
}

// LocalCPUInfo contains the cpu information collected from the node
type LocalCPUInfo struct {
	// BasicInfo describe the cpu features and their status
	BasicInfo CPUBasicInfo `json:"basicInfo,omitempty"`
	// ProcessorInfos contains topology information of all available CPUs
	ProcessorInfos []ProcessorInfo `json:"processorInfos,omitempty"`
	// TotalInfo stores the numbers of cpu processors, cores, sockets and nodes
	TotalInfo CPUTotalInfo `json:"totalInfo,omitempty"`
}

// getHyperThreadEnabled returns whether the cpu is HT-enabled or not
// NOTE: currently only support intel cpu, otherwise it can always return false
func getHyperThreadEnabled() (bool, error) {
	out, err := os.ReadFile("/sys/devices/system/cpu/smt/active")
	if err == nil {
		active, err := strconv.Atoi(strings.TrimSpace(strings.Trim(string(out), "\n")))
		if err != nil {
			return false, err
		}
		return active == 1, nil
	}
	klog.V(5).Infof("read /sys/devices/system/cpu/smt/active err: %v, try `lscpu`", err)

	lsCPUStr, err := lsCPU("-y")
	for _, line := range strings.Split(lsCPUStr, "\n") {
		items := strings.Split(line, ":")
		if len(items) != 2 || !strings.Contains(items[0], "Thread(s) per core") {
			continue
		}
		threadsPerCore, err := strconv.Atoi(strings.TrimSpace(items[1]))
		if err != nil {
			return false, err
		}
		return threadsPerCore > 1, nil
	}
	klog.Warningf("failed to get HyperThreadEnabled, considered as disabled, err: %s", err)
	return false, nil
}

func getCPUBasicInfo() (*CPUBasicInfo, error) {
	cpuBasicInfo := &CPUBasicInfo{}
	var err error
	if cpuBasicInfo.HyperThreadEnabled, err = getHyperThreadEnabled(); err != nil {
		klog.V(5).Infof("get hyperthreadEnabled info error: %v", err)
	}
	if cpuBasicInfo.CatL3CbmMask, err = system.ReadCatL3CbmString(); err != nil {
		klog.V(5).Infof("get l3 cache bit mask error: %v", err)
	}
	return cpuBasicInfo, nil
}

func lsCPU(option string) (string, error) {
	ctx, cancel := context.WithTimeout(context.Background(), cpuCmdTimeout)
	defer cancel()

	executable, err := exec.LookPath("lscpu")
	if err != nil {
		return "", err
	}
	output, err := exec.CommandContext(ctx, executable, option).Output()
	if err != nil {
		return "", fmt.Errorf("failed to exec command %s, err: %v", executable, err)
	}
	return string(output), nil
}

func getProcessorInfos(lsCPUStr string) ([]ProcessorInfo, error) {
	if len(lsCPUStr) <= 0 {
		return nil, fmt.Errorf("lscpu output is empty")
	}

	var processorInfos []ProcessorInfo
	for _, line := range strings.Split(lsCPUStr, "\n") {
		items := strings.Fields(line)
		if len(items) < 6 {
			continue
		}
		cpu, err := strconv.ParseInt(items[0], 10, 32)
		if err != nil {
			continue
		}
		node, _ := strconv.ParseInt(items[1], 10, 32)
		socket, err := strconv.ParseInt(items[2], 10, 32)
		if err != nil {
			continue
		}
		core, err := strconv.ParseInt(items[3], 10, 32)
		if err != nil {
			continue
		}
		l1l2, l3, err := system.GetCacheInfo(items[4])
		if err != nil {
			continue
		}
		online := strings.TrimSpace(items[5])
		info := ProcessorInfo{
			CPUID:    int32(cpu),
			CoreID:   int32(core),
			SocketID: int32(socket),
			NodeID:   int32(node),
			L1dl1il2: l1l2,
			L3:       l3,
			Online:   online,
		}
		processorInfos = append(processorInfos, info)
	}
	if len(processorInfos) <= 0 {
		return nil, fmt.Errorf("no valid processor info")
	}

	// sorted by cpu topology
	// NOTE: in some cases, max(cpuId[...]) can be not equal to len(processors)
	sort.Slice(processorInfos, func(i, j int) bool {
		a, b := processorInfos[i], processorInfos[j]
		if a.NodeID != b.NodeID {
			return a.NodeID < b.NodeID
		}
		if a.SocketID != b.SocketID {
			return a.SocketID < b.SocketID
		}
		if a.CoreID != b.CoreID {
			return a.CoreID < b.CoreID
		}
		return a.CPUID < b.CPUID
	})

	return processorInfos, nil
}

func calculateCPUTotalInfo(processorInfos []ProcessorInfo) *CPUTotalInfo {
	cpuMap := map[int32]struct{}{}
	coreMap := map[int32]struct{}{}
	socketMap := map[int32]struct{}{}
	nodeMap := map[int32]struct{}{}
	l3Map := map[int32]struct{}{}
	for _, p := range processorInfos {
		cpuMap[p.CPUID] = struct{}{}
		coreMap[p.CoreID] = struct{}{}
		socketMap[p.SocketID] = struct{}{}
		nodeMap[p.NodeID] = struct{}{}
		l3Map[p.L3] = struct{}{}
	}
	return &CPUTotalInfo{
		NumberCPUs:    int32(len(cpuMap)),
		NumberCores:   int32(len(coreMap)),
		NumberSockets: int32(len(socketMap)),
		NumberNodes:   int32(len(nodeMap)),
		NumberL3s:     int32(len(l3Map)),
	}
}

// GetLocalCPUInfo returns the local cpu info for cpuset allocation, NUMA-aware scheduling
func GetLocalCPUInfo() (*LocalCPUInfo, error) {
	lsCPUStr, err := lsCPU("-e=CPU,NODE,SOCKET,CORE,CACHE,ONLINE")
	if err != nil {
		return nil, err
	}
	processorInfos, err := getProcessorInfos(lsCPUStr)
	if err != nil {
		return nil, err
	}
	totalInfo := calculateCPUTotalInfo(processorInfos)
	basicInfo, err := getCPUBasicInfo()
	if err != nil {
		return nil, err
	}
	return &LocalCPUInfo{
		BasicInfo:      *basicInfo,
		ProcessorInfos: processorInfos,
		TotalInfo:      *totalInfo,
	}, nil
}
