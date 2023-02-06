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

package cluster

import (
	"strconv"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster/clusterutil"
	"github.com/erda-project/erda/pkg/jsonstore"
)

func TestSetDefaultClusterConfig(t *testing.T) {
	c1 := ClusterInfo{
		ClusterName: "x1",
		Kind:        "MARATHON",
		Options: map[string]string{
			"ADDR":      "http://dcos.x1.cn/service/marathon",
			"ENABLETAG": "true",
		},
	}

	setDefaultClusterConfig(&c1)
	assert.Equal(t, "true", c1.Options["ENABLETAG"])
	assert.Equal(t, "true", c1.Options["ENABLE_ORG"])
	assert.Equal(t, "true", c1.Options["ENABLE_WORKSPACE"])

	c2 := ClusterInfo{
		ClusterName: "x2",
		Kind:        "MARATHON",
		Options: map[string]string{
			"ADDR": "http://dcos.x2.cn/service/marathon",
		},
	}

	setDefaultClusterConfig(&c2)
	assert.Equal(t, "true", c2.Options["ENABLETAG"])
	assert.Equal(t, "true", c2.Options["ENABLE_ORG"])
	assert.Equal(t, "true", c2.Options["ENABLE_WORKSPACE"])

	c3 := ClusterInfo{
		ClusterName: "x3",
		Kind:        "METRONOME",
		Options: map[string]string{
			"ADDR": "http://dcos.x3.cn/service/metronome",
		},
	}

	setDefaultClusterConfig(&c3)
	assert.Equal(t, "true", c3.Options["ENABLETAG"])
	assert.Equal(t, "true", c3.Options["ENABLE_ORG"])
	assert.Equal(t, "true", c3.Options["ENABLE_WORKSPACE"])
}

func TestCreateEdasExecutor(t *testing.T) {
	cl := ClusterImpl{
		js: nil,
	}
	ce := apistructs.ClusterEvent{
		Content: apistructs.ClusterInfo{
			ID:             0,
			Name:           "fake-cluster",
			DisplayName:    "fake-cluster",
			Type:           "fake-cluster",
			CloudVendor:    "fake-cluster",
			Logo:           "fake-cluster",
			Description:    "fake-cluster",
			WildcardDomain: "fake-cluster",
			SchedConfig: &apistructs.ClusterSchedConfig{
				MasterURL:                "fake-cluster",
				AuthType:                 "fake-cluster",
				AuthUsername:             "fake-cluster",
				AuthPassword:             "fake-cluster",
				CACrt:                    "fake-cluster",
				ClientCrt:                "fake-cluster",
				ClientKey:                "fake-cluster",
				EnableTag:                false,
				EdasConsoleAddr:          "fake-cluster",
				AccessKey:                "fake-cluster",
				AccessSecret:             "fake-cluster",
				ClusterID:                "fake-cluster",
				RegionID:                 "fake-cluster",
				LogicalRegionID:          "fake-cluster",
				K8sAddr:                  "fake-cluster",
				RegAddr:                  "fake-cluster:5000",
				CPUSubscribeRatio:        "fake-cluster",
				DevCPUSubscribeRatio:     "fake-cluster",
				TestCPUSubscribeRatio:    "fake-cluster",
				StagingCPUSubscribeRatio: "fake-cluster",
			},
			OpsConfig:    nil,
			System:       nil,
			ManageConfig: nil,
		},
	}
	generateExecutorByClusterPatch := monkey.Patch(clusterutil.GenerateExecutorByCluster, func(cluster, executorType string) string {
		return "fake-cluster"
	})
	createEdasExectorPatch := monkey.Patch(createEdasExector, func(js jsonstore.JsonStore, key string, c string) error {
		return nil
	})
	createPatch := monkey.Patch(create, func(js jsonstore.JsonStore, key string, c ClusterInfo) error {
		return nil
	})
	defer createEdasExectorPatch.Unpatch()
	defer generateExecutorByClusterPatch.Unpatch()
	defer createPatch.Unpatch()

	err := cl.createEdasExecutor(&ce)
	assert.Equal(t, err, nil)
}

func TestCreateK8SExecutor(t *testing.T) {
	cl := ClusterImpl{
		js: nil,
	}
	ce := apistructs.ClusterEvent{
		Content: apistructs.ClusterInfo{
			ID:             0,
			Name:           "fake-cluster",
			DisplayName:    "fake-cluster",
			Type:           "fake-cluster",
			CloudVendor:    "fake-cluster",
			Logo:           "fake-cluster",
			Description:    "fake-cluster",
			WildcardDomain: "fake-cluster",
			SchedConfig: &apistructs.ClusterSchedConfig{
				MasterURL:                "fake-cluster",
				AuthType:                 "fake-cluster",
				AuthUsername:             "fake-cluster",
				AuthPassword:             "fake-cluster",
				CACrt:                    "fake-cluster",
				ClientCrt:                "fake-cluster",
				ClientKey:                "fake-cluster",
				EnableTag:                false,
				EdasConsoleAddr:          "fake-cluster",
				AccessKey:                "fake-cluster",
				AccessSecret:             "fake-cluster",
				ClusterID:                "fake-cluster",
				RegionID:                 "fake-cluster",
				LogicalRegionID:          "fake-cluster",
				K8sAddr:                  "fake-cluster",
				RegAddr:                  "fake-cluster:5000",
				CPUSubscribeRatio:        "fake-cluster",
				DevCPUSubscribeRatio:     "fake-cluster",
				TestCPUSubscribeRatio:    "fake-cluster",
				StagingCPUSubscribeRatio: "fake-cluster",
			},
			OpsConfig:    nil,
			System:       nil,
			ManageConfig: nil,
		},
	}
	generateExecutorByClusterPatch := monkey.Patch(clusterutil.GenerateExecutorByCluster, func(cluster, executorType string) string {
		return "fake-cluster"
	})
	createEdasExectorPatch := monkey.Patch(createEdasExector, func(js jsonstore.JsonStore, key string, c string) error {
		return nil
	})
	createPatch := monkey.Patch(create, func(js jsonstore.JsonStore, key string, c ClusterInfo) error {
		return nil
	})
	defer createEdasExectorPatch.Unpatch()
	defer generateExecutorByClusterPatch.Unpatch()
	defer createPatch.Unpatch()

	err := cl.createK8SExecutor(&ce)
	assert.Equal(t, err, nil)
}

func TestGenerateClusterInfoFromEvent(t *testing.T) {
	ci := apistructs.ClusterInfo{
		ID:             0,
		Name:           "fake-cluster",
		DisplayName:    "fake-cluster",
		Type:           "fake-cluster",
		CloudVendor:    "fake-cluster",
		Logo:           "fake-cluster",
		Description:    "fake-cluster",
		WildcardDomain: "fake-cluster",
		SchedConfig: &apistructs.ClusterSchedConfig{
			MasterURL:                "fake-cluster",
			AuthType:                 "basic",
			AuthUsername:             "fake-cluster",
			AuthPassword:             "fake-cluster",
			CACrt:                    "fake-cluster",
			ClientCrt:                "fake-cluster",
			ClientKey:                "fake-cluster",
			EnableTag:                false,
			EdasConsoleAddr:          "fake-cluster",
			AccessKey:                "fake-cluster",
			AccessSecret:             "fake-cluster",
			ClusterID:                "fake-cluster",
			RegionID:                 "fake-cluster",
			LogicalRegionID:          "fake-cluster",
			K8sAddr:                  "fake-cluster",
			RegAddr:                  "fake-cluster:5000",
			CPUSubscribeRatio:        "fake-cluster",
			DevCPUSubscribeRatio:     "fake-cluster",
			TestCPUSubscribeRatio:    "fake-cluster",
			StagingCPUSubscribeRatio: "fake-cluster",
		},
		OpsConfig:    nil,
		System:       nil,
		ManageConfig: nil,
	}
	generateExecutorByClusterPatch := monkey.Patch(clusterutil.GenerateExecutorByCluster, func(cluster, executorType string) string {
		return "fake-cluster"
	})

	defer generateExecutorByClusterPatch.Unpatch()

	gcr := generateClusterInfoFromEvent(&ci)
	assert.Equal(t, "fake-cluster", gcr.Options["CA_CRT"])
	assert.Equal(t, "fake-cluster", gcr.Options["CLIENT_CRT"])
	assert.Equal(t, "fake-cluster", gcr.Options["CLIENT_KEY"])
	assert.Equal(t, "fake-cluster:fake-cluster", gcr.Options["BASICAUTH"])
	assert.Equal(t, "fake-cluster", gcr.Options["ADDR"])
	assert.Equal(t, strconv.FormatBool(false), gcr.Options["ENABLETAG"])
}

func TestPatchEdasConfig(t *testing.T) {
	ci := apistructs.ClusterInfo{
		ID:             0,
		Name:           "fake-cluster",
		DisplayName:    "fake-cluster",
		Type:           "fake-cluster",
		CloudVendor:    "fake-cluster",
		Logo:           "fake-cluster",
		Description:    "fake-cluster",
		WildcardDomain: "fake-cluster",
		SchedConfig: &apistructs.ClusterSchedConfig{
			MasterURL:                "fake-cluster",
			AuthType:                 "basic",
			AuthUsername:             "fake-cluster",
			AuthPassword:             "fake-cluster",
			CACrt:                    "fake-cluster",
			ClientCrt:                "fake-cluster",
			ClientKey:                "fake-cluster",
			EnableTag:                false,
			EdasConsoleAddr:          "fake-cluster",
			AccessKey:                "fake-cluster",
			AccessSecret:             "fake-cluster",
			ClusterID:                "fake-cluster",
			RegionID:                 "fake-cluster",
			LogicalRegionID:          "fake-cluster",
			K8sAddr:                  "fake-cluster",
			RegAddr:                  "fake-cluster:5000",
			CPUSubscribeRatio:        "fake-cluster",
			DevCPUSubscribeRatio:     "fake-cluster",
			TestCPUSubscribeRatio:    "fake-cluster",
			StagingCPUSubscribeRatio: "fake-cluster",
		},
		OpsConfig:    nil,
		System:       nil,
		ManageConfig: nil,
	}
	generateExecutorByClusterPatch := monkey.Patch(clusterutil.GenerateExecutorByCluster, func(cluster, executorType string) string {
		return "fake-cluster"
	})

	defer generateExecutorByClusterPatch.Unpatch()

	local := &ClusterInfo{
		Options: make(map[string]string),
	}
	patchEdasConfig(local, &ci)

	assert.Equal(t, "fake-cluster", local.Options["ADDR"])
	assert.Equal(t, "fake-cluster", local.Options["ACCESSKEY"])
	assert.Equal(t, "fake-cluster", local.Options["ACCESSSECRET"])
	assert.Equal(t, "fake-cluster", local.Options["CLUSTERID"])
	assert.Equal(t, "fake-cluster", local.Options["REGIONID"])
	assert.Equal(t, "fake-cluster", local.Options["LOGICALREGIONID"])
}

func TestPatchK8SConfig(t *testing.T) {
	ci := apistructs.ClusterInfo{
		ID:             0,
		Name:           "fake-cluster",
		DisplayName:    "fake-cluster",
		Type:           "fake-cluster",
		CloudVendor:    "fake-cluster",
		Logo:           "fake-cluster",
		Description:    "fake-cluster",
		WildcardDomain: "fake-cluster",
		SchedConfig: &apistructs.ClusterSchedConfig{
			MasterURL:                "fake-cluster",
			AuthType:                 "basic",
			AuthUsername:             "fake-cluster",
			AuthPassword:             "fake-cluster",
			CACrt:                    "fake-cluster",
			ClientCrt:                "fake-cluster",
			ClientKey:                "fake-cluster",
			EnableTag:                false,
			EdasConsoleAddr:          "fake-cluster",
			AccessKey:                "fake-cluster",
			AccessSecret:             "fake-cluster",
			ClusterID:                "fake-cluster",
			RegionID:                 "fake-cluster",
			LogicalRegionID:          "fake-cluster",
			K8sAddr:                  "fake-cluster",
			RegAddr:                  "fake-cluster:5000",
			CPUSubscribeRatio:        "1.0",
			DevCPUSubscribeRatio:     "2.0",
			TestCPUSubscribeRatio:    "3.0",
			StagingCPUSubscribeRatio: "4.0",
		},
		OpsConfig:    nil,
		System:       nil,
		ManageConfig: nil,
	}

	local := &ClusterInfo{
		Options: make(map[string]string),
	}
	patchK8SConfig(local, &ci)

	assert.Equal(t, "fake-cluster", local.Options["ADDR"])
	assert.Equal(t, "1.0", local.Options["DEV_CPU_SUBSCRIBE_RATIO"])
	assert.Equal(t, "1.0", local.Options["TEST_CPU_SUBSCRIBE_RATIO"])
	assert.Equal(t, "1.0", local.Options["STAGING_CPU_SUBSCRIBE_RATIO"])
}

func TestPatchClusterConfig(t *testing.T) {
	ci := apistructs.ClusterInfo{
		ID:             0,
		Name:           "fake-cluster",
		DisplayName:    "fake-cluster",
		Type:           "fake-cluster",
		CloudVendor:    "fake-cluster",
		Logo:           "fake-cluster",
		Description:    "fake-cluster",
		WildcardDomain: "fake-cluster",
		SchedConfig: &apistructs.ClusterSchedConfig{
			MasterURL:                "fake-cluster",
			AuthType:                 "basic",
			AuthUsername:             "fake-cluster",
			AuthPassword:             "fake-cluster",
			CACrt:                    "fake-cluster",
			ClientCrt:                "fake-cluster",
			ClientKey:                "fake-cluster",
			EnableTag:                false,
			EdasConsoleAddr:          "fake-cluster",
			AccessKey:                "fake-cluster",
			AccessSecret:             "fake-cluster",
			ClusterID:                "fake-cluster",
			RegionID:                 "fake-cluster",
			LogicalRegionID:          "fake-cluster",
			K8sAddr:                  "fake-cluster",
			RegAddr:                  "fake-cluster:5000",
			CPUSubscribeRatio:        "fake-cluster",
			DevCPUSubscribeRatio:     "fake-cluster",
			TestCPUSubscribeRatio:    "fake-cluster",
			StagingCPUSubscribeRatio: "fake-cluster",
		},
		OpsConfig:    nil,
		System:       nil,
		ManageConfig: nil,
	}

	local := &ClusterInfo{
		Kind:    clusterutil.ServiceKindMarathon,
		Options: make(map[string]string),
	}
	patchClusterConfig(local, &ci)

	assert.Equal(t, "fake-cluster/service/marathon", local.Options["ADDR"])
	assert.Equal(t, "fake-cluster", local.Options["CA_CRT"])
	assert.Equal(t, "fake-cluster", local.Options["CLIENT_CRT"])
	assert.Equal(t, "fake-cluster", local.Options["CLIENT_KEY"])
	assert.Equal(t, "fake-cluster:fake-cluster", local.Options["BASICAUTH"])
}
