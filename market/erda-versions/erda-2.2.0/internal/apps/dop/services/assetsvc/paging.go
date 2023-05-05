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
	"strings"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

// PagingAsset 分页查询 API 资料
func (svc *Service) PagingAsset(req apistructs.PagingAPIAssetsReq) (*apistructs.APIAssetPagingResponse, error) {
	// 参数校验
	if req.QueryParams == nil {
		return nil, apierrors.PagingAPIAssets.InvalidParameter(errors.New("can not load query parameters"))
	}
	if req.OrgID == 0 {
		return nil, apierrors.PagingAPIAssets.MissingParameter(apierrors.MissingOrgID)
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

	// 分步骤查询
	var (
		sq         = dbclient.Sq().Where("org_id = ?", req.OrgID)
		workspaces = make(map[string]string)
		assets     []*apistructs.APIAssetsModel
		total      uint64
	)
	// 1) 如果要求关联了实例化记录
	if req.QueryParams.Instantiation {
		var (
			instantiations []*apistructs.InstantiationModel
			assetIDs       []string
		)

		if err := svc.ListRecords(&instantiations, map[string]interface{}{
			"org_id": req.OrgID,
		}); err != nil {
			logrus.Errorf("failed to ListRecords instantiations")
			if gorm.IsRecordNotFoundError(err) {
				return nil, nil
			}
			return nil, apierrors.PagingAPIAssets.InternalError(err)
		}
		for _, v := range instantiations {
			assetIDs = append(assetIDs, v.AssetID)
			workspaces[v.AssetID] = v.Workspace
		}

		sq = sq.Where("asset_id IN (?)", assetIDs)
	}

	// 2) 如果要求关联了项目
	if req.QueryParams.HasProject {
		sq = sq.Where("project_id IS NOT NULL").
			Where("project_id != 0")
	}

	// 3) 如果需要搜索
	if req.QueryParams.Keyword != "" {
		var (
			specs    []*apistructs.APIAssetVersionSpecsModel
			assetIDs []string
			keyword  = strutil.Concat("%", req.QueryParams.Keyword, "%")
		)
		if err := dbclient.Sq().Where("MATCH(spec) AGAINST(?)", req.QueryParams.Keyword).Find(&specs).Error; err == nil {
			for _, v := range specs {
				assetIDs = append(assetIDs, v.AssetID)
			}
		}
		sq = sq.Where("asset_id = ? OR asset_name LIKE ? OR asset_id IN (?)", req.QueryParams.Keyword, keyword, assetIDs)
	}

	// 4) 如果查询"我负责的", 则查询 我创建的 或 我有 W 权限的; 否则还可查 公开的 和有 R 权限的
	// 用户角色权限
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	var (
		proWList = rolesSet.RolesProjects(bdl.ProMRoles...) // 项目管理人员可写
		appWList = rolesSet.RolesApps(bdl.AppMRoles...)     // 应用管理人员可写
		orgList  = rolesSet.RolesOrgs(bdl.OrgMRoles...)     // 企业管理人员可以读
		proList  = rolesSet.RolesProjects()                 // 项目关联的人员可以读
		appList  = rolesSet.RolesApps()                     // 应用关联的人员可以读

		scopeWhere = "? = creator_id OR org_id IN (?) OR project_id IN (?) OR app_id IN (?) "
	)
	if strings.ToLower(req.QueryParams.Scope) == "mine" {
		// 如果查询的是"我负责的"列表, 则要求用户对关联的项目和应用有 W 权限
		proList = proWList
		appList = appWList
	} else {
		scopeWhere += " OR public = true"
	}
	sq = sq.Where(scopeWhere, req.Identity.UserID, orgList, proList, appList)

	// 分页查询
	if err := sq.Limit(req.QueryParams.PageSize).Offset((req.QueryParams.PageNo - 1) * req.QueryParams.PageSize).
		Order("updated_at DESC").
		Find(&assets).
		Limit(-1).Offset(0).
		Count(&total).Error; err != nil {
		logrus.Errorf("failed to Find assets, err: %v", err)
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, apierrors.PagingAPIAssets.InternalError(err)
	}

	var (
		results []*apistructs.PagingAssetRspObj
		userIDs []string
	)
	// userIDs, 按钮权限
	for _, v := range assets {
		userIDs = append(userIDs, v.CreatorID)

		written := strings.ToLower(req.QueryParams.Scope) == "mine" || writePermission(rolesSet, v)

		results = append(results, &apistructs.PagingAssetRspObj{
			Asset:         v,
			LatestVersion: nil,
			LatestSpec:    nil,
			Permission: map[string]bool{
				"manage":     written,
				"addVersion": written,
				"hasAccess":  svc.assetHasAccess(req.OrgID, v.AssetID),
			},
		})
	}

	// 如果要求响应结果带上 latest version
	if req.QueryParams.LatestVersion {
		for _, v := range results {
			version, err := dbclient.QueryAPILatestVersion(req.OrgID, v.Asset.AssetID)
			if err != nil {
				if gorm.IsRecordNotFoundError(err) {
					continue
				}
				return nil, apierrors.PagingAPIAssets.InternalError(err)
			}
			v.LatestVersion = version
		}
	}

	// 如果要求响应结果带上 latest spec
	if req.QueryParams.LatestSpec {
		for _, v := range results {
			spec, err := dbclient.QueryVersionLatestSpec(req.OrgID, v.LatestVersion.ID)
			if err != nil {
				if gorm.IsRecordNotFoundError(err) {
					continue
				}
				return nil, apierrors.PagingAPIAssets.InternalError(err)
			}
			v.LatestSpec = spec
		}
	}

	pagingResult := apistructs.APIAssetPagingResponse{
		Total:   total,
		List:    results,
		UserIDs: strutil.DedupSlice(userIDs, true),
	}

	return &pagingResult, nil
}

func (svc *Service) assetHasAccess(orgID uint64, assetID string) bool {
	err := dbclient.Sq().First(new(apistructs.APIAccessesModel), map[string]interface{}{
		"org_id":   orgID,
		"asset_id": assetID,
	}).Error
	return err == nil
}
