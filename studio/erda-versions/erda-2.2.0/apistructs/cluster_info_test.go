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

package apistructs

import (
	"testing"

	"github.com/stretchr/testify/assert"
)

var clusterInfo ClusterInfoData

func init() {
	clusterInfo = ClusterInfoData{
		DICE_PROTOCOL:    "http,https",
		DICE_HTTP_PORT:   "80",
		DICE_HTTPS_PORT:  "443",
		DICE_ROOT_DOMAIN: "dev.terminus.io",
	}
}

func TestClusterInfoData_DiceProtocolIsHTTPS(t *testing.T) {
	clusterInfo := ClusterInfoData{
		DICE_PROTOCOL: "http,https",
	}
	assert.True(t, clusterInfo.DiceProtocolIsHTTPS())
	clusterInfo = ClusterInfoData{
		DICE_PROTOCOL: "http",
	}
	assert.False(t, clusterInfo.DiceProtocolIsHTTPS())
}

func TestClusterInfoData_MustGetPublicURL(t *testing.T) {
	assert.Equal(t, "https://soldier.dev.terminus.io:443", clusterInfo.MustGetPublicURL("soldier"))
}

func TestTransferToClusterInfoData(t *testing.T) {
	dataMap := map[string]string{
		"CLUSTER_DNS":             "127.0.0.1",
		"DICE_CLUSTER_NAME":       "cluster-test",
		"DICE_CLUSTER_TYPE":       "kubernetes",
		"DICE_HTTPS_PORT":         "443",
		"DICE_INSIDE":             "false",
		"DICE_IS_EDGE":            "true",
		"DICE_PROTOCOL":           "https",
		"DICE_ROOT_DOMAIN":        "erda.cloud",
		"DICE_SIZE":               "test",
		"DICE_STORAGE_MOUNTPOINT": "/data",
		"DICE_VERSION":            "1.6",
		"ECI_ENABLE":              "true",
		"ECI_HIT_RATE":            "100",
		"ETCD_MONITOR_URL":        "http://127.0.0.1:2381",
		"GLUSTERFS_MONITOR_URL":   "http://127.0.0.1:24007",
		"IS_FDP_CLUSTER":          "true",
		"KUBERNETES_VENDOR":       "dice",
		"KUBERNETES_VERSION":      "v1.20.13",
		"LB_ADDR":                 "127.0.0.1:80",
		"LB_MONITOR_URL":          "http://127.0.0.1:80",
		"MASTER_ADDR":             "127.0.0.1:6443",
		"MASTER_MONITOR_ADDR":     "127.0.0.1:6443",
		"MASTER_VIP_ADDR":         "127.0.0.1:443",
		"MASTER_VIP_URL":          "https://127.0.0.1:443",
		"NETPORTAL_URL":           "",
		"NEXUS_ADDR":              "https://127.0.0.1:8081",
		"NEXUS_PASSWORD":          "xxx",
		"NEXUS_USERNAME":          "xxx",
		"REGISTRY_ADDR":           "https://127.0.0.1:5000",
		"REGISTRY_PASSWORD":       "xxx",
		"REGISTRY_USERNAME":       "xxx",
	}
	clusterInfoData, err := TransferToClusterInfoData(dataMap)
	assert.NoError(t, err)
	assert.Equal(t, true, clusterInfoData.IsK8S())
	assert.Equal(t, true, clusterInfoData.DiceProtocolIsHTTPS())
	openapiPubicURL := clusterInfoData.MustGetPublicURL("openapi")
	assert.Equal(t, "https://openapi.erda.cloud:443", openapiPubicURL)
}
