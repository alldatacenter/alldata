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

package batchresource

import (
	"fmt"
	"time"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/util/clock"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
	"k8s.io/klog/v2"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/slo-controller/noderesource/framework"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

const PluginName = "BatchResource"

var ResourceNames = []corev1.ResourceName{extension.BatchCPU, extension.BatchMemory}

var Clock clock.Clock = clock.RealClock{} // for testing

type Plugin struct{}

func (p *Plugin) Name() string {
	return PluginName
}

func (p *Plugin) NeedSync(strategy *extension.ColocationStrategy, oldNode, newNode *corev1.Node) (bool, string) {
	// batch resource diff is bigger than ResourceDiffThreshold
	resourcesToDiff := ResourceNames
	for _, resourceName := range resourcesToDiff {
		if util.IsResourceDiff(oldNode.Status.Allocatable, newNode.Status.Allocatable, resourceName,
			*strategy.ResourceDiffThreshold) {
			klog.V(4).Infof("node %v resource %v diff bigger than %v, need sync",
				newNode.Name, resourceName, *strategy.ResourceDiffThreshold)
			return true, "batch resource diff is big than threshold"
		}
	}

	return false, ""
}

func (p *Plugin) Execute(strategy *extension.ColocationStrategy, node *corev1.Node, nr *framework.NodeResource) error {
	for _, resourceName := range ResourceNames {
		prepareNodeForResource(node, nr, resourceName)
	}
	return nil
}

func (p *Plugin) Reset(node *corev1.Node, message string) []framework.ResourceItem {
	items := make([]framework.ResourceItem, len(ResourceNames))
	for i := range ResourceNames {
		items[i].Name = ResourceNames[i]
		items[i].Message = message
		items[i].Reset = true
	}

	return items
}

// Calculate calculates Batch resources using the formula below:
// Node.Total - Node.Reserved - System.Used - Pod(non-BE).Used, System.Used = Node.Used - Pod(All).Used.
func (p *Plugin) Calculate(strategy *extension.ColocationStrategy, node *corev1.Node, podList *corev1.PodList,
	metrics *framework.ResourceMetrics) ([]framework.ResourceItem, error) {
	if strategy == nil || node == nil || podList == nil || metrics == nil || metrics.NodeMetric == nil {
		return nil, fmt.Errorf("missing essential arguments")
	}

	// if the node metric is abnormal, do degraded calculation
	if p.isDegradeNeeded(strategy, metrics.NodeMetric, node) {
		klog.InfoS("node need degradation, reset node resources", "node", node.Name)
		return p.degradeCalculate(node,
			"degrade node resource because of abnormal nodeMetric, reason: degradedByBatchResource"), nil
	}

	// NOTE: currently, non-BE pods are considered as LS, and BE pods are considered using Batch
	podLSRequest := util.NewZeroResourceList()
	podLSUsed := util.NewZeroResourceList()
	// pod(All).Used = pod(LS).Used + pod(BE).Used
	podAllUsed := util.NewZeroResourceList()

	nodeMetric := metrics.NodeMetric
	podMetricMap := make(map[string]*slov1alpha1.PodMetricInfo)
	for _, podMetric := range nodeMetric.Status.PodsMetric {
		podMetricMap[util.GetPodMetricKey(podMetric)] = podMetric
	}

	for _, pod := range podList.Items {
		if pod.Status.Phase != corev1.PodRunning && pod.Status.Phase != corev1.PodPending {
			continue
		}

		qosClass := extension.GetPodQoSClass(&pod)

		podRequest := util.GetPodRequest(&pod, corev1.ResourceCPU, corev1.ResourceMemory)
		if qosClass != extension.QoSBE {
			podLSRequest = quotav1.Add(podLSRequest, podRequest)
		}
		podKey := util.GetPodKey(&pod)
		podMetric, ok := podMetricMap[podKey]
		if !ok {
			if qosClass != extension.QoSBE {
				podLSUsed = quotav1.Add(podLSUsed, podRequest)
			}
			podAllUsed = quotav1.Add(podAllUsed, podRequest)
			continue
		}

		if qosClass != extension.QoSBE {
			podLSUsed = quotav1.Add(podLSUsed, getPodMetricUsage(podMetric))
		}
		podAllUsed = quotav1.Add(podAllUsed, getPodMetricUsage(podMetric))
	}

	nodeAllocatable := getNodeAllocatable(node)
	nodeReservation := getNodeReservation(strategy, node)

	// System.Used = Node.Used - Pod(All).Used
	nodeUsage := getNodeMetricUsage(nodeMetric.Status.NodeMetric)
	systemUsed := quotav1.Max(quotav1.Subtract(nodeUsage, podAllUsed), util.NewZeroResourceList())

	// System.Used = max(System.Used, Node.Anno.Reserved)
	nodeAnnoReserved := util.GetNodeReservationFromAnnotation(node.Annotations)
	systemUsed = quotav1.Max(systemUsed, nodeAnnoReserved)

	batchAllocatable, cpuMsg, memMsg := calculateBatchResourceByPolicy(strategy, node, nodeAllocatable,
		nodeReservation, systemUsed,
		podLSRequest, podLSUsed)
	klog.V(6).InfoS("calculate batch resource for node", "node", node.Name, "batch resource",
		batchAllocatable, "cpu", cpuMsg, "memory", memMsg)

	return []framework.ResourceItem{
		{
			Name:     extension.BatchCPU,
			Quantity: resource.NewQuantity(batchAllocatable.Cpu().MilliValue(), resource.DecimalSI),
			Message:  cpuMsg,
		},
		{
			Name:     extension.BatchMemory,
			Quantity: batchAllocatable.Memory(),
			Message:  memMsg,
		},
	}, nil
}

func calculateBatchResourceByPolicy(strategy *extension.ColocationStrategy, node *corev1.Node,
	nodeAllocatable, nodeReserve, systemUsed, podLSReq, podLSUsed corev1.ResourceList) (corev1.ResourceList, string, string) {
	// Node(BE).Alloc = Node.Total - Node.Reserved - System.Used - Pod(LS).Used
	batchAllocatableByUsage := quotav1.Max(quotav1.Subtract(quotav1.Subtract(quotav1.Subtract(
		nodeAllocatable, nodeReserve), systemUsed), podLSUsed), util.NewZeroResourceList())

	// Node(BE).Alloc = Node.Total - Node.Reserved - Pod(LS).Request
	batchAllocatableByRequest := quotav1.Max(quotav1.Subtract(quotav1.Subtract(nodeAllocatable, nodeReserve),
		podLSReq), util.NewZeroResourceList())

	batchAllocatable := batchAllocatableByUsage
	cpuMsg := fmt.Sprintf("batchAllocatable[CPU(Milli-Core)]:%v = nodeAllocatable:%v - nodeReservation:%v - systemUsage:%v - podLSUsed:%v",
		batchAllocatable.Cpu().MilliValue(), nodeAllocatable.Cpu().MilliValue(), nodeReserve.Cpu().MilliValue(),
		systemUsed.Cpu().MilliValue(), podLSUsed.Cpu().MilliValue())

	var memMsg string
	if strategy != nil && strategy.MemoryCalculatePolicy != nil && *strategy.MemoryCalculatePolicy == extension.CalculateByPodRequest {
		batchAllocatable[corev1.ResourceMemory] = *batchAllocatableByRequest.Memory()
		memMsg = fmt.Sprintf("batchAllocatable[Mem(GB)]:%v = nodeAllocatable:%v - nodeReservation:%v - podLSRequest:%v",
			batchAllocatable.Memory().ScaledValue(resource.Giga), nodeAllocatable.Memory().ScaledValue(resource.Giga),
			nodeReserve.Memory().ScaledValue(resource.Giga), podLSReq.Memory().ScaledValue(resource.Giga))
	} else { // use CalculatePolicy "usage" by default
		memMsg = fmt.Sprintf("batchAllocatable[Mem(GB)]:%v = nodeAllocatable:%v - nodeReservation:%v - systemUsage:%v - podLSUsed:%v",
			batchAllocatable.Memory().ScaledValue(resource.Giga), nodeAllocatable.Memory().ScaledValue(resource.Giga),
			nodeReserve.Memory().ScaledValue(resource.Giga), systemUsed.Memory().ScaledValue(resource.Giga),
			podLSUsed.Memory().ScaledValue(resource.Giga))
	}

	return batchAllocatable, cpuMsg, memMsg
}

func (p *Plugin) isDegradeNeeded(strategy *extension.ColocationStrategy, nodeMetric *slov1alpha1.NodeMetric, node *corev1.Node) bool {
	if nodeMetric == nil || nodeMetric.Status.UpdateTime == nil {
		klog.V(3).Infof("invalid NodeMetric: %v, need degradation", nodeMetric)
		return true
	}

	now := Clock.Now()
	if now.After(nodeMetric.Status.UpdateTime.Add(time.Duration(*strategy.DegradeTimeMinutes) * time.Minute)) {
		klog.V(3).Infof("timeout NodeMetric: %v, current timestamp: %v, metric last update timestamp: %v",
			nodeMetric.Name, now, nodeMetric.Status.UpdateTime)
		return true
	}

	return false
}

func (p *Plugin) degradeCalculate(node *corev1.Node, message string) []framework.ResourceItem {
	return p.Reset(node, message)
}

func prepareNodeForResource(node *corev1.Node, nr *framework.NodeResource, name corev1.ResourceName) {
	if nr.Resets[name] {
		delete(node.Status.Capacity, name)
		delete(node.Status.Allocatable, name)
	} else if q := nr.Resources[name]; q == nil { // if the specified resource has no quantity
		delete(node.Status.Capacity, name)
		delete(node.Status.Allocatable, name)
	} else {
		// NOTE: extended resource would be validated as an integer, so it should be checked before the update
		if _, ok := q.AsInt64(); !ok {
			klog.V(2).InfoS("node resource's quantity is not int64 and will be rounded",
				"resource", name, "original", *q)
			q.Set(q.Value())
		}
		node.Status.Capacity[name] = *q
		node.Status.Allocatable[name] = *q
	}
}

// getPodMetricUsage gets pod usage from the PodMetricInfo
func getPodMetricUsage(info *slov1alpha1.PodMetricInfo) corev1.ResourceList {
	cpuQuant := info.PodUsage.ResourceList[corev1.ResourceCPU]
	cpuUsageQuant := resource.NewMilliQuantity(cpuQuant.MilliValue(), cpuQuant.Format)
	memQuant := info.PodUsage.ResourceList[corev1.ResourceMemory]
	memUsageQuant := resource.NewQuantity(memQuant.Value(), memQuant.Format)
	return corev1.ResourceList{corev1.ResourceCPU: *cpuUsageQuant, corev1.ResourceMemory: *memUsageQuant}
}

// getNodeMetricUsage gets node usage from the NodeMetricInfo
func getNodeMetricUsage(info *slov1alpha1.NodeMetricInfo) corev1.ResourceList {
	cpuQ := info.NodeUsage.ResourceList[corev1.ResourceCPU]
	cpuUsageQ := resource.NewMilliQuantity(cpuQ.MilliValue(), cpuQ.Format)
	memQ := info.NodeUsage.ResourceList[corev1.ResourceMemory]
	memUsageQ := resource.NewQuantity(memQ.Value(), memQ.Format)
	return corev1.ResourceList{corev1.ResourceCPU: *cpuUsageQ, corev1.ResourceMemory: *memUsageQ}
}

// getNodeAllocatable gets node allocatable and filters out non-CPU and non-Mem resources
func getNodeAllocatable(node *corev1.Node) corev1.ResourceList {
	result := node.Status.Allocatable.DeepCopy()
	result = quotav1.Mask(result, []corev1.ResourceName{corev1.ResourceCPU, corev1.ResourceMemory})
	return result
}

// getNodeReservation gets node-level safe-guarding reservation with the node's allocatable
func getNodeReservation(strategy *extension.ColocationStrategy, node *corev1.Node) corev1.ResourceList {
	nodeAllocatable := getNodeAllocatable(node)
	cpuReserveQuant := util.MultiplyMilliQuant(nodeAllocatable[corev1.ResourceCPU], getReserveRatio(*strategy.CPUReclaimThresholdPercent))
	memReserveQuant := util.MultiplyQuant(nodeAllocatable[corev1.ResourceMemory], getReserveRatio(*strategy.MemoryReclaimThresholdPercent))

	return corev1.ResourceList{
		corev1.ResourceCPU:    cpuReserveQuant,
		corev1.ResourceMemory: memReserveQuant,
	}
}

// getReserveRatio returns resource reserved ratio
func getReserveRatio(reclaimThreshold int64) float64 {
	return float64(100-reclaimThreshold) / 100.0
}
