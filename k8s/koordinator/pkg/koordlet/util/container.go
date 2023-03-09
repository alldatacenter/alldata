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
	"path"
	"strconv"
	"strings"

	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

// @parentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/****.scope
func GetContainerCgroupPathWithKube(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	return GetContainerCgroupPathWithKubeByID(podParentDir, c.ContainerID)
}

// @parentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/****.scope
func GetContainerCgroupPathWithKubeByID(podParentDir string, containerID string) (string, error) {
	containerDir, err := system.CgroupPathFormatter.ContainerDirFn(containerID)
	if err != nil {
		return "", err
	}
	return path.Join(
		GetPodCgroupDirWithKube(podParentDir),
		containerDir,
	), nil
}

// @parentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cgroup.procs
func GetContainerCgroupCPUProcsPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.CPUProcs), nil
}

func GetContainerCgroupPerfPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return path.Join(system.Conf.CgroupRootDir, "perf_event/", containerPath), nil
}

func GetContainerCgroupCPUAcctUsagePath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.CPUAcctUsage), nil
}

func GetContainerCgroupCPUAcctPSIPath(podParentDir string, c *corev1.ContainerStatus) (PSIPath, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return PSIPath{}, err
	}
	// psi file saved in cpuacct for cgroup v1 file system
	return PSIPath{
		CPU: system.GetCgroupFilePath(containerPath, system.CPUAcctCPUPressure),
		Mem: system.GetCgroupFilePath(containerPath, system.CPUAcctMemoryPressure),
		IO:  system.GetCgroupFilePath(containerPath, system.CPUAcctIOPressure),
	}, nil
}

func GetContainerCgroupMemStatPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.MemoryStat), nil
}

func GetContainerCgroupCPUStatPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.CPUStat), nil
}

func GetContainerCgroupMemLimitPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.MemoryLimit), nil
}

func GetContainerCgroupCPUSharePath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.CPUShares), nil
}

func GetContainerCgroupCFSPeriodPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.CPUCFSPeriod), nil
}

func GetContainerCgroupCFSQuotaPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.CPUCFSQuota), nil
}

func GetContainerCurTasksPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupPathWithKube(podParentDir, c)
	if err != nil {
		return "", err
	}
	return system.GetCgroupFilePath(containerPath, system.CPUTasks), nil
}

func GetContainerCurCPUShare(podParentDir string, c *corev1.ContainerStatus) (int64, error) {
	cgroupPath, err := GetContainerCgroupCPUSharePath(podParentDir, c)
	if err != nil {
		return 0, err
	}
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(strings.TrimSpace(string(rawContent)), 10, 64)
}

func GetContainerCurCFSPeriod(podParentDir string, c *corev1.ContainerStatus) (int64, error) {
	cgroupPath, err := GetContainerCgroupCFSPeriodPath(podParentDir, c)
	if err != nil {
		return 0, err
	}
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(strings.TrimSpace(string(rawContent)), 10, 64)
}

func GetContainerCurCFSQuota(podParentDir string, c *corev1.ContainerStatus) (int64, error) {
	cgroupPath, err := GetContainerCgroupCFSQuotaPath(podParentDir, c)
	if err != nil {
		return 0, err
	}
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(strings.TrimSpace(string(rawContent)), 10, 64)
}

func GetContainerCurMemLimitBytes(podParentDir string, c *corev1.ContainerStatus) (int64, error) {
	cgroupPath, err := GetContainerCgroupMemLimitPath(podParentDir, c)
	if err != nil {
		return 0, err
	}
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return 0, err
	}
	return strconv.ParseInt(strings.TrimSpace(string(rawContent)), 10, 64)
}

// GetContainerCurTasks gets the TIDs of the given container's cgroup.
// DEPRECATED: use resourceexecutor.CgroupReader instead.
func GetContainerCurTasks(podParentDir string, c *corev1.ContainerStatus) ([]int, error) {
	cgroupPath, err := GetContainerCurTasksPath(podParentDir, c)
	if err != nil {
		return nil, err
	}
	return system.GetCgroupCurTasks(cgroupPath)
}

func GetContainerBaseCFSQuota(container *corev1.Container) int64 {
	cpuMilliLimit := util.GetContainerMilliCPULimit(container)
	if cpuMilliLimit <= 0 {
		return -1
	} else {
		return cpuMilliLimit * system.CFSBasePeriodValue / 1000
	}
}

func GetPIDsInPod(podParentDir string, cs []corev1.ContainerStatus) ([]uint32, error) {
	pids := make([]uint32, 0)
	for i := range cs {
		p, err := GetPIDsInContainer(podParentDir, &cs[i])
		if err != nil {
			return nil, err
		}
		pids = append(pids, p...)
	}
	return pids, nil
}

func GetPIDsInContainer(podParentDir string, c *corev1.ContainerStatus) ([]uint32, error) {
	cgroupPath, err := GetContainerCgroupCPUProcsPath(podParentDir, c)
	if err != nil {
		return nil, err
	}
	rawContent, err := os.ReadFile(cgroupPath)
	if err != nil {
		return nil, err
	}
	pidStrs := strings.Fields(strings.TrimSpace(string(rawContent)))
	pids := make([]uint32, len(pidStrs))

	for i := 0; i < len(pids); i++ {
		p, err := strconv.ParseUint(pidStrs[i], 10, 32)
		if err != nil {
			return nil, err
		}
		pids[i] = uint32(p)
	}
	return pids, nil
}
