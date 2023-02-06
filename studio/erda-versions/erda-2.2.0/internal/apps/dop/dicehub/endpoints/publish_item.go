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
	"errors"
	"net/http"
	"net/url"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// QueryPublishItem 查询发布内容
func (e *Endpoints) QueryPublishItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	queryReq := apistructs.QueryPublishItemRequest{
		PageSize:    getInt(r.URL, "pageSize", 10),
		PageNo:      getInt(r.URL, "pageNo", 1),
		Name:        r.URL.Query().Get("name"),
		Q:           r.URL.Query().Get("q"),
		Public:      r.URL.Query().Get("public"),
		Type:        r.URL.Query().Get("type"),
		Ids:         r.URL.Query().Get("ids"),
		PublisherId: getInt(r.URL, "publisherId", -1),
	}
	orgIDStr := r.Header.Get("Org-ID")
	if orgIDStr != "" {
		orgID, err := strconv.ParseInt(orgIDStr, 10, 64)
		if err != nil {
			queryReq.OrgID = orgID
		}
	}
	result, err := e.publishItem.QueryPublishItems(&queryReq)
	if err != nil {
		return apierrors.ErrQueryPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

// QueryMyPublishItem 查询我的发布
func (e *Endpoints) QueryMyPublishItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req = new(apistructs.QueryPublishItemRequest)
	req.FromValues(r.URL.Query())
	result, err := e.publishItem.QueryPublishItems(req)
	if err != nil {
		return apierrors.ErrQueryPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

// CreatePublishItem 创建发布内容
func (e *Endpoints) CreatePublishItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var request apistructs.CreatePublishItemRequest

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrCreatePublishItem.InvalidParameter(err).ToResp(), nil
	}
	orgID, err := getPermissionHeader(r)
	if err != nil {
		return apierrors.ErrCreatePublishItem.NotLogin().ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreatePublishItem.InvalidParameter(err).ToResp(), nil
	}
	request.OrgID = orgID
	request.Creator = userID.String()
	result, err := e.publishItem.CreatePublishItem(&request)

	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(result)
}

// GetPublishItem 获取发布内容详情
func (e *Endpoints) GetPublishItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	publishItemId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrGetPublishItem.InvalidParameter(err).ToResp(), nil
	}
	publishItem, err := e.publishItem.GetPublishItem(publishItemId)

	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(publishItem)
}

// GetPublishItemDistribution 获取发布内容分发信息
func (e *Endpoints) GetPublishItemDistribution(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	publishItemId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrGetPublishItem.InvalidParameter(err).ToResp(), nil
	}

	mobileType, err := getMobileType(r.URL.Query().Get("mobileType"))
	if err != nil {
		return apierrors.ErrGetPublishItem.InvalidParameter(err).ToResp(), nil
	}
	packageName := r.URL.Query().Get("packageName")

	publishItemDistribution, err := e.publishItem.GetPublishItemDistribution(publishItemId, mobileType, packageName,
		ctx.Value(httpserver.ResponseWriter).(http.ResponseWriter), r)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	return httpserver.OkResp(publishItemDistribution)
}

// UpdatePublishItem 更新PublishItem
func (e *Endpoints) UpdatePublishItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var request apistructs.UpdatePublishItemRequest

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrUpdatePublishItem.InvalidParameter(err).ToResp(), nil
	}

	itemID, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrUpdatePublishItem.InvalidParameter(err).ToResp(), nil
	}
	request.ID = itemID
	err = e.publishItem.UpdatePublishItem(&request)

	if err != nil {
		return apierrors.ErrUpdatePublishItem.InternalError(err).ToResp(), nil
	}

	item, err := e.publishItem.GetPublishItem(itemID)
	if err != nil {
		return apierrors.ErrDeletePublishItem.InvalidParameter(err).ToResp(), nil
	}

	return httpserver.OkResp(&item)
}

// DeletePublishItem 删除发布内容
func (e *Endpoints) DeletePublishItem(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	itemID, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrDeletePublishItem.InvalidParameter(err).ToResp(), nil
	}

	item, err := e.publishItem.GetPublishItem(itemID)
	if err != nil {
		return apierrors.ErrDeletePublishItem.InvalidParameter(err).ToResp(), nil
	}

	err = e.publishItem.DeletePublishItem(itemID)

	if err != nil {
		return apierrors.ErrDeletePublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(&item)
}

// CreatePublishItemVersion 创建发布版本
func (e *Endpoints) CreatePublishItemVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var request apistructs.CreatePublishItemVersionRequest

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrCreatePublishItemVersion.InvalidParameter(err).ToResp(), nil
	}
	orgID, err := getPermissionHeader(r)
	if err != nil {
		return apierrors.ErrCreatePublishItemVersion.NotLogin().ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreatePublishItemVersion.InvalidParameter(err).ToResp(), nil
	}
	itemID, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrCreatePublishItemVersion.InvalidParameter(err).ToResp(), nil
	}
	request.OrgID = orgID
	request.PublishItemID = itemID
	request.Creator = userID.String()

	result, err := e.publishItem.PublishItemVersion(&request)

	if err != nil {
		return apierrors.ErrCreatePublishItemVersion.InternalError(err).ToResp(), nil
	}

	item, err := e.publishItem.GetPublishItem(itemID)
	if err != nil {
		return apierrors.ErrDeletePublishItem.InvalidParameter(err).ToResp(), nil
	}

	return httpserver.OkResp(apistructs.CreatePublishItemVersionResponse{
		Data:        *result,
		PublishItem: *item,
	})
}

// CreateOffLineVersion 创建离线包版本
func (e *Endpoints) CreateOffLineVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// 获取上传文件
	_, fileHeader, err := r.FormFile("file")
	if err != nil {
		return apierrors.ErrCreateOffLinePublishItemVersion.InvalidParameter(err).ToResp(), nil
	}
	// 校验用户登录
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrCreateOffLinePublishItemVersion.NotLogin().ToResp(), nil
	}
	// 获取publishitemID
	itemID, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrCreateOffLinePublishItemVersion.InvalidParameter(err).ToResp(), nil
	}
	// 获取企业ID
	orgID, err := getPermissionHeader(r)
	if err != nil {
		return apierrors.ErrCreatePublishItemVersion.NotLogin().ToResp(), nil
	}

	req := apistructs.CreateOffLinePublishItemVersionRequest{
		Desc: r.PostFormValue("desc"),
		// FormFile:      formFile,
		FileHeader:    fileHeader,
		PublishItemID: itemID,
		IdentityInfo:  identityInfo,
		OrgID:         orgID,
	}

	mobiletype, err := e.publishItem.CreateOffLineVersion(req)
	if err != nil {
		return apierrors.ErrCreateOffLinePublishItemVersion.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(mobiletype)
}

// QueryPublishItemVersion 查询发布版本
func (e *Endpoints) QueryPublishItemVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getPermissionHeader(r)
	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.NotLogin().ToResp(), nil
	}
	itemId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.InvalidParameter(err).ToResp(), nil
	}
	mobileType, err := getMobileType(r.URL.Query().Get("mobileType"))
	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.InvalidParameter(err).ToResp(), nil
	}
	queryReq := apistructs.QueryPublishItemVersionRequest{
		PageSize:    getInt(r.URL, "pageSize", 10),
		PageNo:      getInt(r.URL, "pageNo", 1),
		Public:      r.URL.Query().Get("public"),
		MobileType:  mobileType,
		OrgID:       orgID,
		ItemID:      itemId,
		PackageName: r.URL.Query().Get("packageName"),
	}

	result, err := e.publishItem.QueryPublishItemVersions(&queryReq)

	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

// SetPublishItemVersionStatus 设置版本状态
func (e *Endpoints) SetPublishItemVersionStatus(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	itemId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrSetPublishItemVersionStatus.InvalidParameter(err).ToResp(), nil
	}
	itemVersionID, err := getPublishItemVersionId(vars)
	if err != nil {
		return apierrors.ErrSetPublishItemVersionStatus.InvalidParameter(err).ToResp(), nil
	}
	action := vars["action"]
	if action == "public" {
		err = e.publishItem.SetPublishItemVersionPublic(itemVersionID, itemId)
	} else if action == "unpublic" {
		err = e.publishItem.SetPublishItemVersionUnPublic(itemVersionID, itemId)
	} else if action == "default" {
		err = e.publishItem.SetPublishItemVersionDefault(itemVersionID, itemId)
	} else {
		return apierrors.ErrSetPublishItemVersionStatus.InvalidParameter("invalid action").ToResp(), nil
	}

	if err != nil {
		return apierrors.ErrSetPublishItemVersionStatus.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("")
}

// UpdatePublishItemVersionState 更新移动应用发布状态
func (e *Endpoints) UpdatePublishItemVersionState(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.UpdatePublishItemVersionStatesRequset
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreatePublishItemVersion.InvalidParameter(err).ToResp(), nil
	}

	action := vars["action"]
	if action == "publish" {
		req.Public = true
	} else if action == "unpublish" {
		req.Public = false
	} else {
		return apierrors.ErrSetPublishItemVersionStatus.InvalidParameter("invalid action").ToResp(), nil
	}

	if err := e.publishItem.PublicPublishItemVersion(req, e.bdl.GetLocaleByRequest(r)); err != nil {
		return apierrors.ErrSetPublishItemVersionStatus.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("")
}

// GetPublicVersion 获取移动应用线上的版本
func (e *Endpoints) GetPublicVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	itemID, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.InvalidParameter(err).ToResp(), nil
	}

	mobileType, packageName := r.URL.Query().Get("mobileType"), r.URL.Query().Get("packageName")

	results, err := e.publishItem.GetPublicPublishItemVersion(itemID, mobileType, packageName)
	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(results)
}

// GetH5PackageName 获取H5的包名
func (e *Endpoints) GetH5PackageName(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	itemID, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.InvalidParameter(err).ToResp(), nil
	}

	results, err := e.publishItem.GetH5PackageName(itemID)
	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(results)
}

// CheckLaststVersion 获取移动应用最新的版本信息
func (e *Endpoints) CheckLaststVersion(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.GetPublishItemLatestVersionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrQueryPublishItemVersion.InvalidParameter(err).ToResp(), nil
	}

	if req.AK == "" || req.AI == "" {
		return apierrors.ErrQueryPublishItemVersion.MissingParameter("ak or ai").ToResp(), nil
	}

	results, err := e.publishItem.GetPublicPublishItemLaststVersion(ctx, r, req)
	if err != nil {
		return apierrors.ErrQueryPublishItemVersion.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(results)
}

// GetPublishItemBlacklist 获取PublishItem黑名单
func (e *Endpoints) GetPublishItemBlacklist(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	publishItemId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrGetBlacklist.InvalidParameter(err).ToResp(), nil
	}
	queryReq := apistructs.PublishItemUserlistRequest{
		PageSize:      uint64(getInt(r.URL, "pageSize", 10)),
		PageNo:        uint64(getInt(r.URL, "pageNo", 1)),
		PublishItemID: uint64(publishItemId),
	}
	artifact, err := e.publishItem.GetBlacklists(&queryReq)
	if err != nil {
		return apierrors.ErrGetBlacklist.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// GetPublishItemEraselist 获取publishItem擦除数据名单
func (e *Endpoints) GetPublishItemEraselist(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	artifactId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrGetPublishItem.InvalidParameter(err).ToResp(), nil
	}
	queryReq := apistructs.PublishItemUserlistRequest{
		PageSize:      uint64(getInt(r.URL, "pageSize", 10)),
		PageNo:        uint64(getInt(r.URL, "pageNo", 1)),
		PublishItemID: uint64(artifactId),
	}
	artifact, err := e.publishItem.GetEraselists(&queryReq)
	if err != nil {
		return apierrors.ErrGetPublishItem.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(artifact)
}

// AddBlacklist 设置安全参数
func (e *Endpoints) AddBlacklist(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreatePublishItem.InvalidParameter(err).ToResp(), nil
	}
	publishItemId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrCreateBlacklist.InvalidParameter(err).ToResp(), nil
	}
	var request apistructs.PublishItemUserlistRequest

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrCreateBlacklist.InvalidParameter(err).ToResp(), nil
	}
	request.PublishItemID = uint64(publishItemId)
	request.Operator = userID.String()
	err, publishItem := e.publishItem.AddBlacklist(&request)
	if err != nil {
		return apierrors.ErrCreateBlacklist.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(publishItem.ToApiData())
}

// RemoveBlacklist 删除黑名单
func (e *Endpoints) RemoveBlacklist(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	artifactId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrDeleteBlacklist.InvalidParameter(err).ToResp(), nil
	}
	blacklistIdStr := vars["blacklistId"]
	blacklistId, err := strconv.ParseUint(blacklistIdStr, 10, 64)
	if err != nil {
		return apierrors.ErrDeleteBlacklist.InvalidParameter(err).ToResp(), nil
	}
	err, blackList, publishItem := e.publishItem.RemoveBlacklist(blacklistId, uint64(artifactId))
	if err != nil {
		return apierrors.ErrDeleteBlacklist.InternalError(err).ToResp(), nil
	}

	if blackList != nil {
		return httpserver.OkResp(apistructs.PublishItemUserListResponse{
			ID:              blackList.ID,
			UserID:          blackList.UserID,
			UserName:        blackList.UserName,
			DeviceNo:        blackList.DeviceNo,
			CreatedAt:       blackList.CreatedAt,
			PublishItemName: publishItem.Name,
		})
	} else {
		return httpserver.OkResp(apistructs.PublishItemUserListResponse{})
	}
}

// AddErase 设置数据擦除用户
func (e *Endpoints) AddErase(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateEraselist.InvalidParameter(err).ToResp(), nil
	}
	artifactId, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrCreateEraselist.InvalidParameter(err).ToResp(), nil
	}
	var request apistructs.PublishItemUserlistRequest

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrCreateEraselist.InvalidParameter(err).ToResp(), nil
	}
	request.PublishItemID = uint64(artifactId)
	request.Operator = userID.String()
	err, publishItem := e.publishItem.AddErase(&request)
	if err != nil {
		return apierrors.ErrCreateEraselist.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(apistructs.PublicItemAddEraseData{
		Data:     *publishItem.ToApiData(),
		DeviceNo: request.DeviceNo,
	})
}

// UpdateErase 数据擦除状态更新
func (e *Endpoints) UpdateErase(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var request apistructs.PublishItemEraseRequest

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		return apierrors.ErrUpdateErase.InvalidParameter(err).ToResp(), nil
	}
	if err := e.publishItem.UpdateErase(&request); err != nil {
		return apierrors.ErrUpdateErase.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("success")
}

// GetSecurityStatus 获取客户安全信息状态
func (e *Endpoints) GetSecurityStatus(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	ak := r.URL.Query().Get("ak")
	if ak == "" {
		return apierrors.ErrSecurity.MissingParameter("ak").ToResp(), nil
	}
	ai := r.URL.Query().Get("ai")
	if ai == "" {
		return apierrors.ErrSecurity.MissingParameter("ai").ToResp(), nil
	}
	userId := r.URL.Query().Get("userId")
	deviceNo := r.URL.Query().Get("deviceNo")
	if deviceNo == "" {
		return apierrors.ErrSecurity.MissingParameter("deviceNo").ToResp(), nil
	}
	err := errors.New("获取安全信息失败")
	lon := r.URL.Query().Get("lon")
	lonFloat := 0.0
	if lon != "" {
		lonFloat, err = strconv.ParseFloat(lon, 64)
		if err != nil {
			return apierrors.ErrSecurity.InternalError(err).ToResp(), nil
		}
	}

	lat := r.URL.Query().Get("lat")
	latFloat := 0.0
	if lat != "" {
		latFloat, err = strconv.ParseFloat(lat, 64)
		if err != nil {
			return apierrors.ErrSecurity.InternalError(err).ToResp(), nil
		}
	}

	request := apistructs.PublishItemSecurityStatusRequest{
		Ak:       ak,
		Ai:       ai,
		UserID:   userId,
		DeviceNo: deviceNo,
		Lon:      lonFloat,
		Lat:      latFloat,
	}

	resp, err := e.publishItem.GetSecurityStatus(request)
	if err != nil {
		return apierrors.ErrSecurity.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resp)
}

// ListMonitorKeys 获取 publishItem 的监控 ak ai
func (e *Endpoints) ListMonitorKeys(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	publishItemID, err := getPublishItemId(vars)
	if err != nil {
		return apierrors.ErrGetMonitorKeys.InvalidParameter(err).ToResp(), nil
	}

	mks, err := e.publishItem.GetMonitorkeys(&apistructs.QueryAppPublishItemRelationRequest{
		PublishItemID: publishItemID,
	})
	if err != nil {
		return apierrors.ErrGetMonitorKeys.InternalError(err).ToResp(), nil
	}

	mksMap := make(map[string]apistructs.MonitorKeys, 0)
	for _, relation := range mks {
		mksMap[relation.Env+"-"+relation.AI] = relation
	}

	return httpserver.OkResp(mksMap)
}

func getPublishItemId(vars map[string]string) (int64, error) {
	itemIDStr := vars["publishItemId"]
	itemID, err := strconv.ParseInt(itemIDStr, 10, 64)
	if err != nil {
		return 0, errors.New("publishItem id parse failed")
	}
	return itemID, nil
}

func getPublishItemVersionId(vars map[string]string) (int64, error) {
	itemVersionIDStr := vars["publishItemVersionId"]
	itemVersionID, err := strconv.ParseInt(itemVersionIDStr, 10, 64)
	if err != nil {
		return 0, errors.New("publishItemVersion id parse failed")
	}
	return itemVersionID, nil
}

func getMobileType(mobileType string) (apistructs.ResourceType, error) {
	if mobileType == "" {
		return "", nil
	}

	if mobileType != string(apistructs.ResourceTypeIOS) && mobileType != string(apistructs.ResourceTypeAndroid) &&
		mobileType != string(apistructs.ResourceTypeH5) && mobileType != string(apistructs.ResourceTypeAndroidAppBundle) {
		return "", errors.New("mobileType is invalied")
	}

	return apistructs.ResourceType(mobileType), nil
}

func getInt(url *url.URL, key string, defaultValue int64) int64 {
	valueStr := url.Query().Get(key)
	value, err := strconv.ParseInt(valueStr, 10, 32)
	if err != nil {
		logrus.Errorf("get int err: %+v", err)
		return defaultValue
	}
	return value
}

func getInt64(url *url.URL, key string, defaultValue int64) int64 {
	valueStr := url.Query().Get(key)
	value, err := strconv.ParseInt(valueStr, 10, 64)
	if err != nil {
		logrus.Errorf("get int err: %+v", err)
		return defaultValue
	}
	return value
}

func getMonitorKeys(url *url.URL) (*apistructs.MonitorKeys, error) {
	ak, ai := url.Query().Get("ak"), url.Query().Get("ai")
	if ak == "" || ai == "" {
		return nil, errors.New("nil ak or ai")
	}

	return &apistructs.MonitorKeys{
		AK: ak,
		AI: ai,
	}, nil
}
