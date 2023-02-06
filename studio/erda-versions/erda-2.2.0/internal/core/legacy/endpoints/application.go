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
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/conf"
	"github.com/erda-project/erda/internal/core/legacy/dao"
	"github.com/erda-project/erda/internal/core/legacy/model"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/filehelper"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// Private 私有
	Private = "private"
	// Public 公开
	Public = "public"
	// All 全部
	All = ""
)

// CreateApplication 创建应用
func (e *Endpoints) CreateApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	identify, err := user.GetIdentityInfo(r)
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

	// 参数合法性检查
	if err := checkApplicationCreateParam(applicationCreateReq); err != nil {
		return apierrors.ErrCreateApplication.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   identify.UserID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  applicationCreateReq.ProjectID,
		Resource: apistructs.AppResource,
		Action:   apistructs.CreateAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrCreateApplication.AccessDenied().ToResp(), nil
	}

	// 添加应用至DB
	app, err := e.app.CreateWithEvent(identify.UserID, &applicationCreateReq)
	if err != nil {
		return apierrors.ErrCreateApplication.InternalError(err).ToResp(), nil
	}

	// 更新项目活跃时间
	if err := e.project.UpdateProjectActiveTime(&apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  applicationCreateReq.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}

	applicationDTO := e.convertToApplicationDTO(ctx, *app, false, identify.UserID, nil)

	return httpserver.OkResp(applicationDTO)
}

// UpdateApplication 更新应用
func (e *Endpoints) UpdateApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateApplication.NotLogin().ToResp(), nil
	}

	// 检查applicationID合法性
	applicationID, err := strutil.Atoi64(vars["applicationID"])
	if err != nil {
		return apierrors.ErrUpdateApplication.InvalidParameter(err).ToResp(), nil
	}

	// 检查请求body
	if r.Body == nil {
		return apierrors.ErrUpdateApplication.MissingParameter("body").ToResp(), nil
	}
	var applicationUpdateReq apistructs.ApplicationUpdateRequestBody
	if err := json.NewDecoder(r.Body).Decode(&applicationUpdateReq); err != nil {
		return apierrors.ErrUpdateApplication.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  uint64(applicationID),
		Resource: apistructs.AppResource,
		Action:   apistructs.UpdateAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrUpdateApplication.AccessDenied().ToResp(), nil
	}

	application, err := e.app.Get(applicationID)
	if err != nil {
		return apierrors.ErrUpdateApplication.InternalError(err).ToResp(), nil
	}
	if application.IsPublic != applicationUpdateReq.IsPublic {
		// 公开应用操作鉴权
		req := apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.AppScope,
			ScopeID:  uint64(applicationID),
			Resource: apistructs.AppPublicResource,
			Action:   apistructs.UpdateAction,
		}
		if access, err := e.permission.CheckPermission(&req); err != nil || !access {
			return apierrors.ErrUpdateApplication.AccessDenied().ToResp(), nil
		}
	}
	// 更新应用信息至DB
	app, err := e.app.UpdateWithEvent(applicationID, &applicationUpdateReq)
	if err != nil {
		return apierrors.ErrUpdateApplication.InternalError(err).ToResp(), nil
	}

	// 更新项目活跃时间
	if err := e.project.UpdateProjectActiveTime(&apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  uint64(app.ProjectID),
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}

	return httpserver.OkResp("update succ")
}

// GetApplication 获取应用详情
func (e *Endpoints) GetApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 检查applicationID合法性
	applicationID, err := strutil.Atoi64(vars["applicationID"])
	if err != nil {
		return apierrors.ErrGetApplication.InvalidParameter(err).ToResp(), nil
	}

	var userID string
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		v, err := user.GetUserID(r)
		if err != nil {
			return apierrors.ErrGetApplication.NotLogin().ToResp(), nil
		}
		userID = v.String()
		// 操作鉴权
		req := apistructs.PermissionCheckRequest{
			UserID:   userID,
			Scope:    apistructs.AppScope,
			ScopeID:  uint64(applicationID),
			Resource: apistructs.AppResource,
			Action:   apistructs.GetAction,
		}
		if access, err := e.permission.CheckPermission(&req); err != nil || !access {
			return apierrors.ErrGetApplication.AccessDenied().ToResp(), nil
		}
	}

	app, err := e.app.Get(applicationID)
	if err != nil {
		return apierrors.ErrGetApplication.InternalError(err).ToResp(), nil
	}

	orgid_s := r.Header.Get("Org-ID")
	orgID, err := strconv.ParseInt(orgid_s, 10, 64)
	if err != nil {
		orgID = 0
	}
	approves, err := e.db.ListUnblockApplicationApprove(uint64(orgID))
	if err != nil {
		return apierrors.ErrListApplication.InternalError(err).ToResp(), nil
	}
	blockStatusMap := map[uint64]string{}
	for _, approve := range approves {
		extramap := map[string]string{}
		if err := json.Unmarshal([]byte(approve.Extra), &extramap); err != nil {
			continue
		}
		appidsStr := extramap["appIDs"]
		appids := strutil.Split(appidsStr, ",", true)
		status := ""
		if approve.Status == string(apistructs.ApprovalStatusPending) {
			status = "unblocking"
		}
		for _, id := range appids {
			id, err := strconv.ParseUint(id, 10, 64)
			if err != nil {
				continue
			}
			blockStatusMap[id] = status
		}
	}
	return httpserver.OkResp(e.convertToApplicationDTO(ctx, *app, true, userID, blockStatusMap), nil)
}

// DeleteApplication 删除应用
func (e *Endpoints) DeleteApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
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
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  uint64(applicationID),
		Resource: apistructs.AppResource,
		Action:   apistructs.DeleteAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		if dao.IsApplicationNotFoundError(err) {
			return apierrors.ErrDeleteApplication.NotFound().ToResp(), nil
		}
		return apierrors.ErrDeleteApplication.AccessDenied().ToResp(), nil
	}
	app, err := e.app.Get(applicationID)
	if err != nil {
		return apierrors.ErrDeleteApplication.InternalError(err).ToResp(), nil
	}
	applicationDTO := e.convertToApplicationDTO(ctx, *app, false, userID.String(), nil)

	// 删除应用
	if err = e.app.DeleteWithEvent(applicationID); err != nil {
		return apierrors.ErrDeleteApplication.InternalError(err).ToResp(), nil
	}

	// 更新项目活跃时间
	if err := e.project.UpdateProjectActiveTime(&apistructs.ProjectActiveTimeUpdateRequest{
		ProjectID:  applicationDTO.ProjectID,
		ActiveTime: time.Now(),
	}); err != nil {
		logrus.Errorf("update project active time err: %v", err)
	}

	return httpserver.OkResp(applicationDTO)
}

// ListApplication 所有应用列表
func (e *Endpoints) ListApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	return e.listApplications(ctx, r, false)
}

// ListMyApplication 我的应用列表
func (e *Endpoints) ListMyApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	return e.listApplications(ctx, r, true)
}

func (e *Endpoints) listApplications(ctx context.Context, r *http.Request, isMine bool) (httpserver.Responser, error) {
	// 获取当前用户
	var (
		total        int
		applications []model.Application
		err          error
	)

	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListApplication.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrListApplication.MissingParameter("org id").ToResp(), nil
	}
	orgID, err := strutil.Atoi64(orgIDStr)
	if err != nil {
		return apierrors.ErrListApplication.InvalidParameter(err).ToResp(), nil
	}

	// 获取请求参数
	params, err := getListApplicationsParam(e, r)
	if err != nil {
		return apierrors.ErrListApplication.InvalidParameter(err).ToResp(), nil
	}

	if isMine {
		total, applications, err = e.app.ListMyApplications(orgID, userID.String(), params)
	} else {
		// 获取当前用户
		identityInfo, err := user.GetIdentityInfo(r)
		if err != nil {
			return apierrors.ErrInitApplication.NotLogin().ToResp(), nil
		}
		if !identityInfo.IsInternalClient() {
			// 操作鉴权
			req := apistructs.PermissionCheckRequest{
				UserID:   identityInfo.UserID,
				Scope:    apistructs.ProjectScope,
				ScopeID:  params.ProjectID,
				Resource: apistructs.ProjectResource,
				Action:   apistructs.GetAction,
			}
			if access, err := e.permission.CheckPermission(&req); err != nil || !access {
				return apierrors.ErrInitApplication.AccessDenied().ToResp(), nil
			}
		}
		total, applications, err = e.app.List(orgID, int64(params.ProjectID), userID.String(), params)
	}

	if err != nil {
		return apierrors.ErrListApplication.InternalError(err).ToResp(), nil
	}

	// 获取项目下成员对应应用的角色
	memberMap := make(map[int64][]string, 0)

	memberID := r.URL.Query().Get("memberID")
	if memberID != "" {
		// 查找成员项目下有权限的列表
		members, err := e.db.GetMembersByParentID(apistructs.AppScope, int64(params.ProjectID), memberID)
		if err != nil {
			return nil, errors.Errorf("failed to get member list under project, (%v)", err)
		}

		for _, m := range members {
			if m.ResourceKey == apistructs.RoleResourceKey {
				memberMap[m.ScopeID] = append(memberMap[m.ScopeID], m.ResourceValue)
			}
		}
	}

	approves, err := e.db.ListUnblockApplicationApprove(uint64(orgID))
	if err != nil {
		return apierrors.ErrListApplication.InternalError(err).ToResp(), nil
	}
	blockStatusMap := map[uint64]string{}
	for _, approve := range approves {
		extramap := map[string]string{}
		if err := json.Unmarshal([]byte(approve.Extra), &extramap); err != nil {
			continue
		}
		appids_str := extramap["appIDs"]
		appids := strutil.Split(appids_str, ",", true)
		status := ""
		if approve.Status == string(apistructs.ApprovalStatusPending) {
			status = "unblocking"
		}
		for _, id_s := range appids {
			id, err := strconv.ParseUint(id_s, 10, 64)
			if err != nil {
				continue
			}
			blockStatusMap[id] = status
		}
	}

	// 转换成所需格式
	applicationDTOs, err := e.transferAppsToApplicationDTOS(params.IsSimple, applications, blockStatusMap, memberMap)
	if err != nil {
		return apierrors.ErrInitApplication.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(apistructs.ApplicationListResponseData{Total: total, List: applicationDTOs})
}

func (e Endpoints) transferAppsToApplicationDTOS(isSimple bool, applications []model.Application, blockStatusMap map[uint64]string, memberMap map[int64][]string) ([]apistructs.ApplicationDTO, error) {
	projectIDs := make([]uint64, 0, len(applications))
	appIDS := make([]int64, 0)
	orgSet := make(map[int64]struct{})
	for _, app := range applications {
		projectIDs = append(projectIDs, uint64(app.ProjectID))
		orgSet[app.OrgID] = struct{}{}
		appIDS = append(appIDS, app.ID)
	}
	orgIDS := make([]int64, 0)
	for orgID := range orgSet {
		orgIDS = append(orgIDS, orgID)
	}

	_, orgs, err := e.org.ListOrgs(orgIDS, &apistructs.OrgSearchRequest{PageSize: 999, PageNo: 1}, false)
	if err != nil {
		return nil, err
	}
	orgMap := make(map[int64]model.Org, len(orgs))
	for _, org := range orgs {
		orgMap[org.ID] = org
	}

	projectMap, err := e.project.GetModelProjectsMap(projectIDs, false)
	if err != nil {
		return nil, err
	}

	appRuntimeCounts, err := e.db.GetRuntimeCountByAppIDS(appIDS)
	if err != nil {
		return nil, err
	}
	runtimeCounter := make(map[int64]model.ApplicationRuntimeCount)
	for _, appRuntimeCount := range appRuntimeCounts {
		runtimeCounter[appRuntimeCount.ApplicationID] = appRuntimeCount
	}

	pinedAppDTOs := make([]apistructs.ApplicationDTO, 0, 10)
	unpinedAppDTOs := make([]apistructs.ApplicationDTO, 0, len(applications))
	for i := range applications {
		var projectName string
		var projectDisplayName string
		var project *model.Project
		if v, ok := projectMap[applications[i].ProjectID]; ok {
			projectName = v.Name
			projectDisplayName = v.DisplayName
			project = v
		}

		appDTO := apistructs.ApplicationDTO{
			Pined:              applications[i].Pined,
			Name:               applications[i].Name,
			Desc:               applications[i].Desc,
			Creator:            applications[i].UserID,
			CreatedAt:          applications[i].CreatedAt,
			UpdatedAt:          applications[i].UpdatedAt,
			ID:                 uint64(applications[i].ID),
			DisplayName:        applications[i].DisplayName,
			IsPublic:           applications[i].IsPublic,
			ProjectID:          uint64(applications[i].ProjectID),
			ProjectName:        projectName,
			ProjectDisplayName: projectDisplayName,
		}
		if isSimple {
			if appDTO.Pined {
				pinedAppDTOs = append(pinedAppDTOs, appDTO)
			} else {
				unpinedAppDTOs = append(unpinedAppDTOs, appDTO)
			}
			continue
		}
		var config map[string]interface{}
		if err := json.Unmarshal([]byte(applications[i].Config), &config); err != nil {
			config = make(map[string]interface{})
		}

		var extra map[string]string
		if err := json.Unmarshal([]byte(applications[i].Extra), &extra); err != nil {
			extra = make(map[string]string)
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
					workspace.ClusterName = project.GetClusterConfig()[env]
				}
				workspaces = append(workspaces, workspace)
			}
		}

		org := orgMap[applications[i].OrgID]
		var runtimeCount uint
		if appRuntimeCount, ok := runtimeCounter[applications[i].ID]; ok {
			runtimeCount = uint(appRuntimeCount.RuntimeCount)
		}
		gitRepo := strutil.Concat(conf.GittarOutterURL(), "/", applications[i].GitRepoAbbrev)

		gitRepoNew := strutil.Concat(conf.UIDomain(), "/", org.Name, "/dop/", applications[i].ProjectName, "/", applications[i].Name)

		var repoConfig apistructs.GitRepoConfig
		if applications[i].IsExternalRepo {
			json.Unmarshal([]byte(applications[i].RepoConfig), &repoConfig)
			repoConfig.Password = ""
			repoConfig.Username = ""
		}

		isOrgBlocked := false
		blockStatus := ""
		if org.BlockoutConfig.BlockDEV ||
			org.BlockoutConfig.BlockTEST ||
			org.BlockoutConfig.BlockStage ||
			org.BlockoutConfig.BlockProd {
			isOrgBlocked = true
			blockStatus = "blocked"
		}

		now := time.Now()
		if applications[i].UnblockStart != nil && applications[i].UnblockEnd != nil &&
			now.Before(*applications[i].UnblockEnd) && now.After(*applications[i].UnblockStart) {
			blockStatus = "unblocked"
		} else if len(blockStatusMap) > 0 && blockStatusMap[uint64(applications[i].ID)] != "" {
			blockStatus = blockStatusMap[uint64(applications[i].ID)]
		}
		var unblockStart *time.Time
		var unblockEnd *time.Time

		if applications[i].UnblockStart != nil && applications[i].UnblockEnd != nil &&
			now.Before(*applications[i].UnblockEnd) {
			// 过期之后的情况不返回时间
			unblockStart = applications[i].UnblockStart
			unblockEnd = applications[i].UnblockEnd
		}

		appDTO = apistructs.ApplicationDTO{
			ID:             uint64(applications[i].ID),
			Name:           applications[i].Name,
			DisplayName:    applications[i].DisplayName,
			Desc:           applications[i].Desc,
			Logo:           filehelper.APIFileUrlRetriever(applications[i].Logo),
			Config:         config,
			UnBlockStart:   map[bool]*time.Time{true: unblockStart, false: nil}[isOrgBlocked],
			UnBlockEnd:     map[bool]*time.Time{true: unblockEnd, false: nil}[isOrgBlocked],
			BlockStatus:    map[bool]string{true: blockStatus, false: ""}[isOrgBlocked],
			ProjectID:      uint64(applications[i].ProjectID),
			ProjectName:    applications[i].ProjectName,
			OrgID:          uint64(applications[i].OrgID),
			OrgName:        org.Name,
			OrgDisplayName: org.DisplayName,
			Mode:           applications[i].Mode,
			Pined:          applications[i].Pined,
			IsPublic:       applications[i].IsPublic,
			Creator:        applications[i].UserID,
			GitRepo:        gitRepo,
			SonarConfig:    applications[i].SonarConfig,
			GitRepoAbbrev:  applications[i].GitRepoAbbrev,
			GitRepoNew:     gitRepoNew,
			Workspaces:     workspaces,
			Stats: apistructs.ApplicationStats{
				CountRuntimes: runtimeCount,
			},
			IsExternalRepo: applications[i].IsExternalRepo,
			RepoConfig:     &repoConfig,
			CreatedAt:      applications[i].CreatedAt,
			UpdatedAt:      applications[i].UpdatedAt,
			Extra:          applications[i].Extra,
		}
		// 填充成员角色
		if roles, ok := memberMap[int64(appDTO.ID)]; ok {
			appDTO.MemberRoles = roles
		}

		appDTO.ProjectName = projectName
		appDTO.ProjectDisplayName = projectDisplayName
		if appDTO.Pined {
			pinedAppDTOs = append(pinedAppDTOs, appDTO)
		} else {
			unpinedAppDTOs = append(unpinedAppDTOs, appDTO)
		}
	}
	applicationDTOs := make([]apistructs.ApplicationDTO, 0, len(applications))
	applicationDTOs = append(pinedAppDTOs, unpinedAppDTOs...)
	return applicationDTOs, nil
}

// PinApplication  pin应用
func (e Endpoints) PinApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrPinApplication.NotLogin().ToResp(), nil
	}

	// 检查applicationID合法性
	applicationID, err := strutil.Atoi64(vars["applicationID"])
	if err != nil {
		return apierrors.ErrPinApplication.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  uint64(applicationID),
		Resource: apistructs.AppResource,
		Action:   apistructs.GetAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrPinApplication.AccessDenied().ToResp(), nil
	}

	if err := e.app.Pin(applicationID, userID.String()); err != nil {
		return apierrors.ErrPinApplication.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("pin succ")
}

// UnPinApplication  unpin应用
func (e Endpoints) UnPinApplication(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUnPinApplication.NotLogin().ToResp(), nil
	}

	// 检查applicationID合法性
	applicationID, err := strutil.Atoi64(vars["applicationID"])
	if err != nil {
		return apierrors.ErrUnPinApplication.InvalidParameter(err).ToResp(), nil
	}

	// 操作鉴权
	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  uint64(applicationID),
		Resource: apistructs.AppResource,
		Action:   apistructs.GetAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrUnPinApplication.AccessDenied().ToResp(), nil
	}

	if err := e.app.UnPin(applicationID, userID.String()); err != nil {
		return apierrors.ErrUnPinApplication.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("unpin succ")
}

// ListAppTemplates 列出应用模板
func (e Endpoints) ListAppTemplates(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	appMode := r.URL.Query().Get("mode")

	var templates []string
	switch appMode {
	case string(apistructs.ApplicationModeMobile):
		templates = append(templates, "terminusMobileTemplates")
	case "":
		return apierrors.ErrListAppTemplates.MissingParameter("mode").ToResp(), nil
	}

	return httpserver.OkResp(templates)
}

// GetAppIDByNames 根据应用名称批量获取应用ID
func (e Endpoints) GetAppIDByNames(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetAppIDByNames.NotLogin().ToResp(), nil
	}

	projectIDStr := r.URL.Query().Get("projectID")
	if projectIDStr == "" {
		return apierrors.ErrGetAppIDByNames.MissingParameter("projectID").ToResp(), nil
	}
	projectID, err := strconv.ParseUint(projectIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetAppIDByNames.InvalidParameter("projectID").ToResp(), nil
	}

	req := apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.ProjectScope,
		ScopeID:  projectID,
		Resource: apistructs.ProjectResource,
		Action:   apistructs.GetAction,
	}
	if access, err := e.permission.CheckPermission(&req); err != nil || !access {
		return apierrors.ErrGetAppIDByNames.AccessDenied().ToResp(), nil
	}

	names := r.URL.Query()["name"]
	if len(names) == 0 {
		return apierrors.ErrGetAppIDByNames.MissingParameter("name").ToResp(), nil
	}
	apps, err := e.app.GetApplicationsByNames(projectID, names)
	if err != nil {
		return apierrors.ErrGetAppIDByNames.InternalError(err).ToResp(), nil
	}

	resp := apistructs.GetAppIDByNamesResponseData{AppNameToID: map[string]int64{}}
	for i := 0; i < len(apps); i++ {
		resp.AppNameToID[apps[i].Name] = apps[i].ID
	}

	return httpserver.OkResp(resp)
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

// 应用列表时获取请求参数
func getListApplicationsParam(e *Endpoints, r *http.Request) (*apistructs.ApplicationListRequest, error) {
	var listReq apistructs.ApplicationListRequest
	if err := e.queryStringDecoder.Decode(&listReq, r.URL.Query()); err != nil {
		return nil, errors.Errorf("decode appplication list request failed, error: %v", err)
	}
	mode := r.URL.Query().Get("mode")
	if mode != "" {
		err := apistructs.ApplicationMode(mode).CheckAppMode()
		if err != nil {
			return nil, err
		}
	}

	// 获取pageSize
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, errors.Errorf("invalid request, pageSize is invalid")
	}

	// 获取pageNo
	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, errors.Errorf("invalid request, pageNo is invalid")
	}

	// 获取项目Id
	projectIDStr := r.URL.Query().Get("projectId")
	var projectID int64
	if projectIDStr != "" {
		projectID, err = strconv.ParseInt(projectIDStr, 10, 64)
		if err != nil {
			return nil, errors.Errorf("invalid request, projectId is invalid")
		}
	}

	// 按应用名称搜索
	keyword := r.URL.Query().Get("q")

	public := r.URL.Query().Get("public")
	if public != All && public != Public && public != Private {
		return nil, errors.Errorf("invalid request, public is invalid")
	}

	// 是否只返回简单信息
	isSimple := false
	if r.URL.Query().Get("isSimple") == "true" {
		isSimple = true
	}

	orderBy := r.URL.Query().Get("orderBy")

	req := &apistructs.ApplicationListRequest{
		ProjectID: uint64(projectID),
		Mode:      mode,
		Query:     keyword,
		Name:      r.URL.Query().Get("name"),
		PageNo:    pageNo,
		PageSize:  pageSize,
		Public:    public,
		IsSimple:  isSimple,
		OrderBy:   orderBy,

		ApplicationID: listReq.ApplicationID,
	}

	return req, nil
}

func (e *Endpoints) checkPermission(r *http.Request, scopeType apistructs.ScopeType, scopeID int64) error {
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient != "" { // 内部服务调用
		return nil
	}

	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return err
	}

	// 操作鉴权
	if e.member.IsAdmin(userID.String()) { // 系统管理员
		return nil
	}
	if permission, err := e.getPermission(userID.String(), scopeType, scopeID); err != nil ||
		!permission.Access {
		return errors.Errorf("failed to get permission")
	}

	return nil
}

func (e *Endpoints) convertToApplicationDTO(ctx context.Context, application model.Application, withProjectToken bool, userID string, blockStatusMap map[uint64]string) apistructs.ApplicationDTO {
	var config map[string]interface{}
	if err := json.Unmarshal([]byte(application.Config), &config); err != nil {
		config = make(map[string]interface{})
	}

	var extra map[string]string
	if err := json.Unmarshal([]byte(application.Extra), &extra); err != nil {
		extra = make(map[string]string)
	}

	token := ""
	if withProjectToken {
		res, err := e.tokenService.QueryTokens(ctx, &tokenpb.QueryTokensRequest{
			Scope:     string(apistructs.OrgScope),
			ScopeId:   strconv.FormatInt(application.OrgID, 10),
			Type:      mysqltokenstore.PAT.String(),
			CreatorId: userID,
		})
		if err == nil && res != nil && res.Total > 0 {
			token = res.Data[0].AccessKey
		}
	}

	// TODO ApplicationDTO 去除clusterName, 暂时兼容添加
	project, err := e.project.Get(ctx, application.ProjectID, false)
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

	// TODO ApplicationDTO 去除orgName，暂时兼容添加
	var orgName string
	var orgDisplayName string
	org, err := e.org.Get(application.OrgID)
	if err == nil {
		orgName = org.Name
		orgDisplayName = org.DisplayName
	} else {
		logrus.Error(err)
	}

	// TODO 应用表新增reference字段，提供API供orchestrator调用
	var runtimeCount uint
	e.db.Table("ps_v2_project_runtimes").Where("application_id = ?", application.ID).Count(&runtimeCount)

	gitRepo := strutil.Concat(conf.GittarOutterURL(), "/", application.GitRepoAbbrev)

	gitRepoNew := strutil.Concat(conf.UIDomain(), "/", orgName, "/dop/", application.ProjectName, "/", application.Name)

	var repoConfig apistructs.GitRepoConfig
	if application.IsExternalRepo {
		json.Unmarshal([]byte(application.RepoConfig), &repoConfig)
		repoConfig.Password = ""
		repoConfig.Username = ""
	}

	isOrgBlocked := false
	blockStatus := ""
	if org != nil && (org.BlockoutConfig.BlockDEV ||
		org.BlockoutConfig.BlockTEST ||
		org.BlockoutConfig.BlockStage ||
		org.BlockoutConfig.BlockProd) {
		isOrgBlocked = true
		blockStatus = "blocked"
	}
	now := time.Now()
	if application.UnblockStart != nil && application.UnblockEnd != nil &&
		now.Before(*application.UnblockEnd) && now.After(*application.UnblockStart) {
		blockStatus = "unblocked"
	} else if len(blockStatusMap) > 0 && blockStatusMap[uint64(application.ID)] != "" {
		blockStatus = blockStatusMap[uint64(application.ID)]
	}
	var unblockStart *time.Time
	var unblockEnd *time.Time

	if application.UnblockStart != nil && application.UnblockEnd != nil &&
		now.Before(*application.UnblockEnd) {
		// 过期之后的情况不返回时间
		unblockStart = application.UnblockStart
		unblockEnd = application.UnblockEnd
	}

	return apistructs.ApplicationDTO{
		ID:             uint64(application.ID),
		Name:           application.Name,
		DisplayName:    application.DisplayName,
		Desc:           application.Desc,
		Logo:           filehelper.APIFileUrlRetriever(application.Logo),
		Config:         config,
		UnBlockStart:   map[bool]*time.Time{true: unblockStart, false: nil}[isOrgBlocked],
		UnBlockEnd:     map[bool]*time.Time{true: unblockEnd, false: nil}[isOrgBlocked],
		BlockStatus:    map[bool]string{true: blockStatus, false: ""}[isOrgBlocked],
		ProjectID:      uint64(application.ProjectID),
		ProjectName:    application.ProjectName,
		OrgID:          uint64(application.OrgID),
		OrgName:        orgName,
		OrgDisplayName: orgDisplayName,
		Mode:           application.Mode,
		Pined:          application.Pined,
		IsPublic:       application.IsPublic,
		Creator:        application.UserID,
		GitRepo:        gitRepo,
		SonarConfig:    application.SonarConfig,
		GitRepoAbbrev:  application.GitRepoAbbrev,
		GitRepoNew:     gitRepoNew,
		Token:          token,
		Workspaces:     workspaces,
		Stats: apistructs.ApplicationStats{
			CountRuntimes: runtimeCount,
		},
		IsExternalRepo: application.IsExternalRepo,
		RepoConfig:     &repoConfig,
		CreatedAt:      application.CreatedAt,
		UpdatedAt:      application.UpdatedAt,
		Extra:          application.Extra,
	}
}

// CountAppByProID count app by proID
func (e Endpoints) CountAppByProID(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCountApplication.NotLogin().ToResp(), nil
	}
	projectIDStr := r.URL.Query().Get("projectID")

	projectID, err := strconv.ParseInt(projectIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCountApplication.InvalidParameter(err).ToResp(), nil
	}
	count, err := e.db.GetApplicationCountByProjectID(projectID)
	if err != nil {
		return apierrors.ErrCountApplication.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(count)
}
