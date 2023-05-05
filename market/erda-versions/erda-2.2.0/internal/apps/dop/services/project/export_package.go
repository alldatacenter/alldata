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
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

type PackageDB struct {
	Artifacts   []apistructs.Artifact `json:"artifacts"`
	Package     *apistructs.ProjectPackage
	bdl         *bundle.Bundle
	packageName string
	contex      *PackageContext
	TempDir     string
}

func (t *PackageDB) InitData() {
	t.Package = &apistructs.ProjectPackage{
		MetaData: apistructs.ProjectPackageMeta{Version: projectPackageVersion},
	}
}

func (t *PackageDB) SetProject() error {
	var artifactRelease []apistructs.ReleaseData
	for _, a := range t.Artifacts {
		req := apistructs.ReleaseListRequest{}
		req.Version = a.Version
		req.ProjectID = int64(t.contex.ProjectID)

		atype := strings.ToLower(a.Type)
		isProgject := atype == "project"
		if isProgject {
			req.IsProjectRelease = &isProgject
			if a.Name != t.contex.ProjectName {
				return errors.Errorf("Invalid project name for artifact %v", a)
			}
		} else if atype == "application" {
			r, err := t.bdl.GetAppIDByNames(t.contex.ProjectID, t.contex.UserID, []string{a.Name})
			if err != nil {
				return err
			}
			pId, ok := r.AppNameToID[a.Name]
			if !ok {
				return errors.Errorf("Not found application by name %s", a.Name)
			}

			req.ApplicationID = []string{fmt.Sprintf("%d", pId)}
		} else {
			return errors.Errorf("Invalid type of artifact %s", a.Type)
		}

		resp, err := t.bdl.ListReleases(req)
		if err != nil {
			return err
		}

		logrus.Infof("release %v, %v", resp, resp.Releases)

		var ar apistructs.ReleaseData
		var foundRelease bool
		for _, rc := range resp.Releases {
			if rc.Version == a.Version {
				ar = rc
				foundRelease = true
				logrus.Infof("found release %v", ar)
				break
			}
		}
		if !foundRelease {
			return errors.Errorf("Found not exactly one release for artifact name %s, version %s", a.Name, a.Version)
		}

		artifactRelease = append(artifactRelease, ar)

		aPkg := apistructs.ArtifactPkg{
			Artifact: apistructs.Artifact{
				Name:    a.Name,
				Version: a.Version,
				Type:    a.Type,
			},
			ReleaseId: ar.ReleaseID,
		}
		t.Package.Project.Artifacts = append(t.Package.Project.Artifacts, &aPkg)
	}

	type releaseInfo struct {
		ReleaseID string
		DiceYaml  string
		GitRepo   string
		GitBranch string
		GitCommit string
	}
	appReleaseMap := map[string]releaseInfo{}
	for _, ar := range artifactRelease {
		if ar.IsProjectRelease {
			var appReleaseIDs [][]string
			err := json.Unmarshal([]byte(ar.ApplicationReleaseList), &appReleaseIDs)
			if err != nil {
				return errors.Errorf("failed to Unmarshal appReleaseIDs")
			}

			for i := 0; i < len(appReleaseIDs); i++ {
				for _, appRelaseId := range appReleaseIDs[i] {
					arInPR, err := t.bdl.GetRelease(appRelaseId)
					if err != nil {
						return err
					}

					appReleaseMap[arInPR.ApplicationName] = releaseInfo{
						ReleaseID: appRelaseId,
						DiceYaml:  arInPR.Diceyml,
						GitRepo:   arInPR.Labels["gitRepo"],
						GitBranch: arInPR.Labels["gitBranch"],
						GitCommit: arInPR.Labels["gitCommitId"],
					}

					logrus.Infof("application %s release %s", arInPR.ApplicationName, appRelaseId)
				}
			}
		} else {
			appReleaseMap[ar.ApplicationName] = releaseInfo{
				ReleaseID: ar.ReleaseID,
				DiceYaml:  ar.Diceyml,
				GitRepo:   ar.Labels["gitRepo"],
				GitBranch: ar.Labels["gitBranch"],
				GitCommit: ar.Labels["gitCommitId"],
			}

			logrus.Infof("application %s release %s", ar.ApplicationName, ar.ReleaseID)
		}
	}

	addonList, err := t.bdl.ListAddonByProjectID(int64(t.contex.ProjectID), int64(t.contex.OrgID))
	if err != nil {
		return err
	}

	envAddonInstances := map[apistructs.DiceWorkspace]map[string]apistructs.AddonFetchResponseData{
		apistructs.DevWorkspace:     {},
		apistructs.TestWorkspace:    {},
		apistructs.StagingWorkspace: {},
		apistructs.ProdWorkspace:    {},
	}
	for _, a := range addonList.Data {
		e := apistructs.DiceWorkspace(strings.ToUpper(a.Workspace))
		switch e {
		case apistructs.DevWorkspace,
			apistructs.TestWorkspace,
			apistructs.StagingWorkspace,
			apistructs.ProdWorkspace:
			envAddonInstances[e][a.Name] = a
		}
	}

	environments := map[apistructs.DiceWorkspace]*apistructs.ProjectEnvironment{
		apistructs.DevWorkspace:     {Name: apistructs.DevWorkspace},
		apistructs.TestWorkspace:    {Name: apistructs.TestWorkspace},
		apistructs.StagingWorkspace: {Name: apistructs.StagingWorkspace},
		apistructs.ProdWorkspace:    {Name: apistructs.ProdWorkspace},
	}
	envValues := map[string]interface{}{}

	addonMissConfig := map[string]map[apistructs.DiceWorkspace]map[string]interface{}{}
	envHandledAddons := map[apistructs.DiceWorkspace]map[string]interface{}{
		apistructs.DevWorkspace:     {},
		apistructs.TestWorkspace:    {},
		apistructs.StagingWorkspace: {},
		apistructs.ProdWorkspace:    {},
	}
	for app, release := range appReleaseMap {
		aPkg := apistructs.ApplicationPkg{Name: app, GitBranch: release.GitBranch, GitCommit: release.GitCommit}
		t.Package.Project.Applications = append(t.Package.Project.Applications, &aPkg)

		for env, c := range environments {
			addonHandled := envHandledAddons[env]
			addonMap := envAddonInstances[env]

			dice, err := diceyml.New([]byte(release.DiceYaml), true)
			if err != nil {
				return err
			}
			err = dice.MergeEnv(env.String())
			if err != nil {
				return err
			}
			for name, addon := range dice.Obj().AddOns {
				if _, ok := addonHandled[name]; ok {
					continue
				} else {
					addonHandled[name] = struct{}{}
				}

				var missConfig bool
				var configKeys []string
				if addon.Plan == "alicloud-rds:basic" {
					if addonInstance, ok := addonMap[name]; ok {
						for k := range addonInstance.Config {
							configKeys = append(configKeys, k)
						}
					} else {
						configKeys = []string{
							"MYSQL_HOST",
							"MYSQL_PORT",
							"MYSQL_DATABASE",
							"MYSQL_USERNAME",
							"MYSQL_PASSWORD",
						}
					}
				} else if addon.Plan == "alicloud-redis:basic" {
					if addonInstance, ok := addonMap[name]; ok {
						for k := range addonInstance.Config {
							configKeys = append(configKeys, k)
						}
					} else {
						configKeys = []string{
							"REDIS_HOST",
							"REDIS_PORT",
							"REDIS_PASSWORD",
						}
					}
				} else if addon.Plan == "alicloud-ons:basic" {
					if addonInstance, ok := addonMap[name]; ok {
						for k := range addonInstance.Config {
							configKeys = append(configKeys, k)
						}
					} else {
						configKeys = []string{
							"ONS_ACCESSKEY",
							"ONS_SECRETKEY",
							"ONS_NAMESERVER",
							"ONS_PRODUCERID",
							"ONS_TOPIC",
						}
					}
				} else if addon.Plan == "alicloud-oss:basic" {
					if addonInstance, ok := addonMap[name]; ok {
						for k := range addonInstance.Config {
							configKeys = append(configKeys, k)
						}
					} else {
						configKeys = []string{
							"OSS_ACCESS_KEY_ID",
							"OSS_ACCESS_KEY_SECRET",
							"OSS_BUCKET",
							"OSS_ENDPOINT",
							"OSS_HOST",
							"OSS_PROVIDER",
							"OSS_REGION",
							"OSS_STORE_DIR",
						}
					}
				} else if addon.Plan == "alicloud-gateway:basic" {
					if addonInstance, ok := addonMap[name]; ok {
						for k := range addonInstance.Config {
							configKeys = append(configKeys, k)
						}
					} else {
						configKeys = []string{
							"ALIYUN_GATEWAY_INSTANCE_ID",
							"ALIYUN_GATEWAY_VPC_GRANT",
						}
					}
				} else if addon.Plan == "custom:basic" {
					if addonInstance, ok := addonMap[name]; ok {
						for k := range addonInstance.Config {
							configKeys = append(configKeys, k)
						}
					} else {
						missConfig = true
					}
				} else {
					continue
				}

				configMap := map[string]interface{}{}
				if missConfig {
					if miss, ok := addonMissConfig[name]; ok {
						miss[env] = configMap
					} else {
						addonMissConfig[name] = map[apistructs.DiceWorkspace]map[string]interface{}{
							env: configMap,
						}
					}
				} else {
					for _, k := range configKeys {
						addonValue := fmt.Sprintf("values.%s.addons.%s.config.%s", env, name, k)
						encodeKey := strings.ReplaceAll(strings.ReplaceAll(addonValue, ".", "_"), "-", "_")
						configMap[k] = fmt.Sprintf("{{ index .%s }}", encodeKey)
						envValues[addonValue] = ""
					}
				}
				dps := strings.Split(addon.Plan, ":")
				if len(dps) != 2 {
					return errors.Errorf("Invalid plan %s for addon %s", addon.Plan, name)
				}
				a := apistructs.ProjectEnvAddon{
					Name:    name,
					Options: addon.Options,
					Type:    dps[0],
					Plan:    dps[1],
					Config:  configMap,
				}

				c.Addons = append(c.Addons, a)
			}

			// cpu & memory
			var sumCpuQuota float64
			var sumMemoryQuota int
			for _, service := range dice.Obj().Services {
				sumCpuQuota += service.Resources.MaxCPU
				sumMemoryQuota += service.Resources.MaxMem
			}
			clusterNameKey := fmt.Sprintf("values.%s.cluster.name", env)
			encodeKey := strings.ReplaceAll(strings.ReplaceAll(clusterNameKey, ".", "_"), "-", "_")
			c.Cluster.Name = fmt.Sprintf("{{ index .%s }}", encodeKey)
			envValues[clusterNameKey] = ""

			cpuQuotaKey := fmt.Sprintf("values.%s.cluster.quota.cpuQuota", env)
			memoryQuotaKey := fmt.Sprintf("values.%s.cluster.quota.memoryQuota", env)
			encodeCpuQuotaKey := strings.ReplaceAll(strings.ReplaceAll(cpuQuotaKey, ".", "_"), "-", "_")
			encodeMemQuotaKey := strings.ReplaceAll(strings.ReplaceAll(memoryQuotaKey, ".", "_"), "-", "_")
			c.Cluster.Quota = apistructs.ClusterQuota{

				CpuQuota:    fmt.Sprintf("{{ index .%s }}", encodeCpuQuotaKey),
				MemoryQuota: fmt.Sprintf("{{ index .%s }}", encodeMemQuotaKey),
			}
			envValues[cpuQuotaKey] = sumCpuQuota
			envValues[memoryQuotaKey] = sumMemoryQuota
		}
	}

	// from prod to dev
	for _, env := range []apistructs.DiceWorkspace{
		apistructs.ProdWorkspace, apistructs.StagingWorkspace,
		apistructs.TestWorkspace, apistructs.DevWorkspace} {
		for _, addon := range environments[env].Addons {
			if len(addon.Config) == 0 {
				continue
			}

			if list, ok := addonMissConfig[addon.Name]; ok {
				for addonEnv, configRef := range list {
					for k := range addon.Config {
						addonValue := fmt.Sprintf("values.%s.addons.%s.config.%s", addonEnv, addon.Name, k)
						encodeKey := strings.ReplaceAll(strings.ReplaceAll(addonValue, ".", "_"), "-", "_")
						configRef[k] = fmt.Sprintf("{{ index .%s }}", encodeKey)
						envValues[addonValue] = ""
					}
				}
				// set to empty list
				addonMissConfig[addon.Name] = map[apistructs.DiceWorkspace]map[string]interface{}{}
			}
		}
	}
	// clear no config addons
	for _, env := range []apistructs.DiceWorkspace{
		apistructs.ProdWorkspace, apistructs.StagingWorkspace,
		apistructs.TestWorkspace, apistructs.DevWorkspace} {
		var envAddon []apistructs.ProjectEnvAddon
		for _, addon := range environments[env].Addons {
			if len(addon.Config) == 0 {
				logrus.Warnf("no config found for addon %s in env %s", addon.Name, env.String())
				continue
			}
			envAddon = append(envAddon, addon)
		}
		environments[env].Addons = envAddon
	}

	envs := map[string]apistructs.ProjectEnvironment{}
	var includes []string
	for _, e := range environments {
		includefile := fmt.Sprintf("environments/%s-env.yml", e.Name)
		includes = append(includes, includefile)
		envs[includefile] = *e
	}

	t.Package.Project.Environments.Include = includes
	t.Package.Project.Environments.Envs = envs
	t.Package.Project.Environments.EnvsValues = envValues

	return nil
}

func (t *PackageDB) SetMeta() error {
	user, err := t.bdl.GetCurrentUser(t.contex.UserID)
	if err != nil {
		return err
	}
	t.Package.MetaData.Creator = user.Nick

	t.Package.MetaData.Source = apistructs.SourceMeta{
		Organization: t.contex.OrgName,
		Project:      t.contex.ProjectName,
	}

	var publicURL = conf.UIPublicURL()
	if publicURL != "" {
		t.Package.MetaData.Source.Url = fmt.Sprintf("%s/%s/dop/projects/%d", publicURL, t.contex.OrgName, t.contex.ProjectID)
	}

	t.Package.MetaData.Type = "project"
	t.Package.MetaData.CreatedAt = time.Now().Format("2006-01-02 15:04:05")
	t.Package.MetaData.Description = fmt.Sprintf("Project %s package exported.", t.contex.ProjectName)

	return nil
}

func (t *PackageDB) GetProjectPackage() *apistructs.ProjectPackage {
	return t.Package
}

func (t *PackageDB) GetPackageName() string {
	return t.packageName
}

func (t *PackageDB) GetContext() *PackageContext {
	return t.contex
}

func (t *PackageDB) GetTempDir() string {
	return t.TempDir
}

func (p *Project) ExportProjectPackage(record *dao.TestFileRecord) {
	extra := record.Extra.ProjectPackageFileExtraInfo
	logrus.Infof("record extra %v", record)
	if extra == nil || extra.ExportRequest == nil {
		logrus.Errorf("project Package export missing request data")
		return
	}

	req := extra.ExportRequest
	id := record.ID
	if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateProcessing}); err != nil {
		logrus.Error(apierrors.ErrExportProjectPackage.InternalError(err))
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

	packageDB := PackageDB{
		contex: &PackageContext{
			OrgID:        req.OrgID,
			OrgName:      req.OrgName,
			ProjectID:    req.ProjectID,
			ProjectName:  req.ProjectName,
			IdentityInfo: req.IdentityInfo,
		},
		Artifacts:   req.Artifacts,
		bdl:         p.bdl,
		packageName: record.FileName,
		TempDir:     tmpDir,
	}
	packageDirector := PackageDataDirector{}
	packageDirector.New(&packageDB, p.bdl, p.namespace, p.tokenService, p.clusterSvc)
	if err := packageDirector.Construct(); err != nil {
		logrus.Error(apierrors.ErrExportProjectPackage.InternalError(err))
		if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail, ErrorInfo: packageDirector.GenErrInfo()}); err != nil {
			logrus.Error(apierrors.ErrExportProjectPackage.InternalError(err))
		}
		return
	}

	uuid, err := packageDirector.GenAndUploadZipPackage()
	if err != nil {
		logrus.Error(apierrors.ErrExportProjectPackage.InternalError(err))
		packageDirector.errs = append(packageDirector.errs, err)
		if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateFail, ErrorInfo: packageDirector.GenErrInfo()}); err != nil {
			logrus.Error(apierrors.ErrExportProjectPackage.InternalError(err))
		}
		return
	}

	if err := p.UpdateFileRecord(apistructs.TestFileRecordRequest{ID: id, State: apistructs.FileRecordStateSuccess, ApiFileUUID: uuid}); err != nil {
		logrus.Error(apierrors.ErrExportProjectPackage.InternalError(err))
		return
	}
}
