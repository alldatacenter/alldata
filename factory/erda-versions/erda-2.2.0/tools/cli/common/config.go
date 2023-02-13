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

package common

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/utils"
)

// SetProjectWorkspaceConfigss 为指定项目的指定环境设置 features
func SetProjectWorkspaceConfigs(ctx *command.Context, feature, orgName, projectName, workspace string) error {
	var resp apistructs.ProjectWorkSpaceAbilityResponse

	uop, err := GetUserOrgProjID(ctx, orgName, projectName)
	if err != nil {
		return errors.New("can not get orgID or userID or projectID: " + err.Error())
	}

	prjId, _ := strconv.ParseUint(uop.ProjectId, 10, 64)
	abilities, err := ParseProjectWorkspaceConfigs(feature)
	if err != nil {
		return errors.New("parse config list failed: " + err.Error())
	}

	req := apistructs.ProjectWorkSpaceAbility{
		ProjectID: prjId,
		Workspace: workspace,
		Abilities: abilities,
	}

	response, err := ctx.Post().Path("/api/project-workspace-abilities").
		Header(httputil.OrgHeader, uop.OrgId).
		JSONBody(req).Do().JSON(&resp)

	if err != nil {
		return errors.New("failed to request set project workspace config: " + err.Error())
	}

	if !response.IsOK() {
		return errors.New(fmt.Sprintf("ailed to request set project workspace configs, status-code: %d, content-type: %s, raw bod: %s",
			response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())))
	}

	if !resp.Success {
		return errors.New(fmt.Sprintf("failed to request, error code: %s, raw bod: %s, error message: %s", resp.Error.Code, string(response.Body()), resp.Error.Msg))
	}
	return nil
}

// UpdateProjectWorkspaceConfigs 为指定项目的指定环境更新 features
func UpdateProjectWorkspaceConfigs(ctx *command.Context, feature, org, project, workspace string) error {
	var resp apistructs.ProjectWorkSpaceAbilityResponse

	uop, err := GetUserOrgProjID(ctx, org, project)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("config update", "failed to update config, can not get orgID or userID or projectID: "+err.Error(), true))
	}

	prjId, _ := strconv.ParseUint(uop.ProjectId, 10, 64)
	abilities, err := ParseProjectWorkspaceConfigs(feature)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("config update", fmt.Sprintf("failed to update config, parse config list failed: %v", err), true))
	}

	updateReq := apistructs.ProjectWorkSpaceAbility{
		ProjectID: prjId,
		Workspace: workspace,
		Abilities: abilities,
	}

	response, err := ctx.Put().Path("/api/project-workspace-abilities").
		Header(httputil.OrgHeader, uop.OrgId).
		JSONBody(updateReq).Do().JSON(&resp)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("config update", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("config update",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), string(response.Body())), false))
	}

	if !resp.Success {
		return fmt.Errorf(utils.FormatErrMsg("config update",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return nil
}

// GetProjectWorkspaceConfigs 获取指定项目的指定环境支持的 features
func GetProjectWorkspaceConfigs(ctx *command.Context, org, project, workspace string) error {
	var resp apistructs.ProjectWorkSpaceAbilityResponse
	var b bytes.Buffer

	uop, err := GetUserOrgProjID(ctx, org, project)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("config get", "failed to get configs, can not get orgID or userID or projectID: "+err.Error(), true))
	}

	urlPath := "/api/project-workspace-abilities/" + uop.ProjectId + "/" + workspace

	response, err := ctx.Get().Path(urlPath).
		Header(httputil.OrgHeader, uop.OrgId).
		Do().Body(&b)

	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("config get", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("config get",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err = json.Unmarshal(b.Bytes(), &resp); err != nil {
		return fmt.Errorf(utils.FormatErrMsg("config get",
			fmt.Sprintf("failed to unmarshal get config response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return fmt.Errorf(utils.FormatErrMsg("config get",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	ctx.Info("Configs get result: %s\n", resp.Data.Abilities)
	return nil
}

func DelelteProjectWorkspaceConfigs(ctx *command.Context, org, project, workspace string) error {
	var resp apistructs.ExtensionVersionGetResponse
	var b bytes.Buffer

	uop, err := GetUserOrgProjID(ctx, org, project)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("config delete", "failed to delete config, can not get orgID or userID or projectID: "+err.Error(), true))
	}

	urlPath := ""
	if workspace != "" {
		urlPath = fmt.Sprintf("/api/project-workspace-abilities?projectID=%s&workspace=%s", uop.ProjectId, workspace)
	} else {
		urlPath = fmt.Sprintf("/api/project-workspace-abilities?projectID=%s", uop.ProjectId)
	}

	response, err := ctx.Delete().Path(urlPath).
		Header(httputil.OrgHeader, uop.OrgId).
		Do().Body(&b)

	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("config delete", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("config delete",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err = json.Unmarshal(b.Bytes(), &resp); err != nil {
		return fmt.Errorf(utils.FormatErrMsg("config delete",
			fmt.Sprintf("failed to unmarshal config delete response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return fmt.Errorf(utils.FormatErrMsg("config delete",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return nil
}

// ParseProjectWorkspaceConfigs 解析 project workspace 的 feature list
func ParseProjectWorkspaceConfigs(featureList string) (string, error) {
	features := make(map[string]string)

	fts := strings.Split(featureList, ",")

	if len(fts) > 0 {
		for _, f := range fts {
			kv := strings.Split(f, "=")
			switch {
			case len(kv) > 2:
				return "", errors.New(fmt.Sprintf("set configs with invalid parameter %s", f))
			case len(kv) == 2:
				features[kv[0]] = kv[1]
			case len(kv) == 1:
				features[kv[0]] = "enable"
			}
		}
	}

	featureStr, err := json.Marshal(features)
	if err != nil {
		return "", errors.New(fmt.Sprintf("set config failed to Marshal request, error: %v ", err))
	}
	return string(featureStr), nil
}
