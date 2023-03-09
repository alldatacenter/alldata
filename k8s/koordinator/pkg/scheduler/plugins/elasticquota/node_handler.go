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
	corev1 "k8s.io/api/core/v1"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/pkg/scheduler/plugins/elasticquota/core"
)

func (g *Plugin) OnNodeAdd(obj interface{}) {
	node, ok := obj.(*corev1.Node)
	if !ok {
		return
	}

	if node.DeletionTimestamp != nil {
		klog.Errorf("node is deleting:%v", node.Name)
		return
	}

	allocatable := core.RunDecorateNode(node).Status.Allocatable

	g.nodeResourceMapLock.Lock()
	defer g.nodeResourceMapLock.Unlock()

	if _, ok := g.nodeResourceMap[node.Name]; ok {
		return
	}
	g.nodeResourceMap[node.Name] = struct{}{}
	g.groupQuotaManager.UpdateClusterTotalResource(allocatable)
	klog.V(5).Infof("OnNodeAddFunc success %v", node.Name)
}

func (g *Plugin) OnNodeUpdate(oldObj, newObj interface{}) {
	newNode := newObj.(*corev1.Node)
	oldNode := oldObj.(*corev1.Node)

	g.nodeResourceMapLock.Lock()
	defer g.nodeResourceMapLock.Unlock()

	if _, exist := g.nodeResourceMap[newNode.Name]; !exist {
		return
	}

	if newNode.ResourceVersion == oldNode.ResourceVersion {
		klog.Warningf("update node warning, update version for the same, nodeName:%v", newNode.Name)
		return
	}

	if newNode.DeletionTimestamp != nil {
		klog.V(5).Infof("OnNodeUpdateFunc update:%v delete:%v", newNode.Name, newNode.DeletionTimestamp)
		return
	}

	oldNodeAllocatable := core.RunDecorateNode(oldNode).Status.Allocatable
	newNodeAllocatable := core.RunDecorateNode(newNode).Status.Allocatable

	if quotav1.Equals(oldNodeAllocatable, newNodeAllocatable) {
		return
	}

	deltaNodeAllocatable := quotav1.Subtract(newNodeAllocatable, oldNodeAllocatable)
	g.groupQuotaManager.UpdateClusterTotalResource(deltaNodeAllocatable)
	klog.V(5).Infof("OnNodeUpdateFunc success:%v [%v]", newNode.Name, newNodeAllocatable)
}

func (g *Plugin) OnNodeDelete(obj interface{}) {
	node, ok := obj.(*corev1.Node)
	if !ok {
		klog.Errorf("node is nil")
		return
	}

	g.nodeResourceMapLock.Lock()
	defer g.nodeResourceMapLock.Unlock()

	if _, exist := g.nodeResourceMap[node.Name]; !exist {
		return
	}

	allocatable := core.RunDecorateNode(node).Status.Allocatable
	delta := quotav1.Subtract(corev1.ResourceList{}, allocatable)
	g.groupQuotaManager.UpdateClusterTotalResource(delta)
	delete(g.nodeResourceMap, node.Name)
	klog.V(5).Infof("OnNodeDeleteFunc success:%v [%v]", node.Name, delta)
}
