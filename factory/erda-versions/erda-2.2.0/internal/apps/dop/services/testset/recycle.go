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

package testset

import (
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) Recycle(req apistructs.TestSetRecycleRequest) error {
	// 参数校验
	if req.TestSetID == 0 {
		return apierrors.ErrRecycleTestSet.MissingParameter("testSetID")
	}

	// 查询测试集
	ts, err := svc.Get(req.TestSetID)
	if err != nil {
		return err
	}

	// 回收测试用例
	_, tcIDs, err := svc.tcSvc.ListTestCases(apistructs.TestCaseListRequest{
		ProjectID:  ts.ProjectID,
		TestSetIDs: []uint64{ts.ID},
		Recycled:   false,
		IDOnly:     true,
	})
	if len(tcIDs) > 0 {
		if err := svc.tcSvc.BatchUpdateTestCases(apistructs.TestCaseBatchUpdateRequest{
			Recycled:     &[]bool{true}[0],
			TestCaseIDs:  tcIDs,
			IdentityInfo: req.IdentityInfo,
		}); err != nil {
			return err
		}
	}

	// 回收测试集
	var newParentID *uint64
	if req.IsRoot && ts.ParentID != 0 {
		newParentID = &[]uint64{0}[0]
	}
	if err := svc.db.RecycleTestSet(ts.ID, newParentID); err != nil {
		return apierrors.ErrRecycleTestSet.InternalError(fmt.Errorf("failed to recycle current testset, id: %d, err: %v", ts.ID, err))
	}

	// 递归回收子测试集
	subTestSets, err := svc.List(apistructs.TestSetListRequest{
		Recycled:  false,
		ParentID:  &ts.ID,
		ProjectID: &ts.ProjectID,
	})
	if err != nil {
		return err
	}
	for _, subTs := range subTestSets {
		if err := svc.Recycle(apistructs.TestSetRecycleRequest{
			TestSetID:    subTs.ID,
			IsRoot:       false,
			IdentityInfo: req.IdentityInfo,
		}); err != nil {
			return err
		}
	}

	return nil
}

func (svc *Service) CleanFromRecycleBin(req apistructs.TestSetCleanFromRecycleBinRequest) error {
	// 参数校验
	if req.TestSetID == 0 {
		return apierrors.ErrCleanTestSetFromRecycleBin.MissingParameter("testSetID")
	}

	// 查询测试集
	ts, err := svc.Get(req.TestSetID)
	if err != nil {
		return err
	}

	// 不在回收站中无法彻底删除
	if !ts.Recycled {
		return apierrors.ErrCleanTestSetFromRecycleBin.InvalidState("not in recycle bin")
	}

	// 获取回收站中测试集下测试用例列表
	_, tcIDs, err := svc.tcSvc.ListTestCases(apistructs.TestCaseListRequest{
		ProjectID:  ts.ProjectID,
		TestSetIDs: []uint64{ts.ID},
		Recycled:   true,
		IDOnly:     true,
	})
	if err != nil {
		return err
	}
	// 从回收站中彻底删除测试用例
	if len(tcIDs) > 0 {
		if err := svc.tcSvc.BatchCleanFromRecycleBin(apistructs.TestCaseBatchCleanFromRecycleBinRequest{
			TestCaseIDs:  tcIDs,
			IdentityInfo: req.IdentityInfo,
		}); err != nil {
			return err
		}
	}

	// 彻底删除测试集
	if err := svc.db.CleanTestSetFromRecycleBin(ts.ID); err != nil {
		return apierrors.ErrRecycleTestSet.InternalError(fmt.Errorf("failed to clean current testset from recycle bin, id: %d, err: %v", ts.ID, err))
	}

	// 递归回收子测试集
	subTestSets, err := svc.List(apistructs.TestSetListRequest{
		Recycled:  true,
		ParentID:  &ts.ID,
		ProjectID: &ts.ProjectID,
	})
	if err != nil {
		return err
	}
	for _, subTs := range subTestSets {
		if err := svc.CleanFromRecycleBin(apistructs.TestSetCleanFromRecycleBinRequest{
			TestSetID:    subTs.ID,
			IdentityInfo: req.IdentityInfo,
		}); err != nil {
			return err
		}
	}

	return nil
}

func (svc *Service) RecoverFromRecycleBin(req apistructs.TestSetRecoverFromRecycleBinRequest) error {
	// 参数校验
	if req.TestSetID == 0 {
		return apierrors.ErrRecoverTestSetFromRecycleBin.MissingParameter("testSetID")
	}
	if req.RecoverToTestSetID == nil {
		return apierrors.ErrRecoverTestSetFromRecycleBin.MissingParameter("recoverToTestSetID")
	}

	// 查询测试集
	ts, err := svc.Get(req.TestSetID)
	if err != nil {
		return err
	}

	// 不在回收站中无法恢复
	if !ts.Recycled {
		return apierrors.ErrRecoverTestSetFromRecycleBin.InvalidState("not in recycle bin")
	}

	// 查询目标测试集
	targetTs, err := svc.ensureGetTestSet(ts.ProjectID, *req.RecoverToTestSetID)
	if err != nil {
		return apierrors.ErrRecoverTestSetFromRecycleBin.InvalidParameter(err)
	}
	// 目标目录在回收站中无法恢复
	if targetTs.Recycled {
		return apierrors.ErrRecoverTestSetFromRecycleBin.InvalidState(fmt.Sprintf("target testset in recycle bin, id: %d", targetTs.ID))
	}

	// 恢复测试集下的测试用例
	_, tcIDs, err := svc.tcSvc.ListTestCases(apistructs.TestCaseListRequest{
		ProjectID:  ts.ProjectID,
		TestSetIDs: []uint64{ts.ID},
		Recycled:   true,
		IDOnly:     true,
	})
	if err != nil {
		return err
	}
	// 从回收站中恢复测试用例
	if len(tcIDs) > 0 {
		if err := svc.tcSvc.BatchUpdateTestCases(apistructs.TestCaseBatchUpdateRequest{
			Recycled:     &[]bool{false}[0],
			TestCaseIDs:  tcIDs,
			IdentityInfo: req.IdentityInfo,
		}); err != nil {
			return err
		}
	}

	// 恢复测试集至目标测试集
	// 不更新 orderNum，尝试恢复至原有位置
	polishedName, err := svc.GenerateTestSetName(ts.ProjectID, targetTs.ID, ts.ID, ts.Name)
	if err != nil {
		return apierrors.ErrRecoverTestSetFromRecycleBin.InternalError(fmt.Errorf("failed to polish testsetname, err: %v", err))
	}
	if err := svc.db.RecoverTestSet(ts.ID, targetTs.ID, polishedName); err != nil {
		return apierrors.ErrRecoverTestSetFromRecycleBin.InternalError(err)
	}

	// 递归恢复子测试集
	subTestSets, err := svc.List(apistructs.TestSetListRequest{
		Recycled:  true,
		ParentID:  &ts.ID,
		ProjectID: &ts.ProjectID,
	})
	if err != nil {
		return err
	}
	for _, subTs := range subTestSets {
		if err := svc.RecoverFromRecycleBin(apistructs.TestSetRecoverFromRecycleBinRequest{
			TestSetID:          subTs.ID,
			RecoverToTestSetID: &ts.ID,
			IdentityInfo:       req.IdentityInfo,
		}); err != nil {
			return err
		}
	}

	// 更新子测试集目录
	if err := svc.updateChildDirectory(ts.ProjectID, ts.ID); err != nil {
		logrus.Errorf("failed to update child directory when recover testset from recycle bin, testSetID: %d, err: %v", ts.ID, err)
	}

	return nil
}
