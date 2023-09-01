/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kubernetes

import (
	"context"
	"encoding/json"

	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/strategicpatch"
	"k8s.io/client-go/kubernetes"
	"k8s.io/klog/v2"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

// SyncServices synchronizes service objects.
func SyncServices(kubeClient kubernetes.Interface, services []*corev1.Service) error {
	for i := range services {
		if err := SyncService(kubeClient, services[i]); err != nil {
			return err
		}
	}
	return nil
}

// SyncService synchronizes service object.
func SyncService(kubeClient kubernetes.Interface, svc *corev1.Service) error {
	oldSVC, err := kubeClient.CoreV1().Services(svc.Namespace).
		Get(context.Background(), svc.Name, metav1.GetOptions{})
	if err != nil {
		if !apierrors.IsNotFound(err) {
			klog.Errorf("get service (%v) failed: %v", utils.UniqueName(svc), err)
			return err
		}
		// try to create a new service.
		if _, err = kubeClient.CoreV1().Services(svc.Namespace).
			Create(context.Background(), svc, metav1.CreateOptions{}); err != nil {
			klog.Errorf("create service (%v) failed: %v", utils.UniqueName(svc), err)
			return err
		}
		return nil
	}
	// if it already exists, try to update it through patch method.
	return PatchService(kubeClient, oldSVC, svc)
}

// mergeSVC merges old and new service, and generates a service to be updated.
// Service objects are special, because we can only update the following fields and if we merge
// original and new object directly, it may clear the '.spec.ClusterIP field', which is forbidden.
func mergeSVC(oldSVC, newSVC *corev1.Service) *corev1.Service {
	updatedSVC := oldSVC.DeepCopy()
	updatedSVC.Labels = newSVC.Labels
	updatedSVC.Annotations = newSVC.Annotations
	updatedSVC.OwnerReferences = newSVC.OwnerReferences
	updatedSVC.Spec.Type = newSVC.Spec.Type
	updatedSVC.Spec.Selector = newSVC.Spec.Selector
	updatedSVC.Spec.Ports = newSVC.Spec.Ports
	return updatedSVC
}

// PatchService patches the old service to new service.
func PatchService(kubeClient kubernetes.Interface,
	oldSVC, newSVC *corev1.Service) error {
	var oldData []byte
	var err error
	oldData, err = json.Marshal(oldSVC)
	if err != nil {
		klog.Errorf("marshal oldSVC (%+v) failed: %v", oldSVC, err)
		return err
	}
	var newData []byte
	newData, err = json.Marshal(mergeSVC(oldSVC, newSVC))
	if err != nil {
		klog.Errorf("marshal newSVC (%+v) failed: %v", newSVC, err)
		return err
	}
	// build payload for patch method.
	var patchBytes []byte
	patchBytes, err = strategicpatch.CreateTwoWayMergePatch(oldData, newData,
		&corev1.Service{})
	if err != nil {
		klog.Errorf("created merge patch for service %v failed: %v", utils.UniqueName(oldSVC), err)
		return err
	}
	if _, err = kubeClient.CoreV1().Services(oldSVC.Namespace).Patch(context.Background(),
		oldSVC.Name, types.StrategicMergePatchType, patchBytes, metav1.PatchOptions{}); err != nil {
		klog.Errorf("patch service (%v) with (%v) failed: %v",
			utils.UniqueName(oldSVC), string(patchBytes), err)
		return err
	}
	return nil
}
