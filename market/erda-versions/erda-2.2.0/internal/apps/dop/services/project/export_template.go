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

package project

import (
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

type TemplateDB struct {
	ProjectID   uint64 `json:"projectID"`
	ProjectName string `json:"projectName"`
	OrgID       int64  `json:"orgID"`
	Data        *apistructs.ProjectTemplateData
	bdl         *bundle.Bundle
	packageName string
	apistructs.IdentityInfo
}

func (t *TemplateDB) InitData() {
	t.Data = &apistructs.ProjectTemplateData{Version: version}
}

func (t *TemplateDB) SetApplications() error {
	appDtos, err := t.bdl.GetAppList(strconv.FormatInt(t.OrgID, 10), t.UserID, apistructs.ApplicationListRequest{
		ProjectID: t.ProjectID,
		IsSimple:  false,
		PageSize:  9999,
		PageNo:    1,
	})
	if err != nil {
		return err
	}
	t.Data.Applications = appDtos.List
	return nil
}

func (t *TemplateDB) SetMeta() error {
	org, err := t.bdl.GetOrg(t.OrgID)
	if err != nil {
		return err
	}
	t.Data.Meta.ProjectName = t.ProjectName
	t.Data.Meta.OrgName = org.Name
	t.Data.Meta.Source = "erda"
	return nil
}

func (t *TemplateDB) GetProjectTemplate() *apistructs.ProjectTemplateData {
	return t.Data
}

func (t *TemplateDB) GetPackageName() string {
	return t.packageName
}

func (t *TemplateDB) GetProjectID() uint64 {
	return t.ProjectID
}

func (t *TemplateDB) GetIdentityInfo() apistructs.IdentityInfo {
	return t.IdentityInfo
}

func (p *Project) ExportTemplatePackage(record *dao.TestFileRecord) {
	extra := record.Extra.ProjectTemplateFileExtraInfo
	if extra == nil || extra.ExportRequest == nil {
		logrus.Errorf("project template export missing request data")
		return
	}

	req := extra.ExportRequest
	id := record.ID
	if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrExportProjectTemplate.InternalError(err))
		return
	}
	tempDB := TemplateDB{
		OrgID:        req.OrgID,
		ProjectID:    req.ProjectID,
		ProjectName:  req.ProjectName,
		IdentityInfo: req.IdentityInfo,
		bdl:          p.bdl,
		packageName:  record.FileName,
	}
	tempDirector := TemplateDataDirector{}
	tempDirector.New(&tempDB, p.bdl, p.namespace)
	if err := tempDirector.Construct(); err != nil {
		logrus.Error(apierrors.ErrExportProjectTemplate.InternalError(err))
		if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail, ErrorInfo: tempDirector.GenErrInfo()}); err != nil {
			logrus.Error(apierrors.ErrExportProjectTemplate.InternalError(err))
		}
		return
	}

	uuid, err := tempDirector.GenAndUploadZipPackage()
	if err != nil {
		logrus.Error(apierrors.ErrExportProjectTemplate.InternalError(err))
		if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail, ErrorInfo: tempDirector.GenErrInfo()}); err != nil {
			logrus.Error(apierrors.ErrExportProjectTemplate.InternalError(err))
		}
		return
	}

	if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateSuccess, ApiFileUUID: uuid}); err != nil {
		logrus.Error(apierrors.ErrExportProjectTemplate.InternalError(err))
		return
	}
}
