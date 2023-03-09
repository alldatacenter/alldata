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
	"k8s.io/client-go/tools/cache"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func Test_nodeDeviceCache_onPodAdd(t *testing.T) {
	podNamespacedName := types.NamespacedName{
		Namespace: "default",
		Name:      "test",
	}
	tests := []struct {
		name        string
		pod         interface{}
		deviceCache *nodeDeviceCache
		wantCache   map[string]*nodeDevice
	}{
		{
			name:      "object is not pod",
			pod:       &corev1.Node{},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name: "pod node not exist in cache",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name: "pod does not have device resource",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": newNodeDevice(),
				},
			},
			wantCache: map[string]*nodeDevice{
				"test-node": newNodeDevice(),
			},
		},
		{
			name: "incorrect device allocation annotation",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
					Annotations: map[string]string{
						apiext.AnnotationDeviceAllocated: "incorrect-device-allocation-annotation",
					},
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": newNodeDevice(),
				},
			},
			wantCache: map[string]*nodeDevice{
				"test-node": newNodeDevice(),
			},
		},
		{
			name: "correct device allocation annotation",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
					Annotations: map[string]string{
						apiext.AnnotationDeviceAllocated: `{"gpu":[{"minor":1,"resources":{"kubernetes.io/gpu-core":"60","kubernetes.io/gpu-memory":"8Gi","kubernetes.io/gpu-memory-ratio":"50"}}]}`,
					},
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("60"),
									apiext.GPUMemoryRatio: resource.MustParse("50"),
									apiext.GPUMemory:      resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {},
						},
						allocateSet: make(map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList),
					},
				},
			},
			wantCache: map[string]*nodeDevice{
				"test-node": {
					deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        *resource.NewQuantity(40, resource.DecimalSI),
								apiext.GPUMemoryRatio: *resource.NewQuantity(50, resource.DecimalSI),
								apiext.GPUMemory:      resource.MustParse("8Gi"),
							},
						},
					},
					deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("60"),
								apiext.GPUMemoryRatio: resource.MustParse("50"),
								apiext.GPUMemory:      resource.MustParse("8Gi"),
							},
						},
					},
					allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{
						schedulingv1alpha1.GPU: {
							podNamespacedName: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("60"),
									apiext.GPUMemoryRatio: resource.MustParse("50"),
									apiext.GPUMemory:      resource.MustParse("8Gi"),
								},
							},
						},
					},
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
			deviceCache.onPodAdd(tt.pod)
			assert.Equal(t, tt.wantCache, deviceCache.nodeDeviceInfos)
		})
	}
}

func Test_nodeDeviceCache_onPodUpdate(t *testing.T) {
	tests := []struct {
		name        string
		pod         interface{}
		deviceCache *nodeDeviceCache
		wantCache   map[string]*nodeDevice
	}{
		{
			name:      "simply return",
			pod:       &corev1.Pod{},
			wantCache: map[string]*nodeDevice{},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			deviceCache := newNodeDeviceCache()
			deviceCache.onPodUpdate(nil, tt.pod)
			assert.Equal(t, tt.wantCache, deviceCache.nodeDeviceInfos)
		})
	}
}

func Test_nodeDeviceCache_onPodDelete(t *testing.T) {
	podNamespacedName := types.NamespacedName{
		Namespace: "default",
		Name:      "test",
	}
	tests := []struct {
		name        string
		pod         interface{}
		deviceCache *nodeDeviceCache
		wantCache   map[string]*nodeDevice
	}{
		{
			name:      "object is not pod",
			pod:       &corev1.Node{},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name: "delete DeletedFinalStateUnknown",
			pod: cache.DeletedFinalStateUnknown{
				Obj: &corev1.Node{},
			},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name: "pod node not exist in cache",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodFailed,
				},
			},
			wantCache: map[string]*nodeDevice{},
		},
		{
			name: "pod does not have device resource",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodFailed,
				},
			},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": newNodeDevice(),
				},
			},
			wantCache: map[string]*nodeDevice{
				"test-node": newNodeDevice(),
			},
		},
		{
			name: "incorrect device allocation annotation",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
					Annotations: map[string]string{
						apiext.AnnotationDeviceAllocated: "incorrect-device-allocation-annotation",
					},
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
					},
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": newNodeDevice(),
				},
			},
			wantCache: map[string]*nodeDevice{
				"test-node": newNodeDevice(),
			},
		},
		{
			name: "correct device allocation annotation",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
					Annotations: map[string]string{
						apiext.AnnotationDeviceAllocated: `{"gpu":[{"minor":1,"resources":{"kubernetes.io/gpu-core":"60","kubernetes.io/gpu-memory":"8Gi","kubernetes.io/gpu-memory-ratio":"50"}}]}`,
					},
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("60"),
									apiext.GPUMemoryRatio: resource.MustParse("50"),
									apiext.GPUMemory:      resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodFailed,
				},
			},
			deviceCache: &nodeDeviceCache{
				nodeDeviceInfos: map[string]*nodeDevice{
					"test-node": {
						deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("40"),
									apiext.GPUMemoryRatio: resource.MustParse("50"),
									apiext.GPUMemory:      resource.MustParse("8Gi"),
								},
							},
						},
						deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("100"),
									apiext.GPUMemoryRatio: resource.MustParse("100"),
									apiext.GPUMemory:      resource.MustParse("16Gi"),
								},
							},
						},
						deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
							schedulingv1alpha1.GPU: {
								1: corev1.ResourceList{
									apiext.GPUCore:        resource.MustParse("60"),
									apiext.GPUMemoryRatio: resource.MustParse("50"),
									apiext.GPUMemory:      resource.MustParse("8Gi"),
								},
							},
						},
						allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{
							schedulingv1alpha1.GPU: {
								podNamespacedName: {},
							},
						},
					},
				},
			},
			wantCache: map[string]*nodeDevice{
				"test-node": {
					deviceFree: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
								apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("100"),
								apiext.GPUMemoryRatio: resource.MustParse("100"),
								apiext.GPUMemory:      resource.MustParse("16Gi"),
							},
						},
					},
					deviceUsed: map[schedulingv1alpha1.DeviceType]deviceResources{
						schedulingv1alpha1.GPU: {
							1: corev1.ResourceList{
								apiext.GPUCore:        resource.MustParse("0"),
								apiext.GPUMemoryRatio: resource.MustParse("0"),
								apiext.GPUMemory:      resource.MustParse("0"),
							},
						},
					},
					allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{
						schedulingv1alpha1.GPU: {},
					},
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
			deviceCache.onPodDelete(tt.pod)
			assert.Equal(t, tt.wantCache, deviceCache.nodeDeviceInfos)
		})
	}
}
