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
	"testing"

	"github.com/stretchr/testify/assert"
	admissionv1 "k8s.io/api/admission/v1"
	corev1 "k8s.io/api/core/v1"
	schedulingv1 "k8s.io/api/scheduling/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/utils/pointer"
	"sigs.k8s.io/controller-runtime/pkg/client/fake"
	"sigs.k8s.io/controller-runtime/pkg/webhook/admission"

	configv1alpha1 "github.com/koordinator-sh/koordinator/apis/config/v1alpha1"
	"github.com/koordinator-sh/koordinator/apis/extension"
)

func init() {
	_ = configv1alpha1.AddToScheme(scheme.Scheme)
}

func newAdmission(op admissionv1.Operation, object, oldObject runtime.RawExtension, subResource string) admission.Request {
	return admission.Request{
		AdmissionRequest: newAdmissionRequest(op, object, oldObject, subResource),
	}
}

func newAdmissionRequest(op admissionv1.Operation, object, oldObject runtime.RawExtension, subResource string) admissionv1.AdmissionRequest {
	return admissionv1.AdmissionRequest{
		Resource:    metav1.GroupVersionResource{Group: corev1.SchemeGroupVersion.Group, Version: corev1.SchemeGroupVersion.Version, Resource: "pods"},
		Operation:   op,
		Object:      object,
		OldObject:   oldObject,
		SubResource: subResource,
	}
}

func TestClusterColocationProfileMutatingPod(t *testing.T) {
	assert := assert.New(t)

	client := fake.NewClientBuilder().Build()
	decoder, _ := admission.NewDecoder(scheme.Scheme)
	handler := &PodMutatingHandler{
		Client:  client,
		Decoder: decoder,
	}

	namespaceObj := &corev1.Namespace{
		ObjectMeta: metav1.ObjectMeta{
			Name: "default",
			Labels: map[string]string{
				"enable-koordinator-colocation": "true",
			},
		},
	}
	err := client.Create(context.TODO(), namespaceObj)
	assert.NoError(err)

	batchPriorityClass := &schedulingv1.PriorityClass{
		ObjectMeta: metav1.ObjectMeta{
			Name: "koordinator-batch",
		},
		Value: extension.PriorityBatchValueMax,
	}
	err = client.Create(context.TODO(), batchPriorityClass)
	assert.NoError(err)

	profile := &configv1alpha1.ClusterColocationProfile{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test-profile",
		},
		Spec: configv1alpha1.ClusterColocationProfileSpec{
			NamespaceSelector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"enable-koordinator-colocation": "true",
				},
			},
			Selector: &metav1.LabelSelector{
				MatchLabels: map[string]string{
					"koordinator-colocation-pod": "true",
				},
			},
			Labels: map[string]string{
				"testLabelA": "valueA",
			},
			Annotations: map[string]string{
				"testAnnotationA": "valueA",
			},
			SchedulerName:       "koordinator-scheduler",
			QoSClass:            string(extension.QoSBE),
			PriorityClassName:   "koordinator-batch",
			KoordinatorPriority: pointer.Int32(1111),
			Patch: runtime.RawExtension{
				Raw: []byte(`{"metadata":{"labels":{"test-patch-label":"patch-a"},"annotations":{"test-patch-annotation":"patch-b"}}}`),
			},
		},
	}
	err = client.Create(context.TODO(), profile)
	assert.NoError(err)

	testCases := []struct {
		name     string
		pod      *corev1.Pod
		expected *corev1.Pod
	}{
		{
			name: "mutating pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
					Labels: map[string]string{
						"koordinator-colocation-pod": "true",
					},
				},
				Spec: corev1.PodSpec{
					InitContainers: []corev1.Container{
						{
							Name: "test-init-container-a",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("500m"),
									corev1.ResourceMemory: resource.MustParse("1Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("500m"),
									corev1.ResourceMemory: resource.MustParse("1Gi"),
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("1"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("1"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
					Overhead: map[corev1.ResourceName]resource.Quantity{
						corev1.ResourceCPU:    resource.MustParse("1"),
						corev1.ResourceMemory: resource.MustParse("2Gi"),
					},
					SchedulerName: "nonExistSchedulerName",
					Priority:      pointer.Int32Ptr(extension.PriorityBatchValueMax),
				},
			},
			expected: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
					Labels: map[string]string{
						"koordinator-colocation-pod": "true",
						"testLabelA":                 "valueA",
						"test-patch-label":           "patch-a",
						extension.LabelPodQoS:        string(extension.QoSBE),
						extension.LabelPodPriority:   "1111",
					},
					Annotations: map[string]string{
						"testAnnotationA":       "valueA",
						"test-patch-annotation": "patch-b",
					},
				},
				Spec: corev1.PodSpec{
					InitContainers: []corev1.Container{
						{
							Name: "test-init-container-a",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									extension.BatchCPU:    *resource.NewQuantity(500, resource.DecimalSI),
									extension.BatchMemory: resource.MustParse("1Gi"),
								},
								Requests: corev1.ResourceList{
									extension.BatchCPU:    *resource.NewQuantity(500, resource.DecimalSI),
									extension.BatchMemory: resource.MustParse("1Gi"),
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									extension.BatchCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
									extension.BatchMemory: resource.MustParse("4Gi"),
								},
								Requests: corev1.ResourceList{
									extension.BatchCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
									extension.BatchMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
					Overhead: map[corev1.ResourceName]resource.Quantity{
						extension.BatchCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
						extension.BatchMemory: resource.MustParse("2Gi"),
					},
					SchedulerName:     "koordinator-scheduler",
					Priority:          pointer.Int32Ptr(extension.PriorityBatchValueMax),
					PriorityClassName: "koordinator-batch",
				},
			},
		},
		{
			name: "set default request to the limit",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
					Labels: map[string]string{
						"koordinator-colocation-pod": "true",
					},
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Limits: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("1"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
					SchedulerName: "nonExistSchedulerName",
					Priority:      pointer.Int32Ptr(extension.PriorityBatchValueMax),
				},
			},
			expected: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
					Labels: map[string]string{
						"koordinator-colocation-pod": "true",
						"testLabelA":                 "valueA",
						"test-patch-label":           "patch-a",
						extension.LabelPodQoS:        string(extension.QoSBE),
						extension.LabelPodPriority:   "1111",
					},
					Annotations: map[string]string{
						"testAnnotationA":       "valueA",
						"test-patch-annotation": "patch-b",
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
								},
								Requests: corev1.ResourceList{
									extension.BatchCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
									extension.BatchMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
					SchedulerName:     "koordinator-scheduler",
					Priority:          pointer.Int32Ptr(extension.PriorityBatchValueMax),
					PriorityClassName: "koordinator-batch",
				},
			},
		},
		{
			name: "keep limit unset",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
					Labels: map[string]string{
						"koordinator-colocation-pod": "true",
					},
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:    resource.MustParse("1"),
									corev1.ResourceMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
					SchedulerName: "nonExistSchedulerName",
					Priority:      pointer.Int32Ptr(extension.PriorityBatchValueMax),
				},
			},
			expected: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test-pod-1",
					Labels: map[string]string{
						"koordinator-colocation-pod": "true",
						"testLabelA":                 "valueA",
						"test-patch-label":           "patch-a",
						extension.LabelPodQoS:        string(extension.QoSBE),
						extension.LabelPodPriority:   "1111",
					},
					Annotations: map[string]string{
						"testAnnotationA":       "valueA",
						"test-patch-annotation": "patch-b",
					},
				},
				Spec: corev1.PodSpec{
					Containers: []corev1.Container{
						{
							Name: "test-container-a",
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									extension.BatchCPU:    *resource.NewQuantity(1000, resource.DecimalSI),
									extension.BatchMemory: resource.MustParse("4Gi"),
								},
							},
						},
					},
					SchedulerName:     "koordinator-scheduler",
					Priority:          pointer.Int32Ptr(extension.PriorityBatchValueMax),
					PriorityClassName: "koordinator-batch",
				},
			},
		},
	}

	for _, tc := range testCases {
		req := newAdmission(admissionv1.Create, runtime.RawExtension{}, runtime.RawExtension{}, "")
		err = handler.clusterColocationProfileMutatingPod(context.TODO(), req, tc.pod)
		assert.NoError(err)

		assert.Equal(tc.expected, tc.pod)
	}
}
