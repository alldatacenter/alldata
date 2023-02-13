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

// ListNode List node under the specified cluster.
func (k *Kubernetes) ListNode(clusterName string) (*corev1.NodeList, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	nodes, err := client.ClientSet.CoreV1().Nodes().List(context.Background(), v1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s list node error, %v", clusterName, err)
	}

	return nodes, nil
}

// GetNode Get node under the specified cluster.
func (k *Kubernetes) GetNode(clusterName, nodeName string) (*corev1.Node, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	np, err := client.ClientSet.CoreV1().Nodes().Get(context.Background(), nodeName, v1.GetOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s get node %s error, %v", clusterName, nodeName, err)
	}

	return np, nil
}

// CreateNode Create node on specified cluster.
func (k *Kubernetes) CreateNode(clusterName string, node *corev1.Node) (*corev1.Node, error) {
	if node == nil {
		return nil, fmt.Errorf("create action must give a non-nil node entity")
	}

	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	res, err := client.ClientSet.CoreV1().Nodes().Create(context.Background(), node, v1.CreateOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s create node %s error, %v", clusterName, node.Name, err)
	}

	return res, nil
}

// DeleteNode Delete node on specified cluster.
func (k *Kubernetes) DeleteNode(clusterName string, nodeName string) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	err = client.ClientSet.CoreV1().Nodes().Delete(context.Background(), nodeName, v1.DeleteOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s delete node error, %v", clusterName, err)
	}

	return nil
}

// UpdateNode Update node on specified cluster.
func (k *Kubernetes) UpdateNode(clusterName string, node *corev1.Node) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.CoreV1().Nodes().Update(context.Background(), node, v1.UpdateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s update node error, %v", clusterName, err)
	}

	return nil
}
