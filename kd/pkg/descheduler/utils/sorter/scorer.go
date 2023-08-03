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
	"k8s.io/apimachinery/pkg/api/resource"
)

// ResourceToWeightMap contains resource name and weight.
type ResourceToWeightMap map[corev1.ResourceName]int64

func GenDefaultResourceToWeightMap(resourceNames []corev1.ResourceName) ResourceToWeightMap {
	m := ResourceToWeightMap{}
	for _, resourceName := range resourceNames {
		m[resourceName] = 1
	}
	return m
}

func ResourceUsageScorer(resToWeightMap ResourceToWeightMap) func(requested, allocatable corev1.ResourceList) int64 {
	return func(requested, allocatable corev1.ResourceList) int64 {
		var nodeScore, weightSum int64
		for resourceName, quantity := range requested {
			weight := resToWeightMap[resourceName]
			resourceScore := mostRequestedScore(getResourceValue(resourceName, quantity), getResourceValue(resourceName, allocatable[resourceName]))
			nodeScore += resourceScore * weight
			weightSum += weight
		}
		if weightSum == 0 {
			return 0
		}
		return nodeScore / weightSum
	}
}

func mostRequestedScore(requested, capacity int64) int64 {
	if capacity == 0 {
		return 0
	}
	if requested > capacity {
		// `requested` might be greater than `capacity` because pods with no
		// requests get minimum values.
		requested = capacity
	}

	return (requested * 1000) / capacity
}

func getResourceValue(resourceName corev1.ResourceName, quantity resource.Quantity) int64 {
	if resourceName == corev1.ResourceCPU {
		return quantity.MilliValue()
	}
	return quantity.Value()
}
