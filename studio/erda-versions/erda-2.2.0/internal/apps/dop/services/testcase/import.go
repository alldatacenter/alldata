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
	"net/http"

	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

// Import 导入测试用例
func (svc *Service) Import(req apistructs.TestCaseImportRequest, r *http.Request) (*apistructs.TestCaseImportResult, error) {
	// 参数校验
	if !req.FileType.Valid() {
		return nil, apierrors.ErrImportTestCases.InvalidParameter("fileType")
	}
	if req.ProjectID == 0 {
		return nil, apierrors.ErrImportTestCases.MissingParameter("projectID")
	}

	// fake ts
	ts := dao.FakeRootTestSet(req.ProjectID, false)
	if req.TestSetID != 0 {
		_ts, err := svc.db.GetTestSetByID(req.TestSetID)
		if err != nil {
			if gorm.IsRecordNotFoundError(err) {
				return nil, apierrors.ErrImportTestCases.InvalidParameter(fmt.Errorf("testSet not found, id: %d", req.TestSetID))
			}
			return nil, apierrors.ErrImportTestCases.InternalError(err)
		}
		ts = *_ts
	}
	if ts.ProjectID != req.ProjectID {
		return nil, apierrors.ErrImportTestCases.InvalidParameter("projectID")
	}

	// get testsets data
	f, fileHeader, err := r.FormFile("file")
	if err != nil {
		return nil, err
	}
	defer f.Close()

	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: fileHeader.Filename,
		ByteSize:        fileHeader.Size,
		FileReader:      f,
		From:            "testcase",
		IsPublic:        true,
		ExpiredAt:       nil,
	}
	file, err := svc.bdl.UploadFile(uploadReq)
	if err != nil {
		return nil, err
	}

	fileReq := apistructs.TestFileRecordRequest{
		FileName:     fileHeader.Filename,
		Description:  fmt.Sprintf("ProjectID: %v, TestsetID: %v", req.ProjectID, req.TestSetID),
		ProjectID:    req.ProjectID,
		Type:         apistructs.FileActionTypeImport,
		ApiFileUUID:  file.UUID,
		State:        apistructs.FileRecordStatePending,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			ManualTestFileExtraInfo: &apistructs.ManualTestFileExtraInfo{
				TestSetID:     req.TestSetID,
				ImportRequest: &req,
			},
		},
	}
	id, err := svc.CreateFileRecord(fileReq)
	if err != nil {
		return nil, err
	}
	return &apistructs.TestCaseImportResult{Id: id}, nil
}

func (svc *Service) ImportFile(record *dao.TestFileRecord) {
	req := record.Extra.ManualTestFileExtraInfo.ImportRequest
	id := record.ID
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
		return
	}
	if err := svc.ImportTestCases(req, record.ApiFileUUID); err != nil {
		logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail, ErrorInfo: err}); err != nil {
			logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
		}
		return
	}
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateSuccess}); err != nil {
		logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
	}
}

func (svc *Service) ImportTestCases(req *apistructs.TestCaseImportRequest, testFileUUID string) error {
	ts := dao.FakeRootTestSet(req.ProjectID, false)
	if req.TestSetID != 0 {
		_ts, err := svc.db.GetTestSetByID(req.TestSetID)
		if err != nil {
			if gorm.IsRecordNotFoundError(err) {
				return apierrors.ErrImportTestCases.InvalidParameter(fmt.Errorf("testSet not found, id: %d", req.TestSetID))
			}
			return apierrors.ErrImportTestCases.InternalError(err)
		}
		ts = *_ts
	}
	if ts.ProjectID != req.ProjectID {
		return apierrors.ErrImportTestCases.InvalidParameter("projectID")
	}

	f, err := svc.bdl.DownloadDiceFile(testFileUUID)
	if err != nil {
		return err
	}

	if req.FileType == apistructs.TestCaseFileTypeExcel {
		excelTcs, err := svc.decodeFromExcelFile(f)
		if err != nil {
			return apierrors.ErrImportTestCases.InternalError(err)
		}
		if _, err := svc.storeExcel2DB(*req, ts, excelTcs); err != nil {
			return apierrors.ErrImportTestCases.InternalError(err)
		}
	} else {
		xmindTcs, err := svc.decodeFromXMindFile(f)
		if err != nil {
			return apierrors.ErrImportTestCases.InternalError(err)
		}
		if _, err := svc.storeXmind2DB(*req, ts, xmindTcs); err != nil {
			return apierrors.ErrImportTestCases.InternalError(err)
		}
	}

	return nil
}
