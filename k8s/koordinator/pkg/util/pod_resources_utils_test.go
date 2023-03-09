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

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

func Test_GetPodRequest(t *testing.T) {
	type args struct {
		pod *corev1.Pod
	}
	tests := []struct {
		name string
		args args
		want corev1.ResourceList
	}{
		{
			name: "do not panic on an illegal-labeled pod",
			args: args{pod: &corev1.Pod{}},
			want: corev1.ResourceList{},
		},
		{
			name: "get correct pod request",
			args: args{
				pod: &corev1.Pod{
					Spec: corev1.PodSpec{
						Containers: []corev1.Container{
							{
								Resources: corev1.ResourceRequirements{
									Requests: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("4"),
										corev1.ResourceMemory: resource.MustParse("10Gi"),
									},
									Limits: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("4"),
										corev1.ResourceMemory: resource.MustParse("10Gi"),
									},
								},
							},
							{
								Resources: corev1.ResourceRequirements{
									Requests: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("4"),
										corev1.ResourceMemory: resource.MustParse("8Gi"),
									},
									Limits: corev1.ResourceList{
										corev1.ResourceCPU:    resource.MustParse("6"),
										corev1.ResourceMemory: resource.MustParse("12Gi"),
									},
								},
							},
						},
					},
				},
			},
			want: corev1.ResourceList{
				corev1.ResourceCPU:    resource.MustParse("8"),
				corev1.ResourceMemory: resource.MustParse("18Gi"),
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := GetPodRequest(tt.args.pod)
			if !got.Cpu().Equal(*tt.want.Cpu()) {
				t.Errorf("should get correct cpu request, want %v, got %v",
					tt.want.Cpu(), got.Cpu())
			}
			if !got.Memory().Equal(*tt.want.Memory()) {
				t.Errorf("should get correct memory request, want %v, got %v",
					tt.want.Memory(), got.Memory())
			}
		})
	}
}

func Test_GetPodBEMilliCPURequest(t *testing.T) {
	assert := assert.New(t)

	testCases := []struct {
		name        string
		pod         *corev1.Pod
		wantRequest int64
		wantLimit   int64
	}{
		{
			name: "one container",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.BatchCPU: resource.MustParse("2000"),
								},
								Limits: corev1.ResourceList{
									apiext.BatchCPU: resource.MustParse("4000"),
								},
							},
						},
					},
				},
			},
			wantRequest: 2000,
			wantLimit:   4000,
		},
		{
			name: "multiple container",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.BatchCPU: resource.MustParse("4000"),
								},
								Limits: corev1.ResourceList{
									apiext.BatchCPU: resource.MustParse("4000"),
								},
							},
						},
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.BatchCPU: resource.MustParse("2000"),
								},
								Limits: corev1.ResourceList{
									apiext.BatchCPU: resource.MustParse("4000"),
								},
							},
						},
					},
				},
			},
			wantRequest: 6000,
			wantLimit:   8000,
		},
		{
			name: "empty resource",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{},
						},
					},
				},
			},
			wantRequest: 0,
			wantLimit:   -1,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			assert.Equal(tc.wantRequest, GetPodBEMilliCPURequest(tc.pod))
			assert.Equal(tc.wantLimit, GetPodBEMilliCPULimit(tc.pod))
		})
	}
}

func Test_GetPodBEMemoryRequest(t *testing.T) {
	assert := assert.New(t)

	testCases := []struct {
		name        string
		pod         *corev1.Pod
		wantRequest int64
		wantLimit   int64
	}{
		{
			name: "one container",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.BatchMemory: resource.MustParse("2Mi"),
								},
								Limits: corev1.ResourceList{
									apiext.BatchMemory: resource.MustParse("4Mi"),
								},
							},
						},
					},
				},
			},
			wantRequest: 2097152,
			wantLimit:   4194304,
		},
		{
			name: "multiple container",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.BatchMemory: resource.MustParse("2Mi"),
								},
								Limits: corev1.ResourceList{
									apiext.BatchMemory: resource.MustParse("4Mi"),
								},
							},
						},
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									apiext.BatchMemory: resource.MustParse("2Mi"),
								},
								Limits: corev1.ResourceList{
									apiext.BatchMemory: resource.MustParse("2Mi"),
								},
							},
						},
					},
				},
			},
			wantRequest: 4194304,
			wantLimit:   6291456,
		},
		{
			name: "empty resource",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{},
						},
					},
				},
			},
			wantRequest: 0,
			wantLimit:   -1,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			assert.Equal(tc.wantRequest, GetPodBEMemoryByteRequestIgnoreUnlimited(tc.pod))
			assert.Equal(tc.wantLimit, GetPodBEMemoryByteLimit(tc.pod))
		})
	}
}
