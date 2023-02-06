// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package kubernetes

import (
	"context"
	"fmt"

	corev1 "k8s.io/api/core/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// ListSecret List secret under the specified cluster and namespace.
func (k *Kubernetes) ListSecret(clusterName, namespace string) (*corev1.SecretList, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	cmList, err := client.ClientSet.CoreV1().Secrets(namespace).List(context.Background(), v1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s list secret(namespace: %s) error, %v", clusterName, namespace, err)
	}

	return cmList, nil
}

// GetSecret Get secret under the specified cluster and namespace.
func (k *Kubernetes) GetSecret(clusterName, namespace, secName string) (*corev1.Secret, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	cm, err := client.ClientSet.CoreV1().Secrets(namespace).Get(context.Background(), secName, v1.GetOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s get secret(namespace: %s) %s error, %v", clusterName, namespace, secName, err)
	}

	return cm, nil
}

// CreateSecret Crate secret on specified cluster and namespace.
func (k *Kubernetes) CreateSecret(clusterName, namespace string, cm *corev1.Secret) error {
	if cm == nil {
		return fmt.Errorf("secret entity can't be nil")
	}
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.CoreV1().Secrets(namespace).Create(context.Background(), cm, v1.CreateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s create secret(namespace: %s) %s error, %v", clusterName, namespace, cm.Name, err)
	}

	return nil
}

// DeleteSecret Delete secret on specified cluster and namespace.
func (k *Kubernetes) DeleteSecret(clusterName, namespace, secName string) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	err = client.ClientSet.CoreV1().Secrets(namespace).Delete(context.Background(), secName, v1.DeleteOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s delete Secret(namespace: %s) %s error, %v", clusterName, namespace, secName, err)
	}

	return nil
}

// UpdateSecret Update secret on specified cluster and namespace.
func (k *Kubernetes) UpdateSecret(clusterName, namespace string, cm *corev1.Secret) error {
	if cm == nil {
		return fmt.Errorf("secret entity can't be nil")
	}
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.CoreV1().Secrets(namespace).Update(context.Background(), cm, v1.UpdateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s update secret(namespace: %s) %s error, %v", clusterName, namespace, cm.Name, err)
	}

	return nil
}
