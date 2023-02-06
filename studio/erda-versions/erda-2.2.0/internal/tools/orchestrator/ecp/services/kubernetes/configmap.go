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

// ListConfigMap List configMap under the specified cluster and namespace.
func (k *Kubernetes) ListConfigMap(clusterName, namespace string) (*corev1.ConfigMapList, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	cmList, err := client.ClientSet.CoreV1().ConfigMaps(namespace).List(context.Background(), v1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s list configmap(namespace: %s) error, %v", clusterName, namespace, err)
	}

	return cmList, nil
}

// GetConfigMap Get configMap under the specified cluster and namespace.
func (k *Kubernetes) GetConfigMap(clusterName, namespace, cmName string) (*corev1.ConfigMap, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	cm, err := client.ClientSet.CoreV1().ConfigMaps(namespace).Get(context.Background(), cmName, v1.GetOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s get configmap(namespace: %s) %s error, %v", clusterName, namespace, cmName, err)
	}

	return cm, nil
}

// CreateConfigMap Crate configMap on specified cluster and namespace.
func (k *Kubernetes) CreateConfigMap(clusterName, namespace string, cm *corev1.ConfigMap) error {
	if cm == nil {
		return fmt.Errorf("configmap entity can't be nil")
	}
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.CoreV1().ConfigMaps(namespace).Create(context.Background(), cm, v1.CreateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s create configmap(namespace: %s) %s error, %v", clusterName, namespace, cm.Name, err)
	}

	return nil
}

// DeleteConfigMap Delete configMap on specified cluster and namespace.
func (k *Kubernetes) DeleteConfigMap(clusterName, namespace, cmName string) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	err = client.ClientSet.CoreV1().ConfigMaps(namespace).Delete(context.Background(), cmName, v1.DeleteOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s delete configmap(namespace: %s) %s error, %v", clusterName, namespace, cmName, err)
	}

	return nil
}

// UpdateConfigMap Update configMap on specified cluster and namespace.
func (k *Kubernetes) UpdateConfigMap(clusterName, namespace string, cm *corev1.ConfigMap) error {
	if cm == nil {
		return fmt.Errorf("configmap entity can't be nil")
	}
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.CoreV1().ConfigMaps(namespace).Update(context.Background(), cm, v1.UpdateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s update configmap(namespace: %s) %s error, %v", clusterName, namespace, cm.Name, err)
	}

	return nil
}
