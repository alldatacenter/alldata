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
	"strconv"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// GetNotifyGroupDetail 查询通知组详情
func (b *Bundle) GetNotifyGroupDetail(id int64, orgID int64, userID string) (*apistructs.NotifyGroupDetail, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var getResp apistructs.GetNotifyGroupDetailResponse
	resp, err := hc.Get(host).Path(strutil.Concat("/api/notify-groups/", strconv.FormatInt(id, 10), "/detail")).
		Header("Org-ID", strconv.FormatInt(orgID, 10)).
		Header("User-ID", userID).
		Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return &getResp.Data, nil
}

func (b *Bundle) CreateNotifyGroup(orgID string, userID string, request *apistructs.CreateNotifyGroupRequest) (*apistructs.NotifyGroup, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var getResp apistructs.CreateNotifyGroupResponse
	resp, err := hc.Post(host).Path("/api/notify-groups").
		Header("Org-ID", orgID).
		Header("User-ID", userID).JSONBody(request).Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return &getResp.Data, nil
}

func (b *Bundle) QueryNotifyGroup(orgID string, request *apistructs.QueryNotifyGroupRequest) (*apistructs.QueryNotifyGroupResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var getResp apistructs.QueryNotifyGroupResponse
	resp, err := hc.Get(host).Path("/api/notify-groups").
		Param("scopeType", request.ScopeType).
		Param("scopeId", request.ScopeID).
		Param("label", request.Label).
		Param("name", request.Name).
		Param("pageNo", strconv.Itoa(int(request.PageNo))).
		Param("pageSize", strconv.Itoa(int(request.PageSize))).
		Header("Org-ID", orgID).Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return &getResp, nil
}

func (b *Bundle) GetNotifyGroup(id int64, orgID string) (*apistructs.GetNotifyGroupResponse, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var getResp apistructs.GetNotifyGroupResponse
	resp, err := hc.Get(host).Path(strutil.Concat("/api/notify-groups/", strconv.FormatInt(id, 10))).
		Header("Org-ID", orgID).Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return &getResp, nil
}

func (b *Bundle) UpdateNotifyGroup(id int64, orgID string, request *apistructs.UpdateNotifyGroupRequest) (*apistructs.NotifyGroup, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var getResp apistructs.UpdateNotifyGroupResponse
	resp, err := hc.Put(host).Path(strutil.Concat("/api/notify-groups/", strconv.FormatInt(id, 10))).
		Header("Org-ID", orgID).JSONBody(request).Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return &getResp.Data, nil
}

func (b *Bundle) DeleteNotifyGroup(id int64, orgID string) (*apistructs.NotifyGroup, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var getResp apistructs.DeleteNotifyGroupResponse
	resp, err := hc.Delete(host).Path(strutil.Concat("/api/notify-groups/", strconv.FormatInt(id, 10))).
		Header("Org-ID", orgID).Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return &getResp.Data, err
}

func (b *Bundle) QueryNotifiesBySource(orgID string, sourceType, sourceID, itemName, label string, clusterNames ...string) ([]*apistructs.NotifyDetail, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	clusterName := ""
	if len(clusterNames) > 0 {
		clusterName = clusterNames[0]
	}
	var getResp apistructs.QuerySourceNotifyResponse
	resp, err := hc.Get(host).Path("/api/notifies/actions/search-by-source").
		Param("sourceType", sourceType).
		Param("sourceId", sourceID).
		Param("itemName", itemName).
		Param("orgId", orgID).
		Param("clusterName", clusterName).
		Param("label", label).
		Do().JSON(&getResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return nil, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return getResp.Data, nil
}

func (b *Bundle) CreateNotifyHistory(request *apistructs.CreateNotifyHistoryRequest) (int64, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var getResp apistructs.CreateNotifyHistoryResponse
	resp, err := hc.Post(host).Path("/api/notify-histories").JSONBody(request).
		Do().JSON(&getResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !getResp.Success {
		return 0, toAPIError(resp.StatusCode(), getResp.Error)
	}
	return getResp.Data, nil
}

// GetNotifyConfig 获取通知配置
func (b *Bundle) GetNotifyConfig(orgIDstr, userID string) (*apistructs.NotifyConfigUpdateRequestBody, error) {
	// TODO: userID should be deprecated
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var resp apistructs.NotifyConfigGetResponse
	r, err := hc.Get(host).Path(fmt.Sprintf("/api/orgs/%s/actions/get-notify-config", orgIDstr)).
		Header(httputil.InternalHeader, "bundle").
		Header("User-ID", userID). // TODO: for compatibility
		Do().
		JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !r.IsOK() || !resp.Success {
		return nil, toAPIError(r.StatusCode(), resp.Error)
	}
	return &resp.Data, nil
}
