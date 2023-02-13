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

package pipelineyml

import (
	"fmt"
	"strconv"
	"strings"

	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/expression"
)

type ParamsVisitor struct {
	Data              []byte
	RunPipelineParams []apistructs.PipelineRunParam
}

func NewParamsVisitor(data []byte, runPipelineParam []apistructs.PipelineRunParam) *ParamsVisitor {
	v := ParamsVisitor{}
	v.Data = data
	v.RunPipelineParams = runPipelineParam
	return &v
}

func (v *ParamsVisitor) Visit(s *Spec) {
	// 运行时输入参数转化为map
	var runParamsMap = make(map[string]interface{})
	for _, v := range v.RunPipelineParams {
		runParamsMap[v.Name] = v.Value
	}

	replaced := ReplacePipelineParams(string(v.Data), runParamsMap)

	if err := yaml.Unmarshal([]byte(replaced), s); err != nil {
		s.appendError(fmt.Errorf("failed to unmarshal to spec after replaced params, err: %v", err))
	}
}

func ReplacePipelineParams(pipeline string, params map[string]interface{}) string {
	for k, v := range params {
		replaceStr := ""
		switch v.(type) {
		case int:
			replaceStr = strconv.Itoa(v.(int))
		case float64:
			replaceStr = strconv.FormatFloat(v.(float64), 'f', -1, 64)
		case float32:
			replaceStr = strconv.FormatFloat(v.(float64), 'f', -1, 32)
		case bool:
			replaceStr = strconv.FormatBool(v.(bool))
		case string:
			replaceStr = v.(string)
		default:
			replaceStr = fmt.Sprintf("%v", v)
		}

		// generate random params before replace
		replaceStr = expression.ReplaceRandomParams(replaceStr)

		// 替换老的
		pipeline = strings.ReplaceAll(pipeline, fmt.Sprintf("%s%s.%s%s", expression.OldLeftPlaceholder, expression.Params, k, expression.OldRightPlaceholder), replaceStr)

		// 替换新的语法
		pipeline = strings.ReplaceAll(pipeline, fmt.Sprintf("%s %s.%s %s", expression.LeftPlaceholder, expression.Params, k, expression.RightPlaceholder), replaceStr)
	}
	return pipeline
}
