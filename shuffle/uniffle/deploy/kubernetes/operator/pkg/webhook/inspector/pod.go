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
	"strings"

	admissionv1 "k8s.io/api/admission/v1"
	corev1 "k8s.io/api/core/v1"
	apierrors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/sets"
	"k8s.io/klog/v2"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/util"
)

// validateDeletingShuffleServer validates the delete operation towards shuffle server pods,
// and update exclude nodes in configMap.
func (i *inspector) validateDeletingShuffleServer(ar *admissionv1.AdmissionReview) *admissionv1.AdmissionReview {
	pod := &corev1.Pod{}
	if err := json.Unmarshal(ar.Request.OldObject.Raw, pod); err != nil {
		klog.Errorf("unmarshal object of AdmissionReview (%v) failed: %v",
			string(ar.Request.Object.Raw), err)
		return util.AdmissionReviewFailed(ar, err)
	}
	rssName := utils.GetRssNameByPod(pod)
	// allow pods which are not shuffle servers or have been set deletion timestamp.
	if rssName == "" || !util.NeedInspectPod(pod) {
		klog.V(4).Infof("ignored non shuffle server or deleting pod: %v->(%+v/%+v/%v)",
			utils.UniqueName(pod), pod.Labels, pod.Annotations, pod.DeletionTimestamp)
		return util.AdmissionReviewAllow(ar)
	}
	klog.V(4).Infof("check shuffle server pod: %v", utils.BuildShuffleServerKey(pod))
	rss, err := i.rssLister.RemoteShuffleServices(pod.Namespace).Get(rssName)
	if err != nil {
		if apierrors.IsNotFound(err) {
			return util.AdmissionReviewAllow(ar)
		}
		return util.AdmissionReviewFailed(ar, err)
	}
	// we can only delete shuffle server pods when rss is in upgrading phase.
	if rss.Status.Phase != unifflev1alpha1.RSSUpgrading && rss.Status.Phase != unifflev1alpha1.RSSTerminating {
		message := fmt.Sprintf("can not delete the shuffle server pod (%v) directly",
			utils.UniqueName(pod))
		klog.V(4).Info(message)
		return util.AdmissionReviewForbidden(ar, message)
	}

	// when the rss uses specific upgrade mode, we need to check whether the pod is specific.
	if rss.Spec.ShuffleServer.UpgradeStrategy.Type == unifflev1alpha1.SpecificUpgrade {
		specificNames := rss.Spec.ShuffleServer.UpgradeStrategy.SpecificNames
		isSpecific := false
		for _, name := range specificNames {
			if name == pod.Name {
				isSpecific = true
				break
			}
		}
		if !isSpecific {
			message := fmt.Sprintf("can not delete the shuffle server pod (%v) which is not specific",
				utils.UniqueName(pod))
			klog.V(4).Info(message)
			return util.AdmissionReviewForbidden(ar, message)
		}
	}

	// update targetKeys field in status of rss and exclude nodes in configMap used by coordinators.
	if err = i.updateTargetKeysAndExcludeNodes(rss, pod); err != nil {
		return util.AdmissionReviewFailed(ar, err)
	}

	// check whether the shuffle server pod can be deleted.
	if i.ignoreLastApps || util.HasZeroApps(pod) {
		klog.V(3).Infof("shuffle server pod (%v) will be deleted", utils.BuildShuffleServerKey(pod))
		return util.AdmissionReviewAllow(ar)
	}
	message := "there are some apps still running in shuffle server: " + utils.GetShuffleServerNode(pod)
	return util.AdmissionReviewForbidden(ar, message)
}

// updateTargetKeysAndExcludeNodes updates targetKeys field in status of rss and exclude nodes in
// configMap used by coordinators.
func (i *inspector) updateTargetKeysAndExcludeNodes(rss *unifflev1alpha1.RemoteShuffleService,
	pod *corev1.Pod) error {
	targetKeys := sets.NewString(rss.Status.TargetKeys...)
	deletedKeys := sets.NewString(rss.Status.DeletedKeys...)
	currentKey := utils.BuildShuffleServerKey(pod)
	if deletedKeys.Has(currentKey) {
		klog.V(4).Infof("pod (%v) has been deleted", currentKey)
		return nil
	}

	namespace := rss.Namespace
	targetKeys.Insert(currentKey)
	rssCopy := rss.DeepCopy()
	rssCopy.Status.TargetKeys = utils.GetSortedList(targetKeys)
	if _, err := i.rssClient.UniffleV1alpha1().RemoteShuffleServices(namespace).
		UpdateStatus(context.Background(), rssCopy, metav1.UpdateOptions{}); err != nil {
		klog.Errorf("update target keys in status of rss (%v) failed: %v",
			utils.UniqueName(rss), err)
		return err
	}

	cmName := utils.GenerateCoordinatorName(rss)
	cm, err := i.cmLister.ConfigMaps(namespace).Get(cmName)
	if err != nil {
		klog.Errorf("get configMap (%v/%v) of excluded nodes for rss (%v) failed: %v",
			namespace, cmName, utils.UniqueName(rss), err)
		return err
	}

	excludeNodesFileKey := utils.GetExcludeNodesConfigMapKey(rss)
	oldNodes := sets.NewString(strings.Split(cm.Data[excludeNodesFileKey], "\n")...)
	newNodes := oldNodes.Difference(utils.ConvertShuffleServerKeysToNodes(deletedKeys))
	newNodes.Insert(utils.GetShuffleServerNode(pod))
	cmCopy := cm.DeepCopy()
	cmCopy.Data[excludeNodesFileKey] = strings.Join(utils.GetSortedList(newNodes), "\n")
	if _, err = i.kubeClient.CoreV1().ConfigMaps(namespace).
		Update(context.Background(), cmCopy, metav1.UpdateOptions{}); err != nil {
		klog.Errorf("updated exclude nodes in configMap (%v) for rss (%v) failed: %v",
			utils.UniqueName(cmCopy), utils.UniqueName(rss), err)
		return err
	}
	return nil
}
