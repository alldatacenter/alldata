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

package util

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

func Test_GetContainerXXXValue(t *testing.T) {
	type args struct {
		name        string
		fn          func(container *corev1.Container) int64
		container   *corev1.Container
		expectValue int64
	}

	tests := []args{
		{
			name: "test_GetContainerBaseCFSQuota",
			fn:   GetContainerMilliCPULimit,
			container: &corev1.Container{
				Resources: corev1.ResourceRequirements{
					Limits: corev1.ResourceList(
						map[corev1.ResourceName]resource.Quantity{
							corev1.ResourceCPU: *resource.NewMilliQuantity(1, resource.DecimalSI),
						}),
				},
			},
			expectValue: 1,
		},
		{
			name:        "test_GetContainerBaseCFSQuota_invalid",
			fn:          GetContainerMilliCPULimit,
			container:   &corev1.Container{},
			expectValue: -1,
		},
		{
			name: "test_GetContainerMemoryByteLimit",
			fn:   GetContainerMemoryByteLimit,
			container: &corev1.Container{
				Resources: corev1.ResourceRequirements{
					Limits: corev1.ResourceList(
						map[corev1.ResourceName]resource.Quantity{
							corev1.ResourceMemory: *resource.NewQuantity(1, resource.BinarySI),
						}),
				},
			},
			expectValue: 1,
		},
		{
			name:        "test_GetContainerMemoryByteLimit_invalid",
			fn:          GetContainerMemoryByteLimit,
			container:   &corev1.Container{},
			expectValue: -1,
		},
		{
			name: "test_GetContainerBEMilliCPURequest",
			fn:   GetContainerBatchMilliCPURequest,
			container: &corev1.Container{
				Resources: corev1.ResourceRequirements{
					Requests: corev1.ResourceList(
						map[corev1.ResourceName]resource.Quantity{
							extension.BatchCPU: *resource.NewMilliQuantity(1, resource.DecimalSI),
						}),
				},
			},
			expectValue: 1,
		},
		{
			name:        "test_GetContainerBEMilliCPURequest_invalid",
			fn:          GetContainerBatchMilliCPURequest,
			container:   &corev1.Container{},
			expectValue: -1,
		},
		{
			name: "test_GetContainerBEMilliCPULimit",
			fn:   GetContainerBatchMilliCPULimit,
			container: &corev1.Container{
				Resources: corev1.ResourceRequirements{
					Limits: corev1.ResourceList(
						map[corev1.ResourceName]resource.Quantity{
							extension.BatchCPU: *resource.NewMilliQuantity(1, resource.DecimalSI),
						}),
				},
			},
			expectValue: 1,
		},
		{
			name:        "test_GetContainerBEMilliCPULimit_invalid",
			fn:          GetContainerBatchMilliCPULimit,
			container:   &corev1.Container{},
			expectValue: -1,
		},
		{
			name: "test_GetContainerBEMemoryByteRequest",
			fn:   GetContainerBatchMemoryByteRequest,
			container: &corev1.Container{
				Resources: corev1.ResourceRequirements{
					Requests: corev1.ResourceList(
						map[corev1.ResourceName]resource.Quantity{
							extension.BatchMemory: *resource.NewQuantity(1, resource.BinarySI),
						}),
				},
			},
			expectValue: 1,
		},
		{
			name:        "test_GetContainerBEMemoryByteRequest_invalid",
			fn:          GetContainerBatchMemoryByteRequest,
			container:   &corev1.Container{},
			expectValue: -1,
		},
		{
			name: "test_GetContainerBEMemoryByteLimit",
			fn:   GetContainerBatchMemoryByteLimit,
			container: &corev1.Container{
				Resources: corev1.ResourceRequirements{
					Limits: corev1.ResourceList(
						map[corev1.ResourceName]resource.Quantity{
							extension.BatchMemory: *resource.NewQuantity(1, resource.BinarySI),
						}),
				},
			},
			expectValue: 1,
		},
		{
			name:        "test_GetContainerBEMemoryByteLimit_invalid",
			fn:          GetContainerBatchMemoryByteLimit,
			container:   &corev1.Container{},
			expectValue: -1,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			gotValue := tt.fn(tt.container)
			assert.Equal(t, tt.expectValue, gotValue, "checkValue")
		})
	}
}

func TestGetBatchXXXFromResourceList(t *testing.T) {
	tests := []struct {
		name string
		fn   func(r corev1.ResourceList) int64
		arg  corev1.ResourceList
		want int64
	}{
		{
			name: "test GetBatchMilliCPUFromResourceList",
			fn:   GetBatchMilliCPUFromResourceList,
			arg: corev1.ResourceList{
				extension.BatchCPU:    resource.MustParse("1000"),
				extension.BatchMemory: resource.MustParse("2Gi"),
			},
			want: 1000,
		},
		{
			name: "test GetBatchMilliCPUFromResourceList 1",
			fn:   GetBatchMilliCPUFromResourceList,
			arg: corev1.ResourceList{
				extension.BatchMemory: resource.MustParse("2Gi"),
			},
			want: -1,
		},
		{
			name: "test GetBatchMemoryFromResourceList",
			fn:   GetBatchMemoryFromResourceList,
			arg: corev1.ResourceList{
				extension.BatchMemory: resource.MustParse("2Gi"),
			},
			want: 2 * (1 << 30),
		},
		{
			name: "test GetBatchMemoryFromResourceList 1",
			fn:   GetBatchMemoryFromResourceList,
			arg:  corev1.ResourceList{},
			want: -1,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := tt.fn(tt.arg)
			assert.Equal(t, tt.want, got)
		})
	}
}
