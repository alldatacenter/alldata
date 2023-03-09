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

	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// GenerateNodeKey returns a generated key with given meta
func GenerateNodeKey(node *metav1.ObjectMeta) string {
	return fmt.Sprintf("%v/%v", node.GetNamespace(), node.GetName())
}

// GetNodeAddress get node specified type address.
func GetNodeAddress(node *corev1.Node, addrType corev1.NodeAddressType) (string, error) {
	for _, address := range node.Status.Addresses {
		if address.Type == addrType {
			return address.Address, nil
		}
	}
	return "", fmt.Errorf("no address matched types %v", addrType)
}

// IsNodeAddressTypeSupported determine whether addrType is a supported type.
func IsNodeAddressTypeSupported(addrType corev1.NodeAddressType) bool {
	if addrType == corev1.NodeHostName ||
		addrType == corev1.NodeExternalIP ||
		addrType == corev1.NodeExternalDNS ||
		addrType == corev1.NodeInternalIP ||
		addrType == corev1.NodeInternalDNS {
		return true
	}
	return false
}
