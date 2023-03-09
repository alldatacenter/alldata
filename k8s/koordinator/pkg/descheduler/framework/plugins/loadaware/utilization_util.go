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

package loadaware

import (
	"context"
	"fmt"
	"sort"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/klog/v2"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	slolisters "github.com/koordinator-sh/koordinator/pkg/client/listers/slo/v1alpha1"
	deschedulerconfig "github.com/koordinator-sh/koordinator/pkg/descheduler/apis/config"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/framework"
	nodeutil "github.com/koordinator-sh/koordinator/pkg/descheduler/node"
	podutil "github.com/koordinator-sh/koordinator/pkg/descheduler/pod"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/utils/sorter"
)

type Percentage = deschedulerconfig.Percentage
type ResourceThresholds = deschedulerconfig.ResourceThresholds

type NodeUsage struct {
	node       *corev1.Node
	nodeMetric *slov1alpha1.NodeMetric
	usage      map[corev1.ResourceName]*resource.Quantity
	allPods    []*corev1.Pod
	podMetrics map[types.NamespacedName]*slov1alpha1.ResourceMap
}

type NodeThresholds struct {
	lowResourceThreshold  map[corev1.ResourceName]*resource.Quantity
	highResourceThreshold map[corev1.ResourceName]*resource.Quantity
}

type NodeInfo struct {
	*NodeUsage
	thresholds NodeThresholds
}

type continueEvictionCond func(nodeInfo NodeInfo, totalAvailableUsages map[corev1.ResourceName]*resource.Quantity) bool

type evictionReasonGeneratorFn func(nodeInfo NodeInfo) string

const (
	MinResourcePercentage = 0
	MaxResourcePercentage = 100
)

func normalizePercentage(percent Percentage) Percentage {
	if percent > MaxResourcePercentage {
		return MaxResourcePercentage
	}
	if percent < MinResourcePercentage {
		return MinResourcePercentage
	}
	return percent
}

func getNodeThresholds(
	nodeUsages map[string]*NodeUsage,
	lowThreshold, highThreshold ResourceThresholds,
	resourceNames []corev1.ResourceName,
	useDeviationThresholds bool,
) map[string]NodeThresholds {
	var averageResourceUsagePercent ResourceThresholds
	if useDeviationThresholds {
		averageResourceUsagePercent = calcAverageResourceUsagePercent(nodeUsages)
	}

	nodeThresholdsMap := map[string]NodeThresholds{}
	for _, nodeUsage := range nodeUsages {
		thresholds := NodeThresholds{
			lowResourceThreshold:  map[corev1.ResourceName]*resource.Quantity{},
			highResourceThreshold: map[corev1.ResourceName]*resource.Quantity{},
		}
		allocatable := nodeUsage.node.Status.Allocatable
		for _, resourceName := range resourceNames {
			if useDeviationThresholds {
				resourceCapacity := allocatable[resourceName]
				if lowThreshold[resourceName] == MinResourcePercentage {
					thresholds.lowResourceThreshold[resourceName] = &resourceCapacity
					thresholds.highResourceThreshold[resourceName] = &resourceCapacity
				} else {
					thresholds.lowResourceThreshold[resourceName] = resourceThreshold(allocatable, resourceName, normalizePercentage(averageResourceUsagePercent[resourceName]-lowThreshold[resourceName]))
					thresholds.highResourceThreshold[resourceName] = resourceThreshold(allocatable, resourceName, normalizePercentage(averageResourceUsagePercent[resourceName]+highThreshold[resourceName]))
				}
			} else {
				thresholds.lowResourceThreshold[resourceName] = resourceThreshold(allocatable, resourceName, lowThreshold[resourceName])
				thresholds.highResourceThreshold[resourceName] = resourceThreshold(allocatable, resourceName, highThreshold[resourceName])
			}
		}
		nodeThresholdsMap[nodeUsage.node.Name] = thresholds
	}
	return nodeThresholdsMap
}

func resourceThreshold(nodeCapacity corev1.ResourceList, resourceName corev1.ResourceName, threshold Percentage) *resource.Quantity {
	resourceCapacityFraction := func(resourceNodeCapacity int64) int64 {
		// A threshold is in percentages but in <0;100> interval.
		// Performing `threshold * 0.01` will convert <0;100> interval into <0;1>.
		// Multiplying it with capacity will give fraction of the capacity corresponding to the given resource threshold in Quantity units.
		return int64(float64(threshold) * 0.01 * float64(resourceNodeCapacity))
	}

	resourceCapacityQuantity := nodeCapacity[resourceName]
	if resourceName == corev1.ResourceCPU {
		return resource.NewMilliQuantity(resourceCapacityFraction(resourceCapacityQuantity.MilliValue()), resourceCapacityQuantity.Format)
	}
	return resource.NewQuantity(resourceCapacityFraction(resourceCapacityQuantity.Value()), resourceCapacityQuantity.Format)
}

func getNodeUsage(nodes []*corev1.Node, resourceNames []corev1.ResourceName, nodeMetricLister slolisters.NodeMetricLister, getPodsAssignedToNode podutil.GetPodsAssignedToNodeFunc) map[string]*NodeUsage {
	nodeUsages := map[string]*NodeUsage{}
	for _, v := range nodes {
		pods, err := podutil.ListPodsOnANode(v.Name, getPodsAssignedToNode, nil)
		if err != nil {
			klog.ErrorS(err, "Node will not be processed, error accessing its pods", "node", klog.KObj(v))
			continue
		}

		nodeMetric, err := nodeMetricLister.Get(v.Name)
		if err != nil {
			klog.ErrorS(err, "Failed to get NodeMetric", "node", klog.KObj(v))
			continue
		}
		// TODO(joseph): We should check if NodeMetric is expired.
		if nodeMetric.Status.NodeMetric == nil {
			continue
		}

		usage := map[corev1.ResourceName]*resource.Quantity{}
		for _, resourceName := range resourceNames {
			usageQuantity, ok := nodeMetric.Status.NodeMetric.NodeUsage.ResourceList[resourceName]
			if !ok {
				switch resourceName {
				case corev1.ResourceCPU:
					usageQuantity = *resource.NewMilliQuantity(0, resource.DecimalSI)
				case corev1.ResourceMemory, corev1.ResourceEphemeralStorage, corev1.ResourceStorage:
					usageQuantity = *resource.NewQuantity(0, resource.BinarySI)
				default:
					usageQuantity = *resource.NewQuantity(0, resource.DecimalSI)
				}
			}
			usage[resourceName] = &usageQuantity
		}
		usage[corev1.ResourcePods] = resource.NewQuantity(int64(len(pods)), resource.DecimalSI)

		podMetrics := make(map[types.NamespacedName]*slov1alpha1.ResourceMap)
		for _, podMetric := range nodeMetric.Status.PodsMetric {
			podMetrics[types.NamespacedName{Namespace: podMetric.Namespace, Name: podMetric.Name}] = podMetric.PodUsage.DeepCopy()
		}

		nodeUsages[v.Name] = &NodeUsage{
			node:       v,
			nodeMetric: nodeMetric,
			usage:      usage,
			allPods:    pods,
			podMetrics: podMetrics,
		}
	}

	return nodeUsages
}

// classifyNodes classifies the nodes into low-utilization or high-utilization nodes.
// If a node lies between low and high thresholds, it is simply ignored.
func classifyNodes(
	nodeUsages map[string]*NodeUsage,
	nodeThresholds map[string]NodeThresholds,
	lowThresholdFilter, highThresholdFilter func(usage *NodeUsage, threshold NodeThresholds) bool,
) (lowNodes []NodeInfo, highNodes []NodeInfo) {
	for _, nodeUsage := range nodeUsages {
		nodeInfo := NodeInfo{
			NodeUsage:  nodeUsage,
			thresholds: nodeThresholds[nodeUsage.node.Name],
		}
		if lowThresholdFilter(nodeUsage, nodeThresholds[nodeUsage.node.Name]) {
			klog.InfoS("Node is underutilized", "node", klog.KObj(nodeUsage.node), "usage", nodeUsage.usage, "usagePercentage", resourceUsagePercentages(nodeUsage))
			lowNodes = append(lowNodes, nodeInfo)
		} else if highThresholdFilter(nodeUsage, nodeThresholds[nodeUsage.node.Name]) {
			klog.InfoS("Node is overutilized", "node", klog.KObj(nodeUsage.node), "usage", nodeUsage.usage, "usagePercentage", resourceUsagePercentages(nodeUsage))
			highNodes = append(highNodes, nodeInfo)
		} else {
			klog.InfoS("Node is appropriately utilized", "node", klog.KObj(nodeUsage.node), "usage", nodeUsage.usage, "usagePercentage", resourceUsagePercentages(nodeUsage))
		}
	}

	return lowNodes, highNodes
}

func resourceUsagePercentages(nodeUsage *NodeUsage) map[corev1.ResourceName]float64 {
	allocatable := nodeUsage.node.Status.Allocatable
	resourceUsagePercentage := map[corev1.ResourceName]float64{}
	for resourceName, resourceUsage := range nodeUsage.usage {
		resourceCapacity := allocatable[resourceName]
		if !resourceCapacity.IsZero() {
			resourceUsagePercentage[resourceName] = 100 * float64(resourceUsage.MilliValue()) / float64(resourceCapacity.MilliValue())
		}
	}

	return resourceUsagePercentage
}

func evictPodsFromSourceNodes(
	ctx context.Context,
	sourceNodes, destinationNodes []NodeInfo,
	dryRun bool,
	nodeFit bool,
	podEvictor framework.Evictor,
	podFilter framework.FilterFunc,
	nodeIndexer podutil.GetPodsAssignedToNodeFunc,
	resourceNames []corev1.ResourceName,
	continueEviction continueEvictionCond,
	evictionReasonGenerator evictionReasonGeneratorFn,
) {
	var targetNodes []*corev1.Node
	totalAvailableUsages := map[corev1.ResourceName]*resource.Quantity{}
	for _, destinationNode := range destinationNodes {
		targetNodes = append(targetNodes, destinationNode.node)

		for _, resourceName := range resourceNames {
			quantity, ok := totalAvailableUsages[resourceName]
			if !ok {
				switch resourceName {
				case corev1.ResourceCPU:
					quantity = resource.NewMilliQuantity(0, resource.DecimalSI)
				case corev1.ResourceMemory, corev1.ResourceEphemeralStorage, corev1.ResourceStorage:
					quantity = resource.NewQuantity(0, resource.BinarySI)
				default:
					quantity = resource.NewQuantity(0, resource.DecimalSI)
				}
				totalAvailableUsages[resourceName] = quantity
			}
			quantity.Add(*destinationNode.thresholds.highResourceThreshold[resourceName])
			quantity.Sub(*destinationNode.usage[resourceName])
		}
	}

	var keysAndValues []interface{}
	for resourceName, quantity := range totalAvailableUsages {
		keysAndValues = append(keysAndValues, string(resourceName), quantity.String())
	}
	klog.V(4).InfoS("Total capacity to be moved", keysAndValues...)

	for _, srcNode := range sourceNodes {
		nonRemovablePods, removablePods := classifyPods(
			srcNode.allPods,
			podutil.WrapFilterFuncs(podFilter, func(pod *corev1.Pod) bool {
				if !nodeFit {
					return true
				}
				return nodeutil.PodFitsAnyNode(nodeIndexer, pod, targetNodes)
			}),
		)
		klog.V(4).InfoS("Evicting pods from node",
			"node", klog.KObj(srcNode.node), "usage", srcNode.usage,
			"allPods", len(srcNode.allPods), "nonRemovablePods", len(nonRemovablePods), "removablePods", len(removablePods))

		if len(removablePods) == 0 {
			klog.V(4).InfoS("No removable pods on node, try next node", "node", klog.KObj(srcNode.node))
			continue
		}

		sorter.SortPodsByUsage(
			removablePods,
			srcNode.podMetrics,
			map[string]corev1.ResourceList{srcNode.node.Name: srcNode.node.Status.Allocatable},
			sorter.GenDefaultResourceToWeightMap(resourceNames),
		)
		evictPods(ctx, dryRun, removablePods, srcNode, totalAvailableUsages, podEvictor, podFilter, continueEviction, evictionReasonGenerator)
	}
}

func evictPods(
	ctx context.Context,
	dryRun bool,
	inputPods []*corev1.Pod,
	nodeInfo NodeInfo,
	totalAvailableUsages map[corev1.ResourceName]*resource.Quantity,
	podEvictor framework.Evictor,
	podFilter framework.FilterFunc,
	continueEviction continueEvictionCond,
	evictionReasonGenerator evictionReasonGeneratorFn,
) {
	for _, pod := range inputPods {
		if !continueEviction(nodeInfo, totalAvailableUsages) {
			return
		}

		if !podFilter(pod) {
			klog.V(4).InfoS("Pod aborted eviction because it was filtered by filters", "pod", klog.KObj(pod))
			continue
		}
		if dryRun {
			klog.InfoS("Evict pod in dry run mode", "pod", klog.KObj(pod))
		} else {
			evictionOptions := framework.EvictOptions{
				Reason: evictionReasonGenerator(nodeInfo),
			}
			if !podEvictor.Evict(ctx, pod, evictionOptions) {
				klog.InfoS("Failed to Evict Pod", "pod", klog.KObj(pod))
				continue
			}
			klog.InfoS("Evicted Pod", "pod", klog.KObj(pod))
		}

		podMetric := nodeInfo.podMetrics[types.NamespacedName{Namespace: pod.Namespace, Name: pod.Name}]
		if podMetric == nil {
			klog.V(4).InfoS("Failed to find PodMetric", "pod", klog.KObj(pod))
			continue
		}
		for resourceName, availableUsage := range totalAvailableUsages {
			var quantity resource.Quantity
			if resourceName == corev1.ResourcePods {
				quantity = *resource.NewQuantity(1, resource.DecimalSI)
			} else {
				quantity = podMetric.ResourceList[resourceName]
			}
			availableUsage.Sub(quantity)
			if nodeUsage := nodeInfo.usage[resourceName]; nodeUsage != nil {
				nodeUsage.Sub(quantity)
			}
		}

		keysAndValues := []interface{}{
			"node", nodeInfo.node.Name,
		}
		for k, v := range nodeInfo.usage {
			keysAndValues = append(keysAndValues, k, v.String())
		}
		for resourceName, quantity := range totalAvailableUsages {
			keysAndValues = append(keysAndValues, fmt.Sprintf("%s/totalAvailable", resourceName), quantity.String())
		}

		klog.V(4).InfoS("Updated node usage", keysAndValues...)
	}
}

// sortNodesByUsage sorts nodes based on usage.
func sortNodesByUsage(nodes []NodeInfo, resourceToWeightMap sorter.ResourceToWeightMap, ascending bool) {
	scorer := sorter.ResourceUsageScorer(resourceToWeightMap)
	sort.Slice(nodes, func(i, j int) bool {
		var iNodeUsage, jNodeUsage corev1.ResourceList
		if nodeMetric := nodes[i].nodeMetric.Status.NodeMetric; nodeMetric != nil {
			iNodeUsage = nodeMetric.NodeUsage.ResourceList
		}
		if nodeMetric := nodes[j].nodeMetric.Status.NodeMetric; nodeMetric != nil {
			jNodeUsage = nodeMetric.NodeUsage.ResourceList
		}

		iScore := scorer(iNodeUsage, nodes[i].node.Status.Allocatable)
		jScore := scorer(jNodeUsage, nodes[j].node.Status.Allocatable)
		if ascending {
			return iScore < jScore
		}
		return iScore > jScore
	})
}

func isNodeOverutilized(usage, thresholds map[corev1.ResourceName]*resource.Quantity) (corev1.ResourceList, bool) {
	// At least one resource has to be above the threshold
	overutilizedResources := corev1.ResourceList{}
	for resourceName, threshold := range thresholds {
		if used := usage[resourceName]; used != nil {
			if used.Cmp(*threshold) > 0 {
				overutilizedResources[resourceName] = *used
			}
		}
	}
	return overutilizedResources, len(overutilizedResources) > 0
}

func isNodeUnderutilized(usage, thresholds map[corev1.ResourceName]*resource.Quantity) bool {
	// All resources have to be below the low threshold
	for resourceName, threshold := range thresholds {
		if used := usage[resourceName]; used != nil {
			if used.Cmp(*threshold) > 0 {
				return false
			}
		}
	}
	return true
}

func getResourceNames(thresholds ResourceThresholds) []corev1.ResourceName {
	names := make([]corev1.ResourceName, 0, len(thresholds))
	for resourceName := range thresholds {
		names = append(names, resourceName)
	}
	return names
}

func classifyPods(pods []*corev1.Pod, filter func(pod *corev1.Pod) bool) ([]*corev1.Pod, []*corev1.Pod) {
	var nonRemovablePods, removablePods []*corev1.Pod

	for _, pod := range pods {
		if !filter(pod) {
			nonRemovablePods = append(nonRemovablePods, pod)
		} else {
			removablePods = append(removablePods, pod)
		}
	}

	return nonRemovablePods, removablePods
}

func calcAverageResourceUsagePercent(nodeUsages map[string]*NodeUsage) ResourceThresholds {
	allUsedPercentages := ResourceThresholds{}
	for _, nodeUsage := range nodeUsages {
		usage := nodeUsage.usage
		allocatable := nodeUsage.node.Status.Allocatable
		for resourceName, used := range usage {
			total := allocatable[resourceName]
			if total.IsZero() {
				continue
			}
			if resourceName == corev1.ResourceCPU {
				allUsedPercentages[resourceName] += Percentage(used.MilliValue()) / Percentage(total.MilliValue()) * 100.0
			} else {
				allUsedPercentages[resourceName] += Percentage(used.Value()) / Percentage(total.Value()) * 100.0
			}
		}
	}

	average := ResourceThresholds{}
	numberOfNodes := len(nodeUsages)
	for resourceName, totalPercentage := range allUsedPercentages {
		average[resourceName] = totalPercentage / Percentage(numberOfNodes)
	}
	return average
}
