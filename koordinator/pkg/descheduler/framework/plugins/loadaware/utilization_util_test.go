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
	"math"
	"testing"

	"github.com/stretchr/testify/assert"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	slov1alpha1 "github.com/koordinator-sh/koordinator/apis/slo/v1alpha1"
	"github.com/koordinator-sh/koordinator/pkg/descheduler/utils/sorter"
)

var (
	lowPriority      = int32(0)
	highPriority     = int32(10000)
	extendedResource = corev1.ResourceName("example.com/foo")

	testNodeAllocatable = corev1.ResourceList{
		corev1.ResourceCPU:    resource.MustParse("32"),
		corev1.ResourceMemory: resource.MustParse("32Gi"),
		corev1.ResourcePods:   *resource.NewQuantity(110, resource.DecimalSI),
	}

	testNode1 = NodeInfo{
		NodeUsage: &NodeUsage{
			node: &corev1.Node{
				Status: corev1.NodeStatus{
					Allocatable: testNodeAllocatable,
				},
				ObjectMeta: metav1.ObjectMeta{Name: "node1"},
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				Status: slov1alpha1.NodeMetricStatus{
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    *resource.NewMilliQuantity(1730, resource.DecimalSI),
								corev1.ResourceMemory: *resource.NewQuantity(3038982964, resource.BinarySI),
								corev1.ResourcePods:   *resource.NewQuantity(25, resource.BinarySI),
							},
						},
					},
				},
			},
			usage: map[corev1.ResourceName]*resource.Quantity{
				corev1.ResourceCPU:    resource.NewMilliQuantity(1730, resource.DecimalSI),
				corev1.ResourceMemory: resource.NewQuantity(3038982964, resource.BinarySI),
				corev1.ResourcePods:   resource.NewQuantity(25, resource.BinarySI),
			},
		},
	}
	testNode2 = NodeInfo{
		NodeUsage: &NodeUsage{
			node: &corev1.Node{
				Status: corev1.NodeStatus{
					Allocatable: testNodeAllocatable,
				},
				ObjectMeta: metav1.ObjectMeta{Name: "node2"},
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				Status: slov1alpha1.NodeMetricStatus{
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    *resource.NewMilliQuantity(1220, resource.DecimalSI),
								corev1.ResourceMemory: *resource.NewQuantity(3038982964, resource.BinarySI),
								corev1.ResourcePods:   *resource.NewQuantity(11, resource.BinarySI),
							},
						},
					},
				},
			},
			usage: map[corev1.ResourceName]*resource.Quantity{
				corev1.ResourceCPU:    resource.NewMilliQuantity(1220, resource.DecimalSI),
				corev1.ResourceMemory: resource.NewQuantity(3038982964, resource.BinarySI),
				corev1.ResourcePods:   resource.NewQuantity(11, resource.BinarySI),
			},
		},
	}
	testNode3 = NodeInfo{
		NodeUsage: &NodeUsage{
			node: &corev1.Node{
				Status: corev1.NodeStatus{
					Allocatable: testNodeAllocatable,
				},
				ObjectMeta: metav1.ObjectMeta{Name: "node3"},
			},
			nodeMetric: &slov1alpha1.NodeMetric{
				Status: slov1alpha1.NodeMetricStatus{
					NodeMetric: &slov1alpha1.NodeMetricInfo{
						NodeUsage: slov1alpha1.ResourceMap{
							ResourceList: corev1.ResourceList{
								corev1.ResourceCPU:    *resource.NewMilliQuantity(1530, resource.DecimalSI),
								corev1.ResourceMemory: *resource.NewQuantity(5038982964, resource.BinarySI),
								corev1.ResourcePods:   *resource.NewQuantity(20, resource.BinarySI),
							},
						},
					},
				},
			},
			usage: map[corev1.ResourceName]*resource.Quantity{
				corev1.ResourceCPU:    resource.NewMilliQuantity(1530, resource.DecimalSI),
				corev1.ResourceMemory: resource.NewQuantity(5038982964, resource.BinarySI),
				corev1.ResourcePods:   resource.NewQuantity(20, resource.BinarySI),
			},
		},
	}
)

func TestResourceUsagePercentages(t *testing.T) {
	resourceUsagePercentage := resourceUsagePercentages(&NodeUsage{
		node: &corev1.Node{
			Status: corev1.NodeStatus{
				Capacity: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewMilliQuantity(2000, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(3977868*1024, resource.BinarySI),
					corev1.ResourcePods:   *resource.NewQuantity(29, resource.BinarySI),
				},
				Allocatable: corev1.ResourceList{
					corev1.ResourceCPU:    *resource.NewMilliQuantity(1930, resource.DecimalSI),
					corev1.ResourceMemory: *resource.NewQuantity(3287692*1024, resource.BinarySI),
					corev1.ResourcePods:   *resource.NewQuantity(29, resource.BinarySI),
				},
			},
		},
		usage: map[corev1.ResourceName]*resource.Quantity{
			corev1.ResourceCPU:    resource.NewMilliQuantity(1220, resource.DecimalSI),
			corev1.ResourceMemory: resource.NewQuantity(3038982964, resource.BinarySI),
			corev1.ResourcePods:   resource.NewQuantity(11, resource.BinarySI),
		},
	})

	expectedUsageInIntPercentage := map[corev1.ResourceName]float64{
		corev1.ResourceCPU:    63,
		corev1.ResourceMemory: 90,
		corev1.ResourcePods:   37,
	}

	for resourceName, percentage := range expectedUsageInIntPercentage {
		if math.Floor(resourceUsagePercentage[resourceName]) != percentage {
			t.Errorf("Incorrect percentange computation, expected %v, got math.Floor(%v) instead", percentage, resourceUsagePercentage[resourceName])
		}
	}

	t.Logf("resourceUsagePercentage: %#v\n", resourceUsagePercentage)
}

func TestSortNodesByUsageDescendingOrder(t *testing.T) {
	nodeList := []NodeInfo{testNode1, testNode2, testNode3}
	expectedNodeList := []NodeInfo{testNode3, testNode1, testNode2}
	weightMap := sorter.GenDefaultResourceToWeightMap([]corev1.ResourceName{corev1.ResourceCPU, corev1.ResourceMemory, corev1.ResourcePods})
	sortNodesByUsage(nodeList, weightMap, false)

	assert.Equal(t, expectedNodeList, nodeList)
}

func TestSortNodesByUsageAscendingOrder(t *testing.T) {
	nodeList := []NodeInfo{testNode1, testNode2, testNode3}
	expectedNodeList := []NodeInfo{testNode2, testNode1, testNode3}
	weightMap := sorter.GenDefaultResourceToWeightMap([]corev1.ResourceName{corev1.ResourceCPU, corev1.ResourceMemory, corev1.ResourcePods})
	sortNodesByUsage(nodeList, weightMap, true)

	assert.Equal(t, expectedNodeList, nodeList)
}
