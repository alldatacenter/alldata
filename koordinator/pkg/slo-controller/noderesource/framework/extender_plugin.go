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
	corev1 "k8s.io/api/core/v1"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/apis/extension"
)

var (
	globalNodePrepareExtender       = NewRegistry("NodePrepare")
	globalNodeSyncExtender          = NewRegistry("NodeSync")
	globalResourceCalculateExtender = NewRegistry("ResourceCalculate")
)

// Plugin has its name. Plugins in a registry are executed in order of the registration.
type Plugin interface {
	Name() string
}

// NodePreparePlugin implements node resource preparing for the calculated results.
// For example, assign extended resources in the node allocatable.
// It is invoked each time the controller tries updating the latest NodeResource object with calculated results.
type NodePreparePlugin interface {
	Plugin
	Execute(strategy *extension.ColocationStrategy, node *corev1.Node, nr *NodeResource) error
}

func RegisterNodePrepareExtender(plugins ...NodePreparePlugin) {
	ps := make([]Plugin, len(plugins))
	for i := range plugins {
		ps[i] = plugins[i]
	}
	globalNodePrepareExtender.MustRegister(ps...)
}

func RunNodePrepareExtenders(strategy *extension.ColocationStrategy, node *corev1.Node, nr *NodeResource) {
	for _, p := range globalNodePrepareExtender.GetAll() {
		plugin := p.(NodePreparePlugin)
		if err := plugin.Execute(strategy, node, nr); err != nil {
			klog.ErrorS(err, "run node prepare plugin failed", "plugin", plugin.Name(),
				"node", node.Name)
		} else {
			klog.V(5).InfoS("run node prepare plugin successfully", "plugin", plugin.Name(),
				"node", node.Name)
		}
	}
}

func UnregisterNodePrepareExtender(name string) {
	globalNodePrepareExtender.Unregister(name)
}

// NodeSyncPlugin implements the check of resource updating.
// For example, trigger an update if the values of the current is more than 10% different with the former.
type NodeSyncPlugin interface {
	Plugin
	NeedSync(strategy *extension.ColocationStrategy, oldNode, newNode *corev1.Node) (bool, string)
}

func RegisterNodeSyncExtender(plugins ...NodeSyncPlugin) {
	ps := make([]Plugin, len(plugins))
	for i := range plugins {
		ps[i] = plugins[i]
	}
	globalNodeSyncExtender.MustRegister(ps...)
}

func UnregisterNodeSyncExtender(name string) {
	globalNodeSyncExtender.Unregister(name)
}

func RunNodeSyncExtenders(strategy *extension.ColocationStrategy, oldNode, newNode *corev1.Node) bool {
	for _, p := range globalNodeSyncExtender.GetAll() {
		plugin := p.(NodeSyncPlugin)
		needSync, msg := plugin.NeedSync(strategy, oldNode, newNode)
		if needSync {
			klog.V(4).InfoS("run node sync plugin, need sync", "plugin", plugin.Name(),
				"node", newNode.Name, "message", msg)
			return true
		} else {
			klog.V(6).InfoS("run node sync plugin, no need to sync", "plugin", plugin.Name(),
				"node", newNode.Name)
		}
	}
	return false
}

type ResourceResetPlugin interface {
	Plugin
	Reset(node *corev1.Node, message string) []ResourceItem
}

func RunResourceResetExtenders(nr *NodeResource, node *corev1.Node, message string) {
	for _, p := range globalResourceCalculateExtender.GetAll() {
		plugin := p.(ResourceCalculatePlugin)
		resourceItems := plugin.Reset(node, message)
		nr.Set(resourceItems...)
		klog.V(5).InfoS("run resource reset plugin successfully", "plugin", plugin.Name(),
			"node", node.Name, "resource items", resourceItems, "message", message)
	}
}

// ResourceCalculatePlugin implements resource counting and overcommitment algorithms.
// It implements Reset which can be invoked when the calculated resources need a reset.
// A ResourceCalculatePlugin can handle the case when the metrics are abnormal by implementing degraded calculation.
type ResourceCalculatePlugin interface {
	ResourceResetPlugin
	Calculate(strategy *extension.ColocationStrategy, node *corev1.Node, podList *corev1.PodList, metrics *ResourceMetrics) ([]ResourceItem, error)
}

func RegisterResourceCalculateExtender(plugins ...ResourceCalculatePlugin) {
	ps := make([]Plugin, len(plugins))
	for i := range plugins {
		ps[i] = plugins[i]
	}
	globalResourceCalculateExtender.MustRegister(ps...)
}

func UnregisterResourceCalculateExtender(name string) {
	globalResourceCalculateExtender.Unregister(name)
}

func RunResourceCalculateExtenders(nr *NodeResource, strategy *extension.ColocationStrategy, node *corev1.Node,
	podList *corev1.PodList, metrics *ResourceMetrics) {
	for _, p := range globalResourceCalculateExtender.GetAll() {
		plugin := p.(ResourceCalculatePlugin)
		resourceItems, err := plugin.Calculate(strategy, node, podList, metrics)
		if err != nil {
			klog.ErrorS(err, "run resource calculate plugin failed", "plugin", plugin.Name(),
				"node", node.Name)
		} else {
			nr.Set(resourceItems...)
			klog.V(5).InfoS("run resource calculate plugin successfully",
				"plugin", plugin.Name(), "node", node.Name, "resource items", resourceItems)
		}
	}
}
