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
	"context"
	"reflect"
	"testing"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

func newContainerKoordBatchRes(milliCPU, memory int64) corev1.ResourceList {
	// nolint:staticcheck // SA1019: apiext.KoordBatchCPU is deprecated: because of the limitation of extended resource naming
	// nolint:staticcheck // SA1019: apiext.KoordBatchMemory is deprecated: because of the limitation of extended resource naming
	return corev1.ResourceList{
		apiext.KoordBatchCPU:    *resource.NewQuantity(milliCPU, resource.DecimalSI),
		apiext.KoordBatchMemory: *resource.NewQuantity(memory, resource.BinarySI),
	}
}

func newContainerBatchRes(milliCPU, memory int64) corev1.ResourceList {
	return corev1.ResourceList{
		apiext.BatchCPU:    *resource.NewQuantity(milliCPU, resource.DecimalSI),
		apiext.BatchMemory: *resource.NewQuantity(memory, resource.BinarySI),
	}
}

func newKoordBatchPod(milliCPU, memory int64) *corev1.Pod {
	return &corev1.Pod{
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: newContainerKoordBatchRes(milliCPU, memory),
					},
				},
			},
		},
	}
}

func newBatchPod(milliCPU, memory int64) *corev1.Pod {
	return &corev1.Pod{
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Resources: corev1.ResourceRequirements{
						Requests: newContainerBatchRes(milliCPU, memory),
					},
				},
			},
		},
	}
}

func newNodeBatchRes(koordMilliCPU, koordMemory, milliCPU, memory *int64) *framework.Resource {
	result := &framework.Resource{
		ScalarResources: map[corev1.ResourceName]int64{},
	}
	if koordMilliCPU != nil {
		// nolint:staticcheck // SA1019: apiext.KoordBatchCPU is deprecated: because of the limitation of extended resource naming
		result.ScalarResources[apiext.KoordBatchCPU] = *koordMilliCPU
	}
	if koordMemory != nil {
		// nolint:staticcheck // SA1019: apiext.KoordBatchMemory is deprecated: because of the limitation of extended resource naming
		result.ScalarResources[apiext.KoordBatchMemory] = *koordMemory
	}
	if milliCPU != nil {
		result.ScalarResources[apiext.BatchCPU] = *milliCPU
	}
	if memory != nil {
		result.ScalarResources[apiext.BatchMemory] = *memory
	}
	return result
}

func TestPlugin_Filter(t *testing.T) {
	type args struct {
		pod      *corev1.Pod
		nodeInfo *framework.NodeInfo
	}
	tests := []struct {
		name string
		args args
		want *framework.Status
	}{
		{
			name: "success with none batch pod",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Resources: corev1.ResourceRequirements{
									Requests: corev1.ResourceList{
										corev1.ResourceCPU:    *resource.NewQuantity(1, resource.DecimalSI),
										corev1.ResourceMemory: *resource.NewQuantity(1025, resource.BinarySI),
									},
								},
							},
						},
					},
				},
				nodeInfo: &framework.NodeInfo{
					Requested:   newNodeBatchRes(pointer.Int64(2000), pointer.Int64(2048), pointer.Int64(2000), pointer.Int64(2048)),
					Allocatable: newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(4000), pointer.Int64(4096)),
				},
			},
			want: nil,
		},
		{
			// NodeAllocatable: (4000, 4096)
			// NodeRequested: (1000+2000, 1024+2048)
			// Pod: (1000, 1024)
			name: "success with old batch pod",
			args: args{
				pod: newKoordBatchPod(1000, 1024),
				nodeInfo: &framework.NodeInfo{
					Requested:   newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(2000), pointer.Int64(2048)),
					Allocatable: newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(4000), pointer.Int64(4096)),
				},
			},
			want: nil,
		},
		{
			// NodeAllocatable: (4000, 4096)
			// NodeRequested: (1000+2000, 1024+2048)
			// Pod: (1000, 1024)
			name: "success with new batch pod",
			args: args{
				pod: newBatchPod(1000, 1024),
				nodeInfo: &framework.NodeInfo{
					Requested:   newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(2000), pointer.Int64(2048)),
					Allocatable: newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(4000), pointer.Int64(4096)),
				},
			},
			want: nil,
		},
		{
			// NodeAllocatable: (4000, 4096)
			// NodeRequested: (1000+2000, 1024+2048)
			// Pod: (1001, 1024)
			name: "failed with new batch pod because of cpu not enough",
			args: args{
				pod: newBatchPod(1001, 1024),
				nodeInfo: &framework.NodeInfo{
					Requested:   newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(2000), pointer.Int64(2048)),
					Allocatable: newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(4000), pointer.Int64(4096)),
				},
			},
			want: framework.NewStatus(framework.Unschedulable, "Insufficient batch cpu"),
		},
		{
			// NodeAllocatable: (4000, 4096)
			// NodeRequested: (1000+2000, 1024+2048)
			// Pod: (1000, 1024)
			name: "failed with new batch pod because of memory not enough",
			args: args{
				pod: newBatchPod(1000, 1025),
				nodeInfo: &framework.NodeInfo{
					Requested:   newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(2000), pointer.Int64(2048)),
					Allocatable: newNodeBatchRes(pointer.Int64(1000), pointer.Int64(1024), pointer.Int64(4000), pointer.Int64(4096)),
				},
			},
			want: framework.NewStatus(framework.Unschedulable, "Insufficient batch memory"),
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &Plugin{}
			if got := p.Filter(context.TODO(), nil, tt.args.pod, tt.args.nodeInfo); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("Filter() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_computePodBatchRequest(t *testing.T) {
	type args struct {
		pod *corev1.Pod
	}
	tests := []struct {
		name string
		args args
		want *batchResource
	}{
		{
			name: "old pod with koord batch resource",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Resources: corev1.ResourceRequirements{
									Requests: newContainerKoordBatchRes(1000, 1024),
								},
							},
						},
						InitContainers: []corev1.Container{
							{
								Resources: corev1.ResourceRequirements{
									Requests: newContainerKoordBatchRes(2000, 2048),
								},
							},
						},
						Overhead: newContainerKoordBatchRes(2000, 2048),
					},
				},
			},
			want: &batchResource{
				MilliCPU: 4000,
				Memory:   4096,
			},
		},
		{
			name: "new pod with batch resource",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Resources: corev1.ResourceRequirements{
									Requests: newContainerBatchRes(1000, 1024),
								},
							},
						},
					},
				},
			},
			want: &batchResource{
				MilliCPU: 1000,
				Memory:   1024,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := computePodBatchRequest(tt.args.pod); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("computePodBatchRequest() = %v, want %v", got, tt.want)
			}
		})
	}
}
