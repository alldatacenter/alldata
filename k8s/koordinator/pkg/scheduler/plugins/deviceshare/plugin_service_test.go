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
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/gin-gonic/gin"
	nrtinformers "github.com/k8stopologyawareschedwg/noderesourcetopology-api/pkg/generated/informers/externalversions"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	apiequality "k8s.io/apimachinery/pkg/api/equality"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/informers"
	kubefake "k8s.io/client-go/kubernetes/fake"
	schedulerconfig "k8s.io/kubernetes/pkg/scheduler/apis/config"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/defaultbinder"
	"k8s.io/kubernetes/pkg/scheduler/framework/plugins/queuesort"
	"k8s.io/kubernetes/pkg/scheduler/framework/runtime"
	schedulertesting "k8s.io/kubernetes/pkg/scheduler/testing"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	koordfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	koordinatorinformers "github.com/koordinator-sh/koordinator/pkg/client/informers/externalversions"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
)

type pluginTestSuit struct {
	framework.Handle
	framework.Framework
	koordinatorSharedInformerFactory koordinatorinformers.SharedInformerFactory
	nrtSharedInformerFactory         nrtinformers.SharedInformerFactory
	proxyNew                         runtime.PluginFactory
	plugin                           framework.Plugin
}

func newPluginTestSuit(t *testing.T, nodes []*corev1.Node) *pluginTestSuit {
	koordClientSet := koordfake.NewSimpleClientset()
	koordSharedInformerFactory := koordinatorinformers.NewSharedInformerFactory(koordClientSet, 0)
	extendHandle := frameworkext.NewExtendedHandle(
		frameworkext.WithKoordinatorClientSet(koordClientSet),
		frameworkext.WithKoordinatorSharedInformerFactory(koordSharedInformerFactory),
	)
	proxyNew := frameworkext.PluginFactoryProxy(extendHandle, New)

	deviceSharePluginConfig := schedulerconfig.PluginConfig{
		Name: Name,
		Args: nil,
	}

	registeredPlugins := []schedulertesting.RegisterPluginFunc{
		func(reg *runtime.Registry, profile *schedulerconfig.KubeSchedulerProfile) {
			profile.PluginConfig = []schedulerconfig.PluginConfig{
				deviceSharePluginConfig,
			}
		},
		schedulertesting.RegisterBindPlugin(defaultbinder.Name, defaultbinder.New),
		schedulertesting.RegisterQueueSortPlugin(queuesort.Name, queuesort.New),
		schedulertesting.RegisterPreFilterPlugin(Name, proxyNew),
	}

	cs := kubefake.NewSimpleClientset()
	informerFactory := informers.NewSharedInformerFactory(cs, 0)
	snapshot := newTestSharedLister(nil, nodes)

	fh, err := schedulertesting.NewFramework(
		registeredPlugins,
		"koord-scheduler",
		runtime.WithClientSet(cs),
		runtime.WithInformerFactory(informerFactory),
		runtime.WithSnapshotSharedLister(snapshot),
	)
	assert.Nil(t, err)
	return &pluginTestSuit{
		Handle:                           fh,
		koordinatorSharedInformerFactory: koordSharedInformerFactory,
		proxyNew:                         proxyNew,
	}
}

func TestEndpointsQueryNodeDeviceSummary(t *testing.T) {
	suit := newPluginTestSuit(t, nil)
	p, err := suit.proxyNew(nil, suit.Handle)
	assert.NotNil(t, p)
	assert.Nil(t, err)

	ds := p.(*Plugin)

	device := &schedulingv1alpha1.Device{
		ObjectMeta: metav1.ObjectMeta{
			Name: "node1",
		},
	}
	deviceInfo0 := schedulingv1alpha1.DeviceInfo{}
	deviceInfo0.Minor = pointer.Int32Ptr(0)
	deviceInfo0.Health = true
	deviceInfo0.Type = schedulingv1alpha1.GPU
	deviceInfo0.Resources = corev1.ResourceList{
		apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
		apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
	}
	deviceInfo1 := schedulingv1alpha1.DeviceInfo{}
	deviceInfo1.Minor = pointer.Int32Ptr(1)
	deviceInfo1.Health = true
	deviceInfo1.Type = schedulingv1alpha1.GPU
	deviceInfo1.Resources = corev1.ResourceList{
		apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
		apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
	}
	deviceInfo2 := schedulingv1alpha1.DeviceInfo{}
	deviceInfo2.Minor = pointer.Int32Ptr(2)
	deviceInfo2.Health = true
	deviceInfo2.Type = schedulingv1alpha1.FPGA
	deviceInfo2.Resources = corev1.ResourceList{
		apiext.KoordFPGA: *resource.NewQuantity(100, resource.DecimalSI),
	}
	device.Spec.Devices = append(device.Spec.Devices, deviceInfo0)
	device.Spec.Devices = append(device.Spec.Devices, deviceInfo1)
	device.Spec.Devices = append(device.Spec.Devices, deviceInfo2)
	ds.nodeDeviceCache.onDeviceAdd(device)

	podToCreate := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "ns",
			Name:      "pod1",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							apiext.NvidiaGPU: *resource.NewQuantity(1, resource.DecimalSI),
						},
					},
				},
			},
		},
	}
	podToCreate.Spec.NodeName = "node1"
	allocResult := make(map[schedulingv1alpha1.DeviceType][]*extension.DeviceAllocation)
	deviceAllocation := &extension.DeviceAllocation{
		Minor: 0,
		Resources: corev1.ResourceList{
			apiext.GPUCore:        *resource.NewQuantity(49, resource.DecimalSI),
			apiext.GPUMemoryRatio: *resource.NewQuantity(49, resource.DecimalSI),
		},
	}
	allocResult[schedulingv1alpha1.GPU] = append(allocResult[schedulingv1alpha1.GPU], deviceAllocation)
	deviceAllocation = &extension.DeviceAllocation{
		Minor: 1,
		Resources: corev1.ResourceList{
			apiext.GPUCore:        *resource.NewQuantity(49, resource.DecimalSI),
			apiext.GPUMemoryRatio: *resource.NewQuantity(49, resource.DecimalSI),
		},
	}
	allocResult[schedulingv1alpha1.GPU] = append(allocResult[schedulingv1alpha1.GPU], deviceAllocation)
	deviceAllocation = &extension.DeviceAllocation{
		Minor: 2,
		Resources: corev1.ResourceList{
			apiext.KoordFPGA: *resource.NewQuantity(51, resource.DecimalSI),
		},
	}
	allocResult[schedulingv1alpha1.FPGA] = append(allocResult[schedulingv1alpha1.FPGA], deviceAllocation)
	apiext.SetDeviceAllocations(podToCreate, allocResult)
	ds.nodeDeviceCache.onPodAdd(podToCreate)

	engine := gin.Default()
	ds.RegisterEndpoints(engine.Group("/"))
	w := httptest.NewRecorder()
	req, _ := http.NewRequest("GET", "/nodeDeviceSummaries", nil)
	engine.ServeHTTP(w, req)
	assert.Equal(t, http.StatusOK, w.Result().StatusCode)
	nodeDeviceSummary := make(map[string]*NodeDeviceSummary)

	json.Unmarshal([]byte(w.Body.String()), &nodeDeviceSummary)

	nodeDeviceSummaryExpect := make(map[string]*NodeDeviceSummary)
	nodeDeviceSummaryExpect["node1"] = NewNodeDeviceSummary()
	nodeDeviceSummaryExpect["node1"].DeviceTotal = map[corev1.ResourceName]*resource.Quantity{
		apiext.GPUCore:        resource.NewQuantity(200, resource.DecimalSI),
		apiext.GPUMemoryRatio: resource.NewQuantity(200, resource.DecimalSI),
		apiext.KoordFPGA:      resource.NewQuantity(100, resource.DecimalSI),
	}
	assert.True(t, apiequality.Semantic.DeepEqual(nodeDeviceSummary["node1"].DeviceTotal, nodeDeviceSummaryExpect["node1"].DeviceTotal))

	nodeDeviceSummaryExpect["node1"].DeviceFree = map[corev1.ResourceName]*resource.Quantity{
		apiext.GPUCore:        resource.NewQuantity(102, resource.DecimalSI),
		apiext.GPUMemoryRatio: resource.NewQuantity(102, resource.DecimalSI),
		apiext.KoordFPGA:      resource.NewQuantity(49, resource.DecimalSI),
	}
	assert.True(t, apiequality.Semantic.DeepEqual(nodeDeviceSummary["node1"].DeviceFree, nodeDeviceSummaryExpect["node1"].DeviceFree))

	nodeDeviceSummaryExpect["node1"].DeviceUsed = map[corev1.ResourceName]*resource.Quantity{
		apiext.GPUCore:        resource.NewQuantity(98, resource.DecimalSI),
		apiext.GPUMemoryRatio: resource.NewQuantity(98, resource.DecimalSI),
		apiext.KoordFPGA:      resource.NewQuantity(51, resource.DecimalSI),
	}
	assert.True(t, apiequality.Semantic.DeepEqual(nodeDeviceSummary["node1"].DeviceUsed, nodeDeviceSummaryExpect["node1"].DeviceUsed))

	nodeDeviceSummaryExpect["node1"].DeviceTotalDetail = map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: map[int]corev1.ResourceList{
			0: {
				apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
				apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
			},
			1: {
				apiext.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
				apiext.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
			},
		},
		schedulingv1alpha1.FPGA: map[int]corev1.ResourceList{
			2: {
				apiext.KoordFPGA: *resource.NewQuantity(100, resource.DecimalSI),
			},
		},
	}
	assert.True(t, apiequality.Semantic.DeepEqual(nodeDeviceSummary["node1"].DeviceTotalDetail, nodeDeviceSummaryExpect["node1"].DeviceTotalDetail))

	nodeDeviceSummaryExpect["node1"].DeviceFreeDetail = map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: map[int]corev1.ResourceList{
			0: {
				apiext.GPUCore:        *resource.NewQuantity(51, resource.DecimalSI),
				apiext.GPUMemoryRatio: *resource.NewQuantity(51, resource.DecimalSI),
			},
			1: {
				apiext.GPUCore:        *resource.NewQuantity(51, resource.DecimalSI),
				apiext.GPUMemoryRatio: *resource.NewQuantity(51, resource.DecimalSI),
			},
		},
		schedulingv1alpha1.FPGA: map[int]corev1.ResourceList{
			2: {
				apiext.KoordFPGA: *resource.NewQuantity(49, resource.DecimalSI),
			},
		},
	}
	assert.True(t, apiequality.Semantic.DeepEqual(nodeDeviceSummary["node1"].DeviceFreeDetail, nodeDeviceSummaryExpect["node1"].DeviceFreeDetail))

	nodeDeviceSummaryExpect["node1"].DeviceUsedDetail = map[schedulingv1alpha1.DeviceType]deviceResources{
		schedulingv1alpha1.GPU: map[int]corev1.ResourceList{
			0: {
				apiext.GPUCore:        *resource.NewQuantity(49, resource.DecimalSI),
				apiext.GPUMemoryRatio: *resource.NewQuantity(49, resource.DecimalSI),
			},
			1: {
				apiext.GPUCore:        *resource.NewQuantity(49, resource.DecimalSI),
				apiext.GPUMemoryRatio: *resource.NewQuantity(49, resource.DecimalSI),
			},
		},
		schedulingv1alpha1.FPGA: map[int]corev1.ResourceList{
			2: {
				apiext.KoordFPGA: *resource.NewQuantity(51, resource.DecimalSI),
			},
		},
	}
	assert.True(t, apiequality.Semantic.DeepEqual(nodeDeviceSummary["node1"].DeviceUsedDetail, nodeDeviceSummaryExpect["node1"].DeviceUsedDetail))

	nodeDeviceSummaryExpect["node1"].AllocateSet = map[schedulingv1alpha1.DeviceType]map[string]map[int]corev1.ResourceList{
		schedulingv1alpha1.GPU: {
			"ns/pod1": {
				0: {
					apiext.GPUCore:        *resource.NewQuantity(49, resource.DecimalSI),
					apiext.GPUMemoryRatio: *resource.NewQuantity(49, resource.DecimalSI),
				},
				1: {
					apiext.GPUCore:        *resource.NewQuantity(49, resource.DecimalSI),
					apiext.GPUMemoryRatio: *resource.NewQuantity(49, resource.DecimalSI),
				},
			},
		},
		schedulingv1alpha1.FPGA: {
			"ns/pod1": {
				2: {
					apiext.KoordFPGA: *resource.NewQuantity(51, resource.DecimalSI),
				},
			},
		},
	}
	assert.True(t, apiequality.Semantic.DeepEqual(nodeDeviceSummary["node1"].AllocateSet, nodeDeviceSummaryExpect["node1"].AllocateSet))
}
