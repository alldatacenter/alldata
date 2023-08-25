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
	"context"
	"fmt"

	policyv1 "k8s.io/api/policy/v1"
	policyv1beta1 "k8s.io/api/policy/v1beta1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
)

const (
	EvictionKind            = "Eviction"
	EvictionGroupName       = "policy"
	EvictionSubResourceName = "pods/eviction"
)

// EvictPodByVersion evicts Pods using the best available method in Kubernetes.
//
// The available methods are, in order of preference:
// * v1 eviction API
// * v1beta1 eviction API
func EvictPodByVersion(ctx context.Context, kubernetes kubernetes.Interface, namespace, name string, opts metav1.DeleteOptions, evictVersion string) error {
	if evictVersion == "v1" {
		return kubernetes.CoreV1().Pods(namespace).EvictV1(ctx, &policyv1.Eviction{
			ObjectMeta: metav1.ObjectMeta{
				Name:      name,
				Namespace: namespace,
			},
			DeleteOptions: &opts,
		})
	}

	if evictVersion == "v1beta1" {
		return kubernetes.CoreV1().Pods(namespace).EvictV1beta1(ctx, &policyv1beta1.Eviction{
			ObjectMeta: metav1.ObjectMeta{
				Name:      name,
				Namespace: namespace,
			},
			DeleteOptions: &opts,
		})
	}

	return fmt.Errorf("not support evict version, %s", evictVersion)
}

func FindSupportedEvictVersion(client kubernetes.Interface) (groupVersion string, err error) {
	var (
		serverGroups     *metav1.APIGroupList
		resourceList     *metav1.APIResourceList
		foundPolicyGroup bool
		preferredVersion string
	)

	discoveryClient := client.Discovery()
	serverGroups, err = discoveryClient.ServerGroups()
	if serverGroups == nil || err != nil {
		return
	}

	for _, serverGroup := range serverGroups.Groups {
		if serverGroup.Name == EvictionGroupName {
			foundPolicyGroup = true
			preferredVersion = serverGroup.PreferredVersion.Version
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
		if resource.Name == EvictionSubResourceName && resource.Kind == EvictionKind {
			groupVersion = preferredVersion
			return
		}
	}

	return
}
