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
	"archive/zip"
	"bytes"
	"io"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

type TemplateZip struct {
	reader      *zip.Reader
	ProjectID   uint64 `json:"projectID"`
	ProjectName string `json:"projectName"`
	OrgID       int64  `json:"orgID"`
	Data        *apistructs.ProjectTemplateData
	bdl         *bundle.Bundle
	packageName string
	apistructs.IdentityInfo
}

func (t *TemplateZip) InitData() {
	t.Data = &apistructs.ProjectTemplateData{}
}

func (t *TemplateZip) SetApplications() error {
	projectYml, err := t.reader.Open(projectYmlName)
	if err != nil {
		return err
	}
	defer projectYml.Close()

	ymlBytes, err := io.ReadAll(projectYml)
	if err != nil {
		return err
	}
	if err := yaml.Unmarshal(ymlBytes, t.Data); err != nil {
		return err
	}
	return nil
}

func (t *TemplateZip) SetMeta() error {
	return nil
}

func (t *TemplateZip) GetPackageName() string {
	return t.packageName
}

func (t *TemplateZip) GetProjectTemplate() *apistructs.ProjectTemplateData {
	return t.Data
}

func (t *TemplateZip) GetProjectID() uint64 {
	return t.ProjectID
}

func (t *TemplateZip) GetIdentityInfo() apistructs.IdentityInfo {
	return t.IdentityInfo
}

func (p *Project) ImportTemplatePackage(record *dao.TestFileRecord) {
	extra := record.Extra.ProjectTemplateFileExtraInfo
	if extra == nil || extra.ImportRequest == nil {
		logrus.Errorf("%s import func missing request data", packageResource)
		return
	}

	req := extra.ImportRequest
	id := record.ID
	// change record state to processing
	if err := p.updateTemplateFileRecord(id, apistructs.FileRecordStateProcessing, "", nil); err != nil {
		return
	}

	// download zip package
	f, err := p.bdl.DownloadDiceFile(record.ApiFileUUID)
	if err != nil {
		p.updateTemplateFileRecord(id, apistructs.FileRecordStateFail, "", err)
		return
	}
	defer f.Close()

	buff := bytes.NewBuffer([]byte{})
	size, err := io.Copy(buff, f)
	if err != nil {
		logrus.Errorf("%s failed to read package, err: %v", packageResource, err)
		p.updateTemplateFileRecord(id, apistructs.FileRecordStateFail, "", err)
		return
	}
	reader := bytes.NewReader(buff.Bytes())
	zipReader, err := zip.NewReader(reader, size)
	if err != nil {
		logrus.Errorf("%s failed to make zip reader, err: %v", packageResource, err)
		p.updateTemplateFileRecord(id, apistructs.FileRecordStateFail, "", err)
		return
	}

	tempZip := TemplateZip{
		reader:       zipReader,
		OrgID:        req.OrgID,
		ProjectID:    req.ProjectID,
		ProjectName:  req.ProjectName,
		IdentityInfo: req.IdentityInfo,
		bdl:          p.bdl,
		packageName:  record.FileName,
	}
	tempDirector := TemplateDataDirector{}
	tempDirector.New(&tempZip, p.bdl, p.namespace)
	if err := tempDirector.Construct(); err != nil {
		logrus.Errorf("%s failed to construct template data, err: %v", packageResource, err)
		p.updateTemplateFileRecord(id, apistructs.FileRecordStateFail, "", tempDirector.GenErrInfo())
		return
	}

	if err := tempDirector.TryCreateAppsByTemplate(); err != nil {
		logrus.Errorf("%s failed to create apps by template, err: %v", packageResource, err)
		p.updateTemplateFileRecord(id, apistructs.FileRecordStateFail, "", tempDirector.GenErrInfo())
		return
	}

	if errInfo := tempDirector.GenErrInfo(); errInfo != nil {
		p.updateTemplateFileRecord(id, apistructs.FileRecordStateFail, tempDirector.GenDesc(), errInfo)
		return
	}

	p.updateTemplateFileRecord(id, apistructs.FileRecordStateSuccess, "", tempDirector.GenErrInfo())
}
