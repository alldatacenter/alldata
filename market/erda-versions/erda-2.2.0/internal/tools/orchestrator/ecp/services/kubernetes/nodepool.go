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

	crClient "sigs.k8s.io/controller-runtime/pkg/client"

	"github.com/erda-project/erda/pkg/clientgo/apis/openyurt/v1alpha1"
)

// ListNodePool List nodePool under the specified cluster.
func (k *Kubernetes) ListNodePool(clusterName string) (*v1alpha1.NodePoolList, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	var nodePools v1alpha1.NodePoolList
	if err = client.CRClient.List(context.Background(), &nodePools); err != nil {
		return nil, fmt.Errorf("cluster %s list nodepool error, %v", clusterName, err)
	}

	return &nodePools, nil
}

// GetNodePool Get nodePool under the specified cluster.
func (k *Kubernetes) GetNodePool(clusterName, npName string) (*v1alpha1.NodePool, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	var np v1alpha1.NodePool
	if err = client.CRClient.Get(context.Background(), crClient.ObjectKey{Name: npName}, &np); err != nil {
		return nil, fmt.Errorf("cluster %s get nodepool error, %v", clusterName, err)
	}

	return &np, nil
}

// CreateNodePool Crate nodePool on specified cluster.
func (k *Kubernetes) CreateNodePool(clusterName string, nodePool *v1alpha1.NodePool) error {
	if nodePool == nil {
		return fmt.Errorf("create action must give a non-nil NodePool entity")
	}

	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	if err = client.CRClient.Create(context.Background(), nodePool); err != nil {
		return fmt.Errorf("cluster %s create nodepool error, %v", clusterName, err)
	}

	return nil
}

// DeleteNodePool Delete node on specified cluster.
func (k *Kubernetes) DeleteNodePool(clusterName string, nodePoolName string) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	np, err := k.GetNodePool(clusterName, nodePoolName)
	if err != nil {
		return err
	}

	if err = client.CRClient.Delete(context.Background(), np); err != nil {
		return fmt.Errorf("cluster %s delete nodepool error, %v", clusterName, err)
	}

	return nil
}

// UpdateNodePool Update nodePool on specified cluster.
func (k *Kubernetes) UpdateNodePool(clusterName string, nodePool *v1alpha1.NodePool) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	if err = client.CRClient.Update(context.Background(), nodePool); err != nil {
		return fmt.Errorf("cluster %s update nodepool error, %v", clusterName, err)
	}

	return nil
}
