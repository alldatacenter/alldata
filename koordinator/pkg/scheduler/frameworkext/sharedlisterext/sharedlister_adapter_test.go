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
	"fmt"
	"strconv"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

var _ framework.SharedLister = &testSharedLister{}

type testSharedLister struct {
	nodeInfos   []*framework.NodeInfo
	nodeInfoMap map[string]*framework.NodeInfo
}

func newTestSharedLister(nodeNum, podNumPerNode int) *testSharedLister {
	nodeInfoMap := make(map[string]*framework.NodeInfo)
	for i := 0; i < nodeNum; i++ {
		var pods []*corev1.Pod
		for j := 1; j <= podNumPerNode; j++ {
			pods = append(
				pods,
				&corev1.Pod{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "default",
						Name:      fmt.Sprintf("test-pod-%d-%d", i, j),
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
		nodeName := "test-node" + strconv.Itoa(i)
		node := &corev1.Node{
			ObjectMeta: metav1.ObjectMeta{
				Name: nodeName,
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
		nodeInfoMap[nodeName] = nodeInfo
	}

	nodeInfos := make([]*framework.NodeInfo, 0)
	for _, v := range nodeInfoMap {
		nodeInfos = append(nodeInfos, v)
	}

	return &testSharedLister{
		nodeInfos:   nodeInfos,
		nodeInfoMap: nodeInfoMap,
	}
}

func (f *testSharedLister) NodeInfos() framework.NodeInfoLister {
	return f
}

func (f *testSharedLister) List() ([]*framework.NodeInfo, error) {
	return f.nodeInfos, nil
}

func (f *testSharedLister) HavePodsWithAffinityList() ([]*framework.NodeInfo, error) {
	return f.nodeInfos, nil
}

func (f *testSharedLister) HavePodsWithRequiredAntiAffinityList() ([]*framework.NodeInfo, error) {
	return f.nodeInfos, nil
}

func (f *testSharedLister) Get(nodeName string) (*framework.NodeInfo, error) {
	return f.nodeInfoMap[nodeName], nil
}

func TestSharedListerAdapter_List(t *testing.T) {
	defer setupTestNodeInfoTransformer()()

	sharedLister := NewSharedListerAdapter(newTestSharedLister(100, 60))
	nodeInfos, err := sharedLister.NodeInfos().List()
	assert.NoError(t, err)
	assert.Len(t, nodeInfos, 100)
	for _, v := range nodeInfos {
		assert.Equal(t, int64(2), v.Allocatable.ScalarResources[apiext.BatchCPU])
		q := resource.MustParse("2Gi")
		assert.Equal(t, q.Value(), v.Allocatable.ScalarResources[apiext.BatchMemory])
	}
}

func TestSharedListerAdapter_HavePodsWithAffinityList(t *testing.T) {
	defer setupTestNodeInfoTransformer()()

	sharedLister := NewSharedListerAdapter(newTestSharedLister(100, 60))
	nodeInfos, err := sharedLister.NodeInfos().HavePodsWithAffinityList()
	assert.NoError(t, err)
	assert.Len(t, nodeInfos, 100)
	for _, v := range nodeInfos {
		assert.Equal(t, int64(2), v.Allocatable.ScalarResources[apiext.BatchCPU])
		q := resource.MustParse("2Gi")
		assert.Equal(t, q.Value(), v.Allocatable.ScalarResources[apiext.BatchMemory])
	}
}

func TestSharedListerAdapter_HavePodsWithRequiredAntiAffinityList(t *testing.T) {
	defer setupTestNodeInfoTransformer()()

	sharedLister := NewSharedListerAdapter(newTestSharedLister(100, 60))
	nodeInfos, err := sharedLister.NodeInfos().HavePodsWithRequiredAntiAffinityList()
	assert.NoError(t, err)
	assert.Len(t, nodeInfos, 100)
	for _, v := range nodeInfos {
		assert.Equal(t, int64(2), v.Allocatable.ScalarResources[apiext.BatchCPU])
		q := resource.MustParse("2Gi")
		assert.Equal(t, q.Value(), v.Allocatable.ScalarResources[apiext.BatchMemory])
	}
}

func TestSharedListerAdapter_Get(t *testing.T) {
	defer setupTestNodeInfoTransformer()()

	sharedLister := NewSharedListerAdapter(newTestSharedLister(100, 60))
	nodeInfo, err := sharedLister.NodeInfos().Get("test-node90")
	assert.NoError(t, err)
	assert.NotNil(t, nodeInfo)
	assert.Equal(t, int64(2), nodeInfo.Allocatable.ScalarResources[apiext.BatchCPU])
	q := resource.MustParse("2Gi")
	assert.Equal(t, q.Value(), nodeInfo.Allocatable.ScalarResources[apiext.BatchMemory])
}
