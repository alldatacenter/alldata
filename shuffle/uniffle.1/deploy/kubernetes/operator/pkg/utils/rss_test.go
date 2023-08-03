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
	"testing"

	"github.com/stretchr/testify/assert"
	"k8s.io/utils/pointer"

	unifflev1alpha1 "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/api/uniffle/v1alpha1"
)

func TestBuildRSSWithDefaultValue(t *testing.T) {
	rss := BuildRSSWithDefaultValue()
	assertion := assert.New(t)
	assertion.NotEmpty(rss.Name, "need to specify name of rss")
	assertion.NotEmpty(rss.Namespace, "need to specify namespace of rss")
	assertion.NotEmpty(rss.Spec.Coordinator, "need to configure coordinator")
	assertion.NotEmpty(rss.Spec.ShuffleServer, "need to configure shuffle server")
	assertion.NotEmpty(rss.Spec.ConfigMapName, "need to configure configMap name of rss")

	// check config of coordinator.
	assertion.NotEmpty(rss.Spec.Coordinator.CommonConfig, "need to configure commonConfig of coordinator")
	checkCommonConfig(assertion, rss.Spec.Coordinator.CommonConfig)
	assertion.Equal(rss.Spec.Coordinator.Sync, pointer.Bool(defaultCoordinatorSync))
	assertion.Equal(rss.Spec.Coordinator.Count, pointer.Int32(defaultCoordinatorCount))
	assertion.Equal(rss.Spec.Coordinator.Replicas, pointer.Int32(defaultReplicas))
	assertion.Equal(rss.Spec.Coordinator.RPCPort, pointer.Int32(defaultRPCPort))
	assertion.Equal(rss.Spec.Coordinator.HTTPPort, pointer.Int32(defaultHTTPPort))
	assertion.Equal(rss.Spec.Coordinator.ExcludeNodesFilePath, defaultExcludeNodesFilePath)
	assertion.Equal(rss.Spec.Coordinator.RPCNodePort, defaultCoordinatorRPCNodePorts)
	assertion.Equal(rss.Spec.Coordinator.HTTPNodePort, defaultCoordinatorHTTPNodePorts)

	// check config of shuffle server.
	assertion.NotEmpty(rss.Spec.ShuffleServer.CommonConfig, "need to configure commonConfig of shuffle server")
	checkCommonConfig(assertion, rss.Spec.ShuffleServer.CommonConfig)
	assertion.Equal(rss.Spec.ShuffleServer.Sync, pointer.Bool(defaultShuffleServerSync))
	assertion.Equal(rss.Spec.ShuffleServer.Replicas, pointer.Int32(defaultReplicas))
	assertion.Equal(rss.Spec.ShuffleServer.RPCPort, pointer.Int32(defaultRPCPort))
	assertion.Equal(rss.Spec.ShuffleServer.HTTPPort, pointer.Int32(defaultHTTPPort))
	assertion.Equal(rss.Spec.ShuffleServer.UpgradeStrategy, &unifflev1alpha1.ShuffleServerUpgradeStrategy{
		Type: unifflev1alpha1.FullUpgrade,
	})
}

func checkCommonConfig(assertion *assert.Assertions, commonConfig *unifflev1alpha1.CommonConfig) {
	assertion.Equal(commonConfig.RSSPodSpec, &unifflev1alpha1.RSSPodSpec{
		MainContainer: &unifflev1alpha1.MainContainer{
			Image: defaultMainImage,
		},
		HostNetwork: pointer.Bool(defaultHostNetwork),
	})
	assertion.Equal(commonConfig.XmxSize, defaultXmxSize)
	assertion.Equal(commonConfig.ConfigDir, defaultConfigDir)
}
