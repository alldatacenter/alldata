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
	"io/ioutil"
	"os"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/i18n"
	"github.com/erda-project/erda/pkg/excel"
	"github.com/erda-project/erda/pkg/xmind"
)

const (
	maxAllowedNumberForTestCaseNumberExport = 2000
	defaultResource                         = "testcases"
)

func (svc *Service) Export(req apistructs.TestCaseExportRequest) (uint64, error) {
	// 参数校验
	if !req.FileType.Valid() {
		return 0, apierrors.ErrExportTestCases.InvalidParameter("fileType")
	}

	beginPaging := time.Now()
	// 根据分页查询条件，获取总数，进行优化
	req.PageNo = -1
	req.PageSize = -1
	totalResult, err := svc.PagingTestCases(req.TestCasePagingRequest)
	if err != nil {
		return 0, err
	}
	endPaging := time.Now()
	logrus.Debugf("export paging testcases cost: %fs", endPaging.Sub(beginPaging).Seconds())

	// limit
	if totalResult.Total > maxAllowedNumberForTestCaseNumberExport && req.FileType == apistructs.TestCaseFileTypeExcel {
		return 0, apierrors.ErrExportTestCases.InvalidParameter(
			fmt.Sprintf("to many testcases: %d, max allowed number for export excel is: %d, please use xmind", totalResult.Total, maxAllowedNumberForTestCaseNumberExport))
	}
	l := svc.bdl.GetLocale(req.Locale)
	sheetName := l.Get(i18n.I18nKeyTestCaseSheetName, defaultResource)
	if req.FileType == apistructs.TestCaseFileTypeExcel {
		sheetName += ".xlsx"
	} else {
		sheetName += ".xmind"
	}
	fileReq := apistructs.TestFileRecordRequest{
		FileName:     sheetName,
		Description:  fmt.Sprintf("ProjectID: %v, TestsetID: %v", req.ProjectID, req.TestSetID),
		ProjectID:    req.ProjectID,
		Type:         apistructs.FileActionTypeExport,
		State:        apistructs.FileRecordStatePending,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			ManualTestFileExtraInfo: &apistructs.ManualTestFileExtraInfo{
				TestSetID:     req.TestSetID,
				ExportRequest: &req,
			},
		},
	}
	id, err := svc.CreateFileRecord(fileReq)
	if err != nil {
		return 0, err
	}

	return id, nil
}

func (svc *Service) ExportFile(record *dao.TestFileRecord) {
	req := record.Extra.ManualTestFileExtraInfo.ExportRequest
	id := record.ID
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
		return
	}
	fileUUID, err := svc.ExportTestCases(req, record.FileName)
	if err != nil {
		logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
		if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail, ErrorInfo: err}); err != nil {
			logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
		}
		return
	}
	if err := svc.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateSuccess, ApiFileUUID: fileUUID}); err != nil {
		logrus.Error(apierrors.ErrImportTestCases.InternalError(err))
	}
}

func (svc *Service) ExportTestCases(req *apistructs.TestCaseExportRequest, sheetName string) (string, error) {
	req.PageNo = -1
	req.PageSize = -1
	totalResult, err := svc.PagingTestCases(req.TestCasePagingRequest)
	if err != nil {
		return "", err
	}

	var testCases []apistructs.TestCaseWithSimpleSetInfo
	for _, ts := range totalResult.TestSets {
		for _, tc := range ts.TestCases {
			testCases = append(testCases, apistructs.TestCaseWithSimpleSetInfo{TestCase: tc, Directory: ts.Directory})
		}
	}

	f, err := ioutil.TempFile("", "export.*")
	if err != nil {
		return "", apierrors.ErrExportTestCases.InternalError(err)
	}

	defer func() {
		if err := os.Remove(f.Name()); err != nil {
			logrus.Error(apierrors.ErrExportTestCases.InternalError(err))
		}
	}()

	defer f.Close()

	if req.FileType == apistructs.TestCaseFileTypeExcel {
		excelLines, err := svc.convert2Excel(testCases, req.Locale)
		if err != nil {
			return "", apierrors.ErrExportTestCases.InternalError(err)
		}

		if err := excel.Export(f, excelLines, sheetName); err != nil {
			return "", apierrors.ErrExportTestCases.InternalError(err)
		}
	} else {
		xmindContent, err := svc.convert2XMind(testCases, req.Locale)
		if err != nil {
			return "", apierrors.ErrExportTestCases.InternalError(err)
		}

		if err := xmind.Export(f, xmindContent, sheetName); err != nil {
			return "", apierrors.ErrExportTestCases.InternalError(err)
		}
	}

	//Set offset for next read
	f.Seek(0, 0)

	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: sheetName,
		FileReader:      f,
		From:            defaultResource,
		IsPublic:        true,
		ExpiredAt:       nil,
	}
	file, err := svc.bdl.UploadFile(uploadReq)
	if err != nil {
		return "", apierrors.ErrExportTestCases.InternalError(err)
	}
	return file.UUID, nil
}
