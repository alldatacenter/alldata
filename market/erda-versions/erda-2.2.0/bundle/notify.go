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

func (b *Bundle) NotifyList(req apistructs.NotifyPageRequest) (*[]apistructs.DataItem, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var resp apistructs.NotifyListResponse
	path := fmt.Sprintf("/api/notify/records?scope=%v&scopeId=%v", req.Scope, req.ScopeId)
	httpResp, err := hc.Get(host).Path(path).Header(httputil.UserHeader, req.UserId).Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return nil, toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return &resp.Data.List, nil
}

func (b *Bundle) DeleteNotifyRecord(scope, scopeId string, id uint64, userId string) error {
	host, err := b.urls.Monitor()
	if err != nil {
		return err
	}
	hc := b.hc
	var resp apistructs.Header
	path := fmt.Sprintf("/api/notify/records/%d?scope=%v&scopeId=%v", id, scope, scopeId)
	httpResp, err := hc.Delete(host).Path(path).Header(httputil.UserHeader, userId).Do().JSON(&resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return nil
}

func (b *Bundle) SwitchNotifyRecord(scope, scopeId, userId string, operation *apistructs.SwitchOperationData) error {
	host, err := b.urls.Monitor()
	if err != nil {
		return err
	}
	hc := b.hc
	var resp apistructs.Header
	path := fmt.Sprintf("/api/notify/%d/switch?scope=%v&scopeId=%v", operation.Id, scope, scopeId)
	httpResp, err := hc.Put(host).Path(path).Header(httputil.UserHeader, userId).Do().JSON(&resp)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return nil
}

func (b *Bundle) GetNotifyDetail(id uint64) (*apistructs.DetailResponse, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var detailResp apistructs.NotifyDetailResponse
	path := fmt.Sprintf("/api/notify/%d/detail", id)
	httpResp, err := hc.Get(host).Path(path).Do().JSON(&detailResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !detailResp.Success {
		return nil, toAPIError(httpResp.StatusCode(), detailResp.Error)
	}
	return &detailResp.Data, nil
}

func (b *Bundle) GetAllTemplates(scope, scopeId, userId string) (map[string]string, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var resp apistructs.AllTemplatesResponse
	path := fmt.Sprintf("/api/notify/templates?scope=%v&scopeId=%v", scope, scopeId)
	httpResp, err := hc.Get(host).Path(path).Header(httputil.UserHeader, userId).Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return nil, toAPIError(httpResp.StatusCode(), resp.Error)
	}
	templateMap := make(map[string]string)
	for _, v := range resp.Data {
		templateMap[v.ID] = v.Name
	}
	return templateMap, nil
}

func (b *Bundle) GetAllGroups(scope, scopeId, orgId, userId string) ([]apistructs.AllGroups, error) {
	host, err := b.urls.Monitor()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var resp apistructs.AllGroupResponse
	path := "/api/notify/all-group"
	httpResp, err := hc.Get(host).Path(path).Param("scope", scope).Param("scopeId", scopeId).
		Header(httputil.OrgHeader, orgId).Header(httputil.UserHeader, userId).Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return nil, toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return resp.Data, nil
}

func (b *Bundle) GetNotifyConfigMS(userId, orgId string) (bool, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return false, err
	}
	hc := b.hc
	var resp apistructs.NotifyConfigGetResponse
	path := fmt.Sprintf("/api/orgs/%v/actions/get-notify-config", orgId)
	httpResp, err := hc.Get(host).Path(path).Header(httputil.UserHeader, userId).Do().JSON(&resp)
	if err != nil {
		return false, apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return false, toAPIError(httpResp.StatusCode(), resp.Error)
	}
	return resp.Data.Config.EnableMS, nil
}

func (b *Bundle) CollectNotifyMetrics(metrics *apistructs.Metric) error {
	host, err := b.urls.Collector()
	if err != nil {
		return err
	}
	hc := b.hc
	resp, err := hc.Post(host).Path("/collect/notify-metrics").Header("Content-Type", "application/json").
		JSONBody(&metrics).Do().DiscardBody()
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(fmt.Errorf("failed to call monitor status %d", resp.StatusCode()))
	}
	return nil
}
