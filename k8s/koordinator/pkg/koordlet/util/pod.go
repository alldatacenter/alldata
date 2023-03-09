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
	"os"
	"path/filepath"
	"strconv"
	"strings"

	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

// @podKubeRelativeDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
func GetPodCgroupDirWithKube(podKubeRelativeDir string) string {
	return filepath.Join(system.CgroupPathFormatter.ParentDir, podKubeRelativeDir)
}

// @return like kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
func GetPodKubeRelativePath(pod *corev1.Pod) string {
	qosClass := util.GetKubeQosClass(pod)
	return filepath.Join(
		system.CgroupPathFormatter.QOSDirFn(qosClass),
		system.CgroupPathFormatter.PodDirFn(qosClass, string(pod.UID)),
	)
}

// @podParentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpuacct/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpuacct.usage
func GetPodCgroupCPUAcctUsagePath(podParentDir string) string {
	podPath := GetPodCgroupDirWithKube(podParentDir)
	return system.GetCgroupFilePath(podPath, system.CPUAcctUsage)
}

// @podParentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpu.shares
func GetPodCgroupCPUSharePath(podParentDir string) string {
	podPath := GetPodCgroupDirWithKube(podParentDir)
	return system.GetCgroupFilePath(podPath, system.CPUShares)
}

// @podParentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpu.cfs_period_us
func GetPodCgroupCFSPeriodPath(podParentDir string) string {
	podPath := GetPodCgroupDirWithKube(podParentDir)
	return system.GetCgroupFilePath(podPath, system.CPUCFSPeriod)
}

// @podParentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpu.cfs_quota_us
func GetPodCgroupCFSQuotaPath(podParentDir string) string {
	podPath := GetPodCgroupDirWithKube(podParentDir)
	return system.GetCgroupFilePath(podPath, system.CPUCFSQuota)
}

// @podParentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/memory/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/memory.stat
func GetPodCgroupMemStatPath(podParentDir string) string {
	podPath := GetPodCgroupDirWithKube(podParentDir)
	return system.GetCgroupFilePath(podPath, system.MemoryStat)
}

// @podParentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/memory/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/memory.limit_in_bytes
func GetPodCgroupMemLimitPath(podParentDir string) string {
	podPath := GetPodCgroupDirWithKube(podParentDir)
	return system.GetCgroupFilePath(podPath, system.MemoryLimit)
}

// @podParentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpu.stat
func GetPodCgroupCPUStatPath(podParentDir string) string {
	podPath := GetPodCgroupDirWithKube(podParentDir)
	return system.GetCgroupFilePath(podPath, system.CPUStat)
}

// @podParentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return {
//    CPU: /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cpu.pressure
//    Mem: /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/memory.pressure
//    IO:  /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/io.pressure
//  }
func GetPodCgroupCPUAcctPSIPath(podParentDir string) PSIPath {
	podPath := GetPodCgroupDirWithKube(podParentDir)
	return PSIPath{
		CPU: system.GetCgroupFilePath(podPath, system.CPUAcctCPUPressure),
		Mem: system.GetCgroupFilePath(podPath, system.CPUAcctMemoryPressure),
		IO:  system.GetCgroupFilePath(podPath, system.CPUAcctIOPressure),
	}
}

func GetKubeQoSByCgroupParent(cgroupDir string) corev1.PodQOSClass {
	if strings.Contains(cgroupDir, "besteffort") {
		return corev1.PodQOSBestEffort
	} else if strings.Contains(cgroupDir, "burstable") {
		return corev1.PodQOSBurstable
	}
	return corev1.PodQOSGuaranteed
}

func GetPodCurCPUShare(podParentDir string) (int64, error) {
	cgroupPath := GetPodCgroupCPUSharePath(podParentDir)
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(strings.TrimSpace(string(rawContent)), 10, 64)
}

func GetPodCurCFSPeriod(podParentDir string) (int64, error) {
	cgroupPath := GetPodCgroupCFSPeriodPath(podParentDir)
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(strings.TrimSpace(string(rawContent)), 10, 64)
}

func GetPodCurCFSQuota(podParentDir string) (int64, error) {
	cgroupPath := GetPodCgroupCFSQuotaPath(podParentDir)
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(strings.TrimSpace(string(rawContent)), 10, 64)
}

func GetPodCurMemLimitBytes(podParentDir string) (int64, error) {
	cgroupPath := GetPodCgroupMemLimitPath(podParentDir)
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(strings.TrimSpace(string(rawContent)), 10, 64)
}

// @return like kubepods.slice/kubepods-burstable.slice/
func GetPodQoSRelativePath(qosClass corev1.PodQOSClass) string {
	return filepath.Join(
		system.CgroupPathFormatter.ParentDir,
		system.CgroupPathFormatter.QOSDirFn(qosClass),
	)
}

// @return 7712555c_ce62_454a_9e18_9ff0217b8941 from kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice
func ParsePodID(basename string) (string, error) {
	return system.CgroupPathFormatter.PodIDParser(basename)
}

// @return 7712555c_ce62_454a_9e18_9ff0217b8941 from docker-7712555c_ce62_454a_9e18_9ff0217b8941.scope
func ParseContainerID(basename string) (string, error) {
	return system.CgroupPathFormatter.ContainerIDParser(basename)
}
