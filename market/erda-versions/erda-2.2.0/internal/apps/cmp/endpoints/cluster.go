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

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/impl/ess"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

func (e *Endpoints) UpgradeEdgeCluster(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.UpgradeEdgeClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()
	var req apistructs.UpgradeEdgeClusterRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal request: %+v", err)
		return
	}

	// get identity info
	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}
	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return
	}

	recordID, status, precheckHint, err := e.clusters.UpgradeEdgeCluster(ctx, req, i.UserID, i.OrgID)
	if err != nil {
		err = fmt.Errorf("failed to upgrade cluster: %v", err)
		return
	}
	return mkResponse(apistructs.UpgradeEdgeClusterResponse{
		Header: apistructs.Header{Success: true},
		Data:   apistructs.UpgradeEdgeClusterData{RecordID: recordID, Status: status, PrecheckHint: precheckHint},
	})
}

func (e *Endpoints) BatchUpgradeEdgeCluster(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.BatchUpgradeEdgeClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()
	var req apistructs.BatchUpgradeEdgeClusterRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal request: %+v", err)
		return
	}
	logrus.Debugf("batch upgrade request header:%+v", r.Header)

	// get identity info
	userID, err := user.GetUserID(r)
	if err != nil {
		return
	}

	// permission check
	err = e.PermissionCheck(userID.String(), "", "", apistructs.DeleteAction)
	if err != nil {
	}

	go e.clusters.BatchUpgradeEdgeCluster(ctx, req, userID.String())

	return mkResponse(apistructs.BatchUpgradeEdgeClusterResponse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) OrgClusterInfo(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened: %+v", err)
			resp, err = mkResponse(apistructs.OrgClusterInfoResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
				Data: apistructs.OrgClusterInfoData{
					List: []apistructs.OrgClusterInfoBasicData{},
				},
			})
		}
	}()
	req := apistructs.OrgClusterInfoRequest{}
	req.OrgName = r.URL.Query().Get("orgName")
	req.ClusterType = r.URL.Query().Get("clusterType")
	pageSize := r.URL.Query().Get("pageSize")
	pageNo := r.URL.Query().Get("pageNo")
	req.PageSize, err = strconv.Atoi(pageSize)
	if err != nil {
		logrus.Errorf("failed to parse pageSize")
		req.PageSize = 10
	}
	req.PageNo, err = strconv.Atoi(pageNo)
	if err != nil {
		logrus.Errorf("failed to parse pageNo")
		req.PageNo = 1
	}
	if req.PageNo <= 0 {
		req.PageNo = 1
	}
	if req.PageSize <= 0 {
		req.PageSize = 10
	}

	data, er := e.clusters.GetOrgClusterInfo(ctx, req)
	if er != nil {
		err = fmt.Errorf("list org cluster info failed")
		logrus.Errorf("%s, request:%+v, error:%v", err.Error(), req, er)
	}

	return mkResponse(apistructs.OrgClusterInfoResponse{
		Header: apistructs.Header{Success: true},
		Data:   data,
	})
}

func (e *Endpoints) OfflineEdgeCluster(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	var req apistructs.OfflineEdgeClusterRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal to apistructs.OfflineEdgeClusterRequest: %v", err)
		return
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.DeleteAction)
	if err != nil {
		return
	}

	recordID, preCheckHint, err := e.clusters.OfflineEdgeCluster(ctx, req, i.UserID, i.OrgID)
	if err != nil {
		err = fmt.Errorf("failed to offline cluster: %v", err)
		return
	}

	return mkResponse(apistructs.OfflineEdgeClusterResponse{
		Header: apistructs.Header{Success: true},
		Data:   apistructs.OfflineEdgeClusterData{RecordID: recordID, PreCheckHint: preCheckHint},
	})
}

func (e *Endpoints) BatchOfflineEdgeCluster(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	var req apistructs.BatchOfflineEdgeClusterRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal to apistructs.BatchOfflineEdgeClusterRequest: %v", err)
		return
	}

	userid := r.Header.Get("User-ID")

	// permission check, need to be platform admin user
	err = e.PermissionCheck(userid, "", "", apistructs.DeleteAction)
	if err != nil {
		return
	}

	err = e.clusters.BatchOfflineEdgeCluster(ctx, req, userid)
	if err != nil {
		err = fmt.Errorf("failed to offline cluster: %v", err)
		return
	}

	return mkResponse(apistructs.OfflineEdgeClusterResponse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) ClusterInfo(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	clusternames := r.URL.Query().Get("clusterName")
	clusternameList := strutil.Split(clusternames, ",", true)

	orgIDHeader := r.Header.Get(httputil.OrgHeader)
	orgID, err := strconv.Atoi(orgIDHeader)
	if err != nil {
		return mkResponse(apistructs.ImportClusterResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}

	if len(clusternameList) == 0 {
		errstr := "empty clusterName arg"
		return httpserver.ErrResp(200, "1", errstr)
	}

	result, err := e.clusters.ClusterInfo(ctx, uint64(orgID), clusternameList)
	if err != nil {
		errstr := fmt.Sprintf("failed to get clusterinfo: %s, %v", clusternames, err)
		return httpserver.ErrResp(200, "2", errstr)
	}
	return mkResponse(apistructs.OpsClusterInfoResponse{
		Header: apistructs.Header{Success: true},
		Data:   apistructs.OpsClusterInfoData(result),
	})
}

func (e *Endpoints) ClusterUpdate(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {
	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	header := r.Header

	var req apistructs.CMPClusterUpdateRequest
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to parse request body: %v", err)
		return
	}

	if req.OpsConfig != nil {
		errStr := e.handleUpdateReq(ctx, &req)
		if errStr != "ok" {
			err = fmt.Errorf("faliled to handle update request, message: %s", errStr)
			return
		}
	}

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err := fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return errorresp.ErrResp(err)
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.UpdateAction)
	if err != nil {
		return errorresp.ErrResp(err)
	}

	err = e.clusters.UpdateCluster(ctx, req, header)
	if err != nil {
		err = fmt.Errorf("failed to update clusterinfo: %v", err)
		return
	}

	return mkResponse(apistructs.OpsClusterInfoResponse{
		Header: apistructs.Header{Success: true},
		Data:   nil,
	})
}

func (e *Endpoints) handleUpdateReq(ctx context.Context, req *apistructs.CMPClusterUpdateRequest) string {
	var as *ess.Ess
	var isEdit bool
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	resp, err := e.ClusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: req.Name})
	//TODO: check clusterInfo info
	if err != nil {
		return fmt.Sprintf("failed to get cluster info: %v", err)
	}

	clusterInfo := resp.Data
	accountInfoReq := apistructs.BasicCloudConf{
		Region:          clusterInfo.OpsConfig.Region,
		AccessKeyId:     clusterInfo.OpsConfig.AccessKey,
		AccessKeySecret: clusterInfo.OpsConfig.SecretKey,
	}
	clusterInfo.OpsConfig.ScaleMode = req.OpsConfig.ScaleMode
	if req.OpsConfig.ScaleMode != "" {
		err = e.validateOpsConfig(clusterInfo.OpsConfig)
		if err != nil {
			return fmt.Sprintf("cluster: %s, error: %v", clusterInfo.Name, err)
		}

		as, err = e.Ess.Init(accountInfoReq, e.Mns, e.nodes)
		if err != nil {
			return fmt.Sprintf("failed to init ess sdk: %v", err)
		}
	}
	if req.OpsConfig.ScaleMode == "auto" {
		if clusterInfo.OpsConfig != nil && clusterInfo.OpsConfig.ScaleMode == apistructs.ScaleModeScheduler {
			_, err := e.CronService.CronStop(context.Background(), &cronpb.CronStopRequest{
				CronID: uint64(clusterInfo.OpsConfig.ScalePipeLineID),
			})
			if err != nil {
				return fmt.Sprintf("failed to delete pipline cronjob : %v", err)
			}
		}
		name := clusterInfo.Name + ess.EssScaleSchedulerTaskSuff
		err := as.DeleteScheduledTasks(name)
		if err != nil {
			return fmt.Sprintf("failed to delete scheduler task : %v", err)
		}
		err = as.CreateAutoFlow(req.Name, clusterInfo.OpsConfig.VSwitchIDs, clusterInfo.OpsConfig.EcsPassword, clusterInfo.OpsConfig.SgIDs)
		if err != nil {
			return fmt.Sprintf("failed to create auto scale mode: %v", err)
		}

		opsConfig, err := convertPbOpsConfigToOpsConfig(clusterInfo.OpsConfig)
		if err != nil {
			return fmt.Sprintf("failed to convert clusterpb.OpsConfig to apistructs.OpsConfig")
		}
		req.OpsConfig = opsConfig
		req.OpsConfig.EssScaleRule = as.Config.EssScaleRule
		req.OpsConfig.EssGroupID = as.Config.EssGroupID
		req.OpsConfig.ScalePipeLineID = 0
	}
	if req.OpsConfig.ScaleMode == apistructs.ScaleModeScheduler {
		if clusterInfo.OpsConfig != nil && clusterInfo.OpsConfig.ScaleMode == apistructs.ScaleModeScheduler {
			if clusterInfo.OpsConfig.ScalePipeLineID != 0 {
				_, err := e.CronService.CronStop(context.Background(), &cronpb.CronStopRequest{
					CronID: uint64(clusterInfo.OpsConfig.ScalePipeLineID),
				})
				if err != nil {
					return fmt.Sprintf("failed to delete pipline cronjob : %v", err)
				}
			}
			// Update base on existed schedule scale rule
			if req.OpsConfig.LaunchTime == clusterInfo.OpsConfig.LaunchTime {
				isEdit = true
			}
		}
		clusterInfo.OpsConfig.ScaleNumber = int32(req.OpsConfig.ScaleNumber)
		clusterInfo.OpsConfig.LaunchTime = req.OpsConfig.LaunchTime
		clusterInfo.OpsConfig.RepeatMode = req.OpsConfig.RepeatMode
		clusterInfo.OpsConfig.RepeatValue = req.OpsConfig.RepeatValue
		clusterInfo.OpsConfig.ScaleDuration = int32(req.OpsConfig.ScaleDuration)
		err := as.CreateSchedulerFlow(apistructs.SchedulerScaleReq{
			ClusterName:     req.Name,
			VSwitchID:       clusterInfo.OpsConfig.VSwitchIDs,
			EcsPassword:     clusterInfo.OpsConfig.EcsPassword,
			SgID:            clusterInfo.OpsConfig.SgIDs,
			OrgID:           int(clusterInfo.OrgID),
			Region:          clusterInfo.OpsConfig.Region,
			AccessKeyId:     clusterInfo.OpsConfig.AccessKey,
			AccessKeySecret: clusterInfo.OpsConfig.SecretKey,
			Num:             req.OpsConfig.ScaleNumber,
			LaunchTime:      req.OpsConfig.LaunchTime,
			RecurrenceType:  req.OpsConfig.RepeatMode,
			RecurrenceValue: req.OpsConfig.RepeatValue,
			ScaleDuration:   req.OpsConfig.ScaleDuration,
			IsEdit:          isEdit,
			ScheduledTaskId: clusterInfo.OpsConfig.ScheduledTaskId,
		})
		if err != nil {
			return fmt.Sprintf("failed to create scheduler scale mode: %v", err)
		}

		opsConfig, err := convertPbOpsConfigToOpsConfig(clusterInfo.OpsConfig)
		if err != nil {
			return fmt.Sprintf("failed to convert clusterpb.OpsConfig to apistructs.OpsConfig")
		}
		req.OpsConfig = opsConfig
		req.OpsConfig.ScheduledTaskId = as.Config.ScheduledTaskId
		req.OpsConfig.ScalePipeLineID = as.Config.ScalePipeLineID
		req.OpsConfig.EssGroupID = as.Config.EssGroupID
	}
	if req.OpsConfig.ScaleMode == "none" {
		if clusterInfo.OpsConfig != nil && clusterInfo.OpsConfig.ScaleMode == apistructs.ScaleModeScheduler {
			_, err := e.CronService.CronStop(context.Background(), &cronpb.CronStopRequest{
				CronID: uint64(clusterInfo.OpsConfig.ScalePipeLineID),
			})
			if err != nil {
				return fmt.Sprintf("failed to delete pipline cronjob : %v", err)
			}
		}
		name := clusterInfo.Name + ess.EssScaleSchedulerTaskSuff
		err := as.DeleteScheduledTasks(name)
		if err != nil {
			return fmt.Sprintf("failed to delete scheduler task : %v", err)
		}

		opsConfig, err := convertPbOpsConfigToOpsConfig(clusterInfo.OpsConfig)
		if err != nil {
			return fmt.Sprintf("failed to convert clusterpb.OpsConfig to apistructs.OpsConfig")
		}
		req.OpsConfig = opsConfig
		req.OpsConfig.ScalePipeLineID = 0
		req.OpsConfig.EssGroupID = as.Config.EssGroupID
	}
	return "ok"
}

func (e *Endpoints) validateOpsConfig(opsConf *clusterpb.OpsConfig) error {
	if opsConf == nil {
		err := fmt.Errorf("empty ops config")
		logrus.Error(err.Error())
		return err
	}

	if e.isEmpty(opsConf.AccessKey) || e.isEmpty(opsConf.SecretKey) || e.isEmpty(opsConf.Region) || e.isEmpty(opsConf.EcsPassword) {
		err := fmt.Errorf("invalid ops config")
		return err
	}
	return nil
}

func (e Endpoints) isEmpty(str string) bool {
	return strings.Replace(str, " ", "", -1) == ""
}

func (e *Endpoints) ImportCluster(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {

	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	var req apistructs.ImportCluster
	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal to apistructs.ImportCluster: %v", err)
		return
	}

	logrus.Debugf("cluster init retry request body: %v", req)

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return
	}

	if err = e.clusters.ImportClusterWithRecord(ctx, i.UserID, &req); err != nil {
		return
	}

	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	clusterResp, err := e.ClusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: req.ClusterName})
	if err == nil {
		e.SteveAggregator.Add(clusterResp.Data)
	}
	return mkResponse(apistructs.ImportClusterResponse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) InitClusterRetry(ctx context.Context, r *http.Request, vars map[string]string) (resp httpserver.Responser, err error) {

	defer func() {
		if err != nil {
			logrus.Errorf("error happened, error:%v", err)
			resp, err = mkResponse(apistructs.CloudClusterResponse{
				Header: apistructs.Header{
					Success: false,
					Error:   apistructs.ErrorResponse{Msg: err.Error()},
				},
			})
		}
	}()

	var req apistructs.ClusterInitRetry

	if err = json.NewDecoder(r.Body).Decode(&req); err != nil {
		err = fmt.Errorf("failed to unmarshal to apistructs.ImportCluster: %v", err)
		return
	}

	logrus.Debugf("cluster init retry reuqest body: %v", req)

	i, resp := e.GetIdentity(r)
	if resp != nil {
		err = fmt.Errorf("failed to get User-ID or Org-ID from request header")
		return
	}

	// permission check
	err = e.PermissionCheck(i.UserID, i.OrgID, "", apistructs.CreateAction)
	if err != nil {
		return
	}

	orgID, err := strconv.Atoi(i.OrgID)
	if err != nil {
		return
	}

	if err = e.clusters.ClusterInitRetry(uint64(orgID), &req); err != nil {
		return
	}

	return mkResponse(apistructs.ImportClusterResponse{
		Header: apistructs.Header{Success: true},
	})
}

func (e *Endpoints) InitCluster(ctx context.Context, w http.ResponseWriter, r *http.Request, vars map[string]string) error {
	clusterName := r.URL.Query().Get("clusterName")
	accessKey := r.URL.Query().Get("accessKey")
	orgName := r.URL.Query().Get("orgName")

	if accessKey != "" {
		content, err := e.clusters.RenderInitContent(ctx, orgName, clusterName, accessKey)
		if err != nil {
			return err
		}

		w.Write([]byte(content))

		return nil
	}

	// TODO: orgName from front, need split init-command and render-command interface.
	org := r.Header.Get("Org")
	if org == "" {
		return fmt.Errorf("org name is empty")
	}
	respInfo, err := e.clusters.RenderInitCmd(org, clusterName)
	if err != nil {
		return err
	}

	respObj := apistructs.InitClusterResponse{
		Header: apistructs.Header{Success: true},
		Data:   respInfo,
	}

	respData, err := json.Marshal(respObj)
	if err != nil {
		return err
	}

	w.Header().Set("Content-Type", "application/json")
	w.Write(respData)

	return nil
}

func convertPbOpsConfigToOpsConfig(pbConfig *clusterpb.OpsConfig) (*apistructs.OpsConfig, error) {
	data, err := json.Marshal(pbConfig)
	if err != nil {
		return nil, err
	}

	res := apistructs.OpsConfig{}
	if err = json.Unmarshal(data, &res); err != nil {
		return nil, err
	}
	return &res, nil
}
