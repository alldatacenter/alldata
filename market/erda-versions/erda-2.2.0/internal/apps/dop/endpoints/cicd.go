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
	"fmt"
	"net/http"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// CICDTaskLog 包装 cicd task 获取接口
// dashboard: /api/logs?start=0&end=1576498555732000000&count=-200&stream=stderr&id=pipeline-task-2059&source=job
func (e *Endpoints) CICDTaskLog(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {

	_, task, err := e.checkTaskPermission(r, vars)
	if err != nil {
		return apierrors.ErrGetCICDTaskLog.InternalError(err).ToResp(), nil
	}

	// 获取日志
	var logReq apistructs.DashboardSpotLogRequest
	if err := queryStringDecoder.Decode(&logReq, r.URL.Query()); err != nil {
		return apierrors.ErrGetCICDTaskLog.InvalidParameter(err).ToResp(), nil
	}
	logID := task.Extra.UUID
	if logReq.ID != "" {
		var exist bool
		for _, container := range task.Extra.TaskContainers {
			if container.ContainerID == logReq.ID {
				exist = true
			}
		}
		if !exist {
			return apierrors.ErrGetCICDTaskLog.InvalidParameter(
				fmt.Errorf("container: %s don't exist", logReq.ID),
			).ToResp(), nil
		}
		logID = logReq.ID
	}
	logReq.ID = logID
	logReq.Source = apistructs.DashboardSpotLogSourceJob

	debug, ok := vars["debug"]
	if ok {
		b, _ := strconv.ParseBool(debug)
		logReq.Debug = b
	}

	log, err := e.bdl.GetLog(r.Header.Get("org"), logReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	return httpserver.OkResp(log)
}

// ProxyCICDTaskLogDownload 包装 cicd task 下载接口
func (e *Endpoints) ProxyCICDTaskLogDownload(ctx context.Context, r *http.Request, vars map[string]string) error {
	_, task, err := e.checkTaskPermission(r, vars)
	if err != nil {
		return apierrors.ErrDownloadCICDTaskLog.InternalError(err)
	}

	var logReq apistructs.DashboardSpotLogRequest
	if err := queryStringDecoder.Decode(&logReq, r.URL.Query()); err != nil {
		return apierrors.ErrDownloadCICDTaskLog.InvalidParameter(err)
	}

	logID := task.Extra.UUID
	if logReq.ID != "" {
		var exist bool
		for _, container := range task.Extra.TaskContainers {
			if container.ContainerID == logReq.ID {
				exist = true
			}
		}
		if !exist {
			return apierrors.ErrDownloadCICDTaskLog.InvalidParameter(
				fmt.Errorf("container: %s don't exist", logReq.ID),
			)
		}
		logID = logReq.ID
	}

	// proxy
	r.URL.Scheme = "http"
	r.Host = discover.Monitor()
	r.URL.Host = discover.Monitor()
	r.URL.Path = "/api/logs/actions/download"
	q := r.URL.Query()
	q.Add("source", string(apistructs.DashboardSpotLogSourceJob))
	q.Add("id", logID)
	q.Add("start", strconv.FormatInt(int64(logReq.Start), 10))
	q.Add("end", strconv.FormatInt(int64(logReq.End), 10))
	q.Add("stream", string(logReq.Stream))
	r.URL.RawQuery = q.Encode()

	return nil
}

func (e *Endpoints) checkTaskPermission(r *http.Request, vars map[string]string) (
	*apistructs.PipelineDetailDTO, *apistructs.PipelineTaskDTO, error) {

	pipelineIDStr := vars["pipelineID"]
	pipelineID, err := strconv.ParseUint(pipelineIDStr, 10, 64)
	if err != nil {
		return nil, nil, errors.Errorf("pipelineID: %s", pipelineIDStr)
	}
	taskIDStr := vars["taskID"]
	taskID, err := strconv.ParseUint(taskIDStr, 10, 64)
	if err != nil {
		return nil, nil, errors.Errorf("taskID: %s", taskIDStr)
	}

	p, err := e.bdl.GetPipeline(pipelineID)
	if err != nil {
		return nil, nil, err
	}
	task, err := e.bdl.GetPipelineTask(pipelineID, taskID)
	if err != nil {
		return nil, nil, err
	}
	if task.PipelineID != p.ID {
		return nil, nil, errors.Errorf("task not belong to pipeline")
	}

	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return nil, nil, err
	}
	if !identityInfo.IsInternalClient() {
		checkResp, err := e.bdl.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.AppScope,
			ScopeID:  p.ApplicationID,
			Resource: apistructs.PipelineResource,
			Action:   apistructs.ReadAction,
		})
		if err != nil {
			return nil, nil, err
		}
		if !checkResp.Access {
			return nil, nil, apierrors.ErrGetCICDTaskLog.AccessDenied()
		}
	}

	return p, task, nil
}
