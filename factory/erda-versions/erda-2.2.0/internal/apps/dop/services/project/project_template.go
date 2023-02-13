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
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/mholt/archiver"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/namespace"
	"github.com/erda-project/erda/pkg/filehelper"
)

const (
	tempPrefix            = "export.*"
	projectYmlName        = "project.yml"
	metadataYmlName       = "metadata.yml"
	packageResource       = "project-template"
	packageSuffix         = ".zip"
	packageValidityPeriod = 7 * time.Hour * 24
	version               = "1.0"
)

type TemplateDataCreator interface {
	GetPackageName() string
	InitData()
	SetApplications() error
	SetMeta() error
	GetProjectTemplate() *apistructs.ProjectTemplateData
	GetProjectID() uint64
	GetIdentityInfo() apistructs.IdentityInfo
}

type TemplateDataDirector struct {
	Creator       TemplateDataCreator
	succeedAppNum int
	failedAppNum  int
	bdl           *bundle.Bundle
	namespace     *namespace.Namespace
	errs          []error
}

func (t *TemplateDataDirector) New(creator TemplateDataCreator, bdl *bundle.Bundle, namespace *namespace.Namespace) {
	t.Creator = creator
	t.bdl = bdl
	t.namespace = namespace
	t.errs = make([]error, 0)
}

func (t *TemplateDataDirector) Construct() error {
	t.Creator.InitData()
	if err := t.Creator.SetApplications(); err != nil {
		t.errs = append(t.errs, err)
		return err
	}
	if err := t.Creator.SetMeta(); err != nil {
		t.errs = append(t.errs, err)
		return err
	}
	return nil
}

func (t *TemplateDataDirector) CheckTemplatePackage() error {
	tempData := t.Creator.GetProjectTemplate()
	appSet := make(map[string]struct{})
	duplicatedApp := make([]string, 0)
	for _, app := range tempData.Applications {
		if _, ok := appSet[app.Name]; ok {
			duplicatedApp = append(duplicatedApp, app.Name)
		}
		appSet[app.Name] = struct{}{}
	}
	if len(duplicatedApp) > 0 {
		return fmt.Errorf("template package contain duplicated application: %s", strings.Join(duplicatedApp, ","))
	}
	return nil
}

func (t *TemplateDataDirector) GenAndUploadZipPackage() (string, error) {
	ymlBytes, err := yaml.Marshal(t.Creator.GetProjectTemplate())
	if err != nil {
		t.errs = append(t.errs, err)
		return "", err
	}
	zipTmpFile, err := ioutil.TempFile("", tempPrefix)
	if err != nil {
		t.errs = append(t.errs, err)
		return "", err
	}
	defer func() {
		if err := os.Remove(zipTmpFile.Name()); err != nil {
			logrus.Error(apierrors.ErrExportProjectTemplate.InternalError(err))
		}
	}()
	defer zipTmpFile.Close()

	tmpDir := os.TempDir()
	projectYmlPath := filepath.Join(tmpDir, projectYmlName)
	if err := filehelper.CreateFile(projectYmlPath, string(ymlBytes), 0644); err != nil {
		t.errs = append(t.errs, err)
		return "", err
	}
	// write metadata
	metadataYmlPath := filepath.Join(tmpDir, metadataYmlName)
	metaBytes, _ := yaml.Marshal(t.Creator.GetProjectTemplate().Meta)
	if err := filehelper.CreateFile(metadataYmlPath, string(metaBytes), 0644); err != nil {
		t.errs = append(t.errs, err)
		return "", err
	}
	if err := archiver.Zip.Write(zipTmpFile, []string{projectYmlPath, metadataYmlPath}); err != nil {
		t.errs = append(t.errs, err)
		return "", err
	}
	zipTmpFile.Seek(0, 0)
	expiredAt := time.Now().Add(packageValidityPeriod)
	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: t.Creator.GetPackageName(),
		FileReader:      zipTmpFile,
		From:            packageResource,
		IsPublic:        true,
		ExpiredAt:       &expiredAt,
	}
	file, err := t.bdl.UploadFile(uploadReq)
	if err != nil {
		t.errs = append(t.errs, err)
		return "", err
	}
	return file.UUID, nil
}

func (t *TemplateDataDirector) TryCreateAppsByTemplate() error {
	tempData := t.Creator.GetProjectTemplate()
	identityInfo := t.Creator.GetIdentityInfo()
	projectID := t.Creator.GetProjectID()
	for _, tempApp := range tempData.Applications {
		appReq := apistructs.ApplicationCreateRequest{
			Name:           tempApp.Name,
			DisplayName:    tempApp.DisplayName,
			Logo:           tempApp.Logo,
			Desc:           tempApp.Desc,
			ProjectID:      projectID,
			Mode:           apistructs.ApplicationMode(tempApp.Mode),
			Config:         tempApp.Config,
			IsExternalRepo: false,
		}
		newApp, err := t.bdl.CreateAppWithRepo(appReq, identityInfo.UserID)
		if err != nil {
			t.failedAppNum++
			t.errs = append(t.errs, fmt.Errorf("create application %s failed: %v", tempApp.Name, err))
			logrus.Errorf("%s failed to create app: %s, err: %v", packageResource, tempApp.Name, err)
			continue
		}
		t.namespace.GenerateAppExtraInfo(int64(newApp.ID), int64(newApp.ProjectID))
		t.succeedAppNum++
	}
	return nil
}

func (t *TemplateDataDirector) GenErrInfo() error {
	if len(t.errs) > 0 {
		errInfos := make([]string, 0, len(t.errs))
		for _, err := range t.errs {
			errInfos = append(errInfos, fmt.Sprintf("%v", err))
		}
		return fmt.Errorf("%s", strings.Join(errInfos, "\n"))
	}
	return nil
}

func (t *TemplateDataDirector) GenDesc() string {
	return fmt.Sprintf("Imported apps succeed: %d, failed: %d", t.succeedAppNum, t.failedAppNum)
}

func (p *Project) ParseTemplatePackage(r io.ReadCloser) (*apistructs.ProjectTemplateData, error) {
	buff := bytes.NewBuffer([]byte{})
	size, err := io.Copy(buff, r)
	if err != nil {
		return nil, err
	}
	reader := bytes.NewReader(buff.Bytes())
	zipReader, err := zip.NewReader(reader, size)
	if err != nil {
		return nil, err
	}
	tempZip := TemplateZip{reader: zipReader}
	tempDirector := TemplateDataDirector{}
	tempDirector.New(&tempZip, p.bdl, p.namespace)
	if err := tempDirector.Construct(); err != nil {
		return nil, err
	}
	if err := tempDirector.CheckTemplatePackage(); err != nil {
		return nil, err
	}
	return tempDirector.Creator.GetProjectTemplate(), nil
}

func (p *Project) ExportTemplate(req apistructs.ExportProjectTemplateRequest) (uint64, error) {
	packageName := fmt.Sprintf("%s.zip", p.MakeTemplatePackageName(req.ProjectName))
	fileReq := apistructs.TestFileRecordRequest{
		FileName:     packageName,
		Description:  fmt.Sprintf("export project: %s template", req.ProjectDisplayName),
		OrgID:        uint64(req.OrgID),
		Type:         apistructs.FileProjectTemplateExport,
		State:        apistructs.FileRecordStatePending,
		ProjectID:    req.ProjectID,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			ProjectTemplateFileExtraInfo: &apistructs.ProjectTemplateFileExtraInfo{
				ExportRequest: &req,
			},
		},
	}
	id, err := p.CreateFileRecord(fileReq)
	if err != nil {
		return 0, err
	}

	return id, nil
}

func (p *Project) ImportTemplate(req apistructs.ImportProjectTemplateRequest, r *http.Request) (uint64, error) {
	f, fileHeader, err := r.FormFile("file")
	if err != nil {
		return 0, err
	}
	defer f.Close()

	if !strings.HasSuffix(fileHeader.Filename, packageSuffix) {
		return 0, fmt.Errorf("project template file must be a zip package")
	}
	expiredAt := time.Now().Add(packageValidityPeriod)
	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: fileHeader.Filename,
		FileReader:      f,
		From:            packageResource,
		IsPublic:        true,
		ExpiredAt:       &expiredAt,
	}
	uploadRecord, err := p.bdl.UploadFile(uploadReq)
	if err != nil {
		return 0, err
	}

	fileReq := apistructs.TestFileRecordRequest{
		FileName:     fileHeader.Filename,
		Description:  fmt.Sprintf("import project: %s template", req.ProjectDisplayName),
		OrgID:        uint64(req.OrgID),
		ProjectID:    req.ProjectID,
		Type:         apistructs.FileProjectTemplateImport,
		State:        apistructs.FileRecordStatePending,
		IdentityInfo: req.IdentityInfo,
		ApiFileUUID:  uploadRecord.UUID,
		Extra: apistructs.TestFileExtra{
			ProjectTemplateFileExtraInfo: &apistructs.ProjectTemplateFileExtraInfo{
				ImportRequest: &req,
			},
		},
	}
	id, err := p.CreateFileRecord(fileReq)
	if err != nil {
		return 0, err
	}
	return id, nil
}

func (p *Project) MakeTemplatePackageName(origin string) string {
	return fmt.Sprintf("%s_%s", origin, time.Now().Format("20060102150405"))
}

func (p *Project) updateTemplateFileRecord(id uint64, state apistructs.FileRecordState, desc string, err error) error {
	if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: state, Description: desc, ErrorInfo: err}); err != nil {
		logrus.Errorf("%s failed to update file record, err: %v", packageResource, err)
		return err
	}
	return nil
}
