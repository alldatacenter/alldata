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
	"context"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"net/url"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"google.golang.org/grpc/metadata"

	"github.com/mholt/archiver"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/namespace"
	"github.com/erda-project/erda/pkg/filehelper"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
)

const (
	packageProjectTempPrefix      = "exportPackage.*"
	packageValuesYmlName          = "values.yml"
	packageProjectYmlName         = "project.yml"
	packageProjectMetadataYmlName = "metadata.yml"
	projectPackageResource        = "project-package"
	projectPackageSuffix          = ".zip"
	projectPackageValidityPeriod  = 7 * time.Hour * 24 * 365 * 10 // 10 year
	projectPackageVersion         = "1.0.0"
)

type PackageDataCreator interface {
	GetPackageName() string
	InitData()
	SetProject() error
	SetMeta() error
	GetProjectPackage() *apistructs.ProjectPackage
	GetTempDir() string
	GetContext() *PackageContext
}

type PackageContext struct {
	ProjectID   uint64 `json:"projectID"`
	ProjectName string `json:"projectName"`
	OrgID       uint64 `json:"orgID"`
	OrgName     string `json:"orgName"`
	apistructs.IdentityInfo
}

type PackageDataDirector struct {
	Creator      PackageDataCreator
	bdl          *bundle.Bundle
	namespace    *namespace.Namespace
	errs         []error
	tokenService tokenpb.TokenServiceServer
	clusterSvc   clusterpb.ClusterServiceServer
}

func (t *PackageDataDirector) New(creator PackageDataCreator, bdl *bundle.Bundle, namespace *namespace.Namespace, tokenSvc tokenpb.TokenServiceServer, clusterSvc clusterpb.ClusterServiceServer) {
	t.Creator = creator
	t.bdl = bdl
	t.namespace = namespace
	t.errs = make([]error, 0)
	t.tokenService = tokenSvc
	t.clusterSvc = clusterSvc
}

func (t *PackageDataDirector) Construct() error {
	t.Creator.InitData()
	if err := t.Creator.SetProject(); err != nil {
		t.errs = append(t.errs, err)
		return err
	}
	if err := t.Creator.SetMeta(); err != nil {
		t.errs = append(t.errs, err)
		return err
	}

	return nil
}

func (t *PackageDataDirector) CheckPackage() error {
	packageData := t.Creator.GetProjectPackage()

	appSet := make(map[string]struct{})
	duplicatedApp := make([]string, 0)
	for _, app := range packageData.Project.Applications {
		if app.Name == "" {
			return errors.New("project package contains application without name")
		}
		if _, ok := appSet[app.Name]; ok {
			duplicatedApp = append(duplicatedApp, app.Name)
		} else {
			appSet[app.Name] = struct{}{}
		}
	}
	if len(duplicatedApp) > 0 {
		return fmt.Errorf("project package contains duplicated application: %s", strings.Join(duplicatedApp, ","))
	}
	// check artifacts
	artifactSet := make(map[string]struct{})
	duplicatedArtficat := make([]string, 0)
	for _, artifact := range packageData.Project.Artifacts {
		if artifact.Name == "" || artifact.Version == "" || artifact.Type == "" {
			return fmt.Errorf("project package contains artifact without name, version or type")
		}

		artifactId := fmt.Sprintf("%s@%s", artifact.Name, artifact.Version)
		if _, ok := artifactSet[artifactId]; ok {
			duplicatedArtficat = append(duplicatedArtficat, artifactId)
		} else {
			artifactSet[artifactId] = struct{}{}
		}
	}
	if len(duplicatedArtficat) > 0 {
		return fmt.Errorf("project package contains duplicated artifact: %s", strings.Join(duplicatedArtficat, ","))
	}
	return nil
}

func (t *PackageDataDirector) GenAndUploadZipPackage() (string, error) {
	packageContext := t.Creator.GetContext()
	projectPackage := t.Creator.GetProjectPackage()
	tempDir := t.Creator.GetTempDir()

	zipTmpFile, err := ioutil.TempFile("", packageProjectTempPrefix)
	if err != nil {
		return "", err
	}
	defer func() {
		if err := os.Remove(zipTmpFile.Name()); err != nil {
			logrus.Error(apierrors.ErrExportProjectTemplate.InternalError(err))
		}
	}()
	defer zipTmpFile.Close()

	artifactsDir := filepath.Join(tempDir, "artifacts")
	err = os.MkdirAll(artifactsDir, 0755)
	if err != nil {
		return "", err
	}
	for _, artifact := range projectPackage.Project.Artifacts {
		zipfile, err := t.bdl.DownloadRelease(packageContext.OrgID, packageContext.UserID,
			artifact.ReleaseId, artifactsDir)
		if err != nil {
			return "", err
		}

		artifact.ZipFile = filepath.Join("artifacts", zipfile)
		logrus.Infof("download artifact zip %s", zipfile)
	}

	repoDir := filepath.Join(tempDir, "repos")
	err = os.MkdirAll(repoDir, 0755)
	if err != nil {
		return "", err
	}
	for _, app := range projectPackage.Project.Applications {
		req := apistructs.GittarArchiveRequest{
			packageContext.OrgName,
			packageContext.ProjectName,
			app.Name,
			app.GitBranch,
		}
		zipfile, err := t.bdl.GetArchive(packageContext.UserID, req, repoDir)
		if err != nil {
			if err == bundle.RepoNotExist {
				logrus.Warnf("application %s has no gittar repo for %s", app.Name, app.GitBranch)
				continue
			}
			return "", err
		}

		app.ZipRepo = filepath.Join("repos", zipfile)
		logrus.Infof("download repo zip %s", zipfile)
	}

	// write project.yml
	projectYmlPath := filepath.Join(tempDir, packageProjectYmlName)
	ymlBytes, err := yaml.Marshal(projectPackage.Project)
	if err != nil {
		return "", err
	}
	if err := filehelper.CreateFile(projectYmlPath, string(ymlBytes), 0644); err != nil {
		return "", err
	}
	// write *-env.yml
	envDir := filepath.Join(tempDir, "environments")
	err = os.MkdirAll(envDir, 0755)
	for _, include := range projectPackage.Project.Environments.Include {
		if !strings.HasPrefix(include, "environments/") {
			return "", errors.Errorf("include env file '%s' has no environments/ prefix", include)
		}
		incldeYmlPath := filepath.Join(t.Creator.GetTempDir(), include)
		ymlBytes, err := yaml.Marshal(projectPackage.Project.Environments.Envs[include])
		if err != nil {
			return "", err
		}
		if err := filehelper.CreateFile(incldeYmlPath, string(ymlBytes), 0644); err != nil {
			return "", err
		}
	}
	// write values.yml
	valuesYmlPath := filepath.Join(tempDir, packageValuesYmlName)
	ymlBytes, err = yaml.Marshal(projectPackage.Project.Environments.EnvsValues)
	if err != nil {
		return "", err
	}
	if err := filehelper.CreateFile(valuesYmlPath, string(ymlBytes), 0644); err != nil {
		return "", err
	}
	// write metadata.yml
	metadataYmlPath := filepath.Join(tempDir, packageProjectMetadataYmlName)
	metaBytes, _ := yaml.Marshal(projectPackage.MetaData)
	if err := filehelper.CreateFile(metadataYmlPath, string(metaBytes), 0644); err != nil {
		return "", err
	}

	zipList := []string{projectYmlPath, metadataYmlPath, valuesYmlPath, artifactsDir, repoDir, envDir}
	if err := archiver.Zip.Write(zipTmpFile, zipList); err != nil {
		return "", err
	}
	zipTmpFile.Seek(0, 0)
	expiredAt := time.Now().Add(packageValidityPeriod)
	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: t.Creator.GetPackageName(),
		FileReader:      zipTmpFile,
		From:            projectPackageResource,
		IsPublic:        true,
		ExpiredAt:       &expiredAt,
	}
	file, err := t.bdl.UploadFile(uploadReq)
	if err != nil {
		return "", err
	}
	return file.UUID, nil
}

func unzip(src, dest string) error {
	r, err := zip.OpenReader(src)
	if err != nil {
		return err
	}
	defer func() {
		if err := r.Close(); err != nil {
			panic(err)
		}
	}()

	err = os.MkdirAll(dest, 0755)
	if err != nil {
		return err
	}

	// Closure to address file descriptors issue with all the deferred .Close() methods
	extractAndWriteFile := func(f *zip.File) error {
		rc, err := f.Open()
		if err != nil {
			return err
		}
		defer func() {
			if err := rc.Close(); err != nil {
				panic(err)
			}
		}()

		path := filepath.Join(dest, f.Name)

		// Check for ZipSlip (Directory traversal)
		if !strings.HasPrefix(path, filepath.Clean(dest)+string(os.PathSeparator)) {
			return fmt.Errorf("illegal file path: %s", path)
		}

		if f.FileInfo().IsDir() {
			err = os.MkdirAll(path, f.Mode())
			if err != nil {
				return err
			}
		} else {
			err = os.MkdirAll(filepath.Dir(path), f.Mode())
			if err != nil {
				return err
			}
			f, err := os.OpenFile(path, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, f.Mode())
			if err != nil {
				return err
			}
			defer func() {
				if err := f.Close(); err != nil {
					panic(err)
				}
			}()

			_, err = io.Copy(f, rc)
			if err != nil {
				return err
			}
		}
		return nil
	}

	for _, f := range r.File {
		err := extractAndWriteFile(f)
		if err != nil {
			return err
		}
	}

	return nil
}

func initApplicationRepo(repoDir, gittar string, application *apistructs.ApplicationDTO) error {
	initCmd := exec.Command("git", "init")
	initCmd.Dir = repoDir
	out, err := initCmd.CombinedOutput()
	if err != nil {
		return err
	}
	logrus.Infof("application %s repo, git init: %s", application.Name, out)

	emailCmd := exec.Command("git", "config", "user.email", "git@erda.cloud")
	emailCmd.Dir = repoDir
	out, err = emailCmd.CombinedOutput()
	if err != nil {
		return err
	}
	logrus.Infof("application %s repo, git init email: %s", application.Name, out)

	userCmd := exec.Command("git", "config", "user.name", "git")
	userCmd.Dir = repoDir
	out, err = userCmd.CombinedOutput()
	if err != nil {
		return err
	}
	logrus.Infof("application %s repo, git init user: %s", application.Name, out)

	addCmd := exec.Command("git", "add", ".")
	addCmd.Dir = repoDir
	out, err = addCmd.CombinedOutput()
	if err != nil {
		return err
	}
	logrus.Infof("application %s repo, git add: %s", application.Name, out)

	firstComment := fmt.Sprintf("Init application %s repo", application.Name)
	firstCommitCmd := exec.Command("git", "commit", "-m", firstComment)
	firstCommitCmd.Dir = repoDir
	out, err = firstCommitCmd.CombinedOutput()
	if err != nil {
		return err
	}
	logrus.Infof("application %s repo, git commit: %s", application.Name, out)

	repoSplits := strings.SplitN(application.GitRepoNew, "/", 2)
	if len(repoSplits) != 2 {
		return errors.Errorf("invalid repo %s", application.GitRepoNew)
	}

	remoteUrl := fmt.Sprintf("%s/%s", gittar, repoSplits[1])
	logrus.Infof("remote url %s", remoteUrl)

	addRemoteCmd := exec.Command("git", "remote", "add", "origin", remoteUrl)
	addRemoteCmd.Dir = repoDir
	out, err = addRemoteCmd.CombinedOutput()
	if err != nil {
		return err
	}
	logrus.Infof("application %s repo, git remote add: %s", application.Name, out)

	pushCmd := exec.Command("git", "push", "--set-upstream", "origin", "master")
	pushCmd.Dir = repoDir
	out, err = pushCmd.CombinedOutput()
	if err != nil {
		return err
	}

	return nil
}

func (t *PackageDataDirector) TryInitProjectByPackage() error {
	packageData := t.Creator.GetProjectPackage()
	packageContext := t.Creator.GetContext()
	tempDir := t.Creator.GetTempDir()

	project, err := t.bdl.GetProject(packageContext.ProjectID)
	if err != nil {
		return err
	}

	gittarRemoteUrl, err := t.makeGittarRemoteUrl()
	if err != nil {
		return err
	}

	for _, appInfo := range packageData.Project.Applications {
		appReq := apistructs.ApplicationCreateRequest{
			Name:           appInfo.Name,
			ProjectID:      packageContext.ProjectID,
			Mode:           apistructs.ApplicationModeService, // as default
			IsExternalRepo: false,
		}
		newApp, err := t.bdl.CreateAppWithRepo(appReq, packageContext.UserID)
		if err != nil {
			t.errs = append(t.errs, fmt.Errorf("create application %s failed: %v", appInfo.Name, err))
			logrus.Errorf("%s failed to create app: %s, err: %v", projectPackageResource, appInfo.Name, err)
			continue
		}
		t.namespace.GenerateAppExtraInfo(int64(newApp.ID), int64(newApp.ProjectID))

		if appInfo.ZipRepo == "" {
			logrus.Warnf("no repo for application %s", appInfo.Name)
			continue
		}

		distDir := path.Join(tempDir, appInfo.Name)
		err = unzip(appInfo.ZipRepo, distDir)
		if err != nil {
			t.errs = append(t.errs, fmt.Errorf("unzip application %s repo failed: %v", appInfo.Name, err))
			logrus.Errorf("%s failed to unzip app: %s repo, err: %v", projectPackageResource, appInfo.Name, err)
			continue
		}
		if err := initApplicationRepo(distDir, gittarRemoteUrl, newApp); err != nil {
			t.errs = append(t.errs, fmt.Errorf("init application %s repo failed: %v", appInfo.Name, err))
			logrus.Errorf("%s failed to init app: %s repo, err: %v", projectPackageResource, appInfo.Name, err)
			continue
		}
	}

	projectResourceConfig := apistructs.NewResourceConfigs()
	for _, env := range packageData.Project.Environments.Envs {
		if env.Name == "" {
			continue
		}

		for _, addon := range env.Addons {
			var request apistructs.CustomAddonCreateRequest
			request.AddonName = addon.Type
			request.Name = addon.Name
			request.ProjectID = packageContext.ProjectID
			request.Workspace = strings.ToUpper(env.Name.String())
			request.Configs = addon.Config
			request.CustomAddonType = "custom"

			_, err := t.bdl.CreateCustomAddon(packageContext.UserID, strconv.FormatUint(packageContext.OrgID, 10), request)
			if err != nil {
				t.errs = append(t.errs, fmt.Errorf("create addon %s failed: %v", addon.Name, err))
				logrus.Errorf("%s failed to create addon: %s, err: %v", projectPackageResource, addon.Name, err)
				continue
			}
		}

		if env.Cluster.Name == "" {
			t.errs = append(t.errs, fmt.Errorf("cluster name not set for env %s", env.Name))
			logrus.Errorf("%s cluster name not set for env %s", projectPackageResource, env.Name)
			continue
		}

		ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "cmp"}))
		resp, err := t.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: env.Cluster.Name})
		if err != nil {
			t.errs = append(t.errs, fmt.Errorf("get cluster %s failed: %v", env.Cluster.Name, err))
			logrus.Errorf("%s failed to set cluster: %s, err: %v", projectPackageResource, env.Cluster.Name, err)
			continue
		}
		cluster := resp.Data
		rc := projectResourceConfig.GetWSConfig(env.Name)
		if rc != nil {
			rc.ClusterName = cluster.Name

			cQ, err := strconv.ParseFloat(env.Cluster.Quota.CpuQuota, 64)
			if err != nil {
				t.errs = append(t.errs, fmt.Errorf("env %s, parse cup quota %s failed: %v", env.Name, env.Cluster.Quota.CpuQuota, err))
				logrus.Errorf("%s failed to parse env %s cup quota %s failed, err: %v", projectPackageResource, env.Name, env.Cluster.Quota.CpuQuota, err)
				continue
			}
			rc.CPUQuota = cQ
			mQ, err := strconv.ParseFloat(env.Cluster.Quota.MemoryQuota, 64)
			if err != nil {
				t.errs = append(t.errs, fmt.Errorf("env %s, parse memory quota %s failed: %v", env.Name, env.Cluster.Quota.MemoryQuota, err))
				logrus.Errorf("%s failed to parse env %s memory quota %s failed, err: %v", projectPackageResource, env.Name, env.Cluster.Quota.MemoryQuota, err)
				continue
			}
			rc.MemQuota = mQ
		}
	}

	pUpateReq := apistructs.ProjectUpdateRequest{
		ProjectID: packageContext.ProjectID,
		Body: apistructs.ProjectUpdateBody{
			Name:            project.Name,
			ResourceConfigs: projectResourceConfig,
		}}
	err = t.bdl.UpdateProject(pUpateReq, packageContext.OrgID, packageContext.UserID)
	if err != nil {
		t.errs = append(t.errs, fmt.Errorf("update project quota failed: %v", err))
		logrus.Errorf("%s failed to update project quota, err: %v", projectPackageResource, err)
	}

	for _, artifact := range packageData.Project.Artifacts {
		uploadfile, err := t.uploadArtifacZipFile(artifact)
		if err != nil {
			t.errs = append(t.errs, fmt.Errorf("upload artifact %s failed: %v", artifact.ZipFile, err))
			logrus.Errorf("%s failed to upload artifact %s, err: %v", projectPackageResource, artifact.ZipFile, err)
			continue
		}
		req := apistructs.ReleaseUploadRequest{
			DiceFileID:  uploadfile,
			ProjectID:   int64(packageContext.ProjectID),
			ProjectName: packageContext.ProjectName,
			OrgID:       int64(packageContext.OrgID),
			UserID:      packageContext.UserID,
		}
		err = t.bdl.UploadRelease(req)
		if err != nil {
			t.errs = append(t.errs, fmt.Errorf("upload release %s failed: %v", uploadfile, err))
			logrus.Errorf("%s failed to upload release, err: %v", projectPackageResource, err)
			continue
		}
	}

	return nil
}

func (t *PackageDataDirector) makeGittarRemoteUrl() (string, error) {
	gittar, err := t.bdl.GetGittarHost()
	if err != nil {
		return "", err
	}

	if !strings.HasPrefix(gittar, "http://") && !strings.HasPrefix(gittar, "https://") {
		gittar = fmt.Sprintf("http://%s", gittar)
	}
	logrus.Infof("gittar url %s", gittar)
	// query creator PAT when we create repo in app.
	res, err := t.tokenService.QueryTokens(context.Background(), &tokenpb.QueryTokensRequest{
		Scope:     string(apistructs.OrgScope),
		ScopeId:   strconv.FormatUint(t.Creator.GetContext().OrgID, 10),
		Type:      mysqltokenstore.PAT.String(),
		CreatorId: t.Creator.GetContext().UserID,
	})
	if err != nil {
		return "", err
	}
	if res.Total == 0 {
		return "", errors.New("the member is not exist")
	}

	token := res.Data[0].AccessKey
	u := url.UserPassword("git", token)
	gittarUrl, err := url.Parse(gittar)
	if err != nil {
		return "", err
	}
	gittarUrl.User = u

	remoteUrl := gittarUrl.String()
	logrus.Infof("gittar remote url %s", remoteUrl)

	return remoteUrl, nil
}

func (t *PackageDataDirector) uploadArtifacZipFile(artifact *apistructs.ArtifactPkg) (string, error) {
	f, err := os.Open(artifact.ZipFile)
	if err != nil {
		return "", err
	}
	defer f.Close()

	expiredAt := time.Now().Add(projectPackageValidityPeriod)
	req := apistructs.FileUploadRequest{
		FileReader:      f,
		FileNameWithExt: artifact.ZipFile,
		Creator:         t.Creator.GetContext().UserID,
		From:            projectPackageResource,
		IsPublic:        true,
		ExpiredAt:       &expiredAt,
	}
	uploadfile, err := t.bdl.UploadFile(req)
	if err != nil {
		return "", err
	}

	return uploadfile.UUID, nil
}

func (t *PackageDataDirector) GenErrInfo() error {
	if len(t.errs) > 0 {
		errInfos := make([]string, 0, len(t.errs))
		for _, err := range t.errs {
			errInfos = append(errInfos, fmt.Sprintf("%v", err))
		}
		return fmt.Errorf("%s", strings.Join(errInfos, "\n"))
	}
	return nil
}

func (t *PackageDataDirector) GenDesc() string {
	return fmt.Sprintf("Imported project succeed")
}

func (p *Project) ParsePackage(r io.ReadCloser) (*apistructs.ProjectPackage, error) {
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
	packageZip := PackageZip{reader: zipReader}
	packageDirector := PackageDataDirector{}
	packageDirector.New(&packageZip, p.bdl, p.namespace, p.tokenService, p.clusterSvc)
	if err := packageDirector.Construct(); err != nil {
		return nil, err
	}
	if err := packageDirector.CheckPackage(); err != nil {
		return nil, err
	}
	return packageDirector.Creator.GetProjectPackage(), nil
}

func (p *Project) ExportPackage(req apistructs.ExportProjectPackageRequest) (uint64, error) {
	packageName := fmt.Sprintf("%s.zip", p.MakePackageName(req.ProjectName))
	fileReq := apistructs.TestFileRecordRequest{
		FileName:     packageName,
		Description:  fmt.Sprintf("export project: %s package", req.ProjectDisplayName),
		OrgID:        uint64(req.OrgID),
		Type:         apistructs.FileProjectPackageExport,
		State:        apistructs.FileRecordStatePending,
		ProjectID:    req.ProjectID,
		IdentityInfo: req.IdentityInfo,
		Extra: apistructs.TestFileExtra{
			ProjectPackageFileExtraInfo: &apistructs.ProjectPackageFileExtraInfo{
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

func (p *Project) ImportPackage(req apistructs.ImportProjectPackageRequest, r *http.Request) (uint64, error) {
	f, fileHeader, err := r.FormFile("file")
	if err != nil {
		return 0, err
	}
	defer f.Close()

	if !strings.HasSuffix(fileHeader.Filename, projectPackageSuffix) {
		return 0, fmt.Errorf("project template file must be a zip package")
	}
	expiredAt := time.Now().Add(projectPackageValidityPeriod)
	uploadReq := apistructs.FileUploadRequest{
		FileNameWithExt: fileHeader.Filename,
		FileReader:      f,
		From:            projectPackageResource,
		IsPublic:        true,
		ExpiredAt:       &expiredAt,
	}
	uploadRecord, err := p.bdl.UploadFile(uploadReq)
	if err != nil {
		return 0, err
	}

	fileReq := apistructs.TestFileRecordRequest{
		FileName:     fileHeader.Filename,
		Description:  fmt.Sprintf("import project: %s package", req.ProjectDisplayName),
		OrgID:        uint64(req.OrgID),
		ProjectID:    req.ProjectID,
		Type:         apistructs.FileProjectPackageImport,
		State:        apistructs.FileRecordStatePending,
		IdentityInfo: req.IdentityInfo,
		ApiFileUUID:  uploadRecord.UUID,
		Extra: apistructs.TestFileExtra{
			ProjectPackageFileExtraInfo: &apistructs.ProjectPackageFileExtraInfo{
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

func (p *Project) MakePackageName(origin string) string {
	return fmt.Sprintf("%s_%s", origin, time.Now().Format("20060102150405"))
}

func (p *Project) updatePackageFileRecord(id uint64, state apistructs.FileRecordState, desc string, err error) error {
	if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: state, Description: desc, ErrorInfo: err}); err != nil {
		logrus.Errorf("%s failed to update file record, err: %v", projectPackageResource, err)
		return err
	}
	return nil
}
