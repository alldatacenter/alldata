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
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// ListEdgeConfigSetItem List edge configSet item
func (e *Endpoints) ListEdgeConfigSetItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		siteID      int64
		configSetID int64
		pageSize    = DefaultPageSize
		pageNo      = DefaultPageNo
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
	scope := r.URL.Query().Get("scope")
	if scope != "" && !(scope == "public" || scope == "site") {
		return apierrors.ErrListEdgeCfgSetItem.InvalidParameter(fmt.Errorf("%s scope not support", scope)).ToResp(), nil
	}

	siteIDStr := r.URL.Query().Get("siteID")
	if siteIDStr != "" {
		if siteID, err = strutil.Atoi64(siteIDStr); err != nil {
			return apierrors.ErrListEdgeCfgSetItem.InvalidParameter(err).ToResp(), nil
		}
	}

	configSetIDStr := r.URL.Query().Get("configSetID")
	if configSetIDStr != "" {
		if configSetID, err = strutil.Atoi64(configSetIDStr); err != nil {
			return apierrors.ErrListEdgeCfgSetItem.InvalidParameter(err).ToResp(), nil
		}
	} else {
		return apierrors.ErrListEdgeCfgSetItem.InternalError(fmt.Errorf("must provider configSetID")).ToResp(), nil
	}

	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr != "" {
		pageSize, err = strconv.Atoi(pageSizeStr)
		if err != nil {
			return apierrors.ErrListEdgeCfgSetItem.InvalidParameter(err).ToResp(), nil
		}
	}

	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr != "" {
		pageNo, err = strconv.Atoi(pageNoStr)
		if err != nil {
			return apierrors.ErrListEdgeCfgSetItem.InvalidParameter(err).ToResp(), nil
		}
	}

	isNotPagingStr := r.URL.Query().Get("notPaging")
	if isNotPagingStr != "" {
		parseRes, err := strconv.ParseBool(isNotPagingStr)
		if err != nil {
			return apierrors.ErrListEdgeCfgSetItem.InvalidParameter(err).ToResp(), nil
		}
		if parseRes {
			isNotPaging = true
		}
	}

	// parameters check
	if configSetID < 0 || pageNo < 0 || pageSize < 0 {
		return apierrors.ErrListEdgeCfgSetItem.InternalError(fmt.Errorf("illegal query param")).ToResp(), nil
	}

	pageQueryParam := &apistructs.EdgeCfgSetItemListPageRequest{
		Scope:       scope,
		ConfigSetID: configSetID,
		Search:      searchCondition,
		SiteID:      siteID,
		PageNo:      pageNo,
		NotPaging:   isNotPaging,
		PageSize:    pageSize,
	}

	total, cfgSetItems, err := e.edge.ListConfigSetItem(pageQueryParam)

	if err != nil {
		return apierrors.ErrListEdgeCfgSetItem.InternalError(err).ToResp(), nil
	}

	rsp := &apistructs.EdgeCfgSetItemListResponse{
		Total: total,
		List:  *cfgSetItems,
	}
	return httpserver.OkResp(rsp)
}

// GetEdgeConfigSetItem Get edge configSet
func (e *Endpoints) GetEdgeConfigSetItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var err error
	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.GetAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}
	itemID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrListEdgeCfgSetItem.InvalidParameter(err).ToResp(), nil
	}

	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
	}

	cfgSetItem, err := e.edge.GetConfigSetItem(itemID)

	if err != nil {
		return apierrors.ErrListEdgeCfgSetItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(*cfgSetItem)
}

// CreateEdgeConfigSet Create edge configSet
func (e *Endpoints) CreateEdgeConfigSetItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.EdgeCfgSetItemCreateRequest
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
		return apierrors.ErrCreateEdgeCfgSetItem.MissingParameter("body").ToResp(), nil
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateEdgeCfgSetItem.InternalError(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", req)

	itemID, err := e.edge.CreateConfigSetItem(ctx, &req)
	if err != nil {
		return apierrors.ErrCreateEdgeCfgSetItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(itemID)
}

// UpdateEdgeConfigSetItem Update edge configSet
func (e *Endpoints) UpdateEdgeConfigSetItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		req apistructs.EdgeCfgSetItemUpdateRequest
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
	edgeSiteID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrUpdateEdgeCfgSetItem.InvalidParameter(err).ToResp(), nil
	}

	if r.Body == nil {
		return apierrors.ErrUpdateEdgeCfgSetItem.MissingParameter("body").ToResp(), nil
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrUpdateEdgeCfgSetItem.InternalError(err).ToResp(), nil
	}
	logrus.Infof("request body: %+v", req)

	err = e.edge.UpdateConfigSetItem(ctx, edgeSiteID, &req)
	if err != nil {
		return apierrors.ErrUpdateEdgeCfgSetItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(edgeSiteID)
}

// DeleteEdgeConfigSetItem Delete edge configSet
func (e *Endpoints) DeleteEdgeConfigSetItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var err error
	itemID, err := strutil.Atoi64(vars["ID"])
	if err != nil {
		return apierrors.ErrDeleteEdgeCfgSetItem.InvalidParameter(err).ToResp(), nil
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		return apierrors.ErrListEdgeApp.InternalError(fmt.Errorf("failed to get User-ID or Org-ID from request header")).ToResp(), nil
	}

	// permission check
	err = e.EdgePermissionCheck(i.UserID, i.OrgID, "", apistructs.DeleteAction)
	if err != nil {
		return apierrors.AccessDeny.AccessDenied().ToResp(), nil
	}

	if err = e.edge.DeleteConfigSetItem(ctx, itemID); err != nil {
		return apierrors.ErrDeleteEdgeCfgSetItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(itemID)
}
