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

package orgapis

import (
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/tools/monitor/common"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
)

func (p *provider) intRoutes(routes httpserver.Router) error {
	// metric for org center
	routes.GET("/api/orgCenter/metrics/:scope", p.metricq.HandleV1, permission.Intercepter(
		permission.ScopeOrg, p.checkOrgMetrics,
		common.ResourceOrgCenter, permission.ActionGet,
	))
	routes.GET("/api/orgCenter/metrics/:scope/:aggregate", p.metricq.HandleV1, permission.Intercepter(
		permission.ScopeOrg, p.checkOrgMetrics,
		common.ResourceOrgCenter, permission.ActionGet,
	))
	routes.GET("/api/orgCenter/metrics/query", p.metricq.Handle, permission.Intercepter(
		permission.ScopeOrg, p.checkOrgMetrics,
		common.ResourceOrgCenter, permission.ActionGet,
	))
	routes.POST("/api/orgCenter/metrics/query", p.metricq.Handle, permission.Intercepter(
		permission.ScopeOrg, p.checkOrgMetrics,
		common.ResourceOrgCenter, permission.ActionGet,
	))

	// clusters resources for org center
	checkOrgName := permission.OrgIDByOrgName("orgName")
	routes.GET("/api/resources/types", p.getHostTypes, permission.Intercepter(
		permission.ScopeOrg, checkOrgName,
		common.ResourceOrgCenter, permission.ActionList,
	))
	routes.POST("/api/resources/group", p.getGroupHosts, permission.Intercepter(
		permission.ScopeOrg, checkOrgName,
		common.ResourceOrgCenter, permission.ActionList,
	))
	routes.POST("/api/resources/containers/:instance_type", p.getContainers, permission.Intercepter(
		permission.ScopeOrg, permission.OrgIDFromHeader(),
		common.ResourceOrgCenter, permission.ActionList,
	))
	routes.POST("/api/resources/containers/group/allocation/:metric_type", p.groupContainerAllocation, permission.Intercepter(
		permission.ScopeOrg, checkOrgName,
		common.ResourceOrgCenter, permission.ActionList,
	))
	routes.POST("/api/resources/containers/group/count", p.groupContainerCount, permission.Intercepter(
		permission.ScopeOrg, checkOrgName,
		common.ResourceOrgCenter, permission.ActionList,
	))
	routes.POST("/api/resources/host-status", p.getHostStatus, permission.Intercepter(
		permission.ScopeOrg, p.getOrgIDNameFromBody,
		common.ResourceOrgCenter, permission.ActionList,
	))
	routes.POST("/api/resources/hosts/actions/offline", p.offlineHost)
	routes.GET("/api/org/clusters/status", p.clusterStatus, permission.Intercepter(
		permission.ScopeOrg, permission.OrgIDFromHeader(),
		common.ResourceOrgCenter, permission.ActionList,
	))
	return nil
}
