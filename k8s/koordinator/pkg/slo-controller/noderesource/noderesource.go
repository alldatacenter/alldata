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
	"sync"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/client-go/util/retry"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

type nodeBEResource struct {
	IsColocationAvailable bool

	MilliCPU *resource.Quantity
	Memory   *resource.Quantity

	Reason  string
	Message string
}

type SyncContext struct {
	lock       sync.RWMutex
	contextMap map[string]time.Time
}

func NewSyncContext() SyncContext {
	return SyncContext{
		contextMap: map[string]time.Time{},
	}
}

func (s *SyncContext) Load(key string) (time.Time, bool) {
	s.lock.RLock()
	defer s.lock.RUnlock()
	value, ok := s.contextMap[key]
	return value, ok
}

func (s *SyncContext) Store(key string, value time.Time) {
	s.lock.Lock()
	defer s.lock.Unlock()
	s.contextMap[key] = value
}

func (s *SyncContext) Delete(key string) {
	s.lock.Lock()
	defer s.lock.Unlock()
	delete(s.contextMap, key)
}

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

func (r *NodeResourceReconciler) isDegradeNeeded(nodeMetric *slov1alpha1.NodeMetric, node *corev1.Node) bool {
	if nodeMetric == nil || nodeMetric.Status.UpdateTime == nil {
		klog.Warningf("invalid NodeMetric: %v, need degradation", nodeMetric)
		return true
	}

	strategy := config.GetNodeColocationStrategy(r.cfgCache.GetCfgCopy(), node)

	if r.Clock.Now().After(nodeMetric.Status.UpdateTime.Add(time.Duration(*strategy.DegradeTimeMinutes) * time.Minute)) {
		klog.Warningf("timeout NodeMetric: %v, current timestamp: %v, metric last update timestamp: %v",
			nodeMetric.Name, r.Clock.Now(), nodeMetric.Status.UpdateTime)
		return true
	}

	return false
}

func (r *NodeResourceReconciler) resetNodeBEResource(node *corev1.Node, reason, message string) error {
	beResource := &nodeBEResource{
		IsColocationAvailable: false,
		MilliCPU:              nil,
		Memory:                nil,
		Reason:                reason,
		Message:               message,
	}
	return r.updateNodeBEResource(node, beResource)
}

func (r *NodeResourceReconciler) isGPUResourceNeedSync(new, old *corev1.Node) bool {
	strategy := config.GetNodeColocationStrategy(r.cfgCache.GetCfgCopy(), new)

	lastUpdatedTime, ok := r.GPUSyncContext.Load(util.GenerateNodeKey(&new.ObjectMeta))
	if !ok || r.Clock.Now().After(lastUpdatedTime.Add(time.Duration(*strategy.UpdateTimeThresholdSeconds)*time.Second)) {
		klog.Warningf("node %v resource expired, need sync", new.Name)
		return true
	}

	for _, resourceName := range []corev1.ResourceName{extension.GPUCore, extension.GPUMemoryRatio, extension.GPUMemory, extension.KoordGPU} {
		if util.IsResourceDiff(old.Status.Allocatable, new.Status.Allocatable, resourceName, *strategy.ResourceDiffThreshold) {
			klog.Warningf("node %v resource diff bigger than %v, need sync", resourceName, *strategy.ResourceDiffThreshold)
			return true
		}
	}
	return false
}

func (r *NodeResourceReconciler) isGPULabelNeedSync(new, old map[string]string) bool {
	return new[extension.GPUModel] != old[extension.GPUModel] ||
		new[extension.GPUDriver] != old[extension.GPUDriver]
}

func (r *NodeResourceReconciler) updateGPUNodeResource(node *corev1.Node, device *schedulingv1alpha1.Device) error {
	if device == nil {
		return nil
	}
	memoryTotal := resource.NewQuantity(0, resource.BinarySI)
	coreTotal := resource.NewQuantity(0, resource.DecimalSI)
	ratioTotal := resource.NewQuantity(0, resource.DecimalSI)
	koordGpuTotal := resource.NewQuantity(0, resource.DecimalSI)
	hasGPUDevice := false
	for _, device := range device.Spec.Devices {
		if device.Type != schedulingv1alpha1.GPU {
			continue
		}
		hasGPUDevice = true
		if device.Health {
			memoryTotal.Add(device.Resources[extension.GPUMemory])
			coreTotal.Add(device.Resources[extension.GPUCore])
			ratioTotal.Add(device.Resources[extension.GPUMemoryRatio])
			koordGpuTotal.Add(device.Resources[extension.GPUCore])
		}
	}

	if !hasGPUDevice {
		return nil
	}

	copyNode := node.DeepCopy()
	copyNode.Status.Allocatable[extension.GPUCore] = *coreTotal
	copyNode.Status.Allocatable[extension.GPUMemory] = *memoryTotal
	copyNode.Status.Allocatable[extension.GPUMemoryRatio] = *ratioTotal
	copyNode.Status.Allocatable[extension.KoordGPU] = *koordGpuTotal

	if !r.isGPUResourceNeedSync(copyNode, node) {
		return nil
	}

	err := util.RetryOnConflictOrTooManyRequests(func() error {
		updateNode := &corev1.Node{}
		if err := r.Client.Get(context.TODO(), types.NamespacedName{Name: node.Name}, updateNode); err != nil {
			klog.Errorf("failed to get node %v, error: %v", node.Name, err)
			if errors.IsNotFound(err) {
				return nil
			}
			return err
		}

		updateNode.Status.Capacity[extension.GPUMemory] = *memoryTotal
		updateNode.Status.Allocatable[extension.GPUMemory] = *memoryTotal
		updateNode.Status.Capacity[extension.GPUCore] = *coreTotal
		updateNode.Status.Allocatable[extension.GPUCore] = *coreTotal
		updateNode.Status.Capacity[extension.GPUMemoryRatio] = *ratioTotal
		updateNode.Status.Allocatable[extension.GPUMemoryRatio] = *ratioTotal
		updateNode.Status.Capacity[extension.KoordGPU] = *koordGpuTotal
		updateNode.Status.Allocatable[extension.KoordGPU] = *koordGpuTotal

		if err := r.Client.Status().Update(context.TODO(), updateNode); err != nil {
			klog.Errorf("failed to update node gpu resource, %v, error: %v", updateNode.Name, err)
			return err
		}
		return nil
	})
	if err == nil {
		r.GPUSyncContext.Store(util.GenerateNodeKey(&node.ObjectMeta), r.Clock.Now())
	}
	return err
}

func (r *NodeResourceReconciler) updateGPUDriverAndModel(node *corev1.Node, device *schedulingv1alpha1.Device) error {
	if device == nil || device.Labels == nil {
		return nil
	}

	if !r.isGPULabelNeedSync(device.Labels, node.Labels) {
		return nil
	}

	err := util.RetryOnConflictOrTooManyRequests(func() error {
		updateNode := &corev1.Node{}
		if err := r.Client.Get(context.TODO(), types.NamespacedName{Name: node.Name}, updateNode); err != nil {
			klog.Errorf("failed to get node %v, error: %v", node.Name, err)
			if errors.IsNotFound(err) {
				return nil
			}
			return err
		}

		updateNodeNew := updateNode.DeepCopy()
		if updateNodeNew.Labels == nil {
			updateNodeNew.Labels = make(map[string]string)
		}
		updateNodeNew.Labels[extension.GPUModel] = device.Labels[extension.GPUModel]
		updateNodeNew.Labels[extension.GPUDriver] = device.Labels[extension.GPUDriver]

		patch := client.MergeFrom(updateNode)
		if err := r.Client.Patch(context.Background(), updateNodeNew, patch); err != nil {
			klog.Errorf("failed to patch node gpu model and version, err:%v", err)
			return err
		} else {
			klog.Infof("Success to patch node:%v gpu model:%v and version:%v",
				node.Name, device.Labels[extension.GPUModel], device.Labels[extension.GPUDriver])
		}
		return nil
	})
	if err == nil {
		r.GPUSyncContext.Store(util.GenerateNodeKey(&node.ObjectMeta), r.Clock.Now())
	}

	return nil
}

func (r *NodeResourceReconciler) updateNodeBEResource(node *corev1.Node, beResource *nodeBEResource) error {
	copyNode := node.DeepCopy()

	r.prepareNodeResource(copyNode, beResource)

	if needSync := r.isBEResourceSyncNeeded(node, copyNode); !needSync {
		return nil
	}

	return retry.RetryOnConflict(retry.DefaultBackoff, func() error {
		updateNode := &corev1.Node{}
		if err := r.Client.Get(context.TODO(), types.NamespacedName{Name: node.Name}, updateNode); err != nil {
			klog.Errorf("failed to get node %v, error: %v", node.Name, err)
			if errors.IsNotFound(err) {
				return nil
			}
			return err
		}

		r.prepareNodeResource(updateNode, beResource)

		if err := r.Client.Status().Update(context.TODO(), updateNode); err != nil {
			klog.Errorf("failed to update node %v, error: %v", updateNode.Name, err)
			return err
		}
		r.BESyncContext.Store(util.GenerateNodeKey(&node.ObjectMeta), r.Clock.Now())
		klog.V(5).Infof("update node %v success, detail %+v", updateNode.Name, updateNode)
		return nil
	})
}

func (r *NodeResourceReconciler) isBEResourceSyncNeeded(old, new *corev1.Node) bool {
	if new == nil || new.Status.Allocatable == nil || new.Status.Capacity == nil {
		klog.Errorf("invalid input, node should not be nil")
		return false
	}

	strategy := config.GetNodeColocationStrategy(r.cfgCache.GetCfgCopy(), new)

	// scenario 1: update time gap is bigger than UpdateTimeThresholdSeconds
	lastUpdatedTime, ok := r.BESyncContext.Load(util.GenerateNodeKey(&new.ObjectMeta))
	if !ok || r.Clock.Now().After(lastUpdatedTime.Add(time.Duration(*strategy.UpdateTimeThresholdSeconds)*time.Second)) {
		klog.Warningf("node %v resource expired, need sync", new.Name)
		return true
	}

	// scenario 2: resource diff is bigger than ResourceDiffThreshold
	if util.IsResourceDiff(old.Status.Allocatable, new.Status.Allocatable, extension.BatchCPU, *strategy.ResourceDiffThreshold) {
		klog.Warningf("node %v resource diff bigger than %v, need sync", extension.BatchCPU, *strategy.ResourceDiffThreshold)
		return true
	}
	if util.IsResourceDiff(old.Status.Allocatable, new.Status.Allocatable, extension.BatchMemory, *strategy.ResourceDiffThreshold) {
		klog.Warningf("node %v resource diff bigger than %v, need sync", extension.BatchMemory, *strategy.ResourceDiffThreshold)
		return true
	}

	// scenario 3: all good, do nothing
	klog.Infof("all good, no need to sync for node %v", new.Name)
	return false
}

func (r *NodeResourceReconciler) prepareNodeResource(node *corev1.Node, beResource *nodeBEResource) {
	if beResource.MilliCPU == nil {
		delete(node.Status.Capacity, extension.BatchCPU)
		delete(node.Status.Allocatable, extension.BatchCPU)
	} else {
		// NOTE: extended resource would be validated as an integer, so beResource should be checked before the update
		if _, ok := beResource.MilliCPU.AsInt64(); !ok {
			klog.V(2).Infof("batch cpu quantity is not int64 type and will be rounded, original value %v",
				*beResource.MilliCPU)
			beResource.MilliCPU.Set(beResource.MilliCPU.Value())
		}
		node.Status.Capacity[extension.BatchCPU] = *beResource.MilliCPU
		node.Status.Allocatable[extension.BatchCPU] = *beResource.MilliCPU
	}

	if beResource.Memory == nil {
		delete(node.Status.Capacity, extension.BatchMemory)
		delete(node.Status.Allocatable, extension.BatchMemory)
	} else {
		// NOTE: extended resource would be validated as an integer, so beResource should be checked before the update
		if _, ok := beResource.Memory.AsInt64(); !ok {
			klog.V(2).Infof("batch memory quantity is not int64 type and will be rounded, original value %v",
				*beResource.Memory)
			beResource.Memory.Set(beResource.Memory.Value())
		}
		node.Status.Capacity[extension.BatchMemory] = *beResource.Memory
		node.Status.Allocatable[extension.BatchMemory] = *beResource.Memory
	}

	strategy := config.GetNodeColocationStrategy(r.cfgCache.GetCfgCopy(), node)
	runNodePrepareExtenders(strategy, node)
}
