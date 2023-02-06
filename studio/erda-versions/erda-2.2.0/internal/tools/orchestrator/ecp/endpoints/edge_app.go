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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	ImageType      = "image"
	ProductType    = "product"
	AddonType      = "addon"
	MysqlAddonName = "mysql-edge"
)

// ListEdgeApp List Edge application
func (e *Endpoints) ListEdgeApp(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		orgID     int64
		clusterID int64
		err       error
	)

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.ListAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	orgIDStr := r.URL.Query().Get("orgID")
	if orgIDStr != "" {
		if orgID, err = strutil.Atoi64(orgIDStr); err != nil {
			return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
		}
	}

	clusterIDStr := r.URL.Query().Get("clusterID")
	if clusterIDStr != "" {
		if clusterID, err = strutil.Atoi64(clusterIDStr); err != nil {
			return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
		}
	}

	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
	}

	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
	}

	// parameter check
	if orgID < 0 || pageNo < 0 || pageSize < 0 {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("illegal query param")).ToResp(), nil
	}

	pageQueryParam := &apistructs.EdgeAppListPageRequest{
		OrgID:     orgID,
		ClusterID: clusterID,
		PageNo:    pageNo,
		PageSize:  pageSize,
	}

	total, edgeApps, err := e.edge.ListApp(pageQueryParam)

	if err != nil {
		return apierrors.ErrListEdgeApp.InternalError(err).ToResp(), nil
	}

	rsp := &apistructs.EdgeAppListResponse{
		Total: total,
		List:  *edgeApps,
	}
	return httpserver.OkResp(rsp)
}

// GetEdgeAppStatus Get Edge application status
func (e *Endpoints) GetEdgeAppStatus(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		pageSize    = 1
		pageNo      = DefaultPageNo
		isNotPaging bool
		err         error
		app         *dbclient.EdgeApp
		rsp         *apistructs.EdgeAppStatusResponse
	)

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	edgeAppID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
	}

	isNotPagingStr := r.URL.Query().Get("notPaging")
	if isNotPagingStr != "" {
		parseRes, err := strconv.ParseBool(isNotPagingStr)
		if err != nil {
			return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
		}
		if parseRes {
			isNotPaging = true
		}
	}

	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr != "" {
		pageSize, err = strconv.Atoi(pageSizeStr)
		if err != nil {
			return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
		}
	}

	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr != "" {
		pageNo, err = strconv.Atoi(pageNoStr)
		if err != nil {
			return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
		}
	}
	// paramters check
	if pageNo < 0 || pageSize < 0 {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("illegal query param")).ToResp(), nil
	}

	app, err = e.dbClient.GetEdgeApp(edgeAppID)
	if err != nil {
		return apierrors.ErrListEdgeApp.InternalError(err).ToResp(), nil
	}

	switch app.Type {
	case ImageType, ProductType:
		rsp, err = e.edge.GetAppStatus(ctx, edgeAppID)
	case AddonType:
		switch app.AddonName {
		case MysqlAddonName:
			//just update edge sites
			rsp, err = e.edge.GetEdgeMysqlStatus(ctx, edgeAppID)
		}
	}
	if err != nil {
		return apierrors.ErrListEdgeApp.InternalError(err).ToResp(), nil
	}

	total := len(rsp.SiteList)

	rsp.Total = total

	// Paging default
	if !isNotPaging {
		startPoint := (pageNo - 1) * pageSize
		endPoint := startPoint + pageSize
		if startPoint > total {
			rsp.SiteList = make([]apistructs.EdgeAppSiteStatus, 0)
		}

		if endPoint > total {
			rsp.SiteList = rsp.SiteList[startPoint:]
		} else {
			rsp.SiteList = rsp.SiteList[startPoint:endPoint]
		}
	}

	return httpserver.OkResp(*rsp)
}

// GetEdgeApp Get edge application
func (e *Endpoints) GetEdgeApp(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var err error
	var edgeApp *apistructs.EdgeAppInfo

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	edgeAppID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
	}

	edgeApp, err = e.edge.GetApp(edgeAppID)

	if err != nil {
		return apierrors.ErrListEdgeApp.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(*edgeApp)
}

// CreateEdgeApp Create Edge application
func (e *Endpoints) CreateEdgeApp(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var req apistructs.EdgeAppCreateRequest
	//var edgeAppID uint64
	var err error

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrCreateCluster.MissingParameter("body").ToResp(), nil
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateEdgeApp.InternalError(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", req)

	if req.OrgID <= 0 || req.ClusterID <= 0 {
		return apierrors.ErrCreateEdgeApp.InternalError(fmt.Errorf("illegal create param")).ToResp(), nil
	}

	switch req.Type {
	case ImageType, ProductType:
		err = e.edge.CreateApp(ctx, &req)
	case AddonType:
		switch req.AddonName {
		case MysqlAddonName:
			err = e.edge.CreateEdgeMysql(ctx, &req)
		}
	}
	if err != nil {
		return apierrors.ErrCreateEdgeApp.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("ok")
}

// UpdateEdgeApp Update Edge application
func (e *Endpoints) UpdateEdgeApp(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	edgeAppID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrListEdgeApp.InvalidParameter(err).ToResp(), nil
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	var req apistructs.EdgeAppUpdateRequest
	if r.Body == nil {
		return apierrors.ErrCreateCluster.MissingParameter("body").ToResp(), nil
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateEdgeApp.InternalError(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", req)

	if req.OrgID <= 0 || req.ClusterID <= 0 {
		return apierrors.ErrUpdateEdgeApp.InternalError(fmt.Errorf("illegal create param")).ToResp(), nil
	}

	switch req.Type {
	case ImageType, ProductType:
		err = e.edge.UpdateApp(ctx, edgeAppID, &req)
	case AddonType:
		switch req.AddonName {
		case MysqlAddonName:
			//just update edgesites
			err = e.edge.UpdateEdgeMysql(ctx, edgeAppID, &req)
		}
	}
	if err != nil {
		return apierrors.ErrUpdateEdgeApp.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("ok")
}

// DeleteEdgeApp Delete Edge application
func (e *Endpoints) DeleteEdgeApp(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var err error
	var app *dbclient.EdgeApp

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.DeleteAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}
	edgeAppID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrDeleteEdgeApp.InvalidParameter(err).ToResp(), nil
	}

	app, err = e.dbClient.GetEdgeApp(edgeAppID)
	if err != nil {
		return apierrors.ErrDeleteEdgeApp.InternalError(err).ToResp(), nil
	}
	switch app.Type {
	case ImageType, ProductType:
		err = e.edge.DeleteApp(ctx, edgeAppID)
	case AddonType:
		switch app.AddonName {
		case MysqlAddonName:
			//just update edge sites
			err = e.edge.DeleteEdgeMysql(ctx, edgeAppID)
		}
	}
	if err != nil {
		return apierrors.ErrDeleteEdgeApp.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("ok")
}

// OfflineAppSite Offline site which deploy specified application
func (e *Endpoints) OfflineAppSite(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		req apistructs.EdgeAppSiteRequest
		err error
	)
	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.DeleteAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}
	if r.Body == nil {
		return apierrors.ErrCreateCluster.MissingParameter("body").ToResp(), nil
	}

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrOfflineEdgeAppSite.InternalError(err).ToResp(), nil
	}

	edgeAppID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrOfflineEdgeAppSite.InvalidParameter(err).ToResp(), nil
	}

	app, err := e.dbClient.GetEdgeApp(edgeAppID)
	if err != nil {
		return apierrors.ErrOfflineEdgeAppSite.InternalError(err).ToResp(), nil
	}

	switch app.Type {
	case ImageType, ProductType:
		err = e.edge.OfflineAppSite(ctx, app, req.SiteName)
		break
	case AddonType:
		switch app.AddonName {
		case MysqlAddonName:
			err = e.edge.OfflineEdgeMysql(ctx, app, req.SiteName)
			break
		}
		break
	}
	if err != nil {
		return apierrors.ErrOfflineEdgeAppSite.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("ok")
}

// RestartAppSite Restart site which deploy specified application
func (e *Endpoints) RestartAppSite(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		req apistructs.EdgeAppSiteRequest
		err error
	)

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrCreateCluster.MissingParameter("body").ToResp(), nil
	}

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrRestartEdgeApp.InternalError(err).ToResp(), nil
	}

	edgeAppID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrRestartEdgeApp.InvalidParameter(err).ToResp(), nil
	}

	app, err := e.dbClient.GetEdgeApp(edgeAppID)
	if err != nil {
		return apierrors.ErrRestartEdgeApp.InternalError(err).ToResp(), nil
	}

	switch app.Type {
	case ImageType, ProductType:
		err = e.edge.RestartAppSite(ctx, app, req.SiteName)
		break
	case AddonType:
		switch app.AddonName {
		case MysqlAddonName:
			err = e.edge.RestartEdgeMysql(ctx, app, req.SiteName)
			break
		}
		break
	}
	if err != nil {
		return apierrors.ErrRestartEdgeApp.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("ok")
}
