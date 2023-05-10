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

package assetsvc

import (
	"context"
	"sort"
	"strconv"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	AddonNameAPIGateway = "api-gateway"
)

const (
	WorkspaceDev     = "DEV"
	WorkspaceTest    = "TEST"
	WorkspaceStaging = "STAGING"
	WorkspaceProd    = "PROD"
)

// PagingAPIAssetVersions 获取 API 资料版本列表
func (svc *Service) PagingAPIAssetVersions(ctx context.Context, req *apistructs.PagingAPIAssetVersionsReq) (responseData *apistructs.PagingAPIAssetVersionResponse, userIDs []string, err error) {
	// 参数校验
	if req.QueryParams == nil {
		return nil, nil, errors.New("missing query parameters")
	}
	if req.URIParams == nil {
		return nil, nil, errors.New("missing URI parameters")
	}
	if err := apistructs.ValidateAPIAssetID(req.URIParams.AssetID); err != nil {
		return nil, nil, apierrors.PagingAPIAssetVersions.InvalidParameter(err)
	}
	if req.OrgID == 0 {
		return nil, nil, apierrors.PagingAPIAssetVersions.MissingParameter(apierrors.MissingOrgID)
	}

	// 参数初始化
	if req.QueryParams.PageNo < 1 {
		req.QueryParams.PageNo = 1
	}
	if req.QueryParams.PageSize < 1 {
		req.QueryParams.PageSize = 1
	}
	if req.QueryParams.PageSize > 500 {
		req.QueryParams.PageSize = 500
	}
	if !req.QueryParams.Paging {
		req.QueryParams.PageNo = 1
		req.QueryParams.PageSize = 500
	}

	// 查询 asset
	var asset apistructs.APIAssetsModel
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return nil, nil, apierrors.PagingAPIAssetVersions.InternalError(errors.New(svc.text(ctx, "FailedToFindAPIAsset")))
	}

	// 查询 versions
	total, versionsModels, err := dbclient.PagingAPIAssetVersions(req)
	if err != nil {
		return nil, nil, apierrors.PagingAPIAssetVersions.InternalError(err)
	}

	var (
		versionIDs []string
		objsM      = make(map[uint64]*apistructs.PagingAPIAssetVersionRspObj)
		list       []*apistructs.PagingAPIAssetVersionRspObj
		permission = bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
		written    = writePermission(permission, &asset)
	)
	for _, v := range versionsModels {
		obj := apistructs.PagingAPIAssetVersionRspObj{
			Version:    v,
			Spec:       nil,
			Permission: map[string]bool{"edit": written, "delete": written},
		}
		if req.QueryParams.Spec {
			versionIDs = append(versionIDs, strconv.FormatInt(int64(v.ID), 10))
		}
		objsM[v.ID] = &obj
		list = append(list, &obj)
		userIDs = append(userIDs, v.CreatorID, v.UpdaterID)
	}

	// 如果要求查询 specs, 则查询 specs
	if req.QueryParams.Spec {
		if err := dbclient.QuerySpecsFromVersions(req.OrgID, req.URIParams.AssetID, versionIDs, objsM); err != nil {
			return nil, nil, err
		}
	}

	return &apistructs.PagingAPIAssetVersionResponse{
		Total: total,
		List:  list,
	}, strutil.DedupSlice(userIDs, true), nil
}

func (svc *Service) ListSwaggerVersions(req *apistructs.ListSwaggerVersionsReq) (*apistructs.ListSwaggerVersionRsp, error) {
	// 参数校验
	if req == nil {
		return nil, errors.New("missing request parameters")
	}
	if req.QueryParams == nil {
		return nil, errors.New("missing query parameters")
	}
	if req.URIParams == nil {
		return nil, errors.New("missing URI parameters")
	}

	switch {
	case req.QueryParams.Patch && req.QueryParams.Access:
		return svc.listSwaggerVersionOnPatchWithAccess(req)
	case req.QueryParams.Patch && req.QueryParams.Instantiation:
		return svc.listSwaggerVersionOnPatchWithInstantiation(req)
	case req.QueryParams.Patch:
		return svc.listSwaggerVersionOnPatch(req)
	case req.QueryParams.Access:
		return svc.listSwaggerVersionsOnMinorWithAccess(req)
	case req.QueryParams.Instantiation:
		return svc.listSwaggerVersionsOnMinorWithInstantiation(req)
	default:
		return svc.listSwaggerVersionOnMinor(req)
	}
}

func (svc *Service) listSwaggerVersionOnPatch(req *apistructs.ListSwaggerVersionsReq) (*apistructs.ListSwaggerVersionRsp, error) {
	var versions []*apistructs.APIAssetVersionsModel
	if err := svc.ListRecords(&versions, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		return nil, err
	}

	var m = make(map[string]*apistructs.ListSwaggerVersionRspObj)
	svc.collectListSwaggerVersionRspObj(versions, m)
	results := svc.swaggerVersionsResults(m)
	return &apistructs.ListSwaggerVersionRsp{
		Total: uint64(len(results)),
		List:  results,
	}, nil
}

func (svc *Service) listSwaggerVersionOnPatchWithAccess(req *apistructs.ListSwaggerVersionsReq) (*apistructs.ListSwaggerVersionRsp, error) {
	var models []*apistructs.APIAccessesModel
	if err := svc.ListRecords(&models, map[string]interface{}{"asset_id": req.URIParams.AssetID}); err != nil {
		return nil, err
	}

	var m = make(map[string]*apistructs.ListSwaggerVersionRspObj)
	for _, v := range models {
		var versions []*apistructs.APIAssetVersionsModel
		if err := svc.ListRecords(&versions, map[string]interface{}{
			"org_id":   req.OrgID,
			"asset_id": req.URIParams.AssetID,
			"major":    v.Major,
			"minor":    v.Minor,
		}); err != nil {
			continue
		}

		svc.collectListSwaggerVersionRspObj(versions, m)
	}

	results := svc.swaggerVersionsResults(m)

	return &apistructs.ListSwaggerVersionRsp{
		Total: uint64(len(results)),
		List:  results,
	}, nil

}

func (svc *Service) listSwaggerVersionOnPatchWithInstantiation(req *apistructs.ListSwaggerVersionsReq) (*apistructs.ListSwaggerVersionRsp, error) {
	var instantiations []*apistructs.InstantiationModel
	if err := dbclient.Sq().Where(map[string]interface{}{
		"asset_id": req.URIParams.AssetID,
	}).Find(&instantiations).Error; err != nil {
		return nil, err
	}

	var m = make(map[string]*apistructs.ListSwaggerVersionRspObj)
	for _, v := range instantiations {
		var versions []*apistructs.APIAssetVersionsModel
		if err := svc.ListRecords(&versions, map[string]interface{}{
			"org_id":   req.OrgID,
			"asset_id": req.URIParams.AssetID,
			"major":    v.Major,
			"minor":    v.Minor,
		}); err != nil {
			continue
		}

		svc.collectListSwaggerVersionRspObj(versions, m)
	}

	results := svc.swaggerVersionsResults(m)

	return &apistructs.ListSwaggerVersionRsp{
		Total: uint64(len(results)),
		List:  results,
	}, nil
}

func (svc *Service) collectListSwaggerVersionRspObj(versions []*apistructs.APIAssetVersionsModel, m map[string]*apistructs.ListSwaggerVersionRspObj) {
	for _, v := range versions {
		record := map[string]interface{}{
			"id":         v.ID,
			"major":      v.Major,
			"minor":      v.Minor,
			"patch":      v.Patch,
			"deprecated": v.Deprecated,
		}
		if obj, ok := m[v.SwaggerVersion]; ok {
			obj.Versions = append(obj.Versions, record)
			continue
		}
		m[v.SwaggerVersion] = &apistructs.ListSwaggerVersionRspObj{
			SwaggerVersion: v.SwaggerVersion,
			Major:          v.Major,
			Versions:       []map[string]interface{}{record},
		}
	}
}

func (svc *Service) listSwaggerVersionOnMinor(req *apistructs.ListSwaggerVersionsReq) (*apistructs.ListSwaggerVersionRsp, error) {
	var (
		versions []*apistructs.APIAssetVersionsModel
		where    = map[string]interface{}{
			"org_id":   req.OrgID,
			"asset_id": req.URIParams.AssetID,
		}
	)
	if err := svc.ListRecords(&versions, where); err != nil {
		return nil, err
	}

	var m = make(map[string]*apistructs.ListSwaggerVersionRspObj)
	for _, version := range versions {
		record := map[string]interface{}{
			"major":      version.Major,
			"minor":      version.Minor,
			"patch":      version.Patch,
			"id":         version.ID,
			"deprecated": version.Deprecated,
		}

		if obj, ok := m[version.SwaggerVersion]; ok {
			obj.Versions = append(obj.Versions, record)
			continue
		}
		m[version.SwaggerVersion] = &apistructs.ListSwaggerVersionRspObj{
			SwaggerVersion: version.SwaggerVersion,
			Major:          version.Major,
			Versions:       []map[string]interface{}{record},
		}
	}

	results := svc.swaggerVersionsResults(m)
	return &apistructs.ListSwaggerVersionRsp{
		Total: uint64(len(results)),
		List:  results,
	}, nil
}

func (svc *Service) listSwaggerVersionsOnMinorWithInstantiation(req *apistructs.ListSwaggerVersionsReq) (*apistructs.ListSwaggerVersionRsp, error) {
	var instantiations []*apistructs.InstantiationModel
	if err := dbclient.Sq().Where(map[string]interface{}{
		"asset_id": req.URIParams.AssetID,
	}).Find(&instantiations).Error; err != nil {
		return nil, err
	}

	var m = make(map[string]*apistructs.ListSwaggerVersionRspObj)

	for _, instantiation := range instantiations {
		var version apistructs.APIAssetVersionsModel
		if err := dbclient.Sq().Where(map[string]interface{}{
			"org_id":   req.OrgID,
			"asset_id": req.URIParams.AssetID,
			"major":    instantiation.Major,
			"minor":    instantiation.Minor,
		}).Order("patch DESC").First(&version).Error; err != nil {
			continue
		}
		record := map[string]interface{}{
			"major":      version.Major,
			"minor":      version.Minor,
			"patch":      version.Patch,
			"id":         version.ID,
			"deprecated": version.Deprecated,
		}

		if obj, ok := m[instantiation.SwaggerVersion]; ok {
			obj.Versions = append(obj.Versions, record)
			continue
		}
		m[instantiation.SwaggerVersion] = &apistructs.ListSwaggerVersionRspObj{
			SwaggerVersion: instantiation.SwaggerVersion,
			Major:          version.Major,
			Versions:       []map[string]interface{}{record},
		}
	}

	results := svc.swaggerVersionsResults(m)

	return &apistructs.ListSwaggerVersionRsp{
		Total: uint64(len(results)),
		List:  results,
	}, nil
}

func (svc *Service) listSwaggerVersionsOnMinorWithAccess(req *apistructs.ListSwaggerVersionsReq) (*apistructs.ListSwaggerVersionRsp, error) {
	var accesses []*apistructs.APIAccessesModel
	if err := dbclient.Sq().Where(map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}).Find(&accesses).Error; err != nil {
		return nil, err
	}

	var m = make(map[string]*apistructs.ListSwaggerVersionRspObj)

	for _, access := range accesses {
		var version apistructs.APIAssetVersionsModel
		if err := dbclient.Sq().Where(map[string]interface{}{
			"org_id":   req.OrgID,
			"asset_id": req.URIParams.AssetID,
			"major":    access.Major,
			"minor":    access.Minor,
		}).Order("patch DESC").First(&version).Error; err != nil {
			continue
		}
		record := map[string]interface{}{
			"major":      version.Major,
			"minor":      version.Minor,
			"patch":      version.Patch,
			"id":         version.ID,
			"deprecated": version.Deprecated,
		}

		if obj, ok := m[access.SwaggerVersion]; ok {
			obj.Versions = append(obj.Versions, record)
			continue
		}
		m[access.SwaggerVersion] = &apistructs.ListSwaggerVersionRspObj{
			SwaggerVersion: access.SwaggerVersion,
			Major:          version.Major,
			Versions:       []map[string]interface{}{record},
		}
	}

	results := svc.swaggerVersionsResults(m)

	return &apistructs.ListSwaggerVersionRsp{
		Total: uint64(len(results)),
		List:  results,
	}, nil
}

func (svc *Service) swaggerVersionsResults(m map[string]*apistructs.ListSwaggerVersionRspObj) []*apistructs.ListSwaggerVersionRspObj {
	var results []*apistructs.ListSwaggerVersionRspObj
	for _, v := range m {
		v.Versions = groupByMinor(v.Versions)
		results = append(results, v)
	}
	sort.Slice(results, func(i, j int) bool {
		return results[i].Major > results[j].Major
	})

	return results
}

// 按 minor 去重分组
func groupByMinor(records []map[string]interface{}) []map[string]interface{} {
	if len(records) < 2 {
		return records
	}

	var (
		m      = make(map[uint64]map[string]interface{})
		result []map[string]interface{}
	)
	for _, record := range records {
		minor := record["minor"].(uint64)
		if r, ok := m[minor]; ok && record["patch"].(uint64) < r["patch"].(uint64) {
			m[minor] = r
		} else {
			m[minor] = record
		}
	}

	for _, record := range m {
		result = append(result, record)
	}

	sort.Slice(result, func(i, j int) bool {
		return result[i]["minor"].(uint64) > result[j]["minor"].(uint64)
	})

	return result
}

func (svc *Service) ListMyClients(req *apistructs.ListMyClientsReq) (*apistructs.ListMyClientsRsp, *errorresp.APIError) {
	// 参数校验
	if req == nil {
		return nil, apierrors.ListClients.InvalidParameter("missing parameters")
	}
	if req.QueryParams == nil {
		return nil, apierrors.ListClients.MissingParameter("missing parameters")
	}

	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	orgManager := inSlice(strconv.FormatUint(req.OrgID, 10), rolesSet.RolesOrgs(bdl.OrgMRoles...))
	total, models, err := dbclient.ListMyClients(req, orgManager)
	if err != nil {
		return nil, apierrors.ListClients.InternalError(err)
	}

	var list []*apistructs.ClientObj
	for _, v := range models {
		credentials, err := bdl.Bdl.GetClientCredentials(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID, v.ClientID)
		if err != nil {
			return nil, apierrors.ListClients.InternalError(err)
		}
		list = append(list, &apistructs.ClientObj{
			Client: v,
			SK: &apistructs.SK{
				ClientID:     v.ClientID,
				ClientSecret: credentials.ClientSecret,
			},
		})
	}

	return &apistructs.ListMyClientsRsp{
		Total: total,
		List:  list,
	}, nil
}

func (svc *Service) ListContracts(ctx context.Context, req *apistructs.ListContractsReq) (*apistructs.ListContractsRsp, *errorresp.APIError) {
	// 参数校验
	if req == nil || req.QueryParams == nil || req.URIParams == nil {
		return nil, apierrors.ListContracts.InvalidParameter("missing parameters")
	}

	// 参数初始化
	if !req.QueryParams.Paging {
		req.QueryParams.PageNo = 1
		req.QueryParams.PageSize = 500
	}
	if req.QueryParams.PageNo < 1 {
		req.QueryParams.PageNo = 1
	}
	if req.QueryParams.PageSize < 1 {
		req.QueryParams.PageSize = 10
	}
	if req.QueryParams.PageSize > 500 {
		req.QueryParams.PageSize = 500
	}
	if len(req.QueryParams.Status) == 0 {
		req.QueryParams.Status = []apistructs.ContractStatus{
			apistructs.ContractApproving, apistructs.ContractApproved,
			apistructs.ContractDisapproved, apistructs.ContractUnapproved,
		}
	}

	total, list, err := dbclient.ListContracts(ctx, svc.trans, req)
	if err != nil {
		return nil, apierrors.ListContracts.InternalError(err)
	}

	for _, c := range list {
		var access apistructs.APIAccessesModel
		if err := svc.FirstRecord(&access, map[string]interface{}{
			"org_id":          req.OrgID,
			"asset_id":        c.AssetID,
			"swagger_version": c.SwaggerVersion,
		}); err != nil {
			continue
		}

		c.ProjectID = access.ProjectID
		c.Workspace = access.Workspace

		endpoint, err := bdl.Bdl.GetEndpoint(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID, access.EndpointID)
		if err != nil {
			continue
		}
		c.EndpointName = endpoint.Name
	}

	return &apistructs.ListContractsRsp{
		Total: total,
		List:  list,
	}, nil
}

func (svc *Service) ListContractRecords(req *apistructs.ListContractRecordsReq) (*apistructs.ListContractRecordsRsp, *errorresp.APIError) {
	// 参数校验
	if req == nil || req.URIParams == nil || req.OrgID == 0 {
		return nil, apierrors.ListContractRecords.InvalidParameter("invalid parameters")
	}

	models, err := dbclient.ListContractRecords(req)
	if err != nil {
		return nil, apierrors.ListContractRecords.InternalError(err)
	}

	return &apistructs.ListContractRecordsRsp{
		Total: uint64(len(models)),
		List:  models,
	}, nil
}

func (svc *Service) ListAccess(req *apistructs.ListAccessReq) (*apistructs.ListAccessRsp, *errorresp.APIError) {
	// 参数校验
	if req == nil || req.QueryParams == nil {
		return nil, apierrors.ListAccess.InvalidParameter("invalid parameters")
	}
	if req.OrgID == 0 {
		return nil, apierrors.ListAccess.InvalidParameter("invalid orgID")
	}

	// 先查出 "我负责的" asset
	// 再限定只能查这些 asset 关联的 access
	asset, _ := svc.PagingAsset(apistructs.PagingAPIAssetsReq{
		OrgID:    req.OrgID,
		Identity: req.Identity,
		QueryParams: &apistructs.PagingAPIAssetsQueryParams{
			Paging:        false,
			PageNo:        0,
			PageSize:      0,
			Keyword:       "",
			Scope:         "mine",
			HasProject:    false,
			LatestVersion: false,
			LatestSpec:    false,
			Instantiation: false,
		},
	})
	if asset.Total == 0 || len(asset.List) == 0 {
		return &apistructs.ListAccessRsp{
			OrgID: req.OrgID,
			List:  nil,
			Total: 0,
		}, nil
	}

	// "我负责的" asset 列表
	var responsibleAssetIDs []string
	for _, v := range asset.List {
		if v.Asset != nil {
			responsibleAssetIDs = append(responsibleAssetIDs, v.Asset.AssetID)
		}
	}

	total, list, err := dbclient.ListAccess(req, responsibleAssetIDs)
	if err != nil {
		return nil, apierrors.ListAccess.InternalError(err)
	}

	// 按钮权限
	for _, obj := range list {
		// 查 asset model
		var asset apistructs.APIAssetsModel
		if err := svc.FirstRecord(&asset, map[string]interface{}{
			"org_id":   req.OrgID,
			"asset_id": obj.AssetID,
		}); err != nil {
			return nil, apierrors.ListAccess.InternalError(err)
		}

		for _, item := range obj.Children {
			// access 写权限的角色集合继承了 asset 的写权限角色集合
			written := svc.writeAssetPermission(req.OrgID, req.Identity.UserID, asset.AssetID)
			item.Permission["edit"] = written
			item.Permission["delete"] = written
		}
	}

	return &apistructs.ListAccessRsp{
		OrgID: req.OrgID,
		List:  list,
		Total: total,
	}, nil
}

func (svc *Service) ListSwaggerVersionClients(ctx context.Context, req *apistructs.ListSwaggerVersionClientsReq) (*apistructs.ListSwaggerVersionClientRsp, *errorresp.APIError) {
	// 参数校验
	if req == nil || req.URIParams == nil || req.QueryParams == nil {
		return nil, apierrors.ListAccess.InvalidParameter("invalid parameters")
	}
	if req.OrgID == 0 {
		return nil, apierrors.ListAccess.InvalidParameter("invalid orgID")
	}

	data, err := dbclient.ListSwaggerVersionClients(ctx, svc.trans, req)
	if err != nil {
		return nil, apierrors.ListAccess.InternalError(err)
	}

	// 按钮权限
	// 查询 asset
	var (
		asset  apistructs.APIAssetsModel
		access apistructs.APIAccessesModel
	)
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset")
		return nil, apierrors.ListSwaggerClients.InternalError(err)
	}
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord access")
		return nil, apierrors.ListSwaggerClients.InternalError(err)
	}

	permission := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	prove := writePermission(permission, &asset)
	for _, v := range data {
		v.Permission["edit"] = prove
	}

	return &apistructs.ListSwaggerVersionClientRsp{
		Total: uint64(len(data)),
		List:  data,
	}, nil
}

func (svc *Service) ListAPIGateways(req *apistructs.ListAPIGatewaysReq) ([]map[string]string, *errorresp.APIError) {
	if req == nil || req.URIParams == nil {
		return nil, apierrors.ListAPIGateways.InvalidParameter("invalid parameters")
	}
	if req.OrgID == 0 {
		return nil, apierrors.ListAPIGateways.InvalidParameter("invalid orgID")
	}

	asset, err := dbclient.GetAPIAsset(&apistructs.GetAPIAssetReq{
		OrgID:     req.OrgID,
		Identity:  req.Identity,
		URIParams: &apistructs.GetAPIAssetURIPrams{AssetID: req.URIParams.AssetID},
	})
	if err != nil {
		return nil, apierrors.ListAPIGateways.InternalError(err)
	}
	if asset.ProjectID == nil || *asset.ProjectID == 0 {
		return nil, apierrors.ListAPIGateways.InternalError(errors.New("no project is associated with the API asset"))
	}

	return svc.listProjectAPIGateways(strconv.FormatUint(*asset.ProjectID, 10)), nil
}

func (svc *Service) ListProjectAPIGateways(req *apistructs.ListProjectAPIGatewaysReq) ([]map[string]string, *errorresp.APIError) {
	if req == nil || req.URIParams == nil {
		return nil, apierrors.ListAPIGateways.InvalidParameter("invalid parameters")
	}
	if req.OrgID == 0 {
		return nil, apierrors.ListAPIGateways.InvalidParameter("invalid orgID")
	}
	if req.URIParams.ProjectID == "" {
		return nil, apierrors.ListAPIGateways.InvalidParameter("invalid projectID")
	}
	return svc.listProjectAPIGateways(req.URIParams.ProjectID), nil
}

func (svc *Service) listProjectAPIGateways(projectID string) []map[string]string {
	var result []map[string]string
	for _, workspace := range []string{WorkspaceDev, WorkspaceTest, WorkspaceStaging, WorkspaceProd} {
		resp, err := bdl.Bdl.ListByAddonName(AddonNameAPIGateway, projectID, workspace)
		if err != nil {
			logrus.Errorf("failed to ListByAddonName, err: %v", err)
			continue
		}
		if len(resp.Data) == 0 {
			logrus.Warnf("failed to get resp.Data, length is 0")
			continue
		}
		for _, v := range resp.Data {
			result = append(result, map[string]string{"addonInstanceID": v.InstanceID, "workspace": workspace, "status": v.Status})
		}
	}
	return result
}

func (svc *Service) GetApplicationServices(applicationID uint64, orgID uint64, userID string) ([]*apistructs.ListRuntimeServicesResp, *errorresp.APIError) {
	runtimes, err := bdl.Bdl.GetApplicationRuntimes(applicationID, orgID, conf.SuperUserID()) // 为了必定能获取到 runtimes, 使用这个特殊的 userID
	if err != nil {
		logrus.Errorf("failed to GetApplicationRuntimes, applicationID: %v, err: %v", applicationID, err)
	}

	var results []*apistructs.ListRuntimeServicesResp
	for _, runtime := range runtimes {
		services, err := bdl.Bdl.GetRuntimeServices(runtime.ID, orgID, userID)
		if err != nil {
			logrus.Errorf("failed to GetRuntimesServicesResp, runtimeID: %v, err: %v", runtime.ID, err)
			continue
		}
		for name, service := range services.Services {
			ele := &apistructs.ListRuntimeServicesResp{
				RuntimeID:     runtime.ID,
				RuntimeName:   runtime.Name,
				Workspace:     "",
				ProjectID:     services.ProjectID,
				AppID:         applicationID,
				ServiceName:   name,
				ServiceAddr:   service.Addrs,
				ServiceExpose: service.Expose,
			}
			if services.Extra != nil {
				ele.Workspace = services.Extra.Workspace
				ele.AppID = services.Extra.ApplicationId
			} else if runtime.Extra != nil {
				ele.Workspace = runtime.Extra.Workspace
				ele.AppID = runtime.Extra.ApplicationID
			}

			results = append(results, ele)
		}
	}

	return results, nil
}

func (svc *Service) GetRuntimeServices(runtimeID uint64, orgID uint64, userID string) (*bundle.GetRuntimeServicesResponseData, error) {
	return bdl.Bdl.GetRuntimeServices(runtimeID, orgID, userID)
}

// 查询 SLA 列表
func (svc *Service) ListSLAs(ctx context.Context, req *apistructs.ListSLAsReq) (*apistructs.ListSLAsRsp, *errorresp.APIError) {
	if req == nil || req.URIParams == nil {
		return nil, apierrors.ListSLAs.InvalidParameter(svc.text(ctx, "InvalidParams"))
	}
	if req.OrgID == 0 {
		return nil, apierrors.ListSLAs.InvalidParameter(svc.text(ctx, "InvalidParams") + ": OrgID")
	}

	// 查询 access
	var access apistructs.APIAccessesModel
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord access, err: %v", err)
		return nil, apierrors.ListSLAs.InternalError(errors.New(svc.text(ctx, "FailedToFindAccessItem")))
	}

	// 查询 SLAs 列表
	var slas []*apistructs.SLAModel
	if err := svc.ListRecords(&slas, map[string]interface{}{
		"access_id": access.ID,
	}); err != nil {
		logrus.Errorf("failed to ListRecords SLAs")
		if gorm.IsRecordNotFoundError(err) {
			return &apistructs.ListSLAsRsp{
				Total: 0,
				List:  nil,
			}, nil
		}
		return nil, apierrors.ListSLAs.InternalError(errors.New(svc.text(ctx, "FailedToListSLA")))
	}

	// 如果要求标识 SLA 与 contract 的关系, 则查询 contract
	var (
		contract     apistructs.ContractModel
		needContract = req.QueryParams != nil && req.QueryParams.ClientID != 0
	)
	if needContract {
		if err := svc.FirstRecord(&contract, map[string]interface{}{
			"org_id":          req.OrgID,
			"client_id":       req.QueryParams.ClientID,
			"asset_id":        req.URIParams.AssetID,
			"swagger_version": req.URIParams.SwaggerVersion,
		}); err != nil {
			logrus.Errorf("failed to FirstRecord contract, err: %v", err)
			return nil, apierrors.ListSLAs.InternalError(errors.New(svc.text(ctx, "FailedToFindContract")))
		}
	}

	var rsp = apistructs.ListSLAsRsp{
		Total: uint64(len(slas)),
		List:  make([]*apistructs.ListSLAsRspObj, len(slas)),
	}
	for i, sla := range slas {
		sla.Source = apistructs.SourceUser
		obj := &apistructs.ListSLAsRspObj{
			SLAModel:       *sla,
			Limits:         nil,
			AssetID:        access.AssetID,
			AssetName:      access.AssetName,
			SwaggerVersion: access.SwaggerVersion,
			UserTo:         "",
			Default:        access.DefaultSLAID != nil && sla.ID == *access.DefaultSLAID,
			ClientCount:    0,
		}

		// 查询每一个 SLA 的 limit
		var limits []*apistructs.SLALimitModel
		if err := svc.ListRecords(&limits, map[string]interface{}{
			"sla_id": sla.ID,
		}); err != nil && !gorm.IsRecordNotFoundError(err) {
			logrus.Errorf("failed to ListRecord limits, err: %v", err)
			return nil, apierrors.ListSLAs.InternalError(err)
		}
		obj.Limits = limits

		// 查询每一个 SLA 下的 客户端数
		var clientCount uint64
		if err := dbclient.Sq().Table(contract.TableName()).
			Where(map[string]interface{}{
				"org_id":          req.OrgID,
				"asset_id":        req.URIParams.AssetID,
				"swagger_version": req.URIParams.SwaggerVersion,
				"cur_sla_id":      sla.ID,
			}).
			Count(&clientCount).
			Error; err != nil {
			logrus.Errorf("failed to Count clientCount, err: %v", err)
		}
		obj.ClientCount = clientCount

		if needContract {
			switch slaID := sla.ID; {
			case contract.CurSLAID != nil && *contract.CurSLAID == slaID:
				obj.UserTo = apistructs.Current
			case contract.RequestSLAID != nil && *contract.CurSLAID == slaID:
				obj.UserTo = apistructs.Request
			}
		}

		rsp.List[i] = obj
	}

	sort.Slice(rsp.List, func(i, j int) bool {
		if rsp.List[i].Default {
			return true
		}
		if rsp.List[j].Default {
			return false
		}
		if rsp.List[i].Approval.ToLower() == apistructs.AuthorizationAuto {
			return true
		}
		if rsp.List[j].Approval.ToLower() == apistructs.AuthorizationAuto {
			return false
		}
		return rsp.List[i].UpdatedAt.After(rsp.List[j].UpdatedAt)
	})

	unlimiSLA := svc.unlimitedSLA(ctx, &access)
	var cnt uint64
	dbclient.Sq().Model(new(apistructs.ContractModel)).
		Where(map[string]interface{}{"asset_id": access.AssetID, "swagger_version": access.SwaggerVersion, "cur_sla_id": 0}).
		Count(&cnt)
	rsp.List = append([]*apistructs.ListSLAsRspObj{{
		SLAModel:       *unlimiSLA,
		Limits:         nil,
		AssetID:        access.AssetID,
		AssetName:      access.AssetName,
		SwaggerVersion: access.SwaggerVersion,
		UserTo:         "",
		Default:        false,
		ClientCount:    cnt,
	}}, rsp.List...)

	return &rsp, nil
}
