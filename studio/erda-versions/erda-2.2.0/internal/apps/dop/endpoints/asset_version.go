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
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateAPIVersion 创建 API 资料版本
func (e *Endpoints) CreateAPIVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.CreateAPIAssetVersion.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.CreateAPIAssetVersion.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var rb apistructs.CreateAPIAssetVersionBody
	if err = json.NewDecoder(r.Body).Decode(&rb); err != nil {
		return apierrors.CreateAPIAssetVersion.InvalidParameter(err).ToResp(), nil
	}
	if rb.SpecDiceFileUUID == "" {
		return apierrors.CreateAPIAssetVersion.MissingParameter("specDiceFileUUID").ToResp(), nil
	}

	asset, version, spec, err := e.assetSvc.CreateAPIAssetVersion(ctx, apistructs.APIAssetVersionCreateRequest{
		OrgID:            orgID,
		APIAssetID:       vars[urlPathAssetID],
		Major:            rb.Major,
		Minor:            rb.Minor,
		Patch:            rb.Patch,
		Desc:             "",
		SpecProtocol:     apistructs.APISpecProtocol(rb.SpecProtocol),
		SpecDiceFileUUID: rb.SpecDiceFileUUID,
		Spec:             "",
		Instances:        nil,
		IdentityInfo:     identity,
	})
	if err != nil {
		return errorresp.ErrResp(err)
	}

	userIDs := strutil.DedupSlice([]string{asset.CreatorID, asset.UpdaterID, version.CreatorID, version.UpdaterID,
		spec.CreatorID, spec.UpdaterID})

	return httpserver.OkResp(version, userIDs)
}

// PagingAPIAssetVersions 查询 API 资料版本列表
func (e *Endpoints) PagingAPIAssetVersions(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.PagingAPIAssetVersions.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.PagingAPIAssetVersions.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var (
		uriParams   = apistructs.PagingAPIAssetVersionURIParams{AssetID: vars[urlPathAssetID]}
		queryParams apistructs.PagingAPIAssetVersionQueryParams
	)
	if err := e.queryStringDecoder.Decode(&queryParams, r.URL.Query()); err != nil {
		return apierrors.PagingAPIAssetVersions.InvalidParameter(err).ToResp(), nil
	}

	var req = apistructs.PagingAPIAssetVersionsReq{
		OrgID:       orgID,
		Identity:    &identityInfo,
		URIParams:   &uriParams,
		QueryParams: &queryParams,
	}

	versions, userIDs, err := e.assetSvc.PagingAPIAssetVersions(ctx, &req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(versions, userIDs)
}

// GetAPIAssetVersion 查询 API 资产版本详情
func (e *Endpoints) GetAPIAssetVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetAPIAssetVersion.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetAPIAssetVersion.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var uriParams = apistructs.AssetVersionDetailURI{
		AssetID:   vars[urlPathAssetID],
		VersionID: vars[urlPathVersionID],
	}

	var queryParams apistructs.GetAPIAssetVersionQueryParams
	if err := e.queryStringDecoder.Decode(&queryParams, r.URL.Query()); err != nil {
		return apierrors.GetAPIAssetVersion.InvalidParameter(err).ToResp(), nil

	}

	req := apistructs.GetAPIAssetVersionReq{
		OrgID:       orgID,
		Identity:    &identityInfo,
		URIParams:   &uriParams,
		QueryParams: &queryParams,
	}

	response, err := e.assetSvc.GetAssetVersion(&req)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	var userIDs []string
	if response.Version != nil {
		userIDs = append(userIDs, response.Version.CreatorID, response.Version.UpdaterID)
	}
	if response.Asset != nil {
		userIDs = append(userIDs, response.Asset.CreatorID, response.Asset.UpdaterID)
	}
	if response.Spec != nil {
		userIDs = append(userIDs, response.Spec.CreatorID, response.Asset.UpdaterID)
	}

	return httpserver.OkResp(response, strutil.DedupSlice(userIDs))
}

// DeleteAPIAssetVersion 删除 API Version
func (e *Endpoints) DeleteAPIAssetVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.DeleteAPIAssetVersion.ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.DeleteAPIAssetVersion.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	versionID, err := strconv.ParseUint(vars[urlPathVersionID], 10, 64)
	if err != nil {
		return apierrors.DeleteAPIAssetVersion.InvalidParameter(err).ToResp(), nil
	}
	if err = e.assetSvc.DeleteAssetVersionByID(ctx, orgID, vars[urlPathAssetID], versionID, identity.UserID); err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(nil)
}

// DownloadSpecText is the endpoint for downloading swagger text file
func (e *Endpoints) DownloadSpecText(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) (err error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.DownloadSpecText.NotLogin().Write(w)
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.DownloadSpecText.MissingParameter(apierrors.MissingOrgID).Write(w)
	}

	versionID, err := strconv.ParseUint(vars[urlPathVersionID], 10, 64)
	if err != nil {
		return apierrors.DownloadSpecText.InvalidParameter(err).Write(w)
	}

	var req = apistructs.DownloadSpecTextReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.DownloadSpecTextURIParams{
			AssetID:   vars[urlPathAssetID],
			VersionID: versionID,
		},
		QueryParams: &apistructs.DownloadSpecTextQueryParams{SpecProtocol: r.URL.Query().Get("specProtocol")},
	}

	data, apiError := e.assetSvc.DownloadSpecText(&req)
	if apiError != nil {
		return apiError.Write(w)
	}

	w.Header().Add("Content-Type", "text/plain")
	w.Header().Add("Content-Disposition", Attachment(req.URIParams.AssetID, strconv.FormatUint(req.URIParams.VersionID, 10),
		req.QueryParams.SpecProtocol))

	if _, err = w.Write(data); err != nil {
		w.Header().Del("Content-Disposition")
		return apierrors.DownloadSpecText.InternalError(err).Write(w)
	}

	return nil
}

// 修改版本 (标记为不建议使用)
func (e *Endpoints) UpdateAssetVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.UpdateAssetVersion.NotLogin().ToResp(), nil
	}
	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.UpdateAssetVersion.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	versionID, err := strconv.ParseUint(vars[urlPathVersionID], 10, 64)
	if err != nil {
		return apierrors.UpdateAssetVersion.InvalidParameter(err).ToResp(), nil
	}

	var req = apistructs.UpdateAssetVersionReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.AssetVersionDetailURI{
			AssetID:   vars[urlPathAssetID],
			VersionID: versionID,
		},
		Body: new(apistructs.UpdateAssetVersionBody),
	}

	if err := json.NewDecoder(r.Body).Decode(req.Body); err != nil {
		return apierrors.UpdateAssetVersion.InvalidParameter("无效的请求体").ToResp(), nil
	}

	data, apiError := e.assetSvc.UpdateAssetVersion(ctx, &req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(data)
}

// Attachment generates attachment by specProtocol
func Attachment(assetID, versionID, specProtocol string) string {
	v := "oas3"
	suffix := "yaml"
	if strings.HasPrefix(specProtocol, "oas2") {
		v = "oas2"
	}
	if strings.HasSuffix(specProtocol, "json") {
		suffix = "json"
	}
	if specProtocol == "csv" {
		suffix = "csv"
	}
	filename := assetID + "-" + versionID + "-" + v + "." + suffix
	return "attachment; filename=" + strconv.Quote(filename)
}
