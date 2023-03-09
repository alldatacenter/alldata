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
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_GetRootCgroupCPUSetDirWithSystemdDriver(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	tests := []struct {
		name string
		args corev1.PodQOSClass
		want string
	}{
		{
			name: "default",
			args: "",
			want: "/host-cgroup/cpuset/kubepods.slice",
		},
		{
			name: "Guaranteed",
			args: corev1.PodQOSGuaranteed,
			want: "/host-cgroup/cpuset/kubepods.slice",
		},
		{
			name: "Burstable",
			args: corev1.PodQOSBurstable,
			want: "/host-cgroup/cpuset/kubepods.slice/kubepods-burstable.slice",
		},
		{
			name: "Best-effort",
			args: corev1.PodQOSBestEffort,
			want: "/host-cgroup/cpuset/kubepods.slice/kubepods-besteffort.slice",
		},
	}
	for _, tt := range tests {
		system.Conf = system.NewDsModeConfig()
		t.Run(tt.name, func(t *testing.T) {
			got := GetRootCgroupCPUSetDir(tt.args)
			if tt.want != got {
				t.Errorf("getRootCgroupDir want %v but got %v", tt.want, got)
			}
		})
	}
}

func Test_GetRootCgroupCPUSetDirWithCgroupfsDriver(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Cgroupfs)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	tests := []struct {
		name string
		args corev1.PodQOSClass
		want string
	}{
		{
			name: "default",
			args: "",
			want: "/host-cgroup/cpuset/kubepods",
		},
		{
			name: "Guaranteed",
			args: corev1.PodQOSGuaranteed,
			want: "/host-cgroup/cpuset/kubepods",
		},
		{
			name: "Burstable",
			args: corev1.PodQOSBurstable,
			want: "/host-cgroup/cpuset/kubepods/burstable",
		},
		{
			name: "Best-effort",
			args: corev1.PodQOSBestEffort,
			want: "/host-cgroup/cpuset/kubepods/besteffort",
		},
	}
	for _, tt := range tests {
		system.Conf = system.NewDsModeConfig()
		t.Run(tt.name, func(t *testing.T) {
			got := GetRootCgroupCPUSetDir(tt.args)
			if tt.want != got {
				t.Errorf("getRootCgroupDir want %v but got %v", tt.want, got)
			}
		})
	}
}

func Test_GetRootCgroupCurCPUSet(t *testing.T) {
	// prepare testing tmp files
	cgroupRootDir := t.TempDir()
	dname := filepath.Join(cgroupRootDir, system.CgroupCPUSetDir)
	err := os.MkdirAll(dname, 0700)
	if err != nil {
		t.Errorf("failed to prepare tmpdir in %v, err: %v", "GetRootCgroupCurCPUSet", err)
		return
	}
	fname := filepath.Join(dname, system.CPUSetCPUSName)
	_ = os.WriteFile(fname, []byte{'1', ',', '2'}, 0666)

	system.Conf = &system.Config{
		CgroupRootDir: cgroupRootDir,
	}
	// reset Formatter after testing
	rawParentDir := system.CgroupPathFormatter.ParentDir
	system.CgroupPathFormatter.ParentDir = ""
	defer func() {
		system.CgroupPathFormatter.ParentDir = rawParentDir
	}()

	wantCPUSet := []int32{1, 2}

	gotCPUSet, err := GetRootCgroupCurCPUSet(corev1.PodQOSGuaranteed)
	if err != nil {
		t.Errorf("failed to GetRootCgroupCurCPUSet, err: %v", err)
		return
	}
	if !reflect.DeepEqual(wantCPUSet, gotCPUSet) {
		t.Errorf("failed to GetRootCgroupCurCPUSet, want cpuset %v, got %v", wantCPUSet, gotCPUSet)
		return
	}
}

func Test_GetPodKubeRelativePath(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)
	system.Conf = system.NewDsModeConfig()

	assert := assert.New(t)

	testCases := []struct {
		name string
		pod  *corev1.Pod
		path string
	}{
		{
			name: "guaranteed",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: types.UID("uid1"),
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSGuaranteed,
				},
			},
			path: "/kubepods-poduid1.slice",
		},
		{
			name: "burstable",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: types.UID("uid1"),
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSBurstable,
				},
			},
			path: "kubepods-burstable.slice/kubepods-burstable-poduid1.slice",
		},
		{
			name: "besteffort",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: types.UID("uid1"),
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSBestEffort,
				},
			},
			path: "kubepods-besteffort.slice/kubepods-besteffort-poduid1.slice",
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			path := GetPodKubeRelativePath(tc.pod)
			assert.Equal(tc.path, path)
		})
	}
}

func Test_GetPodCgroupStatPath(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)

	assert := assert.New(t)

	testCases := []struct {
		name         string
		relativePath string
		path         string
		fn           func(p string) string
	}{
		{
			name:         "cpuacct",
			relativePath: "pod1",
			path:         "/host-cgroup/cpuacct/kubepods.slice/pod1/cpuacct.usage",
			fn: func(p string) string {
				return GetPodCgroupCPUAcctUsagePath(p)
			},
		},
		{
			name:         "cpushare",
			relativePath: "pod1",
			path:         "/host-cgroup/cpu/kubepods.slice/pod1/cpu.shares",
			fn: func(p string) string {
				return GetPodCgroupCPUSharePath(p)
			},
		},
		{
			name:         "cfsperiod",
			relativePath: "pod1",
			path:         "/host-cgroup/cpu/kubepods.slice/pod1/cpu.cfs_period_us",
			fn: func(p string) string {
				return GetPodCgroupCFSPeriodPath(p)
			},
		},
		{
			name:         "cfsperiod",
			relativePath: "pod1",
			path:         "/host-cgroup/cpu/kubepods.slice/pod1/cpu.cfs_quota_us",
			fn: func(p string) string {
				return GetPodCgroupCFSQuotaPath(p)
			},
		},
		{
			name:         "memorystat",
			relativePath: "pod1",
			path:         "/host-cgroup/memory/kubepods.slice/pod1/memory.stat",
			fn: func(p string) string {
				return GetPodCgroupMemStatPath(p)
			},
		},
		{
			name:         "memorylimit",
			relativePath: "pod1",
			path:         "/host-cgroup/memory/kubepods.slice/pod1/memory.limit_in_bytes",
			fn: func(p string) string {
				return GetPodCgroupMemLimitPath(p)
			},
		},
		{
			name:         "cpustat",
			relativePath: "pod1",
			path:         "/host-cgroup/cpu/kubepods.slice/pod1/cpu.stat",
			fn: func(p string) string {
				return GetPodCgroupCPUStatPath(p)
			},
		},
		{
			name:         "cpupressure",
			relativePath: "pod1",
			path:         "/host-cgroup/cpuacct/kubepods.slice/pod1/cpu.pressure",
			fn: func(p string) string {
				return GetPodCgroupCPUAcctPSIPath(p).CPU
			},
		},
		{
			name:         "mempressure",
			relativePath: "pod1",
			path:         "/host-cgroup/cpuacct/kubepods.slice/pod1/memory.pressure",
			fn: func(p string) string {
				return GetPodCgroupCPUAcctPSIPath(p).Mem
			},
		},
		{
			name:         "iopressure",
			relativePath: "pod1",
			path:         "/host-cgroup/cpuacct/kubepods.slice/pod1/io.pressure",
			fn: func(p string) string {
				return GetPodCgroupCPUAcctPSIPath(p).IO
			},
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			path := tc.fn(tc.relativePath)
			assert.Equal(tc.path, path)
		})
	}
}

func Test_GetKubeQoSByCgroupParent(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)
	assert := assert.New(t)

	testCases := []struct {
		name              string
		path              string
		wantPriorityClass corev1.PodQOSClass
	}{
		{
			name:              "burstable",
			path:              "kubepods-burstable.slice/kubepods-poduid1.slice",
			wantPriorityClass: corev1.PodQOSBurstable,
		},
		{
			name:              "besteffort",
			path:              "kubepods-besteffort.slice/kubepods-poduid1.slice",
			wantPriorityClass: corev1.PodQOSBestEffort,
		},
		{
			name:              "guaranteed",
			path:              "kubepods-poduid1.slice",
			wantPriorityClass: corev1.PodQOSGuaranteed,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			assert.Equal(tc.wantPriorityClass, GetKubeQoSByCgroupParent(tc.path))
		})
	}
}
