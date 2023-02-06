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

package testcase

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/pkg/xmind"
)

func (svc *Service) storeXmind2DB(req apistructs.TestCaseImportRequest, rootTestSet dao.TestSet, tcs []apistructs.TestCaseXmind) (*apistructs.TestCaseImportResult, error) {
	var excelTcs []apistructs.TestCaseExcel
	for _, tc := range tcs {
		excelTcs = append(excelTcs, tc.TestCaseExcel)
	}
	return svc.storeExcel2DB(req, rootTestSet, excelTcs)
}

// insertTestCaseTopic 插入测试用例节点
// Topic 类型
// - 测试集 无标记
// - 测试用例 tc:Px__ 优先级
//   - 前置条件 p:
//   - 步骤1 - 结果1
//   - ...
//   - 步骤n - 结果n
//   - 接口测试
//     - at: (APITest)
//       - headers - result
//       - method - result
//       - url - result
//       - params - result
//       - body - result
//       - outParams - result
//       - asserts - result
func insertTestCaseTopic(parentTopic *xmind.XMLTopic, tc apistructs.TestCase) {
	// 用例名
	tcTitle := fmt.Sprintf("tc:%s__%s", tc.Priority, tc.Name)
	rootTopic := parentTopic.AddAttachedChildTopic(tcTitle)
	// 前置条件
	if tc.PreCondition != "" {
		preTitle := fmt.Sprintf("p:%s", tc.PreCondition)
		rootTopic = rootTopic.AddAttachedChildTopic(preTitle)
	}
	// 步骤结果
	for _, sr := range tc.StepAndResults {
		srTopic := rootTopic.AddAttachedChildTopic(sr.Step)
		srTopic.AddAttachedChildTopic(sr.Result)
	}
	// 接口测试
	if len(tc.APIs) > 0 {
		apiParentTopic := rootTopic.AddAttachedChildTopic("接口测试")
		for _, api := range tc.APIs {
			var apiInfo apistructs.APIInfo
			_ = json.Unmarshal([]byte(api.ApiInfo), &apiInfo)

			// name
			apiTopic := apiParentTopic.AddAttachedChildTopic(fmt.Sprintf("at:%s", apiInfo.Name))

			// headers
			headerBytes, _ := json.Marshal(apiInfo.Headers)
			apiTopic.AddAttachedChildTopic("headers").AddAttachedChildTopic(string(headerBytes))
			// method
			apiTopic.AddAttachedChildTopic("method").AddAttachedChildTopic(apiInfo.Method)
			// url
			apiTopic.AddAttachedChildTopic("url").AddAttachedChildTopic(apiInfo.URL)
			// params
			paramsBytes, _ := json.Marshal(apiInfo.Params)
			apiTopic.AddAttachedChildTopic("params").AddAttachedChildTopic(string(paramsBytes))
			// body
			bodyBytes, _ := json.Marshal(apiInfo.Body)
			apiTopic.AddAttachedChildTopic("body").AddAttachedChildTopic(string(bodyBytes))
			// outParams
			outParamsBytes, _ := json.Marshal(apiInfo.OutParams)
			apiTopic.AddAttachedChildTopic("outParams").AddAttachedChildTopic(string(outParamsBytes))
			// asserts
			assertBytes, _ := json.Marshal(apiInfo.Asserts)
			apiTopic.AddAttachedChildTopic("asserts").AddAttachedChildTopic(string(assertBytes))
		}
	}
}
