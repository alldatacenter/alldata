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
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) CreateAddonDirectly(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrCreateAddon.MissingParameter("ORG-ID").ToResp(), nil
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err).ToResp(), nil
	}
	var addonCreateReq apistructs.AddonDirectCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&addonCreateReq); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("addon create info: %+v", addonCreateReq)

	addonCreateReq.Operator = userID.String()
	addonCreateReq.OrgID = orgID
	// // 校验用户是否为企业管理员或项目管理员
	// manager, err := e.addon.IsManager(userID.String(), apistructs.OrgScope, orgID)
	// if err != nil {
	// 	return apierrors.ErrCreateAddon.InternalError(err).ToResp(), nil
	// }
	// if !manager {
	// 	manager, err = e.addon.IsManager(userID.String(), apistructs.ProjectScope, addonCreateReq.ProjectID)
	// 	if err != nil {
	// 		return apierrors.ErrCreateAddon.InternalError(err).ToResp(), nil
	// 	}
	// 	if !manager {
	// 		return apierrors.ErrCreateAddon.AccessDenied().ToResp(), nil
	// 	}
	// }
	addonid, err := e.addon.AddonCreate(addonCreateReq)
	if err != nil {
		return apierrors.ErrCreateAddon.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(addonid)
}

func (e *Endpoints) CreateAddonTenant(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var addonCreateReq apistructs.AddonTenantCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&addonCreateReq); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err).ToResp(), nil
	}
	instanceid, err := e.addon.CreateAddonTenant(addonCreateReq.Name, addonCreateReq.AddonInstanceRoutingID, addonCreateReq.Configs)
	if err != nil {
		return apierrors.ErrCreateAddon.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(instanceid)
}

// CreateCustomAddon 创建自定义 addon
func (e *Endpoints) CreateCustomAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrCreateAddon.MissingParameter("ORG-ID").ToResp(), nil
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err).ToResp(), nil
	}

	// 校验 body 合法性
	var customAddonReq apistructs.CustomAddonCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&customAddonReq); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("custom addon create info: %+v", customAddonReq)
	// 参数合法性校验
	if err := e.checkCustomAddonCreateParam(&customAddonReq); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err).ToResp(), nil
	}
	customAddonReq.OperatorID = userID.String()
	// 校验用户是否有权限创建自定义addon(企业管理员或项目管理员)
	manager, err := e.addon.CheckCustomAddonPermission(userID.String(), orgID, customAddonReq.ProjectID)
	if err != nil {
		return apierrors.ErrCreateAddon.InternalError(err).ToResp(), nil
	}
	if !manager {
		return apierrors.ErrCreateAddon.AccessDenied().ToResp(), nil
	}

	resultMap, err := e.addon.CreateCustom(&customAddonReq)
	if err != nil {
		return apierrors.ErrCreateAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(resultMap)
}

// UpdateCustomAddon 更新自定义 addon
func (e *Endpoints) UpdateCustomAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrUpdateAddon.MissingParameter("ORG-ID").ToResp(), nil
	}
	orgID, err := strconv.ParseUint(orgIDStr, 10, 64)
	if err != nil {
		return apierrors.ErrUpdateAddon.InvalidParameter(err).ToResp(), nil
	}

	// 校验 body 合法性
	var customAddonReq map[string]interface{}
	if err := json.NewDecoder(r.Body).Decode(&customAddonReq); err != nil {
		return apierrors.ErrUpdateAddon.InvalidParameter(err).ToResp(), nil
	}
	if len(customAddonReq) == 0 {
		return apierrors.ErrUpdateAddon.InvalidParameter("body").ToResp(), nil
	}
	logrus.Infof("custom addon update info: %+v", customAddonReq)

	for key := range customAddonReq {
		if err := strutil.EnvKeyValidator(key); err != nil {
			return apierrors.ErrUpdateAddon.InternalError(err).ToResp(), nil
		}
	}

	// 更新 config 信息至 DB
	if err := e.addon.UpdateCustom(userID.String(), vars["addonID"], orgID, &customAddonReq); err != nil {
		return apierrors.ErrUpdateAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp("update succ")
}

// GetAddon 获取 addon 详情
func (e *Endpoints) GetAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	clientid := r.Header.Get("Client-ID")
	userID, err := user.GetUserID(r)
	if clientid == "" && err != nil {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}
	orgID := r.Header.Get(httputil.OrgHeader)
	if clientid == "" && orgID == "" {
		return apierrors.ErrFetchAddon.MissingParameter("ORG-ID").ToResp(), nil
	}

	addonInfo, err := e.addon.Get(userID.String(), orgID, vars["addonID"], clientid != "")
	if err != nil {
		if strings.Contains(err.Error(), "not found") {
			return apierrors.ErrFetchAddon.NotFound().ToResp(), nil
		}
		return apierrors.ErrFetchAddon.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(addonInfo)
}

// ScaleAddon 设置 addon 服务停止（副本数 N--->0）/启动（副本数 0--->N）
func (e *Endpoints) ScaleAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	action := r.URL.Query().Get(apistructs.ScaleAction)
	if action != apistructs.ScaleActionUp && action != apistructs.ScaleActionDown {
		return apierrors.ErrScaleAddon.InvalidParameter("no parameter " + apistructs.ScaleAction + " or invalid parameter value for parameter " + apistructs.ScaleAction).ToResp(), nil
	}

	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrScaleAddon.NotLogin().ToResp(), nil
	}

	orgID := r.Header.Get(httputil.OrgHeader)
	if orgID == "" {
		return apierrors.ErrScaleAddon.MissingParameter("ORG-ID").ToResp(), nil
	}

	var addonScaleRecords apistructs.AddonScaleRecords
	if err := json.NewDecoder(r.Body).Decode(&addonScaleRecords); err != nil {
		return utils.ErrRespIllegalParam(err, "failed to batch update Overlay, failed to parse req")
	}

	if len(addonScaleRecords.Addons) == 0 && len(addonScaleRecords.AddonRoutingIDs) == 0 {
		return utils.ErrRespIllegalParam(err, "failed to batch update Overlay, no addonScaleRecords or ids provided in request body")
	}

	if len(addonScaleRecords.Addons) != 0 && len(addonScaleRecords.AddonRoutingIDs) != 0 {
		return utils.ErrRespIllegalParam(err, "failed to batch update Overlay, addonScaleRecords and ids must only one with non-empty values in request body")
	}

	var result apistructs.AddonScaleResults
	if len(addonScaleRecords.AddonRoutingIDs) > 0 {
		// scale addon from N--->0 or 0--->N
		result.Total = len(addonScaleRecords.AddonRoutingIDs)
		for _, addonRoutingId := range addonScaleRecords.AddonRoutingIDs {
			if err := e.addon.Scale(userID.String(), addonRoutingId, action); err != nil {
				logrus.Errorf("Do %s for addon %s failed. error: %v", action, addonRoutingId, err)
				msg := fmt.Sprintf("Do %s for addon %s failed. error: %v", action, addonRoutingId, err)
				if result.FailedInfo == nil {
					result.FailedInfo = make(map[string]string)
				}
				result.Faild++
				result.FailedInfo[addonRoutingId] = msg
			} else {
				logrus.Infof("Do %s for addon %s successfully.", action, vars["addonID"])
				result.Successed++
			}
		}
	}

	if len(addonScaleRecords.Addons) > 0 {
		// ToDo: scale addon for replicas or/and resources
	}

	if result.Faild > 0 {
		return httpserver.NotOkResp(result, http.StatusInternalServerError)
	}
	logrus.Infof("scale all addons successfully")

	return httpserver.OkResp(result)
}

// DeleteAddon 删除 addon
func (e *Endpoints) DeleteAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}

	orgID := r.Header.Get(httputil.OrgHeader)
	if orgID == "" {
		return apierrors.ErrFetchAddon.MissingParameter("ORG-ID").ToResp(), nil
	}
	addoninfo, err := e.addon.Get(userID.String(), orgID, vars["addonID"], false)
	if err != nil {
		return apierrors.ErrDeleteAddon.InternalError(err).ToResp(), nil
	}
	if err := e.addon.Delete(userID.String(), vars["addonID"]); err != nil {
		return apierrors.ErrDeleteAddon.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(addoninfo)
}

// ListAddon addon 列表
// type=addon&value=<addonName> 按 addon 名称获取 addon 列表
// type=category
func (e *Endpoints) ListAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrListAddon.NotLogin().ToResp(), nil
	}
	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrListAddon.NotLogin().ToResp(), nil
	}
	orgID, _ := strconv.ParseUint(orgIDStr, 10, 64)

	var addonList *[]apistructs.AddonFetchResponseData
	// 参数校验
	if r.URL.Query().Get("type") == "" {
		return apierrors.ErrListAddon.MissingParameter("type").ToResp(), nil
	}
	if r.URL.Query().Get("value") == "" {
		return apierrors.ErrListAddon.MissingParameter("value").ToResp(), nil
	}
	category := r.URL.Query().Get("category")

	switch r.URL.Query().Get("type") {
	case "addon":
		addonList, err = e.addon.ListByAddonName(orgID, r.URL.Query().Get("value"))
	case "category":
		addonList, err = e.addon.ListByCategory(orgID, r.URL.Query().Get("value"))
	case "workbench":
		addonList, err = e.addon.ListByWorkbench(orgID, userID.String(), category)
	case "org":
		addonList, err = e.addon.ListByOrg(orgID)
	case "project":
		projectID, err := strconv.ParseUint(r.URL.Query().Get("value"), 10, 64)
		if err != nil {
			return apierrors.ErrListAddon.InvalidParameter("value should be integer").ToResp(), nil
		}
		addonList, err = e.addon.ListByProject(orgID, projectID, category)
	case "runtime":
		runtimeID, err := strconv.ParseUint(r.URL.Query().Get("value"), 10, 64)
		projectID := r.URL.Query().Get("projectId")
		workspace := r.URL.Query().Get("workspace")
		if err != nil {
			return apierrors.ErrListAddon.InvalidParameter("value should be integer").ToResp(), nil
		}
		addonList, err = e.addon.ListByRuntime(runtimeID, projectID, workspace)
	case "project_addon_workbench":
		projectID, err := strconv.ParseUint(r.URL.Query().Get("projectId"), 10, 64)
		if err != nil {
			return apierrors.ErrListAddon.InvalidParameter("value should be integer").ToResp(), nil
		}
		addonList, err = e.addon.ListByProject(orgID, projectID, category)
		if addonList == nil {
			return httpserver.OkResp(addonList)
		}
		var filterAddonList []apistructs.AddonFetchResponseData

		addonName := r.URL.Query().Get("addon")
		workspace := r.URL.Query().Get("workspace")
		for _, v := range *addonList {
			if len(addonName) > 0 && v.AddonName != addonName {
				continue
			}
			if len(workspace) > 0 && v.Workspace != workspace {
				continue
			}

			filterAddonList = append(filterAddonList, v)
		}
		return httpserver.OkResp(filterAddonList)
	case "database_addon":
		projectID, err := strconv.ParseUint(r.URL.Query().Get("projectId"), 10, 64)
		if err != nil {
			return apierrors.ErrListAddon.InvalidParameter("value should be integer").ToResp(), nil
		}
		addonList, err = e.addon.ListByProject(orgID, projectID, category)
		if addonList == nil {
			return httpserver.OkResp(addonList)
		}

		var filterAddonList []apistructs.AddonFetchResponseData
		displayNames := r.URL.Query()["displayName"]
		workspace := r.URL.Query().Get("workspace")
		for _, v := range *addonList {
			if displayNames == nil || len(displayNames) <= 0 {
				continue
			}

			var find = false
			for _, displayName := range displayNames {
				if v.AddonName == strings.ToLower(displayName) {
					find = true
					break
				}
			}

			if !find {
				continue
			}

			if len(workspace) > 0 && v.Workspace != workspace {
				continue
			}

			filterAddonList = append(filterAddonList, v)
		}
		return httpserver.OkResp(filterAddonList)
	default:
		return apierrors.ErrListAddon.InvalidParameter("type").ToResp(), nil
	}

	if err != nil {
		return apierrors.ErrListAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(addonList)
}

// ListAvailableAddon dice.yml 编辑时列出可选 addon 列表
func (e *Endpoints) ListAvailableAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID := r.Header.Get(httputil.OrgHeader)
	if orgID == "" {
		return apierrors.ErrListAddon.MissingParameter("ORG-ID").ToResp(), nil
	}
	// TODO 列出用户可选 addon
	projectID := r.URL.Query().Get("projectId")
	if projectID == "" {
		return apierrors.ErrListAddon.MissingParameter("projectId").ToResp(), nil
	}
	workspace := r.URL.Query().Get("workspace")
	if workspace == "" {
		return apierrors.ErrListAddon.MissingParameter("workspace").ToResp(), nil
	}

	addons, err := e.addon.ListAvailable(orgID, projectID, workspace)
	if err != nil {
		return apierrors.ErrListAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(addons)
}

// ListByAddonName 通过addonName来控制
func (e *Endpoints) ListByAddonName(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	projectID := r.URL.Query().Get("projectId")
	if projectID == "" {
		return apierrors.ErrListAddon.MissingParameter("projectId").ToResp(), nil
	}
	workspace := r.URL.Query().Get("workspace")
	if workspace == "" {
		return apierrors.ErrListAddon.MissingParameter("workspace").ToResp(), nil
	}

	addons, err := e.addon.ListByAddonNameAndProject(projectID, workspace, vars["addonName"])
	if err != nil {
		return apierrors.ErrListAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(addons)
}

// ListExtensionAddon dice.yml 编辑时列出可选 extension 列表
func (e *Endpoints) ListExtensionAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	extensionType := r.URL.Query().Get("type")
	if extensionType == "" {
		return apierrors.ErrListAddon.MissingParameter("type").ToResp(), nil
	}

	addons, err := e.addon.ListExtension(extensionType)
	if err != nil {
		return apierrors.ErrListAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(addons)
}

// ListCustomAddon 添加第三方addon，先获取第三方addon列表
func (e *Endpoints) ListCustomAddon(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	addons, err := e.addon.ListCustomAddon()
	if err != nil {
		return apierrors.ErrListAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(addons)
}

// ListAddonMenu addon menu 列表
func (e *Endpoints) ListAddonMenu(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	addonMenu := make(map[string]string)
	addonMenu["database"] = apistructs.AddonCategoryDataBase
	addonMenu["message"] = apistructs.AddonCategoryMessage
	addonMenu["search"] = apistructs.AddonCategorySearch
	addonMenu["distributed_cooperation"] = apistructs.AddonCategoryDistributedCooperation
	addonMenu["custom"] = apistructs.AddonCategoryCustom
	addonMenu["microservice"] = apistructs.AddonCategoryMicroService
	addonMenu["platform_dice"] = apistructs.AddonCategoryPlatformDice
	addonMenu["platform_cluster"] = apistructs.AddonCategoryPlatformCluster
	addonMenu["platform_project"] = apistructs.AddonCategoryPlatformProject

	return httpserver.OkResp(addonMenu)
}

// GetAddonReferences 获取 addon 详情的引用列表
func (e *Endpoints) GetAddonReferences(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}

	orgIDStr := r.Header.Get(httputil.OrgHeader)
	if orgIDStr == "" {
		return apierrors.ErrFetchAddon.NotLogin().ToResp(), nil
	}
	orgID, _ := strconv.ParseUint(orgIDStr, 10, 64)

	// 填充 addon 引用列表
	references, err := e.addon.ListReferencesByRoutingInstanceID(orgID, userID.String(), vars["addonID"])
	if err != nil {
		return apierrors.ErrFetchAddon.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(references)
}

// SyncAddons 同步市场 addons 信息
func (e *Endpoints) SyncAddons() (bool, error) {
	req := apistructs.ExtensionQueryRequest{
		All:  true,
		Type: "addon",
	}
	extensions, err := e.bdl.QueryExtensions(req)
	if err != nil {
		return false, err
	}
	for _, item := range extensions {
		addon.AddonInfos.Store(item.Name, item)
	}

	return false, nil
}

// RemoveAddons 同步市场 addons 信息
func (e *Endpoints) RemoveAddons() (bool, error) {
	insList, err := e.db.ListNoAttachAddon()
	if err != nil {
		logrus.Errorf("查询所有引用为 0 addon报错, error: %+v", err)
		return false, err
	}
	if len(*insList) == 0 {
		logrus.Info("no addon resource should be remove")
		return false, nil
	}
	for _, v := range *insList {
		insItem, err := e.db.GetAddonInstance(v.ID)
		if err != nil {
			logrus.Errorf("后台线程查询addon信息报错, error: %+v", err)
			continue
		}
		if insItem == nil {
			continue
		}
		if insItem.Category == apistructs.AddonCustomCategory {
			continue
		}
		// 删除微服务、通用能力addon
		if insItem.PlatformServiceType != apistructs.PlatformServiceTypeBasic {
			addonProviderRequest := apistructs.AddonProviderRequest{
				UUID:        insItem.ID,
				Plan:        insItem.Plan,
				ClusterName: insItem.Cluster,
			}
			addonSpec, _, err := e.addon.GetAddonExtention(&apistructs.AddonHandlerCreateItem{
				AddonName: insItem.AddonName,
				Plan:      insItem.Plan,
				Options: map[string]string{
					"version": insItem.Version,
				},
			})
			if err != nil {
				continue
			}
			if _, err := e.addon.DeleteAddonProvider(&addonProviderRequest, insItem.ID, insItem.AddonName, addonSpec.Domain); err != nil {
				logrus.Errorf("delete provider addon failed, error is %+v", err)
			}
			if err := e.addon.UpdateAddonStatus(insItem, apistructs.AddonDetached); err != nil {
				logrus.Errorf("syn remove provider addon error, %+v", err)
			}
		} else {
			// 基础addon删除，mysql、es、redis涉及数据，暂时不清除
			if insItem.AddonName == apistructs.AddonMySQL || insItem.AddonName == apistructs.AddonES || insItem.AddonName == apistructs.AddonRedis {
				continue
			}
			// schedule删除
			if err := e.scheduler.Httpendpoints.ServiceGroupImpl.Delete(insItem.Namespace, insItem.ScheduleName, "false"); err != nil {
				logrus.Errorf("failed to delete addon: %s/%s", insItem.Namespace, insItem.ScheduleName)
				continue
			}
			if err := e.addon.UpdateAddonStatus(insItem, apistructs.AddonDetached); err != nil {
				logrus.Errorf("syn remove basic addon error, %+v", err)
			}
		}
	}
	return false, nil
}

// AddonCreateCallback addon provider创建状态回调接口
func (e *Endpoints) AddonCreateCallback(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var req apistructs.AddonCreateCallBackResponse
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err).ToResp(), nil
	}

	logrus.Infof("received addon create callback: %+v", req)

	if err := e.addon.AddonProvisionCallback(vars["addonID"], &req); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err.Error()).ToResp(), nil
	}

	return httpserver.OkResp("ok")
}

// AddonDeleteCallback addon provider删除状态回调接口
func (e *Endpoints) AddonDeleteCallback(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	if err := e.addon.AddonDeprovisionCallback(vars["addonID"]); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err.Error()).ToResp(), nil
	}

	return httpserver.OkResp("ok")
}

// AddonConfigCallback addon provider配置回调接口
func (e *Endpoints) AddonConfigCallback(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	var req apistructs.AddonConfigCallBackResponse
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err).ToResp(), nil
	}

	if err := e.addon.AddonConfigCallback(vars["addonID"], &req); err != nil {
		return apierrors.ErrCreateAddon.InvalidParameter(err.Error()).ToResp(), nil
	}

	return httpserver.OkResp("ok")
}

func (e *Endpoints) checkCustomAddonCreateParam(req *apistructs.CustomAddonCreateRequest) error {
	if req.Name == "" {
		return errors.Errorf("missing param name")
	}
	if req.AddonName == "" {
		return errors.Errorf("missing param addon name")
	}
	if _, ok := addon.AddonInfos.Load(req.AddonName); !ok {
		return errors.Errorf("not found addon: %s", req.AddonName)
	}
	switch strings.ToUpper(req.Workspace) {
	case string(apistructs.DevWorkspace), string(apistructs.TestWorkspace), string(apistructs.StagingWorkspace),
		string(apistructs.ProdWorkspace):
	default:
		return errors.Errorf("invalid workspace: %s", req.Workspace)
	}

	if req.CustomAddonType == apistructs.CUSTOM_TYPE_CUSTOM && len(req.Configs) == 0 {
		return errors.Errorf("missing param configs")
	}

	for key := range req.Configs {
		if err := strutil.EnvKeyValidator(key); err != nil {
			return err
		}
	}

	return nil
}

// AddonMetrics addon链路转发监控信息
func (e *Endpoints) AddonMetrics(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	//鉴权
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateAddon.NotLogin().ToResp(), nil
	}
	addonInsId := r.URL.Query().Get("filter_addon_id")
	if addonInsId == "" {
		return nil, apierrors.ErrListAddonMetris.AccessDenied()
	}
	ins, err := e.db.GetAddonInstance(addonInsId)
	if err != nil {
		return nil, err
	}
	if ins == nil {
		return nil, nil
	}
	// 先检查是否企业管理元
	permissionOK := false
	if ins.OrgID != "" {
		orgIDInt, err := strconv.Atoi(ins.OrgID)
		permissionResult, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(orgIDInt),
			Resource: "addon",
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, err
		}
		if permissionResult.Access {
			permissionOK = true
		}
	}
	// 再检查是否项目管理元
	if !permissionOK && ins.ProjectID != "" {
		projectIDInt, err := strconv.Atoi(ins.ProjectID)
		permissionResult, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.ProjectScope,
			ScopeID:  uint64(projectIDInt),
			Resource: "addon",
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, err
		}
		if !permissionResult.Access {
			return nil, apierrors.ErrListAddonMetris.AccessDenied()
		}
	}
	metricsResp, err := e.bdl.AddonMetrics(r.URL.Path, r.URL.Query())
	if err != nil {
		return nil, apierrors.ErrListAddonMetris.InternalError(err)
	}
	return httpserver.OkResp(metricsResp["data"])
}

// AddonLogs addon日志信息
func (e *Endpoints) AddonLogs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	//鉴权
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateAddon.NotLogin().ToResp(), nil
	}
	addonInsId := vars["instanceId"]
	if addonInsId == "" {
		return nil, apierrors.ErrListAddonMetris.AccessDenied()
	}
	ins, err := e.db.GetAddonInstance(addonInsId)
	if err != nil {
		return nil, err
	}
	if ins == nil {
		return nil, nil
	}
	// 先检查是否企业管理元
	if ins.OrgID != "" {
		orgIDInt, err := strconv.Atoi(ins.OrgID)
		permissionResult, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.OrgScope,
			ScopeID:  uint64(orgIDInt),
			Resource: "middleware",
			Action:   apistructs.GetAction,
		})
		if err != nil {
			return nil, err
		}
		if !permissionResult.Access {
			return nil, apierrors.ErrListAddonMetris.AccessDenied()
		}
	}

	containerID := r.URL.Query().Get("id")
	if containerID == "" {
		return apierrors.ErrGetRuntime.MissingParameter("id").ToResp(), nil
	}

	var logReq apistructs.DashboardSpotLogRequest
	if err := queryStringDecoder.Decode(&logReq, r.URL.Query()); err != nil {
		return nil, err
	}
	logReq.ID = containerID
	logReq.Source = apistructs.DashboardSpotLogSourceContainer

	logResult, err := e.bdl.GetLog(r.Header.Get("org"), logReq)
	if err != nil {
		return nil, err
	}

	return httpserver.OkResp(logResult)
}

// SyncProjects 同步project信息
func (e *Endpoints) SyncProjects() (bool, error) {
	logrus.Infof("start project sync")
	projects, err := e.db.GetDistinctProjectInfo()
	if err != nil {
		return false, err
	}
	if projects == nil {
		return false, nil
	}
	for _, projectID := range *projects {
		if projectID == "" {
			continue
		}
		id, err := strconv.ParseUint(projectID, 10, 64)
		if err != nil {
			return false, errors.Errorf("failed to convert project id: %s, %v", projectID, err)
		}
		// 若找不到项目信息，根据 projectID 实时获取并缓存
		projectResp, err := e.bdl.GetProject(id)
		if err != nil {
			return false, errors.Errorf("failed to get project, %v", err)
		}
		if projectResp == nil {
			continue
		}
		addon.ProjectInfos.Store(projectID, *projectResp)
	}

	addon.ProjectInfos.Range(func(key, value interface{}) bool {
		logrus.Infof("project sync body info key: %s value: %+v", key, value)
		return true
	})

	return false, nil
}

// SyncDeployAddon 检测是否有未正常结束的addon，重新同步
func (e *Endpoints) SyncDeployAddon() (bool, error) {
	addons, err := e.db.ListAttachingAddonInstance()
	if err != nil {
		return false, err
	}
	if addons == nil || len(*addons) == 0 {
		return false, nil
	}
	now := time.Now()
	for _, ins := range *addons {
		subM := now.Sub(ins.CreatedAt)
		logrus.Infof("start addon reDeploy, addon id is %v", ins.ID)
		// 跟现在的时间比较，如果是大于15分钟的，直接失败
		if subM.Minutes() > 16 {
			// schedule删除
			if err := e.addon.FailAndDelete(&ins); err != nil {
				logrus.Errorf("syn deploy addon status error, (%v)", err)
			}
		} else {
			routings, err := e.db.GetByRealInstance(ins.ID)
			if err != nil {
				continue
			}
			if routings == nil || len(*routings) == 0 {
				continue
			}
			addonSpec, addonDice, err := e.addon.GetAddonExtention(&apistructs.AddonHandlerCreateItem{
				AddonName: ins.AddonName,
				Plan:      ins.Plan,
				Options: map[string]string{
					"version": ins.Version,
				},
			})
			if err != nil {
				continue
			}
			routingItem := (*routings)[0]
			if err := e.addon.GetAddonResourceStatus(&ins, &routingItem, addonDice, addonSpec); err != nil {
				continue
			}
			for _, item := range *routings {
				item.Status = string(apistructs.AddonAttached)
				e.db.UpdateInstanceRouting(&item)
			}
		}
	}
	return false, nil
}

func (e *Endpoints) SyncAddonReferenceNum() (bool, error) {
	logrus.Infof("start SyncAddonReferenceNum")
	defer logrus.Infof("end SyncAddonReferenceNum")
	addons, err := e.db.ListAttachedRoutingInstance()
	if err != nil {
		return false, err
	}
	if len(addons) == 0 {
		return false, nil
	}
	for _, routing := range addons {
		orgIDInt, err := strconv.ParseUint(routing.OrgID, 10, 64)
		if err != nil {
			continue
		}
		refinfo, err := e.addon.ListReferencesByRoutingInstanceID(orgIDInt, "1", routing.ID, true)
		if err != nil {
			logrus.Errorf("sync addon reference: ListReferencesByRoutingInstanceID: %v, %+v", err, routing)
			continue
		}
		if refinfo != nil {
			routing.Reference = len(*refinfo)
			if err := e.db.UpdateInstanceRouting(&routing); err != nil {
				logrus.Errorf("sync addon reference: UpdateInstanceRouting: %v", err)
				return false, err
			}
		}
	}

	tenants, err := e.db.ListAddonInstanceTenant()
	for _, tenant := range tenants {
		orgIDInt, err := strconv.ParseUint(tenant.OrgID, 10, 64)
		if err != nil {
			continue
		}
		refinfo, err := e.addon.ListReferencesByRoutingInstanceID(orgIDInt, "1", tenant.ID, true)
		if err != nil {
			logrus.Errorf("sync addon tenant reference: ListReferencesByRoutingInstanceID: %v, %+v", err, tenant)
			continue
		}
		if refinfo != nil {
			tenant.Reference = len(*refinfo)
			if err := e.db.UpdateAddonInstanceTenant(&tenant); err != nil {
				logrus.Errorf("sync addon tenant reference: UpdateAddonInstanceTenant: %v", err)
				return false, err
			}
		}
	}
	return false, nil
}

func (e *Endpoints) AddonYmlExport(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	project := r.URL.Query().Get("project")
	yml, err := e.addon.AddonYmlExport(project)
	if err != nil {
		return apierrors.ErrAddonYmlExport.InternalError(err).ToResp(), nil
	}
	yml_str, err := json.Marshal(yml)
	if err != nil {
		return apierrors.ErrAddonYmlExport.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(string(yml_str))
}

func (e *Endpoints) AddonYmlImport(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrAddonYmlImport.InvalidParameter(err).ToResp(), nil
	}
	project := r.URL.Query().Get("project")
	project_int, err := strconv.ParseUint(project, 10, 64)
	if err != nil {
		return apierrors.ErrAddonYmlImport.InvalidParameter(err).ToResp(), nil
	}
	var yml diceyml.Object
	if err := json.NewDecoder(r.Body).Decode(&yml); err != nil {
		return apierrors.ErrAddonYmlImport.InvalidParameter(err).ToResp(), nil
	}
	if err := e.addon.AddonYmlImport(project_int, yml, string(userID)); err != nil {
		return apierrors.ErrAddonYmlImport.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(nil)
}

func (e *Endpoints) SyncAddonResources() (bool, error) {
	e.addon.SyncAddonResources()
	return false, nil
}

// CleanRemainingAddonAttachment clean remain addon attachment
// Deleting the addon before the addon is created will cause the addon to leak
// Deleting the runtime during deployment will cause this to happen
func (e *Endpoints) CleanRemainingAddonAttachment() (bool, error) {
	return e.addon.CleanRemainingAddonAttachment()
}
