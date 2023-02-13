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

package api

import (
	"os"
	"testing"

	"github.com/erda-project/erda/bundle"
)

const (
	testOrgId = "1"
)

func TestMain(m *testing.M) {
	setup()
	code := m.Run()
	os.Exit(code)
}

func setup() {
	{
		os.Setenv("OPS_ADDR", "ops.default.svc.cluster.local:9027")
		os.Setenv("CMDB_ADDR", "cmdb.default.svc.cluster.local:9093")
	}
	bdl = bundle.New(bundle.WithCMDB())

}

// func TestGetResourcesInDescribeMetricMetaList(t *testing.T) {
// 	resp, err := GetDescribeMetricLast(testOrgId, "acs_rocketmq", "ConsumerLag")
// 	assert.Nil(t, err)
// 	t.Log(len(resp))
// 	for _, item := range resp {
// 		t.Logf("resp: %+v\n", item)
// 	}
// }
//
// func TestGetDescribeMetricLast(t *testing.T) {
// 	resp, err := ListMetricMeta(testOrgId, "acs_rocketmq")
// 	assert.Nil(t, err)
// 	data, _ := json.Marshal(resp)
// 	t.Logf("resp: %+v\n", string(data))
// }
//
// func TestGetDescribeProjectMeta(t *testing.T) {
// 	resp, err := ListProjectMeta(testOrgId, nil)
// 	assert.Nil(t, err)
// 	t.Logf("resp: %+v\n", resp)
// }
//
// func TestGetDescribeSomeProjectMeta(t *testing.T) {
// 	resp, err := ListProjectMeta(testOrgId, []string{"RDS", "ECS"})
// 	assert.Nil(t, err)
// 	assert.Equal(t, 2, len(resp))
// 	t.Logf("resp: %+v\n", resp)
// }
//
// func TestListOrgClusterPair(t *testing.T) {
// 	resp, err := ListOrgInfos()
// 	assert.Nil(t, err)
// 	t.Logf("resp: %+v", resp)
// }
