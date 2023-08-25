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

package sharedlisterext

import (
	"strconv"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

func testTransformer(nodeInfo *framework.NodeInfo) {
	for k, v := range nodeInfo.Allocatable.ScalarResources {
		nodeInfo.Allocatable.ScalarResources[k] = v * 2
	}
}

func setupTestNodeInfoTransformer() func() {
	originalTransformers := nodeInfoTransformerFns
	RegisterNodeInfoTransformer(testTransformer)
	return func() {
		nodeInfoTransformerFns = originalTransformers
	}
}

func BenchmarkNodeInfoCloneWithTransformer(b *testing.B) {
	var pods []*corev1.Pod
	for i := 1; i <= 60; i++ {
		pods = append(
			pods,
			&corev1.Pod{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "default",
					Name:      "test" + strconv.Itoa(i),
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
												Values:   []string{"false"},
											},
											{
												Key:      "dummy-label",
												Operator: corev1.NodeSelectorOpIn,
												Values:   []string{"test"},
											},
										},
									},
								},
							},
						},
						PodAntiAffinity: &corev1.PodAntiAffinity{
							RequiredDuringSchedulingIgnoredDuringExecution: []corev1.PodAffinityTerm{
								{
									LabelSelector: &metav1.LabelSelector{
										MatchLabels: map[string]string{
											"test": "true",
										},
									},
								},
							},
						},
					},
					Containers: []corev1.Container{
						{
							Resources: corev1.ResourceRequirements{
								Requests: corev1.ResourceList{
									corev1.ResourceCPU:              resource.MustParse("1"),
									corev1.ResourceMemory:           resource.MustParse("1Gi"),
									corev1.ResourceEphemeralStorage: resource.MustParse("1Gi"),
									apiext.BatchCPU:                 resource.MustParse("1"),
									apiext.BatchMemory:              resource.MustParse("1Gi"),
								},
							},
							Ports: []corev1.ContainerPort{
								{
									HostPort:      int32(i) + 8000,
									ContainerPort: int32(i) + 8000,
								},
								{
									HostPort:      int32(i) + 9000,
									ContainerPort: int32(i) + 9000,
								},
							},
						},
					},
				},
			},
		)
	}
	nodeInfo := framework.NewNodeInfo(pods...)
	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test",
			Labels: map[string]string{
				"test": "true",
			},
		},
		Status: corev1.NodeStatus{
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:              resource.MustParse("1000"),
				corev1.ResourceMemory:           resource.MustParse("1000Gi"),
				corev1.ResourceEphemeralStorage: resource.MustParse("1000Gi"),
				corev1.ResourcePods:             resource.MustParse("110"),
				apiext.BatchCPU:                 resource.MustParse("1"),
				apiext.BatchMemory:              resource.MustParse("1Gi"),
			},
		},
	}
	nodeInfo.SetNode(node)

	defer setupTestNodeInfoTransformer()()

	for i := 0; i < b.N; i++ {
		n := nodeInfo.Clone()
		TransformOneNodeInfo(n)
	}
}

func TestTransformNodeInfos(t *testing.T) {
	defer setupTestNodeInfoTransformer()()

	node := &corev1.Node{
		ObjectMeta: metav1.ObjectMeta{
			Name: "test",
			Labels: map[string]string{
				"test": "true",
			},
		},
		Status: corev1.NodeStatus{
			Allocatable: corev1.ResourceList{
				corev1.ResourceCPU:              resource.MustParse("1000"),
				corev1.ResourceMemory:           resource.MustParse("1000Gi"),
				corev1.ResourceEphemeralStorage: resource.MustParse("1000Gi"),
				corev1.ResourcePods:             resource.MustParse("110"),
				apiext.BatchCPU:                 resource.MustParse("1"),
				apiext.BatchMemory:              resource.MustParse("1Gi"),
			},
		},
	}
	nodeInfo := framework.NewNodeInfo()
	nodeInfo.SetNode(node)

	originalNodeInfo := nodeInfo.Clone()
	transformedNodeInfos := TransformNodeInfos([]*framework.NodeInfo{nodeInfo})

	assert.Equal(t, originalNodeInfo, nodeInfo)

	clonedNodeInfo := nodeInfo.Clone()
	testTransformer(clonedNodeInfo)
	expectedNodeInfos := []*framework.NodeInfo{
		clonedNodeInfo,
	}
	assert.Equal(t, expectedNodeInfos, transformedNodeInfos)
}
