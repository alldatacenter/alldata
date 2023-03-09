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

package metricsadvisor

import (
	"os"
	"path"
	"reflect"
	"testing"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/util/system"
)

func Test_gpuUsageDetailRecord_GetNodeGPUUsage(t *testing.T) {
	type fields struct {
		deviceCount      int
		devices          []*device
		processesMetrics map[uint32][]*rawGPUMetric
	}
	tests := []struct {
		name   string
		fields fields
		want   []metriccache.GPUMetric
	}{
		{
			name: "single device",
			fields: fields{
				deviceCount: 1,
				devices: []*device{
					{Minor: 0, DeviceUUID: "test-device1", MemoryTotal: 8000},
				},
				processesMetrics: map[uint32][]*rawGPUMetric{
					122: {{SMUtil: 70, MemoryUsed: 1500}},
				},
			},
			want: []metriccache.GPUMetric{
				{
					DeviceUUID:  "test-device1",
					Minor:       0,
					SMUtil:      70,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
				},
			},
		},
		{
			name: "multiple device",
			fields: fields{
				deviceCount: 2,
				devices: []*device{
					{Minor: 0, DeviceUUID: "test-device1", MemoryTotal: 8000},
					{Minor: 1, DeviceUUID: "test-device2", MemoryTotal: 9000},
				},
				processesMetrics: map[uint32][]*rawGPUMetric{
					122: {{SMUtil: 70, MemoryUsed: 1500}, nil},
					222: {nil, {SMUtil: 50, MemoryUsed: 1000}},
				},
			},
			want: []metriccache.GPUMetric{
				{
					DeviceUUID:  "test-device1",
					Minor:       0,
					SMUtil:      70,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
				},
				{
					DeviceUUID:  "test-device2",
					Minor:       1,
					SMUtil:      50,
					MemoryUsed:  *resource.NewQuantity(1000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(9000, resource.BinarySI),
				},
			},
		},
		{
			name: "process on multiple device",
			fields: fields{
				deviceCount: 2,
				devices: []*device{
					{Minor: 0, DeviceUUID: "test-device1", MemoryTotal: 8000},
					{Minor: 1, DeviceUUID: "test-device2", MemoryTotal: 9000},
				},
				processesMetrics: map[uint32][]*rawGPUMetric{
					122: {{SMUtil: 70, MemoryUsed: 1500}, {SMUtil: 30, MemoryUsed: 1000}},
					222: {{SMUtil: 20, MemoryUsed: 1000}, {SMUtil: 50, MemoryUsed: 1000}},
				},
			},
			want: []metriccache.GPUMetric{
				{
					DeviceUUID:  "test-device1",
					Minor:       0,
					SMUtil:      90,
					MemoryUsed:  *resource.NewQuantity(2500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
				},
				{
					DeviceUUID:  "test-device2",
					Minor:       1,
					SMUtil:      80,
					MemoryUsed:  *resource.NewQuantity(2000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(9000, resource.BinarySI),
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			g := &gpuDeviceManager{
				deviceCount:      tt.fields.deviceCount,
				devices:          tt.fields.devices,
				processesMetrics: tt.fields.processesMetrics,
			}
			if got := g.getNodeGPUUsage(); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("gpuUsageDetailRecord.GetNodeGPUUsage() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_gpuUsageDetailRecord_GetPIDsTotalGPUUsage(t *testing.T) {
	type fields struct {
		deviceCount      int
		devices          []*device
		processesMetrics map[uint32][]*rawGPUMetric
	}
	type args struct {
		pids []uint32
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   []metriccache.GPUMetric
	}{
		{
			name: "single device",
			args: args{
				pids: []uint32{122},
			},
			fields: fields{
				deviceCount: 1,
				devices: []*device{
					{Minor: 0, DeviceUUID: "test-device1", MemoryTotal: 14000},
				},
				processesMetrics: map[uint32][]*rawGPUMetric{
					122: {{SMUtil: 70, MemoryUsed: 1500}},
					123: {{SMUtil: 20, MemoryUsed: 1000}},
				},
			},
			want: []metriccache.GPUMetric{
				{
					DeviceUUID:  "test-device1",
					Minor:       0,
					SMUtil:      70,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
			},
		},
		{
			name: "multiple device",
			args: args{
				pids: []uint32{122, 222},
			},
			fields: fields{
				deviceCount: 2,
				devices: []*device{
					{Minor: 0, DeviceUUID: "test-device1", MemoryTotal: 14000},
					{Minor: 1, DeviceUUID: "test-device2", MemoryTotal: 24000},
				},
				processesMetrics: map[uint32][]*rawGPUMetric{
					122: {{SMUtil: 70, MemoryUsed: 1500}, nil},
					222: {nil, {SMUtil: 50, MemoryUsed: 1000}},
				},
			},
			want: []metriccache.GPUMetric{
				{
					DeviceUUID:  "test-device1",
					Minor:       0,
					SMUtil:      70,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
				{
					DeviceUUID:  "test-device2",
					Minor:       1,
					SMUtil:      50,
					MemoryUsed:  *resource.NewQuantity(1000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(24000, resource.BinarySI),
				},
			},
		},
		{
			name: "multiple device-1",
			args: args{
				pids: []uint32{122},
			},
			fields: fields{
				deviceCount: 2,
				devices: []*device{
					{Minor: 0, DeviceUUID: "test-device1", MemoryTotal: 14000},
					{Minor: 1, DeviceUUID: "test-device2", MemoryTotal: 24000},
				},
				processesMetrics: map[uint32][]*rawGPUMetric{
					122: {{SMUtil: 70, MemoryUsed: 1500}, nil},
					222: {nil, {SMUtil: 50, MemoryUsed: 1000}},
				},
			},
			want: []metriccache.GPUMetric{
				{
					DeviceUUID:  "test-device1",
					Minor:       0,
					SMUtil:      70,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
			},
		},
		{
			name: "multiple device and multiple processes",
			args: args{
				pids: []uint32{122, 222},
			},
			fields: fields{
				deviceCount: 2,
				devices: []*device{
					{Minor: 0, DeviceUUID: "test-device1", MemoryTotal: 14000},
					{Minor: 1, DeviceUUID: "test-device2", MemoryTotal: 24000},
				},
				processesMetrics: map[uint32][]*rawGPUMetric{
					122: {{SMUtil: 70, MemoryUsed: 1500}, {SMUtil: 50, MemoryUsed: 1000}},
					222: {{SMUtil: 10, MemoryUsed: 1000}, {SMUtil: 40, MemoryUsed: 3000}},
				},
			},
			want: []metriccache.GPUMetric{
				{
					DeviceUUID:  "test-device1",
					Minor:       0,
					SMUtil:      80,
					MemoryUsed:  *resource.NewQuantity(2500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
				{
					DeviceUUID:  "test-device2",
					Minor:       1,
					SMUtil:      90,
					MemoryUsed:  *resource.NewQuantity(4000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(24000, resource.BinarySI),
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			g := &gpuDeviceManager{
				deviceCount:      tt.fields.deviceCount,
				devices:          tt.fields.devices,
				processesMetrics: tt.fields.processesMetrics,
			}
			if got := g.getTotalGPUUsageOfPIDs(tt.args.pids); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("gpuUsageDetailRecord.GetPIDsTotalGPUUsage() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_gpuDeviceManager_getPodGPUUsage(t *testing.T) {

	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	dir := t.TempDir()
	system.Conf.CgroupRootDir = dir

	p1 := "/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cgroup.procs"
	p1CgroupPath := path.Join(dir, p1)
	if err := writeCgroupContent(p1CgroupPath, []byte("122\n222")); err != nil {
		t.Fatal(err)
	}

	p2 := "/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4acff.scope/cgroup.procs"
	p2CgroupPath := path.Join(dir, p2)
	if err := writeCgroupContent(p2CgroupPath, []byte("45\n67")); err != nil {
		t.Fatal(err)
	}

	type fields struct {
		gpuDeviceManager GPUDeviceManager
	}
	type args struct {
		podParentDir string
		cs           []corev1.ContainerStatus
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    []metriccache.GPUMetric
		wantErr bool
	}{
		{
			name: "multiple processes and multiple device",
			fields: fields{
				gpuDeviceManager: &gpuDeviceManager{
					deviceCount: 2,
					devices: []*device{
						{Minor: 0, DeviceUUID: "12", MemoryTotal: 14000},
						{Minor: 1, DeviceUUID: "23", MemoryTotal: 24000},
					},
					processesMetrics: map[uint32][]*rawGPUMetric{
						122: {{SMUtil: 70, MemoryUsed: 1500}, {SMUtil: 50, MemoryUsed: 1000}},
						222: {{SMUtil: 10, MemoryUsed: 1000}, {SMUtil: 40, MemoryUsed: 3000}},
					},
				},
			},
			args: args{
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				cs: []corev1.ContainerStatus{
					{
						ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
						State: corev1.ContainerState{
							Running: &corev1.ContainerStateRunning{
								StartedAt: v1.NewTime(time.Now()),
							},
						},
					},
					{
						ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4acff",
						State: corev1.ContainerState{
							Running: &corev1.ContainerStateRunning{
								StartedAt: v1.NewTime(time.Now()),
							},
						},
					},
				},
			},
			want: []metriccache.GPUMetric{
				{
					Minor:       0,
					DeviceUUID:  "12",
					SMUtil:      80,
					MemoryUsed:  *resource.NewQuantity(2500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
				{
					Minor:       1,
					DeviceUUID:  "23",
					SMUtil:      90,
					MemoryUsed:  *resource.NewQuantity(4000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(24000, resource.BinarySI),
				},
			},
			wantErr: false,
		},
		{
			name: "multiple processes",
			fields: fields{
				gpuDeviceManager: &gpuDeviceManager{
					deviceCount: 2,
					devices: []*device{
						{Minor: 0, DeviceUUID: "12", MemoryTotal: 14000},
						{Minor: 1, DeviceUUID: "23", MemoryTotal: 24000},
					},
					processesMetrics: map[uint32][]*rawGPUMetric{
						122: {{SMUtil: 70, MemoryUsed: 1500}, nil},
						222: {nil, {SMUtil: 40, MemoryUsed: 3000}},
					},
				},
			},
			args: args{
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				cs: []corev1.ContainerStatus{
					{
						ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
						State: corev1.ContainerState{
							Running: &corev1.ContainerStateRunning{
								StartedAt: v1.NewTime(time.Now()),
							},
						},
					},
					{
						ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4acff",
						State: corev1.ContainerState{
							Running: &corev1.ContainerStateRunning{
								StartedAt: v1.NewTime(time.Now()),
							},
						},
					},
				},
			},
			want: []metriccache.GPUMetric{
				{
					Minor:       0,
					DeviceUUID:  "12",
					SMUtil:      70,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
				{
					Minor:       1,
					DeviceUUID:  "23",
					SMUtil:      40,
					MemoryUsed:  *resource.NewQuantity(3000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(24000, resource.BinarySI),
				},
			},
			wantErr: false,
		},

		{
			name: "single processes",
			fields: fields{
				gpuDeviceManager: &gpuDeviceManager{
					deviceCount: 2,
					devices: []*device{
						{Minor: 0, DeviceUUID: "12", MemoryTotal: 14000},
						{Minor: 1, DeviceUUID: "23", MemoryTotal: 24000},
					},
					processesMetrics: map[uint32][]*rawGPUMetric{
						122: {nil, {SMUtil: 70, MemoryUsed: 1500}},
						222: {nil, {SMUtil: 20, MemoryUsed: 3000}},
					},
				},
			},
			args: args{
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				cs: []corev1.ContainerStatus{
					{
						ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
						State: corev1.ContainerState{
							Running: &corev1.ContainerStateRunning{
								StartedAt: v1.NewTime(time.Now()),
							},
						},
					},
					{
						ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4acff",
						State: corev1.ContainerState{
							Running: &corev1.ContainerStateRunning{
								StartedAt: v1.NewTime(time.Now()),
							},
						},
					},
				},
			},
			want: []metriccache.GPUMetric{
				{
					Minor:       1,
					DeviceUUID:  "23",
					SMUtil:      90,
					MemoryUsed:  *resource.NewQuantity(4500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(24000, resource.BinarySI),
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			g := tt.fields.gpuDeviceManager
			got, err := g.getPodGPUUsage(tt.args.podParentDir, tt.args.cs)
			if (err != nil) != tt.wantErr {
				t.Errorf("gpuDeviceManager.getPodGPUUsage() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("gpuDeviceManager.getPodGPUUsage() = %v, want %v", got, tt.want)
			}
		})
	}
}
func Test_gpuDeviceManager_getContainerGPUUsage(t *testing.T) {

	system.SetupCgroupPathFormatter(system.Systemd)
	defer system.SetupCgroupPathFormatter(system.Systemd)
	dir := t.TempDir()
	system.Conf.CgroupRootDir = dir

	p1 := "/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf.scope/cgroup.procs"
	p1CgroupPath := path.Join(dir, p1)
	if err := writeCgroupContent(p1CgroupPath, []byte("122\n222")); err != nil {
		t.Fatal(err)
	}

	p2 := "/cpu/kubepods.slice/kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/docker-703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4acff.scope/cgroup.procs"
	p2CgroupPath := path.Join(dir, p2)
	if err := writeCgroupContent(p2CgroupPath, []byte("122")); err != nil {
		t.Fatal(err)
	}

	type fields struct {
		gpuDeviceManager GPUDeviceManager
	}
	type args struct {
		podParentDir string
		c            *corev1.ContainerStatus
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    []metriccache.GPUMetric
		wantErr bool
	}{
		{
			name: "multiple processes and multiple device",
			fields: fields{
				gpuDeviceManager: &gpuDeviceManager{
					deviceCount: 2,
					devices: []*device{
						{Minor: 0, DeviceUUID: "12", MemoryTotal: 14000},
						{Minor: 1, DeviceUUID: "23", MemoryTotal: 24000},
					},
					processesMetrics: map[uint32][]*rawGPUMetric{
						122: {{SMUtil: 70, MemoryUsed: 1500}, {SMUtil: 50, MemoryUsed: 1000}},
						222: {{SMUtil: 10, MemoryUsed: 1000}, {SMUtil: 40, MemoryUsed: 3000}},
					},
				},
			},
			args: args{
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				c: &corev1.ContainerStatus{
					ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4abaf",
					State: corev1.ContainerState{
						Running: &corev1.ContainerStateRunning{
							StartedAt: v1.NewTime(time.Now()),
						},
					},
				},
			},
			want: []metriccache.GPUMetric{
				{
					Minor: 0, DeviceUUID: "12",
					SMUtil:      80,
					MemoryUsed:  *resource.NewQuantity(2500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
				{
					Minor:       1,
					DeviceUUID:  "23",
					SMUtil:      90,
					MemoryUsed:  *resource.NewQuantity(4000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(24000, resource.BinarySI),
				},
			},
			wantErr: false,
		},
		{
			name: "single processes and multiple device",
			fields: fields{
				gpuDeviceManager: &gpuDeviceManager{
					deviceCount: 2,
					devices: []*device{
						{Minor: 0, DeviceUUID: "12", MemoryTotal: 14000},
						{Minor: 1, DeviceUUID: "23", MemoryTotal: 24000},
					},
					processesMetrics: map[uint32][]*rawGPUMetric{
						122: {{SMUtil: 70, MemoryUsed: 1500}, {SMUtil: 50, MemoryUsed: 1000}},
						222: {{SMUtil: 10, MemoryUsed: 1000}, {SMUtil: 40, MemoryUsed: 3000}},
					},
				},
			},
			args: args{
				podParentDir: "kubepods-besteffort.slice/kubepods-besteffort-pod6553a60b_2b97_442a_b6da_a5704d81dd98.slice/",
				c: &corev1.ContainerStatus{
					ContainerID: "docker://703b1b4e811f56673d68f9531204e5dd4963e734e2929a7056fd5f33fde4acff",
					State: corev1.ContainerState{
						Running: &corev1.ContainerStateRunning{
							StartedAt: v1.NewTime(time.Now()),
						},
					},
				},
			},
			want: []metriccache.GPUMetric{
				{
					Minor:       0,
					DeviceUUID:  "12",
					SMUtil:      70,
					MemoryUsed:  *resource.NewQuantity(1500, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(14000, resource.BinarySI),
				},
				{
					Minor:       1,
					DeviceUUID:  "23",
					SMUtil:      50,
					MemoryUsed:  *resource.NewQuantity(1000, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(24000, resource.BinarySI),
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			g := tt.fields.gpuDeviceManager
			got, err := g.getContainerGPUUsage(tt.args.podParentDir, tt.args.c)
			if (err != nil) != tt.wantErr {
				t.Errorf("gpuDeviceManager.getContainerGPUUsage() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("gpuDeviceManager.getContainerGPUUsage() = %v, want %v", got, tt.want)
			}
		})
	}
}

func writeCgroupContent(filePath string, content []byte) error {
	err := os.MkdirAll(path.Dir(filePath), os.ModePerm)
	if err != nil {
		return err
	}
	return os.WriteFile(filePath, content, 0655)
}
