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

package servicemesh

import (
	"fmt"

	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
)

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceServiceMesh
}

func (p *provider) DoPostDeployJob(tmcInstance *db.Instance, serviceGroupDeployResult interface{}, clusterConfig map[string]string) (map[string]string, error) {
	if clusterConfig["ISTIO_INSTALLED"] != "true" {
		return nil, fmt.Errorf("istio not installed")
	}
	if clusterConfig["ISTIO_VERSION"] == "1.1.4" {
		return nil, fmt.Errorf("istio need upgrade")
	}

	return nil, nil
}

func (p *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{},
	clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {

	config := map[string]string{
		"ADDON_SERVICE_MESH": "on",
	}

	return config
}

func (p *provider) DoApplyTmcInstanceTenant(req *handlers.ResourceDeployRequest, resourceInfo *handlers.ResourceInfo,
	tmcInstance *db.Instance, tenant *db.InstanceTenant, clusterConfig map[string]string) (map[string]string, error) {

	key, _ := p.TmcIniDb.GetMicroServiceEngineJumpKey(handlers.ResourceMonitor)

	tk := ""
	var monitorTenant, _ = p.TenantDb.GetByEngineAndTenantGroup(handlers.ResourceMonitor, tenant.TenantGroup)
	if monitorTenant != nil {
		monitorConfig := map[string]string{}
		utils.JsonConvertObjToType(monitorTenant.Config, &monitorConfig)
		tk = monitorConfig["TERMINUS_KEY"]
	}

	console := map[string]string{
		"tenantGroup": tenant.TenantGroup,
		"tenantId":    tenant.ID,
		"key":         key,
		"terminusKey": tk,
	}

	str, _ := utils.JsonConvertObjToString(console)
	config := map[string]string{
		"PUBLIC_HOST": str,
	}

	return config, nil
}
