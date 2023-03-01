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

package inspector

import (
	"context"
	"encoding/json"
	"fmt"

	"gomodules.xyz/jsonpatch/v2"
	admissionv1 "k8s.io/api/admission/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/klog/v2"
	"k8s.io/utils/pointer"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/util"
)

// validateRSS validates the create and update operation towards rss objects.
func (i *inspector) validateRSS(ar *admissionv1.AdmissionReview) *admissionv1.AdmissionReview {
	if !i.ignoreRSS && ar.Request.Operation == admissionv1.Update {
		oldRSS := &unifflev1alpha1.RemoteShuffleService{}
		if err := json.Unmarshal(ar.Request.OldObject.Raw, oldRSS); err != nil {
			klog.Errorf("unmarshal old object of rss (%v) failed: %v",
				string(ar.Request.OldObject.Raw), err)
			return util.AdmissionReviewFailed(ar, err)
		}
		// for security purposes, we forbid updating rss objects when they are in upgrading phase.
		// generally speaking, we should also deny updating when rss is terminating. However, it would introduce more
		// complexity and controller's current terminating logic can tolerate the rss object update.
		if oldRSS.Status.Phase == unifflev1alpha1.RSSUpgrading {
			message := "can not update upgrading rss object: " + utils.UniqueName(oldRSS)
			return util.AdmissionReviewForbidden(ar, message)
		}
	}
	newRSS := &unifflev1alpha1.RemoteShuffleService{}
	if err := json.Unmarshal(ar.Request.Object.Raw, newRSS); err != nil {
		klog.Errorf("unmarshal object of rss (%v) failed: %v",
			string(ar.Request.Object.Raw), err)
		return util.AdmissionReviewFailed(ar, err)
	}
	if err := validateRuntimeClassNames(newRSS, i.kubeClient); err != nil {
		klog.Errorf("validate runtime class of rss (%v) failed: %v",
			utils.UniqueName(newRSS), err)
		return util.AdmissionReviewFailed(ar, err)
	}
	if err := validateCoordinator(newRSS.Spec.Coordinator); err != nil {
		klog.Errorf("validate coordinator config of rss (%v) failed: %v",
			utils.UniqueName(newRSS), err)
		return util.AdmissionReviewFailed(ar, err)
	}
	// validate configurations of logHostPath for shuffle servers.
	shuffleServerLogPath := newRSS.Spec.ShuffleServer.LogHostPath
	if len(shuffleServerLogPath) > 0 && len(newRSS.Spec.ShuffleServer.HostPathMounts[shuffleServerLogPath]) == 0 {
		return util.AdmissionReviewFailed(ar, fmt.Errorf("empty log volume mount path for shuffle servers"))
	}
	// validate configurations of different upgrade modes for shuffle servers.
	upgradeStrategy := newRSS.Spec.ShuffleServer.UpgradeStrategy
	switch upgradeStrategy.Type {
	case unifflev1alpha1.FullUpgrade:
	case unifflev1alpha1.PartitionUpgrade:
		var err error
		if upgradeStrategy.Partition == nil {
			err = fmt.Errorf("empty partition for %v", upgradeStrategy.Type)
		} else if *upgradeStrategy.Partition <= 0 {
			err = fmt.Errorf("invalid partition (%v) for %v", *upgradeStrategy.Partition,
				upgradeStrategy.Type)
		}
		if err != nil {
			return util.AdmissionReviewFailed(ar, err)
		}
	case unifflev1alpha1.SpecificUpgrade:
		if len(upgradeStrategy.SpecificNames) == 0 {
			return util.AdmissionReviewFailed(ar,
				fmt.Errorf("empty specific copies for %v", upgradeStrategy.Type))
		}
	case unifflev1alpha1.FullRestart:
	default:
		return util.AdmissionReviewFailed(ar,
			fmt.Errorf("invalid upgrade stragety type (%v)", upgradeStrategy.Type))
	}
	return util.AdmissionReviewAllow(ar)
}

// mutateNmg mutates the rss object according to our needs.
func (i *inspector) mutateRSS(ar *admissionv1.AdmissionReview) *admissionv1.AdmissionReview {
	rss := &unifflev1alpha1.RemoteShuffleService{}
	if err := json.Unmarshal(ar.Request.Object.Raw, rss); err != nil {
		klog.Errorf("unmarshal object of rss (%v) failed: %v",
			string(ar.Request.Object.Raw), err)
		return util.AdmissionReviewFailed(ar, err)
	}
	patches, err := generateRSSPatches(ar, rss)
	if err != nil {
		klog.Errorf("generate patches for rss (%v) failed: %v", utils.UniqueName(rss), err)
		return util.AdmissionReviewFailed(ar, err)
	}
	// if payload is not empty, we need set patch operations in response.
	if len(patches) > 0 {
		return util.AdmissionReviewWithPatches(ar, patches)
	}
	return util.AdmissionReviewAllow(ar)
}

// generateRSSPatches generates patch payloads for mutating rss objects.
func generateRSSPatches(ar *admissionv1.AdmissionReview,
	rss *unifflev1alpha1.RemoteShuffleService) ([]byte, error) {
	// TODO: add default values for RSS objects.
	if ar.Request.Operation == admissionv1.Create {
		rss.SetFinalizers([]string{constants.RSSFinalizerName})
		rss.Spec.ShuffleServer.Sync = pointer.Bool(false)
	}

	original := ar.Request.Object.Raw
	current, err := json.Marshal(rss)
	if err != nil {
		klog.Errorf("marshal rss (%+v) failed: %v", rss, err)
		return nil, err
	}
	var patches []jsonpatch.Operation
	// build patch payload form mutating rss objects.
	patches, err = jsonpatch.CreatePatch(original, current)
	if err != nil {
		klog.Errorf("create patches for rss (%v) failed: %v", string(current), err)
		return nil, err
	}
	var patchBody []byte
	patchBody, err = json.Marshal(patches)
	if err != nil {
		klog.Errorf("marshal patches (%+v) for rss (%v) failed: %v",
			patches, string(current), err)
		return nil, err
	}
	klog.V(4).Infof("patch body (%v) for rss (%v)", string(patchBody), utils.UniqueName(rss))
	return patchBody, nil
}

// validateCoordinator validates configurations for coordinators.
func validateCoordinator(coordinator *unifflev1alpha1.CoordinatorConfig) error {
	// number of RPCNodePort must equal with number of HTTPNodePort
	if len(coordinator.RPCNodePort) != len(coordinator.HTTPNodePort) ||
		// RPCNodePort/HTTPNodePort could be zero
		(len(coordinator.HTTPNodePort) > 0 && len(coordinator.HTTPNodePort) != int(*coordinator.Count)) {
		return fmt.Errorf("invalid number of http or rpc node ports (%v/%v) <> (%v)",
			len(coordinator.RPCNodePort), len(coordinator.HTTPNodePort), *coordinator.Count)
	}
	if len(coordinator.ExcludeNodesFilePath) == 0 {
		return fmt.Errorf("empty exclude nodes file path for coordinators")
	}
	// validate configurations of logHostPath for coordinators.
	coordinatorLogPath := coordinator.LogHostPath
	if len(coordinatorLogPath) > 0 && len(coordinator.HostPathMounts[coordinatorLogPath]) == 0 {
		return fmt.Errorf("empty log volume mount path for coordinators")
	}
	return nil
}

func validateRuntimeClassNames(rss *unifflev1alpha1.RemoteShuffleService, kubeClient kubernetes.Interface) error {
	if err := validateRuntimeClassName(rss.Spec.Coordinator.RuntimeClassName, kubeClient); err != nil {
		klog.Errorf("failed to get runtime class for coordinator: %v", err)
		return err
	}
	if err := validateRuntimeClassName(rss.Spec.ShuffleServer.RuntimeClassName, kubeClient); err != nil {
		klog.Errorf("failed to get runtime class for shuffleServer: %v", err)
		return err
	}
	return nil
}

func validateRuntimeClassName(runtimeClassName *string, kubeClient kubernetes.Interface) error {
	if runtimeClassName == nil {
		return nil
	}
	_, err := kubeClient.NodeV1().RuntimeClasses().Get(context.TODO(), *runtimeClassName, metav1.GetOptions{})
	if err != nil {
		klog.Errorf("failed to get runtime class %v: %v", *runtimeClassName, err)
	}
	return err
}
