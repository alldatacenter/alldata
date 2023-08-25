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
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/klog/v2"
	"sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/koordinator-sh/koordinator/apis/extension"
	schedulingv1alpha1 "github.com/koordinator-sh/koordinator/apis/scheduling/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/config"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

func (r *NodeResourceReconciler) updateDeviceResources(node *corev1.Node) error {
	// calculate device resources
	device := &schedulingv1alpha1.Device{}
	if err := r.Client.Get(context.TODO(), types.NamespacedName{Name: node.Name, Namespace: node.Namespace}, device); err != nil {
		if !errors.IsNotFound(err) {
			return fmt.Errorf("failed to get device resources: %w", err)
		}
		return nil
	}

	// update device resources
	if err := r.updateGPUNodeResource(node, device); err != nil {
		return fmt.Errorf("failed to update gpus node resources, err: %w", err)
	}

	// update node labels for device
	if err := r.updateGPUDriverAndModel(node, device); err != nil {
		klog.Errorf("failed to update gpu model and driver, err: %v", node.Name, err)
		return err
	}

	return nil
}

func (r *NodeResourceReconciler) isGPUResourceNeedSync(new, old *corev1.Node) bool {
	strategy := config.GetNodeColocationStrategy(r.cfgCache.GetCfgCopy(), new)

	lastUpdatedTime, ok := r.GPUSyncContext.Load(util.GenerateNodeKey(&new.ObjectMeta))
	if !ok || r.Clock.Since(lastUpdatedTime) > time.Duration(*strategy.UpdateTimeThresholdSeconds)*time.Second {
		klog.V(4).Infof("node %v resource expired, need sync", new.Name)
		return true
	}

	for _, resourceName := range []corev1.ResourceName{extension.ResourceGPUCore, extension.ResourceGPUMemoryRatio, extension.ResourceGPUMemory, extension.ResourceGPU} {
		if util.IsResourceDiff(old.Status.Allocatable, new.Status.Allocatable, resourceName, *strategy.ResourceDiffThreshold) {
			klog.V(4).Infof("node %v resource diff bigger than %v, need sync", resourceName, *strategy.ResourceDiffThreshold)
			return true
		}
	}
	return false
}

func (r *NodeResourceReconciler) isGPULabelNeedSync(new, old map[string]string) bool {
	return new[extension.LabelGPUModel] != old[extension.LabelGPUModel] ||
		new[extension.LabelGPUDriverVersion] != old[extension.LabelGPUDriverVersion]
}

func (r *NodeResourceReconciler) updateGPUNodeResource(node *corev1.Node, device *schedulingv1alpha1.Device) error {
	// TODO: currently update the device resources barely. move to device plugins or implement a standard plugin later
	if device == nil {
		return nil
	}
	gpuResources := make(corev1.ResourceList)
	totalKoordGPU := resource.NewQuantity(0, resource.DecimalSI)
	hasGPUDevice := false
	for _, device := range device.Spec.Devices {
		if device.Type != schedulingv1alpha1.GPU || !device.Health {
			continue
		}
		hasGPUDevice = true
		resources := extension.TransformDeprecatedDeviceResources(device.Resources)
		util.AddResourceList(gpuResources, resources)
		totalKoordGPU.Add(resources[extension.ResourceGPUCore])
	}
	gpuResources[extension.ResourceGPU] = *totalKoordGPU

	if !hasGPUDevice {
		return nil
	}

	copyNode := node.DeepCopy()
	for resourceName, quantity := range gpuResources {
		copyNode.Status.Allocatable[resourceName] = quantity.DeepCopy()
	}
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

		updateNode = updateNode.DeepCopy() // avoid overwriting the cache
		for resourceName, quantity := range gpuResources {
			updateNode.Status.Capacity[resourceName] = quantity.DeepCopy()
			updateNode.Status.Allocatable[resourceName] = quantity.DeepCopy()
		}
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
	// TODO: currently update the device resources barely. move to device plugins or implement a standard plugin later
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
		updateNodeNew.Labels[extension.LabelGPUModel] = device.Labels[extension.LabelGPUModel]
		updateNodeNew.Labels[extension.LabelGPUDriverVersion] = device.Labels[extension.LabelGPUDriverVersion]

		patch := client.MergeFrom(updateNode)
		if err := r.Client.Patch(context.Background(), updateNodeNew, patch); err != nil {
			klog.Errorf("failed to patch node gpu model and version, err:%v", err)
			return err
		} else {
			klog.Infof("Success to patch node:%v gpu model:%v and version:%v",
				node.Name, device.Labels[extension.LabelGPUModel], device.Labels[extension.LabelGPUDriverVersion])
		}
		return nil
	})
	if err == nil {
		r.GPUSyncContext.Store(util.GenerateNodeKey(&node.ObjectMeta), r.Clock.Now())
	}

	return nil
}
