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

// CreateNamespace Crate namespace on specified cluster.
func (k *Kubernetes) CreateNamespace(clusterName string, namespace *corev1.Namespace) error {
	if namespace == nil {
		return fmt.Errorf("namespace entity can't be nil")
	}

	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.CoreV1().Namespaces().Create(context.Background(), namespace, v1.CreateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s create namespace %s error, %v", clusterName, namespace.Name, err)
	}
	return nil
}

// DeleteNamespace Delete namespace on specified cluster.
func (k *Kubernetes) DeleteNamespace(clusterName string, namespace string) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	err = client.ClientSet.CoreV1().Namespaces().Delete(context.Background(), namespace, v1.DeleteOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s delete namespace %s error, %v", clusterName, namespace, err)
	}

	return nil
}
