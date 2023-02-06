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

	appsv1 "k8s.io/api/apps/v1"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// ListDeployment List deployment under the specified cluster and namespace.
func (k *Kubernetes) ListDeployment(clusterName, namespace string) (*appsv1.DeploymentList, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	cmList, err := client.ClientSet.AppsV1().Deployments(namespace).List(context.Background(), v1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s list deployment(namespace: %s) error, %v", clusterName, namespace, err)
	}

	return cmList, nil
}

// GetDeployment Get deployment under the specified cluster and namespace.
func (k *Kubernetes) GetDeployment(clusterName, namespace, deploymentName string) (*appsv1.Deployment, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	cm, err := client.ClientSet.AppsV1().Deployments(namespace).Get(context.Background(), deploymentName, v1.GetOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s get deployment(namespace: %s) %s error, %v", clusterName, namespace, deploymentName, err)
	}

	return cm, nil
}

// CreateDeployment Crate deployment on specified cluster and namespace.
func (k *Kubernetes) CreateDeployment(clusterName, namespace string, cm *appsv1.Deployment) error {
	if cm == nil {
		return fmt.Errorf("deployment entity can't be nil")
	}
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.AppsV1().Deployments(namespace).Create(context.Background(), cm, v1.CreateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s create deployment(namespace: %s) %s error, %v", clusterName, namespace, cm.Name, err)
	}

	return nil
}

// DeleteDeployment Delete deployment on specified cluster and namespace.
func (k *Kubernetes) DeleteDeployment(clusterName, namespace, deploymentName string) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	err = client.ClientSet.AppsV1().Deployments(namespace).Delete(context.Background(), deploymentName, v1.DeleteOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s delete deployment(namespace: %s) %s error, %v", clusterName, namespace, deploymentName, err)
	}

	return nil
}

// UpdateDeployment Update deployment on specified cluster and namespace.
func (k *Kubernetes) UpdateDeployment(clusterName, namespace string, cm *appsv1.Deployment) error {
	if cm == nil {
		return fmt.Errorf("deployment entity can't be nil")
	}
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.AppsV1().Deployments(namespace).Update(context.Background(), cm, v1.UpdateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s update deployment(namespace: %s) %s error, %v", clusterName, namespace, cm.Name, err)
	}

	return nil
}
