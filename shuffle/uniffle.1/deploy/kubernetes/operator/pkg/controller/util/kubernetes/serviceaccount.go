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

// SyncServiceAccount synchronizes serviceAccount object.
func SyncServiceAccount(kubeClient kubernetes.Interface, sa *corev1.ServiceAccount) error {
	oldSA, err := kubeClient.CoreV1().ServiceAccounts(sa.Namespace).
		Get(context.Background(), sa.Name, metav1.GetOptions{})
	if err != nil {
		if !apierrors.IsNotFound(err) {
			klog.Errorf("get service account (%v) failed: %v", utils.UniqueName(sa), err)
			return err
		}
		// try to create a new serviceAccount.
		if _, err = kubeClient.CoreV1().ServiceAccounts(sa.Namespace).
			Create(context.Background(), sa, metav1.CreateOptions{}); err != nil {
			klog.Errorf("create service account (%v) failed: %v", utils.UniqueName(sa), err)
			return err
		}
		return nil
	}
	// if it already exists, try to update it through patch method.
	return PatchServiceAccount(kubeClient, oldSA, sa)
}

// PatchServiceAccount patches the old serviceAccount to new serviceAccount.
func PatchServiceAccount(kubeClient kubernetes.Interface,
	oldSA, newSA *corev1.ServiceAccount) error {
	var oldData []byte
	var err error
	oldData, err = json.Marshal(oldSA)
	if err != nil {
		klog.Errorf("marshal oldSA (%+v) failed: %v", oldSA, err)
		return err
	}
	var newData []byte
	newData, err = json.Marshal(newSA)
	if err != nil {
		klog.Errorf("marshal newSA (%+v) failed: %v", newSA, err)
		return err
	}
	// build payload for patch method.
	var patchBytes []byte
	patchBytes, err = strategicpatch.CreateTwoWayMergePatch(oldData, newData,
		&corev1.ServiceAccount{})
	if err != nil {
		klog.Errorf("created merge patch for serviceAccount %v failed: %v", utils.UniqueName(oldSA),
			err)
		return err
	}
	if _, err = kubeClient.CoreV1().ServiceAccounts(oldSA.Namespace).Patch(context.Background(),
		oldSA.Name, types.StrategicMergePatchType, patchBytes, metav1.PatchOptions{}); err != nil {
		klog.Errorf("patch serviceAccount (%v) failed: %v", utils.UniqueName(oldSA), err)
	}
	return nil
}
