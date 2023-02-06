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
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/common"
	"github.com/erda-project/erda/tools/cli/utils"
)

var PROJECTDEPLOYMENTSTART = command.Command{
	Name:       "start",
	ParentName: "PROJECTDEPLOYMENT",
	ShortHelp:  "start project's runtimes and addons",
	Example: `
  $ erda-cli project-deployment start --org xxx --project yyy --workspace DEV
`,
	Flags: []command.Flag{
		command.StringFlag{Name: "org", Doc: "[required] the org name the project belongs to", DefaultValue: ""},
		command.StringFlag{Name: "project", Doc: "[required] which project's runtimes to stop", DefaultValue: ""},
		command.StringFlag{Name: "workspace", Doc: "[required] which workspace's runtimes to stop", DefaultValue: ""},
	},
	Run: RunStartProjectInWorkspace,
}

func RunStartProjectInWorkspace(ctx *command.Context, org, project, workspace string) error {
	if project == "" || workspace == "" || org == "" {
		return fmt.Errorf(
			utils.FormatErrMsg("project-deployment start", "failed to start project deployments, one of the flags [org, project, workspace] not set", true))
	}

	uop, err := common.GetUserOrgProjID(ctx, org, project)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("project-deployment start", "failed to start project deployments, can not get orgID or userID or projectID: "+err.Error(), true))
	}

	params := PDParameters{
		orgId:     uop.OrgId,
		userId:    uop.UserId,
		projectId: uop.ProjectId,
		workspace: workspace,
		action:    PROJECT_DEPLOYMENT_ACTION_START,
	}

	appIds, err := GetProjectApplicationIds(ctx, params)
	if err != nil {
		return err
	}

	if len(appIds) == 0 {
		ctx.Info("No applications found for project need start\n")
	}

	runtimeIds := make([]string, 0)
	if len(appIds) > 0 {
		ctx.Info("Project's applications IDs to start is:%v\n", appIds)
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
			ctx.Info("No runtimes found for project need start\n")
		}
	}

	addonIds, err := GetProjectAddonsRoutingKeysForStopOrStart(ctx, params)
	if err != nil {
		return err
	}

	if len(addonIds) == 0 {
		ctx.Info("No addons found for project need start\n")
	}
	if len(addonIds) > 0 {
		ctx.Info("Begin to start project's addons for addon IDs:%v\n", addonIds)
		// TODO: STOP Addons
		if err = StartProjectWorkspaceAddons(ctx, addonIds, params); err != nil {
			return err
		}

		ctx.Info("Waitting %d minutes for project's addons to Running\n", ADDONS_RESTART_WAITTING_DELAY)
		tick := time.Tick(1 * time.Second)
		waits := ADDONS_RESTART_WAITTING_DELAY * 60
		for waits > 0 {
			select {
			case <-tick:
				fmt.Printf("\r%3d", waits)
				waits--
			}
		}

		err = waitProjectAddonsComplete(ctx, params)
		if err != nil {
			return err
		}
	}
	ctx.Succ("project-deployment start project's addons success\n")

	if len(runtimeIds) > 0 {
		ctx.Info("Begin to start project's runtimes for runtime IDs:%v\n", runtimeIds)
		// TODO: STOP Runtimes
		if err = StartProjectWorkspaceRuntimes(ctx, runtimeIds, params); err != nil {
			return err
		}
	}
	ctx.Succ("project-deployment start project's runtimes success\n")

	return nil
}

func StartProjectWorkspaceRuntimes(ctx *command.Context, runtimeIds []string, params PDParameters) error {
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
				utils.FormatErrMsg("project-deployment start", "failed to ParseUint "+runtimeId, false))
		}
		req.IDs = append(req.IDs, id)
	}

	response, err := ctx.Put().Path(fmt.Sprintf("/api/runtimes/actions/batch-update-pre-overlay?scale_action=scaleUp")).
		Header(httputil.OrgHeader, params.orgId).
		JSONBody(req).Do().JSON(&rsp)

	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("project-deployment start", "failed to get applications ("+err.Error()+")", false))
	}
	if !response.IsOK() || !rsp.Success {
		return fmt.Errorf(utils.FormatErrMsg("project-deployment start",
			fmt.Sprintf("failed to start project runtimes, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	if rsp.Data == nil {
		return fmt.Errorf(utils.FormatErrMsg("project-deployment start",
			fmt.Sprintf("failed to start project runtimes, return of scale action scaleDown is nil. status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	return nil
}

func StartProjectWorkspaceAddons(ctx *command.Context, addonIds []string, params PDParameters) error {
	var rsp struct {
		apistructs.Header
		Data apistructs.AddonScaleResults
	}

	req := apistructs.AddonScaleRecords{
		AddonRoutingIDs: make([]string, 0),
	}
	req.AddonRoutingIDs = addonIds

	response, err := ctx.Post().Path(fmt.Sprintf("/api/addons?scale_action=scaleUp")).
		Header(httputil.OrgHeader, params.orgId).
		JSONBody(req).Do().JSON(&rsp)

	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("project-deployment start", "failed to start project addons ("+err.Error()+")", false))
	}
	if !response.IsOK() || !rsp.Success {
		if rsp.Data.Faild > 0 {
			ctx.Warn("Failed to start all addons:\n")
			succIDs := make([]string, 0)
			for _, id := range addonIds {
				if info, ok := rsp.Data.FailedInfo[id]; !ok {
					succIDs = append(succIDs, id)
				} else {
					ctx.Error("Failed start addon:%s, Reason:%s \n", id, info)
				}
			}
			if len(succIDs) > 0 {
				ctx.Info("Successed to stop addons: %v\n", succIDs)
			}
		}

		return fmt.Errorf(utils.FormatErrMsg("project-deployment start",
			fmt.Sprintf("failed to start project addons, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	ctx.Info("Successed to start all addons: %v\n", addonIds)

	return nil
}

func waitProjectAddonsComplete(ctx *command.Context, params PDParameters) error {
	addons := make([]apistructs.AddonFetchResponseData, 0)
	var err error
	for retry := 0; retry < 3; retry++ {
		addons, err = checkProjectAddons(ctx, params)
		if err != nil {
			if retry < 2 {
				ctx.Warn("Check addons for project failed: %v\n", err)
				time.Sleep(15 * time.Second)
				continue
			} else {
				return fmt.Errorf(
					utils.FormatErrMsg("project-deployment start", fmt.Sprintf("check addons for project %s failed ", params.projectId), false))
			}
		}

		if len(addons) > 0 {
			if retry < 2 {
				ctx.Warn("Check addons for project failed, addons %#v for projectID %s not completed\n", addons, params.projectId)
				time.Sleep(15 * time.Second)
				continue
			}
		}
	}

	if len(addons) > 0 {
		switch params.action {
		case PROJECT_DEPLOYMENT_ACTION_STOP:
			ctx.Error("Addons Stop Failed List\n")
		case PROJECT_DEPLOYMENT_ACTION_START:
			ctx.Error("Addons Start Failed List\n")
		}
		for _, addon := range addons {
			ctx.Error("AddonID:%d  Name:%s  DisplayName:%s  Plan:%s  Version:%s  OrgID:%d  ProjectID:%d  ProjectName:%s  Workspace: %s Status:%s  Reference: %d AttachCount:%d\n",
				addon.ID, addon.Name, addon.AddonDisplayName, addon.Plan, addon.Version, addon.OrgID, addon.ProjectID, addon.ProjectName, addon.Workspace, addon.Status, addon.Reference, addon.AttachCount)
		}
		return fmt.Errorf(utils.FormatErrMsg("project-deployment start", "some addons start failed", false))
	}
	return nil
}

func checkProjectAddons(ctx *command.Context, params PDParameters) ([]apistructs.AddonFetchResponseData, error) {
	var listResp apistructs.AddonListResponse

	response, err := ctx.Get().Path(fmt.Sprintf("/api/addons")).
		Header("Org-ID", params.orgId).
		Param("type", "project").
		Param("value", params.projectId).
		Do().
		JSON(&listResp)
	if err != nil {
		return nil, fmt.Errorf(
			utils.FormatErrMsg("project-deployment start", "failed to get addons ("+err.Error()+")", false))
	}
	if !response.IsOK() || !listResp.Success {
		return nil, fmt.Errorf(utils.FormatErrMsg("project-deployment start",
			fmt.Sprintf("failed to get addons, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	addons := make([]apistructs.AddonFetchResponseData, 0)
	for _, addon := range listResp.Data {
		// addon stop/start only support basic addon, and category is not custom
		if addon.PlatformServiceType != apistructs.PlatformServiceTypeBasic || addon.Category == apistructs.AddonCustomCategory {
			continue
		}

		// inside addon stop/start by outside addon
		if addon.IsInsideAddon == apistructs.INSIDE {
			continue
		}

		// addon share scope is not applications or projects can not stop/start
		if addon.ShareScope != apistructs.ApplicationShareScope && addon.ShareScope != apistructs.ProjectShareScope {
			continue
		}

		if params.workspace != "" && addon.Workspace != params.workspace {
			continue
		}

		switch params.action {
		case PROJECT_DEPLOYMENT_ACTION_STOP:
			if addon.Status == string(apistructs.AddonAttached) {
				addons = append(addons, addon)
			}
		case PROJECT_DEPLOYMENT_ACTION_START:
			if addon.Status == string(apistructs.AddonOffline) {
				addons = append(addons, addon)
			}
		}
	}

	return addons, nil
}
