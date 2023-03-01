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

package utils

import (
	"path/filepath"

	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/constants"
)

// GenerateCoordinatorName returns service account or configMap name of coordinators.
func GenerateCoordinatorName(rss *unifflev1alpha1.RemoteShuffleService) string {
	return constants.RSSCoordinator + "-" + rss.Name
}

// GetExcludeNodesConfigMapKey returns configMap key of excluded nodes.
func GetExcludeNodesConfigMapKey(rss *unifflev1alpha1.RemoteShuffleService) string {
	return filepath.Base(rss.Spec.Coordinator.ExcludeNodesFilePath)
}

// GetExcludeNodesMountPath returns excluded nodes file's directory which is used as configMap
// volume mouth path.
func GetExcludeNodesMountPath(rss *unifflev1alpha1.RemoteShuffleService) string {
	return filepath.Dir(rss.Spec.Coordinator.ExcludeNodesFilePath)
}

// BuildCoordinatorInformerFactory builds informer factory for objects related to coordinators.
func BuildCoordinatorInformerFactory(kubeClient kubernetes.Interface) informers.SharedInformerFactory {
	option := func(options *metav1.ListOptions) {
		options.LabelSelector = constants.LabelCoordinator + "=true"
	}
	return informers.NewSharedInformerFactoryWithOptions(
		kubeClient, 0, informers.WithTweakListOptions(option))
}
