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

package executeTaskTable

import (
	"encoding/json"
	"testing"

	"github.com/alecthomas/assert"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/apistructs"
)

func TestComponentFilter_ImportExport(t *testing.T) {
	res := apistructs.AutoTestSceneStep{}
	var b []byte = []byte("{\"id\":13,\"type\":\"SCENE\",\"method\":\"\",\"value\":\"{\\\"runParams\\\":{},\\\"sceneID\\\":45}\",\"name\":\"嵌套：Dice 官网\",\"preID\":0,\"preType\":\"Serial\",\"sceneID\":45,\"spaceID\":42,\"creatorID\":\"\",\"updaterID\":\"\",\"Children\":null}")
	err := json.Unmarshal(b, &res)
	t.Log(res)
	assert.NoError(t, err)

}

type MockTran struct {
	i18n.Translator
}

func (m *MockTran) Text(lang i18n.LanguageCodes, key string) string {
	return ""
}

func (m *MockTran) Sprintf(lang i18n.LanguageCodes, key string, args ...interface{}) string {
	return ""
}

func TestSetData(t *testing.T) {
	table := ExecuteTaskTable{
		State: State{PageNo: 1, PageSize: 1},
		Data:  map[string]interface{}{},
		sdk: &cptype.SDK{
			Tran: &MockTran{},
		},
	}
	p := apistructs.PipelineDetailDTO{
		PipelineStages: []apistructs.PipelineStageDetailDTO{
			{
				PipelineTasks: []apistructs.PipelineTaskDTO{
					{
						Labels: map[string]string{apistructs.AutotestSceneStep: "eyJpZCI6MSwidHlwZSI6IkNVU1RPTSIsIm1ldGhvZCI6IiIsInZhbHVlIjoie1wiY29tbWFuZHNcIjpbXCJzbGVlcCAzNlwiXSxcImltYWdlXCI6XCJyZWdpc3RyeS5lcmRhLmNsb3VkL2VyZGEtYWN0aW9ucy9jdXN0b20tc2NyaXB0LWFjdGlvbjoyMDIxMDUxOS0wMWQyODExXCJ9IiwibmFtZSI6Iua1i+ivleiHquWumuS5iSIsInByZUlEIjowLCJwcmVUeXBlIjoiU2VyaWFsIiwic2NlbmVJRCI6MSwic3BhY2VJRCI6MSwiY3JlYXRvcklEIjoiIiwidXBkYXRlcklEIjoiIiwiQ2hpbGRyZW4iOm51bGwsImFwaVNwZWNJRCI6MH0="},
						ID:     1,
					},
				},
			},
		},
	}
	err := table.setData(&p)
	assert.Equal(t, nil, err)

	waiP := apistructs.PipelineDetailDTO{
		PipelineStages: []apistructs.PipelineStageDetailDTO{
			{
				PipelineTasks: []apistructs.PipelineTaskDTO{
					{
						Labels: map[string]string{
							apistructs.AutotestSceneStep: "eyJpZCI6MTI2NTQsInR5cGUiOiJXQUlUIiwibWV0aG9kIjoiIiwidmFsdWUiOiJ7XG4gICAgXCJ3YWl0VGltZVwiOiAxMFxufSIsIm5hbWUiOiLnrYnlvoXku7vliqEiLCJwcmVJRCI6MCwicHJlVHlwZSI6IlNlcmlhbCIsInNjZW5lSUQiOjE2MTAsInNwYWNlSUQiOjEsImNyZWF0b3JJRCI6IiIsInVwZGF0ZXJJRCI6IiIsIkNoaWxkcmVuIjpudWxsLCJhcGlTcGVjSUQiOjB9",
							apistructs.AutotestType:      apistructs.AutotestSceneStep,
						},
						ID: 2,
					},
				},
			},
		},
	}
	err = table.setData(&waiP)
	assert.Equal(t, nil, err)
}
