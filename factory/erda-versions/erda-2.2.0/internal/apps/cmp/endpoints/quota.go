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
	"fmt"
	"net/http"
	"strconv"

	"github.com/gorilla/schema"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/legacy/httpendpoints/i18n"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/resource"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
)

func (e *Endpoints) GetResourceGauge(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	newDecoder := schema.NewDecoder()
	lang := i18n.Language(r)
	ctx = context.WithValue(ctx, resource.Lang, lang)
	req := &apistructs.GaugeRequest{}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	userIDStr := r.Header.Get(httputil.UserHeader)
	if orgIDStr == "" {
		errStr := fmt.Sprintf("org id is nil")
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	err = newDecoder.Decode(req, r.URL.Query())
	if err != nil {
		errStr := fmt.Sprintf("url param error, err: %v", err)
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}

	if req.CpuPerNode < 1 {
		req.CpuPerNode = 8
	}
	if req.MemPerNode < 1 {
		req.MemPerNode = 32
	}

	content, err := e.Resource.GetGauge(ctx, orgIDStr, userIDStr, req)
	if err != nil || content == nil {
		errStr := fmt.Sprintf("failed to decode clusterhook request, err: %v", err)
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	return httpserver.OkResp(content)
}

func (e *Endpoints) GetResourceClass(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	req := &apistructs.ClassRequest{}
	lang := i18n.Language(r)
	ctx = context.WithValue(ctx, resource.Lang, lang)
	newDecoder := schema.NewDecoder()
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	userIDStr := r.Header.Get(httputil.UserHeader)
	if orgIDStr == "" {
		errStr := fmt.Sprintf("org id is nil")
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}

	err = newDecoder.Decode(req, r.URL.Query())
	if err != nil {
		errStr := fmt.Sprintf("url param error, err: %v", err)
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	pie, err := e.Resource.GetPie(ctx, orgIDStr, userIDStr, req)
	if err != nil || pie == nil {
		errStr := fmt.Sprintf("failed to decode clusterhook request, err: %v", err)
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	return httpserver.OkResp(pie)
}

func (e *Endpoints) GetResourceClusterTrend(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	req := &apistructs.TrendRequest{}
	lang := i18n.Language(r)
	ctx = context.WithValue(ctx, resource.Lang, lang)
	newDecoder := schema.NewDecoder()
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	userIDStr := r.Header.Get(httputil.UserHeader)
	if orgIDStr == "" {
		errStr := fmt.Sprintf("org id is nil")
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	err = newDecoder.Decode(req, r.URL.Query())
	if err != nil {
		errStr := fmt.Sprintf("url param error, err: %v", err)
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	orgID, err := strconv.ParseInt(orgIDStr, 10, 64)
	if err != nil {
		errStr := fmt.Sprintf("orgID parse error, err: %v", err)
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	pie, err := e.Resource.GetClusterTrend(ctx, orgID, userIDStr, req)
	if err != nil || pie == nil {
		errStr := fmt.Sprintf("failed to decode clusterhook request, err: %v", err)
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	return httpserver.OkResp(pie)
}

func (e *Endpoints) GetResourceProjectTrend(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	var l = logrus.WithField("func", "*Endpoints.GetResourceProjectTrend")
	if err = r.ParseForm(); err != nil {
		l.WithError(err).Errorln("failed to ParseForm")
		return httpserver.ErrResp(http.StatusBadRequest, "", "invalid query params")
	}
	var (
		req = new(apistructs.TrendRequest)
		q   = r.URL.Query()
	)
	req.OrgID = r.Header.Get(httputil.OrgHeader)
	req.UserID = r.Header.Get(httputil.UserHeader)
	req.Query = new(apistructs.TrendRequestQuery)
	req.Query.Start = q.Get("start")
	req.Query.End = q.Get("end")
	req.Query.Interval = q.Get("interval")
	req.Query.ClustersNames = q["clusterName"]
	req.Query.Scope = q.Get("scope")
	req.Query.ScopeID = q.Get("scopeID")
	req.Query.ResourceType = q.Get("resourceType")
	if err := req.Validate(); err != nil {
		return httpserver.ErrResp(http.StatusBadRequest, "", err.Error())
	}

	pie, err := e.Resource.GetProjectTrend(ctx, req)
	if err != nil || pie == nil {
		errStr := fmt.Sprintf("get resource project trend error, err: %v", err)
		return httpserver.ErrResp(http.StatusInternalServerError, "", errStr)
	}
	return httpserver.OkResp(pie)
}
