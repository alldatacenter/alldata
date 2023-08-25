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
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/noderesource/framework"
)

type FakeCfgCache struct {
	cfg         extension.ColocationCfg
	available   bool
	errorStatus bool
}

func (f *FakeCfgCache) GetCfgCopy() *extension.ColocationCfg {
	return &f.cfg
}

func (f *FakeCfgCache) IsCfgAvailable() bool {
	return f.available
}

func (f *FakeCfgCache) IsErrorStatus() bool {
	return f.errorStatus
}

func Test_calculateNodeResource(t *testing.T) {
	type args struct {
		node       *corev1.Node
		podList    *corev1.PodList
		nodeMetric *slov1alpha1.NodeMetric
	}
	tests := []struct {
		name string
		args args
		want *framework.NodeResource
	}{
		{
			name: "calculate normal result always no less than zero",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("20"),
							corev1.ResourceMemory: resource.MustParse("40G"),
						},
						Capacity: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("20"),
							corev1.ResourceMemory: resource.MustParse("40G"),
						},
					},
				},
				podList: &corev1.PodList{
					Items: []corev1.Pod{
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podA",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSLS),
								},
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node0",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("20"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("20"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
					},
				},
				nodeMetric: &slov1alpha1.NodeMetric{
					Status: slov1alpha1.NodeMetricStatus{
						UpdateTime: &metav1.Time{Time: time.Now()},
						NodeMetric: &slov1alpha1.NodeMetricInfo{},
						PodsMetric: []*slov1alpha1.PodMetricInfo{},
					},
				},
			},
			want: framework.NewNodeResource([]framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(0, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:0 = nodeAllocatable:20000 - nodeReservation:7000 - systemUsage:0 - podLSUsed:20000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(6, 9),
					Message:  "batchAllocatable[Mem(GB)]:6 = nodeAllocatable:40 - nodeReservation:14 - systemUsage:0 - podLSUsed:20",
				},
			}...),
		},
		{
			name: "calculate normal result correctly by usage",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("100"),
							corev1.ResourceMemory: resource.MustParse("120G"),
						},
						Capacity: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("100"),
							corev1.ResourceMemory: resource.MustParse("120G"),
						},
					},
				},
				podList: &corev1.PodList{
					Items: []corev1.Pod{
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podA",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSLS),
								},
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("20"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("20"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podB",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSBE),
								},
							},
							Spec: corev1.PodSpec{
								NodeName:   "test-node1",
								Containers: []corev1.Container{{}},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podC",
								Namespace: "test",
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									}, {
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodPending,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podD",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSBE),
								},
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("10G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("10G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodSucceeded,
							},
						},
					},
				},
				nodeMetric: &slov1alpha1.NodeMetric{
					Status: slov1alpha1.NodeMetricStatus{
						UpdateTime: &metav1.Time{Time: time.Now()},
						NodeMetric: &slov1alpha1.NodeMetricInfo{
							NodeUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("50"),
									corev1.ResourceMemory: resource.MustParse("55G"),
								},
							},
						},
						PodsMetric: []*slov1alpha1.PodMetricInfo{
							{
								Namespace: "test",
								Name:      "podA",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("11"),
										corev1.ResourceMemory: resource.MustParse("11G"),
									},
								},
							}, {
								Namespace: "test",
								Name:      "podB",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("10"),
										corev1.ResourceMemory: resource.MustParse("10G"),
									},
								},
							},
							{
								Namespace: "test",
								Name:      "podC",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("22"),
										corev1.ResourceMemory: resource.MustParse("22G"),
									},
								},
							},
						},
					},
				},
			},
			want: framework.NewNodeResource([]framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(25000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:25000 = nodeAllocatable:100000 - nodeReservation:35000 - systemUsage:7000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(33, 9),
					Message:  "batchAllocatable[Mem(GB)]:33 = nodeAllocatable:120 - nodeReservation:42 - systemUsage:12 - podLSUsed:33",
				},
			}...),
		},
		{
			name: "calculate normal result correctly with node-specified config by memory usage",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Labels: map[string]string{
							"xxx": "yyy",
						},
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("100"),
							corev1.ResourceMemory: resource.MustParse("120G"),
						},
						Capacity: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("100"),
							corev1.ResourceMemory: resource.MustParse("120G"),
						},
					},
				},
				podList: &corev1.PodList{
					Items: []corev1.Pod{
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podA",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSLS),
								},
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("20"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("20"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podB",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSBE),
								},
							},
							Spec: corev1.PodSpec{
								NodeName:   "test-node1",
								Containers: []corev1.Container{{}},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podC",
								Namespace: "test",
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									}, {
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodPending,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podD",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSBE),
								},
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("10G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("10G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodSucceeded,
							},
						},
					},
				},
				nodeMetric: &slov1alpha1.NodeMetric{
					Status: slov1alpha1.NodeMetricStatus{
						UpdateTime: &metav1.Time{Time: time.Now()},
						NodeMetric: &slov1alpha1.NodeMetricInfo{
							NodeUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("50"),
									corev1.ResourceMemory: resource.MustParse("55G"),
								},
							},
						},
						PodsMetric: []*slov1alpha1.PodMetricInfo{
							{
								Namespace: "test",
								Name:      "podA",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("11"),
										corev1.ResourceMemory: resource.MustParse("11G"),
									},
								},
							}, {
								Namespace: "test",
								Name:      "podB",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("10"),
										corev1.ResourceMemory: resource.MustParse("10G"),
									},
								},
							},
							{
								Namespace: "test",
								Name:      "podC",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("22"),
										corev1.ResourceMemory: resource.MustParse("22G"),
									},
								},
							},
						},
					},
				},
			},
			want: framework.NewNodeResource([]framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(30000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:30000 = nodeAllocatable:100000 - nodeReservation:30000 - systemUsage:7000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(39, 9),
					Message:  "batchAllocatable[Mem(GB)]:39 = nodeAllocatable:120 - nodeReservation:36 - systemUsage:12 - podLSUsed:33",
				},
			}...),
		},
		{
			name: "calculate normal result correctly with node-specified by request",
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Labels: map[string]string{
							"memory-calculate-by-request": "true",
						},
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("100"),
							corev1.ResourceMemory: resource.MustParse("120G"),
						},
						Capacity: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("100"),
							corev1.ResourceMemory: resource.MustParse("120G"),
						},
					},
				},
				podList: &corev1.PodList{
					Items: []corev1.Pod{
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podA",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSLS),
								},
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("20"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("20"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podB",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSBE),
								},
							},
							Spec: corev1.PodSpec{
								NodeName:   "test-node1",
								Containers: []corev1.Container{{}},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodRunning,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podC",
								Namespace: "test",
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									}, {
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("20G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodPending,
							},
						},
						{
							ObjectMeta: metav1.ObjectMeta{
								Name:      "podD",
								Namespace: "test",
								Labels: map[string]string{
									extension.LabelPodQoS: string(extension.QoSBE),
								},
							},
							Spec: corev1.PodSpec{
								NodeName: "test-node1",
								Containers: []corev1.Container{
									{
										Resources: corev1.ResourceRequirements{
											Requests: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("10G"),
											},
											Limits: corev1.ResourceList{
												corev1.ResourceCPU:    resource.MustParse("10"),
												corev1.ResourceMemory: resource.MustParse("10G"),
											},
										},
									},
								},
							},
							Status: corev1.PodStatus{
								Phase: corev1.PodSucceeded,
							},
						},
					},
				},
				nodeMetric: &slov1alpha1.NodeMetric{
					Status: slov1alpha1.NodeMetricStatus{
						UpdateTime: &metav1.Time{Time: time.Now()},
						NodeMetric: &slov1alpha1.NodeMetricInfo{
							NodeUsage: slov1alpha1.ResourceMap{
								ResourceList: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("50"),
									corev1.ResourceMemory: resource.MustParse("55G"),
								},
							},
						},
						PodsMetric: []*slov1alpha1.PodMetricInfo{
							{
								Namespace: "test",
								Name:      "podA",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("11"),
										corev1.ResourceMemory: resource.MustParse("11G"),
									},
								},
							}, {
								Namespace: "test",
								Name:      "podB",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("10"),
										corev1.ResourceMemory: resource.MustParse("10G"),
									},
								},
							},
							{
								Namespace: "test",
								Name:      "podC",
								PodUsage: slov1alpha1.ResourceMap{
									ResourceList: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("22"),
										corev1.ResourceMemory: resource.MustParse("22G"),
									},
								},
							},
						},
					},
				},
			},
			want: framework.NewNodeResource([]framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(30000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:30000 = nodeAllocatable:100000 - nodeReservation:30000 - systemUsage:7000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(36, 9),
					Message:  "batchAllocatable[Mem(GB)]:36 = nodeAllocatable:120 - nodeReservation:24 - podLSRequest:60",
				},
			}...),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			memoryCalculateByReq := extension.CalculateByPodRequest
			r := NodeResourceReconciler{cfgCache: &FakeCfgCache{
				cfg: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                        pointer.BoolPtr(true),
						CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
						MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
						DegradeTimeMinutes:            pointer.Int64Ptr(15),
						UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
						ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
					},
					NodeConfigs: []extension.NodeColocationCfg{
						{
							NodeCfgProfile: extension.NodeCfgProfile{
								NodeSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"xxx": "yyy",
									},
								},
								Name: "xxx-yyy",
							},
							ColocationStrategy: extension.ColocationStrategy{
								CPUReclaimThresholdPercent:    pointer.Int64Ptr(70),
								MemoryReclaimThresholdPercent: pointer.Int64Ptr(70),
							},
						},
						{
							NodeCfgProfile: extension.NodeCfgProfile{
								NodeSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"memory-calculate-by-request": "true",
									},
								},
								Name: "memory-calculate-by-request-true",
							},
							ColocationStrategy: extension.ColocationStrategy{
								CPUReclaimThresholdPercent:    pointer.Int64Ptr(70),
								MemoryReclaimThresholdPercent: pointer.Int64Ptr(80),
								MemoryCalculatePolicy:         &memoryCalculateByReq,
							},
						},
						{
							NodeCfgProfile: extension.NodeCfgProfile{
								NodeSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"abc": "def",
									},
								},
								Name: "abc-def",
							},
							ColocationStrategy: extension.ColocationStrategy{
								CPUReclaimThresholdPercent:    pointer.Int64Ptr(60),
								MemoryReclaimThresholdPercent: pointer.Int64Ptr(60),
							},
						},
					},
				},
			}}
			got := r.calculateNodeResource(tt.args.node, tt.args.nodeMetric, tt.args.podList)
			assert.Equal(t, tt.want.Resources[extension.BatchCPU].Value(), got.Resources[extension.BatchCPU].Value())
			assert.Equal(t, tt.want.Resources[extension.BatchMemory].Value(), got.Resources[extension.BatchMemory].Value())
			assert.Equal(t, tt.want.Messages, got.Messages)
			assert.Equal(t, tt.want.Resets, got.Resets)
		})
	}
}

func Test_isColocationCfgDisabled(t *testing.T) {
	type fields struct {
		config extension.ColocationCfg
	}
	type args struct {
		node *corev1.Node
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			name:   "set as disabled when no config",
			fields: fields{config: extension.ColocationCfg{}},
			args: args{
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
			},
			want: true,
		},
		{
			name: "use cluster config when nil node",
			fields: fields{
				config: extension.ColocationCfg{
					ColocationStrategy: extension.ColocationStrategy{
						Enable:                        pointer.BoolPtr(false),
						CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
						MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
						DegradeTimeMinutes:            pointer.Int64Ptr(15),
						UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
						ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
					},
					NodeConfigs: []extension.NodeColocationCfg{
						{
							NodeCfgProfile: extension.NodeCfgProfile{
								NodeSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"xxx": "yyy",
									},
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								Enable: pointer.BoolPtr(true),
							},
						},
					},
				},
			},
			args: args{},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := NodeResourceReconciler{cfgCache: &FakeCfgCache{
				cfg: tt.fields.config,
			}}
			got := r.isColocationCfgDisabled(tt.args.node)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_updateNodeResource(t *testing.T) {
	enabledCfg := &extension.ColocationCfg{
		ColocationStrategy: extension.ColocationStrategy{
			Enable:                        pointer.BoolPtr(true),
			CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
			MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
			DegradeTimeMinutes:            pointer.Int64Ptr(15),
			UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
			ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
		},
	}
	disableCfg := &extension.ColocationCfg{
		ColocationStrategy: extension.ColocationStrategy{
			Enable:                        pointer.BoolPtr(false),
			CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
			MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
			DegradeTimeMinutes:            pointer.Int64Ptr(15),
			UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
			ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
		},
	}
	type fields struct {
		Client      client.Client
		config      *extension.ColocationCfg
		SyncContext *framework.SyncContext
	}
	type args struct {
		oldNode *corev1.Node
		nr      *framework.NodeResource
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *corev1.Node
		wantErr bool
	}{
		{
			name: "no need to sync, update nothing",
			fields: fields{
				Client: fake.NewClientBuilder().WithRuntimeObjects(&corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				}).Build(),
				config: enabledCfg,
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				nr: framework.NewNodeResource([]framework.ResourceItem{
					{
						Name:     extension.BatchCPU,
						Quantity: resource.NewQuantity(20, resource.DecimalSI),
					},
					{
						Name:     extension.BatchMemory,
						Quantity: resource.NewQuantity(40*1024*1024*1024, resource.BinarySI),
					},
				}...),
			},
			want: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
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
			},
			wantErr: false,
		},
		{
			name: "update be resource successfully",
			fields: fields{
				Client: fake.NewClientBuilder().WithRuntimeObjects(&corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				}).Build(),
				config: enabledCfg,
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				nr: framework.NewNodeResource([]framework.ResourceItem{
					{
						Name:     extension.BatchCPU,
						Quantity: resource.NewQuantity(30, resource.DecimalSI),
					},
					{
						Name:     extension.BatchMemory,
						Quantity: resource.NewQuantity(50*1024*1024*1024, resource.BinarySI),
					},
				}...),
			},
			want: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						extension.BatchCPU:    *resource.NewQuantity(30, resource.DecimalSI),
						extension.BatchMemory: *resource.NewQuantity(50*1024*1024*1024, resource.BinarySI),
					},
					Capacity: corev1.ResourceList{
						extension.BatchCPU:    *resource.NewQuantity(30, resource.DecimalSI),
						extension.BatchMemory: *resource.NewQuantity(50*1024*1024*1024, resource.BinarySI),
					},
				},
			},
			wantErr: false,
		},
		{
			name: "abort update for the node that no longer exists",
			fields: fields{
				Client: fake.NewClientBuilder().Build(),
				config: enabledCfg,
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				nr: framework.NewNodeResource([]framework.ResourceItem{
					{
						Name:     extension.BatchCPU,
						Quantity: resource.NewQuantity(20, resource.DecimalSI),
					},
					{
						Name:     extension.BatchMemory,
						Quantity: resource.NewQuantity(40*1024*1024*1024, resource.BinarySI),
					},
				}...),
			},
			want: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{},
			},
			wantErr: false,
		},
		{
			name: "notice the update for invalid be resource",
			fields: fields{
				Client: fake.NewClientBuilder().WithRuntimeObjects(&corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				}).Build(),
				config: enabledCfg,
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				nr: framework.NewNodeResource([]framework.ResourceItem{
					{
						Name:     extension.BatchCPU,
						Quantity: resource.NewMilliQuantity(22200, resource.DecimalSI),
					},
					{
						Name:     extension.BatchMemory,
						Quantity: resource.NewMilliQuantity(40*1001*1023*1024*1024, resource.BinarySI),
					},
				}...),
			},
			want: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{
						extension.BatchCPU:    resource.MustParse("23"),
						extension.BatchMemory: resource.MustParse("42950637650"),
					},
					Capacity: corev1.ResourceList{
						extension.BatchCPU:    resource.MustParse("23"),
						extension.BatchMemory: resource.MustParse("42950637650"),
					},
				},
			},
			wantErr: false,
		},
		{
			name: "not update be resource with node-specified config",
			fields: fields{
				Client: fake.NewClientBuilder().WithRuntimeObjects(&corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
						Labels: map[string]string{
							"xxx": "yyy",
						},
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
				}).Build(),
				config: &extension.ColocationCfg{
					ColocationStrategy: enabledCfg.ColocationStrategy,
					NodeConfigs: []extension.NodeColocationCfg{
						{
							NodeCfgProfile: extension.NodeCfgProfile{
								NodeSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"xxx": "yyy",
									},
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
								MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
								ResourceDiffThreshold:         pointer.Float64Ptr(0.6),
							},
						},
						{
							NodeCfgProfile: extension.NodeCfgProfile{
								NodeSelector: &metav1.LabelSelector{
									MatchLabels: map[string]string{
										"abc": "def",
									},
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								CPUReclaimThresholdPercent:    pointer.Int64Ptr(60),
								MemoryReclaimThresholdPercent: pointer.Int64Ptr(60),
							},
						},
					},
				},
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
						Labels: map[string]string{
							"xxx": "yyy",
						},
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
				},
				nr: framework.NewNodeResource([]framework.ResourceItem{
					{
						Name:     extension.BatchCPU,
						Quantity: resource.NewQuantity(30, resource.DecimalSI),
					},
					{
						Name:     extension.BatchMemory,
						Quantity: resource.NewQuantity(50*1024*1024*1024, resource.BinarySI),
					},
				}...),
			},
			want: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
					Labels: map[string]string{
						"xxx": "yyy",
					},
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
			},
			wantErr: false,
		},
		{
			name: "reset be resource with enable=false config",
			fields: fields{
				Client: fake.NewClientBuilder().WithRuntimeObjects(&corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				}).Build(),
				config: disableCfg,
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				nr: framework.NewNodeResource([]framework.ResourceItem{
					{
						Name:  extension.BatchCPU,
						Reset: true,
					},
					{
						Name:  extension.BatchMemory,
						Reset: true,
					},
				}...),
			},
			want: &corev1.Node{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-node0",
				},
				Status: corev1.NodeStatus{
					Allocatable: corev1.ResourceList{},
					Capacity:    corev1.ResourceList{},
				},
			},
			wantErr: false,
		},
		{
			name: "failed to update for node not found",
			fields: fields{
				Client: fake.NewClientBuilder().WithScheme(runtime.NewScheme()).Build(),
				config: enabledCfg,
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				nr: framework.NewNodeResource([]framework.ResourceItem{
					{
						Name:     extension.BatchCPU,
						Quantity: resource.NewQuantity(30, resource.DecimalSI),
					},
					{
						Name:     extension.BatchMemory,
						Quantity: resource.NewQuantity(50*1024*1024*1024, resource.BinarySI),
					},
				}...),
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := &NodeResourceReconciler{
				Client: tt.fields.Client,
				cfgCache: &FakeCfgCache{
					cfg: *tt.fields.config,
				},
				NodeSyncContext: tt.fields.SyncContext,
				Clock:           clock.RealClock{},
			}
			oldNodeCopy := tt.args.oldNode.DeepCopy()
			got := r.updateNodeResource(tt.args.oldNode, tt.args.nr)
			assert.Equal(t, tt.wantErr, got != nil, got)
			if !tt.wantErr {
				gotNode := &corev1.Node{}
				_ = r.Client.Get(context.TODO(), types.NamespacedName{Name: tt.args.oldNode.Name}, gotNode)

				wantCPU := tt.want.Status.Allocatable[extension.BatchCPU]
				gotCPU := gotNode.Status.Allocatable[extension.BatchCPU]
				assert.Equal(t, wantCPU.Value(), gotCPU.Value())

				wantMem := tt.want.Status.Allocatable[extension.BatchMemory]
				gotMem := gotNode.Status.Allocatable[extension.BatchMemory]
				assert.Equal(t, wantMem.Value(), gotMem.Value())
			}
			assert.Equal(t, oldNodeCopy, tt.args.oldNode) // must not change the node object in cache
		})
	}
}

func Test_isNodeResourceSyncNeeded(t *testing.T) {
	type fields struct {
		SyncContext *framework.SyncContext
	}
	type args struct {
		strategy *extension.ColocationStrategy
		oldNode  *corev1.Node
		newNode  *corev1.Node
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
		want1  string
	}{
		{
			name:   "cannot update an invalid new node",
			fields: fields{SyncContext: &framework.SyncContext{}},
			args:   args{strategy: &extension.ColocationStrategy{}},
			want:   false,
		},
		{
			name: "needSync for expired node resource",
			fields: fields{
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now().Add(0 - 10*time.Minute)},
				),
			},
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				newNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
			},
			want: true,
		},
		{
			name: "needSync for cpu diff larger than 0.1",
			fields: fields{
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				newNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							extension.BatchCPU:    resource.MustParse("15"),
							extension.BatchMemory: resource.MustParse("40G"),
						},
						Capacity: corev1.ResourceList{
							extension.BatchCPU:    resource.MustParse("15"),
							extension.BatchMemory: resource.MustParse("40G"),
						},
					},
				},
			},
			want: true,
		},
		{
			name: "needSync for cpu diff larger than 0.1",
			fields: fields{
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				newNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
					},
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							extension.BatchCPU:    resource.MustParse("20"),
							extension.BatchMemory: resource.MustParse("70G"),
						},
						Capacity: corev1.ResourceList{
							extension.BatchCPU:    resource.MustParse("20"),
							extension.BatchMemory: resource.MustParse("70G"),
						},
					},
				},
			},
			want: true,
		},
		{
			name: "no need to sync, everything's ok.",
			fields: fields{
				SyncContext: framework.NewSyncContext().WithContext(
					map[string]time.Time{"/test-node0": time.Now()},
				),
			},
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				oldNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
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
				},
				newNode: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name:   "test-node0",
						Labels: map[string]string{"test-label": "test"},
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
				},
			},
			want: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := &NodeResourceReconciler{
				cfgCache:        &FakeCfgCache{},
				NodeSyncContext: tt.fields.SyncContext,
				Clock:           clock.RealClock{},
			}
			got := r.isNodeResourceSyncNeeded(tt.args.strategy, tt.args.oldNode, tt.args.newNode)
			assert.Equal(t, tt.want, got)
		})
	}
}
