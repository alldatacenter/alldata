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

package grabber

import (
	"encoding/json"
	"fmt"
	"os"
	"testing"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/services/cms"
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
}

// func TestGather(t *testing.T) {
// 	g, err := New("acs_rds_dashboard", time.Second*1, "1", 0)
// 	if err != nil {
// 		t.Error(err)
// 		return
// 	}
// 	pipe := make(chan []*api.Metric)
// 	g.Subscribe(pipe)
// 	go func() {
// 		for data := range pipe {
// 			for _, d := range data {
// 				fmt.Printf("*************%+v*************\n", d)
// 			}
// 		}
// 	}()
//
// 	g.Gather()
// }

func TestExtractDataPoints(t *testing.T) {
	dp := "[{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp1wu32409l920l0t\",\"Maximum\":1.6,\"Minimum\":1.6,\"Average\":1.6},{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp1yd8e54355eb400\",\"Maximum\":0.15,\"Minimum\":0.15,\"Average\":0.15},{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp1p4wb6181in436c\",\"Maximum\":49.06,\"Minimum\":49.06,\"Average\":49.06},{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp17ar40w6824r8m0\",\"Maximum\":26.27,\"Minimum\":26.27,\"Average\":26.27},{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp1239m7qt632xkrz\",\"Maximum\":0.5,\"Minimum\":0.5,\"Average\":0.5},{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp18wzw6bp9ht15o5\",\"Maximum\":0.3,\"Minimum\":0.3,\"Average\":0.3},{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp1my6p0k1qo6d7a6\",\"Maximum\":0.55,\"Minimum\":0.55,\"Average\":0.55},{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp14zka4fs768ujyx\",\"Maximum\":0.4,\"Minimum\":0.4,\"Average\":0.4},{\"timestamp\":1593762660000,\"userId\":\"1356642369236709\",\"instanceId\":\"rm-bp186lqo231az52m1\",\"Maximum\":0.08,\"Minimum\":0.08,\"Average\":0.08}]"
	g := &Grabber{Name: "rds"}
	res := g.extractDataPoints(dp, cms.Resource{MetricName: "testMetricName", Statistics: "Maximum,Minimum,Average"})
	for _, r := range res {
		buf, _ := json.Marshal(r)
		t.Logf("%s", string(buf))
	}
}

func TestXXX(t *testing.T) {
	now := time.Now()
	fmt.Println(now.Add(-time.Hour * 24 * 7))
}
