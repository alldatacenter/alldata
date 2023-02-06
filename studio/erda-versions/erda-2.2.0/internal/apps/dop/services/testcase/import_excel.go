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
	"io"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/excel"
	"github.com/erda-project/erda/pkg/strutil"
)

func (svc *Service) decodeFromExcelFile(r io.Reader) (allTestCases []apistructs.TestCaseExcel, err error) {
	var lineTestCaseID string
	defer func() {
		if r := recover(); r != nil {
			err = apierrors.ErrInvalidTestCaseExcelFormat.InternalError(fmt.Errorf("testCaseID: %s", lineTestCaseID))
		}
	}()
	sheets, err := excel.Decode(r)
	if err != nil {
		return nil, err
	}
	if len(sheets) == 0 {
		return nil, fmt.Errorf("not found sheet")
	}
	rows := sheets[0]
	// 校验：至少有两行 title
	if len(rows) < 2 {
		return nil, fmt.Errorf("invalid title format")
	}
	// 根据用例编号进行分组
	groupedRows := make(map[string][][]string) // key: TestCaseID, value: TestCaseInfos
	var orderedRowIDs []string
	var currentTcID string
	for i := 2; i < len(rows); i++ {
		row := rows[i]
		if row[0] != "" {
			currentTcID = row[0]
		}
		groupedRows[currentTcID] = append(groupedRows[currentTcID], row)
		orderedRowIDs = append(orderedRowIDs, currentTcID)
	}
	orderedRowIDs = strutil.DedupSlice(orderedRowIDs, true)

	// 操作每个分组
	for _, testCaseID := range orderedRowIDs {
		lineTestCaseID = testCaseID
		rows, ok := groupedRows[testCaseID]
		if !ok {
			continue
		}
		firstLine := rows[0]
		tcExcel := apistructs.TestCaseExcel{
			Title:         firstLine[1],
			DirectoryName: firstLine[2],
			PriorityName:  firstLine[3],
			PreCondition:  firstLine[4],
		}
		// 步骤与结果列表，接口测试
		for _, row := range rows {
			// 步骤与结果
			if row[5] != "" {
				tcExcel.StepAndResults = append(tcExcel.StepAndResults, apistructs.TestCaseStepAndResult{Step: row[5], Result: row[6]})
			}

			// 接口测试
			if len(row) > 7 && row[7] != "" {

				// 接口名称
				name := row[7]
				// 请求头信息
				var headers []apistructs.APIHeader
				if err := json.Unmarshal([]byte(row[8]), &headers); err != nil {
					return nil, fmt.Errorf("failed to parse api headers, testCaseID: %s, name: %s, err: %v", testCaseID, tcExcel.Title, err)
				}
				// 方法
				method := row[9]
				// 接口地址
				url := row[10]
				// 接口参数
				var params []apistructs.APIParam
				if err := json.Unmarshal([]byte(row[11]), &params); err != nil {
					return nil, fmt.Errorf("failed to parse api params, testCaseID: %s, name: %s, err: %v", testCaseID, tcExcel.Title, err)
				}
				// 请求体
				var reqBody apistructs.APIBody
				if err := json.Unmarshal([]byte(row[12]), &reqBody); err != nil {
					return nil, fmt.Errorf("failed to parse api request body, testCaseID: %s, name: %s, err: %v", testCaseID, tcExcel.Title, err)
				}
				// out 参数
				var outParams []apistructs.APIOutParam
				if err := json.Unmarshal([]byte(row[13]), &outParams); err != nil {
					return nil, fmt.Errorf("failed to parse api out params, testCaseID: %s, name: %s, err: %v", testCaseID, tcExcel.Title, err)
				}
				// 断言
				var asserts [][]apistructs.APIAssert
				if err := json.Unmarshal([]byte(row[14]), &asserts); err != nil {
					return nil, fmt.Errorf("failed ot parse api asserts, testCaesID: %s, name: %s, err: %v", testCaseID, tcExcel.Title, err)
				}

				tcExcel.ApiInfos = append(tcExcel.ApiInfos, apistructs.APIInfo{
					Name:      name,
					Headers:   headers,
					Method:    method,
					URL:       url,
					Params:    params,
					Body:      reqBody,
					OutParams: outParams,
					Asserts:   asserts,
				})
			}
		}

		allTestCases = append(allTestCases, tcExcel)
	}

	return allTestCases, nil
}

func (svc *Service) storeExcel2DB(req apistructs.TestCaseImportRequest, rootTestSet dao.TestSet, tcs []apistructs.TestCaseExcel) (*apistructs.TestCaseImportResult, error) {
	// validate and polish first
	for i := range tcs {
		tc := tcs[i]
		// priority, if invalid, use P3
		if !apistructs.TestCasePriority(tc.PriorityName).IsValid() {
			tcs[i].PriorityName = string(apistructs.TestCasePriorityP3)
		}
	}

	for _, tc := range tcs {
		// create testset
		// split excel directory，在 targetTestSetDir/ 下创建对应的测试集
		parentID := rootTestSet.ID
		tc.DirectoryName = strutil.Trim(tc.DirectoryName)
		dirList := strutil.Split(strutil.TrimPrefixes(tc.DirectoryName, "/"), "/")
		for _, dir := range dirList {
			if dir == "" {
				continue
			}
			existTs, err := svc.db.GetTestSetByNameAndParentIDAndProjectID(rootTestSet.ProjectID, parentID, false, dir)
			if err != nil {
				return nil, err
			}
			if existTs != nil {
				parentID = existTs.ID
			} else {
				// create testSet
				testSet, err := svc.CreateTestSetFn(apistructs.TestSetCreateRequest{
					Name:         dir,
					ProjectID:    &rootTestSet.ProjectID,
					ParentID:     &parentID,
					IdentityInfo: req.IdentityInfo,
				})
				if err != nil {
					return nil, err
				}
				parentID = testSet.ID
			}
		}

		// 过滤只用来创建空子测试集的 fake tc
		if tc.Title == fakeTcName {
			continue
		}

		var apiTests []*apistructs.ApiTestInfo
		for _, apiInfo := range tc.ApiInfos {
			apiInfoBytes, _ := json.Marshal(apiInfo)
			apiTests = append(apiTests, &apistructs.ApiTestInfo{
				ApiInfo: string(apiInfoBytes),
			})
		}

		tcCreateReq := apistructs.TestCaseCreateRequest{
			ProjectID:      rootTestSet.ProjectID,
			TestSetID:      parentID,
			Name:           tc.Title,
			PreCondition:   tc.PreCondition,
			StepAndResults: tc.StepAndResults,
			APIs:           apiTests,
			Priority:       apistructs.TestCasePriority(tc.PriorityName),
			IdentityInfo:   req.IdentityInfo,
		}
		if _, err := svc.CreateTestCase(tcCreateReq); err != nil {
			return nil, err
		}
	}

	importResult := apistructs.TestCaseImportResult{SuccessCount: uint64(len(tcs))}

	return &importResult, nil
}
