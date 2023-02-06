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

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateRuntime 创建应用实例
func (e *Endpoints) CreateRuntime(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	// TODO: 需要等 pipeline action 调用走内网后，再从 header 中取 User-ID (operator)
	var req apistructs.RuntimeCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		// param problem
		return apierrors.ErrCreateRuntime.InvalidParameter("req body").ToResp(), nil
	}

	projectInfo, err := e.bdl.GetProject(req.Extra.ProjectID)
	if err != nil {
		return apierrors.ErrCreateRuntime.InternalError(err).ToResp(), nil
	}
	clusterName, ok := projectInfo.ClusterConfig[req.Extra.Workspace]
	if !ok {
		return apierrors.ErrCreateRuntime.InternalError(errors.New("cluster not found")).ToResp(), nil
	}
	req.ClusterName = clusterName

	data, err := e.runtime.Create(user.ID(req.Operator), &req)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

// CreateRuntimeByRelease 通过releaseId创建runtime
func (e *Endpoints) CreateRuntimeByRelease(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	operator, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateRuntime.NotLogin().ToResp(), nil
	}
	orgid, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrCreateRuntime.InvalidParameter(err).ToResp(), nil
	}
	var req apistructs.RuntimeReleaseCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		// param problem
		return apierrors.ErrCreateRuntime.InvalidParameter("req body").ToResp(), nil
	}

	data, err := e.runtime.CreateByReleaseIDPipeline(ctx, orgid, operator, &req)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

func (e *Endpoints) CreateRuntimeByReleaseAction(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	operator, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateRuntime.NotLogin().ToResp(), nil
	}
	var req apistructs.RuntimeReleaseCreateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		// param problem
		return apierrors.ErrCreateRuntime.InvalidParameter("req body").ToResp(), nil
	}
	data, err := e.runtime.CreateByReleaseID(ctx, operator, &req)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

// DeleteRuntime 删除应用实例
func (e *Endpoints) DeleteRuntime(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrDeleteRuntime.InvalidParameter(err).ToResp(), nil
	}
	operator, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDeleteRuntime.NotLogin().ToResp(), nil
	}
	v := vars["runtimeID"]
	runtimeID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrDeleteRuntime.InvalidParameter(strutil.Concat("runtimeID: ", v)).ToResp(), nil
	}
	logrus.Debugf("deleting runtime %d, operator %s", runtimeID, operator)
	runtimedto, err := e.runtime.Delete(operator, orgID, runtimeID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(runtimedto)
}

// StopRuntime 停止应用实例
func (e *Endpoints) StopRuntime(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrDeployRuntime.InvalidParameter(err).ToResp(), nil
	}
	operator, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDeployRuntime.NotLogin().ToResp(), nil
	}
	v := vars["runtimeID"]
	runtimeID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrDeployRuntime.InvalidParameter("runtimeID: " + v).ToResp(), nil
	}
	// TODO: the response should be apistructs.Runtime
	data, err := e.runtime.Redeploy(operator, orgID, runtimeID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

// StartRuntime 启动应用实例
func (e *Endpoints) StartRuntime(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	return nil, nil
}

// RestartRuntime 重启应用实例
func (e *Endpoints) RestartRuntime(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	return nil, nil
}

// RedeployRuntime 重新部署, 给 action 调用的 endpoint
func (e *Endpoints) RedeployRuntimeAction(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrDeployRuntime.InvalidParameter(err).ToResp(), nil
	}
	operator, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDeployRuntime.NotLogin().ToResp(), nil
	}
	v := vars["runtimeID"]
	runtimeID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrDeployRuntime.InvalidParameter("runtimeID: " + v).ToResp(), nil
	}
	// TODO: the response should be apistructs.Runtime
	data, err := e.runtime.Redeploy(operator, orgID, runtimeID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

// RedeployRuntime 重新部署
func (e *Endpoints) RedeployRuntime(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrDeployRuntime.InvalidParameter(err).ToResp(), nil
	}
	operator, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDeployRuntime.NotLogin().ToResp(), nil
	}
	v := vars["runtimeID"]
	runtimeID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrDeployRuntime.InvalidParameter("runtimeID: " + v).ToResp(), nil
	}
	data, err := e.runtime.RedeployPipeline(ctx, operator, orgID, runtimeID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

func (e *Endpoints) RollbackRuntimeAction(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrRollbackRuntime.InvalidParameter(err).ToResp(), nil
	}
	operator, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrRollbackRuntime.NotLogin().ToResp(), nil
	}
	var req apistructs.RuntimeRollbackRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		// param problem
		return apierrors.ErrRollbackRuntime.InvalidParameter(err).ToResp(), nil
	}
	if req.DeploymentID <= 0 {
		return apierrors.ErrRollbackRuntime.InvalidParameter("deploymentID").ToResp(), nil
	}
	v := vars["runtimeID"]
	runtimeID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrRollbackRuntime.InvalidParameter(strutil.Concat("runtimeID: ", v)).ToResp(), nil
	}
	data, err := e.runtime.Rollback(operator, orgID, runtimeID, req.DeploymentID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

// Rollback 回滚应用实例
func (e *Endpoints) RollbackRuntime(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrRollbackRuntime.InvalidParameter(err).ToResp(), nil
	}
	operator, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrRollbackRuntime.NotLogin().ToResp(), nil
	}
	var req apistructs.RuntimeRollbackRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		// param problem
		return apierrors.ErrRollbackRuntime.InvalidParameter(err).ToResp(), nil
	}
	if req.DeploymentID <= 0 {
		return apierrors.ErrRollbackRuntime.InvalidParameter("deploymentID").ToResp(), nil
	}
	v := vars["runtimeID"]
	runtimeID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrRollbackRuntime.InvalidParameter(strutil.Concat("runtimeID: ", v)).ToResp(), nil
	}
	data, err := e.runtime.RollbackPipeline(ctx, operator, orgID, runtimeID, req.DeploymentID)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

// ListRuntimes 查询应用实例列表
func (e *Endpoints) ListRuntimes(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(err).ToResp(), nil
	}
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetRuntime.NotLogin().ToResp(), nil
	}
	v := r.URL.Query().Get("applicationId")
	if v == "" {
		v = r.URL.Query().Get("applicationID")
	}
	appID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return apierrors.ErrListRuntime.InvalidParameter(strutil.Concat("applicationID: ", v)).ToResp(), nil
	}
	workspace := r.URL.Query().Get("workspace")
	name := r.URL.Query().Get("name")
	data, err := e.runtime.List(userID, orgID, appID, workspace, name)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	userIDs := make([]string, 0, len(data))
	for i := range data {
		userIDs = append(userIDs, data[i].LastOperator)
	}
	return httpserver.OkResp(data, userIDs)
}

// CountPRByWorkspace count runtimes by app id or project id.
func (e *Endpoints) CountPRByWorkspace(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		l          = logrus.WithField("func", "*Endpoints.CountPRByWorkspace")
		projectId  uint64
		resp       = make(map[string]uint64)
		defaultEnv = []string{"STAGING", "DEV", "PROD", "TEST"}
	)
	userId, err := user.GetUserID(r)
	if err != nil {
		logrus.Errorf("failed to get user id ,err :%v", err)
		return nil, err
	}
	orgId, err := getOrgID(r)
	if err != nil {
		logrus.Errorf("failed to get org id ,err :%v", err)
		return nil, err
	}
	projectIdStr := r.URL.Query().Get("projectId")
	logrus.Infof("user id %s", userId)
	logrus.Infof("org id %d", orgId)
	if projectIdStr == "" {
		return apierrors.ErrGetRuntime.InvalidParameter("projectId").ToResp(), nil
	}
	projectId, err = strconv.ParseUint(projectIdStr, 10, 64)
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter("projectId").ToResp(), nil
	}
	appIdStr := r.URL.Query().Get("appId")
	envParam := r.URL.Query()["workspace"]
	if appIdStr != "" {
		appId, err := strconv.ParseUint(appIdStr, 10, 64)
		if err != nil {
			return apierrors.ErrGetRuntime.InvalidParameter("appId").ToResp(), nil
		}
		if len(envParam) == 0 || envParam[0] == "" {
			for i := 0; i < len(defaultEnv); i++ {
				cnt, err := e.runtime.CountARByWorkspace(appId, defaultEnv[i])
				if err != nil {
					l.WithError(err).Warnf("count runtimes of workspace %s failed", defaultEnv[i])
				}
				resp[defaultEnv[i]] = cnt
			}
		} else {
			env := envParam[0]
			cnt, err := e.runtime.CountARByWorkspace(appId, env)
			if err != nil {
				l.WithError(err).Warnf("count runtimes of workspace %s failed", env)
			}
			resp[env] = cnt
		}
	} else {
		apps, err := e.bdl.GetMyApps(string(userId), orgId)
		if err != nil {
			logrus.Errorf("get my app failed,%v", err)
			return nil, err
		}
		appIdMap := make(map[uint64]bool)
		for i := 0; i < len(apps.List); i++ {
			if apps.List[i].ProjectID == projectId {
				appIdMap[apps.List[i].ID] = true
			}
		}
		if len(envParam) == 0 || envParam[0] == "" {
			for i := 0; i < len(defaultEnv); i++ {
				for aid := range appIdMap {
					cnt, err := e.runtime.CountARByWorkspace(aid, defaultEnv[i])
					if err != nil {
						l.WithError(err).Warnf("count runtimes of workspace %s failed", defaultEnv[i])
					}
					resp[defaultEnv[i]] += cnt
				}
			}
		} else {
			env := envParam[0]
			for aid := range appIdMap {
				cnt, err := e.runtime.CountPRByWorkspace(aid, env)
				if err != nil {
					l.WithError(err).Warnf("count runtimes of workspace %s failed", env)
				}
				resp[env] += cnt
			}
		}
	}
	return httpserver.OkResp(resp)
}

// ListRuntimesGroupByApps responses the runtimes for the given apps.
func (e *Endpoints) ListRuntimesGroupByApps(ctx context.Context, r *http.Request, _ map[string]string) (httpserver.Responser, error) {
	var (
		l      = logrus.WithField("func", "*Endpoints.ListRuntimesGroupByApps")
		appIDs []uint64
		env    string
	)

	for _, appID := range r.URL.Query()["applicationID"] {
		id, err := strconv.ParseUint(appID, 10, 64)
		if err != nil {
			l.WithError(err).Warnf("failed to parse applicationID: failed to ParseUint: %s", appID)
		}
		appIDs = append(appIDs, id)
	}
	envParam := r.URL.Query()["workspace"]

	if len(envParam) == 0 {
		env = ""
	} else {
		env = envParam[0]
	}
	runtimes, err := e.runtime.ListGroupByApps(appIDs, env)
	if err != nil {
		return apierrors.ErrListRuntime.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(runtimes)
}

// ListMyRuntimes lists the runtimes for which the current user has permissions
func (e *Endpoints) ListMyRuntimes(ctx context.Context, r *http.Request, _ map[string]string) (httpserver.Responser, error) {
	var (
		l          = logrus.WithField("func", "*Endpoints.ListRuntimesGroupByApps")
		appIDs     []uint64
		appID2Name = make(map[uint64]string)
		env        string
	)

	userId, err := user.GetUserID(r)
	if err != nil {
		l.Errorf("failed to get user id ,err :%v", err)
		return nil, apierrors.ErrListRuntime.NotLogin()
	}
	orgId, err := getOrgID(r)
	if err != nil {
		l.Errorf("failed to get org id ,err :%v", err)
		return nil, apierrors.ErrListRuntime.NotLogin()
	}

	projectIDStr := r.URL.Query().Get("projectID")
	if projectIDStr == "" {
		return nil, apierrors.ErrListRuntime.MissingParameter("projectID")
	}
	projectID, err := strconv.ParseUint(projectIDStr, 10, 64)
	if err != nil {
		return nil, apierrors.ErrListRuntime.InvalidParameter("projectID")
	}

	appIDStrs := r.URL.Query()["appID"]
	for _, str := range appIDStrs {
		id, err := strconv.ParseUint(str, 10, 64)
		if err != nil {
			return nil, apierrors.ErrListRuntime.InvalidState("appID")
		}
		appIDs = append(appIDs, id)
	}

	envParam := r.URL.Query()["workspace"]

	if len(envParam) == 0 {
		env = ""
	} else {
		env = envParam[0]
	}

	var myAppIDs []uint64
	myApps, err := e.bdl.GetMyAppsByProject(string(userId), orgId, projectID, "")
	for i := range myApps.List {
		myAppIDs = append(myAppIDs, myApps.List[i].ID)
		appID2Name[myApps.List[i].ID] = myApps.List[i].Name
	}

	var targetAppIDs []uint64
	if len(appIDs) == 0 {
		targetAppIDs = myAppIDs
	} else {
		for _, id := range appIDs {
			if _, ok := appID2Name[id]; ok {
				targetAppIDs = append(targetAppIDs, id)
			}
		}
	}

	runtimes, err := e.runtime.ListGroupByApps(targetAppIDs, env)
	if err != nil {
		return apierrors.ErrListRuntime.InternalError(err).ToResp(), nil
	}

	var res []*apistructs.RuntimeSummaryDTO
	for _, sli := range runtimes {
		for i := range sli {
			sli[i].ApplicationName = appID2Name[sli[i].ApplicationID]
			res = append(res, sli[i])
		}
	}
	return httpserver.OkResp(res)
}

// BatchRuntimeServices responses the runtimes for the given apps.
func (e *Endpoints) BatchRuntimeServices(ctx context.Context, r *http.Request, _ map[string]string) (httpserver.Responser, error) {
	var (
		l          = logrus.WithField("func", "*Endpoints.BatchRuntimeServices")
		runtimeIDs []uint64
	)

	for _, runtimeID := range r.URL.Query()["runtimeID"] {
		id, err := strconv.ParseUint(runtimeID, 10, 64)
		if err != nil {
			l.WithError(err).Warnf("failed to parse applicationID: failed to ParseUint: %s", runtimeID)
		}
		runtimeIDs = append(runtimeIDs, id)
	}

	serviceMap, err := e.runtime.GetServiceByRuntime(runtimeIDs)
	if err != nil {
		return apierrors.ErrListRuntime.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(serviceMap)
}

// GetRuntime 查询应用实例
func (e *Endpoints) GetRuntime(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(err).ToResp(), nil
	}
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetRuntime.NotLogin().ToResp(), nil
	}
	var (
		idOrName  = vars["idOrName"]
		appID     = r.URL.Query().Get("applicationId")
		workspace = r.URL.Query().Get("workspace")
	)
	data, err := e.runtime.Get(ctx, userID, orgID, idOrName, appID, workspace)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

func (e *Endpoints) GetRuntimeSpec(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(err).ToResp(), nil
	}
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetRuntime.NotLogin().ToResp(), nil
	}
	v := vars["runtimeID"]
	runtimeID, err := strutil.Atoi64(v)
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(strutil.Concat("runtimeID: ", v)).ToResp(), nil
	}
	data, err := e.runtime.GetSpec(userID, orgID, uint64(runtimeID))
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(data)
}

// FullGC 触发全量 GC
func (e *Endpoints) FullGC(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	go e.runtime.FullGC()
	return httpserver.OkResp(nil)
}

func getOrgID(r *http.Request) (uint64, error) {
	v := r.Header.Get("Org-ID")
	if v == "" {
		return 0, fmt.Errorf("Org-ID")
	}
	orgID, err := strconv.ParseUint(v, 10, 64)
	if err != nil {
		return 0, err
	}
	return orgID, nil
}

// ReferCluster 查看 runtime & addon 是否有使用集群
func (e *Endpoints) ReferCluster(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrReferRuntime.NotLogin().ToResp(), nil
	}

	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrReferRuntime.InvalidParameter(err).ToResp(), nil
	}

	clusterName := r.URL.Query().Get("cluster")
	referred := e.runtime.ReferCluster(clusterName, orgID)

	return httpserver.OkResp(referred)
}

// runtimeLogs 包装runtime部署日志
func (e *Endpoints) RuntimeLogs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(err).ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateAddon.NotLogin().ToResp(), nil
	}
	source := r.URL.Query().Get("source")
	if source == "" {
		return apierrors.ErrGetRuntime.MissingParameter("source").ToResp(), nil
	}
	id := r.URL.Query().Get("id")
	if id == "" {
		return apierrors.ErrGetRuntime.MissingParameter("id").ToResp(), nil
	}
	deploymentID, err := strconv.ParseUint(id, 10, 64)
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(strutil.Concat("deploymentID: ", id)).ToResp(), nil
	}
	result, err := e.runtime.RuntimeDeployLogs(userID, orgID, r.Header.Get("org"), deploymentID, r.URL.Query())
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(strutil.Concat("deploymentID: ", id)).ToResp(), nil
	}
	return httpserver.OkResp(result)

}

// OrgcenterJobLogs 包装数据中心--->任务列表 日志
func (e *Endpoints) OrgcenterJobLogs(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	orgID, err := getOrgID(r)
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(err).ToResp(), nil
	}
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateAddon.NotLogin().ToResp(), nil
	}
	jobID := r.URL.Query().Get("id")
	if jobID == "" {
		return apierrors.ErrGetRuntime.MissingParameter("id").ToResp(), nil
	}
	clusterName := r.URL.Query().Get("clusterName")
	if clusterName == "" {
		return apierrors.ErrGetRuntime.MissingParameter("clusterName").ToResp(), nil
	}
	result, err := e.runtime.OrgJobLogs(ctx, userID, orgID, r.Header.Get("org"), jobID, clusterName, r.URL.Query())
	if err != nil {
		return apierrors.ErrGetRuntime.InvalidParameter(strutil.Concat("jobID: ", jobID)).ToResp(), nil
	}
	return httpserver.OkResp(result)
}

// GetAppWorkspaceReleases 获取应用某一环境下可部署的 releases
func (e *Endpoints) GetAppWorkspaceReleases(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetAppWorkspaceReleases.NotLogin().ToResp(), nil
	}

	// parse query params
	var req apistructs.AppWorkspaceReleasesGetRequest
	if err := queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrGetAppWorkspaceReleases.InvalidParameter(err).ToResp(), nil
	}
	if !req.Workspace.Deployable() {
		return apierrors.ErrGetAppWorkspaceReleases.InvalidParameter(errors.Errorf("invalid workspace: %s", req.Workspace)).ToResp(), nil
	}

	// check app permission
	access, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.AppScope,
		ScopeID:  req.AppID,
		Resource: apistructs.AppResource,
		Action:   apistructs.GetAction,
	})
	if err != nil || !access.Access {
		return apierrors.ErrGetAppWorkspaceReleases.AccessDenied().ToResp(), nil
	}

	// get workspace cluster
	app, err := e.bdl.GetApp(req.AppID)
	if err != nil {
		return apierrors.ErrGetAppWorkspaceReleases.InvalidParameter(err).ToResp(), nil
	}
	var clusterName string
	for _, workspace := range app.Workspaces {
		if workspace.Workspace == req.Workspace.String() {
			clusterName = workspace.ClusterName
			break
		}
	}
	if clusterName == "" {
		return apierrors.ErrGetAppWorkspaceReleases.InternalError(errors.Errorf("not found cluster for %s", req.Workspace)).ToResp(), nil
	}

	branches, err := e.bdl.GetAllValidBranchWorkspace(req.AppID, string(userID))
	if err != nil {
		return apierrors.ErrGetAppWorkspaceReleases.InternalError(err).ToResp(), nil
	}

	// 返回当前环境所有 branch 可部署的 release 列表
	result := make(apistructs.AppWorkspaceReleasesGetResponseData)
	for _, branch := range branches {
		matchArtifactWorkspace := false
		for _, artifactWorkspace := range strutil.Split(branch.ArtifactWorkspace, ",", true) {
			if artifactWorkspace == req.Workspace.String() {
				matchArtifactWorkspace = true
				break
			}
		}
		if !matchArtifactWorkspace {
			continue
		}
		ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
		releases, err := e.releaseSvc.ListRelease(ctx, &pb.ReleaseListRequest{
			Branch:                       branch.Name,
			CrossClusterOrSpecifyCluster: clusterName,
			ApplicationID:                []string{strconv.FormatUint(req.AppID, 10)},
			PageSize:                     5,
			PageNo:                       1,
		})
		if err != nil {
			return apierrors.ErrGetAppWorkspaceReleases.InternalError(err).ToResp(), nil
		}
		result[branch.Name] = releases.Data
	}

	return httpserver.OkResp(result)
}

func (e *Endpoints) KillPod(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var req apistructs.RuntimeKillPodRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrKillPod.InvalidParameter("req body").ToResp(), nil
	}
	err := e.runtime.KillPod(req.RuntimeID, req.PodName)
	if err != nil {
		return apierrors.ErrKillPod.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(nil)
}
