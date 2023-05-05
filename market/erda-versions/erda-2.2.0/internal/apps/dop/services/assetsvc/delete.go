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
	"strconv"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// DeleteAssetByAssetID 根据给定的 orgID 和 assetID 删除 APIAsset 表和 APIAssetVersionDetail 表的记录
func (svc *Service) DeleteAssetByAssetID(ctx context.Context, req apistructs.APIAssetDeleteRequest) error {
	// 参数校验
	if req.OrgID == 0 {
		return apierrors.DeleteAPIAsset.MissingParameter(apierrors.MissingOrgID)
	}

	// 查出这个 asset
	var asset apistructs.APIAssetsModel
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.AssetID,
	}); err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil
		}
		return apierrors.DeleteAPIAsset.InternalError(err)
	}

	// 鉴权: 当前用户是否具备删除权限
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.IdentityInfo.UserID)
	if written := writePermission(rolesSet, &asset); !written {
		return apierrors.DeleteAPIAsset.AccessDenied()
	}

	// 如果绑定了 access, 则不可删除
	if err := svc.FirstRecord(new(apistructs.APIAccessesModel), map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.AssetID,
	}); err == nil {
		return errors.New(svc.text(ctx, "APIAssetIsInManagement"))
	}

	return dbclient.DeleteAPIAssetByOrgAssetID(req.OrgID, req.AssetID, true)
}

// 根据给定的主键(id)删除 APIAssetVersion 表的记录
func (svc *Service) DeleteAssetVersionByID(ctx context.Context, orgID uint64, assetID string, versionID uint64, userID string) error {
	var (
		total, majorTotal, minorTotal uint64
		asset                         apistructs.APIAssetsModel
		version                       apistructs.APIAssetVersionsModel
		access                        apistructs.APIAccessesModel
	)

	// 查出一个 asset
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   orgID,
		"asset_id": assetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, assetID: %s, err: %v", assetID, err)
		return err
	}

	// 鉴权 当前用户是否具有删除此 asset 下的 version 的权限
	rolesSet := bdl.FetchAssetRolesSet(orgID, userID)
	if written := writePermission(rolesSet, &asset); !written {
		return apierrors.DeleteAPIAssetVersion.AccessDenied()
	}

	// 查出这个 version
	if err := svc.FirstRecord(&version, map[string]interface{}{
		"org_id":   orgID,
		"asset_id": assetID,
		"id":       versionID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord version, err: %v", err)
		return err
	}

	// 查询 minor 下是否有访问管理
	minorAccess := svc.FirstRecord(new(apistructs.APIAccessesModel), map[string]interface{}{
		"org_id":   orgID,
		"asset_id": assetID,
		"major":    version.Major,
		"minor":    version.Minor,
	}) == nil

	// 查询 swagger_version 下是否有访问管理
	majorAccess := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":   orgID,
		"asset_id": assetID,
		"major":    version.Major,
	}) == nil

	if err := dbclient.Sq().Model(new(apistructs.APIAssetVersionsModel)).
		Where(map[string]interface{}{
			"org_id":   orgID,
			"asset_id": assetID,
		}).Count(&total).
		Where("major = ?", version.Major).Count(&majorTotal).
		Where("minor = ?", version.Minor).Count(&minorTotal).
		Error; err != nil {
		return err
	}
	if total <= 1 {
		return errors.New(svc.text(ctx, "CanNotDelete.AtLeastOneVersion"))
	}
	if majorAccess && majorTotal <= 1 {
		return errors.Errorf(svc.text(ctx, "CanNotDelete.AtLeastOneVersionInManagement"),
			version.SwaggerVersion)
	}
	if minorAccess && minorTotal <= 1 {
		return errors.Errorf(svc.text(ctx, "CanNotDelete.AtLeastOnePatchVersionInManagement"),
			version.SwaggerVersion, version.Major, version.Minor)
	}

	return dbclient.DeleteAPIAssetVersion(orgID, assetID, versionID, true)
}

func (svc *Service) DeleteClient(req *apistructs.DeleteClientReq) *errorresp.APIError {
	// 查出这个 client
	var (
		clientModel apistructs.ClientModel
	)
	if err := svc.FirstRecord(&clientModel, map[string]interface{}{
		"org_id": req.OrgID,
		"id":     req.URIParams.ClientID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord clientModel, client id: %v, err: %v", req.URIParams.ClientID, err)
		return apierrors.DeleteClient.InternalError(err)
	}

	// 鉴权 当前用户是否可以删除此客户端 企业管理员, 当前客户端的创建者可删
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := req.Identity.UserID == clientModel.CreatorID ||
		inSlice(strconv.FormatUint(req.OrgID, 10), rolesSet.RolesOrgs(bdl.OrgMRoles...)); !written {
		return apierrors.DeleteClient.AccessDenied()
	}

	// 查出当前 client 下的所有合约
	var (
		contracts    []*apistructs.ContractModel
		contractsIDs []uint64
	)
	if err := svc.ListRecords(&contracts, map[string]interface{}{
		"org_id":    req.OrgID,
		"client_id": clientModel.ID,
	}); err == nil {
		for _, v := range contracts {
			contractsIDs = append(contractsIDs, v.ID)
		}
	}

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	if err := tx.Where(map[string]interface{}{"org_id": req.OrgID, "client_id": clientModel.ClientID}).
		Delete(new(apistructs.ClientModel)).
		Error; err != nil {
		logrus.Errorf("failed to delete ClientModel, err: %v", err)
		return apierrors.DeleteClient.InternalError(err)
	}

	if err := tx.Where(map[string]interface{}{"org_id": req.OrgID, "client_id": clientModel.ID}).
		Delete(new(apistructs.ContractModel)).Error; err != nil {
		logrus.Errorf("failed to Delete ContractModel, err: %v", err)
		return apierrors.DeleteClient.InternalError(err)
	}

	if err := tx.Where(map[string]interface{}{"org_id": req.OrgID}).
		Where("contract_id IN (?)", contractsIDs).
		Delete(new(apistructs.ContractRecordModel)).
		Error; err != nil {
		logrus.Errorf("failed to Delete ContractRecordModel, contractIDs: %v, err: %v", contractsIDs, err)
		return apierrors.DeleteClient.InternalError(err)
	}

	if err := bdl.Bdl.DeleteClientConsumer(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID, clientModel.ClientID); err != nil {
		logrus.Errorf("failed to DeleteClientConsumer, err: %v", err)
		return apierrors.DeleteClient.InternalError(err)
	}

	tx.Commit()

	return nil
}

func (svc *Service) DeleteAccess(ctx context.Context, req *apistructs.GetAccessReq) *errorresp.APIError {
	var (
		asset     apistructs.APIAssetsModel
		access    apistructs.APIAccessesModel
		contracts []*apistructs.ContractModel
	)

	// 查询这个 access
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id": req.OrgID,
		"id":     req.URIParams.AccessID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord access, err: %v", err)
		return apierrors.DeleteAccess.InternalError(err)
	}

	endpointID := access.EndpointID

	// 查询对应的 asset
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": access.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return apierrors.DeleteContract.InternalError(err)
	}

	// 鉴权 当前用户是否具有删除此 access 的权限
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := writePermission(rolesSet, &asset); !written {
		return apierrors.DeleteAccess.AccessDenied()
	}

	// 检查是否有人正在使用此 access
	if err := svc.ListRecords(&contracts, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        access.AssetID,
		"swagger_version": access.SwaggerVersion,
	}); err != nil {
		logrus.Errorf("failed to ListRecords contracts, err: %v", err)
	}
	for _, contract := range contracts {
		if contract.Status.ToLower() == apistructs.ContractApproved {
			return apierrors.DeleteAccess.InternalError(errors.Errorf(svc.text(ctx, "CanNotDelete.AuthorizedCcallRequestExists"),
				access.AssetName, access.SwaggerVersion))
		}
	}

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	// 先删除 access 名下的调用申请
	for _, contract := range contracts {
		var client apistructs.ClientModel
		if err := svc.FirstRecord(&client, map[string]interface{}{
			"org_id": req.OrgID,
			"id":     contract.ClientID,
		}); err != nil {
			logrus.Errorf("failed to FirstRecord client, contract: %+v, err: %v", contract, err)
			return apierrors.DeleteContract.InternalError(err)
		}
		if apiError := svc.deleteContract(ctx, tx.Sq(), req.OrgID, req.Identity.UserID, contract, &client, &asset, &access); apiError != nil {
			logrus.Errorf("failed to deleteContract, err: %v", apiError)
			return apiError
		}
	}

	if err := tx.Delete(&access).Error; err != nil {
		logrus.Errorf("failed to Delete access, err: %v", err)
		return apierrors.DeleteAccess.InternalError(err)
	}

	if err := bdl.Bdl.DeleteEndpoint(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID, endpointID); err != nil {
		return apierrors.DeleteAccess.InternalError(err)
	}

	tx.Commit()

	return nil
}

// DeleteContract deletes contract
func (svc *Service) DeleteContract(ctx context.Context, req *apistructs.GetContractReq) *errorresp.APIError {
	// 参数校验
	if req == nil || req.URIParams == nil {
		return apierrors.DeleteContract.InvalidParameter("parameters is invalid")
	}
	if req.OrgID == 0 {
		return apierrors.DeleteContract.InvalidParameter(apierrors.MissingOrgID)
	}

	var (
		contract apistructs.ContractModel
		client   apistructs.ClientModel
		asset    apistructs.APIAssetsModel
		access   apistructs.APIAccessesModel
	)

	// 查询 contract
	if err := svc.FirstRecord(&contract, map[string]interface{}{
		"org_id": req.OrgID,
		"id":     req.URIParams.ContractID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord contract, err: %v", err)
		if gorm.IsRecordNotFoundError(err) {
			return nil
		}
		return apierrors.DeleteContract.InternalError(err)
	}

	// 查询 client
	if err := svc.FirstRecord(&client, map[string]interface{}{
		"org_id": req.OrgID,
		"id":     req.URIParams.ClientID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord client, err: %v", err)
		return apierrors.DeleteContract.InternalError(err)
	}

	// 查询 asset
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": contract.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return apierrors.DeleteContract.InternalError(err)
	}

	// 鉴权 当前用户是否具备删除此 contract 的权限
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := req.Identity.UserID == contract.CreatorID || writePermission(rolesSet, &asset); !written {
		return apierrors.DeleteContract.AccessDenied()
	}

	// 查询 access
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        contract.AssetID,
		"swagger_version": contract.SwaggerVersion,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord access, err: %v", err)
		return apierrors.DeleteContract.InternalError(err)
	}

	return svc.deleteContract(ctx, nil, req.OrgID, req.Identity.UserID, &contract, &client, &asset, &access)
}

func (svc *Service) DeleteSLA(ctx context.Context, req *apistructs.DeleteSLAReq) *errorresp.APIError {
	if req == nil || req.URIParams == nil {
		return apierrors.DeleteSLA.InvalidParameter(svc.text(ctx, "InvalidParams"))
	}

	var (
		asset  apistructs.APIAssetsModel
		access apistructs.APIAccessesModel
		sla    apistructs.SLAModel // 要删除的 SLA
		count  uint64              // 访问管理下 SLA 数量
	)
	// 查出 asset
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return apierrors.DeleteSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindAPI")))
	}

	// SLA 的删除权限与对应的 API Asset 的 W 权限一致
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := writePermission(rolesSet, &asset); !written {
		return apierrors.DeleteSLA.AccessDenied()
	}

	// 查出 access
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord access, err: %v", err)
		return apierrors.DeleteSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindAccessItme")))
	}

	// 查出要删除的 SLA
	if err := svc.FirstRecord(&sla, map[string]interface{}{"id": req.URIParams.SLAID}); err != nil {
		logrus.Errorf("failed to FirstRecord sla, err: %v", err)
		return apierrors.DeleteSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindSLA")))
	}

	// 统计使用该 SLA 的合约, 如果存在则不允许删除
	if err := dbclient.Sq().Model(new(apistructs.ContractModel)).
		Where(map[string]interface{}{"cur_sla_id": req.URIParams.SLAID}).
		Count(&count).Error; err != nil {
		logrus.Errorf("failed to Count ContractModel, err: %v", err)
		return apierrors.DeleteSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindRelatedContract")))
	}
	if count > 0 {
		return apierrors.DeleteSLA.InternalError(errors.New(svc.text(ctx, "FailedToDeleteSLA.UsedInSLA")))
	}

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	// 删除记录
	if err := tx.Delete(new(apistructs.SLAModel), map[string]interface{}{
		"id": req.URIParams.SLAID,
	}).Error; err != nil {
		logrus.Errorf("failed to Delete SLAModel, err: %v", err)
		return apierrors.DeleteSLA.InternalError(err)
	}

	// 级联删除 limit 记录
	if err := tx.Delete(new(apistructs.SLALimitModel), map[string]interface{}{
		"sla_id": req.URIParams.SLAID,
	}).Error; err != nil {
		logrus.Errorf("failed to Delete SLALimitModel, err: %v", err)
		return apierrors.DeleteSLA.InternalError(err)
	}

	if err := tx.Model(new(apistructs.ContractModel)).
		Where("request_sla_id = ?", req.URIParams.SLAID).
		Updates(map[string]interface{}{
			"request_sla_id": nil,
		}).Error; err != nil && !gorm.IsRecordNotFoundError(err) {
		logrus.Errorf("failed to Update ContractModel.RequestSLAID, err: %v", err)
		return apierrors.DeleteSLA.InternalError(err)
	}

	// 如果删除是 access 的默认 SLA, 则级联更新 access
	tx.Model(new(apistructs.APIAccessesModel)).
		Where(map[string]interface{}{"org_id": req.OrgID, "id": access.ID}).
		Updates(map[string]interface{}{"default_sla_id": nil})

	tx.Commit()

	return nil
}

func (svc *Service) deleteContract(ctx context.Context, tx *gorm.DB, orgID uint64, userID string, contract *apistructs.ContractModel,
	client *apistructs.ClientModel, asset *apistructs.APIAssetsModel, access *apistructs.APIAccessesModel) *errorresp.APIError {
	org, err := svc.bdl.GetOrg(orgID)
	if err != nil {
		return apierrors.DeleteContract.InternalError(errors.Wrap(err, "failed to GetOrg"))
	}

	sq := tx
	if sq == nil {
		sq = dbclient.Sq()
	}

	if err := sq.Delete(new(apistructs.ContractModel), map[string]interface{}{
		"org_id": orgID,
		"id":     contract.ID,
	}).Error; err != nil {
		logrus.Errorf("failed to Delete ContractModel, err: %v", err)
		return apierrors.DeleteContract.InternalError(errors.Wrap(err, "failed to delete contract"))
	}

	// 操作记录也要删除
	sq.Delete(new(apistructs.ContractRecordModel), map[string]interface{}{
		"org_id":      orgID,
		"contract_id": contract.ID,
	})

	if err := bdl.Bdl.RevokeEndpointFromClient(strconv.FormatUint(orgID, 10), userID, client.ClientID, access.EndpointID); err != nil {
		logrus.Errorf("failed to RevokeEndpointFromClient, err: %v", err)
	}

	// 邮件和站内信
	go svc.contractMsgToUser(orgID, contract.CreatorID, asset.AssetName, client,
		svc.ApprovalResultWhileDelete(ctx, contract.Status, org.Locale))

	return nil
}
