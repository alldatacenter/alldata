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
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/kubernetes/pkg/scheduler/framework"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
	"github.com/koordinator-sh/koordinator/pkg/scheduler/frameworkext"
)

func TestNodeInfoFilterTransformer(t *testing.T) {
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

	transformer := NewNodeInfoFilterTransformer()
	_, transformedNodeInfo, transformed := transformer.(frameworkext.FilterTransformer).BeforeFilter(nil, nil, nil, nodeInfo)
	assert.True(t, transformed)

	assert.Equal(t, originalNodeInfo, nodeInfo)
	expectedNodeInfo := nodeInfo.Clone()
	testTransformer(expectedNodeInfo)
	assert.Equal(t, expectedNodeInfo, transformedNodeInfo)
}
