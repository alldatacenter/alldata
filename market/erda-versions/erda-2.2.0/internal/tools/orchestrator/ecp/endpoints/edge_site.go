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
	"github.com/erda-project/erda/internal/tools/orchestrator/ecp/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	DefaultPageSize = 20
	DefaultPageNo   = 1
)

// ListEdgeSite List edge site
func (e *Endpoints) ListEdgeSite(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		pageSize    = DefaultPageSize
		pageNo      = DefaultPageNo
		orgID       int64
		clusterID   int64
		isNotPaging bool
		err         error
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

	searchCondition := r.URL.Query().Get("search")

	orgIDStr := r.URL.Query().Get("orgID")
	if orgIDStr != "" {
		if orgID, err = strutil.Atoi64(orgIDStr); err != nil {
			return apierrors.ErrListEdgeSite.InvalidParameter(err).ToResp(), nil
		}
	}

	clusterIDStr := r.URL.Query().Get("clusterID")
	if clusterIDStr != "" {
		if clusterID, err = strutil.Atoi64(clusterIDStr); err != nil {
			return apierrors.ErrListEdgeSite.InvalidParameter(err).ToResp(), nil
		}
	}

	isNotPagingStr := r.URL.Query().Get("notPaging")
	if isNotPagingStr != "" {
		parseRes, err := strconv.ParseBool(isNotPagingStr)
		if err != nil {
			return apierrors.ErrListEdgeSite.InvalidParameter(err).ToResp(), nil
		}
		if parseRes {
			isNotPaging = true
		}
	}

	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr != "" {
		pageSize, err = strconv.Atoi(pageSizeStr)
		if err != nil {
			return apierrors.ErrListEdgeSite.InvalidParameter(err).ToResp(), nil
		}
	}

	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr != "" {
		pageNo, err = strconv.Atoi(pageNoStr)
		if err != nil {
			return apierrors.ErrListEdgeSite.InvalidParameter(err).ToResp(), nil
		}
	}

	// parameters check
	if orgID < 0 || clusterID < 0 || pageNo < 0 || pageSize < 0 {
		return apierrors.ErrListEdgeSite.InternalError(fmt.Errorf("illegal query param")).ToResp(), nil
	}

	pageQueryParam := &apistructs.EdgeSiteListPageRequest{
		OrgID:     orgID,
		ClusterID: clusterID,
		NotPaging: isNotPaging,
		Search:    searchCondition,
		PageNo:    pageNo,
		PageSize:  pageSize,
	}

	total, edgeSites, err := e.edge.ListSite(ctx, pageQueryParam)

	if err != nil {
		return apierrors.ErrListEdgeSite.InternalError(err).ToResp(), nil
	}

	rsp := &apistructs.EdgeSiteListResponse{
		Total: total,
		List:  *edgeSites,
	}
	return httpserver.OkResp(rsp)
}

func (e *Endpoints) GetEdgeSite(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrGetEdgeSite.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err := e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	edgeSiteID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrGetEdgeSite.InvalidParameter(err).ToResp(), nil
	}

	edgeSite, err := e.edge.GetEdgeSite(ctx, edgeSiteID)

	if err != nil {
		return apierrors.ErrListEdgeSite.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(*edgeSite)
}

// CreateEdgeSite Create edge site
func (e *Endpoints) CreateEdgeSite(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		req apistructs.EdgeSiteCreateRequest
	)

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrCreateEdgeSite.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err := e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}
	if r.Body == nil {
		return apierrors.ErrCreateCluster.MissingParameter("body").ToResp(), nil
	}

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateEdgeSite.InternalError(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", req)

	// parameters check
	if req.OrgID <= 0 || req.ClusterID <= 0 {
		return apierrors.ErrListEdgeSite.InternalError(fmt.Errorf("illegal create param")).ToResp(), nil
	}

	edgeSiteID, err := e.edge.CreateSite(ctx, &req)
	if err != nil {
		return apierrors.ErrCreateEdgeSite.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(edgeSiteID)
}

// UpdateEdgeSite Update edge site
func (e *Endpoints) UpdateEdgeSite(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		req apistructs.EdgeSiteUpdateRequest
	)

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrUpdateEdgeSite.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err := e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	edgeSiteID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrUpdateEdgeSite.InvalidParameter(err).ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrUpdateEdgeSite.MissingParameter("body").ToResp(), nil
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateEdgeSite.InternalError(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", req)

	err = e.edge.UpdateSite(edgeSiteID, &req)
	if err != nil {
		return apierrors.ErrUpdateEdgeSite.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(edgeSiteID)
}

// DeleteEdgeSite Delete edge site
func (e *Endpoints) DeleteEdgeSite(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var err error
	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrDeleteEdgeSite.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.DeleteAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}
	edgeSiteID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrDeleteEdgeSite.InvalidParameter(err).ToResp(), nil
	}

	if err = e.edge.DeleteSite(ctx, edgeSiteID); err != nil {
		return apierrors.ErrDeleteEdgeSite.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(edgeSiteID)
}

// GetInitEdgeSiteShell Get edge site init shell
func (e *Endpoints) GetInitEdgeSiteShell(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var err error
	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrGetEdgeSiteInit.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}
	edgeSiteID, err := strutil.Atoi64(vars["ID"])

	if err != nil {
		return apierrors.ErrGetEdgeSiteInit.InvalidParameter(err).ToResp(), nil
	}

	res, err := e.edge.GetInitSiteShell(ctx, edgeSiteID)
	if err != nil {
		return apierrors.ErrGetEdgeSiteInit.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(res)
}

// OfflineEdgeHost Offline edge host
func (e *Endpoints) OfflineEdgeHost(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		offlineRequest = struct {
			SiteIP string `json:"siteIP"`
		}{}
	)

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrOfflineEdgeSite.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err := e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	edgeSiteID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrOfflineEdgeSite.InvalidParameter(err).ToResp(), nil
	}

	if err = json.NewDecoder(r.Body).Decode(&offlineRequest); err != nil {
		return apierrors.ErrOfflineEdgeSite.InternalError(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", offlineRequest)

	if err = e.edge.OfflineEdgeHost(ctx, edgeSiteID, offlineRequest.SiteIP); err != nil {
		return apierrors.ErrOfflineEdgeSite.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(edgeSiteID)
}
