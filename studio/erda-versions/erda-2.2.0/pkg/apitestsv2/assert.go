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

package apitestsv2

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/assert"
)

// JudgeAsserts 执行断言测试
func (at *APITest) JudgeAsserts(outParams map[string]interface{}, asserts []apistructs.APIAssert) (bool, []*apistructs.APITestsAssertData) {
	var results []*apistructs.APITestsAssertData
	for _, ast := range asserts {
		// 出参里的值
		actualValue := outParams[ast.Arg]
		//var buffer bytes.Buffer
		//enc := json.NewEncoder(&buffer)
		//enc.SetEscapeHTML(false)
		//enc.Encode(actualValue)
		//actualValueString := buffer.String()
		//if len(actualValueString) > 0 {
		//	actualValueString = actualValueString[:len(actualValueString)-1]
		//}
		succ, err := assert.DoAssert(actualValue, ast.Operator, ast.Value)
		result := apistructs.APITestsAssertData{
			Arg:         ast.Arg,
			Operator:    ast.Operator,
			Value:       ast.Value,
			Success:     succ,
			ActualValue: actualValue,
			ErrorInfo: func() string {
				if err != nil {
					return err.Error()
				}
				return ""
			}(),
		}
		results = append(results, &result)
	}
	// 判断结果
	globalSuccess := true
	for _, result := range results {
		if !result.Success {
			globalSuccess = false
			break
		}
	}
	return globalSuccess, results
}
