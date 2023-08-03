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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_GetPodCgroupParentDir(t *testing.T) {
	system.Conf = system.NewDsModeConfig()

	testCases := []struct {
		name         string
		cgroupDriver system.CgroupDriverType
		pod          *corev1.Pod
		path         string
	}{
		{
			name:         "systemd guaranteed",
			cgroupDriver: system.Systemd,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: types.UID("uid1"),
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSGuaranteed,
				},
			},
			path: "kubepods.slice/kubepods-poduid1.slice",
		},
		{
			name:         "systemd burstable",
			cgroupDriver: system.Systemd,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: types.UID("uid1"),
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSBurstable,
				},
			},
			path: "kubepods.slice/kubepods-burstable.slice/kubepods-burstable-poduid1.slice",
		},
		{
			name:         "systemd besteffort",
			cgroupDriver: system.Systemd,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: types.UID("uid1"),
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSBestEffort,
				},
			},
			path: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-poduid1.slice",
		},
		{
			name:         "cgroupfs guaranteed",
			cgroupDriver: system.Cgroupfs,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: types.UID("uid1"),
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSGuaranteed,
				},
			},
			path: "kubepods/poduid1",
		},
		{
			name:         "cgroupfs besteffort",
			cgroupDriver: system.Cgroupfs,
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID: types.UID("uid1"),
				},
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSBestEffort,
				},
			},
			path: "kubepods/besteffort/poduid1",
		},
	}
	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			system.SetupCgroupPathFormatter(tc.cgroupDriver)

			path := GetPodCgroupParentDir(tc.pod)
			assert.Equal(t, tc.path, path)
		})
	}
}

func Test_GetKubeQoSByCgroupParent(t *testing.T) {
	system.SetupCgroupPathFormatter(system.Systemd)

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
			assert.Equal(t, tc.wantPriorityClass, GetKubeQoSByCgroupParent(tc.path))
		})
	}
}

func TestGetContainerCgroupParentDir_SystemdDriver(t *testing.T) {
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
				podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
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
				podParentDir: "kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
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
			got, err := GetContainerCgroupParentDir(tt.args.podParentDir, tt.args.c)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func TestGetContainerCgroupParentDir_CgroupfsDriver(t *testing.T) {
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
				podParentDir: "kubepods/besteffort/pod6553a60b-2b97-442a-b6da-a5704d81dd98/",
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
				podParentDir: "kubepods/besteffort/pod6553a60b-2b97-442a-b6da-a5704d81dd98/",
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
			got, err := GetContainerCgroupParentDir(tt.args.podParentDir, tt.args.c)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}
