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

package monitorcollector

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
)

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceMonitorCollector
}

func (p *provider) BuildServiceGroupRequest(resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{} {
	req := p.DefaultDeployHandler.BuildServiceGroupRequest(resourceInfo, tmcInstance, clusterConfig).(*apistructs.ServiceGroupCreateV2Request)

	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	for _, service := range resourceInfo.Dice.Services {
		nodeId := utils.GetRandomId()

		// envs
		env := map[string]string{
			"ADDON_ID":          tmcInstance.ID,
			"ADDON_NODE_ID":     nodeId,
			"BOOTSTRAP_SERVERS": instanceOptions["KAFKA_HOSTS"],
			"ZOOKEEPER_ADDR":    instanceOptions["ZK_HOSTS"],
		}
		utils.AppendMap(service.Envs, env)
		if service.Labels == nil {
			service.Labels = make(map[string]string)
		}
		utils.SetlabelsFromOptions(instanceOptions, service.Labels)
	}

	return req
}

func (p *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{},
	clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {
	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	var vip string
	serviceGroup := serviceGroupDeployResult.(*apistructs.ServiceGroup)
	for _, service := range serviceGroup.Services {
		if service.Name == "monitor-collector" {
			vip = service.Vip
			break
		}
	}

	config := map[string]string{
		"BOOTSTRAP_SERVERS":     instanceOptions["KAFKA_HOSTS"],
		"ZOOKEEPER_ADDR":        instanceOptions["ZK_HOSTS"],
		"MONITOR_LOG_COLLECTOR": "http://" + vip + ":7076/collect/logs/container",
	}

	return config
}
