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

package nodenumaresource

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/uuid"

	"github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/util/cpuset"
)

func TestPodEventHandler(t *testing.T) {
	tests := []struct {
		name    string
		pod     *corev1.Pod
		wantAdd bool
		want    cpuset.CPUSet
	}{
		{
			name: "pending pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:  uuid.NewUUID(),
					Name: "test",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodPending,
				},
			},
		},
		{
			name: "scheduled CPU Shared Pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:  uuid.NewUUID(),
					Name: "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node-1",
				},
			},
		},
		{
			name: "terminated Pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:  uuid.NewUUID(),
					Name: "test",
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node-1",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodSucceeded,
				},
			},
		},
		{
			name: "running LSR Pod",
			pod: &corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					UID:  uuid.NewUUID(),
					Name: "test",
					Labels: map[string]string{
						extension.LabelPodQoS: string(extension.QoSLSR),
					},
					Annotations: map[string]string{
						extension.AnnotationResourceSpec:   `{"preferredCPUBindPolicy": "FullPCPUs"}`,
						extension.AnnotationResourceStatus: `{"cpuset": "0-3"}`,
					},
				},
				Spec: corev1.PodSpec{
					NodeName: "test-node-1",
				},
				Status: corev1.PodStatus{
					Phase: corev1.PodRunning,
				},
			},
			wantAdd: true,
			want:    cpuset.MustParse("0-3"),
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			cpuTopology := buildCPUTopologyForTest(2, 2, 4, 2)
			topologyManager := NewCPUTopologyManager()
			topologyManager.UpdateCPUTopologyOptions("test-node-1", func(options *CPUTopologyOptions) {
				options.CPUTopology = cpuTopology
			})
			cpuManager := &cpuManagerImpl{
				topologyManager:  topologyManager,
				allocationStates: map[string]*cpuAllocation{},
			}
			handler := &podEventHandler{
				cpuManager: cpuManager,
			}
			handler.OnAdd(tt.pod)
			handler.OnUpdate(tt.pod, tt.pod)

			allocation := cpuManager.getOrCreateAllocation("test-node-1")
			_, ok := allocation.allocatedPods[tt.pod.UID]
			if tt.wantAdd && !ok {
				t.Errorf("expect add the Pod but not found")
			} else if !tt.wantAdd && ok {
				t.Errorf("expect not add the Pod but found")
			}

			cpusetBuilder := cpuset.NewCPUSetBuilder()
			for _, v := range allocation.allocatedCPUs {
				cpusetBuilder.Add(v.CPUID)
			}
			cpuset := cpusetBuilder.Result()
			if tt.want.IsEmpty() && !cpuset.IsEmpty() {
				t.Errorf("expect empty cpuset but got")
			} else if !tt.want.IsEmpty() && cpuset.IsEmpty() {
				t.Errorf("expect cpuset but got empty")
			} else if !tt.want.Equals(cpuset) {
				t.Errorf("expect cpuset equal, but failed, expect: %v, got: %v", tt.want, cpuset)
			}
			cpuset = allocation.allocatedPods[tt.pod.UID]
			if tt.want.IsEmpty() && !cpuset.IsEmpty() {
				t.Errorf("expect empty cpuset but got")
			} else if !tt.want.IsEmpty() && cpuset.IsEmpty() {
				t.Errorf("expect cpuset but got empty")
			} else if !tt.want.Equals(cpuset) {
				t.Errorf("expect cpuset equal, but failed, expect: %v, got: %v", tt.want, cpuset)
			}

			handler.OnDelete(tt.pod)
			assert.Empty(t, allocation.allocatedPods)
			assert.Empty(t, allocation.allocatedCPUs)
		})
	}

}
