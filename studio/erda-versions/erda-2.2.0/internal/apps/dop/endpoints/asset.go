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
	"sort"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

const (
	urlPathAssetID         = "assetID"
	urlPathVersionID       = "versionID"
	urlPathSwaggerVersion  = "swaggerVersion"
	urlPathMinor           = "minor"
	urlPathInstantiationID = "instantiationID"
	urlPathClientID        = "clientID"
	urlPathContractID      = "contractID"
	urlPathAccessID        = "accessID"
	urlPathProjectID       = "projectID"
	urlPathSLAID           = "slaID"
)

// CreateAPIAsset creates APIAsset
func (e *Endpoints) CreateAPIAsset(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.CreateAPIAsset.NotLogin().ToResp(), nil
	}

	if r.ContentLength == 0 {
		return apierrors.CreateAPIAsset.MissingParameter(apierrors.MissingRequestBody).ToResp(), nil
	}
	var req apistructs.APIAssetCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.CreateAPIAsset.InvalidParameter(err).ToResp(), nil
	}
	req.IdentityInfo = identityInfo

	assetID, err := e.assetSvc.CreateAPIAsset(ctx, req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(assetID)
}

// GetAPIAsset selects APIAsset
func (e *Endpoints) GetAPIAsset(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetAPIAsset.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetAPIAsset.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	req := apistructs.GetAPIAssetReq{
		OrgID:     orgID,
		Identity:  &identityInfo,
		URIParams: &apistructs.GetAPIAssetURIPrams{AssetID: vars[urlPathAssetID]},
	}

	data, err := e.assetSvc.GetAsset(&req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(data, []string{data.Asset.CreatorID})
}

// PagingAPIAssets pages APIAssets
func (e *Endpoints) PagingAPIAssets(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.PagingAPIAssets.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.PagingAPIAssets.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var queryParams apistructs.PagingAPIAssetsQueryParams
	if err := e.queryStringDecoder.Decode(&queryParams, r.URL.Query()); err != nil {
		return apierrors.PagingAPIAssets.InvalidParameter(err).ToResp(), nil
	}

	var req = apistructs.PagingAPIAssetsReq{
		OrgID:       orgID,
		Identity:    &identityInfo,
		QueryParams: &queryParams,
	}

	result, err := e.assetSvc.PagingAsset(req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	sort.Slice(result.List, func(i, j int) bool {
		return result.List[i].Asset.UpdatedAt.After(result.List[j].Asset.UpdatedAt)
	})

	return httpserver.OkResp(result, result.UserIDs)
}

// UpdateAPIAsset updates APIAsset
func (e *Endpoints) UpdateAPIAsset(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.UpdateAPIAsset.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.UpdateAPIAsset.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var (
		keys map[string]interface{}
	)
	if err = json.NewDecoder(r.Body).Decode(&keys); err != nil {
		return apierrors.UpdateAPIAsset.InvalidParameter(err).ToResp(), nil
	}

	var req = apistructs.UpdateAPIAssetReq{
		OrgID:     orgID,
		Identity:  &identity,
		URIParams: &apistructs.UpdateAPIAssetURIParams{AssetID: vars["assetID"]},
		Keys:      keys,
	}

	if apiError := e.assetSvc.UpdateAPIAsset(&req); apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(nil)
}

// DeleteAPIAsset deletes APIAsset
func (e *Endpoints) DeleteAPIAsset(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.DeleteAPIAsset.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.DeleteAPIAsset.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var req = apistructs.APIAssetDeleteRequest{
		OrgID:        orgID,
		AssetID:      vars[urlPathAssetID],
		IdentityInfo: identityInfo,
	}

	if err = e.assetSvc.DeleteAssetByAssetID(ctx, req); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}
