/*
Copyright 2022 The Koordinator Authors.
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
	"math"
	"path/filepath"
	"strconv"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/klog/v2"
	kubeletconfiginternal "k8s.io/kubernetes/pkg/kubelet/apis/config"
	"k8s.io/kubernetes/pkg/kubelet/cm/cpumanager"
	"k8s.io/kubernetes/pkg/kubelet/cm/cpumanager/topology"
	"k8s.io/kubernetes/pkg/kubelet/cm/cpuset"
	"k8s.io/kubernetes/pkg/kubelet/eviction"
	evictionapi "k8s.io/kubernetes/pkg/kubelet/eviction/api"
	"k8s.io/kubernetes/pkg/kubelet/stats/pidlimit"

	koordletutil "github.com/koordinator-sh/koordinator/pkg/koordlet/util"
)

func NewCPUTopology(cpuInfo *koordletutil.LocalCPUInfo) *topology.CPUTopology {
	cpuTopology := &topology.CPUTopology{
		NumCPUs:    int(cpuInfo.TotalInfo.NumberCPUs),
		NumCores:   int(cpuInfo.TotalInfo.NumberCores),
		NumSockets: int(cpuInfo.TotalInfo.NumberSockets),
		CPUDetails: topology.CPUDetails{},
	}
	for _, v := range cpuInfo.ProcessorInfos {
		cpuTopology.CPUDetails[int(v.CPUID)] = topology.CPUInfo{
			NUMANodeID: int(v.NodeID),
			SocketID:   int(v.SocketID),
			CoreID:     int(v.CoreID),
		}
	}
	return cpuTopology
}

func GetStaticCPUManagerPolicyReservedCPUs(topology *topology.CPUTopology, kubeletConfiguration *kubeletconfiginternal.KubeletConfiguration) (cpuset.CPUSet, error) {
	if kubeletConfiguration.CPUManagerPolicy != string(cpumanager.PolicyStatic) {
		return cpuset.CPUSet{}, nil
	}

	reservedCPUs, kubeReserved, systemReserved, err := GetKubeletReservedOptions(kubeletConfiguration, topology)
	if err != nil {
		return cpuset.CPUSet{}, err
	}

	nodeAllocatableReservation, err := GetNodeAllocatableReservation(topology.NumCPUs, 0,
		kubeletConfiguration.EvictionHard, systemReserved, kubeReserved, false)
	if err != nil {
		return cpuset.CPUSet{}, err
	}

	numReservedCPUs, err := getNumReservedCPUs(nodeAllocatableReservation)
	if err != nil {
		return cpuset.CPUSet{}, err
	}

	var reserved cpuset.CPUSet

	if reservedCPUs.Size() > 0 {
		reserved = reservedCPUs
	} else {
		// takeByTopology allocates CPUs associated with low-numbered cores from
		// allCPUs.
		//
		// For example: Given a system with 8 CPUs available and HT enabled,
		// if numReservedCPUs=2, then reserved={0,4}
		allCPUs := topology.CPUDetails.CPUs()
		reserved, _ = TakeByTopology(allCPUs, numReservedCPUs, topology)
	}

	if reserved.Size() != numReservedCPUs {
		err := fmt.Errorf("[cpumanager] unable to reserve the required amount of CPUs (size of %s did not equal %d)", reserved, numReservedCPUs)
		return cpuset.CPUSet{}, err
	}
	return reserved, nil
}

func getNumReservedCPUs(nodeAllocatableReservation corev1.ResourceList) (int, error) {
	reservedCPUs, ok := nodeAllocatableReservation[corev1.ResourceCPU]
	if !ok {
		// The static policy cannot initialize without this information.
		return 0, fmt.Errorf("[cpumanager] unable to determine reserved CPU resources for static policy")
	}
	if reservedCPUs.IsZero() {
		// The static policy requires this to be nonzero. Zero CPU reservation
		// would allow the shared pool to be completely exhausted. At that point
		// either we would violate our guarantee of exclusivity or need to evict
		// any pod that has at least one container that requires zero CPUs.
		// See the comments in policy_static.go for more details.
		return 0, fmt.Errorf("[cpumanager] the static policy requires systemreserved.cpu + kubereserved.cpu to be greater than zero")
	}

	// Take the ceiling of the reservation, since fractional CPUs cannot be
	// exclusively allocated.
	reservedCPUsFloat := float64(reservedCPUs.MilliValue()) / 1000
	numReservedCPUs := int(math.Ceil(reservedCPUsFloat))
	return numReservedCPUs, nil
}

func GetKubeletReservedOptions(kubeletConfiguration *kubeletconfiginternal.KubeletConfiguration, topology *topology.CPUTopology) (reservedSystemCPUs cpuset.CPUSet, kubeReserved, systemReserved corev1.ResourceList, err error) {
	reservedSystemCPUs, err = getReservedCPUs(topology, kubeletConfiguration.ReservedSystemCPUs)
	if err != nil {
		return
	}
	if reservedSystemCPUs.Size() > 0 {
		// at cmd option validation phase it is tested either --system-reserved-cgroup or --kube-reserved-cgroup is specified, so overwrite should be ok
		klog.InfoS("Option --reserved-cpus is specified, it will overwrite the cpu setting in KubeReserved and SystemReserved", "kubeReservedCPUs", kubeletConfiguration.KubeReserved, "systemReservedCPUs", kubeletConfiguration.SystemReserved)
		if kubeletConfiguration.KubeReserved != nil {
			delete(kubeletConfiguration.KubeReserved, "cpu")
		}
		if kubeletConfiguration.SystemReserved == nil {
			kubeletConfiguration.SystemReserved = make(map[string]string)
		}
		kubeletConfiguration.SystemReserved["cpu"] = strconv.Itoa(reservedSystemCPUs.Size())
		klog.InfoS("After cpu setting is overwritten", "kubeReservedCPUs", kubeletConfiguration.KubeReserved, "systemReservedCPUs", kubeletConfiguration.SystemReserved)
	}

	kubeReserved, err = parseResourceList(kubeletConfiguration.KubeReserved)
	if err != nil {
		return
	}
	systemReserved, err = parseResourceList(kubeletConfiguration.SystemReserved)
	if err != nil {
		return
	}
	return
}

func getReservedCPUs(topology *topology.CPUTopology, cpus string) (cpuset.CPUSet, error) {
	emptyCPUSet := cpuset.NewCPUSet()

	if cpus == "" {
		return emptyCPUSet, nil
	}

	reservedCPUSet, err := cpuset.Parse(cpus)
	if err != nil {
		return emptyCPUSet, fmt.Errorf("unable to parse reserved-cpus list: %s", err)
	}

	allCPUSet := topology.CPUDetails.CPUs()
	if !reservedCPUSet.IsSubsetOf(allCPUSet) {
		return emptyCPUSet, fmt.Errorf("reserved-cpus: %s is not a subset of online-cpus: %s", cpus, allCPUSet.String())
	}
	return reservedCPUSet, nil
}

// parseResourceList parses the given configuration map into an API
// ResourceList or returns an error.
func parseResourceList(m map[string]string) (corev1.ResourceList, error) {
	if len(m) == 0 {
		return nil, nil
	}
	rl := make(corev1.ResourceList)
	for k, v := range m {
		switch corev1.ResourceName(k) {
		// CPU, memory, local storage, and PID resources are supported.
		case corev1.ResourceCPU, corev1.ResourceMemory, corev1.ResourceEphemeralStorage, pidlimit.PIDs:
			q, err := resource.ParseQuantity(v)
			if err != nil {
				return nil, err
			}
			if q.Sign() == -1 {
				return nil, fmt.Errorf("resource quantity for %q cannot be negative: %v", k, v)
			}
			rl[corev1.ResourceName(k)] = q
		default:
			return nil, fmt.Errorf("cannot reserve %q resource", k)
		}
	}
	return rl, nil
}

func GetNodeAllocatableReservation(numCPUs int, totalMemoryInBytes int64, evictionHard map[string]string, systemReserved, kubeReserved corev1.ResourceList, experimentalNodeAllocatableIgnoreEvictionThreshold bool) (corev1.ResourceList, error) {
	hardEvictionThresholds, err := getKubeletHardEvictionThresholds(evictionHard, experimentalNodeAllocatableIgnoreEvictionThreshold)
	if err != nil {
		return nil, err
	}

	capacity := corev1.ResourceList{
		corev1.ResourceCPU:    *resource.NewMilliQuantity(int64(numCPUs*1000), resource.DecimalSI),
		corev1.ResourceMemory: *resource.NewQuantity(totalMemoryInBytes, resource.BinarySI),
	}

	evictionReservation := hardEvictionReservation(hardEvictionThresholds, capacity)
	result := make(corev1.ResourceList)
	for k := range capacity {
		value := resource.NewQuantity(0, resource.DecimalSI)
		if systemReserved != nil {
			value.Add(systemReserved[k])
		}
		if kubeReserved != nil {
			value.Add(kubeReserved[k])
		}
		if evictionReservation != nil {
			value.Add(evictionReservation[k])
		}
		if !value.IsZero() {
			result[k] = *value
		}
	}
	return result, nil
}

func getKubeletHardEvictionThresholds(evictionHard map[string]string, experimentalNodeAllocatableIgnoreEvictionThreshold bool) ([]evictionapi.Threshold, error) {
	var hardEvictionThresholds []evictionapi.Threshold
	// If the user requested to ignore eviction thresholds, then do not set valid values for hardEvictionThresholds here.
	if !experimentalNodeAllocatableIgnoreEvictionThreshold {
		var err error
		hardEvictionThresholds, err = eviction.ParseThresholdConfig([]string{}, evictionHard, nil, nil, nil)
		if err != nil {
			return nil, err
		}
	}
	return hardEvictionThresholds, nil
}

// hardEvictionReservation returns a resourcelist that includes reservation of resources based on hard eviction thresholds.
func hardEvictionReservation(thresholds []evictionapi.Threshold, capacity corev1.ResourceList) corev1.ResourceList {
	if len(thresholds) == 0 {
		return nil
	}
	ret := corev1.ResourceList{}
	for _, threshold := range thresholds {
		if threshold.Operator != evictionapi.OpLessThan {
			continue
		}
		switch threshold.Signal {
		case evictionapi.SignalMemoryAvailable:
			memoryCapacity := capacity[corev1.ResourceMemory]
			value := evictionapi.GetThresholdQuantity(threshold.Value, &memoryCapacity)
			ret[corev1.ResourceMemory] = *value
		case evictionapi.SignalNodeFsAvailable:
			storageCapacity := capacity[corev1.ResourceEphemeralStorage]
			value := evictionapi.GetThresholdQuantity(threshold.Value, &storageCapacity)
			ret[corev1.ResourceEphemeralStorage] = *value
		}
	}
	return ret
}

func GetCPUManagerStateFilePath(rootDirectory string) string {
	return filepath.Join(rootDirectory, "cpu_manager_state")
}
