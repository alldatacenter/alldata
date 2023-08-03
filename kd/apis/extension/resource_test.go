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
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

func TestSetResourceStatus(t *testing.T) {
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{Name: "test-container-1"},
				{Name: "test-container-2"},
			},
		},
	}
	err := SetResourceStatus(pod, &ResourceStatus{
		CPUSet: "0-3",
	})
	assert.NoError(t, err)
	expectedResourceStatus := `{"cpuset":"0-3"}`
	assert.Equal(t, expectedResourceStatus, pod.Annotations[AnnotationResourceStatus])
}

func TestGetGetExtendedResourceSpec(t *testing.T) {
	testEmptySpec := &ExtendedResourceSpec{}
	testSpec := &ExtendedResourceSpec{
		Containers: map[string]ExtendedResourceContainerSpec{
			"test-container-1": {
				Requests: corev1.ResourceList{
					BatchCPU:    resource.MustParse("500"),
					BatchMemory: resource.MustParse("1Gi"),
				},
				Limits: corev1.ResourceList{
					BatchCPU:    resource.MustParse("500"),
					BatchMemory: resource.MustParse("1Gi"),
				},
			},
			"test-container-2": {
				Requests: corev1.ResourceList{
					BatchCPU:    resource.MustParse("500"),
					BatchMemory: resource.MustParse("2Gi"),
				},
				Limits: corev1.ResourceList{
					BatchCPU:    resource.MustParse("500"),
					BatchMemory: resource.MustParse("2Gi"),
				},
			},
		},
	}
	testBytes, err := json.Marshal(testSpec)
	assert.NoError(t, err)

	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
			Annotations: map[string]string{
				AnnotationExtendedResourceSpec: string(testBytes),
			},
		},
	}

	got, err := GetExtendedResourceSpec(pod.Annotations)
	assert.NoError(t, err)
	assert.Equal(t, testSpec, got)

	got, err = GetExtendedResourceSpec(nil)
	assert.NoError(t, err)
	assert.Equal(t, testEmptySpec, got)

	testInvalidAnnotations := map[string]string{
		AnnotationExtendedResourceSpec: "invalidValue",
	}
	got, err = GetExtendedResourceSpec(testInvalidAnnotations)
	assert.Error(t, err)
	assert.Nil(t, got)
}

func TestSetExtendedResourceSpec(t *testing.T) {
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "test-container-1",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							BatchCPU:    resource.MustParse("500"),
							BatchMemory: resource.MustParse("1Gi"),
						},
						Limits: corev1.ResourceList{
							BatchCPU:    resource.MustParse("500"),
							BatchMemory: resource.MustParse("1Gi"),
						},
					},
				},
				{
					Name: "test-container-2",
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							BatchCPU:    resource.MustParse("500"),
							BatchMemory: resource.MustParse("2Gi"),
						},
						Limits: corev1.ResourceList{
							BatchCPU:    resource.MustParse("500"),
							BatchMemory: resource.MustParse("2Gi"),
						},
					},
				},
			},
		},
	}
	testSpec := &ExtendedResourceSpec{
		Containers: map[string]ExtendedResourceContainerSpec{
			"test-container-1": {
				Requests: corev1.ResourceList{
					BatchCPU:    resource.MustParse("500"),
					BatchMemory: resource.MustParse("1Gi"),
				},
				Limits: corev1.ResourceList{
					BatchCPU:    resource.MustParse("500"),
					BatchMemory: resource.MustParse("1Gi"),
				},
			},
			"test-container-2": {
				Requests: corev1.ResourceList{
					BatchCPU:    resource.MustParse("500"),
					BatchMemory: resource.MustParse("2Gi"),
				},
				Limits: corev1.ResourceList{
					BatchCPU:    resource.MustParse("500"),
					BatchMemory: resource.MustParse("2Gi"),
				},
			},
		},
	}

	err := SetExtendedResourceSpec(pod, testSpec)
	assert.NoError(t, err)

	assert.NotNil(t, pod.Annotations)
	gotValue := pod.Annotations[AnnotationExtendedResourceSpec]
	gotSpec := &ExtendedResourceSpec{}
	err = json.Unmarshal([]byte(gotValue), gotSpec)
	assert.NoError(t, err)
	assert.Equal(t, testSpec, gotSpec)
}
