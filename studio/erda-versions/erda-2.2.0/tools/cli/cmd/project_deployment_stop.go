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

package cmd

import (
	"fmt"
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

const (
	PROJECT_DEPLOYMENT_ACTION_STOP  string = "stop"
	PROJECT_DEPLOYMENT_ACTION_START string = "start"
	RUNTIME_STOP_WAITTING_DELAY     int    = 1
	ADDONS_RESTART_WAITTING_DELAY   int    = 3
	PAGE_SIZE                       uint64 = 1000
)

var PROJECTDEPLOYMENTSTOP = command.Command{
	Name:       "stop",
	ParentName: "PROJECTDEPLOYMENT",
	ShortHelp:  "stop project's runtimes and addons",
	Example: `
  $ erda-cli project-deployment stop --org xxx  --project yyy --workspace DEV
`,
	Flags: []command.Flag{
		command.StringFlag{Name: "org", Doc: "[required] the org name the project belongs to", DefaultValue: ""},
		command.StringFlag{Name: "project", Doc: "[required] which project's runtimes to stop", DefaultValue: ""},
		command.StringFlag{Name: "workspace", Doc: "[required] which workspace's runtimes to stop", DefaultValue: ""},
	},
	Run: RunStopProjectInWorkspace,
}

type GetApplicationRuntimesResponse struct {
	apistructs.Header
	Data []*bundle.GetApplicationRuntimesDataEle
}

type GetApplicationRuntimesDataEleExtra struct {
	ApplicationID uint64
	BuildID       uint64
	Workspace     string
}

type PDParameters struct {
	orgId     string
	userId    string
	projectId string
	workspace string
	action    string
}

func RunStopProjectInWorkspace(ctx *command.Context, org, project, workspace string) error {
	if project == "" || workspace == "" || org == "" {
		return fmt.Errorf(
			utils.FormatErrMsg("project-deployment stop", "failed to stop project deployments, one of the flags [org, project, workspace] not set", true))
	}

	uop, err := common.GetUserOrgProjID(ctx, org, project)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("project-deployment stop", "failed to stop project deployments, can not get orgID or userID or projectID: "+err.Error(), true))
	}

	params := PDParameters{
		orgId:     uop.OrgId,
		userId:    uop.UserId,
		projectId: uop.ProjectId,
		workspace: workspace,
		action:    PROJECT_DEPLOYMENT_ACTION_STOP,
	}

	appIds, err := GetProjectApplicationIds(ctx, params)
	if err != nil {
		return err
	}

	if len(appIds) == 0 {
		ctx.Info("No applications found for project can stop\n")
	}
	if len(appIds) > 0 {
		ctx.Info("Project's applications IDs to stop is:%v\n", appIds)

		runtimeIds := make([]string, 0)
		for _, appId := range appIds {
			appRuntimeIds, err := GetProjectApplicationRuntimesIDsForStopOrStart(ctx, params, appId)
			if err != nil {
				return err
			}

			if len(appRuntimeIds) > 0 {
				runtimeIds = append(runtimeIds, appRuntimeIds...)
			}
		}
		if len(runtimeIds) == 0 {
			ctx.Info("No runtimes found for project %s can stop\n", project)
		}

		if len(runtimeIds) > 0 {
			ctx.Info("Begin to stop project's runtimes for runtime IDs:%v\n", runtimeIds)
			// TODO: STOP Runtimes
			if err = StopProjectWorkspaceRuntimes(ctx, runtimeIds, params); err != nil {
				return err
			}

			ctx.Info("Waitting %d minutes for project's runtimes to Terminating\n", RUNTIME_STOP_WAITTING_DELAY)
			tick := time.Tick(1 * time.Second)
			waits := RUNTIME_STOP_WAITTING_DELAY * 60
			for waits > 0 {
				select {
				case <-tick:
					fmt.Printf("\r%3d", waits)
					waits--
				}
			}

			err = waitProjectRuntimesComplete(appIds, ctx, params)
			if err != nil {
				return err
			}
		}
	}
	ctx.Succ("project-deployment stop project's runtimes success\n")

	addonIds, err := GetProjectAddonsRoutingKeysForStopOrStart(ctx, params)
	if err != nil {
		return err
	}

	if len(addonIds) == 0 {
		ctx.Info("No addons found for project to stop\n")
	}

	if len(addonIds) > 0 {
		ctx.Info("Begin to stop project's addons for addon IDs:%v\n", addonIds)
		// TODO: STOP Addons
		if err = StopProjectWorkspaceAddons(ctx, addonIds, params); err != nil {
			return err
		}
	}
	ctx.Succ("project-deployment stop project's addons success\n")

	return nil
}

func StopProjectWorkspaceRuntimes(ctx *command.Context, runtimeIds []string, params PDParameters) error {
	var rsp struct {
		apistructs.Header
		Data interface{}
	}

	req := apistructs.RuntimeScaleRecords{
		IDs: make([]uint64, 0),
	}
	for _, runtimeId := range runtimeIds {
		id, err := strconv.ParseUint(runtimeId, 10, 64)
		if err != nil {
			return fmt.Errorf(
				utils.FormatErrMsg("project-deployment stop", "failed to ParseUint "+runtimeId, false))
		}
		req.IDs = append(req.IDs, id)
	}

	response, err := ctx.Put().Path(fmt.Sprintf("/api/runtimes/actions/batch-update-pre-overlay?scale_action=scaleDown")).
		Header(httputil.OrgHeader, params.orgId).
		JSONBody(req).Do().JSON(&rsp)

	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("project-deployment stop", "failed to stop project runtimes ("+err.Error()+")", false))
	}
	if !response.IsOK() || !rsp.Success {
		return fmt.Errorf(utils.FormatErrMsg("project-deployment stop",
			fmt.Sprintf("failed to stop project runtimes, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	if rsp.Data == nil {
		return fmt.Errorf(utils.FormatErrMsg("project-deployment stop",
			fmt.Sprintf("failed to stop project runtimes, return of scale action scaleDown is nil. status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	return nil
}

func StopProjectWorkspaceAddons(ctx *command.Context, addonIds []string, params PDParameters) error {
	var rsp struct {
		apistructs.Header
		Data apistructs.AddonScaleResults
	}

	req := apistructs.AddonScaleRecords{
		AddonRoutingIDs: make([]string, 0),
	}
	req.AddonRoutingIDs = addonIds

	response, err := ctx.Post().Path(fmt.Sprintf("/api/addons?scale_action=scaleDown")).
		Header(httputil.OrgHeader, params.orgId).
		JSONBody(req).Do().JSON(&rsp)

	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("project-deployment stop", "failed to stop project addons ("+err.Error()+")", false))
	}
	if !response.IsOK() || !rsp.Success {
		if rsp.Data.Faild > 0 {
			ctx.Warn("Failed to stop all addons:\n")
			succIDs := make([]string, 0)
			for _, id := range addonIds {
				if info, ok := rsp.Data.FailedInfo[id]; !ok {
					succIDs = append(succIDs, id)
				} else {
					ctx.Error("Failed stop addon:%s, Reason:%s \n", id, info)
				}
			}
			if len(succIDs) > 0 {
				ctx.Info("Successed to stop addons: %v\n", succIDs)
			}
		}

		return fmt.Errorf(utils.FormatErrMsg("project-deployment stop",
			fmt.Sprintf("failed to stop project addons, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	ctx.Info("Successed to stop all addons: %v\n", addonIds)

	return nil
}

func GetProjectApplicationIds(ctx *command.Context, params PDParameters) ([]string, error) {
	var listResp apistructs.ApplicationListResponse

	response, err := ctx.Get().Path(fmt.Sprintf("/api/applications")).
		Header(httputil.OrgHeader, params.orgId).
		Param("projectId", params.projectId).
		Param("pageSize", strconv.FormatUint(PAGE_SIZE, 10)).
		Param("pageNo", "1").
		Do().JSON(&listResp)
	if err != nil {
		return nil, fmt.Errorf(
			utils.FormatErrMsg("project-deployment stop", "failed to get applications ("+err.Error()+")", false))
	}
	if !response.IsOK() || !listResp.Success {
		return nil, fmt.Errorf(utils.FormatErrMsg("project-deployment stop",
			fmt.Sprintf("failed to get applications, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	appIds := make([]string, 0)
	for _, app := range listResp.Data.List {
		appIds = append(appIds, strconv.FormatUint(app.ID, 10))
	}

	for page := 2; listResp.Data.Total > int(PAGE_SIZE)*(page-1); page++ {
		response, err := ctx.Get().Path(fmt.Sprintf("/api/applications")).
			Header(httputil.OrgHeader, params.orgId).
			Param("projectId", params.projectId).
			Param("pageSize", strconv.FormatUint(PAGE_SIZE, 10)).
			Param("pageNo", strconv.FormatInt(int64(page), 10)).
			Do().JSON(&listResp)
		if err != nil {
			return nil, fmt.Errorf(
				utils.FormatErrMsg("project-deployment stop", "failed to get applications ("+err.Error()+")", false))
		}
		if !response.IsOK() || !listResp.Success {
			return nil, fmt.Errorf(utils.FormatErrMsg("project-deployment stop",
				fmt.Sprintf("failed to get applications, status-code: %d, content-type: %s, raw bod: %s",
					response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
		}
		for _, app := range listResp.Data.List {
			appIds = append(appIds, strconv.FormatUint(app.ID, 10))
		}
	}

	return appIds, nil
}

func GetProjectApplicationRuntimesIDsForStopOrStart(ctx *command.Context, params PDParameters, applicationId string) ([]string, error) {
	var listResp GetApplicationRuntimesResponse

	response, err := ctx.Get().Path("/api/runtimes").
		Param("applicationId", applicationId).
		Header("Org-ID", params.orgId).
		Do().
		JSON(&listResp)
	if err != nil {
		return nil, fmt.Errorf(
			utils.FormatErrMsg("project-deployment stop", "failed to get runtimes ("+err.Error()+")", false))
	}
	if !response.IsOK() || !listResp.Success {
		return nil, fmt.Errorf(utils.FormatErrMsg("project-deployment stop",
			fmt.Sprintf("failed to get runtimes, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	runtimeIds := make([]string, 0)
	for _, runtime := range listResp.Data {

		if params.workspace != "" && params.workspace != runtime.Extra.Workspace {
			continue
		}

		switch params.action {
		case PROJECT_DEPLOYMENT_ACTION_STOP:
			if runtime.Status != apistructs.RuntimeStatusStopped {
				runtimeIds = append(runtimeIds, strconv.FormatUint(runtime.ID, 10))
			}
		case PROJECT_DEPLOYMENT_ACTION_START:
			if runtime.Status == apistructs.RuntimeStatusStopped {
				runtimeIds = append(runtimeIds, strconv.FormatUint(runtime.ID, 10))
			}
		}
	}

	return runtimeIds, nil
}

func checkProjectRuntimes(ctx *command.Context, applicationId string, params PDParameters) ([]bundle.GetApplicationRuntimesDataEle, error) {
	var listResp GetApplicationRuntimesResponse

	response, err := ctx.Get().Path("/api/runtimes").
		Param("applicationId", applicationId).
		Header("Org-ID", params.orgId).
		Do().
		JSON(&listResp)
	if err != nil {
		return []bundle.GetApplicationRuntimesDataEle{}, fmt.Errorf(
			utils.FormatErrMsg("project-deployment stop", "failed to check runtime stopped ("+err.Error()+")", false))
	}
	if !response.IsOK() || !listResp.Success {
		return []bundle.GetApplicationRuntimesDataEle{}, fmt.Errorf(utils.FormatErrMsg("project-deployment stop",
			fmt.Sprintf("failed to check runtime stopped, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	unStoppedRuntimeIds := make([]bundle.GetApplicationRuntimesDataEle, 0)
	for _, runtime := range listResp.Data {
		if params.workspace != "" && params.workspace != runtime.Extra.Workspace {
			continue
		}

		switch params.action {
		case PROJECT_DEPLOYMENT_ACTION_STOP:
			if runtime.Status != apistructs.RuntimeStatusStopped {
				unStoppedRuntimeIds = append(unStoppedRuntimeIds, *runtime)
			}
		case PROJECT_DEPLOYMENT_ACTION_START:
			if runtime.Status == apistructs.RuntimeStatusStopped {
				unStoppedRuntimeIds = append(unStoppedRuntimeIds, *runtime)
			}
		}
	}

	return unStoppedRuntimeIds, nil
}

func waitProjectRuntimesComplete(appIds []string, ctx *command.Context, params PDParameters) error {
	unFinishedRuntimes := make([]bundle.GetApplicationRuntimesDataEle, 0)
	for retry := 0; retry < 3; retry++ {
		for _, appId := range appIds {
			appRuntimes, err := checkProjectRuntimes(ctx, appId, params)
			if err != nil {
				if retry < 2 {
					ctx.Info("Check runtimes for project failed: %v\n", err)
					time.Sleep(15 * time.Second)
					break
				} else {
					return fmt.Errorf(
						utils.FormatErrMsg("project-deployment stop", fmt.Sprintf("check runtimes for project %s failed ", params.projectId), false))
				}
			}

			if len(appRuntimes) > 0 {
				if retry < 2 {
					ctx.Info("Check runtimes for project failed, runtimes %#v for applicationID %s not completed\n", appRuntimes, appId)
					time.Sleep(15 * time.Second)
					break
				} else {
					unFinishedRuntimes = append(unFinishedRuntimes, appRuntimes...)
				}
			}
		}
	}

	if len(unFinishedRuntimes) > 0 {
		switch params.action {
		case PROJECT_DEPLOYMENT_ACTION_STOP:
			ctx.Error("Runtimes Stop Failed List \n")
		case PROJECT_DEPLOYMENT_ACTION_START:
			ctx.Error("Runtimes Restart Failed List \n")
		}
		for _, runtime := range unFinishedRuntimes {
			ctx.Error("RuntimeID:%d  Name:%s  ProjectID:%d  ApplicationID:%d  ReleaseID:%s  Workspace: %s\n",
				runtime.ID, runtime.Name, runtime.ProjectID, runtime.ApplicationID, runtime.ReleaseID, runtime.Extra.Workspace)
		}
		return fmt.Errorf(utils.FormatErrMsg("project-deployment stop", "some runtimes stop failed", false))
	}
	return nil
}

func GetProjectAddonsRoutingKeysForStopOrStart(ctx *command.Context, params PDParameters) ([]string, error) {
	var listResp apistructs.AddonListResponse

	response, err := ctx.Get().Path(fmt.Sprintf("/api/addons")).
		Header("Org-ID", params.orgId).
		Param("type", "project").
		Param("value", params.projectId).
		Do().
		JSON(&listResp)
	if err != nil {
		return nil, fmt.Errorf(
			utils.FormatErrMsg("project-deployment stop", "failed to get addons ("+err.Error()+")", false))
	}
	if !response.IsOK() || !listResp.Success {
		return nil, fmt.Errorf(utils.FormatErrMsg("project-deployment stop",
			fmt.Sprintf("failed to get addons, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	addonRoutingKeyIds := make([]string, 0)
	for _, addon := range listResp.Data {
		// addon stop/start only support basic addon, and category is not custom
		if addon.PlatformServiceType != apistructs.PlatformServiceTypeBasic || addon.Category == apistructs.AddonCustomCategory {
			continue
		}

		// inside addon stop/start by outside addon
		if addon.IsInsideAddon == apistructs.INSIDE {
			continue
		}

		// addon share scope is not applications or projects can not stop/restart
		if addon.ShareScope != apistructs.ApplicationShareScope && addon.ShareScope != apistructs.ProjectShareScope {
			continue
		}

		if params.workspace != "" && addon.Workspace != params.workspace {
			continue
		}

		//TODO: delete this when redis and es supported scale
		if !addonCanStopOrStart(ctx, addon) {
			continue
		}

		switch params.action {
		case PROJECT_DEPLOYMENT_ACTION_STOP:
			if addon.Status == string(apistructs.AddonAttached) {
				addonRoutingKeyIds = append(addonRoutingKeyIds, addon.ID)
			}
		case PROJECT_DEPLOYMENT_ACTION_START:
			if addon.Status == string(apistructs.AddonOffline) {
				addonRoutingKeyIds = append(addonRoutingKeyIds, addon.ID)
			}
		}
	}

	return addonRoutingKeyIds, nil
}

// addonCanStopOrStart check if addon can stop/start
func addonCanStopOrStart(ctx *command.Context, addon apistructs.AddonFetchResponseData) bool {
	if addon.AddonName == apistructs.AddonES && (addon.Version == "6.8.9" || addon.Version == "6.8.22") {
		ctx.Info("addon %s with ID [%s] with plan %s with version %s controlled by Operator which can not do stop/start", apistructs.AddonES, addon.ID, addon.Plan, addon.Version)
		return false
	}

	return true
}
