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

package mutating

import (
	"context"
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"
	admissionv1 "k8s.io/api/admission/v1"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

func TestExtendedResourceSpecMutatingPod(t *testing.T) {
	assert := assert.New(t)

	client := fake.NewClientBuilder().Build()
	decoder, _ := admission.NewDecoder(scheme.Scheme)
	handler := &PodMutatingHandler{
		Client:  client,
		Decoder: decoder,
	}
	pod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
			Labels: map[string]string{
				"koordinator-colocation-pod": "true",
				extension.LabelPodQoS:        string(extension.QoSBE),
				extension.LabelPodPriority:   "1111",
			},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "test-container-a",
					Resources: corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							extension.BatchCPU:    resource.MustParse("1000"),
							extension.BatchMemory: resource.MustParse("4Gi"),
							"unknown-resource":    resource.MustParse("1"),
						},
						Requests: corev1.ResourceList{
							extension.BatchCPU:    resource.MustParse("1000"),
							extension.BatchMemory: resource.MustParse("4Gi"),
							"unknown-resource":    resource.MustParse("1"),
						},
					},
				},
			},
			SchedulerName:     "koordinator-scheduler",
			Priority:          pointer.Int32Ptr(extension.PriorityBatchValueMax),
			PriorityClassName: "koordinator-batch",
		},
	}

	testExtendedResourceSpec := extension.ExtendedResourceSpec{
		Containers: map[string]extension.ExtendedResourceContainerSpec{
			"test-container-a": {
				Limits: corev1.ResourceList{
					extension.BatchCPU:    resource.MustParse("1000"),
					extension.BatchMemory: resource.MustParse("4Gi"),
				},
				Requests: corev1.ResourceList{
					extension.BatchCPU:    resource.MustParse("1000"),
					extension.BatchMemory: resource.MustParse("4Gi"),
				},
			},
		},
	}
	testExtendedResourceSpecBytes, err := json.Marshal(testExtendedResourceSpec)
	assert.NoError(err)

	req := newAdmission(admissionv1.Create, runtime.RawExtension{}, runtime.RawExtension{}, "")
	err = handler.extendedResourceSpecMutatingPod(context.TODO(), req, pod)
	assert.NoError(err)

	expectPod := &corev1.Pod{
		ObjectMeta: metav1.ObjectMeta{
			Namespace: "default",
			Name:      "test-pod-1",
			Labels: map[string]string{
				"koordinator-colocation-pod": "true",
				extension.LabelPodQoS:        string(extension.QoSBE),
				extension.LabelPodPriority:   "1111",
			},
			Annotations: map[string]string{
				extension.AnnotationExtendedResourceSpec: string(testExtendedResourceSpecBytes),
			},
		},
		Spec: corev1.PodSpec{
			Containers: []corev1.Container{
				{
					Name: "test-container-a",
					Resources: corev1.ResourceRequirements{
						Limits: corev1.ResourceList{
							extension.BatchCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
							extension.BatchMemory: resource.MustParse("4Gi"),
							"unknown-resource":    resource.MustParse("1"),
						},
						Requests: corev1.ResourceList{
							extension.BatchCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
							extension.BatchMemory: resource.MustParse("4Gi"),
							"unknown-resource":    resource.MustParse("1"),
						},
					},
				},
			},
			SchedulerName:     "koordinator-scheduler",
			Priority:          pointer.Int32Ptr(extension.PriorityBatchValueMax),
			PriorityClassName: "koordinator-batch",
		},
	}
	assert.Equal(expectPod, pod)
}
