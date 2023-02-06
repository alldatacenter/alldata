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
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/i18n"
	"github.com/erda-project/erda/pkg/strutil"
)

func (svc *Service) CreateFileRecord(req apistructs.TestFileRecordRequest) (uint64, error) {
	record := &dao.TestFileRecord{
		FileName:    req.FileName,
		OrgID:       req.OrgID,
		Description: req.Description,
		ProjectID:   req.ProjectID,
		SpaceID:     req.SpaceID,
		Type:        req.Type,
		State:       req.State,
		ApiFileUUID: req.ApiFileUUID,
		OperatorID:  req.UserID,
		Extra:       convertTestFileExtra(req.Extra),
	}

	if err := svc.db.CreateRecord(record); err != nil {
		return 0, apierrors.ErrCreateFileRecord.InternalError(err)
	}
	return record.ID, nil
}

func convertTestFileExtra(fileExtra apistructs.TestFileExtra) dao.TestFileExtra {
	return dao.TestFileExtra{
		ManualTestFileExtraInfo:       fileExtra.ManualTestFileExtraInfo,
		AutotestSpaceFileExtraInfo:    fileExtra.AutotestSpaceFileExtraInfo,
		AutotestSceneSetFileExtraInfo: fileExtra.AutotestSceneSetFileExtraInfo,
		ProjectTemplateFileExtraInfo:  fileExtra.ProjectTemplateFileExtraInfo,
		ProjectPackageFileExtraInfo:   fileExtra.ProjectPackageFileExtraInfo,
		IssueFileExtraInfo:            fileExtra.IssueFileExtraInfo,
	}
}

func (svc *Service) GetFileRecord(id uint64, locale string) (*apistructs.TestFileRecord, error) {
	record, err := svc.db.GetRecord(id)
	if err != nil {
		return nil, apierrors.ErrGetFileRecord.InternalError(err)
	}
	l := svc.bdl.GetLocale(locale)
	project := l.Get(i18n.I18nKeyProjectName)
	testSet := l.Get(i18n.I18nKeyCaseSetName)
	return mapping(record, project, testSet), nil
}

func (svc *Service) UpdateFileRecord(req apistructs.TestFileRecordRequest) error {
	r, err := svc.db.GetRecord(req.ID)
	if err != nil {
		return apierrors.ErrUpdateFileRecord.InternalError(err)
	}

	if req.ApiFileUUID != "" {
		r.ApiFileUUID = req.ApiFileUUID
	}
	if req.State != "" {
		r.State = req.State
	}
	if req.Description != "" {
		r.Description = req.Description
	}
	if req.ErrorInfo != nil {
		errorInfo := fmt.Sprint(req.ErrorInfo)
		if err := strutil.Validate(errorInfo, strutil.MaxRuneCountValidator(apistructs.TestFileRecordErrorMaxLength)); err != nil {
			errorInfo = strutil.Truncate(errorInfo, apistructs.TestFileRecordErrorMaxLength)
		}
		r.ErrorInfo = errorInfo
	}

	return svc.db.UpdateRecord(r)
}

func (svc *Service) ListFileRecords(req apistructs.ListTestFileRecordsRequest) ([]apistructs.TestFileRecord, []string, map[string]int, int, error) {
	if req.PageSize < 1 {
		req.PageSize = 10
	}
	if req.PageNo < 1 {
		req.PageNo = 1
	}
	if req.ProjectName != "" {
		pros, err := svc.bdl.ListProject(req.UserID, apistructs.ProjectListRequest{Query: req.ProjectName, OrgID: req.OrgID, PageSize: 99, PageNo: 1})
		if err != nil {
			return nil, nil, nil, 0, apierrors.ErrListFileRecord.InternalError(err)
		}
		if pros == nil {
			return nil, nil, nil, 0, nil
		}
		req.ProjectIDs = make([]uint64, 0, len(pros.List))
		for _, pro := range pros.List {
			req.ProjectIDs = append(req.ProjectIDs, pro.ID)
		}
	}
	recordDtos, count, total, err := svc.db.ListRecordsByProject(req)
	if err != nil {
		return nil, nil, nil, 0, apierrors.ErrListFileRecord.InternalError(err)
	}

	records := make([]apistructs.TestFileRecord, 0)

	l := svc.bdl.GetLocale(req.Locale)
	project := l.Get(i18n.I18nKeyProjectName)
	testSet := l.Get(i18n.I18nKeyCaseSetName)

	operators := make([]string, 0)
	for _, i := range recordDtos {
		records = append(records, *mapping(&i, project, testSet))
		operators = append(operators, i.OperatorID)
	}
	return records, operators, count, total, nil
}

func (svc *Service) GetFirstFileReady(actionType ...apistructs.FileActionType) (bool, *dao.TestFileRecord, error) {
	ok, record, err := svc.db.FirstFileReady(actionType...)
	if err != nil || !ok {
		return false, nil, err
	}
	return true, record, nil
}

func (svc *Service) BatchClearProcessingRecords() error {
	return svc.db.BatchUpdateRecords()
}

func (svc *Service) DeleteRecordApiFilesByTime(t time.Time) error {
	UUIDs, err := svc.db.DeleteFileRecordByTime(t)
	if err != nil {
		return err
	}
	for _, UUID := range UUIDs {
		if UUID.ApiFileUUID == "" {
			continue
		}
		if err := svc.bdl.DeleteDiceFile(UUID.ApiFileUUID); err != nil {
			return err
		}
	}
	return nil
}

func mapping(s *dao.TestFileRecord, project, testSet string) *apistructs.TestFileRecord {
	record := &apistructs.TestFileRecord{
		ID:          s.ID,
		FileName:    s.FileName,
		OrgID:       s.OrgID,
		ProjectID:   s.ProjectID,
		ApiFileUUID: s.ApiFileUUID,
		TestSetID: func() uint64 {
			if info := s.Extra.ManualTestFileExtraInfo; info != nil {
				return info.TestSetID
			}
			return 0
		}(),
		SpaceID:     s.SpaceID,
		Description: s.Description,
		Type:        s.Type,
		State:       s.State,
		CreatedAt:   s.CreatedAt,
		UpdatedAt:   s.UpdatedAt,
		OperatorID:  s.OperatorID,
		ErrorInfo:   s.ErrorInfo,
	}

	if record.Type == apistructs.FileActionTypeImport || record.Type == apistructs.FileActionTypeExport {
		record.Description = fmt.Sprintf("%v ID: %v, %v ID: %v", project, record.ProjectID, testSet, record.TestSetID)
	}

	if record.State == apistructs.FileRecordStateFail && (record.Type == apistructs.FileActionTypeImport || record.Type == apistructs.FileActionTypeExport) {
		record.Description = s.ErrorInfo
	}
	extra := s.Extra
	if record.Type == apistructs.FileProjectTemplateExport {
		if extra.ProjectTemplateFileExtraInfo != nil && extra.ProjectTemplateFileExtraInfo.ExportRequest != nil {
			record.ProjectName = extra.ProjectTemplateFileExtraInfo.ExportRequest.ProjectName
			record.ProjectDisplayName = extra.ProjectTemplateFileExtraInfo.ExportRequest.ProjectDisplayName
		}
	}
	if record.Type == apistructs.FileProjectTemplateImport {
		if extra.ProjectTemplateFileExtraInfo != nil && extra.ProjectTemplateFileExtraInfo.ImportRequest != nil {
			record.ProjectName = extra.ProjectTemplateFileExtraInfo.ImportRequest.ProjectName
			record.ProjectDisplayName = extra.ProjectTemplateFileExtraInfo.ImportRequest.ProjectDisplayName
		}
	}

	if record.Type == apistructs.FileProjectPackageExport {
		if extra.ProjectPackageFileExtraInfo != nil && extra.ProjectPackageFileExtraInfo.ExportRequest != nil {
			record.ProjectName = extra.ProjectPackageFileExtraInfo.ExportRequest.ProjectName
			record.ProjectDisplayName = extra.ProjectPackageFileExtraInfo.ExportRequest.ProjectDisplayName
		}
	}
	if record.Type == apistructs.FileProjectPackageImport {
		if extra.ProjectPackageFileExtraInfo != nil && extra.ProjectPackageFileExtraInfo.ImportRequest != nil {
			record.ProjectName = extra.ProjectPackageFileExtraInfo.ImportRequest.ProjectName
			record.ProjectDisplayName = extra.ProjectPackageFileExtraInfo.ImportRequest.ProjectDisplayName
		}
	}

	return record
}
