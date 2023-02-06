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

package zkproxy

import (
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
)

func (h *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceZKProxy
}

func (h *provider) BuildServiceGroupRequest(resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{} {
	req := h.DefaultDeployHandler.BuildServiceGroupRequest(resourceInfo, tmcInstance, clusterConfig).(*apistructs.ServiceGroupCreateV2Request)

	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	for name, service := range resourceInfo.Dice.Services {
		env := map[string]string{
			"ADDON_ID":       tmcInstance.ID,
			"ADDON_NODE_ID":  tmcInstance.ID + "_" + name,
			"ETCD_ENDPOINTS": instanceOptions["ETCD_ADDRESS"],
		}
		utils.AppendMap(service.Envs, env)
		if service.Labels == nil {
			service.Labels = make(map[string]string)
		}
		utils.SetlabelsFromOptions(instanceOptions, service.Labels)
	}

	return req
}

func (h *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{},
	clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {
	var vip string
	serviceGroup := serviceGroupDeployResult.(*apistructs.ServiceGroup)
	for _, service := range serviceGroup.Services {
		if service.Name == "zkproxy-1" {
			vip = service.Vip
			break
		}
	}

	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	config := map[string]string{
		"ELASTICJOB_HOST":     instanceOptions["ZOOKEEPER_ADDRESS"],
		"ZKPROXY_PUBLIC_HOST": "http://" + vip + ":8080",
		"ZOOKEEPER_DUBBO":     vip + ":2181",
		"ZKPROXY_HOST":        vip,
		"ZKPROXY_PORT":        "2181",
		"ETCD_ADDRESS":        instanceOptions["ETCD_ADDRESS"],
	}

	return config
}

func (h *provider) DoApplyTmcInstanceTenant(req *handlers.ResourceDeployRequest, resourceInfo *handlers.ResourceInfo,
	tmcInstance *db.Instance, tenant *db.InstanceTenant, clusterConfig map[string]string) (map[string]string, error) {
	tenantOptions := map[string]string{}
	utils.JsonConvertObjToType(tenant.Options, &tenantOptions)

	config := map[string]string{
		"DUBBO_TENANT_ID": tenantOptions["projectId"] + "_" + strings.ToLower(tenantOptions["env"]),
	}

	return config, nil
}
