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

package details_apis

import (
	"net/http"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/tools/monitor/common"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	// metric for alert
	routes.GET("/api/alert/metrics/:scope", p.metricq.HandleV1, permission.Intercepter(
		permission.ScopeOrg, p.getOrgIDByClusters,
		common.ResourceOrgCenter, permission.ActionGet,
	))
	routes.GET("/api/alert/metrics/:scope/:aggregate", p.metricq.HandleV1, permission.Intercepter(
		permission.ScopeOrg, p.getOrgIDByClusters,
		common.ResourceOrgCenter, permission.ActionGet,
	))

	// metrics for system
	routes.GET("/api/system/addon/metrics/:scope/:aggregate", p.systemAddonMetrics, permission.Intercepter(
		permission.ScopeOrg, permission.OrgIDByCluster("filter_cluster_name"),
		common.ResourceOrgCenter, permission.ActionGet,
	))
	return nil
}

func (p *provider) systemAddonMetrics(r *http.Request, params *struct {
	metricq.QueryParams
	AddonID string `query:"filter_addon_id" validate:"required"`
}) interface{} {
	return p.metricq.HandleV1(r, &params.QueryParams)
}
