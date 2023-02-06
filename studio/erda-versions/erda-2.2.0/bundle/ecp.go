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
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

const (
	userIDIdentity       = "User-ID"
	orgIDIdentity        = "Org-ID"
	edgeAppURL           = "/api/edge/app"
	edgeSiteURL          = "/api/edge/site"
	edgeConfigsetURL     = "/api/edge/configset"
	edgeCfgSetItemURL    = "/api/edge/configset-item"
	schedulerClusterInfo = "/api/clusterinfo/%s"
	edgeInstanceInfo     = "/api/instanceinfo"
	edgeSiteInitURL      = "/api/edge/site/init"
	edgeAppSiteRestart   = "/api/edge/app/site/restart/%d"
	edgeAppSiteOffline   = "/api/edge/app/site/offline/%d"
)

func (b *Bundle) ListEdgeApp(req *apistructs.EdgeAppListPageRequest, identify apistructs.Identity) (*apistructs.EdgeAppListResponse, error) {
	var (
		httpReqRes httpserver.Resp
		res        apistructs.EdgeAppListResponse
		buffer     bytes.Buffer
		reqParam   map[string]string
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	hcReq := b.hc.
		Get(host).
		Path(edgeAppURL).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID)

	// Ops Edge 默认分页, Size 20, PageNo 1
	if req.PageNo == 0 && req.PageSize == 0 {
		reqParam = map[string]string{
			"orgID": strconv.Itoa(int(req.OrgID)),
		}
	} else {
		reqParam = map[string]string{
			"pageNo":   strconv.Itoa(req.PageNo),
			"pageSize": strconv.Itoa(req.PageSize),
			"orgID":    strconv.Itoa(int(req.OrgID)),
		}
	}

	if req.ClusterID > 0 {
		reqParam["clusterID"] = strconv.FormatInt(req.ClusterID, 10)
	}

	for key, value := range reqParam {
		hcReq.Param(key, value)
	}

	httpResp, err := hcReq.Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (list edge application) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) GetEdgeApp(appID uint64, identify apistructs.Identity) (*apistructs.EdgeAppInfo, error) {
	var (
		res        apistructs.EdgeAppInfo
		buffer     bytes.Buffer
		httpReqRes httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	httpResp, err := b.hc.
		Get(host).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Path(fmt.Sprintf(edgeAppURL+"/%d", appID)).Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (list edge application) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) CreateEdgeApp(req *apistructs.EdgeAppCreateRequest, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Post(host).
		Path(edgeAppURL).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) UpdateEdgeApp(req *apistructs.EdgeAppUpdateRequest, appID uint64, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Put(host).
		Path(fmt.Sprintf(edgeAppURL+"/%d", appID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) DeleteEdgeApp(appID int64, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Delete(host).
		Path(fmt.Sprintf(edgeAppURL+"/%d", appID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) GetEdgeAppStatus(req *apistructs.EdgeAppStatusListRequest, identify apistructs.Identity) (*apistructs.EdgeAppStatusResponse, error) {
	var (
		res        apistructs.EdgeAppStatusResponse
		buffer     bytes.Buffer
		httpReqRes httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	reqClient := b.hc.
		Get(host).
		Path(fmt.Sprintf(edgeAppURL+"/status/%d", req.AppID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID)

	if req.NotPaging {
		reqClient.Param("notPaging", "true")
	} else {
		reqClient.Param("pageNo", strconv.Itoa(req.PageNo))
		reqClient.Param("pageSize", strconv.Itoa(req.PageSize))
	}

	httpResp, err := reqClient.Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (list edge application) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) ListEdgeSite(req *apistructs.EdgeSiteListPageRequest, identify apistructs.Identity) (*apistructs.EdgeSiteListResponse, error) {
	var (
		httpReqRes httpserver.Resp
		res        apistructs.EdgeSiteListResponse
		buffer     bytes.Buffer
		reqParam   map[string]string
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	hcReq := b.hc.
		Get(host).
		Path(edgeSiteURL).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID)

	if req.NotPaging {
		reqParam = map[string]string{
			"orgID":     strconv.Itoa(int(req.OrgID)),
			"notPaging": strconv.FormatBool(req.NotPaging),
		}
		// 如果不指定采用Ops默认分页参数
	} else if req.PageNo == 0 && req.PageSize == 0 {
		reqParam = map[string]string{
			"orgID": strconv.Itoa(int(req.OrgID)),
		}
	} else {
		reqParam = map[string]string{
			"pageNo":   strconv.Itoa(req.PageNo),
			"pageSize": strconv.Itoa(req.PageSize),
			"orgID":    strconv.Itoa(int(req.OrgID)),
		}
	}

	if req.Search != "" {
		reqParam["search"] = req.Search
	}

	if req.ClusterID > 0 {
		reqParam["clusterID"] = strconv.Itoa(int(req.ClusterID))
	}

	for key, value := range reqParam {
		hcReq.Param(key, value)
	}

	httpResp, err := hcReq.Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (list edge site) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) GetEdgeSite(siteID int64, identify apistructs.Identity) (*apistructs.EdgeSiteInfo, error) {
	var (
		res        apistructs.EdgeSiteInfo
		buffer     bytes.Buffer
		httpReqRes httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	httpResp, err := b.hc.
		Get(host).
		Path(fmt.Sprintf(edgeSiteURL+"/%d", siteID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (list edge application) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) CreateEdgeSite(req *apistructs.EdgeSiteCreateRequest, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Post(host).
		Path(edgeSiteURL).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) UpdateEdgeSite(req *apistructs.EdgeSiteUpdateRequest, siteID int64, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Put(host).
		Path(fmt.Sprintf(edgeSiteURL+"/%d", siteID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) DeleteEdgeSite(siteID int64, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Delete(host).
		Path(fmt.Sprintf(edgeSiteURL+"/%d", siteID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) ListEdgeConfigset(req *apistructs.EdgeConfigSetListPageRequest, identify apistructs.Identity) (*apistructs.EdgeConfigSetListResponse, error) {
	var (
		httpReqRes httpserver.Resp
		res        apistructs.EdgeConfigSetListResponse
		buffer     bytes.Buffer
		reqParam   map[string]string
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	hcReq := b.hc.
		Get(host).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Path(edgeConfigsetURL)

	if req.NotPaging {
		reqParam = map[string]string{
			"orgID":     strconv.Itoa(int(req.OrgID)),
			"notPaging": strconv.FormatBool(req.NotPaging),
		}
	} else if req.PageNo == 0 && req.PageSize == 0 {
		reqParam = map[string]string{
			"orgID": strconv.Itoa(int(req.OrgID)),
		}
	} else {
		reqParam = map[string]string{
			"pageNo":   strconv.Itoa(req.PageNo),
			"pageSize": strconv.Itoa(req.PageSize),
			"orgID":    strconv.Itoa(int(req.OrgID)),
		}
	}

	if req.ClusterID > 0 {
		reqParam["clusterID"] = strconv.FormatInt(req.ClusterID, 10)
	}

	for key, value := range reqParam {
		hcReq.Param(key, value)
	}

	httpResp, err := hcReq.Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (list config set) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) GetEdgeConfigSet(itemID int64, identify apistructs.Identity) (*apistructs.EdgeConfigSetInfo, error) {
	var (
		res        apistructs.EdgeConfigSetInfo
		buffer     bytes.Buffer
		httpReqRes httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	httpResp, err := b.hc.
		Get(host).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Path(fmt.Sprintf(edgeConfigsetURL+"/%d", itemID)).Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (get configset item) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) CreateEdgeConfigset(req *apistructs.EdgeConfigSetCreateRequest, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Post(host).
		Path(edgeConfigsetURL).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) UpdateEdgeConfigset(req *apistructs.EdgeConfigSetUpdateRequest, siteID int64, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Put(host).
		Path(fmt.Sprintf(edgeConfigsetURL+"/%d", siteID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) DeleteEdgeConfigset(siteID int64, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Delete(host).
		Path(fmt.Sprintf(edgeConfigsetURL+"/%d", siteID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) ListEdgeCfgSetItem(req *apistructs.EdgeCfgSetItemListPageRequest, identify apistructs.Identity) (*apistructs.EdgeCfgSetItemListResponse, error) {
	var (
		httpReqRes httpserver.Resp
		res        apistructs.EdgeCfgSetItemListResponse
		buffer     bytes.Buffer
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	hcReq := b.hc.
		Get(host).
		Path(edgeCfgSetItemURL).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID)

	reqParam := map[string]string{
		"pageNo":      strconv.Itoa(req.PageNo),
		"pageSize":    strconv.Itoa(req.PageSize),
		"configSetID": strconv.Itoa(int(req.ConfigSetID)),
	}

	if req.Search != "" {
		reqParam["search"] = req.Search
	}

	for key, value := range reqParam {
		hcReq.Param(key, value)
	}

	httpResp, err := hcReq.Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (list config set item) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) GetEdgeCfgSetItem(itemID int64, identify apistructs.Identity) (*apistructs.EdgeCfgSetItemInfo, error) {
	var (
		res        apistructs.EdgeCfgSetItemInfo
		buffer     bytes.Buffer
		httpReqRes httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	httpResp, err := b.hc.
		Get(host).
		Path(fmt.Sprintf(edgeCfgSetItemURL+"/%d", itemID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Do().Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (get configset item) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	resJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	if err = json.Unmarshal(resJson, &res); err != nil {
		return nil, err
	}

	return &res, nil
}

func (b *Bundle) CreateEdgeCfgSetItem(req *apistructs.EdgeCfgSetItemCreateRequest, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Post(host).
		Path(edgeCfgSetItemURL).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) UpdateEdgeCfgSetItem(req *apistructs.EdgeCfgSetItemUpdateRequest, cfgSetItemID int64, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Put(host).
		Path(fmt.Sprintf(edgeCfgSetItemURL+"/%d", cfgSetItemID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) DeleteEdgeCfgSetItem(siteID int64, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Delete(host).
		Path(fmt.Sprintf(edgeCfgSetItemURL+"/%d", siteID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) ListEdgeCluster(orgID uint64, valueType string, identify apistructs.Identity) ([]map[string]interface{}, error) {
	var (
		edgeClusters = make([]map[string]interface{}, 0)
		edgeCloudKey = "IS_EDGE_CLOUD"
	)

	res, err := b.ListClusters("", orgID)
	if err != nil {
		return edgeClusters, err
	}

	if valueType == apistructs.EdgeListValueTypeName {
		for _, value := range res {
			res, err := b.GetClusterInfo(value.Name, identify)
			if err != nil {
				logrus.Errorf("get cluster %s info error: %v", value.Name, err)
				continue
			}
			if _, ok := res[edgeCloudKey]; ok {
				edgeClusters = append(edgeClusters, map[string]interface{}{
					"name":  value.Name,
					"value": value.Name,
				})
			}
		}
	} else if valueType == apistructs.EdgeListValueTypeID {
		for _, value := range res {
			res, err := b.GetClusterInfo(value.Name, identify)
			if err != nil {
				logrus.Errorf("get cluster %s info error: %v", value.Name, err)
				continue
			}
			if _, ok := res[edgeCloudKey]; ok {
				edgeClusters = append(edgeClusters, map[string]interface{}{
					"name":  value.Name,
					"value": value.ID,
				})
			}
		}
	}

	return edgeClusters, nil
}

func (b *Bundle) ListEdgeSelectSite(orgID, clusterID int64, valueType string, identify apistructs.Identity) ([]map[string]interface{}, error) {
	var (
		sites = make([]map[string]interface{}, 0)
	)

	res, err := b.ListEdgeSite(&apistructs.EdgeSiteListPageRequest{
		OrgID:     orgID,
		ClusterID: clusterID,
		NotPaging: true,
	}, identify)

	if err != nil {
		return nil, err
	}

	if valueType == apistructs.EdgeListValueTypeID {
		for _, value := range res.List {
			sites = append(sites, map[string]interface{}{
				"name":  value.Name,
				"value": value.ID,
			})
		}
	} else if valueType == apistructs.EdgeListValueTypeName {
		for _, value := range res.List {
			sites = append(sites, map[string]interface{}{
				"name":  value.Name,
				"value": value.Name,
			})
		}
	}

	return sites, nil
}

func (b *Bundle) ListEdgeSelectConfigSet(orgID, clusterID int64, valueType string, identify apistructs.Identity) ([]map[string]interface{}, error) {
	configSets := make([]map[string]interface{}, 0)

	res, err := b.ListEdgeConfigset(&apistructs.EdgeConfigSetListPageRequest{
		OrgID:     orgID,
		ClusterID: clusterID,
		NotPaging: true,
	}, identify)

	if err != nil {
		return nil, err
	}

	if valueType == apistructs.EdgeListValueTypeID {
		for _, value := range res.List {
			configSets = append(configSets, map[string]interface{}{
				"name":  value.Name,
				"value": value.ID,
			})
		}
	} else if valueType == apistructs.EdgeListValueTypeName {
		for _, value := range res.List {
			configSets = append(configSets, map[string]interface{}{
				"name":  value.Name,
				"value": value.Name,
			})
		}
	}

	return configSets, nil
}

func (b *Bundle) ListEdgeSelectApps(orgID, clusterID int64, exceptName string, valueType string, identify apistructs.Identity) ([]map[string]interface{}, error) {
	edgeApps := make([]map[string]interface{}, 0)

	res, err := b.ListEdgeApp(&apistructs.EdgeAppListPageRequest{
		OrgID:     orgID,
		ClusterID: clusterID,
	}, identify)

	if err != nil {
		return nil, err
	}

	if valueType == apistructs.EdgeListValueTypeID {
		for _, value := range res.List {
			isBreak := false
			if value.Name == exceptName {
				continue
			}
			for _, s := range value.DependApp {
				if s == exceptName {
					isBreak = true
				}
			}
			if isBreak {
				break
			}
			edgeApps = append(edgeApps, map[string]interface{}{
				"name":  value.Name,
				"value": value.ID,
			})
		}
	} else if valueType == apistructs.EdgeListValueTypeName {
		for _, value := range res.List {
			isBreak := false
			if value.Name == exceptName {
				continue
			}
			for _, s := range value.DependApp {
				if s == exceptName {
					isBreak = true
				}
			}
			if isBreak {
				break
			}
			edgeApps = append(edgeApps, map[string]interface{}{
				"name":  value.Name,
				"value": value.Name,
			})
		}
	}

	return edgeApps, nil
}

func (b *Bundle) GetEdgeSiteInitShell(siteID int64, identify apistructs.Identity) (map[string]interface{}, error) {
	var (
		httpReqRes httpserver.Resp
		buffer     bytes.Buffer
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	httpResp, err := b.hc.
		Get(host).
		Path(fmt.Sprintf(edgeSiteInitURL+"/%d", siteID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Do().
		Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request ops api (get site init shell) not found")
	}

	fmt.Println(httpResp.Body())

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	if shell, ok := httpReqRes.Data.(map[string]interface{}); ok {
		return shell, nil
	} else {
		return nil, fmt.Errorf("response data format error")
	}
}

func (b *Bundle) GetClusterInfo(clusterName string, identify apistructs.Identity) (map[string]interface{}, error) {
	var (
		httpReqRes httpserver.Resp
		buffer     bytes.Buffer
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	httpResp, err := b.hc.
		Get(host).
		Path(fmt.Sprintf(schedulerClusterInfo, clusterName)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Do().
		Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request scheduler api (get cluster info) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	if !httpReqRes.Success {
		return nil, fmt.Errorf(httpReqRes.Err.Msg)
	}

	if info, ok := httpReqRes.Data.(map[string]interface{}); ok {
		return info, nil
	} else {
		return nil, fmt.Errorf("response data format error")
	}
}

func (b *Bundle) GetEdgeInstanceInfo(orgID int64, appName, site string, identify apistructs.Identity) ([]apistructs.InstanceInfoData, error) {
	var (
		httpReqRes httpserver.Resp
		infosData  []apistructs.InstanceInfoData
		buffer     bytes.Buffer
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return nil, err
	}

	httpResp, err := b.hc.
		Get(host).
		Path(edgeInstanceInfo).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		Param("orgID", strconv.Itoa(int(orgID))).
		Param("edgeApplicationName", appName).
		Param("edgeSite", site).
		Do().
		Body(&buffer)

	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}

	if httpResp.IsNotfound() {
		return nil, fmt.Errorf("request scheduler api (get cluster info) not found")
	}

	if err = json.Unmarshal(buffer.Bytes(), &httpReqRes); err != nil {
		return nil, err
	}

	infoJson, err := json.Marshal(httpReqRes.Data)
	if err != nil {
		return nil, err
	}

	err = json.Unmarshal(infoJson, &infosData)
	if err != nil {
		return nil, err
	}

	return infosData, nil
}

func (b *Bundle) RestartEdgeAppSite(appID uint64, req *apistructs.EdgeAppSiteRequest, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Post(host).
		Path(fmt.Sprintf(edgeAppSiteRestart, appID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}

func (b *Bundle) OfflineEdgeAppSite(appID uint64, req *apistructs.EdgeAppSiteRequest, identify apistructs.Identity) error {
	var (
		resp httpserver.Resp
	)

	host, err := b.urls.Orchestrator()
	if err != nil {
		return err
	}

	httpResp, err := b.hc.
		Post(host).
		Path(fmt.Sprintf(edgeAppSiteOffline, appID)).
		Header(userIDIdentity, identify.UserID).
		Header(orgIDIdentity, identify.OrgID).
		JSONBody(req).
		Do().
		JSON(&resp)

	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !httpResp.IsOK() || !resp.Success {
		return toAPIError(httpResp.StatusCode(), resp.Err)
	}

	return nil
}
