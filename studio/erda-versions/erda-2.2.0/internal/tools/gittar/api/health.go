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

package api

import (
	"github.com/erda-project/erda-infra/base/version"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
)

const (
	PodName       = "gittar"
	ComponentName = "gittar"
	MysqlName     = "mysql-connection"
)

// Health health check
func Health(ctx *webcontext.Context) {
	status := apistructs.HealthStatusOk
	modules := make([]apistructs.Module, 0)

	// check mysql server
	status = mysqlHealth(ctx, &modules)
	health := apistructs.HealthResponse{
		Name:    ComponentName,
		Status:  status,
		Modules: modules,
		Tags: map[string]string{
			"pod_name": PodName,
			"version":  version.Version,
		},
	}
	ctx.EchoContext.JSON(200, health)
}

// mysqlHealth check mysql health
func mysqlHealth(ctx *webcontext.Context, modules *[]apistructs.Module) (status apistructs.HealthStatus) {
	if err := ctx.DBClient.Ping(); err != nil {
		status = apistructs.HealthStatusFail
		*modules = append(*modules, apistructs.Module{
			Name:    MysqlName,
			Status:  status,
			Message: err.Error(),
		})
	} else {
		status = apistructs.HealthStatusOk
		*modules = append(*modules, apistructs.Module{
			Name:    MysqlName,
			Status:  status,
			Message: "connected",
		})
	}
	return
}
