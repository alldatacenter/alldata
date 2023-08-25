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
	"path/filepath"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_GetContainerCgroupPath(t *testing.T) {
	helper := system.NewFileTestUtil(t)
	defer helper.Cleanup()
	helper.SetCgroupsV2(false)
	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	type args struct {
		name            string
		fn              func(podParentDir string, c *corev1.ContainerStatus) (string, error)
		resourceType    system.ResourceType
		containerStatus *corev1.ContainerStatus
		podParentDir    string
		expectPath      string
		expectErr       bool
	}

	tests := []args{
		{
			name:         "test_resource_not_found",
			resourceType: "unknown_resource",
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectErr: true,
		},
		{
			name:         "test_cpuacct_usage_path",
			resourceType: system.CPUAcctUsageName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpuacct/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpuacct.usage",
			expectErr:  false,
		},
		{
			name:         "test_cpuacct_usage_path_invalid",
			resourceType: system.CPUAcctUsageName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_memory_stat_path",
			resourceType: system.MemoryStatName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/memory/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/memory.stat",
			expectErr:  false,
		},
		{
			name:         "test_memory_stat_path",
			resourceType: system.MemoryStatName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_stat_path",
			resourceType: system.CPUStatName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.stat",
			expectErr:  false,
		},
		{
			name:         "test_cpu_stat_path_invalid",
			resourceType: system.CPUStatName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_memory_limit_path",
			resourceType: system.MemoryLimitName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/memory/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/memory.limit_in_bytes",
			expectErr:  false,
		},
		{
			name:         "test_memory_limit_path_invalid",
			resourceType: system.MemoryLimitName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_share_path",
			resourceType: system.CPUSharesName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.shares",
			expectErr:  false,
		},
		{
			name:         "test_cpu_share_path_invalid",
			resourceType: system.CPUSharesName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_cfs_period_path",
			resourceType: system.CPUCFSPeriodName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.cfs_period_us",
			expectErr:  false,
		},
		{
			name:         "test_cpu_cfs_period_path",
			resourceType: system.CPUCFSPeriodName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_cfs_quota_path",
			resourceType: system.CPUCFSQuotaName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.cfs_quota_us",
			expectErr:  false,
		},
		{
			name:         "test_cpu_cfs_quota_path_invalid",
			resourceType: system.CPUCFSQuotaName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_current_task_path",
			resourceType: system.CPUTasksName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/tasks",
			expectErr:  false,
		},
		{
			name:         "test_current_task_path_invalid",
			resourceType: system.CPUTasksName,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_procs_path",
			fn:           GetContainerCgroupCPUProcsPath,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cgroup.procs",
			expectErr:  false,
		},
		{
			name:         "test_cpu_procs_path_invalid",
			fn:           GetContainerCgroupCPUProcsPath,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_perf_path",
			fn:           GetContainerCgroupPerfPath,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/perf_event/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope",
			expectErr:  false,
		},
		{
			name:         "test_perf_path_invalid",
			fn:           GetContainerCgroupPerfPath,
			podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
	}

	for _, tt := range tests {
		system.Conf = system.NewDsModeConfig()
		t.Run(tt.name, func(t *testing.T) {
			var gotPath string
			var gotErr error
			if len(tt.resourceType) > 0 {
				gotPath, gotErr = GetContainerCgroupPath(tt.podParentDir, tt.containerStatus, tt.resourceType)
			} else {
				gotPath, gotErr = tt.fn(tt.podParentDir, tt.containerStatus)
			}
			assert.Equal(t, tt.expectPath, gotPath, "checkPath")
			assert.Equal(t, tt.expectErr, gotErr != nil, fmt.Sprintf("checkErr: %v", gotErr))
		})
	}
}

func TestGetPIDsInContainer(t *testing.T) {
	helper := system.NewFileTestUtil(t)
	defer helper.Cleanup()
	helper.SetCgroupsV2(false)
	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	type args struct {
		podParentDir string
		c            *corev1.ContainerStatus
	}
	dir := t.TempDir()
	system.Conf.CgroupRootDir = dir

	podCgroupPath := "/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cgroup.procs"
	sysCgroupPath := filepath.Join(dir, podCgroupPath)
	if err := writeCgroupContent(sysCgroupPath, []byte("12\n23")); err != nil {
		t.Fatal(err)
	}

	tests := []struct {
		name    string
		args    args
		want    []uint32
		wantErr bool
	}{
		{
			name: "cgroup",
			args: args{
				podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				c: &corev1.ContainerStatus{
					ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
				},
			},
			want: []uint32{12, 23},
		},
		{
			name: "not exist",
			args: args{
				podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				c: &corev1.ContainerStatus{
					ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4assf",
				},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetPIDsInContainer(tt.args.podParentDir, tt.args.c)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}
