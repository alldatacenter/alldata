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
	"k8s.io/apimachinery/pkg/api/equality"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func Test_newNodeDeviceCache(t *testing.T) {
	expectNodeDeviceCache := &nodeDeviceCache{
		nodeDeviceInfos: map[string]*nodeDevice{},
	}
	assert.Equal(t, expectNodeDeviceCache, newNodeDeviceCache())
}

func Test_newNodeDevice(t *testing.T) {
	expectNodeDevice := &nodeDevice{
		deviceTotal: map[schedulingv1alpha1.DeviceType]deviceResources{},
		deviceFree:  map[schedulingv1alpha1.DeviceType]deviceResources{},
		deviceUsed:  map[schedulingv1alpha1.DeviceType]deviceResources{},
		allocateSet: map[schedulingv1alpha1.DeviceType]map[types.NamespacedName]map[int]corev1.ResourceList{},
	}
	assert.Equal(t, expectNodeDevice, newNodeDevice())
}

func Test_nodeDevice_getUsed(t *testing.T) {
	allocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
		},
	}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
		},
	}
	nd := newNodeDevice()
	nd.updateCacheUsed(allocations, pod, true)
	used := nd.getUsed(pod)
	expectUsed := map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			1: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
		},
	}
	assert.Equal(t, expectUsed, used)
}

func Test_nodeDevice_replaceWith(t *testing.T) {
	nd := newNodeDevice()
	nd.resetDeviceTotal(map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			1: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
		},
		schedulingv1alpha1.RDMA: {
			1: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
		},
	})

	allocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 2,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("50"),
					apiext.ResourceGPUMemory:      resource.MustParse("5Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
				},
			},
		},
	}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
		},
	}
	nd.updateCacheUsed(allocations, pod, true)

	nnd := nd.replaceWith(map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("50"),
				apiext.ResourceGPUMemory:      resource.MustParse("3Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
			},
		},
	})
	expectTotal := map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
		},
		schedulingv1alpha1.RDMA: {
			1: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
		},
	}
	assert.Equal(t, expectTotal, nnd.deviceTotal)

	expectUsed := map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("50"),
				apiext.ResourceGPUMemory:      resource.MustParse("5Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
			},
		},
	}
	assert.True(t, equality.Semantic.DeepEqual(expectUsed, nnd.deviceUsed))

	expectFree := map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("50"),
				apiext.ResourceGPUMemory:      resource.MustParse("3Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
			},
		},
		schedulingv1alpha1.RDMA: {
			1: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
		},
	}
	assert.True(t, equality.Semantic.DeepEqual(expectFree, nnd.deviceFree))
}

func Test_nodeDevice_allocateGPU(t *testing.T) {
	nd := newNodeDevice()
	nd.resetDeviceTotal(map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			1: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
		},
		schedulingv1alpha1.RDMA: {
			1: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
		},
	})

	allocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
			{
				Minor: 2,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("100"),
					apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
				},
			},
		},
	}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
		},
	}
	nd.updateCacheUsed(allocations, pod, true)

	podRequests := corev1.ResourceList{
		apiext.ResourceGPUCore:        resource.MustParse("50"),
		apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
	}
	preemptible := map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			1: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
		},
	}
	allocateResult, err := nd.tryAllocateDevice(podRequests, preemptible)
	assert.NoError(t, err)
	expectAllocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("50"),
					apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
				},
			},
		},
	}
	assert.True(t, equality.Semantic.DeepEqual(expectAllocations, allocateResult))

	podRequests = corev1.ResourceList{
		apiext.ResourceGPUCore:        resource.MustParse("200"),
		apiext.ResourceGPUMemoryRatio: resource.MustParse("200"),
	}
	allocateResult, err = nd.tryAllocateDevice(podRequests, preemptible)
	assert.NoError(t, err)
	expectAllocations = allocations
	assert.True(t, equality.Semantic.DeepEqual(expectAllocations, allocateResult))
}

func Test_nodeDevice_allocateGPUWithUnhealthyInstance(t *testing.T) {
	nd := newNodeDevice()
	nd.resetDeviceTotal(map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			1: corev1.ResourceList{}, // mock unhealthy state
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
		},
	})

	podRequests := corev1.ResourceList{
		apiext.ResourceGPUCore:        resource.MustParse("50"),
		apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
	}
	allocateResult, err := nd.tryAllocateDevice(podRequests, nil)
	assert.NoError(t, err)
	expectAllocations := apiext.DeviceAllocations{
		schedulingv1alpha1.GPU: {
			{
				Minor: 2,
				Resources: corev1.ResourceList{
					apiext.ResourceGPUCore:        resource.MustParse("50"),
					apiext.ResourceGPUMemory:      resource.MustParse("4Gi"),
					apiext.ResourceGPUMemoryRatio: resource.MustParse("50"),
				},
			},
		},
	}
	assert.True(t, equality.Semantic.DeepEqual(expectAllocations, allocateResult))
}

func Test_nodeDevice_allocateRDMA(t *testing.T) {
	nd := newNodeDevice()
	nd.resetDeviceTotal(map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: {
			1: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceGPUCore:        resource.MustParse("100"),
				apiext.ResourceGPUMemory:      resource.MustParse("8Gi"),
				apiext.ResourceGPUMemoryRatio: resource.MustParse("100"),
			},
		},
		schedulingv1alpha1.RDMA: {
			1: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
		},
	})

	allocations := apiext.DeviceAllocations{
		schedulingv1alpha1.RDMA: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceRDMA: resource.MustParse("100"),
				},
			},
			{
				Minor: 2,
				Resources: corev1.ResourceList{
					apiext.ResourceRDMA: resource.MustParse("100"),
				},
			},
		},
	}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
		},
	}
	nd.updateCacheUsed(allocations, pod, true)

	podRequests := corev1.ResourceList{
		apiext.ResourceRDMA: resource.MustParse("50"),
	}
	allocateResult := apiext.DeviceAllocations{}
	preemptible := map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.RDMA: {
			1: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
			2: corev1.ResourceList{
				apiext.ResourceRDMA: resource.MustParse("100"),
			},
		},
	}
	err := nd.tryAllocateDeviceByType(podRequests, schedulingv1alpha1.RDMA, allocateResult, preemptible)
	assert.NoError(t, err)
	expectAllocations := apiext.DeviceAllocations{
		schedulingv1alpha1.RDMA: {
			{
				Minor: 1,
				Resources: corev1.ResourceList{
					apiext.ResourceRDMA: resource.MustParse("50"),
				},
			},
		},
	}
	assert.True(t, equality.Semantic.DeepEqual(expectAllocations, allocateResult))

	podRequests = corev1.ResourceList{
		apiext.ResourceRDMA: resource.MustParse("200"),
	}
	allocateResult = apiext.DeviceAllocations{}
	err = nd.tryAllocateDeviceByType(podRequests, schedulingv1alpha1.RDMA, allocateResult, preemptible)
	assert.NoError(t, err)
	expectAllocations = allocations
	assert.True(t, equality.Semantic.DeepEqual(expectAllocations, allocateResult))
}
