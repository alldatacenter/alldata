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
	"encoding/json"
	"testing"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/envconf"
)

func TestAutoTestRunWait(t *testing.T) {
	jsonStr := `{"waitTimeSec": 2}`
	envMap := map[string]string{
		"ACTION_WAIT_TIME_SEC": "2",
	}

	var (
		jsonWait AutoTestRunWait
		envWait  AutoTestRunWait
	)
	err := json.Unmarshal([]byte(jsonStr), &jsonWait)
	assert.NoError(t, err)
	err = envconf.Load(&envWait, envMap)
	assert.NoError(t, err)
	assert.Equal(t, 2, jsonWait.WaitTimeSec)
	assert.Equal(t, 2, envWait.WaitTimeSec)
}

func TestToJsonCopyText(t *testing.T) {
	jsonStr := `{
	"method": "",
	"name": "abc",
	"type": "API",
	"value": "{\"apiSpec\":{\"asserts\":[],\"body\":{\"content\":null,\"type\":\"\"},\"headers\":null,\"id\":\"\",\"method\":\"GET\",\"name\":\"a\",\"out_params\":[],\"params\":[{\"key\":\"d\",\"value\":\"${{ params.a }}\"}],\"url\":\"www.xxx.com?d=${{ params.a }}\"},\"loop\":null}"
}`
	step := AutoTestSceneStep{
		Name:      "abc",
		SpaceID:   1,
		SceneID:   1,
		CreatorID: "123",
		Type:      StepTypeAPI,
		Value:     "{\"apiSpec\":{\"asserts\":[],\"body\":{\"content\":null,\"type\":\"\"},\"headers\":null,\"id\":\"\",\"method\":\"GET\",\"name\":\"a\",\"out_params\":[],\"params\":[{\"key\":\"d\",\"value\":\"${{ params.a }}\"}],\"url\":\"www.xxx.com?d=${{ params.a }}\"},\"loop\":null}",
	}

	assert.Equal(t, jsonStr, step.ToJsonCopyText())
}

func TestIsEffectiveStepType(t *testing.T) {
	tt := []struct {
		t    StepAPIType
		want bool
	}{
		{StepTypeAPI, true},
		{StepTypeCustomScript, true},
		{StepTypeConfigSheet, true},
		{StepTypeWait, false},
		{"", false},
	}
	for _, v := range tt {
		assert.Equal(t, v.want, v.t.IsEffectiveStepType())
	}
}
