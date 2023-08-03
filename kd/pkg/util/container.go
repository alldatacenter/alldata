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
	"fmt"
	"strings"

	corev1 "k8s.io/api/core/v1"

	apiext "github.com/koordinator-sh/koordinator/apis/extension"
)

func GetEmptyContainerExtendedResources() *apiext.ExtendedResourceContainerSpec {
	return &apiext.ExtendedResourceContainerSpec{
		Requests: corev1.ResourceList{},
		Limits:   corev1.ResourceList{},
	}
}

func GetContainerExtendedResources(container *corev1.Container) *apiext.ExtendedResourceContainerSpec {
	return GetContainerTargetExtendedResources(container, ExtendedResourceNames...)
}

// GetContainerTargetExtendedResources gets the resource requirements of a container with given extended resources.
// It returns nil if container is nil or specifies no extended resource.
func GetContainerTargetExtendedResources(container *corev1.Container, resourceNames ...corev1.ResourceName) *apiext.ExtendedResourceContainerSpec {
	if container == nil {
		return nil
	}

	r := GetEmptyContainerExtendedResources()

	for _, name := range resourceNames {
		// assert container.Resources.Requests != nil && container.Resources.Limits != nil
		q, ok := container.Resources.Requests[name]
		if ok {
			r.Requests[name] = q
		}
		q, ok = container.Resources.Limits[name]
		if ok {
			r.Limits[name] = q
		}
	}

	if len(r.Requests) <= 0 && len(r.Limits) <= 0 { // no requirement of specified extended resources
		return nil
	}

	return r
}

func GetContainerMilliCPULimit(c *corev1.Container) int64 {
	if cpuLimit, ok := c.Resources.Limits[corev1.ResourceCPU]; ok {
		return cpuLimit.MilliValue()
	}
	return -1
}

func GetContainerMemoryByteLimit(c *corev1.Container) int64 {
	if memLimit, ok := c.Resources.Limits[corev1.ResourceMemory]; ok {
		return memLimit.Value()
	}
	return -1
}

func FindContainerIdAndStatusByName(status *corev1.PodStatus, name string) (string, *corev1.ContainerStatus, error) {
	allStatuses := status.InitContainerStatuses
	allStatuses = append(allStatuses, status.ContainerStatuses...)
	for _, container := range allStatuses {
		if container.Name == name && container.ContainerID != "" {
			_, cID, err := ParseContainerId(container.ContainerID)
			if err != nil {
				return "", nil, err
			}
			return cID, &container, nil
		}
	}
	return "", nil, fmt.Errorf("unable to find ID for container with name %v in pod status (it may not be running)", name)
}

func ParseContainerId(data string) (cType, cID string, err error) {
	// Trim the quotes and split the type and ID.
	parts := strings.Split(strings.Trim(data, "\""), "://")
	if len(parts) != 2 {
		err = fmt.Errorf("invalid container ID: %q", data)
		return
	}
	cType, cID = parts[0], parts[1]
	return
}
