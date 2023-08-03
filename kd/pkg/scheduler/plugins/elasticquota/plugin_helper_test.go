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

package elasticquota

import (
	"context"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	schedulerv1alpha1 "sigs.k8s.io/scheduler-plugins/pkg/apis/scheduling/v1alpha1"
	schedulerclientsetfake "sigs.k8s.io/scheduler-plugins/pkg/generated/clientset/versioned/fake"
	schedulerinformers "sigs.k8s.io/scheduler-plugins/pkg/generated/informers/externalversions"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

func TestGetQuotaName(t *testing.T) {
	tests := []struct {
		name            string
		pod             *corev1.Pod
		elasticQuotas   []*schedulerv1alpha1.ElasticQuota
		expectQuotaName string
	}{
		{
			name: "default quota",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "test-ns",
					Name:      "test-pod",
				},
			},
			expectQuotaName: extension.DefaultQuotaName,
		},
		{
			name: "quota name from label",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "test-ns",
					Name:      "test-pod",
					Labels: map[string]string{
						extension.LabelQuotaName: "test",
					},
				},
			},
			expectQuotaName: "test",
		},
		{
			name: "quota name from namespace",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "test-ns",
					Name:      "test-pod",
				},
			},
			elasticQuotas: []*schedulerv1alpha1.ElasticQuota{
				MakeEQ("test-ns", "parent-quota").Annotations(map[string]string{extension.LabelQuotaIsParent: "true"}).Obj(),
				MakeEQ("test-ns", "test-ns").Obj(),
			},
			expectQuotaName: "test-ns",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			fakeClient := schedulerclientsetfake.NewSimpleClientset()
			for _, eq := range tt.elasticQuotas {
				_, err := fakeClient.SchedulingV1alpha1().ElasticQuotas(eq.Namespace).Create(context.TODO(), eq, metav1.CreateOptions{})
				assert.NoError(t, err)
			}
			informerFactory := schedulerinformers.NewSharedInformerFactory(fakeClient, 0)
			lister := informerFactory.Scheduling().V1alpha1().ElasticQuotas().Lister()
			informerFactory.Start(nil)
			informerFactory.WaitForCacheSync(nil)
			quotaName := GetQuotaName(tt.pod, lister)
			assert.Equal(t, tt.expectQuotaName, quotaName)
		})
	}
}
