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

package testplan

import (
	"fmt"
	"io"
	"strconv"
	"sync"

	"github.com/mohae/deepcopy"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/i18n"
	"github.com/erda-project/erda/pkg/excel"
	"github.com/erda-project/erda/pkg/strutil"
)

func (t *TestPlan) Export(w io.Writer, req apistructs.TestPlanCaseRelExportRequest) error {
	// 参数校验
	if !req.FileType.Valid() {
		return apierrors.ErrExportTestPlanCaseRels.InvalidParameter("fileType")
	}
	// 根据分页查询条件，获取总数，进行优化
	req.PageNo = 1
	req.PageSize = 1
	totalResult, err := t.PagingTestPlanCaseRels(req.TestPlanCaseRelPagingRequest)
	if err != nil {
		return err
	}
	total := totalResult.Total
	// 以 size=200 并行获取 testCase，加快速度
	pageSize := 200
	count := int(total)/int(pageSize) + 1
	var wg sync.WaitGroup
	var errs []string
	testSetMap := make(map[int][]apistructs.TestSetWithPlanCaseRels)
	for i := 0; i < count; i++ {
		c := deepcopy.Copy(req.TestPlanCaseRelPagingRequest)
		copiedReq, ok := c.(apistructs.TestPlanCaseRelPagingRequest)
		if !ok {
			panic("should not be here")
		}
		copiedReq.PageNo = int64(i + 1)
		copiedReq.PageSize = int64(pageSize)
		wg.Add(1)
		go func(order int, req apistructs.TestPlanCaseRelPagingRequest, testSetMap map[int][]apistructs.TestSetWithPlanCaseRels) {
			defer wg.Done()
			pagingResult, err := t.PagingTestPlanCaseRels(req)
			if err != nil {
				errs = append(errs, err.Error())
				return
			}
			testSetMap[order] = pagingResult.TestSets
		}(i, copiedReq, testSetMap)
	}
	wg.Wait()

	// 错误处理
	if len(errs) > 0 {
		return fmt.Errorf(strutil.Join(errs, ",", true))
	}

	// 结果处理
	var rels []apistructs.TestPlanCaseRel
	testSets := make([][]apistructs.TestSetWithPlanCaseRels, 0, count)
	for i := 0; i < count; i++ {
		testSets = append(testSets, testSetMap[i])
	}
	for _, testSets := range testSets {
		for _, ts := range testSets {
			for _, tc := range ts.TestCases {
				rels = append(rels, tc)
			}
		}
	}

	// 查询所有执行者信息
	var executorIDs []string
	for _, rel := range rels {
		executorIDs = append(executorIDs, rel.ExecutorID)
	}
	executorIDs = strutil.DedupSlice(executorIDs, true)
	userListResp, err := t.bdl.ListUsers(apistructs.UserListRequest{UserIDs: executorIDs})
	if err != nil {
		return err
	}
	executorMap := make(map[string]apistructs.UserInfo)
	for _, executor := range userListResp.Users {
		executorMap[executor.ID] = executor
	}

	l := t.bdl.GetLocale(req.Locale)

	// export
	switch req.FileType {
	case apistructs.TestCaseFileTypeExcel:
		excelLines, err := t.convert2Excel(rels, executorMap, req.Locale)
		if err != nil {
			return err
		}
		return excel.ExportExcelByCell(w, excelLines, l.Get(i18n.I18nKeyTestPlanSheetName))
	default:
		return fmt.Errorf("export type %s is not supported yet", req.FileType)
	}
}

func (t *TestPlan) convert2Excel(rels []apistructs.TestPlanCaseRel, userMap map[string]apistructs.UserInfo, locale string) ([][]excel.Cell, error) {
	l := t.bdl.GetLocale(locale)

	title1 := []excel.Cell{
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCaseNum), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCaseName), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCaseSetName), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCasePriority), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCasePrecondition), 1),
		excel.NewHMergeCell(l.Get(i18n.I18nKeyCaseStepResult), 1),
		excel.EmptyCell(),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCaseExecutor), 1),
		excel.NewVMergeCell(l.Get(i18n.I18nKeyCaseExecResult), 1),
	}
	title2 := []excel.Cell{
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.EmptyCell(),
		excel.NewCell(l.Get(i18n.I18nKeyCaseStep)),
		excel.NewCell(l.Get(i18n.I18nKeyCaseExpectResult)),
		excel.EmptyCell(),
		excel.EmptyCell(),
	}

	testCaseIDs := []uint64{}
	testSetIDs := []uint64{}
	testCaseMap := map[uint64]dao.TestCase{}
	testSetMap := map[uint64]dao.TestSet{}
	for _, rel := range rels {
		testCaseIDs = append(testCaseIDs, rel.TestCaseID)
		if rel.TestSetID > 0 {
			testSetIDs = append(testSetIDs, rel.TestSetID)
		}
	}
	testCaseIDs = strutil.DedupUint64Slice(testCaseIDs, true)
	testSetIDs = strutil.DedupUint64Slice(testSetIDs, true)
	testCases, err := t.db.ListTestCasesByIDs(testCaseIDs)
	if err != nil {
		return nil, err
	}
	for _, testCase := range testCases {
		testCaseMap[testCase.ID] = testCase
	}

	if len(testSetIDs) > 0 {
		testSets, err := t.db.ListTestSetByIDs(testSetIDs)
		if err != nil {
			return nil, err
		}
		for _, testSet := range testSets {
			testSetMap[testSet.ID] = testSet
		}
	}
	var allRelLines [][]excel.Cell
	for _, rel := range rels {
		// get testSet directory
		directory := "/"
		if rel.TestSetID > 0 {
			directory = testSetMap[rel.TestSetID].Directory
		}

		tc := testCaseMap[rel.TestCaseID]

		var oneRelLines [][]excel.Cell

		// 步骤会有多条，需要垂直合并单元格
		maxLen := len(tc.StepAndResults)

		for i := 0; i < maxLen; i++ {
			// 插入一行
			line := []excel.Cell{
				excel.NewCell(strconv.FormatUint(tc.ID, 10)),
				excel.NewCell(tc.Name),
				excel.NewCell(directory),
				excel.NewCell(string(tc.Priority)),
				excel.NewCell(tc.PreCondition),
			}

			// 操作步骤、预期结果
			if i < len(tc.StepAndResults) {
				line = append(line, excel.NewCell(tc.StepAndResults[i].Step), excel.NewCell(tc.StepAndResults[i].Result))
			} else {
				line = append(line, excel.EmptyCells(2)...)
			}

			// 测试执行人、测试执行结果
			var executorName string
			if rel.ExecutorID != "" {
				executorName = fmt.Sprintf("USER-ID: %s", rel.ExecutorID)
			}
			executor, ok := userMap[rel.ExecutorID]
			if ok {
				executorName = executor.Nick
			}
			line = append(line,
				excel.NewCell(executorName),
				excel.NewCell(l.Get(i18n.I18nKeyCaseExecResult+"."+string(rel.ExecStatus))),
			)

			oneRelLines = append(oneRelLines, line)
		}

		// 调整第一行，作单元格合并
		if len(oneRelLines) > 0 {
			firstLine := oneRelLines[0]
			vMergeNum := len(oneRelLines) - 1
			// 只合并前5列基础信息和最后2列
			cellIndexes := []int{0, 1, 2, 3, 4, 7, 8}
			for _, idx := range cellIndexes {
				firstLine[idx] = excel.NewVMergeCell(firstLine[idx].Value, vMergeNum)
			}
		}

		allRelLines = append(allRelLines, oneRelLines...)
	}

	allLines := [][]excel.Cell{title1, title2}
	for _, relLines := range allRelLines {
		allLines = append(allLines, relLines)
	}

	return allLines, nil
}
