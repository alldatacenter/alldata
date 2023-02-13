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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (e *Endpoints) clusterHook(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	req := apistructs.ClusterEvent{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errStr := fmt.Sprintf("failed to decode clusterhook request, err: %v", err)
		logrus.Error(errStr)
		return httpserver.ErrResp(http.StatusBadRequest, "", errStr)
	}
	if err := e.pipelineSvc.ClusterHook(req); err != nil {
		errStr := fmt.Sprintf("failed to handle cluster event, err: %v", err)
		logrus.Error(errStr)
		return httpserver.ErrResp(http.StatusBadRequest, "", errStr)
	}
	return httpserver.OkResp(nil)
}

func (e *Endpoints) executorInfos(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	return httpserver.OkResp(actionexecutor.GetExecutorInfo())
}

func (e *Endpoints) triggerRefreshExecutors(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	if err := e.clusterInfo.BatchUpdateAndDispatchRefresh(); err != nil {
		return httpserver.ErrResp(http.StatusInternalServerError, "", err.Error())
	}
	return httpserver.OkResp("trigger refresh executors successfully")
}
