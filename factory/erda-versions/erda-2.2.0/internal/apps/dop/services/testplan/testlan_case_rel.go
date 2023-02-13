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
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

func (t *TestPlan) CreateCaseRelations(req apistructs.TestPlanCaseRelCreateRequest) (*apistructs.TestPlanCaseRelCreateResult, error) {
	// 参数校验
	if req.TestPlanID == 0 {
		return nil, apierrors.ErrCreateTestPlanCaseRel.MissingParameter("testPlanID")
	}

	// 查询 testplan
	tp, err := t.Get(req.TestPlanID)
	if err != nil {
		return nil, err
	}

	// 校验 testSetIDs，获取 testCaseIDs 取并集
	if len(req.TestSetIDs) > 0 {
		testCaseIDsUnderTestSetsIds, err := t.testCaseSvc.ListTestCasesDeep(apistructs.TestCaseListRequest{
			ProjectID:            tp.ProjectID,
			AllowEmptyTestSetIDs: false,
			TestSetIDs:           req.TestSetIDs,
			Recycled:             false,
			IDOnly:               true,
		})
		if err != nil {
			return nil, err
		}
		for _, id := range testCaseIDsUnderTestSetsIds {
			req.TestCaseIDs = append(req.TestCaseIDs, id)
		}
	}
	req.TestCaseIDs = strutil.DedupUint64Slice(req.TestCaseIDs, true)

	if len(req.TestCaseIDs) == 0 && len(req.TestSetIDs) == 0 {
		return nil, apierrors.ErrCreateTestPlanCaseRel.MissingParameter("testCaseIDs or testSetIDs")
	}

	// 校验 testCaseIDs 是否存在
	tcs, err := t.db.ListTestCasesByIDs(req.TestCaseIDs)
	if err != nil {
		return nil, apierrors.ErrCreateTestPlanCaseRel.InvalidParameter(err)
	}

	// 校验哪些 testCaseIDs 已经在当前 plan 下已经存在关联关系
	// 已存在的不作操作，不考虑 testCase 的 testSetID 更新的情况
	existTcIDs, _, err := t.db.CheckTestPlanCaseRelIDsExistOrNot(req.TestPlanID, req.TestCaseIDs)
	if err != nil {
		return nil, apierrors.ErrCreateTestPlanCaseRel.InternalError(err)
	}
	existTcIDMap := make(map[uint64]struct{})
	for _, existTcID := range existTcIDs {
		existTcIDMap[existTcID] = struct{}{}
	}

	// 批量插入
	var rels []dao.TestPlanCaseRel
	for _, tc := range tcs {
		if _, ok := existTcIDMap[uint64(tc.ID)]; ok {
			continue
		}
		rel := dao.TestPlanCaseRel{
			TestPlanID: tp.ID,
			TestSetID:  tc.TestSetID,
			TestCaseID: uint64(tc.ID),
			ExecStatus: apistructs.CaseExecStatusInit,
			CreatorID:  req.UserID,
		}
		rels = append(rels, rel)
	}
	if err := t.db.BatchCreateTestPlanCaseRels(rels); err != nil {
		return nil, apierrors.ErrCreateTestPlanCaseRel.InternalError(err)
	}

	// result
	result := apistructs.TestPlanCaseRelCreateResult{TotalCount: uint64(len(rels))}

	return &result, nil
}

func (t *TestPlan) GetRel(relID uint64) (*apistructs.TestPlanCaseRel, error) {
	dbRel, err := t.db.GetTestPlanCaseRel(relID)
	if err != nil {
		return nil, apierrors.ErrGetTestPlanCaseRel.InternalError(err)
	}
	if dbRel == nil {
		return nil, apierrors.ErrGetTestPlanCaseRel.NotFound()
	}

	tc, err := t.testCaseSvc.GetTestCase(dbRel.TestCaseID)
	if err != nil {
		return nil, err
	}

	rel := t.ConvertRel(dbRel, tc)

	// relation issue bugs
	issueCaseRels, err := t.db.ListIssueTestCaseRelations(apistructs.IssueTestCaseRelationsListRequest{
		TestPlanCaseRelID: rel.ID,
	})
	if err != nil {
		return nil, apierrors.ErrListIssueTestCaseRels.InternalError(err)
	}
	var issueIDs []uint64
	issueRelationIDMap := make(map[uint64]uint64) // key: issueID, value: issueCaseRelationID
	for _, issueCaseRel := range issueCaseRels {
		issueIDs = append(issueIDs, issueCaseRel.IssueID)
		issueRelationIDMap[issueCaseRel.IssueID] = uint64(issueCaseRel.ID)
	}

	issueIDs = strutil.DedupUint64Slice(issueIDs, true)
	var issueStatusSlice []int64
	var issueMap = map[uint64]*apistructs.Issue{}
	for _, issueID := range issueIDs {
		issue, err := t.issueSvc.GetIssue(apistructs.IssueGetRequest{ID: issueID})
		if err == nil {
			var rels []apistructs.TestPlanCaseRel
			rels, err = t.getTestPlanCaseRels(issueID)
			issue.TestPlanCaseRels = rels
		}
		if err != nil {
			// 若 issue 已经不存在，则主动删除该依赖关系
			if apiErr, ok := err.(*errorresp.APIError); ok {
				if apiErr.HttpCode() == http.StatusNotFound {
					if err := t.InternalRemoveTestPlanCaseRelIssueRelationsByIssueID(issueID); err != nil {
						logrus.Errorf("failed to internal remove testPlanCaseRel issueRelations by issueID: %d, err: %v", issueID, err)
					}
				}
				continue
			}
			return nil, err
		}

		issueMap[issueID] = issue
		issueStatusSlice = append(issueStatusSlice, issue.State)
	}

	// 批量获取 issueSt
	issueStatusMap, err := t.batchGetIssueState(issueStatusSlice)
	if err != nil {
		return nil, err
	}
	for k, issue := range issueMap {
		testPlanCaseRelIssueBug := apistructs.TestPlanCaseRelIssueBug{
			IssueRelationID: issueRelationIDMap[k],
			IssueID:         uint64(issue.ID),
			IterationID:     issue.IterationID,
			Title:           issue.Title,
			Priority:        issue.Priority,
			CreatedAt:       issue.CreatedAt,
		}

		if issueStatusMap != nil {
			issueState, ok := issueStatusMap[issue.State]
			if ok {
				testPlanCaseRelIssueBug.State = apistructs.IssueState(issueState.StateName)
				testPlanCaseRelIssueBug.StateBelong = issueState.StateBelong
			}
		}
		rel.IssueBugs = append(rel.IssueBugs, testPlanCaseRelIssueBug)
	}

	return rel, nil
}

func (t *TestPlan) getTestPlanCaseRels(issueID uint64) ([]apistructs.TestPlanCaseRel, error) {
	// 查询关联的测试计划用例
	testPlanCaseRels := make([]apistructs.TestPlanCaseRel, 0)
	issueTestCaseRels, err := t.db.ListIssueTestCaseRelations(apistructs.IssueTestCaseRelationsListRequest{IssueID: issueID})
	if err != nil {
		return nil, err
	}
	if len(issueTestCaseRels) > 0 {
		var relIDs []uint64
		for _, issueCaseRel := range issueTestCaseRels {
			relIDs = append(relIDs, issueCaseRel.TestPlanCaseRelID)
		}
		relIDs = strutil.DedupUint64Slice(relIDs, true)
		rels, err := t.ListTestPlanCaseRels(apistructs.TestPlanCaseRelListRequest{IDs: relIDs})
		if err != nil {
			return nil, err
		}

		for _, rel := range rels {
			testPlanCaseRels = append(testPlanCaseRels, rel)
		}
	}
	return testPlanCaseRels, nil
}

func (t *TestPlan) batchGetIssueState(issueStatusSlice []int64) (results map[int64]apistructs.IssueStatus, err error) {
	issueStates, err := t.issueStateSvc.GetIssuesStatesNameByID(issueStatusSlice)
	if err != nil {
		return nil, err
	}

	results = map[int64]apistructs.IssueStatus{}
	for _, v := range issueStates {
		results[v.StateID] = v
	}
	return results, nil
}

// BatchUpdateTestPlanCaseRels 批量更新测试计划测试用例关系
func (t *TestPlan) BatchUpdateTestPlanCaseRels(req apistructs.TestPlanCaseRelBatchUpdateRequest) error {
	// 参数校验
	if req.TestPlanID == 0 {
		return apierrors.ErrBatchUpdateTestPlanCaseRels.MissingParameter("testPlanID")
	}
	if len(req.RelationIDs) == 0 && req.TestSetID == nil {
		return nil
	}

	// 处理 relationIDs
	if req.TestSetID != nil {
		// 递归查询 testSet 下的所有测试集
		tsIDs, _, err := t.db.ListTestSetsRecursive(apistructs.TestSetListRequest{
			Recycled:      false,
			ParentID:      req.TestSetID,
			ProjectID:     &req.ProjectID,
			TestSetIDs:    nil,
			NoSubTestSets: false,
		})
		if err != nil {
			return err
		}
		// 根据测试集列表查询所有待操作的关联关系
		rels, err := t.ListTestPlanCaseRels(apistructs.TestPlanCaseRelListRequest{
			TestSetIDs:   tsIDs,
			IDOnly:       true,
			IdentityInfo: req.IdentityInfo,
		})
		if err != nil {
			return err
		}
		// 追加 ID
		for _, rel := range rels {
			req.RelationIDs = append(req.RelationIDs, rel.ID)
		}
	}

	// 删除
	if req.Delete {
		// 删除 缺陷和测试计划用例关联
		if err := t.db.DeleteIssueTestCaseRelationsByTestPlanCaseRelIDs(req.RelationIDs); err != nil {
			return apierrors.ErrBatchUpdateTestPlanCaseRels.InternalError(err)
		}
		// 删除测试计划用例
		if err := t.db.DeleteTestPlanCaseRelations(req.TestPlanID, req.RelationIDs); err != nil {
			return apierrors.ErrBatchUpdateTestPlanCaseRels.InternalError(err)
		}
		return nil
	}

	// 批量更新
	if err := t.db.BatchUpdateTestPlanCaseRels(req); err != nil {
		return apierrors.ErrBatchUpdateTestPlanCaseRels.InternalError(err)
	}

	return nil
}

// RemoveTestPlanCaseRelIssueRelations 解除测试计划用例与事件缺陷的关联
func (t *TestPlan) RemoveTestPlanCaseRelIssueRelations(req apistructs.TestPlanCaseRelIssueRelationRemoveRequest) error {
	// 参数校验
	if req.TestPlanID == 0 {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.MissingParameter("testPlanID")
	}
	if req.TestPlanCaseRelID == 0 {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.MissingParameter("testPlanCaseRelID")
	}
	if len(req.IssueTestCaseRelationIDs) == 0 {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.MissingParameter("issueTestCaseRelationIDs")
	}

	// 查询测试计划用例
	rel, err := t.GetRel(req.TestPlanCaseRelID)
	if err != nil {
		return err
	}

	// 判断待删除的用例缺陷关联 ID 是否合法
	existIssueRelationIDMap := make(map[uint64]struct{})
	for _, bug := range rel.IssueBugs {
		existIssueRelationIDMap[bug.IssueRelationID] = struct{}{}
	}
	var notExistIssueRelationIDs []uint64
	for _, waitDeleteID := range req.IssueTestCaseRelationIDs {
		if _, ok := existIssueRelationIDMap[waitDeleteID]; !ok {
			notExistIssueRelationIDs = append(notExistIssueRelationIDs, waitDeleteID)
		}
	}
	if len(notExistIssueRelationIDs) > 0 {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.InvalidParameter(fmt.Errorf("some issueTestCaseRelationIDs not exists: %v", notExistIssueRelationIDs))
	}

	// 删除
	if err := t.db.DeleteIssueTestCaseRelationsByIDs(req.IssueTestCaseRelationIDs); err != nil {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.InternalError(err)
	}

	// 更新测试计划用例更新人
	if err := t.db.BatchUpdateTestPlanCaseRels(apistructs.TestPlanCaseRelBatchUpdateRequest{
		TestPlanID:   rel.TestPlanID,
		RelationIDs:  []uint64{rel.ID},
		IdentityInfo: apistructs.IdentityInfo{UserID: req.IdentityInfo.UserID},
	}); err != nil {
		return apierrors.ErrUpdateTestPlanCaseRel.InternalError(err)
	}

	return nil
}

// AddTestPlanCaseRelIssueRelations 新增测试计划用例与事件缺陷的关联
func (t *TestPlan) AddTestPlanCaseRelIssueRelations(req apistructs.TestPlanCaseRelIssueRelationAddRequest) error {
	// 参数校验
	if req.TestPlanID == 0 {
		return apierrors.ErrAddTestPlanCaseRelIssueRelation.MissingParameter("testPlanID")
	}
	if req.TestPlanCaseRelID == 0 {
		return apierrors.ErrAddTestPlanCaseRelIssueRelation.MissingParameter("testPlanCaseRelID")
	}
	if len(req.IssueIDs) == 0 {
		return apierrors.ErrAddTestPlanCaseRelIssueRelation.MissingParameter("issueIDs")
	}

	// 查询测试计划用例
	rel, err := t.GetRel(req.TestPlanCaseRelID)
	if err != nil {
		return err
	}

	// 新增
	var issues []apistructs.Issue
	for _, issueID := range req.IssueIDs {
		issue, err := t.issueSvc.GetIssue(apistructs.IssueGetRequest{
			ID: issueID,
		})
		if err == nil {
			var rels []apistructs.TestPlanCaseRel
			rels, err = t.getTestPlanCaseRels(issueID)
			issue.TestPlanCaseRels = rels
		}
		if err != nil {
			return err
		}
		issues = append(issues, *issue)
	}
	// 批量创建关联
	var issueCaseRels []dao.IssueTestCaseRelation
	for _, issue := range issues {
		issueCaseRels = append(issueCaseRels, dao.IssueTestCaseRelation{
			IssueID:           uint64(issue.ID),
			TestPlanID:        rel.TestPlanID,
			TestPlanCaseRelID: rel.ID,
			TestCaseID:        rel.TestCaseID,
			CreatorID:         req.UserID,
		})
	}
	if err := t.db.BatchCreateIssueTestCaseRelations(issueCaseRels); err != nil {
		return apierrors.ErrBatchCreateIssueTestCaseRel.InternalError(err)
	}

	// 更新测试计划用例更新人
	if err := t.db.BatchUpdateTestPlanCaseRels(apistructs.TestPlanCaseRelBatchUpdateRequest{
		TestPlanID:   rel.TestPlanID,
		RelationIDs:  []uint64{rel.ID},
		IdentityInfo: apistructs.IdentityInfo{UserID: req.IdentityInfo.UserID},
	}); err != nil {
		return apierrors.ErrUpdateTestPlanCaseRel.InternalError(err)
	}

	return nil
}

// InternalRemoveTestPlanCaseRelIssueRelationsByIssueID 根据 issueID 删除测试计划用例与事件缺陷的关联
func (t *TestPlan) InternalRemoveTestPlanCaseRelIssueRelationsByIssueID(issueID uint64) error {
	// 无需查询 issue 是否存在，若 issue 不存在，关联关系本就应该被删除

	if err := t.db.DeleteIssueTestCaseRelationsByIssueIDs([]uint64{issueID}); err != nil {
		return apierrors.ErrRemoveTestPlanCaseRelIssueRelation.InternalError(err)
	}
	return nil
}

// ConvertRel
func (t *TestPlan) ConvertRel(dbRel *dao.TestPlanCaseRel, tc *apistructs.TestCase) *apistructs.TestPlanCaseRel {
	rel := apistructs.TestPlanCaseRel{
		ID:         dbRel.ID,
		TestPlanID: dbRel.TestPlanID,
		TestSetID:  dbRel.TestSetID,
		TestCaseID: dbRel.TestCaseID,
		ExecStatus: dbRel.ExecStatus,
		CreatorID:  dbRel.CreatorID,
		UpdaterID:  dbRel.UpdaterID,
		ExecutorID: dbRel.ExecutorID,
		CreatedAt:  dbRel.CreatedAt,
		UpdatedAt:  dbRel.UpdatedAt,
	}
	if tc != nil {
		rel.Name = tc.Name
		rel.Priority = tc.Priority
		rel.APICount = tc.APICount
	}
	return &rel
}
