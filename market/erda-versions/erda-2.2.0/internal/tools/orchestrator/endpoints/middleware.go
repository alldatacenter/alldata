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
	"net/http"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// ListMiddlewares 获取 addon 真实实例列表
func (e *Endpoints) ListMiddleware(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrListAddon.NotLogin().ToResp(), nil
	}
	orgID, _ := strconv.ParseUint(orgIDStr, 10, 64)

	// 鉴权
	permissionResult, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  orgID,
		Resource: "middleware",
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, err
	}
	if !permissionResult.Access {
		return apierrors.ErrListAddon.AccessDenied().ToResp(), nil
	}

	params, err := e.getMiddlewareListParams(r)
	if err != nil {
		return apierrors.ErrListAddon.InvalidParameter(err).ToResp(), nil
	}

	// 获取 middleware 列表
	middlewares, err := e.addon.ListMiddleware(orgID, params)
	if err != nil {
		return apierrors.ErrListAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(middlewares)
}

// GetMiddleware 获取 middleware 详情
func (e *Endpoints) GetMiddleware(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrFetchAddon.MissingParameter("orgId").ToResp(), nil
	}
	orgID, _ := strconv.ParseUint(orgIDStr, 10, 64)
	// 鉴权
	permissionResult, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgID),
		Resource: "middleware",
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, err
	}
	if !permissionResult.Access {
		return apierrors.ErrListAddon.AccessDenied().ToResp(), nil
	}
	// 查询middleware信息
	middleware, err := e.addon.GetMiddleware(orgID, userID.String(), vars["middlewareID"])
	if err != nil {
		return apierrors.ErrFetchAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(middleware)
}

// GetMiddlewareAddonClassification 获取 middleware addon分类资源占用
func (e *Endpoints) GetMiddlewareAddonClassification(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	logrus.Info("start GetMiddlewareAddonClassification")
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrFetchAddon.MissingParameter("orgId").ToResp(), nil
	}
	orgID, _ := strconv.ParseUint(orgIDStr, 10, 64)
	// 鉴权
	permissionResult, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgID),
		Resource: "middleware",
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, err
	}
	if !permissionResult.Access {
		return apierrors.ErrListAddon.AccessDenied().ToResp(), nil
	}

	params, err := e.getMiddlewareListParams(r)
	if err != nil {
		return apierrors.ErrListAddon.InvalidParameter(err).ToResp(), nil
	}

	// 查询middleware信息
	middleware, err := e.addon.GetMiddlewareAddonClassification(orgID, params)
	if err != nil {
		return apierrors.ErrFetchAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(middleware)
}

// GetMiddlewareAddonDaily 获取 middleware 每日addon资源占用
func (e *Endpoints) GetMiddlewareAddonDaily(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrFetchAddon.MissingParameter("orgId").ToResp(), nil
	}
	orgID, _ := strconv.ParseUint(orgIDStr, 10, 64)
	// 鉴权
	permissionResult, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgID),
		Resource: "middleware",
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, err
	}
	if !permissionResult.Access {
		return apierrors.ErrListAddon.AccessDenied().ToResp(), nil
	}

	params, err := e.getMiddlewareListParams(r)
	if err != nil {
		return apierrors.ErrListAddon.InvalidParameter(err).ToResp(), nil
	}

	// 查询middleware信息
	middleware, err := e.addon.GetMiddlewareAddonDaily(orgID, params)
	if err != nil {
		return apierrors.ErrFetchAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(middleware)
}

// InnerGetMiddleware 内部获取middleware详情
func (e *Endpoints) InnerGetMiddleware(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 查询middleware信息
	middleware, err := e.addon.InnerGetMiddleware(vars["middlewareID"])
	if err != nil {
		return apierrors.ErrFetchAddon.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(middleware)
}

// GetMiddlewareResource 获取 middleware 资源详情
func (e *Endpoints) GetMiddlewareResource(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}
	orgID, _ := strconv.ParseUint(orgIDStr, 10, 64)
	// 鉴权
	permissionResult, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.OrgScope,
		ScopeID:  uint64(orgID),
		Resource: "middleware",
		Action:   apistructs.GetAction,
	})
	if err != nil {
		return nil, err
	}
	if !permissionResult.Access {
		return apierrors.ErrListAddon.AccessDenied().ToResp(), nil
	}
	// 查询middleware resource信息
	middlewareResource, err := e.addon.GetMiddlewareResource(vars["middlewareID"])
	if err != nil {
		return apierrors.ErrFetchAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(middlewareResource)
}

func (e *Endpoints) getMiddlewareListParams(r *http.Request) (*apistructs.MiddlewareListRequest, error) {
	var (
		projectID uint64
		err       error
	)
	projectIDStr := r.URL.Query().Get("projectId")
	if projectIDStr != "" {
		projectID, err = strconv.ParseUint(projectIDStr, 10, 64)
		if err != nil {
			return nil, err
		}
	}
	addonName := r.URL.Query().Get("addonName")
	workspace := r.URL.Query().Get("workspace")
	switch strings.ToUpper(workspace) {
	case "", string(apistructs.DevWorkspace), string(apistructs.TestWorkspace), string(apistructs.StagingWorkspace),
		string(apistructs.ProdWorkspace):
	default:
		return nil, errors.Errorf("invalid workspace")
	}

	// 获取pageNo
	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, err
	}

	// 获取pageSize
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, err
	}

	instanceID := r.URL.Query().Get("instanceId")
	instanceIP := r.URL.Query().Get("ip")
	return &apistructs.MiddlewareListRequest{
		ProjectID:  projectID,
		AddonName:  addonName,
		Workspace:  workspace,
		PageNo:     pageNo,
		PageSize:   pageSize,
		InstanceID: instanceID,
		InstanceIP: instanceIP,
	}, nil
}
