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

package apis

import (
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/tools/monitor/common"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
)

// permission resources
const (
	ResourceOrgCenter = "monitor_org_center"
	ResourceRuntime   = "monitor_runtime"
	ResourceProject   = "monitor_project"
	ResourceOrgAlert  = "monitor_org_alert"
	ResourceNotify    = "notify"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	// report task
	checkOrgID := permission.QueryValue("scopeId")
	routes.GET("/api/org/report/tasks", p.listOrgReportTasks, permission.Intercepter(
		permission.ScopeOrg, checkOrgID,
		common.ResourceOrgCenter, permission.ActionList,
	))
	routes.POST("/api/org/report/tasks", p.creatOrgReportTask)
	routes.PUT("/api/org/report/tasks/:id", p.updateOrgReportTask)
	routes.PUT("/api/org/report/tasks/:id/switch", p.switchOrgReportTask)
	routes.GET("/api/org/report/tasks/:id", p.getOrgReportTask)
	routes.DELETE("/api/org/report/tasks/:id", p.delOrgReportTask)
	routes.POST("/api/org/report/tasks/:id/run-now", p.runReportTaskAtOnce) // for test

	// report type
	routes.GET("/api/report/types", p.listReportType)

	// report history
	routes.GET("/api/report/histories", p.listReportHistories)
	routes.POST("/api/report/histories", p.createReportHistory)
	routes.GET("/api/report/histories/:id", p.getReportHistory)
	routes.DELETE("/api/report/histories/:id", p.delReportHistory)
	return nil
}
