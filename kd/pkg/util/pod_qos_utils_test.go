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

func Test_IsPodCfsQuotaNeedUnset(t *testing.T) {
	type args struct {
		podAnnotations map[string]string
		podAlloc       *apiext.ResourceStatus
	}
	tests := []struct {
		name    string
		args    args
		want    bool
		wantErr bool
	}{
		{
			name: "need unset for cpuset pod",
			args: args{
				podAnnotations: map[string]string{},
				podAlloc: &apiext.ResourceStatus{
					CPUSet: "2-4",
				},
			},
			want:    true,
			wantErr: false,
		},
		{
			name: "no need to unset for cpushare pod",
			args: args{
				podAnnotations: map[string]string{},
				podAlloc:       &apiext.ResourceStatus{},
			},
			want:    false,
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if tt.args.podAlloc != nil {
				podAllocJson := DumpJSON(tt.args.podAlloc)
				tt.args.podAnnotations[apiext.AnnotationResourceStatus] = podAllocJson
			}
			got, err := IsPodCfsQuotaNeedUnset(tt.args.podAnnotations)
			assert.Equal(t, tt.wantErr, err != nil)
			assert.Equal(t, tt.want, got)
		})
	}
}

func Test_GetKubeQosClass(t *testing.T) {
	assert := assert.New(t)

	testCases := []struct {
		name              string
		pod               *corev1.Pod
		wantPriorityClass corev1.PodQOSClass
	}{
		{
			name: "Guaranteed from status",
			pod: &corev1.Pod{
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSGuaranteed,
				},
			},
			wantPriorityClass: corev1.PodQOSGuaranteed,
		},
		{
			name: "Besteffort from status",
			pod: &corev1.Pod{
				Status: corev1.PodStatus{
					QOSClass: corev1.PodQOSBestEffort,
				},
			},
			wantPriorityClass: corev1.PodQOSBestEffort,
		},
		{
			name: "Besteffort from resource",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{},
				},
			},
			wantPriorityClass: corev1.PodQOSBestEffort,
		},
		{
			name: "Burstable from resource",
			pod: &corev1.Pod{
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU: resource.MustParse("4"),
								},
								Limits: corev1.ResourceList{
									corev1.ResourceCPU: resource.MustParse("8"),
								},
							},
						},
					},
				},
			},
			wantPriorityClass: corev1.PodQOSBurstable,
		},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			assert.Equal(tc.wantPriorityClass, GetKubeQosClass(tc.pod))
		})
	}
}
