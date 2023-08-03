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

package estimator

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/apis/config/v1beta2"
)

func TestDefaultEstimator(t *testing.T) {
	tests := []struct {
		name          string
		pod           *corev1.Pod
		scalarFactors map[corev1.ResourceName]int64
		want          map[corev1.ResourceName]int64
	}{
		{
			name: "estimate empty pod",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name:      "main",
							Resources: corev1.ResourceRequirements{},
						},
					},
				},
			},
			want: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    DefaultMilliCPURequest,
				corev1.ResourceMemory: DefaultMemoryRequest,
			},
		},
		{
			name: "estimate guaranteed pod",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Limits: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
								Requests: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
			},
			want: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    3400,
				corev1.ResourceMemory: 6012954214, // 5.6Gi
			},
		},
		{
			name: "estimate burstable pod",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Limits: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU:    resource.MustParse("8"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
								Requests: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
			},
			want: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    8000,
				corev1.ResourceMemory: 6012954214, // 5.6Gi
			},
		},
		{
			name: "estimate guaranteed pod and zoomed cpu factors",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Limits: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
								Requests: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
			},
			scalarFactors: map[corev1.ResourceName]int64{
				corev1.ResourceCPU: 110,
			},
			want: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    4000,
				corev1.ResourceMemory: 6012954214, // 5.6Gi
			},
		},
		{
			name: "estimate guaranteed pod and zoomed memory factors",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "main",
							Resources: corev1.ResourceRequirements{
								Limits: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
								Requests: map[corev1.ResourceName]resource.Quantity{
									corev1.ResourceCPU:    resource.MustParse("4"),
									corev1.ResourceMemory: resource.MustParse("8Gi"),
								},
							},
						},
					},
				},
			},
			scalarFactors: map[corev1.ResourceName]int64{
				corev1.ResourceMemory: 110,
			},
			want: map[corev1.ResourceName]int64{
				corev1.ResourceCPU:    3400,
				corev1.ResourceMemory: 8589934592, // 5.6Gi
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var v1beta2args v1beta2.LoadAwareSchedulingArgs
			v1beta2args.EstimatedScalingFactors = tt.scalarFactors
			v1beta2.SetDefaults_LoadAwareSchedulingArgs(&v1beta2args)
			var loadAwareSchedulingArgs config.LoadAwareSchedulingArgs
			err := v1beta2.Convert_v1beta2_LoadAwareSchedulingArgs_To_config_LoadAwareSchedulingArgs(&v1beta2args, &loadAwareSchedulingArgs, nil)
			assert.NoError(t, err)
			estimator, err := NewDefaultEstimator(&loadAwareSchedulingArgs, nil)
			assert.NoError(t, err)
			assert.NotNil(t, estimator)
			assert.Equal(t, defaultEstimatorName, estimator.Name())

			got, err := estimator.Estimate(tt.pod)
			assert.NoError(t, err)
			assert.Equal(t, tt.want, got)
		})
	}
}
