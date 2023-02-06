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

package apigateway

import (
	"encoding/json"
	"fmt"
	"strings"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceApiGateway
}

func (p *provider) DoPreDeployJob(resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance) error {
	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	az := tmcInstance.Az

	pipelineYml := apistructs.PipelineYml{
		Version: "1.1",
		Envs: map[string]string{
			"KONG_DATABASE":                 "postgres",
			"KONG_PG_HOST":                  instanceOptions["POSTGRESQL_HOST"],
			"KONG_CASSANDRA_CONTACT_POINTS": instanceOptions["POSTGRESQL_HOST"],
			"KONG_PG_PORT":                  instanceOptions["POSTGRESQL_PORT"],
			"KONG_PG_USER":                  instanceOptions["POSTGRESQL_USER"],
			"KONG_PG_PASSWORD":              instanceOptions["POSTGRESQL_PASSWORD"],
			"KONG_PG_DATABASE":              instanceOptions["POSTGRESQL_DATABASE"],
		},
		Stages: [][]*apistructs.PipelineYmlAction{{{
			Type:      "custom-script",
			Version:   "1.0",
			Image:     resourceInfo.Dice.Services["api-gateway-0"].Image,
			Commands:  []string{"kong migrations bootstrap"},
			Resources: apistructs.Resources{Cpu: 0.1, Mem: 512},
		}}},
	}

	yml, _ := json.Marshal(pipelineYml)

	pipelineReq := &apistructs.PipelineCreateRequestV2{
		PipelineYml:     string(yml),
		PipelineSource:  "dice",
		PipelineYmlName: uuid.UUID() + ".yml",
		ClusterName:     az,
		AutoRunAtOnce:   true,
	}

	pipelineResp, err := p.Bdl.CreatePipeline(pipelineReq)
	if err != nil {
		return err
	}

	startTime := time.Now().Unix()
	status := apistructs.PipelineStatusUnknown
	for time.Now().Unix()-startTime < handlers.RuntimeMaxUpTimeoutSeconds {
		time.Sleep(10 * time.Second)

		detail, err := p.Bdl.GetPipelineV2(apistructs.PipelineDetailRequest{
			SimplePipelineBaseResult: true,
			PipelineID:               pipelineResp.ID,
		})
		if err != nil {
			continue
		}

		status = detail.Status
		if status == apistructs.PipelineStatusSuccess ||
			status == apistructs.PipelineStatusFailed ||
			status == apistructs.PipelineStatusTimeout ||
			status == apistructs.PipelineStatusStopByUser ||
			status == apistructs.PipelineStatusDBError ||
			status == apistructs.PipelineStatusError ||
			status == apistructs.PipelineStatusStartError ||
			status == apistructs.PipelineStatusCreateError ||
			status == apistructs.PipelineStatusLostConn ||
			status == apistructs.PipelineStatusCancelByRemote {
			break
		}
	}

	p.Bdl.DeletePipeline(pipelineResp.ID)

	switch status {
	case apistructs.PipelineStatusSuccess:
		return nil
	default:
		return fmt.Errorf("init job run failed: %s", status)
	}
}

func (p *provider) BuildServiceGroupRequest(resourceInfo *handlers.ResourceInfo, tmcInstance *db.Instance, clusterConfig map[string]string) interface{} {
	req := p.DefaultDeployHandler.BuildServiceGroupRequest(resourceInfo, tmcInstance, clusterConfig).(*apistructs.ServiceGroupCreateV2Request)

	delete(req.GroupLabels, "LOCATION-CLUSTER-SERVICE")

	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)

	for name, service := range resourceInfo.Dice.Services {
		delete(service.Envs, "LOCATION-CLUSTER-SERVICE")

		// envs
		nodeId := tmcInstance.ID + "_" + name
		env := map[string]string{
			"ADDON_ID":                      tmcInstance.ID,
			"ADDON_NODE_ID":                 nodeId,
			"DICE_CLUSTER":                  tmcInstance.Az,
			"KONG_PG_HOST":                  instanceOptions["POSTGRESQL_HOST"],
			"KONG_CASSANDRA_CONTACT_POINTS": instanceOptions["POSTGRESQL_HOST"],
			"KONG_PG_PORT":                  instanceOptions["POSTGRESQL_PORT"],
			"KONG_PG_USER":                  instanceOptions["POSTGRESQL_USER"],
			"KONG_PG_PASSWORD":              instanceOptions["POSTGRESQL_PASSWORD"],
			"KONG_PG_DATABASE":              instanceOptions["POSTGRESQL_DATABASE"],
		}
		utils.AppendMap(service.Envs, env)

		// labels
		service.Labels["HAPROXY_GROUP"] = "external"
		service.Labels["HAPROXY_0_VHOST"] = p.getHaproxyVHost(clusterConfig["DICE_ROOT_DOMAIN"])

		options := map[string]string{}
		utils.JsonConvertObjToType(tmcInstance.Options, &options)
		utils.SetlabelsFromOptions(options, service.Labels)

		// volumes
		//hostPath := tmcInstance.ID
		if p.IsNotDCOSCluster(clusterConfig["DICE_CLUSTER_TYPE"]) {
			/*
				service.Binds = diceyml.Binds{
					hostPath + ":/opt/backup:rw",
				}
			*/

			//  /opt/backup volume
			vol := addon.SetAddonVolumes(options, "/opt/backup", false)
			service.Volumes = diceyml.Volumes{vol}
		}
	}

	return req
}

func (p *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{},
	clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {

	serviceGroup := serviceGroupDeployResult.(*apistructs.ServiceGroup)
	mainClusterDomain := p.Cfg.MainClusterInfo.RootDomain
	mainClusterScheme := p.Cfg.MainClusterInfo.Protocol
	mainClusterHTTPPort := p.Cfg.MainClusterInfo.HttpPort
	mainClusterHTTPSPort := p.Cfg.MainClusterInfo.HttpsPort

	var vip string
	for _, service := range serviceGroup.Services {
		if service.Name == "api-gateway-0" {
			vip = service.Vip
			break
		}
	}

	config := map[string]string{}

	schema := mainClusterScheme
	var port string
	if strings.Contains(schema, "https") {
		schema = "https"
		port = mainClusterHTTPSPort
	} else if strings.Contains(schema, "http") {
		schema = "http"
		port = mainClusterHTTPPort
	}
	config["HEPA_GATEWAY_HOST"] = schema + "://hepa." + mainClusterDomain
	config["HEPA_GATEWAY_PORT"] = port

	config["VIP_KONG_HOST"] = "http://" + vip
	config["PROXY_KONG_PORT"] = "8000"
	config["GATEWAY_ENDPOINT"] = "gateway." + clusterConfig["DICE_ROOT_DOMAIN"]
	config["GATEWAY_INSTANCE_ID"] = tmcInstance.ID
	config["ADMIN_ENDPOINT"] = vip + ":8001"
	config["KONG_HOST"] = vip

	return config
}

func (p *provider) DoApplyTmcInstanceTenant(req *handlers.ResourceDeployRequest, resourceInfo *handlers.ResourceInfo,
	tmcInstance *db.Instance, tenant *db.InstanceTenant, clusterConfig map[string]string) (map[string]string, error) {
	resultConfig := map[string]string{}

	tenantOptions := map[string]string{}
	utils.JsonConvertObjToType(tenant.Options, &tenantOptions)
	instanceConfig := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Config, &instanceConfig)

	gatewayReq := apistructs.GatewayTenantRequest{
		ID:              tenant.ID,
		TenantGroup:     tenant.TenantGroup,
		Az:              tmcInstance.Az,
		ProjectId:       tenantOptions["projectId"],
		ProjectName:     tenantOptions["projectName"],
		Env:             tenantOptions["env"],
		ServiceName:     "api-gateway",
		InstanceId:      tmcInstance.ID,
		InnerAddr:       "http://" + instanceConfig["KONG_HOST"] + ":8000",
		AdminAddr:       instanceConfig["ADMIN_ENDPOINT"],
		GatewayEndpoint: instanceConfig["GATEWAY_ENDPOINT"],
	}
	err := p.Bdl.CreateGatewayTenant(&gatewayReq)
	success := err == nil // for debug purpose
	if !success {
		return nil, err
	}

	key, _ := p.TmcIniDb.GetMicroServiceEngineJumpKey(tmcInstance.Engine)

	console := map[string]string{
		"tenantGroup": tenant.TenantGroup,
		"tenantId":    tenant.ID,
		"key":         key,
	}

	str, _ := utils.JsonConvertObjToString(console)
	resultConfig["PUBLIC_HOST"] = str

	return resultConfig, nil
}

func (p *provider) getHaproxyVHost(domain string) string {
	domainPrefixes := []string{
		"dev-gateway.",
		"test-gateway.",
		"staging-gateway.",
		"gateway.",
	}

	for i, prefix := range domainPrefixes {
		domainPrefixes[i] = prefix + domain
	}

	return strings.Join(domainPrefixes, ",")
}
