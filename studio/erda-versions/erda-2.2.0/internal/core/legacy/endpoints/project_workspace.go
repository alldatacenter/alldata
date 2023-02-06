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

package endpoints

import (
	"context"
	"encoding/json"
	"io/ioutil"
	"net/http"
	"strconv"

	"github.com/google/uuid"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// CreateProjectWorkSpace 创建 project workspace abilities
func (e *Endpoints) CreateProjectWorkSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateProjectWorkspaceAbilities.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.ErrCreateProjectWorkspaceAbilities.InvalidParameter(err).ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrCreateProjectWorkspaceAbilities.MissingParameter("body").ToResp(), nil
	}
	bodyData, err := ioutil.ReadAll(r.Body)
	if err != nil {
		logrus.WithError(err).Errorln("failed to read request body")
		return apierrors.ErrCreateProjectWorkspaceAbilities.InvalidParameter(err).ToResp(), nil
	}
	var projectWorkSpaceCreateReq apistructs.ProjectWorkSpaceAbility
	if err := json.Unmarshal(bodyData, &projectWorkSpaceCreateReq); err != nil {
		return apierrors.ErrCreateProjectWorkspaceAbilities.InvalidParameter(err).ToResp(), nil
	}

	isValidWorkspace := false
	for _, v := range apistructs.DiceWorkspaceSlice {
		if string(v) == projectWorkSpaceCreateReq.Workspace {
			isValidWorkspace = true
			break
		}
	}

	if !isValidWorkspace {
		return apierrors.ErrCreateProjectWorkspaceAbilities.InvalidParameter(errors.Errorf("workspace %s is invalid",
			projectWorkSpaceCreateReq.Workspace)).ToResp(), nil
	}

	logrus.Infof("erda_workspace create request body: %+v", projectWorkSpaceCreateReq)
	logrus.Infof("erda_workspace create request body data: %s", string(bodyData))

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.ProjectScope,
		ScopeID:  projectWorkSpaceCreateReq.ProjectID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.CreateAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrCreateProjectWorkspaceAbilities.AccessDenied().ToResp(), nil
	}

	if projectWorkSpaceCreateReq.ID == "" {
		projectWorkSpaceCreateReq.ID = uuid.NewString()
	}

	if projectWorkSpaceCreateReq.OrgID == 0 {
		projectWorkSpaceCreateReq.OrgID = orgID
	}

	err = e.db.CreateProjectWorkspaceAbilities(projectWorkSpaceCreateReq)
	if err != nil {
		return apierrors.ErrCreateProjectWorkspaceAbilities.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(nil)
}

// GetProjectWorkSpace 通过 project_id 和 workspace 获取 project workspace abilities
func (e *Endpoints) GetProjectWorkSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetProjectWorkspaceAbilities.NotLogin().ToResp(), nil
	}

	projid := vars["projectID"]
	workspace := vars["workspace"]

	logrus.Infof("get project workspace abilities for projiectID %s with workspace %s", projid, workspace)

	projectID, err := strconv.ParseUint(projid, 10, 64)
	if err != nil {
		return apierrors.ErrGetProjectWorkspaceAbilities.InvalidParameter("projectID").ToResp(), nil
	}

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.ProjectScope,
		ScopeID:  projectID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrCreateProjectWorkspaceAbilities.AccessDenied().ToResp(), nil
	}

	ability, err := e.db.GetProjectWorkspaceAbilities(projectID, workspace)
	if err != nil {
		return apierrors.ErrGetProjectWorkspaceAbilities.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(ability)
}

// UpdateProjectWorkSpace 更新 project workspace abilities
func (e *Endpoints) UpdateProjectWorkSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateProjectWorkspaceAbilities.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InvalidParameter(err).ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrUpdateProjectWorkspaceAbilities.MissingParameter("body").ToResp(), nil
	}
	bodyData, err := ioutil.ReadAll(r.Body)
	if err != nil {
		logrus.WithError(err).Errorln("failed to read request body")
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InvalidParameter(err).ToResp(), nil
	}
	var projectWorkSpaceUpdateReq apistructs.ProjectWorkSpaceAbility
	if err := json.Unmarshal(bodyData, &projectWorkSpaceUpdateReq); err != nil {
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InvalidParameter(err).ToResp(), nil
	}

	isValidWorkspace := false
	for _, v := range apistructs.DiceWorkspaceSlice {
		if string(v) == projectWorkSpaceUpdateReq.Workspace {
			isValidWorkspace = true
			break
		}
	}

	logrus.Infof("erda_workspace update request body: %+v", projectWorkSpaceUpdateReq)
	logrus.Infof("erda_workspace update request body data: %s", string(bodyData))

	if !isValidWorkspace {
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InvalidParameter(errors.Errorf("workspace %s is invalid",
			projectWorkSpaceUpdateReq.Workspace)).ToResp(), nil
	}

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.ProjectScope,
		ScopeID:  projectWorkSpaceUpdateReq.ProjectID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.UpdateAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrUpdateProjectWorkspaceAbilities.AccessDenied().ToResp(), nil
	}

	ability, err := e.db.GetProjectWorkspaceAbilities(projectWorkSpaceUpdateReq.ProjectID, projectWorkSpaceUpdateReq.Workspace)
	if err != nil {
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InternalError(err).ToResp(), nil
	}

	toUpdate := make(map[string]string)
	err = json.Unmarshal([]byte(projectWorkSpaceUpdateReq.Abilities), &toUpdate)
	if err != nil {
		errToReport := errors.Errorf("Unmarshal update request abilities failed: %v", err)
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InternalError(errToReport).ToResp(), nil
	}
	abilities := make(map[string]string)
	err = json.Unmarshal([]byte(ability.Abilities), &abilities)
	if err != nil {
		errToReport := errors.Errorf("Unmarshal old abilities failed: %v", err)
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InternalError(errToReport).ToResp(), nil
	}

	for k, v := range toUpdate {
		abilities[k] = v
	}

	str, err := json.Marshal(abilities)
	if err != nil {
		errToReport := errors.Errorf("Marshal new abilities failed: %v", err)
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InternalError(errToReport).ToResp(), nil
	}

	ability.Abilities = string(str)
	if ability.OrgID == 0 {
		ability.OrgID = orgID
	}

	err = e.db.UpdateProjectWorkspaceAbilities(ability)
	if err != nil {
		return apierrors.ErrUpdateProjectWorkspaceAbilities.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(nil)
}

// DeleteProjectWorkSpace 通过 project_id 和 workspace 删除对应的 project workspace abilities
func (e *Endpoints) DeleteProjectWorkSpace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDeleteProjectWorkspaceAbilities.NotLogin().ToResp(), nil
	}

	projid := r.URL.Query().Get("projectID")
	workspace := r.URL.Query().Get("workspace")

	if workspace == "" {
		logrus.Infof("delete project workspace abilities for projiectID %s", projid)
	} else {
		logrus.Infof("delete project workspace ability for projiectID %s with workspace %s", projid, workspace)
	}

	projectID, err := strconv.ParseUint(projid, 10, 64)
	if err != nil {
		return apierrors.ErrDeleteProjectWorkspaceAbilities.InvalidParameter("projectID").ToResp(), nil
	}

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.ProjectScope,
		ScopeID:  projectID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.DeleteAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrDeleteProjectWorkspaceAbilities.AccessDenied().ToResp(), nil
	}

	err = e.db.DeleteProjectWorkspaceAbilities(projectID, workspace)
	if err != nil {
		return apierrors.ErrDeleteProjectWorkspaceAbilities.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(nil)
}
