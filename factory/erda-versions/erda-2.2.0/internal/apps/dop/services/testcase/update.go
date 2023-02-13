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

	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	singleBlankChar = " "
)

// UpdateTestCase 更新测试用例
func (svc *Service) UpdateTestCase(req apistructs.TestCaseUpdateRequest) error {
	// 参数检查
	if req.ID == 0 {
		return apierrors.ErrUpdateTestCase.MissingParameter("id")
	}
	if req.Priority != "" {
		if !req.Priority.IsValid() {
			return apierrors.ErrUpdateTestCase.InvalidParameter(fmt.Sprintf("priority: %s", req.Priority))
		}
	}
	if err := strutil.Validate(req.Desc, strutil.MaxRuneCountValidator(apistructs.TestCaseDescMaxLength)); err != nil {
		return apierrors.ErrCreateTestCase.InvalidParameter(fmt.Sprintf("desc %v", err))
	}

	if req.PreCondition == "" {
		req.PreCondition = singleBlankChar
	}
	if req.Desc == "" {
		req.Desc = singleBlankChar
	}

	// 查询测试用例
	tc, err := svc.db.GetTestCaseByID(req.ID)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return apierrors.ErrUpdateTestCase.NotFound()
		}
		logrus.Errorf("failed to query testcase, id: %d, err: %v", req.ID, err)
		return apierrors.ErrUpdateTestCase.InternalError(fmt.Errorf("query testcase failed"))
	}

	// 更新至数据库
	if req.Name != "" {
		tc.Name = req.Name
	}
	if req.Priority != "" {
		tc.Priority = req.Priority
	}
	if req.PreCondition != "" {
		tc.PreCondition = req.PreCondition
	}
	if len(req.StepAndResults) > 0 {
		tc.StepAndResults = req.StepAndResults
	}
	if req.Desc != "" {
		tc.Desc = req.Desc
	}
	tc.UpdaterID = req.IdentityInfo.UserID

	if err := svc.db.UpdateTestCase(tc); err != nil {
		return apierrors.ErrUpdateTestCase.InternalError(err)
	}

	// 更新/创建/删除 API 信息
	// 查询已存在的 API 列表，若已存在的 API 在新的全量 API 中未找到，则需要删除
	existAPIs, err := svc.ListAPIs(int64(tc.ID))
	if err != nil {
		return apierrors.ErrUpdateTestCase.InternalError(err)
	}
	for _, existAPI := range existAPIs {
		foundInReq := false
		for _, reqAPI := range req.APIs {
			if existAPI.ApiID == reqAPI.ApiID {
				foundInReq = true
				break
			}
		}
		if !foundInReq {
			if err := svc.DeleteAPI(existAPI.ApiID); err != nil {
				return fmt.Errorf("failed to delete api info, apiID: %d, err: %v", existAPI.ApiID, err)
			}
		}
	}
	// 创建或更新
	if len(req.APIs) > 0 {
		err := svc.createOrUpdateAPIs(uint64(tc.ID), tc.ProjectID, req.APIs)
		if err != nil {
			return apierrors.ErrUpdateTestCase.InternalError(fmt.Errorf("failed to create api info, err: %v", err))
		}
	}

	return nil
}

func (svc *Service) BatchUpdateTestCases(req apistructs.TestCaseBatchUpdateRequest) error {
	// 参数校验
	if req.Priority != "" {
		if !req.Priority.IsValid() {
			return apierrors.ErrBatchUpdateTestCases.InvalidParameter(fmt.Sprintf("priority: %s", req.Priority))
		}
	}

	// 校验目标测试集是否存在
	if req.MoveToTestSetID != nil && *req.MoveToTestSetID != 0 {
		_, err := svc.db.GetTestSetByID(*req.MoveToTestSetID)
		if err != nil {
			if gorm.IsRecordNotFoundError(err) {
				return apierrors.ErrBatchUpdateTestCases.InvalidParameter(fmt.Sprintf("testset not found, id: %d", *req.MoveToTestSetID))
			}
			return apierrors.ErrBatchUpdateTestCases.InvalidParameter(fmt.Sprintf("failed to find testset, id: %d, err: %v", *req.MoveToTestSetID, err))
		}
	}

	// 校验 ids 是否都存在
	_, err := svc.db.ListTestCasesByIDs(req.TestCaseIDs)
	if err != nil {
		return apierrors.ErrBatchUpdateTestCases.InvalidParameter(err)
	}

	// 批量更新字段
	if err := svc.db.BatchUpdateTestCases(req); err != nil {
		return apierrors.ErrBatchUpdateTestCases.InternalError(err)
	}

	// 如果是移动到回收站,解除事件和执行计划关联
	if req.Recycled != nil && *req.Recycled {
		err = svc.db.DeleteIssueTestCaseRelationsByTestCaseIDs(req.TestCaseIDs)
		if err != nil {
			return apierrors.ErrBatchUpdateTestCases.InternalError(
				fmt.Errorf("failed to delete issue case relation, caseIDs: %+v", req.TestCaseIDs))
		}
		err = svc.db.DeleteTestPlanCaseRelationsByTestCaseIds(req.TestCaseIDs)
		if err != nil {
			return apierrors.ErrBatchUpdateTestCases.InternalError(
				fmt.Errorf("failed to delete use plan case relation, caseIDs: %+v", req.TestCaseIDs))
		}
	}
	return nil
}
