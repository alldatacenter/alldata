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
	"fmt"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
)

func (svc *Service) UpdateAPIAsset(req *apistructs.UpdateAPIAssetReq) *errorresp.APIError {
	// parameters validation
	if req.URIParams == nil {
		return apierrors.UpdateAPIAsset.MissingParameter("URI parameters")
	}
	if req.URIParams.AssetID == "" {
		return apierrors.UpdateAPIAsset.MissingParameter("assetID")
	}
	if err := apistructs.ValidateAPIAssetID(req.URIParams.AssetID); err != nil {
		return apierrors.UpdateAPIAsset.InvalidParameter("assetID")
	}
	if req.Keys == nil {
		return apierrors.UpdateAPIAsset.MissingParameter("missing request body")
	}
	if req.OrgID == 0 {
		return apierrors.UpdateAPIAsset.MissingParameter("orgID")
	}

	// retrieve asset
	var asset apistructs.APIAssetsModel
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return apierrors.UpdateAPIAsset.InternalError(err)
	}

	// authentication
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := writePermission(rolesSet, &asset); !written {
		return apierrors.UpdateAPIAsset.AccessDenied()
	}

	var (
		where = map[string]interface{}{
			"org_id":   req.OrgID,
			"asset_id": req.URIParams.AssetID,
		}
		cascadeUpdates = map[string]interface{}{"asset_name": req.Keys["assetName"]}
	)

	req.Keys["updater_id"] = req.Identity.UserID
	req.Keys["updated_at"] = time.Now()

	// validate the length of assetName
	if assetName, ok := req.Keys["assetName"]; ok {
		assetNameS, ok := assetName.(string)
		if !ok {
			return apierrors.UpdateAPIAsset.InternalError(errors.New("assetName must be string"))
		}
		if err := strutil.Validate(assetNameS, strutil.MinLenValidator(1), strutil.MaxLenValidator(191)); err != nil {
			return apierrors.UpdateAPIAsset.InvalidParameter(errors.Wrapf(err, "assetName: %s", assetNameS))
		}
	}

	// check whether the user is doing the operation of clearing the associated project or application
	// (the key without a value)
	var (
		projectID, ok1 = req.Keys["projectID"]
		appID, ok2     = req.Keys["appID"]
		clearProject   = ok1 && projectID == nil
		clearApp       = ok2 && appID == nil
	)
	if clearProject {
		req.Keys["project_name"] = nil
	}
	if clearApp {
		req.Keys["app_name"] = nil
	}

	// update db
	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	if err := tx.Model(new(apistructs.APIAssetsModel)).
		Where(where).
		Updates(req.Keys).
		Error; err != nil {
		return apierrors.UpdateAPIAsset.InternalError(err)
	}

	// cascade update assetName in table access
	if cascadeUpdates["asset_name"] == nil {
		return nil
	}
	if err := tx.Model(new(apistructs.APIAccessesModel)).Where(where).Updates(cascadeUpdates).Error; err != nil {
		return apierrors.UpdateAPIAsset.InternalError(errors.Wrap(err, "failed to UpdateAccess"))
	}

	// cascade update assetName in table version
	if err := tx.Model(new(apistructs.APIAssetVersionsModel)).Where(where).Updates(cascadeUpdates).Error; err != nil {
		return apierrors.UpdateAPIAsset.InternalError(errors.Wrap(err, "failed to UpdateVersion"))
	}

	// cascade update assetName in table contract
	if err := tx.Model(new(apistructs.ContractModel)).Where(where).Updates(cascadeUpdates).Error; err != nil {
		return apierrors.UpdateAPIAsset.InternalError(errors.Wrap(err, "failed to UpdateContract"))
	}

	tx.Commit()

	return nil
}

func (svc *Service) UpdateInstantiation(ctx context.Context, req *apistructs.UpdateInstantiationReq) (*apistructs.InstantiationModel, *errorresp.APIError) {
	// parameters validation
	if req.URIParams == nil {
		return nil, apierrors.UpdateInstantiation.MissingParameter("URI parameters")
	}
	if err := apistructs.ValidateAPIAssetID(req.URIParams.AssetID); err != nil {
		return nil, apierrors.UpdateInstantiation.InvalidParameter("assetID")
	}
	if req.Body == nil {
		return nil, apierrors.UpdateInstantiation.InvalidParameter("missing request body")
	}
	if req.OrgID == 0 {
		return nil, apierrors.UpdateInstantiation.InvalidParameter("OrgID")
	}

	var (
		asset  apistructs.APIAssetsModel
		access apistructs.APIAccessesModel
	)
	// retrieve asset
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, assetID: %s, err: %v", req.URIParams.AssetID, err)
		return nil, apierrors.UpdateInstantiation.InternalError(err)
	}

	// authentication
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := writePermission(rolesSet, &asset); !written {
		return nil, apierrors.UpdateInstantiation.AccessDenied()
	}

	// can not update if there is an Access
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
		"minor":           req.URIParams.Minor,
	}); err == nil {
		return nil, apierrors.UpdateInstantiation.InternalError(errors.New(svc.text(ctx, "CanNotUpdateInstance")))
	}

	// parse the url of runtime instance
	var (
		urlStr = req.Body.URL
	)
	if urlStr == "" {
		// delete the association relationship with the instance if the giving url is ""
		if err := dbclient.Sq().Delete(new(apistructs.InstantiationModel), map[string]interface{}{
			"id": req.URIParams.InstantiationID,
		}).Error; err != nil {
			return nil, apierrors.UpdateInstantiation.InternalError(errors.Wrap(err, "failed to clear instance url"))
		}
		return nil, nil
	}

	if !strings.HasPrefix(urlStr, "http://") && !strings.HasPrefix(urlStr, "https://") && !strings.HasPrefix(urlStr, "//") {
		urlStr = "//" + strings.TrimLeft(urlStr, "/")
	}
	_, err := url.Parse(urlStr)
	if err != nil {
		return nil, apierrors.UpdateInstantiation.InternalError(errors.Wrap(err, "invalid instance url"))
	}

	if err := dbclient.UpdateInstantiation(req); err != nil {
		return nil, apierrors.UpdateInstantiation.InternalError(err)
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
		return nil, apierrors.UpdateInstantiation.InternalError(errors.New("failed to GetInstantiations"))
	}

	return instantiations, nil
}

func (svc *Service) UpdateClient(req *apistructs.UpdateClientReq) (*apistructs.ClientModel, *apistructs.SK, *errorresp.APIError) {
	if req == nil || req.URIParams == nil || req.QueryParams == nil || req.Body == nil {
		return nil, nil, apierrors.UpdateClient.InvalidParameter("invalid parameter or body")
	}

	var (
		model apistructs.ClientModel
		where = map[string]interface{}{
			"org_id": req.OrgID,
			"id":     req.URIParams.ClientID,
		}
	)
	if err := svc.FirstRecord(&model, where); err != nil {
		return nil, nil, apierrors.UpdateClient.InternalError(err)
	}

	var sk apistructs.SK
	if req.QueryParams.ResetClientSecret {
		credentials, err := bdl.Bdl.ResetClientCredentials(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
			model.ClientID)
		if err != nil {
			return nil, nil, apierrors.UpdateClient.InternalError(err)
		}
		sk.ClientID = credentials.ClientId
		sk.ClientSecret = credentials.ClientSecret
	} else {
		credentials, err := bdl.Bdl.GetClientCredentials(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
			model.ClientID)
		if err != nil {
			return nil, nil, apierrors.UpdateClient.InternalError(err)
		}
		sk.ClientID = credentials.ClientId
		sk.ClientSecret = credentials.ClientSecret
	}

	var (
		updates = map[string]interface{}{
			"displayName": req.Body.DisplayName,
			"desc":        req.Body.Desc,
			"updater_id":  req.Identity.UserID,
			"updated_at":  time.Now(),
		}
	)
	if err := dbclient.Sq().Model(&model).Where(where).Updates(updates).Error; err != nil {
		return nil, nil, apierrors.UpdateClient.InternalError(err)
	}
	return &model, &sk, nil
}

func (svc *Service) UpdateContract(ctx context.Context, req *apistructs.UpdateContractReq) (*apistructs.ClientModel, *apistructs.ContractModel,
	*errorresp.APIError) {
	if req == nil || req.URIParams == nil || req.Body == nil {
		return nil, nil, apierrors.UpdateContract.InvalidParameter("invalid parameter")
	}

	contractID, err := strconv.ParseUint(req.URIParams.ContractID, 10, 64)
	if err != nil {
		return nil, nil, apierrors.UpdateContract.InvalidParameter("invalid contract id")
	}

	var (
		client   apistructs.ClientModel
		asset    apistructs.APIAssetsModel
		contract apistructs.ContractModel
		access   apistructs.APIAccessesModel
		where    = map[string]interface{}{
			"org_id": req.OrgID,
			"id":     contractID,
		}
	)

	// retrieve the client
	if err := svc.FirstRecord(&client, map[string]interface{}{
		"org_id": req.OrgID,
		"id":     req.URIParams.ClientID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord client, err: %v", err)
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil, apierrors.UpdateContract.InternalError(errors.New(svc.text(ctx, "ClientNotFound")))
		}
		return nil, nil, apierrors.UpdateContract.InternalError(err)
	}

	// retrieve the contract
	if err := svc.FirstRecord(&contract, where); err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil, apierrors.UpdateContract.InternalError(errors.New("contract not found"))
		}
		return nil, nil, apierrors.UpdateContract.InternalError(err)
	}

	// retrieve the  asset
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": contract.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return nil, nil, apierrors.UpdateContract.InternalError(err)
	}

	// authentication: can the user approve it ?
	// 鉴权 当前用户是否具备审批权限
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	written := writePermission(rolesSet, &asset)

	// retrieve access
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        contract.AssetID,
		"swagger_version": contract.SwaggerVersion,
	}); err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil, apierrors.UpdateContract.InternalError(errors.New("access not found"))
		}
		return nil, nil, apierrors.UpdateContract.InternalError(err)
	}

	defer func() {
		err := svc.createOrUpdateClientLimits(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
			access.EndpointID, client.ClientID, contractID)
		if err != nil {
			logrus.Errorf("createOrUpdateClientLimits failed, err:%+v", err)
		}
	}()

	switch {
	case req.Body.RequestSLAID != nil:
		// If the current user is not an org administrator or the client creator, he cannot apply for a new SLA
		// 如果不是企业管理员, 也不是客户端创建者, 则不能申请新的 SLA
		if !inSlice(strconv.FormatUint(req.OrgID, 10), rolesSet.RolesOrgs(bdl.OrgMRoles...)) && req.Identity.UserID != contract.CreatorID {
			return nil, nil, apierrors.UpdateContract.AccessDenied()
		}
		if err := svc.updateContractRequestSLA(ctx, req, &contract, &access, &asset); err != nil {
			logrus.Errorf("failed to updateContractRequestSLA, err: %v", err)
			return nil, nil, apierrors.UpdateContract.InternalError(errors.New(svc.text(ctx, "FailedToApplySLA")))
		}

	case req.Body.Status != nil:
		if !written {
			return nil, nil, apierrors.UpdateContract.AccessDenied()
		}

		if err := svc.updateContractStatus(ctx, req, &client, &access, &contract); err != nil {
			logrus.Errorf("failed to updateContractStatus, err: %v", err)
			return nil, nil, apierrors.UpdateContract.InternalError(errors.New(svc.text(ctx, "FailedToProcessContract")))
		}

	case req.Body.CurSLAID != nil:
		if !written {
			return nil, nil, apierrors.UpdateContract.AccessDenied()
		}

		if err := svc.updateContractCurSLA(ctx, req, &contract, &client, &access); err != nil {
			logrus.Errorf("failed to updateContractCurSLA, err: %v", err)
			return nil, nil, apierrors.UpdateContract.InternalError(errors.New(svc.text(ctx, "FailedToUpdateSLA")))
		}

	default:
		return nil, nil, apierrors.UpdateContract.InvalidParameter("invalid request body")
	}

	return &client, &contract, nil
}

// the manager modifies the contract status (approve the contract)
func (svc *Service) updateContractStatus(ctx context.Context, req *apistructs.UpdateContractReq, client *apistructs.ClientModel, access *apistructs.APIAccessesModel,
	contract *apistructs.ContractModel) error {
	org, err := svc.bdl.GetOrg(req.OrgID)
	if err != nil {
		return errors.Wrap(err, "failed to GetOrg")
	}

	if req.Body.Status == nil {
		return nil
	}

	// do something depends on contract status
	status := req.Body.Status.ToLower()
	switch status {
	case apistructs.ContractApproved:
		// the manager change the contract status to "approved", call the api-gateway to grant to the client
		if err := bdl.Bdl.GrantEndpointToClient(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
			client.ClientID, access.EndpointID); err != nil {
			return err
		}
	case apistructs.ContractDisapproved:
		// do nothing with api-gateway
	case apistructs.ContractUnapproved:
		//  call the api-gateway to revoke the grant
		if err := bdl.Bdl.RevokeEndpointFromClient(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
			client.ClientID, access.EndpointID); err != nil {
			return err
		}
	default:
		return errors.New("invalid contract status")
	}

	timeNow := time.Now()
	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	if err := tx.Model(contract).
		Where(map[string]interface{}{"org_id": req.OrgID, "id": req.URIParams.ContractID}).
		Updates(map[string]interface{}{"status": status, "updated_at": timeNow}).
		Error; err != nil {
		return errors.Wrap(err, "failed to Updates contractModel")
	}

	action := status2Action(status)
	if err := tx.Create(&apistructs.ContractRecordModel{
		ID:         0,
		OrgID:      req.OrgID,
		ContractID: contract.ID,
		Action:     fmt.Sprintf("%s对该调用申请的授权", action),
		CreatorID:  req.Identity.UserID,
		CreatedAt:  timeNow,
	}).Error; err != nil {
		return errors.Wrap(err, "failed to Create contractRecordModel")
	}

	tx.Commit()

	// notification by mail and in-site letter
	go svc.contractMsgToUser(req.OrgID, contract.CreatorID, access.AssetName, client,
		svc.ApprovalResultFromStatus(ctx, status, org.Locale))

	return nil
}

// 管理人员修改合约的 SLA
func (svc *Service) updateContractCurSLA(ctx context.Context, req *apistructs.UpdateContractReq, contract *apistructs.ContractModel, client *apistructs.ClientModel,
	access *apistructs.APIAccessesModel) error {
	org, err := svc.bdl.GetOrg(req.OrgID)
	if err != nil {
		return errors.Wrap(err, "failed to GetOrg")
	}

	if req.Body.CurSLAID == nil {
		return nil
	}

	// 查出 SLA
	sla := svc.unlimitedSLA(ctx, access)
	if *req.Body.CurSLAID != 0 {
		if err := svc.FirstRecord(sla, map[string]interface{}{"id": *req.Body.CurSLAID}); err != nil {
			return errors.Wrap(err, "failed to FirstRecord sla")
		}
	}

	// update contract
	var (
		timeNow = time.Now()
		updates = map[string]interface{}{
			"cur_sla_id":       *req.Body.CurSLAID,
			"updated_at":       timeNow,
			"updater_id":       req.Identity.UserID,
			"sla_committed_at": timeNow,
		}
		action = fmt.Sprintf("将 SLA 变更为 %s", sla.Name)
	)

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	if contract.RequestSLAID != nil && *contract.RequestSLAID == *req.Body.CurSLAID {
		updates["request_sla_id"] = nil
		action = fmt.Sprintf("通过了对名称为 %s 的 SLA 的申请", sla.Name)
	}

	if err := tx.Model(&contract).Updates(updates).Error; err != nil {
		return errors.Wrap(err, "failed to Updates contract")
	}

	// 新增操作记录
	if err := tx.Create(&apistructs.ContractRecordModel{
		ID:         0,
		OrgID:      req.OrgID,
		ContractID: contract.ID,
		Action:     action,
		CreatorID:  req.Identity.UserID,
		CreatedAt:  timeNow,
	}).Error; err != nil {
		return errors.Wrap(err, "failed to Create contractRecordModel")
	}

	tx.Commit()

	go svc.contractMsgToUser(req.OrgID, contract.CreatorID, access.AssetName, client,
		svc.ApprovalResultSLAUpdated(ctx, sla.Name, org.Locale))

	return nil
}

func (svc *Service) updateContractRequestSLA(ctx context.Context, req *apistructs.UpdateContractReq, contract *apistructs.ContractModel, access *apistructs.APIAccessesModel,
	asset *apistructs.APIAssetsModel) error {
	org, err := svc.bdl.GetOrg(req.OrgID)
	if err != nil {
		return errors.Wrap(err, "failed to GetOrg")
	}

	// 如果申请的是本来就在申请中(页面逻辑不会出现这种情况), 直接返回
	if contract.RequestSLAID != nil {
		if *req.Body.RequestSLAID == *contract.RequestSLAID {
			return nil
		}
		if *req.Body.RequestSLAID == 0 {
			return errors.New("invalid SLA ID")
		}
	}

	// 查出 SLA
	var (
		sla      apistructs.SLAModel
		whereSLA = map[string]interface{}{
			"id": *req.Body.RequestSLAID,
		}
		client      apistructs.ClientModel
		whereClient = map[string]interface{}{"id": contract.ClientID}
	)
	if err := svc.FirstRecord(&sla, whereSLA); err != nil {
		return errors.Wrap(err, "failed to FirstRecord sla")
	}
	if err := svc.FirstRecord(&client, whereClient); err != nil {
		return errors.Wrap(err, "failed to FirstRecord client")
	}

	var (
		timeNow       = time.Now()
		whereContract = map[string]interface{}{"id": contract.ID}
		updates       = map[string]interface{}{
			"request_sla_id": req.Body.RequestSLAID,
			"updated_at":     timeNow,
			"updater_id":     req.Identity.UserID,
		}
		record = apistructs.ContractRecordModel{
			ID:         0,
			OrgID:      req.OrgID,
			ContractID: contract.ID,
			Action:     fmt.Sprintf("申请了名称为 %s 的 SLA", sla.Name),
			CreatorID:  req.Identity.UserID,
			CreatedAt:  timeNow,
		}
	)

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	// 如果申请的是自动授权的
	if sla.Approval.ToLower() == apistructs.AuthorizationAuto {
		updates["cur_sla_id"] = *req.Body.RequestSLAID
		updates["sla_committed_at"] = timeNow
		updates["request_sla_id"] = nil
	}

	// 如果申请的是当前的(页面逻辑不会出现这种情况)
	if contract.CurSLAID != nil && *contract.CurSLAID == *req.Body.RequestSLAID {
		updates["request_sla_id"] = nil
	}

	if err := tx.Model(&contract).
		Where(whereContract).
		Updates(updates).
		Error; err != nil {
		return errors.Wrap(err, "failed to Updates contract")
	}

	if err := tx.Create(&record).Error; err != nil {
		return errors.Wrap(err, "failed to Create record")
	}

	tx.Commit()

	go svc.contractMsgToManager(ctx, req.OrgID, req.Identity.UserID, asset, access,
		svc.RequestItemSLA(ctx, sla.Name, org.Locale),
		updates["request_sla_id"] == nil)

	return nil
}

func (svc *Service) UpdateAccess(ctx context.Context, req *apistructs.UpdateAccessReq) (*apistructs.APIAccessesModel, *errorresp.APIError) {
	if req == nil || req.Body == nil {
		return nil, apierrors.UpdateAccess.InvalidParameter("invalid parameters")
	}

	var (
		asset  apistructs.APIAssetsModel
		access apistructs.APIAccessesModel
		where  = map[string]interface{}{
			"org_id": req.OrgID,
			"id":     req.URIParams.AccessID,
		}
		updates = map[string]interface{}{
			"minor":           req.Body.Minor,
			"workspace":       req.Body.Workspace,
			"authentication":  req.Body.Authentication,
			"authorization":   req.Body.Authorization,
			"bindDomain":      strings.Join(req.Body.BindDomain, ""),
			"addonInstanceID": req.Body.AddonInstanceID,
		}
	)

	// 查找这个 access
	if err := svc.FirstRecord(&access, where); err != nil {
		logrus.Errorf("failed to FirstRecord access, where: %v, err: %v", where, err)
		if gorm.IsRecordNotFoundError(err) {
			return nil, apierrors.UpdateAccess.InternalError(errors.New("access not found"))
		}
		return nil, apierrors.UpdateAccess.InternalError(err)
	}

	// 查出对应的 asset
	var whereFirstAsset = map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": access.AssetID,
	}
	if err := svc.FirstRecord(&asset, whereFirstAsset); err != nil {
		logrus.Errorf("failed to FirstRecord asset, where: %v, err: %v", whereFirstAsset, err)
		return nil, apierrors.UpdateAccess.InternalError(errors.Errorf("API %s not found", access.AssetName))

	}

	// 鉴权 当前用户是否具备修改 access 的权限
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := req.Identity.UserID == access.CreatorID || writePermission(rolesSet, &asset); !written {
		return nil, apierrors.UpdateAccess.AccessDenied()
	}

	// 查找这个 access 原来的实例
	var (
		exInstantiation  apistructs.InstantiationModel
		whereFirstExInst = map[string]interface{}{
			"org_id":          req.OrgID,
			"asset_id":        access.AssetID,
			"swagger_version": access.SwaggerVersion,
			"minor":           access.Minor,
		}
	)
	if err := svc.FirstRecord(&exInstantiation, whereFirstExInst); err != nil {
		logrus.Errorf("failed to FirstRecord exInstantiation, where: %v, err: %v", whereFirstExInst, err)
		return nil, apierrors.UpdateAccess.InternalError(err)
	}

	// 查找 access 的 projectName
	if project, err := bdl.Bdl.GetProject(access.ProjectID); err == nil {
		updates["project_name"] = project.Name
	}

	// 查找即将关联的实例
	var (
		instantiations apistructs.InstantiationModel
		whereFirstInst = map[string]interface{}{
			"asset_id":        access.AssetID,
			"swagger_version": access.SwaggerVersion,
			"minor":           req.Body.Minor,
		}
	)
	if err := svc.FirstRecord(&instantiations, whereFirstInst); err != nil {
		logrus.Errorf("failed to FirstRecord instantiation, ")
		if gorm.IsRecordNotFoundError(err) {
			return nil, apierrors.UpdateAccess.InternalError(errors.New("no instantiation"))
		}
		return nil, apierrors.UpdateAccess.InternalError(err)
	}

	// 检查两次实例的 workspace 是否被切换 (不允许切换)
	if exInstantiation.Workspace != instantiations.Workspace {
		return nil, apierrors.UpdateAccess.InternalError(errors.Errorf("can not checkout from %v.%v.* [workspace:%s] to %v.%v.* [workspace:%s]: different workspace",
			exInstantiation.Major, exInstantiation.Minor, exInstantiation.Workspace,
			instantiations.Major, exInstantiation.Minor, exInstantiation.Workspace))
	}

	if strings.ToLower(instantiations.Type) == InsTypeDice {
		// 检查实例的环境与请求的环境是否一致 (不允许不一致)
		if instantiations.Workspace != req.Body.Workspace {
			return nil, apierrors.UpdateAccess.InternalError(errors.Errorf("can not update, different workspace between the intance [%s] and the gateway [%s]",
				instantiations.Workspace, req.Body.Workspace))
		}
	}

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	// 更新
	if err := tx.Model(&access).Where(where).Updates(updates).Error; err != nil {
		return nil, apierrors.UpdateInstantiation.InternalError(err)
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
		return nil, apierrors.UpdateAccess.InternalError(errors.Wrap(err, "invalid instance url"))
	}
	host = parsedURL.Host
	path = parsedURL.Path
	if path == "" {
		path = "/"
	}

	// 更新流量入口
	authType := apistructs.AT_KEY_AUTH
	switch req.Body.Authentication.ToLower() {
	case apistructs.AuthenticationKeyAuth:
	case apistructs.AuthenticationSignAuth:
		authType = apistructs.AT_SIGN_AUTH
	case apistructs.AuthenticationOAuth2:
		return nil, apierrors.CreateAccess.InvalidParameter("not support OAuth2 yet")
	default:
		return nil, apierrors.CreateAccess.InvalidParameter("not support the authentication method yet")
	}
	if err := bdl.Bdl.UpdateEndpoint(
		strconv.FormatUint(req.OrgID, 10),
		req.Identity.UserID,
		access.EndpointID, apistructs.PackageDto{
			Name:             fmt.Sprintf("%s_%d_%s", access.AssetID, access.Major, access.Workspace),
			BindDomain:       req.Body.BindDomain,
			AuthType:         authType,
			AclType:          apistructs.ACL_ON,
			Scene:            apistructs.OPENAPI_SCENE,
			Description:      "creation of endpoint from apim",
			NeedBindCloudapi: false,
		}); err != nil {
		return nil, apierrors.UpdateAccess.InternalError(errors.Wrap(err, "failed to UpdateEndpoint"))
	}

	// 更新路由配置
	if err := bdl.Bdl.CreateOrUpdateEndpointRootRoute(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
		access.EndpointID, host, path); err != nil {
		return nil, apierrors.UpdateAccess.InternalError(errors.Wrap(err, "failed to UpdateEndpointRootRoute"))
	}

	tx.Commit()

	return &access, nil
}

func (svc *Service) UpdateSLA(ctx context.Context, req *apistructs.UpdateSLAReq) *errorresp.APIError {
	if req == nil || req.URIParams == nil || req.Body == nil {
		return apierrors.UpdateSLA.InvalidParameter(svc.text(ctx, "InvalidParams"))
	}
	if len(req.Body.Limits) != 1 {
		return apierrors.UpdateSLA.InvalidParameter("at least and only one limit condition can be set")
	}
	limit := req.Body.Limits[0]
	if limit.Limit == 0 {
		return apierrors.UpdateSLA.InvalidParameter("the limit times can not be 0")
	}
	if !limit.Unit.Valid() {
		return apierrors.UpdateSLA.InvalidParameter("invalid time unit")
	}

	org, err := svc.bdl.GetOrg(req.OrgID)
	if err != nil {
		return apierrors.UpdateSLA.InternalError(errors.Wrap(err, "failed to GetOrg"))
	}

	var (
		sla    apistructs.SLAModel
		asset  apistructs.APIAssetsModel
		access apistructs.APIAccessesModel
		where  = map[string]interface{}{
			"id": req.URIParams.SLAID,
		}
		updates = make(map[string]interface{})
		timeNow = time.Now()
	)
	// 查出 asset
	if err := svc.FirstRecord(&asset, map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord asset, err: %v", err)
		return apierrors.UpdateSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindAPIAsset")))
	}

	// SLA 的编辑权限与 API Asset 的 W 权限一致
	rolesSet := bdl.FetchAssetRolesSet(req.OrgID, req.Identity.UserID)
	if written := writePermission(rolesSet, &asset); !written {
		return apierrors.UpdateSLA.AccessDenied()
	}

	// 查出 access
	if err := svc.FirstRecord(&access, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": req.URIParams.SwaggerVersion,
	}); err != nil {
		logrus.Errorf("failed to FirstRecord access, err: %v", err)
		return apierrors.UpdateSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindAccessItem")))
	}

	// 查出 SLA
	if err := svc.FirstRecord(&sla, where); err != nil {
		logrus.Errorf("failed to FirstRecord sla, err: %v", err)
		return apierrors.UpdateSLA.InternalError(errors.New(svc.text(ctx, "FailedToFindSLA")))
	}

	// 如果要修改名称, 则要检查是否被改成同名的 SLA
	if req.Body.Name != nil {
		if trimSpace := strings.Replace(*req.Body.Name, " ", "", -1); trimSpace == "UnlimitedSLA" || trimSpace == "无限制SLA" {
			return apierrors.CreateSLA.InternalError(errors.Errorf(svc.text(ctx, "CanNotNamed")+":"+svc.text(ctx, "SystemReserved"), *req.Body.Name))
		}
	}
	var (
		exName apistructs.SLAModel
	)
	if err := dbclient.Sq().Model(&exName).Where(map[string]interface{}{
		"access_id": access.ID,
		"name":      req.Body.Name,
	}).Where("id != ?", req.URIParams.SLAID).
		Find(&exName).Error; err == nil {
		return apierrors.UpdateSLA.InvalidParameter(errors.New(svc.text(ctx, "SameNameSLA")))
	}

	// 如果要更新授权方式为自动, 则要先验证是否已存在自动授权的 SLA
	if req.Body.Approval.ToLower() == apistructs.AuthorizationAuto {
		var exAuto apistructs.SLAModel
		if err := dbclient.Sq().Model(&exAuto).Where(map[string]interface{}{
			"access_id": access.ID,
			"approval":  apistructs.AuthorizationAuto,
		}).Where("id != ?", req.URIParams.SLAID).
			Find(&exAuto).Error; err == nil {
			return apierrors.UpdateSLA.InvalidParameter(errors.Errorf(svc.text(ctx, "AutoSLA"), exAuto.Name))
		}
	}

	tx := dbclient.Tx()
	defer tx.RollbackUnlessCommitted()

	// 更新基本信息
	if req.Body.Name != nil {
		updates["name"] = *req.Body.Name
	}
	if req.Body.Desc != nil {
		updates["desc"] = *req.Body.Name
	}
	if req.Body.Approval != nil && req.Body.Approval.Valid() {
		updates["approval"] = *req.Body.Approval
	}
	if len(updates) > 0 {
		if err := tx.Model(new(apistructs.SLAModel)).
			Where(where).
			Updates(updates).
			Error; err != nil {
			logrus.Errorf("failed to Update SLAModel, err: %v", err)
			return apierrors.UpdateSLA.InternalError(errors.New("failed to update SLA"))
		}
	}

	// 更新 limit
	if err := tx.Delete(new(apistructs.SLALimitModel), map[string]interface{}{"sla_id": sla.ID}).Error; err != nil {
		logrus.Errorf("failed to Delete SLALimitModel, err: %v", err)
		return apierrors.DeleteSLA.InternalError(errors.New("failed to override limit"))
	}
	if err := tx.Create(&apistructs.SLALimitModel{
		BaseModel: apistructs.BaseModel{
			ID:        0,
			CreatedAt: timeNow,
			UpdatedAt: timeNow,
			CreatorID: req.Identity.UserID,
			UpdaterID: req.Identity.UserID,
		},
		SLAID: sla.ID,
		Limit: limit.Limit,
		Unit:  limit.Unit,
	}).Error; err != nil {
		logrus.Errorf("failed to Create limits, err: %v", err)
		return apierrors.UpdateSLA.InternalError(errors.New("failed to update limit"))
	}

	if len(updates) > 0 {
		switch approval := req.Body.Approval.ToLower(); {
		// 如果 SLA 授权方式从手动改为自动, 则将 access 的默认 SLA 设为该 SLA
		case sla.Approval.ToLower() == apistructs.AuthorizationManual && approval == apistructs.AuthorizationAuto:
			tx.Model(new(apistructs.APIAccessesModel)).
				Where(map[string]interface{}{"org_id": req.OrgID, "id": access.ID}).
				Updates(map[string]interface{}{"default_sla_id": sla.ID})

		// 如果本来该 SLA 是 access 默认 SLA, 现在改为手动了, 则要将 access 的默认 SLA 清空
		case access.DefaultSLAID != nil && *access.DefaultSLAID == sla.ID && approval == apistructs.AuthorizationManual:
			tx.Model(new(apistructs.APIAccessesModel)).
				Where(map[string]interface{}{"org_id": req.OrgID, "id": access.ID}).
				Updates(map[string]interface{}{"default_sla_id": nil})
		}
	}

	tx.Commit()

	// 调用 hepa 依赖, 更新网关侧流量限制
	go func() {
		// 查询受影响的客户端列表
		affectedClients, affectedContracts, err := svc.slaAffectClients(sla.ID)
		if err != nil {
			logrus.Warnf("failed to get slaAffectClients, err: %v", err)
			return
		}

		// 推送消息
		sentContractIDs := make(map[uint64]bool)
		for _, affectedClient := range affectedClients {
			for _, affectedContract := range affectedContracts {
				if _, ok := sentContractIDs[affectedContract.ID]; ok {
					continue
				}
				svc.contractMsgToUser(req.OrgID, affectedContract.CreatorID, asset.AssetName, affectedClient,
					svc.ManagerRewriteSLA(ctx, sla.Name, org.Locale))
				err = svc.createOrUpdateClientLimits(strconv.FormatUint(req.OrgID, 10), req.Identity.UserID,
					access.EndpointID, affectedClient.ClientID, affectedContract.ID)
				if err != nil {
					logrus.Errorf("createOrUpdateClientLimits failed, err:%+v", err)
				}
			}
		}
	}()

	return nil
}

// 修改 asset version
func (svc *Service) UpdateAssetVersion(ctx context.Context, req *apistructs.UpdateAssetVersionReq) (*apistructs.APIAssetVersionsModel, *errorresp.APIError) {
	if req == nil || req.URIParams == nil {
		return nil, apierrors.UpdateAssetVersion.InvalidParameter(svc.text(ctx, "InvalidParams"))
	}
	if req.Body == nil {
		return nil, apierrors.UpdateAssetVersion.InvalidParameter("invalid request body")
	}

	// 鉴权
	if written := svc.writeAssetPermission(req.OrgID, req.Identity.UserID, req.URIParams.AssetID); !written {
		return nil, apierrors.UpdateAssetVersion.AccessDenied()
	}

	// 查出版本
	var version apistructs.APIAssetVersionsModel
	if err := svc.FirstRecord(&version, map[string]interface{}{"id": req.URIParams.VersionID, "org_id": req.OrgID}); err != nil {
		logrus.Errorf("failed to FirstRecord version, err: %v", err)
		return nil, apierrors.UpdateAssetVersion.InternalError(errors.New(svc.text(ctx, "FailedToFindVersion")))
	}

	// 更新版本
	updates := map[string]interface{}{
		"deprecated": req.Body.Deprecated,
		"updated_at": time.Now(),
		"updater_id": req.Identity.UserID,
	}
	if err := dbclient.Sq().Model(&version).Updates(updates).Error; err != nil {
		logrus.Errorf("failed to FirstRecord version, err: %v", err)
		return nil, apierrors.UpdateAssetVersion.InternalError(errors.New("failed to set the version deprecated"))
	}

	// 消息通知
	go svc.updateAssetVersionMsg(req, &version)

	return &version, nil
}

func (svc *Service) updateAssetVersionMsg(req *apistructs.UpdateAssetVersionReq, version *apistructs.APIAssetVersionsModel) {
	// 查询 minor latest version
	var latestVersion apistructs.APIAssetVersionsModel
	if err := dbclient.Sq().Where(map[string]interface{}{
		"org_id":   req.OrgID,
		"asset_id": req.URIParams.AssetID,
		"major":    version.Major,
		"minor":    version.Minor,
	}).Order("patch DESC").
		First(&latestVersion).
		Error; err != nil {
		logrus.Errorf("failed to first latestVersion, err: %v", err)
		return
	}
	// 如果受影响的不是最新版本, 则无需通知消息
	if latestVersion.ID != version.ID {
		logrus.Debug("needless to msg for version id")
		return
	}

	// 查询 access
	var access apistructs.APIAccessesModel
	if err := dbclient.Sq().Where(map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": version.SwaggerVersion,
	}).First(&access).Error; err != nil {
		logrus.Warnf("failed to first access, %v", err)
		return
	}

	// 如果 access 所用的 minor 版本与修改的 version 的 minor 不一致, 则说明没有收到影响
	if access.Minor != version.Minor {
		return
	}

	// 查询受影响的合约
	var contracts []*apistructs.ContractModel
	if err := svc.ListRecords(&contracts, map[string]interface{}{
		"org_id":          req.OrgID,
		"asset_id":        req.URIParams.AssetID,
		"swagger_version": version.SwaggerVersion,
	}); err != nil {
		logrus.Warnf("failed to ListRecords contracts, err: %v", err)
		return
	}

	for _, contract := range contracts {
		var client apistructs.ClientModel
		if err := svc.FirstRecord(&client, map[string]interface{}{
			"org_id": req.OrgID,
			"id":     contract.ClientID,
		}); err != nil {
			logrus.Warnf("failed to FirstRecord, err: %v", err)
			continue
		}
		go svc.updateVersionMsgToUser(req.OrgID, contract.CreatorID, access.AssetName, version, &client)
	}
}
