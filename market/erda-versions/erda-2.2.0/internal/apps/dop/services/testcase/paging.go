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
	"fmt"
	"sort"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

// ListTestCases return result TestCases, TestCaseIDs and error if have.
func (svc *Service) ListTestCases(req apistructs.TestCaseListRequest) ([]apistructs.TestCase, []uint64, error) {
	if req.ProjectID == 0 && !req.AllowMissingProjectID {
		return nil, nil, apierrors.ErrListTestCases.MissingParameter("projectID")
	}
	if len(req.TestSetIDs) == 0 && !req.AllowEmptyTestSetIDs {
		return nil, nil, apierrors.ErrListTestCases.MissingParameter("testCaseIDs")
	}
	dbTcs, err := svc.db.ListTestCasesByTestSetIDs(req)
	if err != nil {
		return nil, nil, apierrors.ErrListTestCases.InternalError(err)
	}
	var tcs []apistructs.TestCase
	var tcsIDs []uint64
	for _, dbTc := range dbTcs {
		tc := apistructs.TestCase{ID: uint64(dbTc.ID)}
		if !req.IDOnly {
			converted, err := svc.convertTestCase(dbTc)
			if err != nil {
				return nil, nil, apierrors.ErrListTestCases.InternalError(fmt.Errorf("failed to convert testcase, id: %d, err: %v", tc.ID, err))
			}
			tc = *converted
		}
		tcs = append(tcs, tc)
		tcsIDs = append(tcsIDs, tc.ID)
	}
	return tcs, tcsIDs, nil
}

func (svc *Service) ListTestCasesDeep(req apistructs.TestCaseListRequest) ([]uint64, error) {
	_, resultTestCaseIDs, err := svc.ListTestCases(req)
	if err != nil {
		return nil, apierrors.ErrListTestCases.InternalError(err)
	}
	testSets := []dao.TestSet{}
	for _, testSetID := range req.TestSetIDs {
		testSets, err = svc.db.ListTestSets(apistructs.TestSetListRequest{
			ProjectID: &req.ProjectID,
			ParentID:  &testSetID,
		})
		if err != nil {
			return nil, apierrors.ErrListTestCases.InternalError(err)
		}
	}
	for {
		if len(testSets) == 0 {
			break
		}
		childTestSets := []dao.TestSet{}
		testSetIDs := []uint64{}
		for _, testSet := range testSets {
			testSetIDs = append(testSetIDs, testSet.ID)
			testSets, err := svc.db.ListTestSets(apistructs.TestSetListRequest{
				ProjectID: &req.ProjectID,
				ParentID:  &testSet.ID,
			})
			if err != nil {
				return nil, apierrors.ErrListTestCases.InternalError(err)
			}
			childTestSets = append(childTestSets, testSets...)
		}
		_, testCaseIDs, err := svc.ListTestCases(apistructs.TestCaseListRequest{
			ProjectID:            req.ProjectID,
			AllowEmptyTestSetIDs: false,
			TestSetIDs:           testSetIDs,
			Recycled:             false,
			IDOnly:               true,
		})
		if err != nil {
			return nil, apierrors.ErrListTestCases.InternalError(err)
		}
		resultTestCaseIDs = append(resultTestCaseIDs, testCaseIDs...)
		testSets = childTestSets
	}
	return resultTestCaseIDs, nil

}

// PagingTestCases 测试用例分页查询
func (svc *Service) PagingTestCases(req apistructs.TestCasePagingRequest) (*apistructs.TestCasePagingResponseData, error) {
	// get paged testcases
	testCases, total, err := svc.db.PagingTestCases(req)
	if err != nil {
		return nil, err
	}

	if total == 0 {
		return &apistructs.TestCasePagingResponseData{}, nil
	}

	// get testsets from paged testcases
	testSets, err := svc.db.ListTestSets(apistructs.TestSetListRequest{
		Recycled:  req.Recycled,
		ProjectID: &req.ProjectID,
		TestSetIDs: func() (tsIDs []uint64) {
			for _, tc := range testCases {
				tsIDs = append(tsIDs, tc.TestSetID)
			}
			return
		}(),
		NoSubTestSets: true,
	})
	if err != nil {
		return nil, apierrors.ErrPagingTestCases.InternalError(err)
	}

	// 将 测试用例列表 转换为 测试集(包含测试用例)列表
	mapOfTestSetIDAndDir := make(map[uint64]string)
	for _, ts := range testSets {
		mapOfTestSetIDAndDir[ts.ID] = ts.Directory
	}
	resultTestSetMap := make(map[uint64]apistructs.TestSetWithCases)
	var testSetIDOrdered []uint64

	// 将 测试用例 按序归类到 测试集
	// batchConvert testCases
	convertedTCs, err := svc.batchConvertTestCases(req.ProjectID, testCases)
	if err != nil {
		return nil, err
	}
	// map: ts.ID -> TestSetWithCases ([]tc)
	for i, tc := range testCases {
		// testSetID 排序
		if _, ok := resultTestSetMap[tc.TestSetID]; !ok {
			testSetIDOrdered = append(testSetIDOrdered, tc.TestSetID)
		}
		// testSetWithCase 内容填充
		tmp := resultTestSetMap[tc.TestSetID]
		tmp.Directory = mapOfTestSetIDAndDir[tc.TestSetID]
		tmp.TestSetID = tc.TestSetID
		tmp.TestCases = append(tmp.TestCases, *convertedTCs[i])
		resultTestSetMap[tc.TestSetID] = tmp
	}
	resultTestSets := make([]apistructs.TestSetWithCases, 0)
	for _, tsID := range testSetIDOrdered {
		if ts, ok := resultTestSetMap[tsID]; ok {
			resultTestSets = append(resultTestSets, ts)
		}
	}

	// 获取所有用户 ID
	var allUserIDs []string
	for _, ts := range resultTestSets {
		for _, tc := range ts.TestCases {
			allUserIDs = append(allUserIDs, tc.CreatorID, tc.UpdaterID)
		}
	}
	allUserIDs = strutil.DedupSlice(allUserIDs, true)

	// 返回结果
	result := apistructs.TestCasePagingResponseData{
		Total:    total,
		TestSets: resultTestSets,
		UserIDs:  allUserIDs,
	}

	return &result, nil
}

// getAlphabetSortedTestSetIDs 返回根据字典序升序排好的 testSetID 列表
// 1: /z       2: /ab
// 2: /ab  =>  3: /c
// 3: /c       1: /z
func getAlphabetSortedTestSetIDs(testSets []dao.TestSet, order string) []uint64 {
	m := make(map[string]uint64) // key: dir, value: testSetID
	var allTestSetDirs []string
	for _, ts := range testSets {
		m[ts.Directory] = ts.ID
		allTestSetDirs = append(allTestSetDirs, ts.Directory)
	}
	allTestSetDirs = strutil.DedupSlice(allTestSetDirs, true)
	sort.Strings(allTestSetDirs)

	var result []uint64
	for _, dir := range allTestSetDirs {
		result = append(result, m[dir])
	}
	return result
}
