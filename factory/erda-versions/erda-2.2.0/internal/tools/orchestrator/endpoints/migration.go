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
	"net/http"

	"github.com/gorilla/schema"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/apierrors"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
)

// MigrationLog migration log接口
func (e *Endpoints) MigrationLog(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	//鉴权
	_, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrCreateAddon.NotLogin().ToResp(), nil
	}
	// 获取日志
	var logReq apistructs.DashboardSpotLogRequest
	if err := queryStringDecoder.Decode(&logReq, r.URL.Query()); err != nil {
		return apierrors.ErrMigrationLog.InvalidParameter(err).ToResp(), nil
	}
	logReq.ID = "migration-task-" + vars["migrationId"]
	logReq.Source = apistructs.DashboardSpotLogSourceJob
	// 查询日志信息
	log, err := e.bdl.GetLog(r.Header.Get("org"), logReq)
	if err != nil {
		return errorresp.ErrResp(err)
	}
	if len(log.Lines) == 0 {
		log.Lines = []apistructs.DashboardSpotLogLine{}
	}
	return httpserver.OkResp(log)
}

// 清理migration namespace
func (e *Endpoints) CleanUnusedMigrationNs() (bool, error) {
	return e.migration.CleanUnusedMigrationNs()
}

var queryStringDecoder *schema.Decoder

func init() {
	queryStringDecoder = schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)
}
