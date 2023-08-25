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

package util

import (
	corev1 "k8s.io/api/core/v1"
	quotav1 "k8s.io/apiserver/pkg/quota/v1"
)

// NOTE: functions in this file can be overwritten for extension

func GetPodMilliCPULimit(pod *corev1.Pod) int64 {
	podCPUMilliLimit := int64(0)
	for _, container := range pod.Spec.Containers {
		containerCPUMilliLimit := GetContainerMilliCPULimit(&container)
		if containerCPUMilliLimit <= 0 {
			return -1
		}
		podCPUMilliLimit += containerCPUMilliLimit
	}
	for _, container := range pod.Spec.InitContainers {
		containerCPUMilliLimit := GetContainerMilliCPULimit(&container)
		if containerCPUMilliLimit <= 0 {
			return -1
		}
		podCPUMilliLimit = MaxInt64(podCPUMilliLimit, containerCPUMilliLimit)
	}
	if podCPUMilliLimit <= 0 {
		return -1
	}
	return podCPUMilliLimit
}

func GetPodRequest(pod *corev1.Pod, resourceNames ...corev1.ResourceName) corev1.ResourceList {
	result := corev1.ResourceList{}
	for _, container := range pod.Spec.Containers {
		result = quotav1.Add(result, container.Resources.Requests)
	}
	// take max_resource(sum_pod, any_init_container)
	for _, container := range pod.Spec.InitContainers {
		result = quotav1.Max(result, container.Resources.Requests)
	}
	// add pod overhead if it exists
	if pod.Spec.Overhead != nil {
		result = quotav1.Add(result, pod.Spec.Overhead)
	}
	if len(resourceNames) > 0 {
		result = quotav1.Mask(result, resourceNames)
	}
	return result
}

func GetPodBEMilliCPURequest(pod *corev1.Pod) int64 {
	podCPUMilliReq := int64(0)
	// TODO: count init containers and pod overhead
	for _, container := range pod.Spec.Containers {
		containerCPUMilliReq := GetContainerBatchMilliCPURequest(&container)
		if containerCPUMilliReq <= 0 {
			containerCPUMilliReq = 0
		}
		podCPUMilliReq += containerCPUMilliReq
	}

	return podCPUMilliReq
}

func GetPodBEMilliCPULimit(pod *corev1.Pod) int64 {
	podCPUMilliLimit := int64(0)
	// TODO: count init containers and pod overhead
	for _, container := range pod.Spec.Containers {
		containerCPUMilliLimit := GetContainerBatchMilliCPULimit(&container)
		if containerCPUMilliLimit <= 0 {
			return -1
		}
		podCPUMilliLimit += containerCPUMilliLimit
	}
	if podCPUMilliLimit <= 0 {
		return -1
	}
	return podCPUMilliLimit
}

func GetPodBEMemoryByteRequestIgnoreUnlimited(pod *corev1.Pod) int64 {
	podMemoryByteRequest := int64(0)
	// TODO: count init containers and pod overhead
	for _, container := range pod.Spec.Containers {
		containerMemByteRequest := GetContainerBatchMemoryByteRequest(&container)
		if containerMemByteRequest < 0 {
			// consider request of unlimited container as 0
			continue
		}
		podMemoryByteRequest += containerMemByteRequest
	}
	return podMemoryByteRequest
}

func GetPodBEMemoryByteLimit(pod *corev1.Pod) int64 {
	podMemoryByteLimit := int64(0)
	// TODO: count init containers and pod overhead
	for _, container := range pod.Spec.Containers {
		containerMemByteLimit := GetContainerBatchMemoryByteLimit(&container)
		if containerMemByteLimit <= 0 {
			return -1
		}
		podMemoryByteLimit += containerMemByteLimit
	}
	if podMemoryByteLimit <= 0 {
		return -1
	}
	return podMemoryByteLimit
}

// AddResourceList adds the resources in newList to list.
func AddResourceList(list, newList corev1.ResourceList) {
	for name, quantity := range newList {
		if value, ok := list[name]; !ok {
			list[name] = quantity.DeepCopy()
		} else {
			value.Add(quantity)
			list[name] = value
		}
	}
}
