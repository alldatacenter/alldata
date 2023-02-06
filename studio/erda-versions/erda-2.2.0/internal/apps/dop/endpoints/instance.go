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

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

// 实例化 (即创建一个 instantiation)
func (e *Endpoints) CreateInstantiation(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.CreateInstantiation.NotLogin().ToResp(), err
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.CreateInstantiation.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	var rb apistructs.CreateInstantiationBody
	if err := json.NewDecoder(r.Body).Decode(&rb); err != nil {
		return apierrors.CreateInstantiation.InvalidParameter(err).ToResp(), nil
	}

	minor, err := strconv.ParseUint(vars[urlPathMinor], 10, 64)
	if err != nil {
		return apierrors.CreateInstantiation.InvalidParameter("URI parameter 'minor' must be uint").ToResp(), nil
	}

	var req = apistructs.CreateInstantiationReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.CreateInstantiationURIParams{
			AssetID:        vars[urlPathAssetID],
			SwaggerVersion: vars[urlPathSwaggerVersion],
			Minor:          minor,
		},
		Body: &rb,
	}

	instantiation, apiError := e.assetSvc.CreateInstantiation(ctx, &req)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	var rsp = apistructs.GetInstantiationRsp{
		InstantiationModel: *instantiation,
		ProjectName:        "",
		RuntimeName:        "",
	}

	// 查询 projectName
	if project, err := e.assetSvc.GetProject(instantiation.ProjectID); err == nil {
		rsp.ProjectName = project.Name
	}

	if instantiation.Type != "dice" {
		return httpserver.OkResp(map[string]interface{}{
			"instantiation": rsp,
		})
	}

	if services, err := e.assetSvc.GetRuntimeServices(instantiation.RuntimeID, req.OrgID, req.Identity.UserID); err == nil {
		rsp.RuntimeName = services.Name
	}

	return httpserver.OkResp(map[string]interface{}{
		"instantiation": rsp,
		"permission": map[string]bool{
			"edit": true,
		},
	})
}

// 查询 minor 下的 instantiation
func (e *Endpoints) GetInstantiations(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetInstantiations.NotLogin().ToResp(), err
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetInstantiations.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	minor, err := strconv.ParseUint(vars[urlPathMinor], 10, 64)
	if err != nil {
		return apierrors.CreateInstantiation.InvalidParameter("URI parameter 'minor' must be uint").ToResp(), nil
	}

	var req = apistructs.GetInstantiationsReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.GetInstantiationsURIParams{
			AssetID:        vars[urlPathAssetID],
			SwaggerVersion: vars[urlPathSwaggerVersion],
			Minor:          minor,
		},
	}

	instantiation, ok, apiErr := e.assetSvc.GetInstantiation(&req)
	if apiErr != nil {
		return apiErr.ToResp(), nil
	}
	if !ok {
		return httpserver.OkResp(nil)
	}

	var rsp = apistructs.GetInstantiationRsp{
		InstantiationModel: *instantiation,
		ProjectName:        "",
		RuntimeName:        "",
	}

	// 查询 projectName
	if project, err := e.assetSvc.GetProject(instantiation.ProjectID); err == nil {
		rsp.ProjectName = project.Name
	}

	// 查询 access
	hasAccess := e.assetSvc.FirstRecord(new(apistructs.APIAccessesModel), map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
		"minor":           req.URIParams.Minor,
	}) == nil

	if instantiation.Type != "dice" {
		return httpserver.OkResp(map[string]interface{}{
			"instantiation": rsp,
		})
	}

	if services, err := e.assetSvc.GetRuntimeServices(instantiation.RuntimeID, req.OrgID, req.Identity.UserID); err == nil {
		rsp.RuntimeName = services.Name
	}

	return httpserver.OkResp(map[string]interface{}{
		"instantiation": rsp,
		"permission":    map[string]bool{"edit": !hasAccess},
	})
}

// 修改实例
func (e *Endpoints) UpdateInstantiation(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.UpdateInstantiation.ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.GetInstantiations.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	minor, err := strconv.ParseUint(vars[urlPathMinor], 10, 64)
	if err != nil {
		return apierrors.UpdateInstantiation.InvalidParameter("URI paramter 'minor' must be uint").ToResp(), nil
	}

	instantiationID, err := strconv.ParseUint(vars[urlPathInstantiationID], 10, 64)
	if err != nil {
		return apierrors.GetInstantiations.InvalidParameter("instantiationID is invalid").ToResp(), nil
	}

	var body apistructs.UpdateInstantiationBody
	if err = json.NewDecoder(r.Body).Decode(&body); err != nil {
		return apierrors.GetInstantiations.InvalidParameter("request body is invalid").ToResp(), nil
	}

	var req = apistructs.UpdateInstantiationReq{
		OrgID:    orgID,
		Identity: &identity,
		URIParams: &apistructs.UpdateInstantiationURIParams{
			AssetID:         vars[urlPathAssetID],
			SwaggerVersion:  vars[urlPathSwaggerVersion],
			Minor:           minor,
			InstantiationID: instantiationID,
		},
		Body: &body,
	}

	instantiation, apiErr := e.assetSvc.UpdateInstantiation(ctx, &req)
	if apiErr != nil {
		return apiErr.ToResp(), nil
	}
	if instantiation == nil {
		return httpserver.OkResp(map[string]interface{}{
			"instantiation": nil,
		})
	}

	var rsp = apistructs.GetInstantiationRsp{
		InstantiationModel: *instantiation,
		ProjectName:        "",
		RuntimeName:        "",
	}

	// 查询 projectName
	if project, err := e.assetSvc.GetProject(instantiation.ProjectID); err == nil {
		rsp.ProjectName = project.Name
	}

	if instantiation.Type != "dice" {
		return httpserver.OkResp(map[string]interface{}{
			"instantiation": rsp,
			"permission":    map[string]bool{"edit": true},
		})
	}

	if services, err := e.assetSvc.GetRuntimeServices(instantiation.RuntimeID, req.OrgID, req.Identity.UserID); err == nil {
		rsp.RuntimeName = services.Name
	}

	return httpserver.OkResp(map[string]interface{}{
		"instantiation": rsp,
		"permission":    map[string]bool{"edit": true},
	})
}

// 拉取 application 下的服务的地址
func (e *Endpoints) ListRuntimeServices(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	identity, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ListRuntimeServices.ToResp(), nil
	}

	orgID, err := user.GetOrgID(r)
	if err != nil {
		return apierrors.ListRuntimeServices.MissingParameter(apierrors.MissingOrgID).ToResp(), nil
	}

	applicationID, err := strconv.ParseUint(vars["appID"], 10, 64)
	if err != nil {
		return apierrors.ListRuntimeServices.InvalidParameter(errors.Wrap(err, "applicationID invalid")).ToResp(), nil
	}

	data, apiError := e.assetSvc.GetApplicationServices(applicationID, orgID, identity.UserID)
	if apiError != nil {
		return apiError.ToResp(), nil
	}

	return httpserver.OkResp(data)
}
