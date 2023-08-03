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

package noderesource

import (
	"context"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/clock"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	schedulingfake "github.com/koordinator-sh/koordinator/pkg/client/clientset/versioned/fake"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/noderesource/framework"
)

func Test_updateNodeGPUResource_updateGPUDriverAndModel(t *testing.T) {
	fakeClient := schedulingfake.NewSimpleClientset().SchedulingV1alpha1().Devices()
	testNode := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-node",
		},
		Status: corev1.NodeStatus{
			Allocatable: corev1.ResourceList{
				extension.BatchCPU:    resource.MustParse("20"),
				extension.BatchMemory: resource.MustParse("40G"),
			},
			Capacity: corev1.ResourceList{
				extension.BatchCPU:    resource.MustParse("20"),
				extension.BatchMemory: resource.MustParse("40G"),
			},
		},
	}
	scheme := runtime.NewScheme()
	schedulingv1alpha1.AddToScheme(scheme)
	metav1.AddMetaToScheme(scheme)
	corev1.AddToScheme(scheme)
	r := &NodeResourceReconciler{
		Client:         fake.NewClientBuilder().WithRuntimeObjects(testNode).WithScheme(scheme).Build(),
		GPUSyncContext: framework.NewSyncContext(),
		Clock:          clock.RealClock{},
		cfgCache: &FakeCfgCache{
			cfg: extension.ColocationCfg{
				ColocationStrategy: extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
			},
		},
	}
	fakeDevice := &schedulingv1alpha1.Device{
		ObjectMeta: metav1.ObjectMeta{
			Name: testNode.Name,
			Labels: map[string]string{
				extension.LabelGPUModel:         "A100",
				extension.LabelGPUDriverVersion: "480",
			},
		},
		Spec: schedulingv1alpha1.DeviceSpec{
			Devices: []schedulingv1alpha1.DeviceInfo{
				{
					UUID:   "1",
					Minor:  pointer.Int32Ptr(0),
					Health: true,
					Type:   schedulingv1alpha1.GPU,
					Resources: map[corev1.ResourceName]resource.Quantity{
						extension.ResourceGPUCore:        *resource.NewQuantity(100, resource.BinarySI),
						extension.ResourceGPUMemory:      *resource.NewQuantity(8000, resource.BinarySI),
						extension.ResourceGPUMemoryRatio: *resource.NewQuantity(100, resource.BinarySI),
					},
				},
				{
					UUID:   "2",
					Minor:  pointer.Int32Ptr(1),
					Health: true,
					Type:   schedulingv1alpha1.GPU,
					Resources: map[corev1.ResourceName]resource.Quantity{
						extension.ResourceGPUCore:        *resource.NewQuantity(100, resource.BinarySI),
						extension.ResourceGPUMemory:      *resource.NewQuantity(10000, resource.BinarySI),
						extension.ResourceGPUMemoryRatio: *resource.NewQuantity(100, resource.BinarySI),
					},
				},
			},
		},
	}
	fakeClient.Create(context.TODO(), fakeDevice, metav1.CreateOptions{})
	for i := 0; i < 10; i++ {
		r.updateGPUNodeResource(testNode, fakeDevice)
	}
	err := r.Client.Get(context.TODO(), types.NamespacedName{Name: testNode.Name}, testNode)
	assert.Equal(t, nil, err)
	actualMemoryRatio := testNode.Status.Allocatable[extension.ResourceGPUMemoryRatio]
	actualMemory := testNode.Status.Allocatable[extension.ResourceGPUMemory]
	actualCore := testNode.Status.Allocatable[extension.ResourceGPUCore]
	assert.Equal(t, actualMemoryRatio.Value(), resource.NewQuantity(200, resource.DecimalSI).Value())
	assert.Equal(t, actualMemory.Value(), resource.NewQuantity(18000, resource.BinarySI).Value())
	assert.Equal(t, actualCore.Value(), resource.NewQuantity(200, resource.BinarySI).Value())

	r.updateGPUDriverAndModel(testNode, fakeDevice)
	err = r.Client.Get(context.TODO(), types.NamespacedName{Name: testNode.Name}, testNode)
	assert.Equal(t, nil, err)
	assert.Equal(t, testNode.Labels[extension.LabelGPUModel], "A100")
	assert.Equal(t, testNode.Labels[extension.LabelGPUDriverVersion], "480")
}

func Test_isGPUResourceNeedSync(t *testing.T) {
	tests := []struct {
		oldNode     *corev1.Node
		newNode     *corev1.Node
		SyncContext *framework.SyncContext
		expected    bool
	}{
		{
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						extension.ResourceGPUCore:        resource.MustParse("20"),
						extension.ResourceGPUMemory:      resource.MustParse("40G"),
						extension.ResourceGPUMemoryRatio: resource.MustParse("20"),
					},
				},
			},
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						extension.ResourceGPUCore:        resource.MustParse("20"),
						extension.ResourceGPUMemory:      resource.MustParse("40G"),
						extension.ResourceGPUMemoryRatio: resource.MustParse("20"),
					},
				},
			},
			framework.NewSyncContext().WithContext(
				map[string]time.Time{"/test-node0": time.Now()},
			),
			false,
		},
		{
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						extension.ResourceGPUCore:        resource.MustParse("20"),
						extension.ResourceGPUMemory:      resource.MustParse("40G"),
						extension.ResourceGPUMemoryRatio: resource.MustParse("21"),
					},
				},
			},
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						extension.ResourceGPUCore:        resource.MustParse("21"),
						extension.ResourceGPUMemory:      resource.MustParse("40G"),
						extension.ResourceGPUMemoryRatio: resource.MustParse("20"),
					},
				},
			},
			framework.NewSyncContext().WithContext(
				map[string]time.Time{"/test-node0": time.Now()},
			),
			false,
		},
		{
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						extension.ResourceGPUCore:        resource.MustParse("20"),
						extension.ResourceGPUMemory:      resource.MustParse("40G"),
						extension.ResourceGPUMemoryRatio: resource.MustParse("20"),
					},
				},
			},
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						extension.ResourceGPUCore:        resource.MustParse("20"),
						extension.ResourceGPUMemory:      resource.MustParse("40G"),
						extension.ResourceGPUMemoryRatio: resource.MustParse("20"),
					},
				},
			},
			framework.NewSyncContext().WithContext(
				map[string]time.Time{"/test-node0": time.Now().Add(-time.Duration(600) * time.Second)},
			),
			true,
		},
	}
	configf := &extension.ColocationCfg{
		ColocationStrategy: extension.ColocationStrategy{
			Enable:                        pointer.BoolPtr(true),
			CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
			MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
			DegradeTimeMinutes:            pointer.Int64Ptr(15),
			UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
			ResourceDiffThreshold:         pointer.Float64Ptr(0.2),
		},
	}
	for _, tt := range tests {
		r := &NodeResourceReconciler{
			GPUSyncContext: tt.SyncContext,
			cfgCache:       &FakeCfgCache{cfg: *configf},
			Clock:          clock.RealClock{},
		}
		actual := r.isGPUResourceNeedSync(tt.newNode, tt.oldNode)
		assert.Equal(t, tt.expected, actual)
	}
}

func Test_isGPULabelNeedSync(t *testing.T) {
	tests := []struct {
		oldNode  *corev1.Node
		newNode  *corev1.Node
		expected bool
	}{
		{
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
					Labels: map[string]string{
						extension.LabelGPUModel:         "A100",
						extension.LabelGPUDriverVersion: "480",
					},
				},
			},
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
					Labels: map[string]string{
						extension.LabelGPUModel:         "A100",
						extension.LabelGPUDriverVersion: "480",
					},
				},
			},
			false,
		},
		{
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
					Labels: map[string]string{
						extension.LabelGPUModel:         "P40",
						extension.LabelGPUDriverVersion: "480",
					},
				},
			},
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
					Labels: map[string]string{
						extension.LabelGPUModel:         "A100",
						extension.LabelGPUDriverVersion: "480",
					},
				},
			},
			true,
		},
		{
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
					Labels: map[string]string{
						extension.LabelGPUModel:         "A100",
						extension.LabelGPUDriverVersion: "470",
					},
				},
			},
			&corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
					Labels: map[string]string{
						extension.LabelGPUModel:         "A100",
						extension.LabelGPUDriverVersion: "480",
					},
				},
			},
			true,
		},
	}
	for _, tt := range tests {
		r := &NodeResourceReconciler{}
		actual := r.isGPULabelNeedSync(tt.newNode.Labels, tt.oldNode.Labels)
		assert.Equal(t, tt.expected, actual)
	}
}
