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

package pipelinesvc

import (
	"encoding/json"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/pkg/expression"
	"github.com/erda-project/erda/pkg/pipeline_snippet_client"
)

func TestHandleQueryPipelineYamlBySnippetConfigs(t *testing.T) {

	var svc = &PipelineSvc{}
	guard := monkey.Patch(pipeline_snippet_client.BatchGetSnippetPipelineYml, func(snippetConfig []apistructs.SnippetConfig) ([]apistructs.BatchSnippetConfigYml, error) {
		var result []apistructs.BatchSnippetConfigYml
		for _, v := range snippetConfig {
			result = append(result, apistructs.BatchSnippetConfigYml{
				Config: v,
				Yml:    v.ToString(),
			})
		}
		return result, nil
	})
	guard1 := monkey.Patch(pipeline_snippet_client.GetSnippetPipelineYml, func(snippetConfig apistructs.SnippetConfig) (string, error) {
		return snippetConfig.ToString(), nil
	})
	defer guard.Unpatch()
	defer guard1.Unpatch()

	var table = []struct {
		sourceSnippetConfigs []apistructs.SnippetConfig
	}{
		{
			sourceSnippetConfigs: []apistructs.SnippetConfig{
				{
					Source: "autotest",
					Name:   "custom",
					Labels: map[string]string{
						"key3": "key",
					},
				},
				{
					Source: "local",
					Name:   "pipeline",
					Labels: map[string]string{
						"key3": "key",
					},
				},
				{
					Source: "autotest",
					Name:   "custom",
					Labels: map[string]string{
						"key1": "key",
					},
				},
				{
					Source: "autotest",
					Name:   "custom",
					Labels: map[string]string{
						"key1": "key",
					},
				},
			},
		},
		{
			sourceSnippetConfigs: []apistructs.SnippetConfig{
				{
					Source: "local",
					Name:   "pipeline",
					Labels: map[string]string{
						"key3": "key",
					},
				},
			},
		},
		{
			sourceSnippetConfigs: []apistructs.SnippetConfig{
				{
					Source: "autotest",
					Name:   "custom",
					Labels: map[string]string{
						"key3": "key",
					},
				},
				{
					Source: "autotest",
					Name:   "custom",
					Labels: map[string]string{
						"key1": "key",
					},
				},
				{
					Source: "autotest",
					Name:   "custom",
					Labels: map[string]string{
						"key1": "key",
					},
				},
			},
		},
		{
			sourceSnippetConfigs: []apistructs.SnippetConfig{},
		},
		{
			sourceSnippetConfigs: nil,
		},
	}
	for _, data := range table {
		_, err := svc.HandleQueryPipelineYamlBySnippetConfigs(data.sourceSnippetConfigs)
		assert.NoError(t, err)
	}
}

func Test_getActionDetail(t *testing.T) {
	var table = []struct {
		config  apistructs.SnippetDetailQuery
		spec    string
		outputs []string
	}{
		{
			config: apistructs.SnippetDetailQuery{
				Alias: "jsonparse",
				SnippetConfig: apistructs.SnippetConfig{
					Name:   "jsonparse",
					Source: apistructs.ActionSourceType,
					Labels: map[string]string{
						"actionJson":    "{\"alias\":\"jsonparse\",\"type\":\"jsonparse\",\"description\":\"对 json 解析值然后返回出参\",\"version\":\"1.0\",\"params\":{\"data\":\"{\\\"aaa\\\": 1}\",\"out_params\":[{\"expression\":\".aaa\",\"key\":\"aaa\"}]},\"resources\":{},\"displayName\":\"json 解析\"}",
						"actionVersion": "1.0",
					},
				},
			},
			outputs: []string{
				"name",
				"name1",
				"key1",
			},
			spec: "name: jsonparse\nversion: '1.0'\ntype: action\ncategory: custom_task\ndisplayName: json 解析\ndesc: 对 json 解析值然后返回出参\npublic: true\nsupportedVersions:\n  - \">= 3.21\"\nlabels:\n  configsheet: true\n\nparams:\n  - name: out_params\n    required: false\n    desc: 出参\n    type: struct_array\n    struct:\n      - name: key\n        required: true\n        desc: 出参名\n      - name: expression\n        required: true\n        desc: 支持 linux jq 表达式 以 . 开头，支持 jackson 以 $. 开头\n  - name: data\n    required: true\n    desc: json 数据\n    \n    \noutputsFromParams:\n  - type: jq\n    keyExpr: \"[.out_params[].key]\"    \n\n",
		},
		{
			config: apistructs.SnippetDetailQuery{
				Alias: "jsonparse",
				SnippetConfig: apistructs.SnippetConfig{
					Name:   "jsonparse",
					Source: apistructs.ActionSourceType,
					Labels: map[string]string{
						"actionJson":    "{\"alias\":\"jsonparse\",\"type\":\"jsonparse\",\"description\":\"对 json 解析值然后返回出参\",\"version\":\"1.0\",\"params\":{\"data\":\"{\\\"aaa\\\": 1}\",\"out_params\":[{\"expression\":\".aaa\",\"key\":\"aaa\"}]},\"resources\":{},\"displayName\":\"json 解析\"}",
						"actionVersion": "1.0",
					},
				},
			},
			spec: "name: jsonparse\nversion: '1.0'\ntype: action\ncategory: custom_task\ndisplayName: json 解析\ndesc: 对 json 解析值然后返回出参\npublic: true\nsupportedVersions:\n  - \">= 3.21\"\nlabels:\n  configsheet: true\n\nparams:\n  - name: out_params\n    required: false\n    desc: 出参\n    type: struct_array\n    struct:\n      - name: key\n        required: true\n        desc: 出参名\n      - name: expression\n        required: true\n        desc: 支持 linux jq 表达式 以 . 开头，支持 jackson 以 $. 开头\n  - name: data\n    required: true\n    desc: json 数据\n    \n    \noutputsFromParams:\n  - type: jq\n    keyExpr: \"[.out_params[].key]\"    \n\n",
		},
		{
			config: apistructs.SnippetDetailQuery{
				Alias: "jsonparse",
				SnippetConfig: apistructs.SnippetConfig{
					Name:   "jsonparse",
					Source: apistructs.ActionSourceType,
					Labels: map[string]string{
						"actionJson":    "{\"alias\":\"jsonparse\",\"type\":\"jsonparse\",\"description\":\"对 json 解析值然后返回出参\",\"version\":\"1.0\",\"params\":{\"data\":\"{\\\"aaa\\\": 1}\",\"out_params\":[{\"expression\":\".aaa\",\"key\":\"aaa\"}]},\"resources\":{},\"displayName\":\"json 解析\"}",
						"actionVersion": "1.0",
					},
				},
			},
			outputs: []string{
				"result",
			},
			spec: "name: jsonparse\nversion: '1.0'\ntype: action\ncategory: custom_task\ndisplayName: json 解析\ndesc: 对 json 解析值然后返回出参\npublic: true\nsupportedVersions:\n  - \">= 3.21\"\nlabels:\n  configsheet: true\n\nparams:\n  - name: out_params\n    required: false\n    desc: 出参\n    type: struct_array\n    struct:\n      - name: key\n        required: true\n        desc: 出参名\n      - name: expression\n        required: true\n        desc: 支持 linux jq 表达式 以 . 开头，支持 jackson 以 $. 开头\n  - name: data\n    required: true\n    desc: json 数据\n\n\noutputs:\n  - name: result",
		},
	}

	var s = &PipelineSvc{}

	for _, data := range table {

		bdl := &bundle.Bundle{}
		s.bdl = bdl
		s.actionMgr = &actionmgr.MockActionMgr{}
		monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "SearchExtensions", func(b *bundle.Bundle, req apistructs.ExtensionSearchRequest) (map[string]apistructs.ExtensionVersion, error) {
			var extensionVersion = map[string]apistructs.ExtensionVersion{}
			for _, v := range req.Extensions {
				extensionVersion[v] = apistructs.ExtensionVersion{
					Spec: data.spec,
				}
			}
			return extensionVersion, nil
		})

		monkey.Patch(handlerActionOutputsWithJq, func(action *apistructs.PipelineYmlAction, jq string) ([]string, error) {
			return data.outputs, nil
		})

		detail, err := s.getActionDetail(data.config)
		assert.NoError(t, err)

		assert.Equal(t, len(data.outputs), len(data.outputs))
		for _, key := range detail.Outputs {
			var find = false
			for _, output := range data.outputs {
				if key == expression.GenOutputRef(data.config.Alias, output) {
					find = true
				}
			}
			assert.True(t, find)
		}
	}
}

func Test_ActionJson(t *testing.T) {

	var str = "{\"alias\":\"account-login\",\"type\":\"api-test\",\"description\":\"执行单个接口测试。上层可以通过 pipeline.yml 编排一组接口测试的执行顺序。\",\"version\":\"2.0\",\"params\":{\"asserts\":[{\"arg\":\"status\",\"operator\":\"=\",\"value\":\"200\"},{\"arg\":\"sessionId\",\"operator\":\"not_empty\"}],\"headers\":[{\"key\":\"Content-Type\",\"value\":\"application/x-www-form-urlencoded\"}],\"method\":\"POST\",\"name\":\"登录\",\"out_params\":[{\"expression\":\".sessionid\",\"key\":\"sessionId\",\"source\":\"body:json\"},{\"key\":\"status\",\"source\":\"status\"},{\"expression\":\".id\",\"key\":\"userId\",\"source\":\"body:json\"}],\"params\":[{\"key\":\"username\",\"value\":\"${params.username}\"},{\"key\":\"password\",\"value\":\"${params.password}\"}],\"url\":\"${params.openapi_addr}/login\"},\"resources\":{},\"displayName\":\"接口测试\",\"logoUrl\":\"//terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/10/10/24195384-07b7-4203-93e1-666373639af4.png\"}"

	var action apistructs.PipelineYmlAction
	err := json.Unmarshal([]byte(str), &action)
	if err != nil {
		t.Fail()
	}

	params := action.Params
	if params == nil {
		t.Fail()
	}

	outParamsBytes, err := json.Marshal(action.Params["out_params"])
	if err != nil {
		t.Fail()
	}

	var outParams []apistructs.APIOutParam
	err = json.Unmarshal(outParamsBytes, &outParams)
	if err != nil {
		t.Fail()
	}
}
