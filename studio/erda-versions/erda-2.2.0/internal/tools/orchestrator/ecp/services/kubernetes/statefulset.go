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

// ListStatefulSet List statefulSet under the specified cluster and namespace.
func (k *Kubernetes) ListStatefulSet(clusterName, namespace string) (*appsv1.StatefulSetList, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	cmList, err := client.ClientSet.AppsV1().StatefulSets(namespace).List(context.Background(), v1.ListOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s list statefulSet(namespace: %s) error, %v", clusterName, namespace, err)
	}

	return cmList, nil
}

// GetStatefulSet Get statefulSet under the specified cluster and namespace.
func (k *Kubernetes) GetStatefulSet(clusterName, namespace, statefulSetName string) (*appsv1.StatefulSet, error) {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return nil, fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	cm, err := client.ClientSet.AppsV1().StatefulSets(namespace).Get(context.Background(), statefulSetName, v1.GetOptions{})
	if err != nil {
		return nil, fmt.Errorf("cluster %s get statefulSet(namespace: %s) %s error, %v", clusterName, namespace, statefulSetName, err)
	}

	return cm, nil
}

// CreateStatefulSet Crate statefulSet on specified cluster and namespace.
func (k *Kubernetes) CreateStatefulSet(clusterName, namespace string, cm *appsv1.StatefulSet) error {
	if cm == nil {
		return fmt.Errorf("statefulSet entity can't be nil")
	}
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.AppsV1().StatefulSets(namespace).Create(context.Background(), cm, v1.CreateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s create statefulSet(namespace: %s) %s error, %v", clusterName, namespace, cm.Name, err)
	}

	return nil
}

// DeleteStatefulSet Delete statefulSet on specified cluster and namespace.
func (k *Kubernetes) DeleteStatefulSet(clusterName, namespace, statefulSetName string) error {
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	err = client.ClientSet.AppsV1().StatefulSets(namespace).Delete(context.Background(), statefulSetName, v1.DeleteOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s delete statefulSet(namespace: %s) %s error, %v", clusterName, namespace, statefulSetName, err)
	}

	return nil
}

// UpdateStatefulSet Update statefulSet on specified cluster and namespace.
func (k *Kubernetes) UpdateStatefulSet(clusterName, namespace string, cm *appsv1.StatefulSet) error {
	if cm == nil {
		return fmt.Errorf("statefulSet entity can't be nil")
	}
	client, err := k.getK8sClient(clusterName)
	if err != nil {
		return fmt.Errorf("get cluster %s clientset error, %v", clusterName, err)
	}

	_, err = client.ClientSet.AppsV1().StatefulSets(namespace).Update(context.Background(), cm, v1.UpdateOptions{})
	if err != nil {
		return fmt.Errorf("cluster %s update statefulSet(namespace: %s) %s error, %v", clusterName, namespace, cm.Name, err)
	}

	return nil
}
