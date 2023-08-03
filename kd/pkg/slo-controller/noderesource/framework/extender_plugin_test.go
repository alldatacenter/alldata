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

package framework

import (
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

const (
	testNodeAnnoKey = "test-anno-key"
	testNodeAnnoVal = "test-anno-val"
)

type SetNodeAnnotation struct{}

func (s *SetNodeAnnotation) Name() string {
	return "SetNodeAnnotation"
}

func (s *SetNodeAnnotation) Execute(strategy *extension.ColocationStrategy, node *corev1.Node, nr *NodeResource) error {
	node.Annotations[testNodeAnnoKey] = testNodeAnnoVal
	return nil
}

func Test_NodePrepareExtender(t *testing.T) {
	t.Run("prepare extender", func(t *testing.T) {
		extender := &SetNodeAnnotation{}
		startedSize := globalNodePrepareExtender.Size()
		RegisterNodePrepareExtender(extender)
		assert.Equal(t, startedSize+1, globalNodePrepareExtender.Size())
		node := &corev1.Node{
			ObjectMeta: metav1.ObjectMeta{
				Annotations: map[string]string{},
			},
		}
		RunNodePrepareExtenders(nil, node, NewNodeResource())
		annoVal := node.Annotations["test-anno-key"]
		if annoVal != "test-anno-val" {
			t.Errorf("runNodePrepareExtenders got node anno %v, want %v", annoVal, testNodeAnnoVal)
		}
		UnregisterNodePrepareExtender(extender.Name())
	})
}

func Test_RegisterAlreadyExistNodePrepareExtender(t *testing.T) {
	t.Run("prepare extender", func(t *testing.T) {
		extender := &SetNodeAnnotation{}
		startedSize := globalNodePrepareExtender.Size()
		// register for the first time
		RegisterNodePrepareExtender(extender)
		assert.Equal(t, startedSize+1, globalNodePrepareExtender.Size())
		// register duplicated
		extender1 := &SetNodeAnnotation{}
		RegisterNodePrepareExtender(extender1)
		assert.Equal(t, startedSize+1, globalNodePrepareExtender.Size())
		UnregisterNodePrepareExtender(extender.Name())
		assert.Equal(t, startedSize, globalNodePrepareExtender.Size())
	})
}

var _ NodePreparePlugin = (*testNodeResourcePlugin)(nil)
var _ NodeSyncPlugin = (*testNodeResourcePlugin)(nil)
var _ ResourceCalculatePlugin = (*testNodeResourcePlugin)(nil)

type testNodeResourcePlugin struct{}

func (p *testNodeResourcePlugin) Name() string {
	return "testPlugin"
}

func (p *testNodeResourcePlugin) Execute(strategy *extension.ColocationStrategy, node *corev1.Node, nr *NodeResource) error {
	return nil
}

func (p *testNodeResourcePlugin) NeedSync(strategy *extension.ColocationStrategy, oldNode, newNode *corev1.Node) (bool, string) {
	return true, "always sync"
}

func (p *testNodeResourcePlugin) Reset(node *corev1.Node, msg string) []ResourceItem {
	return []ResourceItem{
		{
			Name:    "unknown",
			Reset:   true,
			Message: msg,
		},
	}
}

func (p *testNodeResourcePlugin) Calculate(strategy *extension.ColocationStrategy, node *corev1.Node, podList *corev1.PodList, metrics *ResourceMetrics) ([]ResourceItem, error) {
	return []ResourceItem{
		{
			Name:     "unknown",
			Quantity: resource.NewQuantity(0, resource.DecimalSI),
		},
	}, nil
}

func TestNodeSyncPlugin(t *testing.T) {
	t.Run("node sync extender", func(t *testing.T) {
		plugin := &testNodeResourcePlugin{}
		startedSize := globalNodeSyncExtender.Size()
		RegisterNodeSyncExtender(plugin)
		assert.Equal(t, startedSize+1, globalNodeSyncExtender.Size())

		RegisterNodeSyncExtender(plugin)
		assert.Equal(t, startedSize+1, globalNodeSyncExtender.Size(), "register duplicated")

		assert.NotPanics(t, func() {
			UnregisterNodeSyncExtender(plugin.Name())
		}, "unregistered")
	})
}

func TestResourceCalculatePlugin(t *testing.T) {
	t.Run("resource calculate extender", func(t *testing.T) {
		plugin := &testNodeResourcePlugin{}
		startedSize := globalResourceCalculateExtender.Size()
		RegisterResourceCalculateExtender(plugin)
		assert.Equal(t, startedSize+1, globalResourceCalculateExtender.Size())

		RegisterResourceCalculateExtender(plugin)
		assert.Equal(t, startedSize+1, globalResourceCalculateExtender.Size(), "register duplicated")

		assert.NotPanics(t, func() {
			UnregisterResourceCalculateExtender(plugin.Name())
		}, "unregistered")
	})
}
