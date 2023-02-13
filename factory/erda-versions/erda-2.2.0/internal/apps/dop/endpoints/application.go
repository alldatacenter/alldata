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
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/types"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateApplication 创建应用
func (e *Endpoints) CreateApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateApplication.InvalidParameter(err).ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrCreateApplication.MissingParameter("body is nil").ToResp(), nil
	}

	var applicationCreateReq apistructs.ApplicationCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&applicationCreateReq); err != nil {
		return apierrors.ErrCreateApplication.InvalidParameter("can't decode body").ToResp(), nil
	}
	if !strutil.IsValidPrjOrAppName(applicationCreateReq.Name) {
		return apierrors.ErrCreateApplication.InvalidParameter(errors.Errorf("app name is invalid %s",
			applicationCreateReq.Name)).ToResp(), nil
	}
	logrus.Infof("request body: %+v", applicationCreateReq)

	// check param
	if err := checkApplicationCreateParam(applicationCreateReq); err != nil {
		return apierrors.ErrCreateApplication.InvalidParameter(err).ToResp(), nil
	}

	// check permission
	req := apistructs.PermissionCheckRequest{
		UserID:   identity.UserID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  applicationCreateReq.ProjectID,
		Resource: apistructs.AppResource,
		Action:   apistructs.CreateAction,
	}
	if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
		return apierrors.ErrCreateApplication.AccessDenied().ToResp(), nil
	}

	// Create app with repo
	applicationDTO, err := e.bdl.CreateAppWithRepo(applicationCreateReq, identity.UserID)
	if err != nil {
		return apierrors.ErrCreateApplication.InternalError(err).ToResp(), nil
	}

	// generate extra info
	e.namespace.GenerateAppExtraInfo(int64(applicationDTO.ID), int64(applicationDTO.ProjectID))

	return httpserver.OkResp(applicationDTO)
}

func checkApplicationCreateParam(applicationCreateReq apistructs.ApplicationCreateRequest) error {
	if applicationCreateReq.Name == "" {
		return errors.Errorf("invalid request, name is empty")
	}
	if applicationCreateReq.ProjectID == 0 {
		return errors.Errorf("invalid request, projectId is empty")
	}
	err := applicationCreateReq.Mode.CheckAppMode()
	return err
}

// getWorkspaces return workspaces
func (e *Endpoints) getWorkspaces(extraStr string, projectID uint64) []apistructs.ApplicationWorkspace {
	var extra map[string]string
	if err := json.Unmarshal([]byte(extraStr), &extra); err != nil {
		extra = make(map[string]string)
	}
	project, err := e.bdl.GetProject(projectID)
	if err != nil {
		logrus.Error(err)
	}

	workspaces := make([]apistructs.ApplicationWorkspace, 0, len(extra))
	for k, v := range extra {
		if strings.Contains(k, ".") {
			env := strings.Split(k, ".")[0]
			workspace := apistructs.ApplicationWorkspace{
				Workspace:       env,
				ConfigNamespace: v,
			}
			if project != nil {
				workspace.ClusterName = project.ClusterConfig[env]
			}
			workspaces = append(workspaces, workspace)
		}
	}
	return workspaces
}

// DeleteApplication delete application
func (e *Endpoints) DeleteApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrDeleteApplication.NotLogin().ToResp(), nil
	}

	// 检查applicationID合法性
	applicationID, err := strutil.Atoi64(vars["applicationID"])
	if err != nil {
		return apierrors.ErrDeleteApplication.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   identity.UserID,
		Scope:    apistructs.AppScope,
		ScopeID:  uint64(applicationID),
		Resource: apistructs.AppResource,
		Action:   apistructs.DeleteAction,
	}
	if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
		return apierrors.ErrDeleteApplication.AccessDenied().ToResp(), nil
	}
	appDto, err := e.bdl.GetApp(uint64(applicationID))
	if err != nil {
		return apierrors.ErrDeleteApplication.InternalError(err).ToResp(), nil
	}

	// Check if there is a runtime under the application
	runtimes, err := e.bdl.GetApplicationRuntimes(appDto.ID, appDto.OrgID, identity.UserID)
	if err != nil {
		return apierrors.ErrDeleteApplication.InternalError(err).ToResp(), nil
	}
	if len(runtimes) > 0 {
		return apierrors.ErrDeleteApplication.InternalError(fmt.Errorf("failed to delete application(there exists runtime)")).ToResp(), nil
	}
	// Delete repo
	if err = e.deleteGittarRepo(appDto); err != nil {
		return apierrors.ErrDeleteApplication.InternalError(err).ToResp(), nil
	}

	// Delete app
	_, err = e.bdl.DeleteApp(uint64(applicationID), identity.UserID)
	if err != nil {
		return apierrors.ErrDeleteApplication.InternalError(err).ToResp(), nil
	}

	// delete extra
	e.deleteExtraInfo(appDto.Extra, identity)

	// delete issue
	if err = e.db.DeleteIssueAppRelationsByApp(applicationID); err != nil {
		logrus.Warnf("failed to delete mr relations, %v", err)
	}

	// delete branch rule
	if err = e.db.DeleteBranchRuleByScope(apistructs.AppScope, applicationID); err != nil {
		logrus.Warnf("failed to delete app branch rules, (%v)", err)
	}

	return httpserver.OkResp(appDto)
}

func (e *Endpoints) deleteGittarRepo(appDto *apistructs.ApplicationDTO) (err error) {
	if err = e.bdl.DeleteRepo(int64(appDto.ID)); err != nil {
		// 防止有老数据不存在repoID，还是以repo路径进行删除
		if err = e.bdl.DeleteGitRepo(appDto.GitRepoAbbrev); err != nil {
			logrus.Errorf(err.Error())
			return fmt.Errorf("failed to delete repo, please try again")
		}
	}
	return nil
}

// 从addon platform删除namespace
func (e *Endpoints) deleteExtraInfo(extra string, identityInfo apistructs.IdentityInfo) {
	var extraMap map[string]string
	if err := json.Unmarshal([]byte(extra), &extraMap); err != nil {
		logrus.Warnf("failed to unmarshal extra, (%v)", err)
		return
	}
	for _, v := range extraMap {
		// 删除 namespace relation(删除namespace relation须在删除namespace前进行，后续须推动优化)
		if strings.Contains(v, string(types.DefaultWorkspace)) {
			if err := e.namespace.DeleteRelation(nil, nil, v, identityInfo.UserID); err != nil {
				logrus.Warnf(err.Error())
			}
		}
		// 删除 namespace
		if err := e.namespace.DeleteNamespace(nil, v, identityInfo); err != nil {
			logrus.Warnf(err.Error())
		}
	}
}

// InitApplication init mobile application
func (e *Endpoints) InitApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// get current user
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrInitApplication.NotLogin().ToResp(), nil
	}

	applicationID, err := strconv.ParseUint(vars["applicationID"], 10, 64)
	if err != nil {
		return apierrors.ErrInitApplication.InvalidParameter(err).ToResp(), nil
	}

	var appInitReq apistructs.ApplicationInitRequest
	if err := json.NewDecoder(r.Body).Decode(&appInitReq); err != nil {
		return apierrors.ErrInitApplication.InvalidParameter(err).ToResp(), nil
	}
	appInitReq.ApplicationID = applicationID
	appInitReq.IdentityInfo = identityInfo

	if !identityInfo.IsInternalClient() {
		// check permission
		req := apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  applicationID,
			Resource: apistructs.AppResource,
			Action:   apistructs.CreateAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrInitApplication.AccessDenied().ToResp(), nil
		}
	}

	pipelineID, err := e.app.Init(&appInitReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(pipelineID)
}

// UpdateApplication 更新应用
func (e *Endpoints) UpdateApplication(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrUpdateApplication.NotLogin().ToResp(), nil
	}

	applicationID, err := strutil.Atoi64(vars["applicationID"])
	if err != nil {
		return apierrors.ErrUpdateApplication.InvalidParameter(err).ToResp(), nil
	}
	if r.Body == nil {
		return apierrors.ErrUpdateApplication.MissingParameter("body").ToResp(), nil
	}
	var applicationUpdateReq apistructs.ApplicationUpdateRequestBody
	if err := json.NewDecoder(r.Body).Decode(&applicationUpdateReq); err != nil {
		return apierrors.ErrUpdateApplication.InvalidParameter(err).ToResp(), nil
	}

	// Check permission
	req := apistructs.PermissionCheckRequest{
		UserID:   identity.UserID,
		Scope:    apistructs.AppScope,
		ScopeID:  uint64(applicationID),
		Resource: apistructs.AppResource,
		Action:   apistructs.UpdateAction,
	}
	if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
		return apierrors.ErrUpdateApplication.AccessDenied().ToResp(), nil
	}

	appDto, err := e.bdl.GetApp(uint64(applicationID))
	if err != nil {
		return apierrors.ErrUpdateApplication.InternalError(err).ToResp(), nil
	}
	if appDto.IsPublic != applicationUpdateReq.IsPublic {
		req = apistructs.PermissionCheckRequest{
			UserID:   identity.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  uint64(applicationID),
			Resource: apistructs.AppPublicResource,
			Action:   apistructs.UpdateAction,
		}
		if access, err := e.bdl.CheckPermission(&req); err != nil || !access.Access {
			return apierrors.ErrUpdateApplication.AccessDenied().ToResp(), nil
		}
	}

	if appDto.IsExternalRepo && applicationUpdateReq.RepoConfig != nil {
		err = e.bdl.UpdateRepo(apistructs.UpdateRepoRequest{
			AppID:  int64(appDto.ID),
			Config: applicationUpdateReq.RepoConfig,
		})
		if err != nil {
			return apierrors.ErrUpdateApplication.InternalError(err).ToResp(), nil
		}
	}

	_, err = e.bdl.UpdateApp(applicationUpdateReq, uint64(applicationID), identity.UserID)
	if err != nil {
		return apierrors.ErrUpdateApplication.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("update succ")
}
