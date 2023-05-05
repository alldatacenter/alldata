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

	"github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

//Health Component health check interface
func (e *Endpoints) Health(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	res := &apistructs.HealthResponse{
		Name:   "dop",
		Status: apistructs.HealthStatusFail,
		Tags: map[string]string{
			"pod_name": "dop",
			"version":  version.Version,
		},
	}
	res.Modules = make([]apistructs.Module, 0)
	err := e.db.DB.DB().Ping()
	if err != nil {
		sql := apistructs.Module{
			Name:    "mysql-connection",
			Status:  apistructs.HealthStatusFail,
			Message: err.Error(),
		}
		res.Modules = append(res.Modules, sql)
		return httpserver.OkResp(res)
	}

	res.Modules = append(res.Modules, apistructs.Module{
		Name:    "mysql-connection",
		Status:  apistructs.HealthStatusOk,
		Message: "connected",
	})
	res.Status = apistructs.HealthStatusOk
	return httpserver.HTTPResponse{
		Status:  http.StatusOK,
		Content: res,
	}, nil
}
