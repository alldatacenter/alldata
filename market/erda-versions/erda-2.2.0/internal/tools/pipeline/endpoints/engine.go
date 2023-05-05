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

	"github.com/erda-project/erda/internal/tools/pipeline/pipengine/actionexecutor"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (e *Endpoints) reloadActionExecutorConfig(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {

	_, cfgChan, err := e.dbClient.ListPipelineConfigsOfActionExecutor()
	if err != nil {
		return httpserver.ErrResp(http.StatusInternalServerError, "RELOAD PIPENGINE CONFIG: LIST", err.Error())
	}

	if err := actionexecutor.GetManager().Initialize(ctx, cfgChan, e.clusterInfo); err != nil {
		return httpserver.ErrResp(http.StatusInternalServerError, "RELOAD PIPENGINE CONFIG: RELOAD", err.Error())
	}
	return httpserver.OkResp(nil)
}
