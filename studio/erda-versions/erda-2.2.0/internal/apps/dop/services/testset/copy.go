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
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) Copy(req apistructs.TestSetCopyRequest) (uint64, bool, error) {
	// 参数校验
	var isAsync bool
	if req.TestSetID == 0 {
		return 0, isAsync, apierrors.ErrCopyTestSet.InvalidParameter("cannot copy root testset")
	}
	if req.CopyToTestSetID == req.TestSetID {
		return 0, isAsync, apierrors.ErrCopyTestSet.InvalidParameter("cannot copy to itself")
	}

	// 查询待拷贝测试集
	srcTs, err := svc.Get(req.TestSetID)
	if err != nil {
		return 0, isAsync, err
	}

	// 查询目标测试集
	var dstTs *apistructs.TestSet
	if req.CopyToTestSetID != 0 {
		dstTs, err = svc.Get(req.CopyToTestSetID)
		if err != nil {
			return 0, isAsync, err
		}
		findInSub, err := svc.findTargetTestSetIDInSubTestSets([]uint64{srcTs.ID}, srcTs.ProjectID, dstTs.ID)
		if err != nil {
			return 0, isAsync, apierrors.ErrCopyTestSet.InternalError(err)
		}
		if findInSub {
			return 0, isAsync, apierrors.ErrCopyTestSet.InvalidParameter("cannot copy to sub testset")
		}
	}

	pagingReq := apistructs.TestCasePagingRequest{
		PageNo:    -1,
		PageSize:  -1,
		TestSetID: req.TestSetID,
		ProjectID: srcTs.ProjectID,
		Recycled:  false,
	}
	totalResult, err := svc.tcSvc.PagingTestCases(pagingReq)
	if err != nil {
		return 0, isAsync, apierrors.ErrCopyTestSet.InternalError(err)
	}
	if int(totalResult.Total) <= conf.TestSetSyncCopyMaxNum() {
		copiedTsIDs, err := svc.recursiveCopy(srcTs, dstTs, req.IdentityInfo)
		if err != nil {
			return 0, isAsync, apierrors.ErrCopyTestSet.InternalError(err)
		}
		return copiedTsIDs[0], isAsync, nil
	}

	isAsync = true
	fileReq := apistructs.TestFileRecordRequest{
		Description:  fmt.Sprintf("ProjectID: %v, TestsetID: %v", srcTs.ProjectID, req.TestSetID),
		ProjectID:    srcTs.ProjectID,
		Type:         apistructs.FileActionTypeCopy,
		State:        apistructs.FileRecordStatePending,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			ManualTestFileExtraInfo: &apistructs.ManualTestFileExtraInfo{
				TestSetID: req.TestSetID,
				CopyRequest: &apistructs.TestSetCopyAsyncRequest{
					SourceTestSet: srcTs,
					DestTestSet:   dstTs,
					IdentityInfo:  req.IdentityInfo,
				},
			},
		},
	}

	id, err := svc.tcSvc.CreateFileRecord(fileReq)
	if err != nil {
		return 0, isAsync, err
	}

	return id, isAsync, nil
}

func (svc *Service) CopyTestSet(record *dao.TestFileRecord) {
	req := record.Extra.ManualTestFileExtraInfo.CopyRequest
	if err := svc.tcSvc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrCopyTestSet.InternalError(err))
		return
	}

	if _, err := svc.recursiveCopy(req.SourceTestSet, req.DestTestSet, req.IdentityInfo); err != nil {
		logrus.Error(apierrors.ErrCopyTestSet.InternalError(err))
		if err := svc.tcSvc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateFail}); err != nil {
			logrus.Error(apierrors.ErrCopyTestSet.InternalError(err))
		}
		return
	}

	if err := svc.tcSvc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: record.ID, State: apistructs.FileRecordStateSuccess}); err != nil {
		logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
	}
}

func (svc *Service) recursiveCopy(srcTs, dstTs *apistructs.TestSet, identityInfo apistructs.IdentityInfo) ([]uint64, error) {
	var newTsIDs []uint64
	if dstTs == nil {
		dstTs = &apistructs.TestSet{
			ID:        0,
			ProjectID: srcTs.ProjectID,
			ParentID:  0,
			Recycled:  srcTs.Recycled,
			Directory: "/",
			Order:     0,
		}
	}
	// 先创建新测试集
	copiedTs, err := svc.Create(apistructs.TestSetCreateRequest{
		ProjectID: &srcTs.ProjectID,
		ParentID:  &dstTs.ID,
		Name:      srcTs.Name,
	})
	if err != nil {
		return nil, err
	}
	newTsIDs = append(newTsIDs, copiedTs.ID)

	// 将 原测试集下的用例复制到新测试集
	_, waitCopyTcIDs, err := svc.tcSvc.ListTestCases(apistructs.TestCaseListRequest{
		ProjectID:  srcTs.ProjectID,
		TestSetIDs: []uint64{srcTs.ID},
		Recycled:   false,
		IDOnly:     true,
	})
	if err != nil {
		return nil, err
	}
	if len(waitCopyTcIDs) > 0 {
		_, err = svc.tcSvc.BatchCopyTestCases(apistructs.TestCaseBatchCopyRequest{
			CopyToTestSetID: copiedTs.ID,
			ProjectID:       copiedTs.ProjectID,
			TestCaseIDs:     waitCopyTcIDs,
			IdentityInfo:    identityInfo,
		})
		if err != nil {
			return nil, err
		}
	}

	// 递归调用子测试集
	subTestSets, err := svc.List(apistructs.TestSetListRequest{
		Recycled:  false,
		ParentID:  &srcTs.ID,
		ProjectID: &srcTs.ProjectID,
	})
	if err != nil {
		return nil, err
	}
	for _, subTs := range subTestSets {
		newSubTsIDs, err := svc.recursiveCopy(&subTs, copiedTs, identityInfo)
		if err != nil {
			return nil, err
		}
		newTsIDs = append(newTsIDs, newSubTsIDs...)
	}
	return newTsIDs, nil
}
