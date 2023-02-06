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

package executeInfo

import (
	"context"
	"encoding/json"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/autotest"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

func Test_convertReportToConfig(t *testing.T) {
	m := apistructs.PipelineReport{}
	bt := `{"id":123,"pipelineID":123,"type":"auto-test-plan","meta":{"data":"{\"domain\":\"domain\",\"header\":{\"Cookie\":\"cookie\",\"cluster-id\":\"2\",\"cluster-name\":\"name\",\"org\":\"erda\",\"project-id\":\"13\"},\"global\":{\"111\":{\"name\":\"111\",\"type\":\"string\",\"value\":\"111\",\"desc\":\"111\"}}}"},"creatorID":"","updaterID":"","createdAt":"2021-09-03T17:25:48+08:00","updatedAt":"2021-09-03T17:25:48+08:00"}`
	err := json.Unmarshal([]byte(bt), &m)
	assert.NoError(t, err)
	c, err := convertReportToConfig(m)
	assert.NoError(t, err)
	want := apistructs.AutoTestAPIConfig{
		Domain: "domain",
		Header: map[string]string{
			"Cookie":       "cookie",
			"cluster-id":   "2",
			"cluster-name": "name",
			"org":          "erda",
			"project-id":   "13",
		},
		Global: map[string]apistructs.AutoTestConfigItem{
			"111": {
				Name:  "111",
				Type:  "string",
				Value: "111",
				Desc:  "111",
			},
		}}
	assert.Equal(t, want, c)
}

func TestGetApiConfigName(t *testing.T) {
	m := apistructs.PipelineReport{}
	bt := `{"id":123,"pipelineID":123,"type":"auto-test-plan","meta":{"AUTOTEST_DISPLAY_NAME":"执行参数-custom","data":"{\"domain\":\"domain\",\"header\":{\"Cookie\":\"cookie\",\"cluster-id\":\"2\",\"cluster-name\":\"name\",\"org\":\"erda\",\"project-id\":\"13\"},\"global\":{\"111\":{\"name\":\"111\",\"type\":\"string\",\"value\":\"111\",\"desc\":\"111\"}}}"},"creatorID":"","updaterID":"","createdAt":"2021-09-03T17:25:48+08:00","updatedAt":"2021-09-03T17:25:48+08:00"}`
	err := json.Unmarshal([]byte(bt), &m)
	assert.NoError(t, err)
	executeEnv := getApiConfigName(m)
	assert.Equal(t, "执行参数-custom", executeEnv)

	m1 := apistructs.PipelineReport{}
	emptyMeta := `{"id":123,"pipelineID":123,"type":"auto-test-plan","creatorID":"","updaterID":"","createdAt":"2021-09-03T17:25:48+08:00","updatedAt":"2021-09-03T17:25:48+08:00"}`
	err = json.Unmarshal([]byte(emptyMeta), &m1)
	assert.NoError(t, err)
	executeEnv = getApiConfigName(m1)
	assert.Equal(t, "", executeEnv)
}

func TestRender(t *testing.T) {
	bdl := bundle.New()
	m1 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetPipeline",
		func(bdl *bundle.Bundle, pipelineID uint64) (*apistructs.PipelineDetailDTO, error) {
			return &apistructs.PipelineDetailDTO{
				PipelineDTO: apistructs.PipelineDTO{
					Status: apistructs.PipelineStatusSuccess,
				},
			}, nil
		})
	defer m1.Unpatch()

	m2 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetPipelineReportSet",
		func(bdl *bundle.Bundle, pipelineID uint64, types []string) (*apistructs.PipelineReportSet, error) {
			return &apistructs.PipelineReportSet{
				PipelineID: 1,
				Reports: []apistructs.PipelineReport{
					{
						ID:         1,
						PipelineID: 1,
						Type:       apistructs.PipelineReportTypeAutotestPlan,
						Meta: map[string]interface{}{
							"data":                        apistructs.AutoTestAPIConfig{},
							autotest.CmsCfgKeyDisplayName: "execute-env",
						},
					},
				},
			}, nil
		})
	defer m2.Unpatch()

	ctxBdl := context.WithValue(context.Background(), protocol.GlobalInnerKeyCtxBundle.String(), protocol.ContextBundle{Bdl: bdl})
	comp := ComponentFileInfo{}
	err := comp.Render(ctxBdl, &apistructs.Component{State: map[string]interface{}{"pipelineId": 1}}, apistructs.ComponentProtocolScenario{}, apistructs.ComponentEvent{}, &apistructs.GlobalStateData{})
	assert.Equal(t, true, err != nil)
}
