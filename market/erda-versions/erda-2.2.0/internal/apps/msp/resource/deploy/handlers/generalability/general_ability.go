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

package generalability

import (
	"context"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/ghodss/yaml"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const (
	ProjectDisplayName    = "general_ability"
	ProjectDescFormat     = "%s集群能力项目"
	ApplicationNameSuffix = "_ability"
	AbilityPrefix         = "ABILITY_"
	AbilityHostSuffix     = "_HOST"
	AbilityPortSuffix     = "_PORT"

	AbilityRuntimeId = "abilityRuntimeId"

	LifeHost = "lifeHost"
	LifePort = "lifePort"
)

type GeneralAbilityDeployHandler struct {
	*handlers.DefaultDeployHandler
}

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.ServiceType == "GENERAL_ABILITY"
}

func (p *provider) BuildServiceGroupRequest(resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{} {
	// general-ability addon will call orchestrator to do real deploy
	return nil
}

func (p *provider) DoDeploy(serviceGroupDeployRequest interface{}, resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) (
	interface{}, error) {

	runtimeId, err := p.doRuntimeCreateRequest(tmcInstance, resourceInfo)
	if err != nil {
		return nil, err
	}

	runtimeStatus, err := p.waitRuntimeReady(tmcInstance, runtimeId)
	if err != nil {
		return nil, err
	}

	return runtimeStatus, nil
}

func (p *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{}, clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {
	runtimeStatus := serviceGroupDeployResult.(*bundle.GetRuntimeServicesResponseData)

	config := map[string]string{}
	for name, service := range runtimeStatus.Services {
		// todo lifeHost support

		addrs := service.Addrs
		if len(addrs) == 0 {
			continue
		}

		addr := addrs[0]
		idx := strings.LastIndex(addr, ":")
		keyPrefix := AbilityPrefix + tmcInstance.Engine + "_" + name
		hostKey := strings.ToUpper(strings.ReplaceAll(keyPrefix+AbilityHostSuffix, "-", "_"))
		portKey := strings.ToUpper(strings.ReplaceAll(keyPrefix+AbilityPortSuffix, "-", "_"))
		if idx == -1 {
			config[hostKey] = addr
		} else {
			config[hostKey] = addr[0:idx]
			config[portKey] = addr[idx+1:]
		}
	}

	return config
}

func (p *provider) DoApplyTmcInstanceTenant(req *handlers.ResourceDeployRequest, resourceInfo *handlers.ResourceInfo,
	tmcInstance *db.Instance, tenant *db.InstanceTenant, clusterConfig map[string]string) (map[string]string, error) {
	// todo add lifeHost support
	return map[string]string{}, nil
}

func (p *provider) doRuntimeCreateRequest(tmcInstance *db.Instance, resourceInfo *handlers.ResourceInfo) (uint64, error) {
	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	orgIdValue := instanceOptions["orgId"]
	if len(orgIdValue) == 0 {
		return 0, fmt.Errorf("orgId is null")
	}
	orgId, err := strconv.ParseUint(orgIdValue, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("orgId[%s] can not convert to number", orgIdValue)
	}

	az := tmcInstance.Az
	userId := instanceOptions["User-ID"]
	workspace := instanceOptions["workspace"]
	var projectId uint64
	if resourceInfo.Tmc.DeployMode == handlers.DeployModePAAS {
		projectIdValue := instanceOptions["projectId"]
		if len(projectIdValue) == 0 {
			return 0, fmt.Errorf("projectId is null")
		}
		projectId, _ = strconv.ParseUint(projectIdValue, 10, 64)
	} else {
		projectId, err = p.getProjectId(orgId, az)
		if err != nil {
			return 0, err
		}
	}

	applicationId, err := p.getApplicationId(orgId, projectId, tmcInstance.Engine, userId)
	if err != nil {
		return 0, err
	}

	releaseId := tmcInstance.ReleaseID
	if len(releaseId) == 0 {
		releaseId, err = p.createRelease(orgId, resourceInfo.Dice, tmcInstance.Engine, tmcInstance.Version)
		if err != nil {
			return 0, err
		}
		err = p.TmcVersionDb.UpdateReleaseId(tmcInstance.Engine, tmcInstance.Version, releaseId)
		if err != nil {
			return 0, err
		}
	}

	runtimeId, err := p.getRuntimeId(resourceInfo.Tmc.DeployMode, orgId, projectId, applicationId, workspace, tmcInstance.Version, releaseId, az)
	if err != nil {
		return 0, err
	}

	instanceOptions[AbilityRuntimeId] = strconv.FormatUint(runtimeId, 10)
	// todo support lifeHost config

	optionsStr, _ := utils.JsonConvertObjToString(instanceOptions)
	p.InstanceDb.Model(tmcInstance).Update("options", optionsStr)

	return runtimeId, nil
}

func (p *provider) getProjectId(orgId uint64, az string) (uint64, error) {
	userID := p.GetDiceOperatorId()
	name := az + "_" + ProjectDisplayName
	project, err := p.Bdl.GetProjectByOrgIdAndName(orgId, name, userID)
	if err != nil {
		return 0, err
	}
	if project != nil {
		return project.ID, nil
	}

	desc := fmt.Sprintf(ProjectDescFormat, az)
	projectId, err := p.createProject(orgId, name, az, ProjectDisplayName, desc)
	if err != nil {
		return 0, err
	}

	return projectId, nil
}

// todo: 询 msp 这里创建项目时了逻辑，以及 quota 按环境如何分配
func (p *provider) createProject(orgId uint64, projectName, clusterName, displayName, desc string) (uint64, error) {
	return p.Bdl.CreateProject(apistructs.ProjectCreateRequest{
		ResourceConfigs: &apistructs.ResourceConfigs{
			PROD: &apistructs.ResourceConfig{
				ClusterName: clusterName,
				CPUQuota:    5.0 / 4,
				MemQuota:    16.0 / 4,
			},
			STAGING: &apistructs.ResourceConfig{
				ClusterName: clusterName,
				CPUQuota:    5.0 / 4,
				MemQuota:    16.0 / 4,
			},
			TEST: &apistructs.ResourceConfig{
				ClusterName: clusterName,
				CPUQuota:    5.0 / 4,
				MemQuota:    16.0 / 4,
			},
			DEV: &apistructs.ResourceConfig{
				ClusterName: clusterName,
				CPUQuota:    5.0 / 4,
				MemQuota:    16.0 / 4,
			},
		},
		OrgID:       orgId,
		Name:        projectName,
		DisplayName: displayName,
		Desc:        desc,
		CpuQuota:    5.0,
		MemQuota:    16.0,
	}, p.GetDiceOperatorId())
}

func (p *provider) getApplicationId(orgId uint64, projectId uint64, engine string, userId string) (uint64, error) {
	applicationName := engine + ApplicationNameSuffix
	operatorId := p.GetDiceOperatorId()
	applications, err := p.Bdl.GetAppsByProjectAndAppName(projectId, orgId, operatorId, applicationName)
	if err != nil {
		return 0, err
	}
	if applications.Total > 0 {
		return applications.List[0].ID, nil
	}

	applicationId, err := p.createApplication(orgId, projectId, applicationName, engine, "ABILITY")
	if err != nil {
		return 0, err
	}

	if len(userId) == 0 {
		return applicationId, nil
	}

	err = p.addApplicationManager(applicationId, userId)
	if err != nil {
		return 0, err
	}

	return applicationId, nil
}

func (p *provider) createApplication(orgId uint64, projectId uint64, applicationName string, desc string, mode string) (uint64, error) {
	app, err := p.Bdl.CreateApp(apistructs.ApplicationCreateRequest{
		ProjectID: projectId,
		Name:      applicationName,
		Desc:      desc,
		Mode:      apistructs.ApplicationMode(mode),
	}, p.GetDiceOperatorId())
	if err != nil {
		return 0, err
	}
	return app.ID, nil
}

func (p *provider) addApplicationManager(applicationId uint64, managerUserId string) error {
	return p.Bdl.AddMember(apistructs.MemberAddRequest{
		Scope: apistructs.Scope{
			Type: "app",
			ID:   strconv.FormatUint(applicationId, 10),
		},
		Roles:   []string{"Manager"},
		UserIDs: []string{managerUserId},
	}, p.GetDiceOperatorId())
}

func (p *provider) createRelease(orgId uint64, dice *diceyml.Object, engine string, version string) (string, error) {

	releaseName := engine + ":" + version
	for _, service := range dice.Services {
		if service.Deployments.Selectors == nil {
			service.Deployments.Selectors = map[string]diceyml.Selector{}
		}
		service.Envs["ADDON_GROUP"] = "ability"
		service.Envs["ADDON_TYPE"] = engine
		service.Envs["ADDON_ID"] = engine
	}

	yml, err := yaml.Marshal(*dice)
	if err != nil {
		return "", fmt.Errorf("error marshal to yaml:%s", err.Error())
	}

	ctx := transport.WithHeader(context.Background(), metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	resp, err := p.DicehubReleaseSvc.CreateRelease(ctx, &pb.ReleaseCreateRequest{
		ReleaseName: releaseName,
		Dice:        string(yml),
		OrgID:       int64(orgId),
		UserID:      p.GetDiceOperatorId(),
	})
	if err != nil {
		return "", err
	}
	return resp.ReleaseID, nil
}

func (p *provider) getRuntimeId(deployMode string, orgId uint64, projectId uint64, applicationId uint64, workspace string, version string, releaseId string, clusterName string) (uint64, error) {
	runtimeName := version
	if deployMode == handlers.DeployModePAAS {
		runtimeName += "-" + clusterName
	}

	runtimes, err := p.Bdl.GetRuntimes(runtimeName, strconv.FormatUint(applicationId, 10), workspace, strconv.FormatUint(orgId, 10), p.GetDiceOperatorId())
	if err != nil {
		return 0, err
	}

	if len(runtimes) > 0 {
		runtime := runtimes[0]
		if runtime.DeployStatus == apistructs.DeploymentStatusWaiting || runtime.DeployStatus == apistructs.DeploymentStatusDeploying {
			return runtime.ID, nil
		} else if runtime.DeployStatus == apistructs.DeploymentStatusCanceling {
			return 0, fmt.Errorf("get runtime canceling, can not deploy")
		}
	}

	runtimeId, err := p.createRuntime(orgId, projectId, applicationId, workspace, runtimeName, releaseId, clusterName)
	if err != nil {
		return 0, err
	}

	return runtimeId, nil
}

func (p *provider) createRuntime(orgId uint64, projectId uint64, applicationId uint64, workspace string, runtimeName string, releaseId string, clusterName string) (uint64, error) {
	runtime, err := p.Bdl.CreateRuntime(apistructs.RuntimeCreateRequest{
		Name:        runtimeName,
		ReleaseID:   releaseId,
		Operator:    p.GetDiceOperatorId(),
		ClusterName: clusterName,
		Source:      "PIPELINE",
		Extra: apistructs.RuntimeCreateRequestExtra{
			OrgID:         orgId,
			ProjectID:     projectId,
			ApplicationID: applicationId,
			Workspace:     workspace,
		},
	}, orgId, p.GetDiceOperatorId())
	if err != nil {
		return 0, err
	}
	return runtime.RuntimeID, nil
}

func (p *provider) waitRuntimeReady(instance *db.Instance, runtimeId uint64) (*bundle.GetRuntimeServicesResponseData, error) {

	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(instance.Options, &instanceOptions)

	orgId, _ := strconv.ParseUint(instanceOptions["orgId"], 10, 64)

	startTime := time.Now().Unix()
	for time.Now().Unix()-startTime < handlers.RuntimeMaxUpTimeoutSeconds {
		runtime, err := p.Bdl.GetRuntimeServices(runtimeId, orgId, p.GetDiceOperatorId())
		if err != nil {
			continue
		}

		if runtime.DeployStatus == string(apistructs.DeploymentStatusOK) {
			return runtime, nil
		}

		time.Sleep(10 * time.Second)
	}

	return nil, fmt.Errorf("wait servicegroup up timeout")
}
