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

package noderesource

import (
	"context"
	"fmt"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/noderesource/framework"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func (r *NodeResourceReconciler) isColocationCfgDisabled(node *corev1.Node) bool {
	cfg := r.cfgCache.GetCfgCopy()
	if cfg.Enable == nil || !*cfg.Enable {
		return true
	}
	strategy := config.GetNodeColocationStrategy(cfg, node)
	if strategy == nil || strategy.Enable == nil {
		return true
	}
	return !(*strategy.Enable)
}

func (r *NodeResourceReconciler) resetNodeResource(node *corev1.Node, message string) error {
	nr := framework.NewNodeResource()

	framework.RunResourceResetExtenders(nr, node, message)

	return r.updateNodeResource(node, nr)
}

func (r *NodeResourceReconciler) calculateNodeResource(node *corev1.Node,
	nodeMetric *slov1alpha1.NodeMetric, podList *corev1.PodList) *framework.NodeResource {
	nr := framework.NewNodeResource()
	metrics := &framework.ResourceMetrics{
		NodeMetric: nodeMetric,
	}

	strategy := config.GetNodeColocationStrategy(r.cfgCache.GetCfgCopy(), node)
	framework.RunResourceCalculateExtenders(nr, strategy, node, podList, metrics)

	return nr
}

func (r *NodeResourceReconciler) updateNodeResource(node *corev1.Node, nr *framework.NodeResource) error {
	nodeCopy := node.DeepCopy() // avoid overwriting the cache
	strategy := config.GetNodeColocationStrategy(r.cfgCache.GetCfgCopy(), node)

	r.prepareNodeResource(strategy, nodeCopy, nr)

	if needSync := r.isNodeResourceSyncNeeded(strategy, node, nodeCopy); !needSync {
		return nil
	}

	return util.RetryOnConflictOrTooManyRequests(func() error {
		nodeCopy = &corev1.Node{}
		if err := r.Client.Get(context.TODO(), types.NamespacedName{Name: node.Name}, nodeCopy); err != nil {
			if errors.IsNotFound(err) {
				klog.V(4).InfoS("aborted to update node", "node", nodeCopy.Name, "err", err)
				return nil
			}
			klog.ErrorS(err, "failed to get node", "node", node.Name)
			return err
		}

		nodeCopy = nodeCopy.DeepCopy() // avoid overwriting the cache
		r.prepareNodeResource(strategy, nodeCopy, nr)

		if err := r.Client.Status().Update(context.TODO(), nodeCopy); err != nil {
			klog.ErrorS(err, "failed to update node status", "node", nodeCopy.Name)
			return err
		}
		r.NodeSyncContext.Store(util.GenerateNodeKey(&node.ObjectMeta), r.Clock.Now())
		klog.V(5).InfoS("update node successfully", "node", nodeCopy.Name, "detail", nodeCopy)
		return nil
	})
}

// updateNodeExtensions is an extension point for updating node other than node metric resources.
func (r *NodeResourceReconciler) updateNodeExtensions(node *corev1.Node, nodeMetric *slov1alpha1.NodeMetric, podList *corev1.PodList) error {
	// update device resources
	if err := r.updateDeviceResources(node); err != nil {
		klog.V(4).InfoS("failed to update device resources for node", "node", node.Name,
			"err", err)
		return err
	}

	return nil
}

func (r *NodeResourceReconciler) isNodeResourceSyncNeeded(strategy *extension.ColocationStrategy, oldNode, newNode *corev1.Node) bool {
	if newNode == nil || newNode.Status.Allocatable == nil || newNode.Status.Capacity == nil {
		klog.ErrorS(fmt.Errorf("invalid node status"), "invalid input, node should be non-nil")
		return false
	}

	if r.isCommonNodeNeedSync(strategy, oldNode, newNode) {
		klog.V(6).InfoS("need sync for node", "node", newNode.Name)
		return true
	}

	needSync := framework.RunNodeSyncExtenders(strategy, oldNode, newNode)
	if needSync {
		klog.V(6).InfoS("need sync for node by extender", "node", newNode.Name)
		return true
	}

	klog.V(4).InfoS("all good, no need to sync for node", "node", newNode.Name)
	return false
}

func (r *NodeResourceReconciler) isCommonNodeNeedSync(strategy *extension.ColocationStrategy, oldNode, newNode *corev1.Node) bool {
	// update time gap is bigger than UpdateTimeThresholdSeconds
	lastUpdatedTime, ok := r.NodeSyncContext.Load(util.GenerateNodeKey(&newNode.ObjectMeta))
	if !ok || r.Clock.Since(lastUpdatedTime) > time.Duration(*strategy.UpdateTimeThresholdSeconds)*time.Second {
		klog.V(4).InfoS("node resource expired, need sync", "node", newNode.Name)
		return true
	}

	return false
}

func (r *NodeResourceReconciler) prepareNodeResource(strategy *extension.ColocationStrategy, node *corev1.Node, nr *framework.NodeResource) {
	framework.RunNodePrepareExtenders(strategy, node, nr)
}
