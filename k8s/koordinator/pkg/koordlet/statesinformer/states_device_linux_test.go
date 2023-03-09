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

package statesinformer

import (
	"context"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	schedulingfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	"github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache"
	mock_metriccache "github.com/koordinator-sh/koordinator/pkg/koordlet/metriccache/mockmetriccache"
)

func Test_reportGPUDevice(t *testing.T) {
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test",
		},
	}
	fakeClient := schedulingfake.NewSimpleClientset().SchedulingV1alpha1().Devices()
	ctl := gomock.NewController(t)
	mockMetricCache := mock_metriccache.NewMockMetricCache(ctl)
	fakeResult := metriccache.NodeResourceQueryResult{
		Metric: &metriccache.NodeResourceMetric{
			GPUs: []metriccache.GPUMetric{
				{
					DeviceUUID:  "1",
					Minor:       0,
					SMUtil:      80,
					MemoryUsed:  *resource.NewQuantity(30, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(8000, resource.BinarySI),
				},
				{
					DeviceUUID:  "2",
					Minor:       1,
					SMUtil:      40,
					MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
					MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
				},
			},
		},
	}
	mockMetricCache.EXPECT().GetNodeResourceMetric(gomock.Any()).Return(fakeResult).AnyTimes()
	r := &statesInformer{
		deviceClient: fakeClient,
		metricsCache: mockMetricCache,
		states: &pluginState{
			informerPlugins: map[pluginName]informerPlugin{
				nodeInformerName: &nodeInformer{
					node: testNode,
				},
			},
		},
		getGPUDriverAndModelFunc: func() (string, string) {
			return "A100", "470"
		},
	}
	r.reportDevice()
	expectedDevices := []schedulingv1alpha1.DeviceInfo{
		{
			UUID:   "1",
			Minor:  pointer.Int32Ptr(0),
			Type:   schedulingv1alpha1.GPU,
			Health: true,
			Resources: map[corev1.ResourceName]resource.Quantity{
				extension.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
				extension.GPUMemory:      *resource.NewQuantity(8000, resource.BinarySI),
				extension.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
			},
		},
		{
			UUID:   "2",
			Minor:  pointer.Int32Ptr(1),
			Type:   schedulingv1alpha1.GPU,
			Health: true,
			Resources: map[corev1.ResourceName]resource.Quantity{
				extension.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
				extension.GPUMemory:      *resource.NewQuantity(10000, resource.BinarySI),
				extension.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
			},
		},
	}
	device, err := fakeClient.Get(context.TODO(), "test", metav1.GetOptions{})
	assert.Equal(t, nil, err)
	assert.Equal(t, device.Spec.Devices, expectedDevices)

	fakeResult.Metric.GPUs = append(fakeResult.Metric.GPUs, metriccache.GPUMetric{
		DeviceUUID:  "3",
		Minor:       2,
		SMUtil:      40,
		MemoryUsed:  *resource.NewQuantity(50, resource.BinarySI),
		MemoryTotal: *resource.NewQuantity(10000, resource.BinarySI),
	})
	mockMetricCache.EXPECT().GetNodeResourceMetric(gomock.Any()).Return(fakeResult).AnyTimes()
	r.reportDevice()

	expectedDevices = append(expectedDevices, schedulingv1alpha1.DeviceInfo{
		UUID:   "3",
		Minor:  pointer.Int32Ptr(2),
		Type:   schedulingv1alpha1.GPU,
		Health: true,
		Resources: map[corev1.ResourceName]resource.Quantity{
			extension.GPUCore:        *resource.NewQuantity(100, resource.DecimalSI),
			extension.GPUMemory:      *resource.NewQuantity(10000, resource.BinarySI),
			extension.GPUMemoryRatio: *resource.NewQuantity(100, resource.DecimalSI),
		},
	})
	device, err = fakeClient.Get(context.TODO(), "test", metav1.GetOptions{})
	assert.Equal(t, nil, err)
	assert.Equal(t, device.Spec.Devices, expectedDevices)
	assert.Equal(t, device.Labels[extension.GPUModel], "A100")
	assert.Equal(t, device.Labels[extension.GPUDriver], "470")
}
