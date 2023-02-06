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

package loges

import (
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceLogEs
}

func (p *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{},
	clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {
	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	var urls []string
	serviceGroup := serviceGroupDeployResult.(*apistructs.ServiceGroup)
	for _, service := range serviceGroup.Services {
		if strings.HasPrefix(service.Name, "elasticsearch-") {
			urls = append(urls, "http://"+service.Vip+":9200")
		}
	}

	config := map[string]string{
		"MONITOR_LOG_KEY":           tmcInstance.ID,
		"MONITOR_LOG_OUTPUT":        "elasticsearch-rollover",
		"MONITOR_LOG_OUTPUT_CONFIG": "-",
		"MONITOR_LOG_COLLECTOR":     instanceOptions["MONITOR_LOG_COLLECTOR"],
		"ES_URLS":                   strings.Join(urls, ","),
	}

	return config
}

func (p *provider) BuildServiceGroupRequest(resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{} {
	req := p.DefaultDeployHandler.BuildServiceGroupRequest(resourceInfo, tmcInstance, clusterConfig).(*apistructs.ServiceGroupCreateV2Request)

	req.GroupLabels["ADDON_GROUPS"] = "4"

	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	for name, service := range resourceInfo.Dice.Services {
		nodeId := utils.GetRandomId()

		// envs
		env := map[string]string{
			"ADDON_ID":                  tmcInstance.ID,
			"ADDON_NODE_ID":             nodeId,
			"BOOTSTRAP_SERVERS":         instanceOptions["BOOTSTRAP_SERVERS"],
			"ZOOKEEPER_ADDR":            instanceOptions["ZOOKEEPER_ADDR"],
			"MONITOR_LOG_OUTPUT":        "elasticsearch-rollover",
			"MONITOR_LOG_OUTPUT_CONFIG": "-",
			"MONITOR_LOG_COLLECTOR":     instanceOptions["MONITOR_LOG_COLLECTOR"],
		}
		utils.AppendMap(service.Envs, env)

		if !strings.HasPrefix(name, "elasticsearch-") {
			continue
		}

		// labels
		if service.Labels == nil {
			service.Labels = make(map[string]string)
		}
		options := map[string]string{}
		utils.JsonConvertObjToType(tmcInstance.Options, &options)
		utils.SetlabelsFromOptions(options, service.Labels)

		// volumes
		if p.IsNotDCOSCluster(clusterConfig["DICE_CLUSTER_TYPE"]) {
			/*
				service.Binds = diceyml.Binds{
					nodeId + "_data:/usr/share/elasticsearch/data:rw",
					nodeId + "_logs:/usr/share/elasticsearch/logs:rw",
				}
			*/
			//  /usr/share/elasticsearch/data volume
			vol01 := addon.SetAddonVolumes(options, "/usr/share/elasticsearch/data", false)
			//  /usr/share/elasticsearch/logs volume
			vol02 := addon.SetAddonVolumes(options, "/usr/share/elasticsearch/logs", false)
			service.Volumes = diceyml.Volumes{vol01, vol02}

		} else {
			service.Binds = diceyml.Binds{
				clusterConfig["DICE_STORAGE_MOUNTPOINT"] + "/addon/elasticsearch/" + nodeId + "/data:/usr/share/elasticsearch/data:rw",
				clusterConfig["DICE_STORAGE_MOUNTPOINT"] + "/addon/elasticsearch/" + nodeId + "/data:/usr/share/elasticsearch/logs:rw",
			}
		}
	}

	return req
}
