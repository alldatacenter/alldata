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

package dbclient

import (
	"fmt"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
)

// func TestDBClient_CreateRuntime(t *testing.T) {
// 	client := initDb(t)
//
// 	err := client.CreateRuntime(&Runtime{
// 		ApplicationID: 1,
// 		Creator:       "55",
// 		Workspace:     "TEST",
// 		Name:          "feature/test3",
// 		Status:        "Healthy",
// 		LegacyStatus:  "DELETING",
// 		Deleting:      false,
// 		Version:       "1",
// 		ScheduleName:  ScheduleName{Namespace: "services", Name: "prod-123"},
// 		Source:        "PIPELINE",
// 		DiceVersion:   "2",
// 	})
// 	assert.NoError(t, err)
// }
//
// func TestDBClient_FindRuntimeServices(t *testing.T) {
// 	client := initDb(t)
//
// 	service := RuntimeService{
// 		RuntimeId: 1,
// 		Errors:    `[{"code":"InstanceFailed","msg":"实例 runtimes_v1_services_dev-4_web.c1ba9ec8-eee3-11e8-93c0-02420b542c69 启动失败, Failed/BeforeHealthCheckTimeout","ctx":null}]`,
// 	}
// 	err := client.CreateOrUpdateRuntimeService(&service, true)
// 	if !assert.NoError(t, err) {
// 		return
// 	}
//
// 	rss, err := client.FindRuntimeServices(1)
// 	if assert.NoError(t, err) {
// 		assert.Equal(t, 1, len(rss))
// 	}
//
// 	var errs []apistructs.ErrorResponse
// 	err = json.Unmarshal([]byte(rss[0].Errors), &errs)
// 	if assert.NoError(t, err) {
// 		if assert.Equal(t, 1, len(errs)) {
// 			assert.Equal(t, "InstanceFailed", errs[0].Code)
// 		}
// 	}
// }
//
// func TestDBClient_FindRuntimesByAppId(t *testing.T) {
// 	client := initDb(t)
//
// 	rs, err := client.FindRuntimesByAppId(1)
// 	require.NoError(t, err)
//
// 	fmt.Println(rs)
// }
//
// func initDb(t *testing.T) *DBClient {
// 	os.Setenv("MYSQL_HOST", "127.0.0.1")
// 	os.Setenv("MYSQL_PORT", "3306")
// 	os.Setenv("MYSQL_DATABASE", "orchestrator")
// 	os.Setenv("MYSQL_USERNAME", "root")
// 	client, err := Open()
// 	if assert.NoError(t, err) {
// 		return client
// 	}
// 	t.FailNow()
// 	return nil
// }

func TestInitScheduleName(t *testing.T) {
	r := Runtime{
		ApplicationID: 1,
		Workspace:     "dev",
		Name:          "feature/dev",
	}
	clusterType := "dcos"
	name := md5V(fmt.Sprintf("%d-%s-%s", r.ApplicationID, r.Workspace, r.Name))
	if clusterType == apistructs.EDAS {
		r.ID = 1111
		name = fmt.Sprintf("%s-%d", strings.ToLower(r.Workspace), r.ID)
	}
	fmt.Println(name)
}

func TestFnvV(t *testing.T) {
	s1 := "1-STAGING-hotfix/1"
	s2 := "1-STAGING-hotfix/2"
	str1 := fnvV(s1)
	str11 := fnvV(s1)
	str2 := fnvV(s2)
	assert.NotEqual(t, str1, str2)
	assert.Equal(t, str1, str11)
	assert.Equal(t, 10, len(str1))
	assert.Equal(t, 10, len(str2))
}
