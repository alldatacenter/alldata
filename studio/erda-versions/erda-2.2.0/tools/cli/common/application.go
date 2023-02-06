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
	"fmt"
	"os/exec"
	"strconv"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/tools/cli/command"
	"github.com/erda-project/erda/tools/cli/utils"
)

func GetApplicationDetail(ctx *command.Context, orgID, projectID, applicationID uint64) (
	apistructs.ApplicationDTO, error) {
	var (
		resp apistructs.ApplicationFetchResponse
		b    bytes.Buffer
	)

	response, err := ctx.Get().Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Path(fmt.Sprintf("/api/applications/%d?projectID=%d", applicationID, projectID)).
		Do().Body(&b)
	if err != nil {
		return apistructs.ApplicationDTO{}, fmt.Errorf(utils.FormatErrMsg(
			"get application detail", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return apistructs.ApplicationDTO{}, fmt.Errorf(utils.FormatErrMsg("get application detail",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return apistructs.ApplicationDTO{}, fmt.Errorf(utils.FormatErrMsg("get application detail",
			fmt.Sprintf("failed to unmarshal application detail response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return apistructs.ApplicationDTO{}, fmt.Errorf(utils.FormatErrMsg("get application detail",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}

	return resp.Data, nil
}

func GetApplicationIdByName(ctx *command.Context, orgID, projectID uint64, application string) (uint64, error) {
	appList, err := GetApplications(ctx, orgID, projectID)
	if err != nil {
		return 0, err
	}

	for _, app := range appList {
		if app.Name == application {
			return app.ID, nil
		}
	}
	return 0, errors.New(fmt.Sprintf("Invalid application name %s, may not exist or has no permission", application))
}

func GetApplications(ctx *command.Context, orgID, projectID uint64) ([]apistructs.ApplicationDTO, error) {
	var apps []apistructs.ApplicationDTO
	err := utils.PagingAll(func(pageNo, pageSize int) (bool, error) {
		page, err := GetPagingApplications(ctx, orgID, projectID, pageNo, pageSize)
		if err != nil {
			return false, err
		}
		apps = append(apps, page.List...)
		return page.Total > len(apps), nil
	}, 50)
	if err != nil {
		return nil, err
	}

	return apps, nil
}

func GetMyApplications(ctx *command.Context, orgID, projectID uint64) ([]apistructs.ApplicationDTO, error) {
	var apps []apistructs.ApplicationDTO
	err := utils.PagingAll(func(pageNo, pageSize int) (bool, error) {
		page, err := GetPagingMyApplications(ctx, orgID, projectID, pageNo, pageSize)
		if err != nil {
			return false, err
		}
		apps = append(apps, page.List...)
		return page.Total > len(apps), nil
	}, 50)
	if err != nil {
		return nil, err
	}

	return apps, nil
}

func GetPagingApplications(ctx *command.Context, orgID, projectID uint64, pageNo, pageSize int) (apistructs.ApplicationListResponseData, error) {
	var resp apistructs.ApplicationListResponse
	var b bytes.Buffer

	response, err := ctx.Get().Path("/api/applications").
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Param("projectId", strconv.FormatUint(projectID, 10)).
		Param("pageNo", strconv.Itoa(pageNo)).Param("pageSize", strconv.Itoa(pageSize)).
		Do().Body(&b)
	if err != nil {
		return apistructs.ApplicationListResponseData{}, fmt.Errorf(
			utils.FormatErrMsg("list", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return apistructs.ApplicationListResponseData{}, fmt.Errorf(utils.FormatErrMsg("list",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return apistructs.ApplicationListResponseData{}, fmt.Errorf(utils.FormatErrMsg("list",
			fmt.Sprintf("failed to unmarshal application list response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return apistructs.ApplicationListResponseData{}, fmt.Errorf(
			utils.FormatErrMsg("list",
				fmt.Sprintf("failed to request, error code: %s, error message: %s",
					resp.Error.Code, resp.Error.Msg), false))
	}

	return resp.Data, nil
}

func GetPagingMyApplications(ctx *command.Context, orgID, projectID uint64, pageNo, pageSize int) (apistructs.ApplicationListResponseData, error) {
	var resp apistructs.ApplicationListResponse
	var b bytes.Buffer

	response, err := ctx.Get().Path("/api/applications/actions/list-my-applications").
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Param("projectId", strconv.FormatUint(projectID, 10)).
		Param("pageNo", strconv.Itoa(pageNo)).Param("pageSize", strconv.Itoa(pageSize)).
		Do().Body(&b)
	if err != nil {
		return apistructs.ApplicationListResponseData{}, fmt.Errorf(
			utils.FormatErrMsg("list", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return apistructs.ApplicationListResponseData{}, fmt.Errorf(utils.FormatErrMsg("list",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return apistructs.ApplicationListResponseData{}, fmt.Errorf(utils.FormatErrMsg("list",
			fmt.Sprintf("failed to unmarshal application list response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return apistructs.ApplicationListResponseData{}, fmt.Errorf(
			utils.FormatErrMsg("list",
				fmt.Sprintf("failed to request, error code: %s, error message: %s",
					resp.Error.Code, resp.Error.Msg), false))
	}

	return resp.Data, nil
}

func DeleteApplication(ctx *command.Context, applicationID uint64) error {
	var resp apistructs.ApplicationDeleteResponse
	var b bytes.Buffer

	response, err := ctx.Delete().
		Path(fmt.Sprintf("/api/applications/%d", applicationID)).Do().Body(&b)
	if err != nil {
		return fmt.Errorf(
			utils.FormatErrMsg("delete", "failed to request ("+err.Error()+")", false))
	}

	if !response.IsOK() {
		return fmt.Errorf(utils.FormatErrMsg("delete",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				response.StatusCode(), response.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &resp); err != nil {
		return fmt.Errorf(utils.FormatErrMsg("delete",
			fmt.Sprintf("failed to unmarshal releases remove application response ("+err.Error()+")"), false))
	}

	if !resp.Success {
		return fmt.Errorf(utils.FormatErrMsg("delete",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				resp.Error.Code, resp.Error.Msg), false))
	}
	return nil
}

func CreateApplication(ctx *command.Context, projectID uint64, application, mode, desc,
	sonarhost, sonartoken, sonarproject string) (apistructs.ApplicationDTO, error) {
	var request apistructs.ApplicationCreateRequest
	var response apistructs.ApplicationCreateResponse
	var b bytes.Buffer

	request.Name = application
	request.Mode = apistructs.ApplicationMode(mode)
	request.Desc = desc
	request.ProjectID = projectID
	if sonarhost != "" {
		request.SonarConfig = &apistructs.SonarConfig{
			Host:       sonarhost,
			Token:      sonartoken,
			ProjectKey: sonarproject,
		}
	}

	resp, err := ctx.Post().Path("/api/applications").JSONBody(request).Do().Body(&b)
	if err != nil {
		return apistructs.ApplicationDTO{}, fmt.Errorf(
			utils.FormatErrMsg("create", "failed to request ("+err.Error()+")", false))
	}

	if !resp.IsOK() {
		return apistructs.ApplicationDTO{}, fmt.Errorf(utils.FormatErrMsg("create",
			fmt.Sprintf("failed to request, status-code: %d, content-type: %s, raw bod: %s",
				resp.StatusCode(), resp.ResponseHeader("Content-Type"), b.String()), false))
	}

	if err := json.Unmarshal(b.Bytes(), &response); err != nil {
		return apistructs.ApplicationDTO{}, fmt.Errorf(utils.FormatErrMsg("create",
			fmt.Sprintf("failed to unmarshal application create response ("+err.Error()+")"), false))
	}

	if !response.Success {
		return apistructs.ApplicationDTO{}, fmt.Errorf(utils.FormatErrMsg("create",
			fmt.Sprintf("failed to request, error code: %s, error message: %s",
				response.Error.Code, response.Error.Msg), false))
	}

	return response.Data, nil
}

func PushApplication(dir, repo string, force bool) error {
	// config remote
	remoteName := fmt.Sprintf("remote-%d", time.Now().UnixNano())
	addRemote := exec.Command("git", "remote", "add", remoteName, repo)
	addRemote.Dir = dir
	output, err := addRemote.CombinedOutput()
	if err != nil {
		return errors.Errorf("failed to add remote repo, error: %v, %s", err, output)
	}
	defer func() {
		removeRemote := exec.Command("git", "remote", "remove", remoteName)
		removeRemote.Dir = dir
		output, err = removeRemote.CombinedOutput()
	}()

	// push code
	args := []string{"push", "-u", remoteName, "--all"}
	if force {
		args = append(args, "--force")
	}
	pushAll := exec.Command("git", args...)
	pushAll.Dir = dir
	output, err = pushAll.CombinedOutput()
	if err != nil {
		return errors.Errorf("git push repo %s err, %s", repo, output)
	}

	args = []string{"push", "-u", remoteName, "--tags"}
	if force {
		args = append(args, "--force")
	}
	pushTags := exec.Command("git", args...)
	pushTags.Dir = dir
	output, err = pushTags.CombinedOutput()
	if err != nil {
		return errors.Errorf("git push repo %s err, %s", repo, output)
	}

	return nil
}

func GetApplicationID(ctx *command.Context, orgID, projectID uint64, application string) (string, uint64, error) {
	var applicationID uint64
	if application != "" {
		// TODO get no projectid
		appId, err := GetApplicationIdByName(ctx, orgID, projectID, application)
		if err != nil {
			return application, applicationID, err
		}
		applicationID = appId
	}

	if application == "" && ctx.CurrentApplication.Application == "" {
		return application, applicationID, errors.New("Invalid application name")
	}

	if application == "" && ctx.CurrentApplication.Application != "" {
		application = ctx.CurrentApplication.Application
	}

	if applicationID <= 0 && ctx.CurrentApplication.ApplicationID <= 0 && application != "" {
		appId, err := GetApplicationIdByName(ctx, orgID, projectID, application)
		if err != nil {
			return application, applicationID, err
		}
		ctx.CurrentApplication.ApplicationID = appId
		applicationID = appId
	}

	if applicationID <= 0 && ctx.CurrentApplication.ApplicationID <= 0 {
		return application, applicationID, errors.New("Invalid application id")
	}

	if applicationID == 0 && ctx.CurrentApplication.ApplicationID > 0 {
		applicationID = ctx.CurrentApplication.ApplicationID
	}

	return application, applicationID, nil
}
