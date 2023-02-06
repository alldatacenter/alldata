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

package registercenter

import (
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceRegisterCenter
}

func (p *provider) BuildSubResourceDeployRequest(name string, addon *diceyml.AddOn, req *handlers.ResourceDeployRequest) *handlers.ResourceDeployRequest {
	subReq := p.DefaultDeployHandler.BuildSubResourceDeployRequest(name, addon, req)

	switch name {

	case handlers.ResourceNacos:
		subReq.Options["NACOS_TENANT_ID"] = req.Options["NACOS_TENANT_ID"] // seems always empty?
	case handlers.ResourceZookeeper:
		// do nothing
	case handlers.ResourceZKProxy:
		subReq.Options["projectId"] = req.Options["projectId"]
		subReq.Options["env"] = req.Options["env"]
	}

	return subReq
}

func (p *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{},
	clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {
	config := map[string]string{}
	options := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &options)

	config["NACOS_ADDRESS"] = options["NACOS_ADDRESS"]

	if _, ok := options["ZOOKEEPER_ADDRESS"]; ok {
		config["ZOOKEEPER_ADDRESS"] = options["ZOOKEEPER_ADDRESS"]
		config["ELASTICJOB_HOST"] = options["ZOOKEEPER_ADDRESS"]
	} else {
		config["ELASTICJOB_HOST"] = options["ELASTICJOB_HOST"]
		config["ZKPROXY_PUBLIC_HOST"] = options["ZKPROXY_PUBLIC_HOST"]
		config["ZOOKEEPER_DUBBO"] = options["ZOOKEEPER_DUBBO"]
		config["ETCD_ADDRESS"] = options["ETCD_ADDRESS"]
	}

	return config
}

func (p *provider) DoApplyTmcInstanceTenant(req *handlers.ResourceDeployRequest, resourceInfo *handlers.ResourceInfo,
	tmcInstance *db.Instance, tenant *db.InstanceTenant, clusterConfig map[string]string) (map[string]string, error) {
	resultConfig := map[string]string{}
	tenantOptions := map[string]string{}
	utils.JsonConvertObjToType(tenant.Options, &tenantOptions)
	instanceConfig := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Config, &instanceConfig)

	key, _ := p.TmcIniDb.GetMicroServiceEngineJumpKey(tmcInstance.Engine)

	console := map[string]string{
		"tenantGroup": tenant.TenantGroup,
		"tenantId":    tenant.ID,
		"key":         key,
	}

	dubboTenantId := tenantOptions["DUBBO_TENANT_ID"]
	if len(dubboTenantId) > 0 {
		resultConfig["DUBBO_TENANT_ID"] = dubboTenantId
		console["DUBBO_TENANT_ID"] = dubboTenantId
		console["ZKPROXY_PUBLIC_HOST"] = instanceConfig["ZKPROXY_PUBLIC_HOST"]
	}
	nacosTenantId := tenantOptions["NACOS_TENANT_ID"]
	if len(nacosTenantId) > 0 {
		resultConfig["NACOS_TENANT_ID"] = nacosTenantId
		console["NACOS_TENANT_ID"] = nacosTenantId
		console["NACOS_PUBLIC_HOST"] = instanceConfig["NACOS_ADDRESS"]
	}

	str, _ := utils.JsonConvertObjToString(console)
	resultConfig["PUBLIC_HOST"] = str
	return resultConfig, nil
}
