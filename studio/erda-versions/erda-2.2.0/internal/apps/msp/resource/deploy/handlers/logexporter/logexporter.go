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

package logexporter

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
)

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceLogExporter
}

func (p *provider) BuildServiceGroupRequest(resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{} {
	req := p.DefaultDeployHandler.BuildServiceGroupRequest(resourceInfo, tmcInstance, clusterConfig).(*apistructs.ServiceGroupCreateV2Request)

	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	for _, service := range resourceInfo.Dice.Services {
		nodeId := utils.GetRandomId()

		// envs
		env := map[string]string{
			"ADDON_ID":                  tmcInstance.ID,
			"ADDON_NODE_ID":             nodeId,
			"BOOTSTRAP_SERVERS":         instanceOptions["BOOTSTRAP_SERVERS"],
			"ZOOKEEPER_ADDR":            instanceOptions["ZOOKEEPER_ADDR"],
			"MONITOR_LOG_KEY":           tmcInstance.ID,
			"MONITOR_LOG_OUTPUT":        instanceOptions["MONITOR_LOG_OUTPUT"],
			"MONITOR_LOG_OUTPUT_CONFIG": instanceOptions["MONITOR_LOG_OUTPUT_CONFIG"],
			"MONITOR_LOG_COLLECTOR":     instanceOptions["MONITOR_LOG_COLLECTOR"],
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

	config := map[string]string{
		"MONITOR_LOG_KEY":           tmcInstance.ID,
		"MONITOR_LOG_OUTPUT":        instanceOptions["MONITOR_LOG_OUTPUT"],
		"MONITOR_LOG_OUTPUT_CONFIG": instanceOptions["MONITOR_LOG_OUTPUT_CONFIG"],
		"MONITOR_LOG_COLLECTOR":     instanceOptions["MONITOR_LOG_COLLECTOR"],
	}

	return config
}
