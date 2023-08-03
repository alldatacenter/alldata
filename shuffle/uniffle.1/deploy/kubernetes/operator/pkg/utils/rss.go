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
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/utils/pointer"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
)

const (
	defaultRssName              = "rss"
	defaultConfigMapName        = "rss-config"
	defaultCoordinatorSync      = true
	defaultCoordinatorCount     = 2
	defaultExcludeNodesFilePath = "/config/exclude_nodes"
	defaultShuffleServerSync    = false
	defaultRPCPort              = 19997
	defaultHTTPPort             = 19996
	defaultReplicas             = 1
	defaultXmxSize              = "800M"
	defaultConfigDir            = "/data/rssadmin/rss/conf"
	defaultHostNetwork          = true
	defaultMainImage            = "rss-server:latest"
)

var (
	defaultCoordinatorRPCNodePorts  = []int32{30001, 30011}
	defaultCoordinatorHTTPNodePorts = []int32{30002, 30012}
)

// BuildRSSWithDefaultValue builds a rss object with required or default values for testing.
func BuildRSSWithDefaultValue() *unifflev1alpha1.RemoteShuffleService {
	return &unifflev1alpha1.RemoteShuffleService{
		ObjectMeta: metav1.ObjectMeta{
			Name:      defaultRssName,
			Namespace: corev1.NamespaceDefault,
		},
		Spec: unifflev1alpha1.RemoteShuffleServiceSpec{
			Coordinator:   buildCoordinatorConfigWithDefaultValue(),
			ShuffleServer: buildShuffleServerConfigWithDefaultValue(),
			ConfigMapName: defaultConfigMapName,
		},
	}
}

func buildCoordinatorConfigWithDefaultValue() *unifflev1alpha1.CoordinatorConfig {
	return &unifflev1alpha1.CoordinatorConfig{
		CommonConfig:         buildCommonConfig(),
		Sync:                 pointer.Bool(defaultCoordinatorSync),
		Count:                pointer.Int32(defaultCoordinatorCount),
		Replicas:             pointer.Int32(defaultReplicas),
		RPCPort:              pointer.Int32(defaultRPCPort),
		HTTPPort:             pointer.Int32(defaultHTTPPort),
		ExcludeNodesFilePath: defaultExcludeNodesFilePath,
		RPCNodePort:          defaultCoordinatorRPCNodePorts,
		HTTPNodePort:         defaultCoordinatorHTTPNodePorts,
	}
}

func buildShuffleServerConfigWithDefaultValue() *unifflev1alpha1.ShuffleServerConfig {
	return &unifflev1alpha1.ShuffleServerConfig{
		CommonConfig: buildCommonConfig(),
		Sync:         pointer.Bool(defaultShuffleServerSync),
		Replicas:     pointer.Int32(defaultReplicas),
		RPCPort:      pointer.Int32(defaultRPCPort),
		HTTPPort:     pointer.Int32(defaultHTTPPort),
		UpgradeStrategy: &unifflev1alpha1.ShuffleServerUpgradeStrategy{
			Type: unifflev1alpha1.FullUpgrade,
		},
	}
}

func buildCommonConfig() *unifflev1alpha1.CommonConfig {
	return &unifflev1alpha1.CommonConfig{
		RSSPodSpec: &unifflev1alpha1.RSSPodSpec{
			MainContainer: &unifflev1alpha1.MainContainer{
				Image: defaultMainImage,
			},
			HostNetwork: pointer.Bool(defaultHostNetwork),
		},
		XmxSize:   defaultXmxSize,
		ConfigDir: defaultConfigDir,
	}
}
