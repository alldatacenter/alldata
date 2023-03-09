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

package deviceshare

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/uuid"
	"k8s.io/client-go/tools/cache"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func Test_nodeDeviceCache_onDeviceAdd(t *testing.T) {
	tests := []struct {
		name        string
		device      interface{}
		deviceCache *nodeDeviceCache
		wantCache   map[string]*nodeDevice
	}{
		{
			name:      "invalid object",
			device:    &corev1.Pod{},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name:      "nil device",
			device:    &schedulingv1alpha1.Device{},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name:      "normal case 1",
			device:    generateFakeDevice(),
			wantCache: generateFakeNodeDeviceInfos(),
		},
		{
			name:   "normal case 2",
			device: generateFakeDevice(),
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node-1": {
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								0: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
						allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
					},
				},
			},
			wantCache: generateFakeNodeDeviceInfos(),
		},
		{
			name:   "normal case 3",
			device: generateMultipleFakeDevice(),
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node-1": {
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("8Gi"),
								},
							},
						},
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("8Gi"),
								},
							},
						},
						deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
						allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
					},
				},
			},
			wantCache: map[string]*nodeDevice{
				"test-node-1": {
					deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							0: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							0: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
					allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			deviceCache := tt.deviceCache
			if deviceCache == nil {
				deviceCache = newNodeDeviceCache()
			}
			deviceCache.onDeviceAdd(tt.device)
			assert.Equal(t, tt.wantCache, deviceCache.nodeDeviceInfos)
		})
	}
}

func Test_nodeDeviceCache_onDeviceUpdate(t *testing.T) {
	tests := []struct {
		name        string
		oldDevice   interface{}
		newDevice   interface{}
		deviceCache *nodeDeviceCache
		wantCache   map[string]*nodeDevice
	}{
		{
			name:      "invalid object",
			oldDevice: &corev1.Pod{},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name:      "normal case 1",
			oldDevice: generateFakeDevice(),
			newDevice: &schedulingv1alpha1.Device{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: schedulingv1alpha1.DeviceSpec{
					Devices: []schedulingv1alpha1.DeviceInfo{
						{
							UUID:   string(uuid.NewUUID()),
							Minor:  pointer.Int32Ptr(1),
							Health: true,
							Type:   schedulingv1alpha1.GPU,
							Resources: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("32Gi"),
							},
						},
					},
				},
			},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: generateFakeNodeDeviceInfos(),
			},
			wantCache: map[string]*nodeDevice{
				"test-node-1": {
					deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("32Gi"),
							},
						},
					},
					deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("32Gi"),
							},
						},
					},
					deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
					allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
				},
			},
		},
		{
			name:      "normal case 2",
			oldDevice: generateFakeDevice(),
			newDevice: &schedulingv1alpha1.Device{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: schedulingv1alpha1.DeviceSpec{
					Devices: []schedulingv1alpha1.DeviceInfo{},
				},
			},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: generateFakeNodeDeviceInfos(),
			},
			wantCache: map[string]*nodeDevice{
				"test-node-1": {
					deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {},
					},
					deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {},
					},
					deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
					allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
				},
			},
		},
		{
			name: "normal case 3",
			oldDevice: &schedulingv1alpha1.Device{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node-1",
				},
				Spec: schedulingv1alpha1.DeviceSpec{
					Devices: []schedulingv1alpha1.DeviceInfo{
						{
							UUID:  string(uuid.NewUUID()),
							Minor: pointer.Int32Ptr(1),
							Type:  schedulingv1alpha1.GPU,
							Resources: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
						{
							UUID:  string(uuid.NewUUID()),
							Minor: pointer.Int32Ptr(1),
							Type:  schedulingv1alpha1.FPGA,
							Resources: corev1.ResourceList{
								apiext.KoordFPGA: resource.MustParse("100"),
							},
						},
					},
				},
			},
			newDevice: generateFakeDevice(),
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node-1": {
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								1: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
							schedulingv1alpha1.FPGA: {
								1: corev1.ResourceList{
									apiext.KoordFPGA: resource.MustParse("100"),
								},
							},
						},
						deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
						allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
					},
				},
			},
			wantCache: map[string]*nodeDevice{
				"test-node-1": {
					deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
						schedulingv1alpha1.FPGA: {},
					},
					deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
						schedulingv1alpha1.FPGA: {},
					},
					deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
					allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			deviceCache := tt.deviceCache
			if deviceCache == nil {
				deviceCache = newNodeDeviceCache()
			}
			deviceCache.onDeviceUpdate(tt.oldDevice, tt.newDevice)
			assert.Equal(t, tt.wantCache, deviceCache.nodeDeviceInfos)
		})
	}
}

func Test_nodeDeviceCache_onDeviceDelete(t *testing.T) {
	tests := []struct {
		name        string
		device      interface{}
		deviceCache *nodeDeviceCache
		wantCache   map[string]*nodeDevice
	}{
		{
			name:      "invalid object",
			device:    &corev1.Pod{},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name: "delete DeletedFinalStateUnknown",
			device: cache.DeletedFinalStateUnknown{
				Obj: &corev1.Node{},
			},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name:   "nil device",
			device: &schedulingv1alpha1.Device{},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: generateFakeNodeDeviceInfos(),
			},
			wantCache: generateFakeNodeDeviceInfos(),
		},
		{
			name:   "normal case 1",
			device: generateFakeDevice(),
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: generateFakeNodeDeviceInfos(),
			},
			wantCache: map[string]*nodeDevice{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			deviceCache := tt.deviceCache
			if deviceCache == nil {
				deviceCache = newNodeDeviceCache()
			}
			deviceCache.onDeviceDelete(tt.device)
			assert.Equal(t, tt.wantCache, deviceCache.nodeDeviceInfos)
		})
	}
}

func generateFakeDevice() *schedulingv1alpha1.Device {
	return &schedulingv1alpha1.Device{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node-1",
		},
		Spec: schedulingv1alpha1.DeviceSpec{
			Devices: []schedulingv1alpha1.DeviceInfo{
				{
					UUID:   string(uuid.NewUUID()),
					Minor:  pointer.Int32Ptr(1),
					Health: true,
					Type:   schedulingv1alpha1.GPU,
					Resources: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("100"),
						apiext.GPUMemoryRatio: resource.MustParse("100"),
						apiext.GPUMemory:      resource.MustParse("16Gi"),
					},
				},
			},
		},
	}
}

func generateMultipleFakeDevice() *schedulingv1alpha1.Device {
	return &schedulingv1alpha1.Device{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node-1",
		},
		Spec: schedulingv1alpha1.DeviceSpec{
			Devices: []schedulingv1alpha1.DeviceInfo{
				{
					UUID:   string(uuid.NewUUID()),
					Minor:  pointer.Int32Ptr(0),
					Health: true,
					Type:   schedulingv1alpha1.GPU,
					Resources: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("100"),
						apiext.GPUMemoryRatio: resource.MustParse("100"),
						apiext.GPUMemory:      resource.MustParse("16Gi"),
					},
				},
				{
					UUID:   string(uuid.NewUUID()),
					Minor:  pointer.Int32Ptr(1),
					Health: true,
					Type:   schedulingv1alpha1.GPU,
					Resources: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("100"),
						apiext.GPUMemoryRatio: resource.MustParse("100"),
						apiext.GPUMemory:      resource.MustParse("16Gi"),
					},
				},
			},
		},
	}
}

func generateFakeNodeDeviceInfos() map[string]*nodeDevice {
	return map[string]*nodeDevice{
		"test-node-1": {
			deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
				schedulingv1alpha1.GPU: {
					1: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("100"),
						apiext.GPUMemoryRatio: resource.MustParse("100"),
						apiext.GPUMemory:      resource.MustParse("16Gi"),
					},
				},
			},
			deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
				schedulingv1alpha1.GPU: {
					1: corev1.ResourceList{
						apiext.GPUCore:        resource.MustParse("100"),
						apiext.GPUMemoryRatio: resource.MustParse("100"),
						apiext.GPUMemory:      resource.MustParse("16Gi"),
					},
				},
			},
			deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
			allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
		},
	}
}
