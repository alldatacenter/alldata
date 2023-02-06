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
	"io/fs"
	"io/ioutil"
	"os"
	"path"
	"strings"
	"text/template"

	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

type PackageZip struct {
	reader         *zip.Reader
	zipDir         string
	ProjectPackage *apistructs.ProjectPackage
	bdl            *bundle.Bundle
	packageName    string
	contex         *PackageContext
	TmpDir         string
}

func (t *PackageZip) InitData() {
	t.ProjectPackage = &apistructs.ProjectPackage{}
	zipDir, inDir := zipInDirectory(t.reader)
	if inDir {
		t.zipDir = zipDir
	}
}

func (t *PackageZip) SetProject() error {
	err := t.readProject()
	if err != nil {
		return err
	}

	err = t.readValues()
	if err != nil {
		return err
	}

	t.ProjectPackage.Project.Environments.Envs = map[string]apistructs.ProjectEnvironment{}
	for _, envInclude := range t.ProjectPackage.Project.Environments.Include {
		err = t.readIncludeEnvFile(envInclude)
		if err != nil {
			return err
		}
	}

	return nil
}

func (t *PackageZip) openZipReader(zipfile string) (fs.File, error) {
	if t.zipDir != "" {
		f := path.Join(t.zipDir, zipfile)
		return t.reader.Open(f)
	}

	return t.reader.Open(zipfile)
}

func zipInDirectory(zipReader *zip.Reader) (string, bool) {
	var prefix string
	var hasProjectYml, hasMetadataYml, hasValuesYml bool
	for _, f := range zipReader.File {
		if !strings.HasPrefix(f.Name, "__") {
			splits := strings.SplitN(f.Name, "/", 2)
			if len(splits) == 2 {
				if prefix == "" {
					prefix = splits[0]
				} else {
					if prefix != splits[0] {
						return "", false
					}
				}

				switch splits[1] {
				case "project.yml":
					hasProjectYml = true
				case "metadata.yml":
					hasMetadataYml = true
				case "values.yml":
					hasValuesYml = true
				}
			} else {
				return "", false
			}
		}
	}

	if hasValuesYml && hasMetadataYml && hasProjectYml {
		return prefix, true
	} else {
		return "", false
	}
}

func (t *PackageZip) readProject() error {
	projectYml, err := t.openZipReader(packageProjectYmlName)
	if err != nil {
		return err
	}
	defer projectYml.Close()

	ymlBytes, err := io.ReadAll(projectYml)
	if err != nil {
		return err
	}
	if err := yaml.Unmarshal(ymlBytes, &t.ProjectPackage.Project); err != nil {
		return err
	}

	for _, app := range t.ProjectPackage.Project.Applications {
		if app.ZipRepo == "" {
			continue
		}
		filename := path.Join(t.TmpDir, app.ZipRepo)
		err = t.readZipfile(app.ZipRepo, filename)
		if err != nil {
			return err
		}

		app.ZipRepo = filename
	}

	for _, artifact := range t.ProjectPackage.Project.Artifacts {
		filename := path.Join(t.TmpDir, artifact.ZipFile)
		err = t.readZipfile(artifact.ZipFile, filename)
		if err != nil {
			return err
		}

		artifact.ZipFile = filename
	}
	return nil
}

func (t *PackageZip) readIncludeEnvFile(envfile string) error {
	envYml, err := t.openZipReader(envfile)
	if err != nil {
		return err
	}
	defer envYml.Close()

	ymlBytes, err := io.ReadAll(envYml)
	if err != nil {
		return err
	}

	values := t.ProjectPackage.Project.Environments.EnvsValues
	newValues := map[string]interface{}{}
	for k, v := range values {
		key := strings.ReplaceAll(strings.ReplaceAll(k, ".", "_"), "-", "_")
		value := v
		if v == "" {
			value = "NeedToSet" // TODO
		}
		newValues[key] = value
	}

	envTemplate, err := template.New("env").Parse(string(ymlBytes))
	if err != nil {
		return err
	}
	var buf bytes.Buffer
	err = envTemplate.ExecuteTemplate(&buf, "env", newValues)
	if err != nil {
		return err
	}

	env := apistructs.ProjectEnvironment{}
	if err := yaml.Unmarshal(buf.Bytes(), &env); err != nil {
		return err
	}
	t.ProjectPackage.Project.Environments.Envs[envfile] = env

	return nil
}

func (t *PackageZip) readValues() error {
	valuesYml, err := t.openZipReader(packageValuesYmlName)
	if err != nil {
		return err
	}
	defer valuesYml.Close()

	ymlBytes, err := io.ReadAll(valuesYml)
	if err != nil {
		return err
	}

	values := map[string]interface{}{}
	if err := yaml.Unmarshal(ymlBytes, &values); err != nil {
		return err
	}
	t.ProjectPackage.Project.Environments.EnvsValues = values
	return nil
}

func (t *PackageZip) readZipfile(zipfile, filename string) error {
	tmpZip, err := t.openZipReader(zipfile)
	if err != nil {
		return err
	}
	defer tmpZip.Close()

	dirs := path.Dir(filename)
	err = os.MkdirAll(dirs, 0755)
	if err != nil {
		return err
	}

	dstFile, err := os.OpenFile(filename, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, 0755)
	if err != nil {
		return err
	}
	defer dstFile.Close()

	_, err = io.Copy(dstFile, tmpZip)
	if err != nil {
		return err
	}

	return nil
}

func (t *PackageZip) SetMeta() error {
	return nil
}

func (t *PackageZip) GetPackageName() string {
	return t.packageName
}

func (t *PackageZip) GetProjectPackage() *apistructs.ProjectPackage {
	return t.ProjectPackage
}

func (t *PackageZip) GetContext() *PackageContext {
	return t.contex
}

func (t *PackageZip) GetTempDir() string {
	return t.TmpDir
}

func (p *Project) ImportProjectPackage(record *dao.TestFileRecord) {
	extra := record.Extra.ProjectPackageFileExtraInfo
	if extra == nil || extra.ImportRequest == nil {
		logrus.Errorf("%s import func missing request data", projectPackageResource)
		return
	}

	req := extra.ImportRequest
	id := record.ID
	// change record state to processing
	if err := p.updatePackageFileRecord(id, apistructs.FileRecordStateProcessing, "", nil); err != nil {
		return
	}

	// download zip package
	f, err := p.bdl.DownloadDiceFile(record.ApiFileUUID)
	if err != nil {
		p.updatePackageFileRecord(id, apistructs.FileRecordStateFail, "", err)
		return
	}
	defer f.Close()

	buff := bytes.NewBuffer([]byte{})
	size, err := io.Copy(buff, f)
	if err != nil {
		logrus.Errorf("%s failed to read package, err: %v", packageResource, err)
		p.updatePackageFileRecord(id, apistructs.FileRecordStateFail, "", err)
		return
	}
	reader := bytes.NewReader(buff.Bytes())
	zipReader, err := zip.NewReader(reader, size)
	if err != nil {
		logrus.Errorf("%s failed to make zip reader, err: %v", packageResource, err)
		p.updatePackageFileRecord(id, apistructs.FileRecordStateFail, "", err)
		return
	}

	tmpDir, err := ioutil.TempDir(os.TempDir(), "*")
	if err != nil {
		logrus.Error(apierrors.ErrExportProjectPackage.InternalError(err))
		if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail, ErrorInfo: err}); err != nil {
			logrus.Error(apierrors.ErrExportProjectPackage.InternalError(err))
		}
		return
	}
	defer func() {
		if err := os.RemoveAll(tmpDir); err != nil {
			logrus.Errorf("remove tmp dir %s failed", err)
		}
	}()

	packageZip := PackageZip{
		reader:      zipReader,
		bdl:         p.bdl,
		packageName: record.FileName,
		contex: &PackageContext{
			OrgID:        req.OrgID,
			OrgName:      req.OrgName,
			ProjectID:    req.ProjectID,
			ProjectName:  req.ProjectName,
			IdentityInfo: req.IdentityInfo,
		},
		TmpDir: tmpDir,
	}
	packageDirector := PackageDataDirector{}
	packageDirector.New(&packageZip, p.bdl, p.namespace, p.tokenService, p.clusterSvc)
	if err := packageDirector.Construct(); err != nil {
		logrus.Errorf("%s failed to construct package data, err: %v", projectPackageResource, err)
		p.updatePackageFileRecord(id, apistructs.FileRecordStateFail, "", packageDirector.GenErrInfo())
		return
	}

	if err := packageDirector.TryInitProjectByPackage(); err != nil {
		logrus.Errorf("%s failed to create apps by template, err: %v", packageResource, err)
		p.updatePackageFileRecord(id, apistructs.FileRecordStateFail, "", packageDirector.GenErrInfo())
		return
	}

	if errInfo := packageDirector.GenErrInfo(); errInfo != nil {
		p.updatePackageFileRecord(id, apistructs.FileRecordStateFail, packageDirector.GenDesc(), errInfo)
		return
	}

	p.updatePackageFileRecord(id, apistructs.FileRecordStateSuccess, "", packageDirector.GenErrInfo())
}
