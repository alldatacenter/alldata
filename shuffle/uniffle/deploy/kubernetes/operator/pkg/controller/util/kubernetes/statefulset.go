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

	appsv1 "k8s.io/api/apps/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/strategicpatch"
	"k8s.io/client-go/kubernetes"
	"k8s.io/klog/v2"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
)

// SyncStatefulSet synchronizes statefulSet object and returns original and patched object.
func SyncStatefulSet(kubeClient kubernetes.Interface, sts *appsv1.StatefulSet,
	overwrite bool) (oldSts, newSts *appsv1.StatefulSet, err error) {
	oldSts, err = kubeClient.AppsV1().StatefulSets(sts.Namespace).
		Get(context.Background(), sts.Name, metav1.GetOptions{})
	if err != nil {
		if !apierrors.IsNotFound(err) {
			klog.Errorf("get statefulSet (%v) failed: %v", utils.UniqueName(sts), err)
			return nil, nil, err
		}
		var created *appsv1.StatefulSet
		created, err = kubeClient.AppsV1().StatefulSets(sts.Namespace).
			Create(context.Background(), sts, metav1.CreateOptions{})
		if err != nil {
			klog.Errorf("create statefulSet (%v) failed: %v", utils.UniqueName(sts), err)
			return nil, nil, err
		}
		return nil, created, nil
	}
	// determine if we need to force an update.
	if !overwrite {
		return oldSts, nil, nil
	}
	// if it already exists, try to update it through patch method.
	newSts, err = PatchStatefulSet(kubeClient, oldSts, sts)
	return oldSts, newSts, err
}

// NeedUpdateSts returns whether we need to update the statefulSet.
func NeedUpdateSts(oldSts, newSts *appsv1.StatefulSet) ([]byte, bool, error) {
	var oldData []byte
	var err error
	oldData, err = json.Marshal(oldSts)
	if err != nil {
		klog.Errorf("marshal oldSts (%+v) failed: %v", oldSts, err)
		return nil, false, err
	}
	var newData []byte
	newData, err = json.Marshal(newSts)
	if err != nil {
		klog.Errorf("marshal newSts (%+v) failed: %v", newSts, err)
		return nil, false, err
	}
	var patchBytes []byte
	patchBytes, err = strategicpatch.CreateTwoWayMergePatch(oldData, newData,
		&appsv1.StatefulSet{})
	if err != nil {
		klog.Errorf("created merge patch for statefulSet %v failed: %v",
			utils.UniqueName(oldSts), err)
		return nil, false, err
	}
	return patchBytes, string(patchBytes) != "{}", nil
}

// PatchStatefulSet patches the old statefulSet to new statefulSet.
func PatchStatefulSet(kubeClient kubernetes.Interface,
	oldSts, newSts *appsv1.StatefulSet) (*appsv1.StatefulSet, error) {
	patchBytes, update, err := NeedUpdateSts(oldSts, newSts)
	if err != nil {
		return nil, err
	}
	if !update {
		klog.V(4).Infof("do not need to patch statefulSet (%v)", utils.UniqueName(oldSts))
		return nil, nil
	}
	klog.V(5).Infof("patch body (%v) to statefulSet (%v)",
		string(patchBytes), utils.UniqueName(oldSts))
	// build payload for patch method.
	var patched *appsv1.StatefulSet
	patched, err = kubeClient.AppsV1().StatefulSets(oldSts.Namespace).Patch(context.Background(),
		oldSts.Name, types.StrategicMergePatchType, patchBytes,
		metav1.PatchOptions{})
	if err != nil {
		klog.Errorf("patch statefulSet (%v) failed: %v", utils.UniqueName(oldSts), err)
		return nil, err
	}
	return patched, nil
}
