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
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/url"
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/getkin/kin-openapi/openapi3"
	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apidocsvc"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/swagger/oas2"
	"github.com/erda-project/erda/pkg/swagger/oas3"
	"github.com/erda-project/erda/pkg/swagger/oasconv"
)

const (
	InsTypeDice     = "dice"
	InsTypeExternal = "external"
)

func accessDetailPath(orgName string, accessPrimary uint64) string {
	id := strconv.FormatUint(accessPrimary, 10)
	return filepath.Join("/", orgName, "/workBench/apiManage/access-manage/detail", id)
}

func myClientsPath(orgName string, clientPrimary uint64) string {
	id := strconv.FormatUint(clientPrimary, 10)
	return filepath.Join("/", orgName, "/workBench/apiManage/client", id)
}

func (svc Service) text(ctx context.Context, key string) string {
	langCodes := httpserver.UnwrapI18nCodes(ctx)
	return svc.trans.Text(langCodes, key)
}

// CreateAPIAsset 创建 API 资料
func (svc *Service) CreateAPIAsset(ctx context.Context, req apistructs.APIAssetCreateRequest) (apiAssetID string, err error) {
	// 参数校验
	if req.AssetID == "" {
		return "", apierrors.CreateAPIAsset.MissingParameter("assetID")
	}
	if err := apistructs.ValidateAPIAssetID(req.AssetID); err != nil {
		return "", apierrors.CreateAPIAsset.InvalidParameter(fmt.Errorf("assetID: %v", err))
	}
	if req.AssetName == "" {
		req.AssetName = req.AssetID
	}
	if err := strutil.Validate(req.AssetName,
		strutil.MinLenValidator(1),
		strutil.MaxLenValidator(191),
	); err != nil {
		return "", apierrors.CreateAPIAsset.InvalidParameter(fmt.Errorf("assetName: %v", err))
	}
	if req.OrgID == 0 {
		return "", apierrors.CreateAPIAsset.MissingParameter("orgID")
	}
	// 校验每个 version
	for _, version := range req.Versions {
		version.OrgID = req.OrgID
		version.APIAssetID = req.AssetID
		version.IdentityInfo = req.IdentityInfo
		if err := svc.readSpec(ctx, &version); err != nil {
			return "", apierrors.CreateAPIAsset.InvalidParameter(err)
		}
		if _, err := svc.parseSpec(ctx, &version.SpecProtocol, version.Spec); err != nil {
			logrus.Errorf("failed to parseSpec, err: %v", err)
			return "", apierrors.CreateAPIAssetVersion.InvalidParameter(errors.Wrap(err, svc.text(ctx, "SwaggerFileISInvalidOAS")))
		}
		// 校验每个 instance
		for _, ins := range version.Instances {
			if err := validateVersionInstanceRequest(ins); err != nil {
				return "", err
			}
		}
	}

	// 获取额外参数
	var (
		timeNow              = time.Now()
		projectID, appID     *uint64
		projectName, appName *string
	)
	// 来自 action 或 design_center 的创建请求要查询 project 和 app 信息
	if req.Source == apistructs.CreateAPIAssetSourceAction ||
		req.Source == apistructs.CreateAPIAssetSourceDesignCenter {
		project, err := bdl.Bdl.GetProject(req.ProjectID)
		if err != nil {
			return "", err
		}
		projectID = &project.ID
		projectName = &project.Name

		app, err := bdl.Bdl.GetApp(req.AppID)
		if err != nil {
			return "", err
		}
		appID = &app.ID
		appName = &app.Name
	}

	var (
		exAsset    apistructs.APIAssetsModel
		assetModel = dbclient.APIAssetsModel{
			BaseModel: apistructs.BaseModel{
				ID:        0,
				CreatedAt: timeNow,
				UpdatedAt: timeNow,
				CreatorID: req.IdentityInfo.UserID,
				UpdaterID: req.IdentityInfo.UserID,
			},
			OrgID:        req.OrgID,
			AssetID:      req.AssetID,
			AssetName:    req.AssetName,
			Desc:         req.Desc,
			Logo:         req.Logo,
			ProjectID:    projectID,
			ProjectName:  projectName,
			AppID:        appID,
			AppName:      appName,
			Public:       false,
			CurVersionID: 0,
			CurMajor:     0,
			CurMinor:     0,
			CurPatch:     0,
		}
	)
	// 检查是否已存在这个 asset
	switch err := svc.FirstRecord(&exAsset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.AssetID,
	}); {
	case err == nil &&
		(req.Source == apistructs.CreateAPIAssetSourceAction ||
			req.Source == apistructs.CreateAPIAssetSourceDesignCenter):
		// assetID 已存在 且 来自 action 的创建, 则跳过新建流程

	case err == nil:
		// assetID 已存在, 则不允许创建, 立即响应 err
		return "", apierrors.CreateAPIAsset.InternalError(errors.New("assetID already exists"))

	case gorm.IsRecordNotFoundError(err):
		// 如果 assetID 不存在, 则正常创建
		if err := dbclient.Sq().Create(&assetModel).Error; err != nil {
			logrus.Errorf("failed to Create assetModel, err: %v", err)
			return "", apierrors.CreateAPIAsset.InternalError(err)
		}

	default:
		// 查询错误, 响应 err
		logrus.Errorf("failed to FirstRecord exAsset, err: %v", err)
		return "", apierrors.CreateAPIAsset.InternalError(err)
	}

	// 创建或更新 API Asset Versions
	for _, version := range req.Versions {
		version.APIAssetID = assetModel.AssetID
		version.OrgID = assetModel.OrgID
		version.IdentityInfo = req.IdentityInfo
		if _, _, _, err := svc.CreateAPIAssetVersion(ctx, version); err != nil {
			return "", err
		}
	}

	return assetModel.AssetID, nil
}

// CreateAPIAssetVersion 创建 API 资料版本
func (svc *Service) CreateAPIAssetVersion(ctx context.Context, req apistructs.APIAssetVersionCreateRequest) (apiAsset *dbclient.APIAssetsModel,
	version *apistructs.APIAssetVersionsModel, spec *dbclient.APIAssetVersionSpecsModel, err error) {
	// 参数校验
	if err := apistructs.ValidateAPIAssetID(req.APIAssetID); err != nil {
		return nil, nil, nil, apierrors.CreateAPIAssetVersion.InvalidParameter(fmt.Errorf("apiAssetID: %v", err))
	}
	if req.OrgID == 0 {
		return nil, nil, nil, apierrors.CreateAPIAssetVersion.MissingParameter("orgID")
	}

	if req.Source == "" {
		req.Source = "local"
	}

	// 查询所属的 asset
	var (
		asset      apistructs.APIAssetsModel
		whereAsset = map[string]interface{}{
			"org_id":   req.OrgID,
			"asset_id": req.APIAssetID,
		}
	)
	if err := svc.FirstRecord(&asset, whereAsset); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return nil, nil, nil, err
	}

	// 鉴权 {asset 创建者, 企业管理人员, 关联项目管理人员, 关联应用管理人员} 才可以创建 version
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.UserID)
	if written := writePermission(rolesSet, &asset); !written {
		return nil, nil, nil, apierrors.CreateAPIAssetVersion.AccessDenied()
	}

	if err := svc.readSpec(ctx, &req); err != nil {
		return nil, nil, nil, apierrors.CreateAPIAssetVersion.InvalidParameter(err)
	}
	swagger, err := svc.parseSpec(ctx, &req.SpecProtocol, req.Spec)
	if err != nil {
		logrus.Errorf("failed to parseSpec, err: %v", err)
		return nil, nil, nil, apierrors.CreateAPIAssetVersion.InvalidParameter(errors.Wrap(err, svc.text(ctx, "SwaggerFileISInvalidOAS")))
	}
	for i, instance := range req.Instances {
		if err := validateVersionInstanceRequest(instance); err != nil {
			return nil, nil, nil, err
		}
		handleVersionInstanceRequest(&req.Instances[i])
	}

	var (
		timeNow = time.Now()
	)

	version = &apistructs.APIAssetVersionsModel{
		BaseModel: apistructs.BaseModel{
			ID:        0,
			CreatedAt: timeNow,
			UpdatedAt: timeNow,
			CreatorID: req.IdentityInfo.UserID,
			UpdaterID: req.IdentityInfo.UserID,
		},
		OrgID:          req.OrgID,
		AssetID:        req.APIAssetID,
		AssetName:      asset.AssetName,
		Major:          req.Major,
		Minor:          req.Minor,
		Patch:          req.Patch,
		Desc:           req.Desc,
		SpecProtocol:   req.SpecProtocol,
		SwaggerVersion: swagger.Info.Version,
		Deprecated:     false,
		Source:         req.Source,
		AppID:          req.AppID,
		Branch:         req.Branch,
		ServiceName:    req.ServiceName,
	}

	if err = dbclient.GenSemVer(req.OrgID, req.APIAssetID, swagger.Info.Version, &version.Major, &version.Minor, &version.Patch); err != nil {
		return nil, nil, nil, apierrors.CreateAPIAssetVersion.InternalError(err)
	}

	if err := dbclient.Sq().Create(version).Error; err != nil {
		return nil, nil, nil, apierrors.CreateAPIAssetVersion.InternalError(err)
	}

	// 创建或更新 API Asset Version Spec
	spec = &dbclient.APIAssetVersionSpecsModel{
		BaseModel: apistructs.BaseModel{
			ID:        0,
			CreatorID: req.IdentityInfo.UserID,
			UpdaterID: req.IdentityInfo.UserID,
		},
		OrgID:        req.OrgID,
		AssetID:      req.APIAssetID,
		VersionID:    version.ID,
		SpecProtocol: string(req.SpecProtocol),
		Spec:         req.Spec,
	}
	if err := dbclient.CreateOrUpdateAPIAssetVersionSpec(spec); err != nil {
		return nil, nil, nil, apierrors.CreateAPIAssetVersion.InternalError(err)
	}

	// 插入搜索用的索引和片段
	go svc.createOAS3IndexFragments(swagger, req.APIAssetID, asset.AssetName, version.ID)

	// 创建 API 实例
	for _, instanceReq := range req.Instances {
		services, err := bdl.Bdl.GetRuntimeServices(instanceReq.RuntimeID, req.OrgID, req.IdentityInfo.UserID)
		if err != nil {
			logrus.Errorf("failed to GetRuntimeServices, err: %v", err)
			continue
		}
		if len(services.Services) == 0 {
			logrus.Error("length of services.Services == 0")
			continue
		}
		service, ok := services.Services[instanceReq.ServiceName]
		if !ok {
			logrus.Errorf("no this service which name is %s", instanceReq.ServiceName)
			continue
		}
		if len(service.Addrs) == 0 {
			logrus.Error("length of service.Addrs == 0")
			continue
		}
		instanceAddr := service.Addrs[0]
		var appID uint64
		if services.Extra != nil {
			appID = services.Extra.ApplicationId
		} else {
			appID = *asset.AppID
		}
		if _, apiError := svc.CreateInstantiation(ctx, &apistructs.CreateInstantiationReq{
			OrgID:    req.OrgID,
			Identity: &req.IdentityInfo,
			URIParams: &apistructs.CreateInstantiationURIParams{
				AssetID:        req.APIAssetID,
				SwaggerVersion: swagger.Info.Version,
				Minor:          req.Minor,
			},
			Body: &apistructs.CreateInstantiationBody{
				Type:        "dice", // 仅 action 创建实例时会在此处创建实例化, 所以 Type 的值为 "dice"
				URL:         instanceAddr,
				ProjectID:   services.ProjectID,
				AppID:       appID,
				RuntimeID:   instanceReq.RuntimeID,
				ServiceName: instanceReq.ServiceName,
				Workspace:   services.Extra.Workspace,
			},
		}); apiError != nil {
			return nil, nil, nil, apiError
		}
	}

	// 更新 APIAsset
	latestVersion, err := dbclient.QueryAPILatestVersion(req.OrgID, req.APIAssetID)
	if err != nil {
		return nil, nil, nil, err
	}

	if err := dbclient.Sq().Model(new(apistructs.APIAssetsModel)).Where(map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.APIAssetID,
	}).Updates(map[string]interface{}{
		"updater_id":     req.IdentityInfo.UserID,
		"updated_at":     time.Now(),
		"cur_version_id": latestVersion.ID,
		"cur_major":      latestVersion.Major,
		"cur_minor":      latestVersion.Minor,
		"cur_patch":      latestVersion.Patch,
	}).Error; err != nil {
		return nil, nil, nil, err
	}

	// 查询更新后的 APIAsset
	var updatedAsset dbclient.APIAssetsModel
	if err := svc.FirstRecord(&updatedAsset, whereAsset); err != nil {
		return nil, nil, nil, err
	}

	// 保留最新 10 条version
	var (
		versions    []*apistructs.APIAssetVersionsModel
		versionsIDs []uint64
		where       = map[string]interface{}{
			"org_id":   version.OrgID,
			"asset_id": version.AssetID,
			"major":    version.Major,
			"minor":    version.Minor,
		}
	)
	if err := dbclient.Sq().Where(where).Order("patch DESC").
		Find(&versions); err != nil {
		logrus.Errorf("failed to query newest ten versions: %v", err)
	}
	if len(versions) >= 10 {
		for i := 10; i < len(versions); i++ {
			versionsIDs = append(versionsIDs, versions[i].ID)
		}
		if err := dbclient.Sq().Where(where).
			Where("id IN (?)", versionsIDs).
			Delete(new(apistructs.APIAssetVersionsModel)).Error; err != nil {
			logrus.Errorf("failed to delete old versions: %+v", err)
		}
		if err := dbclient.Sq().
			Where(map[string]interface{}{
				"org_id":   req.OrgID,
				"asset_id": req.APIAssetID,
			}).
			Where("version_id IN (?)", versionsIDs).
			Delete(new(apistructs.APIAssetVersionSpecsModel)).Error; err != nil {
			logrus.Errorf("failed to Delete SpecsModel, err: %v", err)
		}
	}

	return &updatedAsset, version, spec, nil
}

func (svc *Service) CreateInstantiation(ctx context.Context, req *apistructs.CreateInstantiationReq) (*apistructs.InstantiationModel, *errorresp.APIError) {
	// 参数校验
	if req.Body == nil {
		return nil, apierrors.CreateInstantiation.MissingParameter("missing body")
	}
	if req.Body.Type = strings.ToLower(req.Body.Type); req.Body.Type != InsTypeDice && req.Body.Type != InsTypeExternal {
		return nil, apierrors.CreateInstantiation.MissingParameter("missing type")
	}
	if _, err := url.Parse(req.Body.URL); err != nil {
		return nil, apierrors.CreateInstantiation.InvalidParameter("invalid url format")
	}

	var (
		timeNow = time.Now()
		asset   apistructs.APIAssetsModel
		version apistructs.APIAssetVersionsModel
	)

	// 查询 asset
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return nil, apierrors.CreateInstantiation.InternalError(err)
	}

	// 查出 version
	switch err := svc.FirstRecord(&version, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
	}); {
	case err == nil:
	case gorm.IsRecordNotFoundError(err):
		return nil, apierrors.CreateInstantiation.InternalError(
			errors.Errorf(svc.text(ctx, "VersionLabelNotExits"), req.URIParams.SwaggerVersion))
	default:
		logrus.Errorf("failed to FirstRecord version, err: %v", err)
		return nil, apierrors.CreateInstantiation.InternalError(err)
	}

	var instantiation = apistructs.InstantiationModel{
		BaseModel: apistructs.BaseModel{
			ID:        0,
			CreatedAt: timeNow,
			UpdatedAt: timeNow,
			CreatorID: req.Identity.UserID,
			UpdaterID: req.Identity.UserID,
		},
		OrgID:          req.OrgID,
		Name:           "",
		AssetID:        req.URIParams.AssetID,
		SwaggerVersion: req.URIParams.SwaggerVersion,
		Major:          version.Major,
		Minor:          req.URIParams.Minor,
		Type:           req.Body.Type,
		URL:            req.Body.URL,
		ProjectID:      req.Body.ProjectID,
		AppID:          req.Body.AppID,
		ServiceName:    req.Body.ServiceName,
		RuntimeID:      req.Body.RuntimeID,
		Workspace:      req.Body.Workspace,
	}
	if err := dbclient.FirstOrCreateInstantiation(&instantiation, map[string]interface{}{
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
		"minor":           req.URIParams.Minor,
	}); err != nil {
		return nil, apierrors.CreateInstantiation.InternalError(err)
	}

	// 如果绑定实例前, asset 还没关联项目和应用, 则将实例的项目和应用关联到 asset (允许失败)
	switch asset.ProjectID {
	case nil:
		project, err := bdl.Bdl.GetProject(instantiation.ProjectID)
		if err != nil {
			logrus.Errorf("failed to GetProject, err: %v", err)
			break
		}
		app, err := bdl.Bdl.GetApp(instantiation.AppID)
		if err != nil {
			logrus.Errorf("failed to GetApp, err: %v", err)
			break
		}

		if err := dbclient.Sq().Model(new(apistructs.APIAssetsModel)).
			Where(map[string]interface{}{
				"org_id":   req.OrgID,
				"asset_id": req.URIParams.AssetID,
			}).
			Updates(map[string]interface{}{
				"project_id":   instantiation.ProjectID,
				"project_name": project.Name,
				"app_id":       instantiation.AppID,
				"app_name":     app.Name,
				"updated_at":   timeNow,
				"updater_id":   req.Identity.UserID,
			},
			).Error; err != nil {
			logrus.Errorf("failed to Updates AssetModel, err: %v", err)
		}
	}

	instantiations, ok, apiError := svc.GetInstantiation(&apistructs.GetInstantiationsReq{
		OrgID:    req.OrgID,
		Identity: req.Identity,
		URIParams: &apistructs.GetInstantiationsURIParams{
			AssetID:        req.URIParams.AssetID,
			SwaggerVersion: req.URIParams.SwaggerVersion,
			Minor:          req.URIParams.Minor,
		},
	})
	if apiError != nil {
		return nil, apiError
	}
	if !ok {
		return nil, apierrors.CreateInstantiation.InternalError(errors.New(svc.text(ctx, "FailedToCreateInstantiation")))
	}

	return instantiations, nil
}

func (svc *Service) CreateClient(ctx context.Context, req *apistructs.CreateClientReq) (*apistructs.ClientModel, *errorresp.APIError) {
	if req == nil || req.Body == nil {
		return nil, apierrors.CreateClient.InvalidParameter(svc.text(ctx, "InvalidParams"))
	}
	if req.Body.Name == "" {
		return nil, apierrors.CreateClient.InvalidParameter(svc.text(ctx, "InvalidClientName"))
	}
	if len(req.Body.DisplayName) == 0 || len(req.Body.DisplayName) > 50 {
		return nil, apierrors.CreateClient.InvalidParameter(svc.text(ctx, "InvalidClientDisplayName"))
	}

	// 检查同名 client 是否已存在
	if err := svc.FirstRecord(new(apistructs.ClientModel), map[string]interface{}{
		"org_id": req.OrgID,
		"name":   req.Body.Name,
	}); err == nil {
		return nil, apierrors.CreateClient.InternalError(errors.New(svc.text(ctx, "ClientNameIsExists")))
	}

	// 检查此用户下同 displayName 的 client 是否存在
	if err := svc.FirstRecord(new(apistructs.ClientModel), map[string]interface{}{
		"org_id":       req.OrgID,
		"display_name": req.Body.DisplayName,
		"creator_id":   req.Identity.UserID,
	}); err == nil {
		return nil, apierrors.CreateClient.InternalError(errors.New(svc.text(ctx, "SameNameClientIsExists")))
	}

	consumer, err := bdl.Bdl.CreateClientConsumer(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID, req.Body.Name)
	if err != nil {
		return nil, apierrors.CreateClient.InternalError(err)
	}

	var (
		timeNow = time.Now()
		model   = apistructs.ClientModel{
			BaseModel: apistructs.BaseModel{
				ID:        0,
				CreatedAt: timeNow,
				UpdatedAt: timeNow,
				CreatorID: req.Identity.UserID,
				UpdaterID: req.Identity.UserID,
			},
			OrgID:       req.OrgID,
			Name:        req.Body.Name,
			Desc:        req.Body.Desc,
			ClientID:    consumer.ClientId,
			DisplayName: req.Body.DisplayName,
		}
	)

	if err = dbclient.Sq().Create(&model).Error; err != nil {
		return nil, apierrors.CreateClient.InternalError(err)
	}

	return &model, nil
}

// CreateContract 创建一个合约. 注意创建合约时, 需要查询客户端详情, 查询客户端详情时传入的 ClientID 是 dice_api_clients 的主键
func (svc *Service) CreateContract(ctx context.Context, req *apistructs.CreateContractReq) (*apistructs.ClientModel, *apistructs.SK,
	*apistructs.ContractModel, *errorresp.APIError) {
	// 参数校验
	if req.URIParams == nil {
		return nil, nil, nil, apierrors.CreateContract.InvalidParameter("no URI parameter")
	}
	if req.Body == nil {
		return nil, nil, nil, apierrors.CreateContract.InvalidParameter("no body")
	}
	if req.OrgID == 0 {
		return nil, nil, nil, apierrors.CreateContract.InvalidParameter("orgID can not be 0")
	}

	// 参数初始化
	var (
		asset  apistructs.APIAssetsModel
		access apistructs.APIAccessesModel
	)
	// 查询 assetName
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.Body.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return nil, nil, nil, apierrors.CreateContract.InternalError(errors.New(svc.text(ctx, "FailedToFindAPIAsset")))
	}

	// 查询客户端详情 (一般角色只能用自己的客户端, 企业管理员可以用所有客户端)
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	rolesMOrgs := rolesSet.RolesOrgs(bdl.OrgMRoles...)
	client, err := dbclient.GetMyClient(&apistructs.GetClientReq{
		OrgID:     req.OrgID,
		Identity:  req.Identity,
		URIParams: &apistructs.GetClientURIParams{ClientID: req.URIParams.ClientID},
	}, inSlice(strconv.FormatUint(req.OrgID, 10), rolesMOrgs))
	if err != nil {
		logrus.Errorf("failed to GetMyClient, err: %v", err)
		return nil, nil, nil, apierrors.CreateContract.InternalError(errors.New(svc.text(ctx, "ClientNotExistsOrPermissionDenied")))
	}

	// 查询 SK
	credentials, err := bdl.Bdl.GetClientCredentials(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
		client.ClientID)
	if err != nil {
		logrus.Errorf("failed to GetClientCredentials, err: %v", err)
		return nil, nil, nil, apierrors.CreateContract.InternalError(errors.New(svc.text(ctx, "FailedToFindClientSecret")))
	}
	sk := apistructs.SK{
		ClientID:     credentials.ClientId,
		ClientSecret: credentials.ClientSecret,
	}

	// 查询 access
	if err = svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.Body.AssetID,
		"swagger_version": req.Body.SwaggerVersion,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord access: %v", err)
		return nil, nil, nil, apierrors.CreateContract.InternalError(errors.New(svc.text(ctx, "FailedToFindAccessItem")))
	}

	// 查询 access 下的 SLA 列表
	var slas []*apistructs.SLAModel
	if _ = svc.ListRecords(&slas, map[string]interface{}{
		"access_id": access.ID,
	}); len(slas) > 0 && req.Body.SLAID == nil {
		return nil, nil, nil, apierrors.CreateContract.InvalidParameter(svc.text(ctx, "NoSALBeChosen"))
	}

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()
	tx.Exec("set innodb_lock_wait_timeout=8")

	// 查询是否已经有这样的合约了, 如果有且不是撤销状态, 则不再创建了
	var (
		exContract apistructs.ContractModel
		where      = map[string]interface{}{
			"asset_id":        req.Body.AssetID,
			"org_id":          req.OrgID,
			"client_id":       req.URIParams.ClientID,
			"swagger_version": req.Body.SwaggerVersion,
		}
	)
	err = tx.Set("gorm:query_option", "FOR UPDATE").Where(where).First(&exContract).Error
	// case 1: record not found
	if gorm.IsRecordNotFoundError(err) {
		contract, err := svc.createContractFirstTime(ctx, tx, req, &asset, &access, client)
		if err != nil {
			logrus.Errorf("failed to createContractFirstTime, err: %v", err)
			return nil, nil, nil, apierrors.CreateContract.InternalError(errors.New(svc.text(ctx, "FailedToApplyToCallAPI")))
		}
		return client, &sk, contract, nil
	}
	// case 2: lock wait timeout
	if err != nil {
		logrus.WithError(err).WithFields(where).Errorln("the record is already exists and is in updating")
		return nil, nil, nil, apierrors.CreateContract.InvalidState(fmt.Sprintf("repeated request: %v", err))
	}
	// case 3: the record is already exists
	contract, err := svc.createContractIfExists(ctx, tx, req, &asset, &access, client, &exContract)
	if err != nil {
		logrus.Errorf("failed to createContractIfExists, err: %v", err)
		return nil, nil, nil, apierrors.CreateContract.InternalError(errors.New(svc.text(ctx, "FailedToApplyToCallAPI")))
	}
	return client, &sk, contract, nil
}

// 创建合约时, 如果合约已存在, 进入此分支
func (svc *Service) createContractIfExists(ctx context.Context, tx *dbclient.TX, req *apistructs.CreateContractReq, asset *apistructs.APIAssetsModel,
	access *apistructs.APIAccessesModel,
	client *apistructs.ClientModel, exContract *apistructs.ContractModel) (*apistructs.ContractModel, error) {
	var (
		err      error
		timeNow  = time.Now()
		updates  = make(map[string]interface{})
		contract apistructs.ContractModel
		record   = apistructs.ContractRecordModel{
			ID:         0,
			OrgID:      req.OrgID,
			ContractID: exContract.ID,
			Action:     "ActionReplyToApplyToCall",
			CreatorID:  req.Identity.UserID,
			CreatedAt:  timeNow,
		}
		sla apistructs.SLAModel
	)

	org, err := svc.bdl.GetOrg(req.OrgID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to GetOrg")
	}

	// 如果合约处于 "等待授权" 和 "已授权" 以外的情形, 则修改为 "等待授权"
	if exContract.Status.ToLower() != apistructs.ContractApproving &&
		exContract.Status.ToLower() != apistructs.ContractApproved {
		exContract.Status = apistructs.ContractApproving
		updates["status"] = exContract.Status
	}

	// 如果合约处 "等待授权" 且 access 是自动授权的, 则修改为 "已授权"
	if exContract.Status.ToLower() == apistructs.ContractApproving &&
		access.Authorization.ToLower() == apistructs.AuthorizationAuto {
		exContract.Status = apistructs.ContractApproved
		updates["status"] = exContract.Status
	}

	// 库表操作和消息通知
	defer func() {
		updates["updated_at"] = timeNow
		updates["updater_id"] = req.Identity.UserID
		if err = tx.Model(&contract).
			Where(map[string]interface{}{"id": exContract.ID}).
			Updates(updates).
			Error; err != nil {
			err = errors.Wrap(err, "failed to Updates contract")
			return
		}

		switch updatingStatus, ok := updates["status"]; {
		case !ok:
			// 如果没有修改 status, 则关注 SLA 状态

			// 如果申请了新的 SLA 且处于待审批的状态, 则记录操作, 并通知管理员
			if reqSLAID, ok := updates["request_sla_id"]; ok && reqSLAID != nil {
				go svc.contractMsgToManager(ctx, req.OrgID, req.Identity.UserID, asset, access,
					svc.RequestItemSLA(ctx, sla.Name, org.Locale),
					false)
				record.SLAName = sla.Name
				record.Action = "ApplyToUseSLA"
				break
			}

			// 如果通过了申请的 SLA, 则记录操作, 通知用户
			if _, ok := updates["cur_sla_id"]; ok {
				result := svc.ApprovalResultSLAUpdated(ctx, sla.Name, org.Locale)
				go svc.contractMsgToUser(req.OrgID, req.Identity.UserID, asset.AssetName, client, result)
				record.Action = string(result)
			}

		case updatingStatus == apistructs.ContractApproving:
			// 如果是更新后的状态是"等待授权", 向管理员发送通知
			go svc.contractMsgToManager(ctx, req.OrgID, req.Identity.UserID, asset, access,
				svc.RequestItemAPI(ctx, access.AssetName, access.SwaggerVersion, org.Locale),
				false)
			record.Action += "," + "WaitForProving"

		case updatingStatus == apistructs.ContractApproved:
			// 如果更新后的状态是"已授权", 向用户发送通知; 调用网关测的授权
			go svc.contractMsgToUser(req.OrgID, req.Identity.UserID, access.AssetName, client,
				svc.ManagerProvedContract(ctx, org.Locale))
			record.Action += "," + "AutoProved"
			if err = bdl.Bdl.GrantEndpointToClient(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
				client.ClientID, access.EndpointID); err != nil {
				err = errors.Wrapf(err, "failed to GrantEndpointToClient, clientID: %s, endpointID: %s",
					client.ClientID, access.EndpointID)
				return
			}
		}

		if err = tx.Create(&record).Error; err != nil {
			err = errors.Wrap(err, "failed to Create record")
		}

		tx.Commit()

		go func() {
			err := svc.createOrUpdateClientLimits(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
				access.EndpointID, client.ClientID, contract.ID)
			if err != nil {
				logrus.Errorf("create or update client limits failed, err:%+v", err)
			}
		}()
	}()

	// 如果没有申请 SLA, 则结束业务流程
	if req.Body.SLAID == nil {
		return &contract, err
	}

	exContract.RequestSLAID = req.Body.SLAID
	updates["request_sla_id"] = req.Body.SLAID

	// 如果合约没有当前 SLA, 且 access 有默认 SLA, 则令当前 SLA 为默认 SLA
	if exContract.CurSLAID == nil && access.DefaultSLAID != nil {
		exContract.CurSLAID = access.DefaultSLAID
		updates["cur_sla_id"] = exContract.CurSLAID
		updates["sla_committed_at"] = timeNow
	}

	// 如果当前合约与申请的合约一致, 则相当于自动通过
	if exContract.CurSLAID != nil && *exContract.CurSLAID == *req.Body.SLAID {
		updates["request_sla_id"] = nil
	}

	// 如果 SLA 是自动审批的, 则将请求中的 SLA 转移到当前 SLA
	if err := svc.FirstRecord(&sla, map[string]interface{}{"id": req.Body.SLAID}); err == nil && sla.Approval.ToLower() == apistructs.AuthorizationAuto {
		updates["cur_sla_id"] = req.Body.SLAID
		updates["sla_committed_at"] = timeNow
		updates["request_sla_id"] = nil
	}

	return &contract, err
}

// 创建合约时, 如果合约不存在, 进入此分支
func (svc *Service) createContractFirstTime(ctx context.Context, tx *dbclient.TX, req *apistructs.CreateContractReq,
	asset *apistructs.APIAssetsModel, access *apistructs.APIAccessesModel,
	client *apistructs.ClientModel) (*apistructs.ContractModel, error) {
	var (
		timeNow  = time.Now()
		contract = apistructs.ContractModel{
			BaseModel: apistructs.BaseModel{
				ID:        0,
				CreatedAt: timeNow,
				UpdatedAt: timeNow,
				CreatorID: req.Identity.UserID,
				UpdaterID: req.Identity.UserID,
			},
			OrgID:          req.OrgID,
			AssetID:        access.AssetID,
			AssetName:      access.AssetName,
			SwaggerVersion: access.SwaggerVersion,
			ClientID:       client.ID,
			Status:         apistructs.ContractApproving,
			CurSLAID:       nil,
			RequestSLAID:   nil,
			SLACommittedAt: nil,
		}
	)

	org, err := svc.bdl.GetOrg(req.OrgID)
	if err != nil {
		return nil, errors.Wrap(err, "failed to GetOrg")
	}

	// 如果 access 下有 sla, 但没有申请 SLA, 则不允许申请
	var count uint64
	dbclient.Sq().Model(new(apistructs.SLAModel)).Where(map[string]interface{}{"access_id": access.ID}).Count(&count)
	if count > 0 && (req.Body.SLAID == nil || *req.Body.SLAID == 0) {
		return nil, apierrors.CreateContract.InvalidParameter(svc.text(ctx, "MustWithOneSLA"))
	}

	// 如果 access 是自动授权的, 令 contract.Status 为 "已授权"
	if access.Authorization.ToLower() == apistructs.AuthorizationAuto {
		contract.Status = apistructs.ContractApproved
	}

	// 如果 access 有默认 SLA, 则令 contract 当前 SLA 为其默认 SLA
	if access.DefaultSLAID != nil && *access.DefaultSLAID != 0 {
		contract.CurSLAID = access.DefaultSLAID
		contract.SLACommittedAt = &timeNow
	}

	if req.Body.SLAID == nil {
		var id uint64 = 0
		req.Body.SLAID = &id
	}

	sla, err := svc.querySLAByID(ctx, *req.Body.SLAID, access)
	switch {
	case err != nil:
		// 查询不到申请的 SLA
		return nil, errors.Wrap(err, "failed to FirstRecord sla")

	case sla.Approval.ToLower() == apistructs.AuthorizationAuto:
		// 如果 SLA 是自动授权的
		contract.CurSLAID = req.Body.SLAID
		contract.SLACommittedAt = &timeNow

	case sla.Source == apistructs.SourceSystem:
		// 如果 SLA 是无限制的, 则令 contract 当前 SLA 为申请的 SLA
		contract.CurSLAID = req.Body.SLAID

	case contract.CurSLAID != nil && *req.Body.SLAID == *contract.CurSLAID:
		// 如果申请的 SLA 与 当前的 SLA 一致 (无需操作)

	default:
		// 默认情形, 令 contract 的申请中 SLA 为传入的 SLA
		contract.RequestSLAID = req.Body.SLAID
	}

	// 创建合约记录
	if err := tx.Create(&contract).Error; err != nil {
		return nil, err
	}

	// 操作记录
	var record = apistructs.ContractRecordModel{
		ID:         0,
		OrgID:      req.OrgID,
		ContractID: contract.ID,
		Action:     "ApplyToCallAPI",
		SLAName:    sla.Name,
		CreatorID:  req.Identity.UserID,
		CreatedAt:  timeNow,
	}

	switch contract.Status.ToLower() {
	case apistructs.ContractApproving:
		// 等待审批, 通知管理人员进行审批

		go svc.contractMsgToManager(ctx, req.OrgID, req.Identity.UserID, asset, access,
			svc.RequestItemAPI(ctx, access.AssetName, access.SwaggerVersion, org.Locale),
			false)

		record.Action += "," + "WaitForProving"
		if err := tx.Create(&record).Error; err != nil {
			logrus.Errorf("failed to Create record: %v", err)
			return nil, errors.Wrap(err, "failed to Create record")
		}

	case apistructs.ContractApproved:
		// 如果合约已授权, 调用网关侧的授权; 通知用户已通过

		go svc.contractMsgToUser(req.OrgID, req.Identity.UserID, access.AssetName, client,
			svc.ManagerProvedContract(ctx, org.Locale))

		record.Action += "," + "AutoProved"
		if err := tx.Create(&record).Error; err != nil {
			logrus.Errorf("failed to Create record: %v", err)
			return nil, errors.Wrap(err, "failed to Create record")
		}

		if err := bdl.Bdl.GrantEndpointToClient(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
			client.ClientID, access.EndpointID); err != nil {
			logrus.Errorf("failed to GrantEndpointToClient, err: %v", err)
			return nil, errors.Wrap(err, "failed to GrantEndpointToClient")
		}
	}

	tx.Commit()

	go func() {
		err := svc.createOrUpdateClientLimits(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
			access.EndpointID, client.ClientID, contract.ID)
		if err != nil {
			logrus.Errorf("createOrUpdateClientLimits failed, err:%+v", err)
		}
	}()

	return &contract, nil
}

func (svc *Service) CreateAccess(ctx context.Context, req *apistructs.CreateAccessReq) (map[string]interface{}, *errorresp.APIError) {
	// 参数校验
	if req == nil || req.Body == nil {
		return nil, apierrors.CreateAccess.InvalidParameter("invalid parameters")
	}
	if req.OrgID == 0 {
		return nil, apierrors.CreateAccess.InvalidParameter("invalid orgID")
	}

	var asset apistructs.APIAssetsModel
	if err := svc.FirstRecord(&asset, map[string]interface{}{"org_id": req.OrgID, "asset_id": req.Body.AssetID}); err != nil {
		return nil, apierrors.CreateAccess.InternalError(errors.Wrap(err, "failed to GetAPIAsset"))
	}

	// 查找一个版本记录 (为了查找swaggerVersion)
	var version apistructs.APIAssetVersionsModel
	if err := svc.FirstRecord(&version, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.Body.AssetID,
		"major":    req.Body.Major,
	}); err != nil {
		return nil, apierrors.CreateAccess.InternalError(errors.Wrap(err, "failed to FirstVersion"))
	}
	swaggerVersion := version.SwaggerVersion

	// 检查 access 是否已存在
	switch err := dbclient.Sq().First(new(apistructs.APIAccessesModel), map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.Body.AssetID,
		"swagger_version": swaggerVersion,
	}).Error; {
	case err == nil:
		return nil, apierrors.CreateAccess.InternalError(fmt.Errorf(svc.text(ctx, "AccessItemAlreadyExists"), req.Body.AssetID, swaggerVersion))
	case gorm.IsRecordNotFoundError(err):
	default:
		return nil, apierrors.CreateAccess.InternalError(err)
	}

	// 查找实例
	instantiations, ok, apiError := svc.GetInstantiation(&apistructs.GetInstantiationsReq{
		OrgID:    req.OrgID,
		Identity: req.Identity,
		URIParams: &apistructs.GetInstantiationsURIParams{
			AssetID:        req.Body.AssetID,
			SwaggerVersion: swaggerVersion,
			Minor:          req.Body.Minor,
		},
	})
	if apiError != nil {
		return nil, apiError
	}
	if !ok {
		return nil, apierrors.CreateAccess.InternalError(errors.New("no instantiation"))
	}
	if strings.ToLower(instantiations.Type) == InsTypeDice {
		if req.Body.ProjectID != instantiations.ProjectID {
			return nil, apierrors.CreateAccess.InvalidParameter(svc.text(ctx, "ProjectErrorPleaseRetry"))
		}
		if req.Body.Workspace != instantiations.Workspace {
			return nil, apierrors.CreateAccess.InvalidParameter(svc.text(ctx, "WorkspaceErrorPleaseRetry"))
		}
	}

	// 解析实例的 url
	var (
		urlStr     = instantiations.URL
		host, path string
	)
	if !strings.HasPrefix(urlStr, "http://") && !strings.HasPrefix(urlStr, "https://") && !strings.HasPrefix(urlStr, "//") {
		urlStr = "//" + strings.TrimLeft(urlStr, "/")
	}
	parsedURL, err := url.Parse(urlStr)
	if err != nil {
		return nil, apierrors.CreateAccess.InternalError(errors.Wrap(err, "invalid instance url"))
	}
	host = parsedURL.Host
	path = parsedURL.Path
	if path == "" {
		path = "/"
	}

	// 创建流量入口
	authType := apistructs.AT_KEY_AUTH
	switch req.Body.Authentication.ToLower() {
	case apistructs.AuthenticationKeyAuth:
	case apistructs.AuthenticationSignAuth:
		authType = apistructs.AT_SIGN_AUTH
	case apistructs.AuthenticationOAuth2:
		return nil, apierrors.CreateAccess.InvalidParameter(svc.text(ctx, "UnsupportedOAuth2"))

	default:
		return nil, apierrors.CreateAccess.InvalidParameter(svc.text(ctx, "UnsupportedAuthentication"))
	}
	endpointID, err := bdl.Bdl.CreateEndpoint(
		strconv.FormatUint(req.OrgID, 10),
		req.Identity.UserID,
		strconv.FormatUint(req.Body.ProjectID, 10),
		req.Body.Workspace,
		apistructs.PackageDto{
			Name:             fmt.Sprintf("%s_%d_%s", req.Body.AssetID, req.Body.Major, req.Body.Workspace),
			BindDomain:       req.Body.BindDomain,
			AuthType:         authType,
			AclType:          apistructs.ACL_ON,
			Scene:            apistructs.OPENAPI_SCENE,
			Description:      "creation of endpoint from apim",
			NeedBindCloudapi: false,
		},
	)

	if err != nil {
		return nil, apierrors.CreateAccess.InternalError(errors.Wrap(err, "failed to CreateEndpoint"))
	}

	// 路由规则
	if err = bdl.Bdl.CreateOrUpdateEndpointRootRoute(strconv.FormatUint(req.OrgID, 10),
		req.Identity.UserID, endpointID, host, path); err != nil {
		_ = bdl.Bdl.DeleteEndpoint(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID, endpointID)
		return nil, apierrors.CreateAccess.InternalError(errors.Wrapf(err,
			"failed to CreateOrUpdateEndpointRootRoute, {endpointID:%s, host:%s, path:%s}",
			endpointID, host, path))
	}

	var (
		timeNow = time.Now()
	)
	project, err := bdl.Bdl.GetProject(req.Body.ProjectID)
	if err != nil {
		logrus.Errorf("failed to GetProject")
		return nil, apierrors.CreateAccess.InternalError(err)
	}

	access := apistructs.APIAccessesModel{
		BaseModel: apistructs.BaseModel{
			ID:        0,
			CreatedAt: timeNow,
			UpdatedAt: timeNow,
			CreatorID: req.Identity.UserID,
			UpdaterID: req.Identity.UserID,
		},
		OrgID:           req.OrgID,
		AssetID:         req.Body.AssetID,
		AssetName:       asset.AssetName,
		SwaggerVersion:  version.SwaggerVersion,
		Major:           req.Body.Major,
		Minor:           req.Body.Minor,
		ProjectID:       req.Body.ProjectID,
		Workspace:       req.Body.Workspace,
		EndpointID:      endpointID,
		Authentication:  req.Body.Authentication,
		Authorization:   req.Body.Authorization,
		AddonInstanceID: req.Body.AddonInstanceID,
		BindDomain:      strings.Join(req.Body.BindDomain, ","),
		ProjectName:     project.Name,
	}
	if err := dbclient.Sq().Create(&access).Error; err != nil {
		_ = bdl.Bdl.DeleteEndpoint(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID, endpointID)
		return nil, apierrors.CreateAccess.InternalError(errors.Wrap(err, "failed to CreateAccess"))
	}

	return map[string]interface{}{"access": access}, nil
}

func (svc *Service) CreateSLA(ctx context.Context, req *apistructs.CreateSLAReq) *errorresp.APIError {
	// 参数校验
	if req == nil || req.URIParams == nil {
		return apierrors.CreateSLA.InvalidParameter(svc.text(ctx, "InvalidParams"))
	}
	if req.Body == nil {
		return apierrors.CreateSLA.InvalidParameter(svc.text(ctx, "InvalidRequestBody"))
	}
	if req.OrgID == 0 {
		return apierrors.CreateSLA.InvalidParameter(svc.text(ctx, "InvalidOrgID"))
	}
	if !req.Body.Approval.Valid() {
		return apierrors.CreateSLA.InvalidParameter(svc.text(ctx, "InvalidApprove"))
	}
	if len(req.Body.Limits) == 0 {
		return apierrors.CreateSLA.InvalidParameter(svc.text(ctx, "AtLeastOneLimitInSLA"))
	}

	// 查询 asset
	var asset apistructs.APIAssetsModel
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord, err: %v", err)
		return apierrors.CreateSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindAPI")))
	}

	// 鉴权: 创建 SLA 的权限与 API Asset 的 W 权限一致
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := writePermission(rolesSet, &asset); !written {
		return apierrors.CreateSLA.AccessDenied()
	}

	// 查询 access
	var access apistructs.APIAccessesModel
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord access, err: %v", err)
		return apierrors.CreateSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindAccessItme")))
	}

	// 检查重名
	if trimSpace := strings.Replace(req.Body.Name, " ", "", -1); trimSpace == "UnlimitedSLA" || trimSpace == "无限制SLA" {
		text := svc.text(ctx, "CanNotNamed") + ": " + svc.text(ctx, "SystemReservedName")
		return apierrors.CreateSLA.InternalError(errors.Errorf(text, req.Body.Name))
	}
	var exSLA apistructs.SLAModel
	if err := svc.FirstRecord(&exSLA, map[string]interface{}{
		"access_id": access.ID,
		"name":      req.Body.Name,
	}); err == nil {
		logrus.Errorf("记录已存在: %+v", exSLA)
		return apierrors.CreateSLA.InternalError(errors.New(svc.text(ctx, "SLAIsAlreadyExists") + ": " + req.Body.Name))
	}

	// 参数初始化
	timeNow := time.Now()
	slaModel := &apistructs.SLAModel{
		BaseModel: apistructs.BaseModel{
			ID:        0,
			CreatedAt: timeNow,
			UpdatedAt: timeNow,
			CreatorID: req.Identity.UserID,
			UpdaterID: req.Identity.UserID,
		},
		Name:     req.Body.Name,
		Desc:     req.Body.Desc,
		Approval: req.Body.Approval,
		AccessID: access.ID,
	}

	// 如果是自动授权的, 要检查此前是否已经存在自动授权 SLA 了
	if req.Body.Approval.ToLower() == apistructs.AuthorizationAuto {
		var exAuto apistructs.SLAModel
		if err := svc.FirstRecord(&exAuto, map[string]interface{}{
			"access_id": access.ID,
			"approval":  apistructs.AuthorizationAuto,
		}); err == nil {
			return apierrors.CreateSLA.InvalidParameter(errors.Errorf(svc.text(ctx, "AutoAuthIsExists"), exAuto.Name))
		}
	}

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	if err := tx.Create(&slaModel).Error; err != nil {
		logrus.Errorf("failed to Create slaModel, body: %+v, err: %v", *req.Body, err)
		return apierrors.CreateSLA.InternalError(errors.New(svc.text(ctx, "FailedToCreateSLA")))
	}

	for _, v := range req.Body.Limits {
		if v.Limit == 0 {
			return apierrors.CreateSLA.InvalidParameter(svc.text(ctx, "LimitTimesCanNotBe0"))
		}
		if !v.Unit.Valid() {
			return apierrors.CreateSLA.InvalidParameter(svc.text(ctx, "InvalidDurationUnit"))
		}
		if err := tx.Create(&apistructs.SLALimitModel{
			BaseModel: apistructs.BaseModel{
				ID:        0,
				CreatedAt: timeNow,
				UpdatedAt: timeNow,
				CreatorID: req.Identity.UserID,
				UpdaterID: req.Identity.UserID,
			},
			SLAID: slaModel.ID,
			Limit: v.Limit,
			Unit:  v.Unit,
		}).Error; err != nil {
			logrus.Errorf("failed to Create limits, body: %+v, err: %v", *req.Body, err)
			return apierrors.CreateSLA.InternalError(errors.New(svc.text(ctx, "FailedToCreateSLALimit")))
		}
	}

	tx.Commit()

	return nil
}

// handleSpec 按顺序短路读取 Inode, SpecDiceFileUUID, Spec 三个字段.
// 如果 Inode 不为空, 则根据 Inode 查找 gittar 中对应的 API 文档, 将其作为待发布的文档,
// 并设置req.Source, req.AppID, req.Branch, req.ServiceName 等描述 spec 来源的字段;
// 如果 SpecDiceFileUUID 不为空, 则查找服务器中对应 API 文档文件, 将其作为待发布的文档;
// 如果 Spec 不为空, 则将其作为待发布的文档.
func (svc *Service) readSpec(ctx context.Context, req *apistructs.APIAssetVersionCreateRequest) error {
	if req.Inode != "" {
		content, apiError := apidocsvc.FetchAPIDocContent(req.OrgID, req.UserID, req.Inode, oasconv.Protocol(req.SpecProtocol), svc.branchRuleSvc)
		if apiError != nil {
			return apiError
		}
		if content.Meta == nil {
			return errors.New(svc.text(ctx, "FailedToQueryAPIDocContent") + ": content is nil")
		}
		var meta apistructs.APIDocMeta
		if err := json.Unmarshal(content.Meta, &meta); err != nil {
			return errors.Wrap(err, svc.text(ctx, "FailedToQueryAPIDocContent"))
		}
		req.Spec = meta.Blob.Content
		req.SpecDiceFileUUID = ""

		ft, err := bundle.NewGittarFileTree(req.Inode)
		if err != nil {
			return err
		}
		appID, err := strconv.ParseUint(ft.ApplicationID(), 10, 64)
		if err != nil {
			return err
		}
		req.Source = apistructs.CreateAPIAssetSourceDesignCenter
		req.AppID = appID
		req.Branch = ft.BranchName()
		req.ServiceName = content.Name

		return nil
	}

	if req.SpecDiceFileUUID != "" {
		// 从 dice files 获取文件
		fr, err := bdl.Bdl.DownloadDiceFile(req.SpecDiceFileUUID)
		if err != nil {
			return fmt.Errorf("failed to get spec from file: %v", err)
		}
		fileBytes, err := ioutil.ReadAll(fr)
		if err != nil {
			return fmt.Errorf("failed to read from file: %v", err)
		}
		req.Spec = string(fileBytes)
		req.SpecDiceFileUUID = ""
		return nil
	}

	if req.Spec == "" {
		return errors.New(svc.text(ctx, "NoValidAPIDocAddressOrContent"))
	}

	return nil
}

func (svc *Service) createOAS3IndexFragments(v3 *openapi3.Swagger, assetID, assetName string, versionID uint64) {
	var (
		version string
		timeNow = time.Now()
	)
	if v3.Info != nil {
		version = v3.Info.Version
	}

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	for path_, pathItem := range v3.Paths {
		for method, operation := range map[string]*openapi3.Operation{
			http.MethodDelete:  pathItem.Delete,
			http.MethodGet:     pathItem.Get,
			http.MethodHead:    pathItem.Head,
			http.MethodOptions: pathItem.Options,
			http.MethodPatch:   pathItem.Patch,
			http.MethodPost:    pathItem.Post,
			http.MethodPut:     pathItem.Put,
			http.MethodTrace:   pathItem.Trace,
			http.MethodConnect: pathItem.Connect,
		} {
			if operation == nil {
				continue
			}
			// 展开每一个 operation
			if err := oas3.ExpandOperation(operation, v3); err != nil {
				logrus.Errorf("failed to ExpandOperation, err: %v", err)
				continue
			}

			index := apistructs.APIOAS3IndexModel{
				ID:          0,
				CreatedAt:   timeNow,
				UpdatedAt:   timeNow,
				AssetID:     assetID,
				AssetName:   assetName,
				InfoVersion: version,
				VersionID:   versionID,
				Path:        path_,
				Method:      method,
				OperationID: operation.OperationID,
				Description: operation.Description,
			}
			if create := tx.Create(&index); create.Error != nil {
				continue
			}

			operationData, _ := json.Marshal(operation)
			fragment := apistructs.APIOAS3FragmentModel{
				ID:        0,
				CreatedAt: timeNow,
				UpdatedAt: timeNow,
				IndexID:   index.ID,
				VersionID: versionID,
				Operation: string(operationData),
			}
			if create := tx.Create(&fragment); create.Error != nil {
				continue
			}
		}
	}

	tx.Commit()
}

// parseSpec 解析 spec protocol 和 spec
func (svc *Service) parseSpec(ctx context.Context, protocol *apistructs.APISpecProtocol, spec string) (v3 *openapi3.Swagger, err error) {
	if spec == "" {
		return nil, apierrors.ValidateAPISpec.MissingParameter("spec")
	}

	var (
		data         = []byte(spec)
		m            map[string]interface{}
		fileFormat   = "json"
		fileProtocol = "oas3"
	)
	if err = json.Unmarshal(data, &m); err != nil {
		if err := yaml.Unmarshal(data, &m); err != nil {
			text := svc.text(ctx, "InvalidAPIDocFormat") + ":" + svc.text(ctx, "InvalidJsonYaml")
			return nil, apierrors.ValidateAPISpec.InvalidParameter(text)
		}
		fileFormat = "yaml"
	}

	if _, ok := m["openapi"]; !ok {
		if _, ok := m["swagger"]; !ok {
			return nil, apierrors.ValidateAPISpec.InvalidParameter(svc.text(ctx, "MissingOASProtoSign"))
		}
		fileProtocol = "oas2"
	}

	if fileFormat == "yaml" {
		if data, err = oasconv.YAMLToJSON(data); err != nil {
			return nil, apierrors.ValidateAPISpec.InvalidParameter(svc.text(ctx, "InvalidAPIDocFormat"))
		}
	}

	*protocol = apistructs.APISpecProtocol(fileProtocol + "-" + fileFormat)

	switch fileProtocol {
	case "oas2":
		v2, err := oas2.LoadFromData(data)
		if err != nil {
			return nil, err
		}
		v3, err = oasconv.OAS2ConvTo3(v2)
		if err != nil {
			return nil, err
		}
	case "oas3":
		v3, err = oas3.LoadFromData(data)
		if err != nil {
			return nil, err
		}
	default:
		return nil, apierrors.ValidateAPISpec.InvalidParameter("specProtocol")
	}

	if v3 == nil {
		return nil, errors.New("swagger is nil")
	}

	if err = oas3.ValidateOAS3(context.TODO(), *v3); err != nil {
		return nil, err
	}
	return v3, nil
}

// parseVersionInstanceRequest 校验 instanceType 和 对应字段
func validateVersionInstanceRequest(req apistructs.APIAssetVersionInstanceCreateRequest) error {
	if !req.InstanceType.Valid() {
		return apierrors.ValidateAPIInstance.InvalidParameter("instanceType")
	}
	switch req.InstanceType {
	case apistructs.APIInstanceTypeService:
		if req.RuntimeID == 0 {
			return apierrors.ValidateAPIInstance.MissingParameter("runtimeID")
		}
		if req.ServiceName == "" {
			return apierrors.ValidateAPIInstance.MissingParameter("serviceName")
		}
	case apistructs.APIInstanceTypeGateway:
		if req.EndpointID == "" {
			return apierrors.ValidateAPIInstance.MissingParameter("endpointID")
		}
	case apistructs.APIInstanceTypeOther:
		if req.URL == "" {
			return apierrors.ValidateAPIInstance.MissingParameter("url")
		}
	}
	return nil
}

// handleVersionInstanceRequest 处理 实例请求
func handleVersionInstanceRequest(req *apistructs.APIAssetVersionInstanceCreateRequest) {
	if req.Name == "" {
		switch req.InstanceType {
		case apistructs.APIInstanceTypeService:
			req.Name = "Runtime"
		case apistructs.APIInstanceTypeGateway:
			req.Name = "API Gateway"
		case apistructs.APIInstanceTypeOther:
			req.Name = "Direct URL"
		}
	}
}
