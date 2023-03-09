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
	"testing"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
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

func (cache *FakeCfgCache) IsErrorStatus() bool {
	return cache.errorStatus
}

func Test_calculateBEResource(t *testing.T) {
	type args struct {
		node       *corev1.Node
		podList    *corev1.PodList
		nodeMetric *slov1alpha1.NodeMetric
	}
	tests := []struct {
		name string
		args args
		want *nodeBEResource
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
			want: &nodeBEResource{
				MilliCPU: resource.NewQuantity(0, resource.DecimalSI),
				Memory:   resource.NewScaledQuantity(6, 9),
				Message: "nodeAllocatableBE[CPU(Milli-Core)]:0 = nodeAllocatable:20000 - nodeReservation:7000 - systemUsage:0 - podLSUsed:20000\n" +
					"nodeAllocatableBE[Mem(GB)]:6 = nodeAllocatable:40 - nodeReservation:14 - systemUsage:0 - podLSUsed:20\n",
			},
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
			want: &nodeBEResource{
				MilliCPU: resource.NewQuantity(25000, resource.DecimalSI),
				Memory:   resource.NewScaledQuantity(33, 9),
				Message: "nodeAllocatableBE[CPU(Milli-Core)]:25000 = nodeAllocatable:100000 - nodeReservation:35000 - systemUsage:7000 - podLSUsed:33000\n" +
					"nodeAllocatableBE[Mem(GB)]:33 = nodeAllocatable:120 - nodeReservation:42 - systemUsage:12 - podLSUsed:33\n",
			},
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
			want: &nodeBEResource{
				MilliCPU: resource.NewQuantity(30000, resource.DecimalSI),
				Memory:   resource.NewScaledQuantity(39, 9),
				Message: "nodeAllocatableBE[CPU(Milli-Core)]:30000 = nodeAllocatable:100000 - nodeReservation:30000 - systemUsage:7000 - podLSUsed:33000\n" +
					"nodeAllocatableBE[Mem(GB)]:39 = nodeAllocatable:120 - nodeReservation:36 - systemUsage:12 - podLSUsed:33\n",
			},
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
			want: &nodeBEResource{
				MilliCPU: resource.NewQuantity(30000, resource.DecimalSI),
				Memory:   resource.NewScaledQuantity(36, 9),
				Message: "nodeAllocatableBE[CPU(Milli-Core)]:30000 = nodeAllocatable:100000 - nodeReservation:30000 - systemUsage:7000 - podLSUsed:33000\n" +
					"nodeAllocatableBE[Mem(GB)]:36 = nodeAllocatable:120 - nodeReservation:24 - podLSRequest:60\n",
			},
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
							NodeSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"xxx": "yyy",
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								CPUReclaimThresholdPercent:    pointer.Int64Ptr(70),
								MemoryReclaimThresholdPercent: pointer.Int64Ptr(70),
							},
						},
						{
							NodeSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"memory-calculate-by-request": "true",
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								CPUReclaimThresholdPercent:    pointer.Int64Ptr(70),
								MemoryReclaimThresholdPercent: pointer.Int64Ptr(80),
								MemoryCalculatePolicy:         &memoryCalculateByReq,
							},
						},
						{
							NodeSelector: &metav1.LabelSelector{
								MatchLabels: map[string]string{
									"abc": "def",
								},
							},
							ColocationStrategy: extension.ColocationStrategy{
								CPUReclaimThresholdPercent:    pointer.Int64Ptr(60),
								MemoryReclaimThresholdPercent: pointer.Int64Ptr(60),
							},
						},
					},
				},
			}}
			got := r.calculateBEResource(tt.args.node, tt.args.podList, tt.args.nodeMetric)
			if !got.MilliCPU.Equal(*tt.want.MilliCPU) {
				t.Errorf("calculateBEResource() should get correct cpu resource, want %v, got %v",
					tt.want.MilliCPU, got.MilliCPU)
			}
			if !got.Memory.Equal(*tt.want.Memory) {
				t.Errorf("calculateBEResource() should get correct memory resource, want %v, got %v",
					tt.want.Memory, got.Memory)
			}
			if got.Message != tt.want.Message {
				t.Errorf("calculateBEResource() should get correct resource message, want %v, got %v",
					tt.want.Message, got.Message)
			}
		})
	}
}

func Test_getPodMetricUsage(t *testing.T) {
	type args struct {
		info *slov1alpha1.PodMetricInfo
	}
	tests := []struct {
		name string
		args args
		want corev1.ResourceList
	}{
		{
			name: "get correct scaled resource quantity",
			args: args{
				info: &slov1alpha1.PodMetricInfo{
					PodUsage: slov1alpha1.ResourceMap{
						ResourceList: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("4"),
							corev1.ResourceMemory: resource.MustParse("10Gi"),
							"unknown_resource":    resource.MustParse("1"),
						},
					},
				},
			},
			want: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("4"),
				corev1.ResourceMemory: resource.MustParse("10Gi"),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := NodeResourceReconciler{}
			got := r.getPodMetricUsage(tt.args.info)
			testingCorrectResourceList(t, &tt.want, &got)
		})
	}
}

func Test_getNodeMetricUsage(t *testing.T) {
	type args struct {
		info *slov1alpha1.NodeMetricInfo
	}
	tests := []struct {
		name string
		args args
		want corev1.ResourceList
	}{
		{
			name: "get correct scaled resource quantity",
			args: args{
				info: &slov1alpha1.NodeMetricInfo{
					NodeUsage: slov1alpha1.ResourceMap{
						ResourceList: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("40"),
							corev1.ResourceMemory: resource.MustParse("80Gi"),
							"unknown_resource":    resource.MustParse("10"),
						},
					},
				},
			},
			want: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("40"),
				corev1.ResourceMemory: resource.MustParse("80Gi"),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			r := NodeResourceReconciler{}
			got := r.getNodeMetricUsage(tt.args.info)
			testingCorrectResourceList(t, &tt.want, &got)
		})
	}
}

func Test_getNodeReservation(t *testing.T) {
	type args struct {
		node *corev1.Node
	}
	tests := []struct {
		name string
		args args
		want corev1.ResourceList
	}{
		{
			name: "get correct reserved node resource quantity",
			args: args{
				node: &corev1.Node{
					Status: corev1.NodeStatus{
						Allocatable: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("100"),
							corev1.ResourceMemory: resource.MustParse("100Gi"),
						},
						Capacity: corev1.ResourceList{
							corev1.ResourceCPU:    resource.MustParse("100"),
							corev1.ResourceMemory: resource.MustParse("100Gi"),
						},
					},
				},
			},
			want: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("35"),
				corev1.ResourceMemory: resource.MustParse("35Gi"),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
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
				},
			}}
			got := r.getNodeReservation(tt.args.node)
			testingCorrectResourceList(t, &tt.want, &got)
		})
	}
}

func testingCorrectResourceList(t *testing.T, want, got *corev1.ResourceList) {
	if !got.Cpu().Equal(*want.Cpu()) {
		t.Errorf("should get correct cpu request, want %v, got %v",
			want.Cpu(), got.Cpu())
	}
	if !got.Memory().Equal(*want.Memory()) {
		t.Errorf("should get correct memory request, want %v, got %v",
			want.Memory(), got.Memory())
	}
}
