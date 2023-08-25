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

package utils

import (
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	clientset "k8s.io/client-go/kubernetes"
)

const (
	EvictionKind           = "Eviction"
	EvictionGroupName      = "policy"
	EvictionSubResouceName = "pods/eviction"
)

// SupportEviction detect if the K8s server support eviction subresource
// If support, it will return its groupVersion; Otherwise, it will return ""
func SupportEviction(client clientset.Interface) (groupVersion string, err error) {
	var (
		serverGroups          *metav1.APIGroupList
		resourceList          *metav1.APIResourceList
		foundPolicyGroup      bool
		preferredGroupVersion string
	)

	discoveryClient := client.Discovery()
	serverGroups, err = discoveryClient.ServerGroups()
	if serverGroups == nil || err != nil {
		return
	}

	for _, serverGroup := range serverGroups.Groups {
		if serverGroup.Name == EvictionGroupName {
			foundPolicyGroup = true
			preferredGroupVersion = serverGroup.PreferredVersion.GroupVersion
			break
		}
	}
	if !foundPolicyGroup {
		return
	}

	resourceList, err = discoveryClient.ServerResourcesForGroupVersion("v1")
	if err != nil {
		return
	}
	for _, resource := range resourceList.APIResources {
		if resource.Name == EvictionSubResouceName && resource.Kind == EvictionKind {
			groupVersion = preferredGroupVersion
			return
		}
	}

	return
}
