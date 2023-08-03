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
	"fmt"
	"os"
	"path/filepath"
	"strconv"
	"strings"

	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

// GetContainerCgroupPath gets the file path of the given container's cgroup.
// @parentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cgroup.procs
func GetContainerCgroupPath(podParentDir string, c *corev1.ContainerStatus, resourceType system.ResourceType) (string, error) {
	resource, err := system.GetCgroupResource(resourceType)
	if err != nil {
		return "", fmt.Errorf("failed to get resource type %v, err: %w", resourceType, err)
	}
	containerPath, err := GetContainerCgroupParentDir(podParentDir, c)
	if err != nil {
		return "", fmt.Errorf("failed to get container cgroup path, err: %w", err)
	}
	return system.GetCgroupFilePath(containerPath, resource), nil
}

// @parentDir kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/
// @return /sys/fs/cgroup/cpu/kubepods.slice/kubepods-burstable.slice/kubepods-pod7712555c_ce62_454a_9e18_9ff0217b8941.slice/cgroup.procs
func GetContainerCgroupCPUProcsPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	return GetContainerCgroupPath(podParentDir, c, system.CPUProcsName)
}

func GetContainerCgroupPerfPath(podParentDir string, c *corev1.ContainerStatus) (string, error) {
	containerPath, err := GetContainerCgroupParentDir(podParentDir, c)
	if err != nil {
		return "", err
	}
	if system.GetCurrentCgroupVersion() == system.CgroupVersionV2 {
		return filepath.Join(system.Conf.CgroupRootDir, containerPath), nil
	}
	return filepath.Join(system.Conf.CgroupRootDir, "perf_event/", containerPath), nil
}

func GetContainerBaseCFSQuota(container *corev1.Container) int64 {
	cpuMilliLimit := util.GetContainerMilliCPULimit(container)
	if cpuMilliLimit <= 0 {
		return -1
	} else {
		return cpuMilliLimit * system.CFSBasePeriodValue / 1000
	}
}

// ParseContainerID parse container ID from the container base path.
// e.g. 7712555c_ce62_454a_9e18_9ff0217b8941 from docker-7712555c_ce62_454a_9e18_9ff0217b8941.scope
func ParseContainerID(basename string) (string, error) {
	return system.CgroupPathFormatter.ContainerIDParser(basename)
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
