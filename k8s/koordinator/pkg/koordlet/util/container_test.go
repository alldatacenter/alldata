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
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_getContainerCgroupPathWithSystemdDriver(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	type args struct {
		podParentDir string
		c            *corev1.ContainerStatus
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "docker-container",
			args: args{
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				c: &corev1.ContainerStatus{
					ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
				},
			},
			want:    "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope",
			wantErr: false,
		},
		{
			name: "containerd-container",
			args: args{
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				c: &corev1.ContainerStatus{
					ContainerID: "containerd://413715dc061efe71c16ef989f2f2ff5cf999a7dc905bb7078eda82cbb38210ec",
				},
			},
			want:    "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/cri-containerd-413715dc061efe71c16ef989f2f2ff5cf999a7dc905bb7078eda82cbb38210ec.scope",
			wantErr: false,
		},
		{
			name: "invalid-container",
			args: args{
				podParentDir: "",
				c: &corev1.ContainerStatus{
					ContainerID: "invalid",
				},
			},
			want:    "",
			wantErr: true,
		},
		{
			name: "unsupported-container",
			args: args{
				podParentDir: "",
				c: &corev1.ContainerStatus{
					ContainerID: "crio://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
				},
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		system.Conf = system.NewDsModeConfig()
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetContainerCgroupPathWithKube(tt.args.podParentDir, tt.args.c)
			if (err != nil) != tt.wantErr {
				t.Errorf("getContainerCgroupPath() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getContainerCgroupPath() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getContainerCgroupPathWithCgroupfsDriver(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Cgroupfs)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	type args struct {
		podParentDir string
		c            *corev1.ContainerStatus
	}
	tests := []struct {
		name    string
		args    args
		want    string
		wantErr bool
	}{
		{
			name: "docker-container",
			args: args{
				podParentDir: "besteffort/pod6553a60b-2b97-442a-b6da-a5704d81dd98/",
				c: &corev1.ContainerStatus{
					ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
				},
			},
			want:    "kubepods/besteffort/pod6553a60b-2b97-442a-b6da-a5704d81dd98/703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			wantErr: false,
		},
		{
			name: "containerd-container",
			args: args{
				podParentDir: "besteffort/pod6553a60b-2b97-442a-b6da-a5704d81dd98/",
				c: &corev1.ContainerStatus{
					ContainerID: "containerd://413715dc061efe71c16ef989f2f2ff5cf999a7dc905bb7078eda82cbb38210ec",
				},
			},
			want:    "kubepods/besteffort/pod6553a60b-2b97-442a-b6da-a5704d81dd98/413715dc061efe71c16ef989f2f2ff5cf999a7dc905bb7078eda82cbb38210ec",
			wantErr: false,
		},
		{
			name: "invalid-container",
			args: args{
				podParentDir: "",
				c: &corev1.ContainerStatus{
					ContainerID: "invalid",
				},
			},
			want:    "",
			wantErr: true,
		},
		{
			name: "unsupported-container",
			args: args{
				podParentDir: "",
				c: &corev1.ContainerStatus{
					ContainerID: "crio://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
				},
			},
			want:    "",
			wantErr: true,
		},
	}
	for _, tt := range tests {
		system.Conf = system.NewDsModeConfig()
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetContainerCgroupPathWithKube(tt.args.podParentDir, tt.args.c)
			if (err != nil) != tt.wantErr {
				t.Errorf("getContainerCgroupPath() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("getContainerCgroupPath() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_GetContainerCurTasks(t *testing.T) {
	type args struct {
		podParentDir string
		c            *corev1.ContainerStatus
	}
	type field struct {
		containerParentDir string
		tasksFileStr       string
		invalidPath        bool
	}
	tests := []struct {
		name    string
		args    args
		field   field
		want    []int
		wantErr bool
	}{
		{
			name: "throw an error for empty input",
			args: args{
				c: &corev1.ContainerStatus{},
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse tasks correctly",
			field: field{
				containerParentDir: "pod0/cri-containerd-1.scope",
				tasksFileStr:       "22264\n22265\n22266\n22267\n29925\n29926\n37587\n41340\n45169\n",
			},
			args: args{
				podParentDir: "pod0",
				c: &corev1.ContainerStatus{
					ContainerID: "containerd://1",
				},
			},
			want:    []int{22264, 22265, 22266, 22267, 29925, 29926, 37587, 41340, 45169},
			wantErr: false,
		},
		{
			name: "throw an error for invalid path",
			field: field{
				containerParentDir: "pod0/cri-containerd-1.scope",
				tasksFileStr:       "22264\n22265\n22266\n22267\n29925\n29926\n37587\n41340\n45169\n",
				invalidPath:        true,
			},
			args: args{
				podParentDir: "pod0",
				c: &corev1.ContainerStatus{
					ContainerID: "containerd://1",
				},
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse error",
			field: field{
				containerParentDir: "pod0/cri-containerd-1.scope",
				tasksFileStr:       "22264\n22265\n22266\n22587\nabs",
			},
			args: args{
				podParentDir: "pod0",
				c: &corev1.ContainerStatus{
					ContainerID: "containerd://1",
				},
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "parse empty",
			field: field{
				containerParentDir: "pod0/cri-containerd-1.scope",
				tasksFileStr:       "",
			},
			args: args{
				podParentDir: "pod0",
				c: &corev1.ContainerStatus{
					ContainerID: "containerd://1",
				},
			},
			want:    nil,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cgroupRootDir := t.TempDir()

			dname := filepath.Join(cgroupRootDir, system.CgroupCPUDir, tt.field.containerParentDir)
			err := os.MkdirAll(dname, 0700)
			assert.NoError(t, err)
			fname := filepath.Join(dname, system.CPUTasksName)
			_ = os.WriteFile(fname, []byte(tt.field.tasksFileStr), 0666)

			system.Conf = &system.Config{
				CgroupRootDir: cgroupRootDir,
			}
			// reset Formatter after testing
			rawParentDir := system.CgroupPathFormatter.ParentDir
			system.CgroupPathFormatter.ParentDir = ""
			defer func() {
				system.CgroupPathFormatter.ParentDir = rawParentDir
			}()
			if tt.field.invalidPath {
				system.Conf.CgroupRootDir = "invalidPath"
			}

			got, err := GetContainerCurTasks(tt.args.podParentDir, tt.args.c)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_GetContainerCgroupXXXPath(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	type args struct {
		name            string
		fn              func(podParentDir string, c *corev1.ContainerStatus) (string, error)
		containerStatus *corev1.ContainerStatus
		podParentDir    string
		expectPath      string
		expectErr       bool
	}

	tests := []args{
		{
			name:         "test_cpuacct_usage_path",
			fn:           GetContainerCgroupCPUAcctUsagePath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpuacct/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpuacct.usage",
			expectErr:  false,
		},
		{
			name:         "test_cpuacct_usage_path_invalid",
			fn:           GetContainerCgroupCPUAcctUsagePath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_memory_stat_path",
			fn:           GetContainerCgroupMemStatPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/memory/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/memory.stat",
			expectErr:  false,
		},
		{
			name:         "test_memory_stat_path",
			fn:           GetContainerCgroupMemStatPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_stat_path",
			fn:           GetContainerCgroupCPUStatPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.stat",
			expectErr:  false,
		},
		{
			name:         "test_cpu_stat_path_invalid",
			fn:           GetContainerCgroupCPUStatPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_memory_limit_path",
			fn:           GetContainerCgroupMemLimitPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/memory/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/memory.limit_in_bytes",
			expectErr:  false,
		},
		{
			name:         "test_memory_limit_path_invalid",
			fn:           GetContainerCgroupMemLimitPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_share_path",
			fn:           GetContainerCgroupCPUSharePath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.shares",
			expectErr:  false,
		},
		{
			name:         "test_cpu_share_path_invalid",
			fn:           GetContainerCgroupCPUSharePath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_cfs_period_path",
			fn:           GetContainerCgroupCFSPeriodPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.cfs_period_us",
			expectErr:  false,
		},
		{
			name:         "test_cpu_cfs_period_path",
			fn:           GetContainerCgroupCFSPeriodPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_cfs_quota_path",
			fn:           GetContainerCgroupCFSQuotaPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.cfs_quota_us",
			expectErr:  false,
		},
		{
			name:         "test_cpu_cfs_quota_path_invalid",
			fn:           GetContainerCgroupCFSQuotaPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_current_task_path",
			fn:           GetContainerCurTasksPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/tasks",
			expectErr:  false,
		},
		{
			name:         "test_current_task_path_invalid",
			fn:           GetContainerCurTasksPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_cpu_procs_path",
			fn:           GetContainerCgroupCPUProcsPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cgroup.procs",
			expectErr:  false,
		},
		{
			name:         "test_cpu_procs_path_invalid",
			fn:           GetContainerCgroupCPUProcsPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "",
			expectErr:  true,
		},
		{
			name:         "test_perf_path",
			fn:           GetContainerCgroupPerfPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: "/host-cgroup/perf_event/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope",
			expectErr:  false,
		},
		{
			name:         "test_perf_path_invalid",
			fn:           GetContainerCgroupPerfPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
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
			gotPath, gotErr := tt.fn(tt.podParentDir, tt.containerStatus)
			assert.Equal(t, tt.expectPath, gotPath, "checkPath")
			assert.Equal(t, tt.expectErr, gotErr != nil, "checkError")
		})
	}
}

func Test_GetContainerPSIPath(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	type args struct {
		name            string
		fn              func(podParentDir string, c *corev1.ContainerStatus) (PSIPath, error)
		containerStatus *corev1.ContainerStatus
		podParentDir    string
		expectPath      PSIPath
		expectErr       bool
	}

	tests := []args{
		{
			name:         "test_psi_path",
			fn:           GetContainerCgroupCPUAcctPSIPath,
			podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
			containerStatus: &corev1.ContainerStatus{
				ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
			},
			expectPath: PSIPath{
				CPU: "/host-cgroup/cpuacct/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cpu.pressure",
				Mem: "/host-cgroup/cpuacct/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/memory.pressure",
				IO:  "/host-cgroup/cpuacct/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/io.pressure",
			},
			expectErr: true,
		},
	}

	for _, tt := range tests {
		system.Conf = system.NewDsModeConfig()
		t.Run(tt.name, func(t *testing.T) {
			gotPath, gotErr := tt.fn(tt.podParentDir, tt.containerStatus)
			assert.Equal(t, tt.expectPath.CPU, gotPath.CPU, "checkPathCPU")
			assert.Equal(t, tt.expectPath.Mem, gotPath.Mem, "checkPathMem")
			assert.Equal(t, tt.expectPath.IO, gotPath.IO, "checkPathIO")
			assert.Equal(t, tt.expectErr, gotErr == nil, "checkError")
		})
	}
}

func TestGetPIDsInContainer(t *testing.T) {
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
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				c: &corev1.ContainerStatus{
					ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
				},
			},
			want: []uint32{12, 23},
		},
		{
			name: "not exist",
			args: args{
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
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
			if (err != nil) != tt.wantErr {
				t.Errorf("GetPIDsInContainer() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetPIDsInContainer() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestGetPIDsInPod(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	dir := t.TempDir()
	system.Conf.CgroupRootDir = dir

	p1 := "/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cgroup.procs"
	p1CgroupPath := filepath.Join(dir, p1)
	if err := writeCgroupContent(p1CgroupPath, []byte("12\n23")); err != nil {
		t.Fatal(err)
	}

	p2 := "/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4acff.scope/cgroup.procs"
	p2CgroupPath := filepath.Join(dir, p2)
	if err := writeCgroupContent(p2CgroupPath, []byte("45\n67")); err != nil {
		t.Fatal(err)
	}
	type args struct {
		podParentDir string
		cs           []corev1.ContainerStatus
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
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				cs: []corev1.ContainerStatus{
					{
						ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
					},
					{
						ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4acff",
					},
				},
			},
			want:    []uint32{12, 23, 45, 67},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetPIDsInPod(tt.args.podParentDir, tt.args.cs)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetPIDsInPod() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetPIDsInPod() = %v, want %v", got, tt.want)
			}
		})
	}
}

func writeCgroupContent(filePath string, content []byte) error {
	err := os.MkdirAll(filepath.Dir(filePath), os.ModePerm)
	if err != nil {
		return err
	}
	return os.WriteFile(filePath, content, 0655)
}
