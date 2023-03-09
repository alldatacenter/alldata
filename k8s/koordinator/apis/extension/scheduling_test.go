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

package extension

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func Test_GetDeviceAllocations(t *testing.T) {
	tests := []struct {
		name    string
		pod     *corev1.Pod
		want    DeviceAllocations
		wantErr bool
	}{
		{
			name: "nil annotations",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
			},
		},
		{
			name: "incorrect annotations",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
					Annotations: map[string]string{
						AnnotationDeviceAllocated: "incorrect-device-allocation",
					},
				},
			},
			wantErr: true,
		},
		{
			name: "correct annotations",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
					Annotations: map[string]string{
						AnnotationDeviceAllocated: `{"gpu":[{"minor":1,"resources":{"kubernetes.io/gpu-core":"100","kubernetes.io/gpu-memory":"16Gi","kubernetes.io/gpu-memory-ratio":"100"}}]}`,
					},
				},
			},
			want: DeviceAllocations{
				schedulingv1alpha1.GPU: []*DeviceAllocation{
					{
						Minor: 1,
						Resources: corev1.ResourceList{
							GPUCore:        resource.MustParse("100"),
							GPUMemoryRatio: resource.MustParse("100"),
							GPUMemory:      resource.MustParse("16Gi"),
						},
					},
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := GetDeviceAllocations(tt.pod.Annotations)
			assert.Equal(t, tt.wantErr, err != nil)
			if !tt.wantErr {
				assert.Equal(t, tt.want, got)
			}
		})
	}
}

func Test_SetDeviceAllocations(t *testing.T) {
	tests := []struct {
		name           string
		pod            *corev1.Pod
		allocations    DeviceAllocations
		wantAnnotation string
		wantErr        bool
	}{
		{
			name: "valid allocations",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:       "123456789",
					Namespace: "default",
					Name:      "test",
				},
			},
			allocations: DeviceAllocations{
				schedulingv1alpha1.GPU: []*DeviceAllocation{
					{
						Minor: 1,
						Resources: corev1.ResourceList{
							GPUCore:        resource.MustParse("100"),
							GPUMemoryRatio: resource.MustParse("100"),
							GPUMemory:      resource.MustParse("16Gi"),
						},
					},
				},
			},
			wantAnnotation: `{"gpu":[{"minor":1,"resources":{"kubernetes.io/gpu-core":"100","kubernetes.io/gpu-memory":"16Gi","kubernetes.io/gpu-memory-ratio":"100"}}]}`,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := SetDeviceAllocations(tt.pod, tt.allocations)
			assert.Equal(t, tt.wantErr, err != nil)
			if !tt.wantErr {
				assert.Equal(t, tt.wantAnnotation, tt.pod.Annotations[AnnotationDeviceAllocated])
			}
		})
	}
}
