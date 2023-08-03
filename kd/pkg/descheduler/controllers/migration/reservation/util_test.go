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

package reservation

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	sev1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
)

func TestCreateOrUpdateReservationOptions(t *testing.T) {
	ownerReferences := []metav1.OwnerReference{
		{
			Controller: pointer.Bool(true),
			Name:       "test",
		},
	}

	tests := []struct {
		name        string
		pod         *corev1.Pod
		wantOptions *sev1alpha1.PodMigrateReservationOptions
	}{
		{
			name: "skip current node",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					OwnerReferences: ownerReferences,
					Namespace:       "default",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
				},
			},
			wantOptions: &sev1alpha1.PodMigrateReservationOptions{
				Template: &sev1alpha1.ReservationTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{
						Labels: map[string]string{
							LabelCreatedBy: "koord-descheduler",
						},
					},
					Spec: sev1alpha1.ReservationSpec{
						AllocateOnce: true,
						Owners: []sev1alpha1.ReservationOwner{
							{
								Controller: &sev1alpha1.ReservationControllerReference{
									OwnerReference: ownerReferences[0],
									Namespace:      "default",
								},
							},
						},
						Template: &corev1.PodTemplateSpec{
							ObjectMeta: metav1.ObjectMeta{
								Namespace:       "default",
								OwnerReferences: ownerReferences,
							},
							Spec: corev1.PodSpec{
								Affinity: &corev1.Affinity{
									NodeAffinity: &corev1.NodeAffinity{
										RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
											NodeSelectorTerms: []corev1.NodeSelectorTerm{
												{
													MatchFields: []corev1.NodeSelectorRequirement{
														{
															Key:      "metadata.name",
															Operator: corev1.NodeSelectorOpNotIn,
															Values:   []string{"test-node"},
														},
													},
												},
											},
										},
									},
								},
							},
						},
					},
				},
			},
		},
		{
			name: "skip current node with existing node affinity",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					OwnerReferences: ownerReferences,
					Namespace:       "default",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node",
					Affinity: &corev1.Affinity{
						NodeAffinity: &corev1.NodeAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
								NodeSelectorTerms: []corev1.NodeSelectorTerm{
									{
										MatchExpressions: []corev1.NodeSelectorRequirement{
											{
												Key:      "test",
												Operator: corev1.NodeSelectorOpIn,
												Values:   []string{"xxxx"},
											},
										},
									},
								},
							},
						},
					},
				},
			},
			wantOptions: &sev1alpha1.PodMigrateReservationOptions{
				Template: &sev1alpha1.ReservationTemplateSpec{
					ObjectMeta: metav1.ObjectMeta{
						Labels: map[string]string{
							LabelCreatedBy: "koord-descheduler",
						},
					},
					Spec: sev1alpha1.ReservationSpec{
						AllocateOnce: true,
						Owners: []sev1alpha1.ReservationOwner{
							{
								Controller: &sev1alpha1.ReservationControllerReference{
									OwnerReference: ownerReferences[0],
									Namespace:      "default",
								},
							},
						},
						Template: &corev1.PodTemplateSpec{
							ObjectMeta: metav1.ObjectMeta{
								Namespace:       "default",
								OwnerReferences: ownerReferences,
							},
							Spec: corev1.PodSpec{
								Affinity: &corev1.Affinity{
									NodeAffinity: &corev1.NodeAffinity{
										RequiredDuringSchedulingIgnoredDuringExecution: &corev1.NodeSelector{
											NodeSelectorTerms: []corev1.NodeSelectorTerm{
												{
													MatchExpressions: []corev1.NodeSelectorRequirement{
														{
															Key:      "test",
															Operator: corev1.NodeSelectorOpIn,
															Values:   []string{"xxxx"},
														},
													},
													MatchFields: []corev1.NodeSelectorRequirement{
														{
															Key:      "metadata.name",
															Operator: corev1.NodeSelectorOpNotIn,
															Values:   []string{"test-node"},
														},
													},
												},
											},
										},
									},
								},
							},
						},
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			job := &sev1alpha1.PodMigrationJob{}
			got := CreateOrUpdateReservationOptions(job, tt.pod)
			assert.True(t, got.Template.Labels[apiext.LabelReservationOrder] != "")
			delete(got.Template.Labels, apiext.LabelReservationOrder)
			assert.Equal(t, tt.wantOptions, got)
		})
	}
}
