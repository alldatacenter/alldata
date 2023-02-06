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

package runtimeapis

import (
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/tools/monitor/common"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	// metric for project view
	checkProject := permission.QueryValue("filter_project_id")
	routes.GET("/api/project/metrics/:scope", p.metricq.HandleV1, permission.Intercepter(
		permission.ScopeProject, checkProject,
		common.ResourceProject, permission.ActionGet,
	))
	routes.GET("/api/project/metrics/:scope/:aggregate", p.metricq.HandleV1, permission.Intercepter(
		permission.ScopeProject, checkProject,
		common.ResourceProject, permission.ActionGet,
	))
	routes.GET("/api/project/metrics/query", p.metricq.Handle, permission.Intercepter(
		permission.ScopeProject, checkProject,
		common.ResourceProject, permission.ActionGet,
	))
	routes.POST("/api/project/metrics/query", p.metricq.Handle, permission.Intercepter(
		permission.ScopeProject, checkProject,
		common.ResourceProject, permission.ActionGet,
	))
	return nil
}
