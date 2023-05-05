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
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/i18n"
	"github.com/erda-project/erda/pkg/excel"
)

const (
	i18nKeyCaseAPITest = ""
)

func (svc *Service) convert2Excel(tcs []apistructs.TestCaseWithSimpleSetInfo, locale string) ([][]excel.Cell, error) {
	begin := time.Now()
	defer func() {
		end := time.Now()
		fmt.Println("convert excel: ", end.Sub(begin).Seconds(), "s")
	}()
	l := svc.bdl.GetLocale(locale)
	title1 := []excel.Cell{
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCaseNum), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCaseName), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCaseSetName), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCasePriority), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCasePrecondition), 1),
		excel.NewHMergeCell(l.Get(i18n.I18nKeyCaseStepResult), 1),
		excel.EmptyCell(),
		excel.NewHMergeCell(l.Get(i18n.I18nKeyCaseAPITest), 7),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
	}
	title2 := []excel.Cell{
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.NewCell(l.Get(i18n.I18nKeyCaseStep)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseExpectResult)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseAPITestName)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseAPITestHeaders)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseAPITestMethod)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseAPITestUrl)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseAPITestParams)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseAPITestBody)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseAPITestOutParams)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseAPITestAsserts)),
	}

	var allTcLines [][]excel.Cell

	for _, tc := range tcs {
		var oneTcLines [][]excel.Cell

		// 步骤与结果会有多条，接口测试也会有多个，取两个长度更大的一个，作为垂直单元格合并的数值
		maxLen := len(tc.StepAndResults)
		if len(tc.APIs) > maxLen {
			maxLen = len(tc.APIs)
		}
		for i := 0; i < maxLen; i++ {
			// 插入一行
			line := []excel.Cell{
				excel.NewCell(strconv.FormatUint(tc.ID, 10)),
				excel.NewCell(tc.Name),
				excel.NewCell(tc.Directory),
				excel.NewCell(string(tc.Priority)),
				excel.NewCell(tc.PreCondition),
			}

			// 操作步骤、预期结果
			if i < len(tc.StepAndResults) {
				line = append(line, excel.NewCell(tc.StepAndResults[i].Step), excel.NewCell(tc.StepAndResults[i].Result))
			} else {
				line = append(line, excel.EmptyCells(2)...)
			}

			// 接口名称、请求头信息、方法、接口地址、接口参数、请求体、out 参数、断言
			if i < len(tc.APIs) {
				var api apistructs.APIInfo
				if err := json.Unmarshal([]byte(tc.APIs[i].ApiInfo), &api); err != nil {
					return nil, err
				}
				// params
				headerBytes, _ := json.Marshal(api.Headers)
				paramsBytes, _ := json.Marshal(api.Params)
				reqBodyBytes, _ := json.Marshal(api.Body)
				outParamsBytes, _ := json.Marshal(api.OutParams)
				assertBytes, _ := json.Marshal(api.Asserts)
				line = append(line,
					excel.NewCell(api.Name),
					excel.NewCell(string(headerBytes)),
					excel.NewCell(api.Method),
					excel.NewCell(api.URL),
					excel.NewCell(string(paramsBytes)),
					excel.NewCell(string(reqBodyBytes)),
					excel.NewCell(string(outParamsBytes)),
					excel.NewCell(string(assertBytes)),
				)
			} else {
				line = append(line, excel.EmptyCells(8)...)
			}

			oneTcLines = append(oneTcLines, line)
		}

		// 调整第一行，作单元格合并
		if len(oneTcLines) > 0 {
			firstLine := oneTcLines[0]
			vMergeNum := len(oneTcLines) - 1
			// 只合并前5列基础信息
			for i := 0; i < 5; i++ {
				firstLine[i] = excel.NewVMergeCell(firstLine[i].Value, vMergeNum)
				// 被合并的单元格数据置为空，优化文件大小
				// e.g., 6673 bytes -> 3888 bytes
				for j := 1; j < len(oneTcLines); j++ {
					oneTcLines[j][i] = excel.EmptyCell()
				}
			}
		}

		allTcLines = append(allTcLines, oneTcLines...)
	}

	allLines := [][]excel.Cell{title1, title2}
	for _, tcLines := range allTcLines {
		allLines = append(allLines, tcLines)
	}

	return allLines, nil
}
