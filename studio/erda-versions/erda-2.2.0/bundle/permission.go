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

package bundle

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// CheckPermission 鉴权
func (b *Bundle) CheckPermission(req *apistructs.PermissionCheckRequest) (*apistructs.PermissionCheckResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var permissionResp apistructs.PermissionCheckResponse
	resp, err := hc.Post(host).Path("/api/permissions/actions/check").
		JSONBody(req).
		Do().JSON(&permissionResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !permissionResp.Success {
		return nil, toAPIError(resp.StatusCode(), permissionResp.Error)
	}

	return &permissionResp.Data, nil
}

// StateCheckPermission 鉴权
func (b *Bundle) StateCheckPermission(req *apistructs.PermissionCheckRequest) (*apistructs.StatePermissionCheckResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var permissionResp apistructs.StatePermissionCheckResponse
	resp, err := hc.Post(host).Path("/api/permissions/actions/stateCheck").
		JSONBody(req).
		Do().JSON(&permissionResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !permissionResp.Success {
		return nil, toAPIError(resp.StatusCode(), permissionResp.Error)
	}

	return &permissionResp.Data, nil
}

// ScopeRoleAccess 查询给定用户是否有相应权限
func (b *Bundle) ScopeRoleAccess(userID string, req *apistructs.ScopeRoleAccessRequest) (*apistructs.ScopeRole, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var permissionResp apistructs.ScopeRoleAccessResponse
	resp, err := hc.Post(host).Path("/api/permissions/actions/access").
		Header(httputil.UserHeader, userID).
		JSONBody(req).
		Do().JSON(&permissionResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !permissionResp.Success {
		return nil, toAPIError(resp.StatusCode(), permissionResp.Error)
	}

	return &permissionResp.Data, nil
}

func (b *Bundle) ScopeRoleAccessList(userID string, req *apistructs.ScopeRoleAccessRequest) (*apistructs.PermissionList, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var permissionListResp apistructs.PermissionListResponse
	resp, err := hc.Post(host).Path("/api/permissions/actions/access").
		Header(httputil.UserHeader, userID).
		JSONBody(req).
		Do().JSON(&permissionListResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !permissionListResp.Success {
		return nil, toAPIError(resp.StatusCode(), permissionListResp.Error)
	}
	return &permissionListResp.Data, nil
}

// ListScopeRole 获取给定用户所有角色权限
func (b *Bundle) ListScopeRole(userID, orgID string) (*apistructs.ScopeRoleList, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var permissionResp apistructs.ScopeRoleListResponse
	resp, err := hc.Get(host).Path("/api/permissions").
		Header(httputil.UserHeader, userID).
		Header(httputil.OrgHeader, orgID).
		Do().JSON(&permissionResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !permissionResp.Success {
		return nil, toAPIError(resp.StatusCode(), permissionResp.Error)
	}

	return &permissionResp.Data, nil
}
