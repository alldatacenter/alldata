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

package etcd

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceEtcd
}

func (p *provider) BuildServiceGroupRequest(resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{} {
	req := p.DefaultDeployHandler.BuildServiceGroupRequest(resourceInfo, tmcInstance, clusterConfig).(*apistructs.ServiceGroupCreateV2Request)

	for name, service := range resourceInfo.Dice.Services {
		nodeId := tmcInstance.ID + "_" + name
		env := map[string]string{
			"ADDON_ID":      tmcInstance.ID,
			"ADDON_NODE_ID": nodeId,
		}
		utils.AppendMap(service.Envs, env)

		//labels
		if service.Labels == nil {
			service.Labels = make(map[string]string)
		}
		options := map[string]string{}
		utils.JsonConvertObjToType(tmcInstance.Options, &options)
		utils.SetlabelsFromOptions(options, service.Labels)

		if p.IsNotDCOSCluster(clusterConfig["DICE_CLUSTER_TYPE"]) {
			//service.Binds = diceyml.Binds{nodeId + "_data:/etcd/data:rw"}
			//  /etcd/data volume
			vol := addon.SetAddonVolumes(options, "/etcd/data", false)
			service.Volumes = diceyml.Volumes{vol}
		} else {
			service.Binds = diceyml.Binds{
				clusterConfig["DICE_STORAGE_MOUNTPOINT"] + "/addon/etcd/" + nodeId + "/etcd/data:/etcd/data:rw",
			}
		}
	}

	return req
}

func (p *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{},
	clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {
	var vip string
	serviceGroup := serviceGroupDeployResult.(*apistructs.ServiceGroup)
	for _, service := range serviceGroup.Services {
		if service.Name == "etcd-1" {
			vip = service.Vip
			break
		}
	}

	config := map[string]string{
		"ETCD_HOST":    vip,
		"ETCD_PORT":    "2379",
		"ETCD_ADDRESS": vip + ":2379",
	}

	return config
}
