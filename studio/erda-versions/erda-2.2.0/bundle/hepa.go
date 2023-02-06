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
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// 创建流量入口
func (b *Bundle) CreateEndpoint(orgID, userID, projectID string, workspace string, packageDto apistructs.PackageDto) (endpointID string, err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.EndpointInfoResponse
	resp, err := b.hc.Post(host).
		Path("/api/gateway/openapi/packages").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Param("orgId", orgID).
		Param("projectId", projectID).
		Param("env", workspace).
		JSONBody(packageDto).Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	endpointID = fetchResp.Data.Id
	return
}

// 更新流量入口
func (b *Bundle) UpdateEndpoint(orgID, userID, endpointID string, packageDto apistructs.PackageDto) (err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.Header
	resp, err := b.hc.Patch(host).
		Path(fmt.Sprintf("/api/gateway/openapi/packages/%s", endpointID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		JSONBody(packageDto).Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	return
}

// 删除流量入口
func (b *Bundle) DeleteEndpoint(orgID, userID, endpointID string) (err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.Header
	resp, err := b.hc.Delete(host).
		Path(fmt.Sprintf("/api/gateway/openapi/packages/%s", endpointID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	return
}

// 获取流量入口详情
func (b *Bundle) GetEndpoint(orgID, userID, endpointID string) (dto *apistructs.PackageInfoDto, err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.EndpointInfoResponse
	resp, err := b.hc.Get(host).
		Path(fmt.Sprintf("/api/gateway/openapi/packages/%s", endpointID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	dto = &fetchResp.Data
	return
}

// 创建或更新路由规则
func (b *Bundle) CreateOrUpdateEndpointRootRoute(orgID, userID, endpointID, redirectAddr, redirectPath string) (err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.Header
	resp, err := b.hc.Put(host).
		Path(fmt.Sprintf("/api/gateway/openapi/packages/%s/root-api", endpointID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		JSONBody(apistructs.OpenapiDto{
			RedirectAddr: redirectAddr,
			RedirectPath: redirectPath,
		}).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	return
}

// 创建调用方
func (b *Bundle) CreateClientConsumer(orgID, userID string, clientName string) (dto *apistructs.ClientInfoDto, err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.ClientInfoResponse
	resp, err := b.hc.Post(host).
		Path("/api/gateway/openapi/clients").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Param("orgId", orgID).
		Param("clientName", clientName).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	dto = &fetchResp.Data
	return
}

// 删除调用方
func (b *Bundle) DeleteClientConsumer(orgID, userID, clientID string) (err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.Header
	resp, err := b.hc.Delete(host).
		Path(fmt.Sprintf("/api/gateway/openapi/clients/%s", clientID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	return
}

// 获取调用方凭证信息
func (b *Bundle) GetClientCredentials(orgID, userID, clientID string) (dto *apistructs.ClientInfoDto, err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.ClientInfoResponse
	resp, err := b.hc.Get(host).
		Path(fmt.Sprintf("/api/gateway/openapi/clients/%s/credentials", clientID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	dto = &fetchResp.Data
	return
}

// 重置调用方密钥
func (b *Bundle) ResetClientCredentials(orgID, userID, clientID string) (dto *apistructs.ClientInfoDto, err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.ClientInfoResponse
	resp, err := b.hc.Patch(host).
		Path(fmt.Sprintf("/api/gateway/openapi/clients/%s/credentials", clientID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	dto = &fetchResp.Data
	return
}

// 授权调用方流量入口权限
func (b *Bundle) GrantEndpointToClient(orgID, userID, clientID, endpointID string) (err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.Header
	resp, err := b.hc.Post(host).
		Path(fmt.Sprintf("/api/gateway/openapi/clients/%s/packages/%s", clientID, endpointID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	return
}

// 收回调用方流量入口权限
func (b *Bundle) RevokeEndpointFromClient(orgID, userID, clientID, endpointID string) (err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.Header
	resp, err := b.hc.Delete(host).
		Path(fmt.Sprintf("/api/gateway/openapi/clients/%s/packages/%s", clientID, endpointID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	return
}

// 获取tenant-group id
func (b *Bundle) GetTenantGroupID(orgID, userID, projectID, workspace string) (id string, err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.TenantGroupResponse
	resp, err := b.hc.Get(host).
		Path("/api/gateway/tenant-group").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		Param("projectId", projectID).
		Param("env", workspace).
		Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	id = fetchResp.Data
	return
}

// 创建或更新限流规则
func (b *Bundle) CreateOrUpdateClientLimits(orgID, userID, clientID, endpointID string, limits []apistructs.LimitType) (err error) {
	host, err := b.urls.Hepa()
	if err != nil {
		return
	}
	var fetchResp apistructs.Header
	resp, err := b.hc.Put(host).
		Path(fmt.Sprintf("/api/gateway/openapi/clients/%s/packages/%s/limits", clientID, endpointID)).
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, orgID).
		Header(httputil.UserHeader, userID).
		JSONBody(apistructs.ChangeLimitsReq{Limits: limits}).Do().JSON(&fetchResp)
	if err != nil {
		err = apierrors.ErrInvoke.InternalError(err)
		return
	}
	if !resp.IsOK() || !fetchResp.Success {
		err = toAPIError(resp.StatusCode(), fetchResp.Error)
		return
	}
	return
}
