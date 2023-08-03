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

package batchresource

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/clock"
	"k8s.io/utils/pointer"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/noderesource/framework"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func makeResourceList(cpu, memory string) corev1.ResourceList {
	return corev1.ResourceList{
		corev1.ResourceCPU:    resource.MustParse(cpu),
		corev1.ResourceMemory: resource.MustParse(memory),
	}
}

func makeNodeStat(cpu, memory string) corev1.NodeStatus {
	return corev1.NodeStatus{
		Capacity:    makeResourceList(cpu, memory),
		Allocatable: makeResourceList(cpu, memory),
	}
}

func makeResourceReq(cpu, memory string) corev1.ResourceRequirements {
	return corev1.ResourceRequirements{
		Requests: makeResourceList(cpu, memory),
		Limits:   makeResourceList(cpu, memory),
	}
}

var podList = &corev1.PodList{
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
						Resources: makeResourceReq("20", "20G"),
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
						Resources: makeResourceReq("10", "20G"),
					}, {
						Resources: makeResourceReq("10", "20G"),
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
						Resources: makeResourceReq("10", "10G"),
					},
				},
			},
			Status: corev1.PodStatus{
				Phase: corev1.PodSucceeded,
			},
		},
	},
}

var resourceMetrics = &framework.ResourceMetrics{
	NodeMetric: &slov1alpha1.NodeMetric{
		Status: slov1alpha1.NodeMetricStatus{
			UpdateTime: &metav1.Time{Time: time.Now()},
			NodeMetric: &slov1alpha1.NodeMetricInfo{
				NodeUsage: slov1alpha1.ResourceMap{
					ResourceList: makeResourceList("50", "55G"),
				},
			},
			PodsMetric: []*slov1alpha1.PodMetricInfo{
				genPodMetric("test", "podA", "11", "11G"),
				genPodMetric("test", "podB", "10", "10G"),
				genPodMetric("test", "podC", "22", "22G"),
			},
		},
	},
}

func genPodMetric(namespace string, name string, cpu string, memory string) *slov1alpha1.PodMetricInfo {
	return &slov1alpha1.PodMetricInfo{
		Name:      name,
		Namespace: namespace,
		PodUsage: slov1alpha1.ResourceMap{
			ResourceList: makeResourceList(cpu, memory),
		},
	}
}

func TestPluginCalculate(t *testing.T) {
	memoryCalculateByReq := extension.CalculateByPodRequest
	type args struct {
		strategy             *extension.ColocationStrategy
		node                 *corev1.Node
		nodeAnnoReservedCase []*extension.NodeReservation
	}

	tests := []struct {
		name    string
		args    args
		want    []framework.ResourceItem
		wantErr bool
	}{
		{
			name:    "error for invalid arguments",
			wantErr: true,
		},
		{
			name: "calculate with memory usage",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
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
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve nothing from node.annotation",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
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
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve specific cpus from node.annotation and left equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								ReservedCPUs: "0-1",
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
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
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve specific cpus from node.annotation and right equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								ReservedCPUs: "0-19",
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(12000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:12000 = nodeAllocatable:100000 - nodeReservation:35000 - systemUsage:20000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(33, 9),
					Message:  "batchAllocatable[Mem(GB)]:33 = nodeAllocatable:120 - nodeReservation:42 - systemUsage:12 - podLSUsed:33",
				},
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve cpus by quantity from node.annotation and right equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources: corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("20")},
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(12000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:12000 = nodeAllocatable:100000 - nodeReservation:35000 - systemUsage:20000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(33, 9),
					Message:  "batchAllocatable[Mem(GB)]:33 = nodeAllocatable:120 - nodeReservation:42 - systemUsage:12 - podLSUsed:33",
				},
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve cpus by quantity and specific cores from node.annotation and right equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources:    corev1.ResourceList{corev1.ResourceCPU: resource.MustParse("20")},
								ReservedCPUs: "0-9",
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(22000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:22000 = nodeAllocatable:100000 - nodeReservation:35000 - systemUsage:10000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(33, 9),
					Message:  "batchAllocatable[Mem(GB)]:33 = nodeAllocatable:120 - nodeReservation:42 - systemUsage:12 - podLSUsed:33",
				},
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve memory from node.annotation and right equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources: corev1.ResourceList{corev1.ResourceMemory: resource.MustParse("20G")},
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(25000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:25000 = nodeAllocatable:100000 - nodeReservation:35000 - systemUsage:7000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(25, 9),
					Message:  "batchAllocatable[Mem(GB)]:25 = nodeAllocatable:120 - nodeReservation:42 - systemUsage:20 - podLSUsed:33",
				},
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve memory from node.annotation and left equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources: corev1.ResourceList{corev1.ResourceMemory: resource.MustParse("5G")},
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
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
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve memory and cpu from node.annotation and left equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources: corev1.ResourceList{
									corev1.ResourceMemory: resource.MustParse("5G"),
									corev1.ResourceCPU:    resource.MustParse("2"),
								},
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
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
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve memory and cpu from node.annotation and right equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources: corev1.ResourceList{
									corev1.ResourceMemory: resource.MustParse("20G"),
									corev1.ResourceCPU:    resource.MustParse("10"),
								},
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(22000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:22000 = nodeAllocatable:100000 - nodeReservation:35000 - systemUsage:10000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(25, 9),
					Message:  "batchAllocatable[Mem(GB)]:25 = nodeAllocatable:120 - nodeReservation:42 - systemUsage:20 - podLSUsed:33",
				},
			},
			wantErr: false,
		},
		{
			name: "calculate with memory usage and reserve memory and specific cores from node.annotation and right equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources: corev1.ResourceList{
									corev1.ResourceMemory: resource.MustParse("20G"),
								},
								ReservedCPUs: "0-9",
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
				{
					Name:     extension.BatchCPU,
					Quantity: resource.NewQuantity(22000, resource.DecimalSI),
					Message:  "batchAllocatable[CPU(Milli-Core)]:22000 = nodeAllocatable:100000 - nodeReservation:35000 - systemUsage:10000 - podLSUsed:33000",
				},
				{
					Name:     extension.BatchMemory,
					Quantity: resource.NewScaledQuantity(25, 9),
					Message:  "batchAllocatable[Mem(GB)]:25 = nodeAllocatable:120 - nodeReservation:42 - systemUsage:20 - podLSUsed:33",
				},
			},
			wantErr: false,
		},
		{
			name: "calculate with memory request",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(70),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(80),
					MemoryCalculatePolicy:         &memoryCalculateByReq,
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Labels: map[string]string{
							"memory-calculate-by-request": "true",
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
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
			},
			wantErr: false,
		},
		{
			name: "calculate with memory request and reserve memory from node.annotation and left equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(70),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(80),
					MemoryCalculatePolicy:         &memoryCalculateByReq,
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Labels: map[string]string{
							"memory-calculate-by-request": "true",
						},
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources: corev1.ResourceList{corev1.ResourceMemory: resource.MustParse("5G")},
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
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
			},
			wantErr: false,
		},
		{
			name: "calculate with memory request and reserve memory from node.annotation and right equal sys.used",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(70),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(80),
					MemoryCalculatePolicy:         &memoryCalculateByReq,
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node1",
						Labels: map[string]string{
							"memory-calculate-by-request": "true",
						},
						Annotations: map[string]string{
							extension.AnnotationNodeReservation: util.GetNodeAnnoReservedJson(extension.NodeReservation{
								Resources: corev1.ResourceList{corev1.ResourceMemory: resource.MustParse("10G")},
							}),
						},
					},
					Status: makeNodeStat("100", "120G"),
				},
			},
			want: []framework.ResourceItem{
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
			},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{}
			got, gotErr := p.Calculate(tt.args.strategy, tt.args.node, podList, resourceMetrics)
			assert.Equal(t, tt.wantErr, gotErr != nil)
			testingCorrectResourceItems(t, tt.want, got)
		})
	}
}

func TestPlugin_isDegradeNeeded(t *testing.T) {
	const degradeTimeoutMinutes = 10
	type fields struct {
		Clock clock.Clock
	}
	type args struct {
		strategy   *extension.ColocationStrategy
		nodeMetric *slov1alpha1.NodeMetric
		node       *corev1.Node
	}
	tests := []struct {
		name   string
		fields fields
		args   args
		want   bool
	}{
		{
			name: "empty NodeMetric should degrade",
			fields: fields{
				Clock: clock.RealClock{},
			},
			args: args{
				nodeMetric: nil,
			},
			want: true,
		},
		{
			name: "empty NodeMetric status should degrade",
			fields: fields{
				Clock: clock.RealClock{},
			},
			args: args{
				nodeMetric: &slov1alpha1.NodeMetric{},
			},
			want: true,
		},
		{
			name: "outdated NodeMetric status should degrade",
			fields: fields{
				Clock: clock.RealClock{},
			},
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:             pointer.BoolPtr(true),
					DegradeTimeMinutes: pointer.Int64Ptr(degradeTimeoutMinutes),
				},
				nodeMetric: &slov1alpha1.NodeMetric{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
						Labels: map[string]string{
							"xxx": "yy",
						},
					},
					Status: slov1alpha1.NodeMetricStatus{
						UpdateTime: &metav1.Time{
							Time: time.Now().Add(time.Minute * -(degradeTimeoutMinutes + 1)),
						},
					},
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
						Labels: map[string]string{
							"xxx": "yyy",
						},
					},
					Status: corev1.NodeStatus{},
				},
			},
			want: true,
		},
		{
			name: "outdated NodeMetric status should degrade 1",
			fields: fields{
				Clock: clock.NewFakeClock(time.Now().Add(time.Minute * (degradeTimeoutMinutes + 1))),
			},
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:             pointer.BoolPtr(true),
					DegradeTimeMinutes: pointer.Int64Ptr(degradeTimeoutMinutes),
				},
				nodeMetric: &slov1alpha1.NodeMetric{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
						Labels: map[string]string{
							"xxx": "yy",
						},
					},
					Status: slov1alpha1.NodeMetricStatus{
						UpdateTime: &metav1.Time{
							Time: time.Now(),
						},
					},
				},
				node: &corev1.Node{
					ObjectMeta: metav1.ObjectMeta{
						Name: "test-node0",
						Labels: map[string]string{
							"xxx": "yyy",
						},
					},
					Status: corev1.NodeStatus{},
				},
			},
			want: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			oldClock := Clock
			Clock = tt.fields.Clock
			defer func() {
				Clock = oldClock
			}()

			p := &Plugin{}
			assert.Equal(t, tt.want, p.isDegradeNeeded(tt.args.strategy, tt.args.nodeMetric, tt.args.node))
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
			want: makeResourceList("4", "10Gi"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := getPodMetricUsage(tt.args.info)
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
			want: makeResourceList("40", "80Gi"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := getNodeMetricUsage(tt.args.info)
			testingCorrectResourceList(t, &tt.want, &got)
		})
	}
}

func Test_getNodeReservation(t *testing.T) {
	type args struct {
		strategy *extension.ColocationStrategy
		node     *corev1.Node
	}
	tests := []struct {
		name string
		args args
		want corev1.ResourceList
	}{
		{
			name: "get correct reserved node resource quantity",
			args: args{
				strategy: &extension.ColocationStrategy{
					Enable:                        pointer.BoolPtr(true),
					CPUReclaimThresholdPercent:    pointer.Int64Ptr(65),
					MemoryReclaimThresholdPercent: pointer.Int64Ptr(65),
					DegradeTimeMinutes:            pointer.Int64Ptr(15),
					UpdateTimeThresholdSeconds:    pointer.Int64Ptr(300),
					ResourceDiffThreshold:         pointer.Float64Ptr(0.1),
				},
				node: &corev1.Node{
					Status: makeNodeStat("100", "100Gi"),
				},
			},
			want: makeResourceList("35", "35Gi"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := getNodeReservation(tt.args.strategy, tt.args.node)
			testingCorrectResourceList(t, &tt.want, &got)
		})
	}
}

func testingCorrectResourceItems(t *testing.T, want, got []framework.ResourceItem) {
	assert.Equal(t, len(want), len(got))
	for i := range want {
		qWant, qGot := want[i].Quantity, got[i].Quantity
		want[i].Quantity, got[i].Quantity = nil, nil
		assert.Equal(t, want[i], got[i], "equal fields for resource "+want[i].Name)
		assert.Equal(t, qWant.MilliValue(), qGot.MilliValue(), "equal values for resource "+want[i].Name)
		want[i].Quantity, got[i].Quantity = qWant, qGot
	}
}

func testingCorrectResourceList(t *testing.T, want, got *corev1.ResourceList) {
	assert.Equal(t, want.Cpu().MilliValue(), got.Cpu().MilliValue(), "should get correct cpu request")
	assert.Equal(t, want.Memory().Value(), got.Memory().Value(), "should get correct memory request")
}
