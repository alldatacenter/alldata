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

package utils

import (
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func TestApplyOverlay(t *testing.T) {
	target := diceyml.Object{
		Services: map[string]*diceyml.Service{
			"demo": {
				Resources: diceyml.Resources{
					CPU:  1.0,
					Mem:  256,
					Disk: 0,
				},
			},
		},
	}

	var overlay diceyml.Object
	err := json.Unmarshal([]byte(`{"services":{"demo":{"scale":1,"resources":{"cpu":2.0,"mem":512,"disk":0}}}}`), &overlay)
	assert.NoError(t, err)

	ApplyOverlay(&target, &overlay)

	//fmt.Println(source)
	assert.Equal(t, 2.0, target.Services["demo"].Resources.CPU)
	assert.Equal(t, 512, target.Services["demo"].Resources.Mem)
	assert.Equal(t, 0, target.Services["demo"].Resources.Disk)
}

// func TestApplyOverlay2(t *testing.T) {
// 	var target spec.LegacyDice
// 	err := json.Unmarshal([]byte(`
// {"diceVersion":"","name":"","description":"","keywords":null,"website":"","repository":"","logo":"","buildMode":"","jointDebug":false,"endpoints":{"demo":{"context":"","scale":1,"ports":[8082],"environment":{"TERMINUS_APP_NAME":"demo","TERMINUS_TRACE_ENABLE":"true","TRACE_SAMPLE":"1"},"resources":{"cpu":0.1,"mem":256,"disk":0},"modulePath":"","cmd":"","hosts":null,"volumes":null,"image":"docker-registry.registry.marathon.mesos:5000/org-default-default/nwwtest-galaxy:demo-1540189786479971316","commit":"","shouldPackage":false,"health_check":{"http":{"port":"0","path":"","duration":"0"},"exec":{"cmd":"echo 1","duration":"0"}},"unitTest":false}},"commitId":"","commitAuthor":"","commitEmail":"","commitTime":"","commitMessage":"","branch":"feature/demo3","globalEnv":{"TERMINUS_APP_NAME":"demo","TERMINUS_TRACE_ENABLE":"true","TRACE_SAMPLE":"1"},"abilityDeclaration ":null,"runtimeAddonInstanceId":""}
// `), &target)
// 	assert.NoError(t, err)
//
// 	var overlay spec.LegacyDice
// 	err = json.Unmarshal([]byte(`
// {"services":{"demo":{"scale":1,"environment":{"test2":"222","TEST":"1","TERMINUS_APP_NAME":"demo","TERMINUS_TRACE_ENABLE":"true","TRACE_SAMPLE":"1"},"resources":{"cpu":0.2,"mem":512.0,"disk":0.0},"buildpack":{},"shouldPackage":true,"unitTest":false}},"build_mode":"standard"}
// `), &overlay)
// 	assert.NoError(t, err)
//
// 	ApplyOverlay(&target, &overlay)
//
// 	//fmt.Println(target)
// 	assert.Equal(t, 0.2, target.Endpoints["demo"].Resources.CPU)
// 	assert.Equal(t, 512.0, target.Endpoints["demo"].Resources.Mem)
// 	assert.Equal(t, 0.0, target.Endpoints["demo"].Resources.Disk)
// }

func TestConvertToLegacyDice_GlobalEnvShouldNotMerge(t *testing.T) {
	diceYmlContent := `
version: 2.0

envs:
  TERMINUS_APP_NAME: PAMPAS_BLOG
  TERMINUS_TRACE_ENABLE: false
  TERMINUS_APP_NAME: PAMPAS_BLOG
  TRACE_SAMPLE: 1
  MYSQL_DATABASE: blog
  TEST_PARAM: param_value

services:
  user-service:
    deployments:
      replicas: 1
    resources:
      cpu: 0.1
      mem: 512
    envs:
      ONLY: one

environments:
  development:
    envs:
      APP_NAME: pampas-blog-dev
`

	dice, err := diceyml.New([]byte(diceYmlContent), true)
	assert.NoError(t, err)

	assert.NoError(t, dice.MergeEnv("development"))

	serviceEnvs := dice.Obj().Services["user-service"].Envs
	assert.Equal(t, 1, len(serviceEnvs))
	assert.Equal(t, "one", serviceEnvs["ONLY"])

	legacyDice := ConvertToLegacyDice(dice, nil)

	legacyServiceEnvs := legacyDice.Services["user-service"].Environment
	assert.Equal(t, 1, len(legacyServiceEnvs))
	assert.Equal(t, "one", legacyServiceEnvs["ONLY"])

	//fmt.Println(legacyDice)
}

func TestConvertServiceLabels(t *testing.T) {
	groupLabels := map[string]string{
		"JUST_FOR": "TEST",
		"TEST3":    "OK",
		"TEST4":    "FAIL",
	}
	servLabels := map[string]string{
		"TEST5": "HAO",
	}
	serviceName := "MY_LITTLE_SERVICE_NAME"
	serviceLabels := ConvertServiceLabels(groupLabels, servLabels, serviceName)
	assert.Equal(t, "HAO", serviceLabels["TEST5"])
	assert.Equal(t, serviceName, serviceLabels["DICE_SERVICE"])
	assert.Equal(t, serviceName, serviceLabels["DICE_SERVICE_NAME"])
}

func TestBuildDiscoveryConfig(t *testing.T) {
	var sg apistructs.ServiceGroup
	sg.Services = []apistructs.Service{
		{
			Name:  "backend-1",
			Vip:   "backend-1.marathon.dice",
			Ports: []diceyml.ServicePort{{Port: 8080}, {Port: 8090}},
		},
		{
			Name:  "frontend-2",
			Vip:   "frontend-2.marathon.dice",
			Ports: []diceyml.ServicePort{{Port: 8081}},
		},
		{
			Name: "empty",
			Vip:  "empty.dice",
		},
	}
	assert.Equal(t, map[string]string{
		"DICE_DISCOVERY_TEST_PROJECT_BACKEND_1_HOST":   "backend-1.marathon.dice",
		"DICE_DISCOVERY_TEST_PROJECT_BACKEND_1_PORT":   "8080",
		"DICE_DISCOVERY_TEST_PROJECT_BACKEND_1_PORT0":  "8080",
		"DICE_DISCOVERY_TEST_PROJECT_BACKEND_1_PORT1":  "8090",
		"DICE_DISCOVERY_TEST_PROJECT_FRONTEND_2_HOST":  "frontend-2.marathon.dice",
		"DICE_DISCOVERY_TEST_PROJECT_FRONTEND_2_PORT":  "8081",
		"DICE_DISCOVERY_TEST_PROJECT_FRONTEND_2_PORT0": "8081",
	}, BuildDiscoveryConfig("test-project", &sg))
}
