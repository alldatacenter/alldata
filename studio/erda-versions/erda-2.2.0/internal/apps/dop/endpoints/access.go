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
	"net/http"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"

	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

// CreateAccess creates an Access
func (e *Endpoints) CreateAccess(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.CreateAccess.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.CreateAccess.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var body apistructs.CreateAccessBody
	if err = json.NewDecoder(r.Body).Decode(&body); err != nil {
		return apierrors.CreateAccess.InvalidParameter("invalid request body").ToResp(), nil
	}

	var req = apistructs.CreateAccessReq{
		OrgID:    orgID,
		Identity: &identity,
		Body:     &body,
	}

	data, apiError := e.assetSvc.CreateAccess(ctx, &req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(data)
}

// ListAccess lists Accesses
func (e *Endpoints) ListAccess(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ListAccess.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.ListAccess.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var queryParams apistructs.ListAccessQueryParams
	if err = e.queryStringDecoder.Decode(&queryParams, r.URL.Query()); err != nil {
		return apierrors.ListAccess.InvalidParameter("invalid query parameters").ToResp(), nil
	}

	var req = apistructs.ListAccessReq{
		OrgID:       orgID,
		Identity:    &identity,
		QueryParams: &queryParams,
	}

	data, apiError := e.assetSvc.ListAccess(&req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(data)
}

func (e *Endpoints) GetAccess(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetAccess.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetAccess.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var req = apistructs.GetAccessReq{
		OrgID:     orgID,
		Identity:  &identity,
		URIParams: &apistructs.GetAccessURIParams{AccessID: vars[urlPathAccessID]},
	}

	data, apiError := e.assetSvc.GetAccess(&req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}
	return httpserver.OkResp(data)
}

func (e *Endpoints) DeleteAccess(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.DeleteAccess.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.DeleteAccess.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var req = apistructs.GetAccessReq{
		OrgID:     orgID,
		Identity:  &identity,
		URIParams: &apistructs.GetAccessURIParams{AccessID: vars[urlPathAccessID]},
	}

	if apiError := e.assetSvc.DeleteAccess(ctx, &req); apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(nil)
}

func (e *Endpoints) UpdateAccess(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.UpdateAccess.NotLogin().ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.UpdateAccess.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var req = apistructs.UpdateAccessReq{
		OrgID:     orgID,
		Identity:  &identity,
		URIParams: &apistructs.GetAccessURIParams{AccessID: vars[urlPathAccessID]},
		Body:      new(apistructs.UpdateAccessBody),
	}

	if err = json.NewDecoder(r.Body).Decode(req.Body); err != nil {
		return apierrors.UpdateAccess.InvalidParameter("invalid body").ToResp(), nil
	}

	access, apiError := e.assetSvc.UpdateAccess(ctx, &req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(map[string]interface{}{"access": access})
}
