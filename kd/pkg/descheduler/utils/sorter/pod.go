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

package sorter

import (
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/types"
	schedulingcorev1helper "k8s.io/component-helpers/scheduling/corev1"
	apiscorehelper "k8s.io/kubernetes/pkg/apis/core/helper"

	"github.com/koordinator-sh/koordinator/apis/extension"
	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/util"
)

var koordPriorityClassOrder = map[extension.PriorityClass]int{
	extension.PriorityNone:  5,
	extension.PriorityProd:  4,
	extension.PriorityMid:   3,
	extension.PriorityBatch: 2,
	extension.PriorityFree:  1,
}

var koordQoSClassOrder = map[extension.QoSClass]int{
	extension.QoSNone:   5,
	extension.QoSSystem: 4,
	extension.QoSLSE:    4,
	extension.QoSLSR:    3,
	extension.QoSLS:     2,
	extension.QoSBE:     1,
}

var k8sQoSClassOrder = map[corev1.PodQOSClass]int{
	corev1.PodQOSGuaranteed: 3,
	corev1.PodQOSBurstable:  2,
	corev1.PodQOSBestEffort: 1,
}

// Priority compares pods by Priority
func Priority(p1, p2 *corev1.Pod) int {
	priority1 := schedulingcorev1helper.PodPriority(p1)
	priority2 := schedulingcorev1helper.PodPriority(p2)
	if priority1 == priority2 {
		return 0
	}
	if priority1 > priority2 {
		return 1
	}
	return -1
}

// KubernetesQoSClass compares pods by Kubernetes QosClass
func KubernetesQoSClass(p1, p2 *corev1.Pod) int {
	qos1 := k8sQoSClassOrder[util.GetKubeQosClass(p1)]
	qos2 := k8sQoSClassOrder[util.GetKubeQosClass(p2)]
	if qos1 == qos2 {
		return 0
	}
	if qos1 > qos2 {
		return 1
	}
	return -1
}

// KoordinatorQoSClass compares pods by the Koordinator QoSClass
func KoordinatorQoSClass(p1, p2 *corev1.Pod) int {
	qos1 := koordQoSClassOrder[extension.GetPodQoSClass(p1)]
	qos2 := koordQoSClassOrder[extension.GetPodQoSClass(p2)]
	if qos1 == qos2 {
		return 0
	}
	if qos1 > qos2 {
		return 1
	}
	return -1
}

// KoordinatorPriorityClass compares pods by the Koordinator PriorityClass
func KoordinatorPriorityClass(p1, p2 *corev1.Pod) int {
	priorityClass1 := koordPriorityClassOrder[extension.GetPriorityClass(p1)]
	priorityClass2 := koordPriorityClassOrder[extension.GetPriorityClass(p2)]
	if priorityClass1 == priorityClass2 {
		return 0
	}
	if priorityClass1 > priorityClass2 {
		return 1
	}
	return -1
}

// PodUsage compares pods by the actual usage
func PodUsage(podMetrics map[types.NamespacedName]*slov1alpha1.ResourceMap, nodeAllocatableMap map[string]corev1.ResourceList, resourceToWeightMap ResourceToWeightMap) CompareFn {
	scorer := ResourceUsageScorer(resourceToWeightMap)
	return func(p1, p2 *corev1.Pod) int {
		p1Metric, p1Found := podMetrics[types.NamespacedName{Namespace: p1.Namespace, Name: p1.Name}]
		p2Metric, p2Found := podMetrics[types.NamespacedName{Namespace: p2.Namespace, Name: p2.Name}]
		if !p1Found || !p2Found {
			return cmpBool(!p1Found, !p2Found)
		}
		p1Score := scorer(p1Metric.ResourceList, nodeAllocatableMap[p1.Spec.NodeName])
		p2Score := scorer(p2Metric.ResourceList, nodeAllocatableMap[p2.Spec.NodeName])
		if p1Score == p2Score {
			return 0
		}
		if p1Score > p2Score {
			return 1
		}
		return -1
	}
}

// PodCreationTimestamp compares the pods by the creation timestamp
func PodCreationTimestamp(p1, p2 *corev1.Pod) int {
	if p1.CreationTimestamp.Equal(&p2.CreationTimestamp) {
		return 0
	}
	if p1.CreationTimestamp.Before(&p2.CreationTimestamp) {
		return 1
	}
	return -1
}

func PodDeletionCost(p1, p2 *corev1.Pod) int {
	p1DeletionCost, _ := apiscorehelper.GetDeletionCostFromPodAnnotations(p1.Annotations)
	p2DeletionCost, _ := apiscorehelper.GetDeletionCostFromPodAnnotations(p2.Annotations)
	if p1DeletionCost == p2DeletionCost {
		return 0
	}
	if p1DeletionCost > p2DeletionCost {
		return 1
	}
	return -1
}

func EvictionCost(p1, p2 *corev1.Pod) int {
	p1EvictionCost, _ := extension.GetEvictionCost(p1.Annotations)
	p2EvictionCost, _ := extension.GetEvictionCost(p2.Annotations)
	if p1EvictionCost == p2EvictionCost {
		return 0
	}
	if p1EvictionCost > p2EvictionCost {
		return 1
	}
	return -1
}

func PodSorter(cmp ...CompareFn) *MultiSorter {
	comparators := []CompareFn{
		KoordinatorPriorityClass,
		Priority,
		KubernetesQoSClass,
		KoordinatorQoSClass,
		PodDeletionCost,
		EvictionCost,
	}
	comparators = append(comparators, cmp...)
	comparators = append(comparators, PodCreationTimestamp)
	return OrderedBy(comparators...)
}

func SortPodsByUsage(pods []*corev1.Pod, podMetrics map[types.NamespacedName]*slov1alpha1.ResourceMap, nodeAllocatableMap map[string]corev1.ResourceList, resourceToWeightMap ResourceToWeightMap) {
	PodSorter(Reverse(PodUsage(podMetrics, nodeAllocatableMap, resourceToWeightMap))).Sort(pods)
}
