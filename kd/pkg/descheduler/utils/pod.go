/*
Copyright 2022 The Koordinator Authors.
Copyright 2017 The Kubernetes Authors.

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

package utils

import (
	"fmt"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// GetResourceRequest finds and returns the request value for a specific resource.
func GetResourceRequest(pod *corev1.Pod, resource corev1.ResourceName) int64 {
	if resource == corev1.ResourcePods {
		return 1
	}

	requestQuantity := GetResourceRequestQuantity(pod, resource)
	if resource == corev1.ResourceCPU {
		return requestQuantity.MilliValue()
	}
	return requestQuantity.Value()
}

// GetResourceRequestQuantity finds and returns the request quantity for a specific resource.
func GetResourceRequestQuantity(pod *corev1.Pod, resourceName corev1.ResourceName) resource.Quantity {
	requestQuantity := resource.Quantity{}
	switch resourceName {
	case corev1.ResourceCPU:
		requestQuantity = resource.Quantity{Format: resource.DecimalSI}
	case corev1.ResourceMemory, corev1.ResourceStorage, corev1.ResourceEphemeralStorage:
		requestQuantity = resource.Quantity{Format: resource.BinarySI}
	default:
		requestQuantity = resource.Quantity{Format: resource.DecimalSI}
	}

	for _, container := range pod.Spec.Containers {
		if rQuantity, ok := container.Resources.Requests[resourceName]; ok {
			requestQuantity.Add(rQuantity)
		}
	}

	for _, container := range pod.Spec.InitContainers {
		if rQuantity, ok := container.Resources.Requests[resourceName]; ok {
			if requestQuantity.Cmp(rQuantity) < 0 {
				requestQuantity = rQuantity.DeepCopy()
			}
		}
	}

	// We assume pod overhead feature gate is enabled.
	// We can't import the scheduler settings so we will inherit the default.
	if pod.Spec.Overhead != nil {
		if podOverhead, ok := pod.Spec.Overhead[resourceName]; ok && !requestQuantity.IsZero() {
			requestQuantity.Add(podOverhead)
		}
	}

	return requestQuantity
}

// IsMirrorPod returns true if the pod is a Mirror Pod.
func IsMirrorPod(pod *corev1.Pod) bool {
	_, ok := pod.Annotations[corev1.MirrorPodAnnotationKey]
	return ok
}

// IsPodTerminating returns true if the pod DeletionTimestamp is set.
func IsPodTerminating(pod *corev1.Pod) bool {
	return pod.DeletionTimestamp != nil
}

// IsStaticPod returns true if the pod is a static pod.
func IsStaticPod(pod *corev1.Pod) bool {
	source, err := GetPodSource(pod)
	return err == nil && source != "api"
}

// IsCriticalPriorityPod returns true if the pod has critical priority.
func IsCriticalPriorityPod(pod *corev1.Pod) bool {
	return pod.Spec.Priority != nil && *pod.Spec.Priority >= SystemCriticalPriority
}

// IsDaemonsetPod returns true if the pod is a IsDaemonsetPod.
func IsDaemonsetPod(ownerRefList []metav1.OwnerReference) bool {
	for _, ownerRef := range ownerRefList {
		if ownerRef.Kind == "DaemonSet" {
			return true
		}
	}
	return false
}

// IsPodWithLocalStorage returns true if the pod has local storage.
func IsPodWithLocalStorage(pod *corev1.Pod) bool {
	for _, volume := range pod.Spec.Volumes {
		if volume.HostPath != nil || volume.EmptyDir != nil {
			return true
		}
	}

	return false
}

// IsPodWithPVC returns true if the pod has claimed a persistent volume.
func IsPodWithPVC(pod *corev1.Pod) bool {
	for _, volume := range pod.Spec.Volumes {
		if volume.PersistentVolumeClaim != nil {
			return true
		}
	}
	return false
}

// GetPodSource returns the source of the pod based on the annotation.
func GetPodSource(pod *corev1.Pod) (string, error) {
	if pod.Annotations != nil {
		if source, ok := pod.Annotations["kubernetes.io/config.source"]; ok {
			return source, nil
		}
	}
	return "", fmt.Errorf("cannot get source of pod %q", pod.UID)
}
