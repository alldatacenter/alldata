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

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/internal/tools/orchestrator/spec"
	"github.com/erda-project/erda/internal/tools/orchestrator/utils"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/strutil"
)

// Deprecated
func (s *Endpoints) epBulkGetRuntimeStatusDetail(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	rs_ := r.URL.Query().Get("runtimeIds")
	rs := strings.Split(rs_, ",")
	var runtimeIds []uint64
	for _, r := range rs {
		runtimeId, err := strconv.ParseUint(r, 10, 64)
		if err != nil {
			return utils.ErrRespIllegalParam(err, fmt.Sprintf("failed to bulk get runtime StatusDetail, invalid runtimeIds: %s", rs_))
		}
		runtimeIds = append(runtimeIds, runtimeId)
	}
	funcErrMsg := fmt.Sprintf("failed to bulk get runtime StatusDetail, runtimeIds: %v", runtimeIds)
	runtimes, err := s.db.FindRuntimesByIds(runtimeIds)
	if err != nil {
		return utils.ErrResp0101(err, funcErrMsg)
	}
	if _, err := user.GetUserID(r); err != nil {
		return apierrors.ErrGetRuntime.NotLogin().ToResp(), nil
	}
	data := make(map[uint64]interface{})
	for _, r := range runtimes {
		vars := map[string]string{
			"namespace": r.ScheduleName.Namespace,
			"name":      r.ScheduleName.Name,
		}
		if status, err := s.scheduler.EpGetRuntimeStatus(context.Background(), vars); err != nil {
			return utils.ErrResp0101(err, fmt.Sprintf("failed to bulk get runtime StatusDetail, runtimeId: %d", r.ID))
		} else {
			data[r.ID] = status
		}
	}
	return httpserver.OkResp(data)
}

// Deprecated
func (s *Endpoints) epUpdateOverlay(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	appId_ := r.URL.Query().Get("applicationId")
	appId, err := strconv.Atoi(appId_)
	if err != nil {
		return utils.ErrRespIllegalParam(err, fmt.Sprintf("failed to update Overlay, appId invalid: %v", appId_))
	}
	workspace := r.URL.Query().Get("workspace")
	if len(workspace) == 0 {
		return utils.ErrRespIllegalParam(errors.Errorf("workspace invalid: %v", workspace), "failed to update Overlay")
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateRuntime.NotLogin().ToResp(), nil
	}
	perm, err := s.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  uint64(appId),
		Resource: "runtime-" + strutil.ToLower(workspace),
		Action:   apistructs.OperateAction,
	})
	if err != nil {
		return apierrors.ErrUpdateRuntime.InternalError(err).ToResp(), nil
	}
	if !perm.Access {
		return apierrors.ErrUpdateRuntime.AccessDenied().ToResp(), nil
	}
	runtimeName := r.URL.Query().Get("runtimeName")
	if len(runtimeName) == 0 {
		return utils.ErrRespIllegalParam(errors.Errorf("runtimeName invalid: %v", runtimeName), "failed to update Overlay")
	}
	var overlay apistructs.PreDiceDTO
	if err := json.NewDecoder(r.Body).Decode(&overlay); err != nil {
		return utils.ErrRespIllegalParam(err, "failed to update Overlay, failed to parse req")
	}

	rsr := apistructs.RuntimeScaleRecord{
		ApplicationId: uint64(appId),
		Workspace:     workspace,
		Name:          runtimeName,
		PayLoad:       overlay,
	}

	oldOverlayDataForAudit, err, errMsg := s.processRuntimeScaleRecord(rsr, "")
	if err != nil {
		return utils.ErrResp0101(err, errMsg)
	}

	return httpserver.OkResp(oldOverlayDataForAudit)
}

func getPreDeploymentDiceOverlay(pre *dbclient.PreDeployment) (diceyml.Object, error) {
	var oldOverlay diceyml.Object
	if pre.DiceOverlay != "" {
		if err := json.Unmarshal([]byte(pre.DiceOverlay), &oldOverlay); err != nil {
			logrus.Errorf("Unmarshal PreDeployment record dice_overaly to diceyml.Object for runtime [application_id: %v workspace: %v name: %v ] failed, err: %v", pre.ApplicationId, pre.Workspace, pre.RuntimeName, err)
			return oldOverlay, errors.Errorf("Unmarshal PreDeployment record dice_overaly to diceyml.Object for runtime [application_id: %v workspace: %v name: %v ] failed, err: %v", pre.ApplicationId, pre.Workspace, pre.RuntimeName, err)
		}
	} else {
		var diceObj diceyml.Object
		// 没有 pre.DiceOverlay 信息表示部署之后还未进行过 scale 操作，因此如果当前这次 scale 操作是 scaleDown, 则按 pre.Dice 中的副本数设置 恢复时的副本数
		if err := json.Unmarshal([]byte(pre.Dice), &diceObj); err != nil {
			logrus.Errorf("Unmarshal PreDeployment record dice to diceyml.Object for runtime [application_id: %v workspace: %v name: %v ] failed, err: %v", pre.ApplicationId, pre.Workspace, pre.RuntimeName, err)
			return oldOverlay, errors.Errorf("Unmarshal PreDeployment record dice to diceyml.Object for runtime [application_id: %v workspace: %v name: %v ] failed, err: %v", pre.ApplicationId, pre.Workspace, pre.RuntimeName, err)
		}
		oldOverlay.Services = make(map[string]*diceyml.Service)
		for k, v := range diceObj.Services {
			oldOverlay.Services[k] = &diceyml.Service{
				Deployments: diceyml.Deployments{
					Replicas: v.Deployments.Replicas,
				},
				Resources: diceyml.Resources{
					CPU: v.Resources.CPU,
					Mem: v.Resources.Mem,
				},
			}
		}
	}
	return oldOverlay, nil
}

// processRuntimeScaleRecord 处理单个 runtime 对应的 scale 操作
func (s *Endpoints) processRuntimeScaleRecord(rsc apistructs.RuntimeScaleRecord, action string) (apistructs.PreDiceDTO, error, string) {
	uniqueId := spec.RuntimeUniqueId{
		ApplicationId: rsc.ApplicationId,
		Workspace:     rsc.Workspace,
		Name:          rsc.Name,
	}
	logrus.Errorf("process runtime scale for runtime %#v", uniqueId)

	pre, err := s.db.FindPreDeployment(uniqueId)
	if err != nil {
		logrus.Errorf("process runtime scale failed, find PreDeployment record in table ps_v2_pre_builds failed for runtime %#v, err: %v", uniqueId, err)
		errMsg := fmt.Sprintf("find PreDeployment record in table ps_v2_pre_builds failed for runtime %#v, err: %v", uniqueId, err)
		return apistructs.PreDiceDTO{}, err, errMsg
	}

	oldOverlay, err := getPreDeploymentDiceOverlay(pre)
	if err != nil {
		return apistructs.PreDiceDTO{}, err, fmt.Sprintf("%s", err)
	}

	// Global Envs
	if rsc.PayLoad.Envs != nil {
		oldOverlay.Envs = rsc.PayLoad.Envs
	}
	var (
		needUpdateServices     []string
		oldOverlayDataForAudit = apistructs.PreDiceDTO{
			Services: make(map[string]*apistructs.RuntimeInspectServiceDTO, 0),
		}
	)
	oldServiceReplicas := make(map[string]int)
	for k, v := range rsc.PayLoad.Services {
		oldService, exists := oldOverlay.Services[k]
		if !exists || oldService == nil {
			oldService = &diceyml.Service{}
			if oldOverlay.Services == nil {
				oldOverlay.Services = map[string]*diceyml.Service{}
			}
			oldOverlay.Services[k] = oldService
		}
		// Local Envs
		if v.Envs != nil {
			oldService.Envs = v.Envs
		}
		// record need update scale's service

		if oldService.Resources.CPU != v.Resources.CPU || oldService.Resources.Mem != v.Resources.Mem ||
			oldService.Resources.Disk != v.Resources.Disk || oldService.Deployments.Replicas != v.Deployments.Replicas {
			needUpdateServices = append(needUpdateServices, k)
			// record old service's scale for audit
			oldOverlayDataForAudit.Services[k] = genOverlayDataForAudit(oldService)
		} else if action == apistructs.ScaleActionUp {
			// 由于 pre.DiceOverlay 保留了 scale 到 0 之前的副本数（比如2），因而 如果 从 0 恢复到之前非0 副本（2），实际上原来判断是否需要更新的逻辑就认为不需要更新
			needUpdateServices = append(needUpdateServices, k)
			// record old service's scale for audit
			oldOverlayDataForAudit.Services[k] = genOverlayDataForAudit(oldService)
		}

		// 仅更新最新部署的副本数不为 0 的情况，如果，最新部署副本为 0，则保留上次部署时的副本数，用于启动恢复
		if v.Deployments.Replicas == 0 {
			oldServiceReplicas[k] = oldService.Deployments.Replicas
		}

		// Replicas
		oldService.Deployments.Replicas = v.Deployments.Replicas
		// Resources
		oldService.Resources.CPU = v.Resources.CPU
		oldService.Resources.Mem = v.Resources.Mem
		oldService.Resources.Disk = v.Resources.Disk
	}

	// really update scale
	if len(needUpdateServices) != 0 {
		runtime, err := s.db.FindRuntime(uniqueId)
		if err != nil {
			logrus.Errorf("process runtime scale failed, find runtime record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
			errMsg := fmt.Sprintf("find runtime record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
			return apistructs.PreDiceDTO{}, err, errMsg
		}
		if runtime == nil {
			logrus.Errorf("process runtime scale failed, runtime %s is not existed for runtime %#v", uniqueId.Name, uniqueId)
			errMsg := fmt.Sprintf("runtime %s is not existed for runtime %#v", uniqueId.Name, uniqueId)
			return apistructs.PreDiceDTO{}, errors.Errorf("runtime %s is not existed", uniqueId.Name), errMsg
		}

		namespace, name := runtime.ScheduleName.Args()
		sg := &apistructs.ServiceGroup{
			ClusterName: runtime.ClusterName,
			Dice: apistructs.Dice{
				ID:       name,
				Type:     namespace,
				Services: make([]apistructs.Service, 0),
			},
		}
		for _, svcName := range needUpdateServices {
			sg.Services = append(sg.Services, apistructs.Service{
				Name:  svcName,
				Scale: oldOverlay.Services[svcName].Deployments.Replicas,
				Resources: apistructs.Resources{
					Cpu:  oldOverlay.Services[svcName].Resources.CPU,
					Mem:  float64(oldOverlay.Services[svcName].Resources.Mem),
					Disk: float64(oldOverlay.Services[svcName].Resources.Disk),
				},
			})
		}

		// 未设置 action，则需要自行判断用于执行 scale 操作之后的 runtime 状态更新
		if action == "" {
			// 如果所有的 serice 的副本数都调整为0，则是停止操作，Runtime 状态应该为 Stopped，否则不是停止操作，Runtime 状态应该为 Healthy
			action = apistructs.ScaleActionDown
			for _, svc := range sg.Services {
				if svc.Scale > 0 {
					action = apistructs.ScaleActionUp
					break
				}
			}
		}

		sgb, _ := json.Marshal(&sg)
		logrus.Debugf("scale service group body is %s", string(sgb))

		if _, err := s.scheduler.Httpendpoints.ServiceGroupImpl.Scale(sg); err != nil {
			logrus.Errorf("process runtime scale failed, ScaleServiceGroup failed for runtime %s for runtime %#v failed, err: %v", uniqueId.Name, uniqueId, err)
			errMsg := fmt.Sprintf("ScaleServiceGroup failed for runtime %s for runtime %#v failed, err: %v", uniqueId.Name, uniqueId, err)
			return apistructs.PreDiceDTO{}, err, errMsg
		}

		if action == apistructs.ScaleActionDown || action == apistructs.ScaleActionUp {
			addons, err := s.db.GetUnDeletableAttachMentsByRuntimeID(runtime.ID)
			if err != nil {
				logrus.Warnf("process runtime scale successed, but update runtime referenced addon attact_count for runtime %#v failed, err: %v", uniqueId, err)
			}
			switch action {
			case apistructs.ScaleActionDown:
				runtime.Status = apistructs.RuntimeStatusStopped
				if err := s.db.UpdateRuntime(runtime); err != nil {
					logrus.Warnf("process runtime scale successed, but update runtime_status to 'Stopped' in %s for runtime %#v failed, err: %v", runtime.TableName(), uniqueId, err)
				}

				for _, att := range *addons {
					addonRouting, err := s.db.GetInstanceRouting(att.RoutingInstanceID)
					if err != nil {
						logrus.Warnf("process runtime scale successed, but update runtime referenced addon attact_count for runtime %#v failed, err: %v", uniqueId, err)
						continue
					}

					if addonRouting != nil && addonRouting.PlatformServiceType == 0 {
						att.Deleted = apistructs.AddonScaleDown
						if err := s.db.UpdateAttachment(&att); err != nil {
							logrus.Warnf("process runtime scale successed, but update table %s for runtime %#v failed, err: %v", att.TableName(), uniqueId, err)
						}
					}
				}

			case apistructs.ScaleActionUp:
				runtime.Status = apistructs.RuntimeStatusHealthy
				if err := s.db.UpdateRuntime(runtime); err != nil {
					logrus.Warnf("process runtime scale successed, but update runtime_status to 'Healthy' in %s for runtime %#v failed, err: %v", runtime.TableName(), uniqueId, err)
				}

				for _, att := range *addons {
					addonRouting, err := s.db.GetInstanceRouting(att.RoutingInstanceID)
					if err != nil {
						logrus.Warnf("process runtime scale successed, but update runtime referenced addon attact_count for runtime %#v failed, err: %v", uniqueId, err)
						continue
					}

					if addonRouting != nil && addonRouting.PlatformServiceType == 0 {
						att.Deleted = apistructs.AddonNotDeleted
						if err := s.db.UpdateAttachment(&att); err != nil {
							logrus.Warnf("process runtime scale successed, but update table %s for runtime %#v failed, err: %v", att.TableName(), uniqueId, err)
						}
					}
				}
			}
		}
		s.db.UpdateRuntime(runtime)
	}

	// 保留非停止状态下的副本数
	if action != apistructs.ScaleActionDown {
		for k := range oldOverlay.Services {
			if oldOverlay.Services[k].Deployments.Replicas == 0 {
				if replicas, ok := oldServiceReplicas[k]; ok {
					if replicas > 0 {
						oldOverlay.Services[k].Deployments.Replicas = replicas
					}
				}
			}
		}
		// save changes
		o_, err := json.Marshal(oldOverlay)
		if err != nil {
			logrus.Errorf("process runtime scale failed, ScaleServiceGroup for runtime %s for runtime %#v successfully, but json.Marshal diceyml.Object oldOverlay %#v failed, err: %v", uniqueId.Name, uniqueId, oldOverlay, err)
			errMsg := fmt.Sprintf("ScaleServiceGroup for runtime %s for runtime %#v successfully, but json.Marshal diceyml.Object oldOverlay %#v failed, err: %v", uniqueId.Name, uniqueId, oldOverlay, err)
			return apistructs.PreDiceDTO{}, err, errMsg

		}
		pre.DiceOverlay = string(o_)
		if err := s.db.UpdatePreDeployment(pre); err != nil {
			logrus.Errorf("process runtime scale failed, ScaleServiceGroup for runtime %s for runtime %#v successfully, but update table ps_v2_pre_builds failed, err: %v", uniqueId.Name, uniqueId, err)
			errMsg := fmt.Sprintf("ScaleServiceGroup for runtime %s for runtime %#v successfully, but update table ps_v2_pre_builds failed, err: %v", uniqueId.Name, uniqueId, err)
			return apistructs.PreDiceDTO{}, err, errMsg
		}
	}

	logrus.Errorf("process runtime scale for runtime %#v successfully", uniqueId)
	return oldOverlayDataForAudit, nil, ""
}

// getRuntimeScaleRecordByRuntimeId 通过 runtime ID 列表查找 Runtime 实例并转换成 RuntimeScaleRecord 对象列表
// 注意：逻辑中确保每个 Id 都有对应的 Runtime，以免有不存在的 id
func (s *Endpoints) getRuntimeScaleRecordByRuntimeIds(ids []uint64) ([]dbclient.Runtime, []apistructs.RuntimeScaleRecord, error) {
	rsrs := make([]apistructs.RuntimeScaleRecord, 0)

	runtimes, err := s.db.FindRuntimesByIds(ids)
	if err != nil {
		logrus.Errorf("find runtimes by ids in ps_v2_project_runtimes failed, err: %v", err)
		return runtimes, rsrs, err
	}
	if len(runtimes) == 0 {
		logrus.Errorf("[batch redeploy] find runtimes by ids in ps_v2_project_runtimes failed, err: %v", err)
		return runtimes, rsrs, errors.Errorf("no runtimes found by runtime ids %#v", ids)
	}

	// 确保所有的 ids 都有对应的 Runtime 记录
	notExistedIds := make([]uint64, 0)
	existedIds := make(map[uint64]bool)
	for _, rt := range runtimes {
		existedIds[rt.ID] = true
	}
	for _, id := range ids {
		if _, existed := existedIds[id]; !existed {
			notExistedIds = append(notExistedIds, id)
		}
	}

	if len(notExistedIds) > 0 {
		logrus.Errorf("[batch redeploy] not found runtimes for runtime id:  %#v", notExistedIds)
		return runtimes, rsrs, errors.Errorf("no runtimes found for runtime id: %#v", notExistedIds)
	}

	for _, rt := range runtimes {
		runtimescaleRecord := apistructs.RuntimeScaleRecord{
			ApplicationId: rt.ApplicationID,
			Workspace:     rt.Workspace,
			Name:          rt.Name,
			RuntimeID:     rt.ID,
			PayLoad: apistructs.PreDiceDTO{
				Services: make(map[string]*apistructs.RuntimeInspectServiceDTO),
			},
		}

		uniqueId := spec.RuntimeUniqueId{
			ApplicationId: rt.ApplicationID,
			Workspace:     rt.Workspace,
			Name:          rt.Name,
		}

		pre, err := s.db.FindPreDeployment(uniqueId)
		if err != nil {
			logrus.Errorf("find PreDeployment record in table ps_v2_pre_builds failed for runtime %#v, err: %v", uniqueId, err)
			return runtimes, rsrs, errors.Errorf("find PreDeployment record in table ps_v2_pre_builds failed for runtime %#v, err: %v", uniqueId, err)
		}

		oldOverlay, err := getPreDeploymentDiceOverlay(pre)
		if err != nil {
			logrus.Errorf("getPreDeploymentDiceOverlay failed get r diceyml.Object in table ps_v2_pre_builds for runtime %#v, err: %v", uniqueId, err)
			return runtimes, rsrs, errors.Errorf("no runtimes found for runtime id: %#v", notExistedIds)
		}
		for k, svc := range oldOverlay.Services {
			runtimescaleRecord.PayLoad.Services[k] = &apistructs.RuntimeInspectServiceDTO{
				Deployments: apistructs.RuntimeServiceDeploymentsDTO{
					Replicas: svc.Deployments.Replicas,
				},
				Resources: apistructs.RuntimeServiceResourceDTO{
					CPU: svc.Resources.CPU,
					Mem: svc.Resources.Mem,
				},
			}
		}
		rsrs = append(rsrs, runtimescaleRecord)
	}
	return runtimes, rsrs, nil
}

// BatchUpdateOverlay 批量处理 runtimes 的服务扩缩容、删除、重新部署等操作
func (s *Endpoints) BatchUpdateOverlay(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrUpdateRuntime.NotLogin().ToResp(), nil
	}

	var runtimeScaleRecords apistructs.RuntimeScaleRecords
	if err := json.NewDecoder(r.Body).Decode(&runtimeScaleRecords); err != nil {
		return utils.ErrRespIllegalParam(err, "failed to batch update Overlay, failed to parse req")
	}

	if len(runtimeScaleRecords.Runtimes) == 0 && len(runtimeScaleRecords.IDs) == 0 {
		return utils.ErrRespIllegalParam(err, "failed to batch update Overlay, no runtimeRecords or ids provided in request body")
	}

	if len(runtimeScaleRecords.Runtimes) != 0 && len(runtimeScaleRecords.IDs) != 0 {
		return utils.ErrRespIllegalParam(err, "failed to batch update Overlay, runtimeRecords and ids must only one wtih non-empty values in request body")
	}

	var runtimes []dbclient.Runtime
	if len(runtimeScaleRecords.Runtimes) == 0 {
		runtimes, runtimeScaleRecords.Runtimes, err = s.getRuntimeScaleRecordByRuntimeIds(runtimeScaleRecords.IDs)
		if err != nil {
			logrus.Errorf("[batch redeploy] find runtimes by ids in ps_v2_project_runtimes failed, err: %v", err)
			return utils.ErrResp0101(err, fmt.Sprintf("failed get diceyml.Object in table ps_v2_pre_builds for runtime ids %#v", runtimeScaleRecords.IDs))
		}
	}

	// check permission
	for _, rsr := range runtimeScaleRecords.Runtimes {
		perm, err := s.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.AppScope,
			ScopeID:  rsr.ApplicationId,
			Resource: "runtime-" + strutil.ToLower(rsr.Workspace),
			Action:   apistructs.OperateAction,
		})
		if err != nil {
			return apierrors.ErrUpdateRuntime.InternalError(err).ToResp(), nil
		}
		if !perm.Access {
			return apierrors.ErrUpdateRuntime.AccessDenied().ToResp(), nil
		}
	}

	action := r.URL.Query().Get(apistructs.ScaleAction)

	// 根据 action 的取值执行相应操作
	switch action {
	// 批量重新部署
	case apistructs.ScaleActionReDeploy:
		logrus.Infof("[batch redeploy] do batch runtimes redeploy")
		batchRuntimeReDeployResult := s.batchRuntimeReDeploy(ctx, userID, runtimes, runtimeScaleRecords)
		if batchRuntimeReDeployResult.Failed > 0 {
			return httpserver.NotOkResp(batchRuntimeReDeployResult, http.StatusInternalServerError)
		}
		logrus.Infof("[batch redeploy] redeploy all runtimes successfully")
		return httpserver.OkResp(batchRuntimeReDeployResult)

	// 批量恢复
	case apistructs.ScaleActionUp:
		logrus.Infof("[batch recovery] do batch runtimes recover scale up from replicas 0 to last non-zero, will get non-zero from table ps_v2_pre_builds filed dice_overlay")
		s.batchRuntimeRecovery(&runtimeScaleRecords)

	// 批量停止
	case apistructs.ScaleActionDown:
		logrus.Infof("[batch scale] do batch runtimes scale down to replicas 0")
		s.batchRuntimeScaleAddRuntimeIDs(&runtimeScaleRecords, apistructs.ScaleActionDown)

	// 批量删除
	case apistructs.ScaleActionDelete:
		logrus.Infof("[batch delete] do batch runtimes delete")
		batchRuntimeDeleteResult := s.batchRuntimeDelete(userID, runtimes, runtimeScaleRecords)
		if batchRuntimeDeleteResult.Failed > 0 {
			return httpserver.NotOkResp(batchRuntimeDeleteResult, http.StatusInternalServerError)
		}
		logrus.Infof("[batch delete] delete all runtimes successfully")
		return httpserver.OkResp(batchRuntimeDeleteResult)

	// 批量非停止/启动方式的扩缩容
	case "":
		logrus.Infof("[batch scale] do batch runtime scales based http request payLoad, no need adjust")
		if len(runtimeScaleRecords.IDs) > 0 {
			return utils.ErrRespIllegalParam(err, "failed to do specifical batch scale, only runtimeRecords wtih non-empty values in request body is needed")
		}
		s.batchRuntimeScaleAddRuntimeIDs(&runtimeScaleRecords, "")
	// 请求字符串指定 scale_action	参数,但对应的值为无效值
	default:
		return apierrors.ErrUpdateRuntime.InvalidParameter("invalid parameter value for parameter " + apistructs.ScaleAction + ", valid value is [scaleUp] [scaleDown] [delete] [reDeploy] []").ToResp(), nil
	}

	// scale runtime
	//oldOverlayDataForAudits := make([]apistructs.PreDiceDTO,0)
	oldOverlayDataForAudits := apistructs.BatchRuntimeScaleResults{
		Total:           len(runtimeScaleRecords.Runtimes),
		Successed:       0,
		Faild:           0,
		FailedScales:    make([]apistructs.RuntimeScaleRecord, 0),
		FailedIds:       make([]uint64, 0),
		SuccessedScales: make([]apistructs.PreDiceDTO, 0),
		SuccessedIds:    make([]uint64, 0),
	}
	for _, rsr := range runtimeScaleRecords.Runtimes {
		oldOverlayDataForAudit, err, errMsg := s.processRuntimeScaleRecord(rsr, action)
		if err != nil {
			logrus.Errorf(errMsg)
			oldOverlayDataForAudits.Faild++
			rsr.ErrMsg = errMsg
			oldOverlayDataForAudits.FailedIds = append(oldOverlayDataForAudits.FailedIds, rsr.RuntimeID)
			oldOverlayDataForAudits.FailedScales = append(oldOverlayDataForAudits.FailedScales, rsr)
		} else {
			oldOverlayDataForAudits.Successed++
			oldOverlayDataForAudits.SuccessedIds = append(oldOverlayDataForAudits.SuccessedIds, rsr.RuntimeID)
			oldOverlayDataForAudits.SuccessedScales = append(oldOverlayDataForAudits.SuccessedScales, oldOverlayDataForAudit)
		}
	}

	if oldOverlayDataForAudits.Faild > 0 {
		return httpserver.NotOkResp(oldOverlayDataForAudits, http.StatusInternalServerError)
	}
	logrus.Infof("[batch scale] scale all runtimes successfully")
	return httpserver.OkResp(oldOverlayDataForAudits)
}

func genOverlayDataForAudit(oldService *diceyml.Service) *apistructs.RuntimeInspectServiceDTO {
	return &apistructs.RuntimeInspectServiceDTO{
		Resources: apistructs.RuntimeServiceResourceDTO{
			CPU:  oldService.Resources.CPU,
			Mem:  oldService.Resources.Mem,
			Disk: oldService.Resources.Disk,
		},
		Deployments: apistructs.RuntimeServiceDeploymentsDTO{
			Replicas: oldService.Deployments.Replicas,
		},
	}
}

// batchRuntimeReDeploy 批量重新部署
func (s *Endpoints) batchRuntimeReDeploy(ctx context.Context, userID user.ID, runtimes []dbclient.Runtime, runtimeScaleRecords apistructs.RuntimeScaleRecords) apistructs.BatchRuntimeReDeployResults {
	batchRuntimeReDeployResult := apistructs.BatchRuntimeReDeployResults{
		Total:           len(runtimeScaleRecords.Runtimes),
		Success:         0,
		Failed:          0,
		ReDeployed:      make([]apistructs.RuntimeDeployDTO, 0),
		ReDeployedIds:   make([]uint64, 0),
		UnReDeployed:    make([]apistructs.RuntimeDTO, 0),
		UnReDeployedIds: make([]uint64, 0),
		ErrMsg:          make([]string, 0),
	}

	// 根据请求 RuntimeScaleRecords.IDs  执行重新部署
	if len(runtimeScaleRecords.IDs) > 0 {
		for _, runtime := range runtimes {
			redep, err := s.runtime.RedeployPipeline(ctx, userID, runtime.OrgID, runtime.ID)
			if err != nil {
				logrus.Errorf("[batch redeploy] redeploy failed for runtime %s for runtime instance: %#v, error: %v", runtime.Name, runtime, err)
				errMsg := fmt.Sprintf("redeploy redeploy failed for runtime %s for runtime instance: %#v, error: %v", runtime.Name, runtime, err)
				batchRuntimeReDeployResult.ErrMsg = append(batchRuntimeReDeployResult.ErrMsg, errMsg)
				batchRuntimeReDeployResult.Failed++
				batchRuntimeReDeployResult.UnReDeployedIds = append(batchRuntimeReDeployResult.UnReDeployedIds, runtime.ID)
				curr := apistructs.RuntimeDTO{
					Name:          runtime.Name,
					Workspace:     runtime.Workspace,
					ApplicationID: runtime.ApplicationID,
				}
				batchRuntimeReDeployResult.UnReDeployed = append(batchRuntimeReDeployResult.UnReDeployed, curr)
				continue
			}

			logrus.Infof("[batch redeploy] redeploy successed for rruntime %s for runtime instance: %#v", runtime.Name, runtime)
			batchRuntimeReDeployResult.Success++
			batchRuntimeReDeployResult.ReDeployedIds = append(batchRuntimeReDeployResult.ReDeployedIds, runtime.ID)
			batchRuntimeReDeployResult.ReDeployed = append(batchRuntimeReDeployResult.ReDeployed, *redep)
		}
	} else {
		// 根据请求 RuntimeScaleRecords.Runtimes  执行重新部署
		for index := range runtimeScaleRecords.Runtimes {
			uniqueId := spec.RuntimeUniqueId{
				ApplicationId: runtimeScaleRecords.Runtimes[index].ApplicationId,
				Workspace:     runtimeScaleRecords.Runtimes[index].Workspace,
				Name:          runtimeScaleRecords.Runtimes[index].Name,
			}

			curr := apistructs.RuntimeDTO{
				Name:          uniqueId.Name,
				Workspace:     uniqueId.Workspace,
				ApplicationID: uniqueId.ApplicationId,
			}

			runtime, err := s.db.FindRuntime(uniqueId)
			if err != nil {
				logrus.Errorf("[batch redeploy] find runtime record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
				errMsg := fmt.Sprintf("find runtime record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
				batchRuntimeReDeployResult.ErrMsg = append(batchRuntimeReDeployResult.ErrMsg, errMsg)
				batchRuntimeReDeployResult.Failed++
				batchRuntimeReDeployResult.UnReDeployedIds = append(batchRuntimeReDeployResult.UnReDeployedIds, runtime.ID)
				batchRuntimeReDeployResult.UnReDeployed = append(batchRuntimeReDeployResult.UnReDeployed, curr)
				continue
			}
			if runtime == nil {
				logrus.Errorf("[batch redeploy] runtime %s is not existed for runtime %#v, can not redeploy", uniqueId.Name, uniqueId)
				errMsg := fmt.Sprintf("runtime %s is not existed for runtime %#v, can not redeploy", uniqueId.Name, uniqueId)
				batchRuntimeReDeployResult.ErrMsg = append(batchRuntimeReDeployResult.ErrMsg, errMsg)
				batchRuntimeReDeployResult.Failed++
				batchRuntimeReDeployResult.UnReDeployedIds = append(batchRuntimeReDeployResult.UnReDeployedIds, runtime.ID)
				batchRuntimeReDeployResult.UnReDeployed = append(batchRuntimeReDeployResult.UnReDeployed, curr)
				continue
			}

			redep, err := s.runtime.RedeployPipeline(ctx, userID, runtime.OrgID, runtime.ID)
			if err != nil {
				logrus.Errorf("[batch redeploy] redeploy failed for runtime %s for %#v", uniqueId.Name, uniqueId)
				errMsg := fmt.Sprintf("redeploy failed for runtime %s for %#v", uniqueId.Name, uniqueId)
				batchRuntimeReDeployResult.ErrMsg = append(batchRuntimeReDeployResult.ErrMsg, errMsg)
				batchRuntimeReDeployResult.Failed++
				batchRuntimeReDeployResult.UnReDeployedIds = append(batchRuntimeReDeployResult.UnReDeployedIds, runtime.ID)
				batchRuntimeReDeployResult.UnReDeployed = append(batchRuntimeReDeployResult.UnReDeployed, curr)
				continue
			}

			logrus.Infof("[batch redeploy] redeploy successed for runtime %s for %#v", uniqueId.Name, uniqueId)
			batchRuntimeReDeployResult.Success++
			batchRuntimeReDeployResult.ReDeployedIds = append(batchRuntimeReDeployResult.ReDeployedIds, runtime.ID)
			batchRuntimeReDeployResult.ReDeployed = append(batchRuntimeReDeployResult.ReDeployed, *redep)
		}
	}
	return batchRuntimeReDeployResult
}

// batchRuntimeRecovery 批量恢复（副本数 0 ----> N）
func (s *Endpoints) batchRuntimeRecovery(runtimeScaleRecords *apistructs.RuntimeScaleRecords) {
	// len(runtimeScaleRecords.IDs) > 0, 则已经在 getRuntimeScaleRecordByRuntimeIds 逻辑中构建了 runtimeScaleRecords.Runtimes，此处无需重新构建

	// 根据请求 RuntimeScaleRecords.IDs  执行重新部署
	if len(runtimeScaleRecords.IDs) == 0 {
		for index := range runtimeScaleRecords.Runtimes {
			uniqueId := spec.RuntimeUniqueId{
				ApplicationId: runtimeScaleRecords.Runtimes[index].ApplicationId,
				Workspace:     runtimeScaleRecords.Runtimes[index].Workspace,
				Name:          runtimeScaleRecords.Runtimes[index].Name,
			}
			runtime, err := s.db.FindRuntime(uniqueId)
			if err != nil {
				logrus.Errorf("[batch delete] find runtime record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
				continue
			}
			if runtime != nil {
				runtimeScaleRecords.Runtimes[index].RuntimeID = runtime.ID
			}

			pre, err := s.db.FindPreDeployment(uniqueId)
			if err != nil {
				logrus.Errorf("[batch scale up] find PreDeployment record in table ps_v2_pre_builds failed for runtime %#v, err: %v", uniqueId, err)
				continue
			}
			var oldOverlay diceyml.Object
			if pre.DiceOverlay != "" {
				if err = json.Unmarshal([]byte(pre.DiceOverlay), &oldOverlay); err != nil {
					logrus.Errorf("[batch scale up] Unmarshal PreDeployment record dice_overaly to diceyml.Object for runtime %#v failed, err: %v", uniqueId, err)
					continue
				}
			} else {
				var diceObj diceyml.Object
				if err = json.Unmarshal([]byte(pre.Dice), &diceObj); err != nil {
					logrus.Errorf("[batch scale up] Unmarshal PreDeployment record dice to diceyml.Object for runtime %#v failed, err: %v", uniqueId, err)
					continue
				}
				oldOverlay.Services = make(map[string]*diceyml.Service)
				for k, v := range diceObj.Services {
					oldOverlay.Services[k] = &diceyml.Service{
						Deployments: diceyml.Deployments{
							Replicas: v.Deployments.Replicas,
						},
						Resources: diceyml.Resources{
							CPU: v.Resources.CPU,
							Mem: v.Resources.Mem,
						},
					}
				}
			}

			for k, v := range runtimeScaleRecords.Runtimes[index].PayLoad.Services {
				if _, ok := oldOverlay.Services[k]; ok {
					v.Deployments.Replicas = oldOverlay.Services[k].Deployments.Replicas
				}
			}
		}
	}
}

func (s *Endpoints) batchRuntimeDelete(userID user.ID, runtimes []dbclient.Runtime, runtimeScaleRecords apistructs.RuntimeScaleRecords) apistructs.BatchRuntimeDeleteResults {
	batchRuntimeDeleteResult := apistructs.BatchRuntimeDeleteResults{
		Total:        len(runtimeScaleRecords.Runtimes),
		Success:      0,
		Failed:       0,
		Deleted:      make([]apistructs.RuntimeDTO, 0),
		DeletedIds:   make([]uint64, 0),
		UnDeleted:    make([]apistructs.RuntimeDTO, 0),
		UnDeletedIds: make([]uint64, 0),
		ErrMsg:       make([]string, 0),
	}

	// 根据请求 RuntimeScaleRecords.IDs  执行删除
	if len(runtimeScaleRecords.IDs) > 0 {
		for _, runtime := range runtimes {
			logrus.Debugf("[batch delete] deleting runtime %d, operator %s", runtime.ID, userID)
			runtimedto, err := s.runtime.Delete(userID, runtime.OrgID, runtime.ID)
			if err != nil {
				logrus.Errorf("[batch delete] delete runtime Id %v  runtime %#v failed, error: %v", runtime.ID, runtime, err)
				errMsg := fmt.Sprintf("delete runtime Id %v  runtime %#v failed, error: %v", runtime.ID, runtime, err)
				batchRuntimeDeleteResult.Failed++
				batchRuntimeDeleteResult.ErrMsg = append(batchRuntimeDeleteResult.ErrMsg, errMsg)
				batchRuntimeDeleteResult.UnDeletedIds = append(batchRuntimeDeleteResult.UnDeletedIds, runtime.ID)
				continue
			}

			batchRuntimeDeleteResult.Success++
			batchRuntimeDeleteResult.DeletedIds = append(batchRuntimeDeleteResult.DeletedIds, runtime.ID)
			batchRuntimeDeleteResult.Deleted = append(batchRuntimeDeleteResult.Deleted, *runtimedto)
		}
	} else {
		// 根据请求 RuntimeScaleRecords.Runtimes  执行删除
		for index := range runtimeScaleRecords.Runtimes {
			uniqueId := spec.RuntimeUniqueId{
				ApplicationId: runtimeScaleRecords.Runtimes[index].ApplicationId,
				Workspace:     runtimeScaleRecords.Runtimes[index].Workspace,
				Name:          runtimeScaleRecords.Runtimes[index].Name,
			}

			curr := apistructs.RuntimeDTO{
				Name:          uniqueId.Name,
				Workspace:     uniqueId.Workspace,
				ApplicationID: uniqueId.ApplicationId,
			}

			runtime, err := s.db.FindRuntime(uniqueId)
			if err != nil {
				logrus.Errorf("[batch delete] find runtime record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
				errMsg := fmt.Sprintf("find runtime record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
				batchRuntimeDeleteResult.Failed++
				batchRuntimeDeleteResult.ErrMsg = append(batchRuntimeDeleteResult.ErrMsg, errMsg)
				batchRuntimeDeleteResult.UnDeletedIds = append(batchRuntimeDeleteResult.UnDeletedIds, runtime.ID)
				batchRuntimeDeleteResult.UnDeleted = append(batchRuntimeDeleteResult.UnDeleted, curr)
				continue
			}
			if runtime == nil {
				logrus.Errorf("[batch delete] runtime %s is not existed for runtime %#v", uniqueId.Name, uniqueId)
				errMsg := fmt.Sprintf("no record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
				batchRuntimeDeleteResult.Failed++
				batchRuntimeDeleteResult.ErrMsg = append(batchRuntimeDeleteResult.ErrMsg, errMsg)
				batchRuntimeDeleteResult.UnDeletedIds = append(batchRuntimeDeleteResult.UnDeletedIds, runtime.ID)
				batchRuntimeDeleteResult.UnDeleted = append(batchRuntimeDeleteResult.UnDeleted, curr)
				continue
			}

			logrus.Debugf("[batch delete] deleting runtime %d, operator %s", runtime.ID, userID)
			runtimedto, err := s.runtime.Delete(userID, runtime.OrgID, runtime.ID)
			if err != nil {
				logrus.Errorf("[batch delete] runtime %s is delete failed %#v, error: %v", uniqueId.Name, uniqueId, err)
				errMsg := fmt.Sprintf("runtime %s is delete failed %#v, error: %v", uniqueId.Name, uniqueId, err)
				batchRuntimeDeleteResult.Failed++
				batchRuntimeDeleteResult.ErrMsg = append(batchRuntimeDeleteResult.ErrMsg, errMsg)
				batchRuntimeDeleteResult.UnDeletedIds = append(batchRuntimeDeleteResult.UnDeletedIds, runtime.ID)
				batchRuntimeDeleteResult.UnDeleted = append(batchRuntimeDeleteResult.UnDeleted, curr)
				continue
			}

			batchRuntimeDeleteResult.Success++
			batchRuntimeDeleteResult.DeletedIds = append(batchRuntimeDeleteResult.DeletedIds, runtime.ID)
			batchRuntimeDeleteResult.Deleted = append(batchRuntimeDeleteResult.Deleted, *runtimedto)
		}
	}
	return batchRuntimeDeleteResult
}

// batchRuntimeScaleDownAddRuntimeIDs 为 RuntimeScaleRecord 对象加入 runtimeID
func (s *Endpoints) batchRuntimeScaleAddRuntimeIDs(runtimeScaleRecords *apistructs.RuntimeScaleRecords, action string) {
	for index := range runtimeScaleRecords.Runtimes {

		if action == apistructs.ScaleActionDown {
			for _, v := range runtimeScaleRecords.Runtimes[index].PayLoad.Services {
				v.Deployments.Replicas = 0
			}
		}

		if runtimeScaleRecords.Runtimes[index].RuntimeID == 0 {
			uniqueId := spec.RuntimeUniqueId{
				ApplicationId: runtimeScaleRecords.Runtimes[index].ApplicationId,
				Workspace:     runtimeScaleRecords.Runtimes[index].Workspace,
				Name:          runtimeScaleRecords.Runtimes[index].Name,
			}

			runtime, err := s.db.FindRuntime(uniqueId)
			logrus.Errorf("[batch redeploy] find runtime record in ps_v2_project_runtimes for runtime %#v, runtime %#v, error: %v", uniqueId, runtime, err)
			if err != nil {
				logrus.Errorf("[batch redeploy] find runtime record in ps_v2_project_runtimes for runtime %#v failed, err: %v", uniqueId, err)
				continue
			}
			if runtime != nil {
				runtimeScaleRecords.Runtimes[index].RuntimeID = runtime.ID
			}
		}
	}
}
