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

package loganalytics

import (
	"context"
	"fmt"
	"time"

	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
	"github.com/erda-project/erda/pkg/parser/diceyml"
)

const AddonLogIndexPrefix = "rlogs-"

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceLogAnalytics
}

func (p *provider) BuildSubResourceDeployRequest(name string, addon *diceyml.AddOn, req *handlers.ResourceDeployRequest) *handlers.ResourceDeployRequest {
	deployment, _ := p.LogDeploymentDb.GetByClusterName(req.Az, db.LogTypeLogAnalytics)
	if deployment != nil {
		// already deployed outside, no need to do further deploy
		orgId := req.Options["orgId"]
		orgDeployment, _ := p.LogDeploymentDb.GetByClusterNameAndOrgId(req.Az, orgId, db.LogTypeLogAnalytics)
		if orgDeployment == nil {
			orgDeployment = deployment
			orgDeployment.ID = 0
			orgDeployment.OrgId = orgId
			p.LogDeploymentDb.Save(&orgDeployment)
		}

		return nil
	}

	return p.DefaultDeployHandler.BuildSubResourceDeployRequest(name, addon, req)
}

func (p *provider) BuildTmcInstanceConfig(tmcInstance *db.Instance, serviceGroupDeployResult interface{},
	clusterConfig map[string]string, additionalConfig map[string]string) map[string]string {
	config := map[string]string{}
	options := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &options)

	// if already deployed outside, just return ????
	orgId := options["orgId"]
	if len(orgId) == 0 {
		orgId = "0"
	}
	deployment, _ := p.LogDeploymentDb.GetByClusterName(tmcInstance.Az, db.LogTypeLogAnalytics)
	if deployment != nil {
		// already deployed outside, no need to do further deploy
		orgDeployment, _ := p.LogDeploymentDb.GetByClusterNameAndOrgId(tmcInstance.Az, orgId, db.LogTypeLogAnalytics)
		if orgDeployment == nil {
			orgDeployment = deployment
			orgDeployment.ID = 0
			orgDeployment.OrgId = orgId
			p.LogDeploymentDb.Save(&orgDeployment)
		}

		return config
	}

	if options["MONITOR_LOG_OUTPUT"] == "elasticsearch-rollover" {
		p.createLogDeployment(orgId, tmcInstance.Az, options["ES_URLS"], options["MONITOR_LOG_COLLECTOR"], clusterConfig)
		config["MONITOR_LOG_OUTPUT"] = "elasticsearch-rollover"
		config["MONITOR_LOG_OUTPUT_CONFIG"] = "-"
		config["MONITOR_LOG_COLLECTOR"] = options["MONITOR_LOG_COLLECTOR"]
	}

	return config
}

func (p *provider) DoApplyTmcInstanceTenant(req *handlers.ResourceDeployRequest, resourceInfo *handlers.ResourceInfo,
	tmcInstance *db.Instance, tenant *db.InstanceTenant, clusterConfig map[string]string) (map[string]string, error) {

	options := map[string]string{}

	instanceOptions := map[string]string{}
	utils.JsonConvertObjToType(tmcInstance.Options, &instanceOptions)
	utils.AppendMap(options, instanceOptions)

	tenantOptions := map[string]string{}
	utils.JsonConvertObjToType(tenant.Options, &tenantOptions)
	utils.AppendMap(options, tenantOptions)

	collector := instanceOptions["MONITOR_LOG_COLLECTOR"]
	if len(collector) == 0 {
		dp, _ := p.LogDeploymentDb.GetByClusterName(tmcInstance.Az, db.LogTypeLogAnalytics)
		if dp != nil {
			collector = dp.CollectorUrl
		}
	}

	config, err := p.createLogAnalytics(tenant.ID, tmcInstance.Az, options)
	if err != nil {
		return nil, err
	}
	config["MONITOR_LOG_KEY"] = tenant.ID
	config["MONITOR_LOG_OUTPUT"] = "elasticsearch-rollover"
	config["MONITOR_LOG_OUTPUT_CONFIG"] = "-"
	config["MONITOR_LOG_COLLECTOR"] = collector

	params := map[string]string{
		"tenantId":    tenant.ID,
		"tenantGroup": tenant.TenantGroup,
		"logKey":      tenant.ID,
		"key":         "LogQuery",
	}
	paramstr, _ := utils.JsonConvertObjToString(params)
	config["PUBLIC_HOST"] = paramstr // params for microservice menu

	return config, nil
}

func (p *provider) DeleteTenant(tenant *db.InstanceTenant, tmcInstance *db.Instance, clusterConfig map[string]string) error {

	logInstance, _ := p.LogInstanceDb.GetLatestByLogKey(tenant.ID, db.LogTypeLogAnalytics)
	if logInstance != nil && len(logInstance.ClusterName) > 0 {
		p.LogInstanceDb.Model(logInstance).Update("is_delete", 1)
		p.deleteIndex(logInstance.OrgId, tenant.ID, logInstance.ClusterName)
	}

	return p.DefaultDeployHandler.DeleteTenant(tenant, tmcInstance, clusterConfig)
}

func (p *provider) createLogDeployment(orgId string, clusterName string, esUrls string, collector string, clusterConfig map[string]string) {
	domain := clusterConfig["DICE_ROOT_DOMAIN"]
	clusterType := 0
	if clusterConfig["DICE_IS_EDGE"] == "true" {
		clusterType = 1
	}

	deploy := db.LogDeployment{
		OrgId:        orgId,
		ClusterName:  clusterName,
		ClusterType:  clusterType,
		EsUrl:        esUrls,
		EsConfig:     "{}",
		CollectorUrl: collector,
		Domain:       domain,
		Created:      time.Now(),
		Updated:      time.Now(),
		LogType:      string(db.LogTypeLogAnalytics),
	}

	p.LogDeploymentDb.Save(&deploy)
}

func (p *provider) createLogAnalytics(logKey string, clusterName string, options map[string]string) (map[string]string, error) {
	p.createIndex(options["orgId"], clusterName, logKey)

	config := map[string]string{
		"TERMINUS_LOG_KEY": logKey,
	}

	instance, err := p.LogInstanceDb.GetLatestByLogKey(logKey, db.LogTypeLogAnalytics)
	if err != nil {
		return nil, err
	}
	if instance != nil {
		return config, nil
	}

	configStr, _ := utils.JsonConvertObjToString(config)
	instance = &db.LogInstance{
		LogKey:          logKey,
		ClusterName:     clusterName,
		OrgId:           options["orgId"],
		OrgName:         options["orgName"],
		ProjectId:       options["projectId"],
		ProjectName:     options["projectName"],
		Workspace:       options["workspace"],
		ApplicationId:   options["applicationId"],
		ApplicationName: options["applicationName"],
		RuntimeId:       options["runtimeId"],
		RuntimeName:     options["runtimeName"],
		Plan:            "",
		IsDelete:        0,
		Version:         options["version"],
		Config:          configStr,
		Created:         time.Now(),
		Updated:         time.Now(),
		LogType:         string(db.LogTypeLogAnalytics),
	}
	if err := p.InstanceDb.Save(instance).Error; err != nil {
		return nil, err
	}

	return config, nil
}

func (p *provider) createIndex(orgId string, clusterName string, logKey string) error {
	logDeployment, err := p.LogDeploymentDb.GetByClusterNameAndOrgId(clusterName, orgId, db.LogTypeLogAnalytics)
	if err != nil {
		return err
	}

	if logDeployment == nil {
		return fmt.Errorf("could not found logDeployment for cluster: %s, orgId: %s", clusterName, orgId)
	}

	esClient := utils.GetESClientsFromLogAnalytics(logDeployment, logKey)
	if esClient == nil {
		return fmt.Errorf("can not build esclient")
	}

	indexPrefix := AddonLogIndexPrefix + logKey
	index := "<" + indexPrefix + "-{now/d{yyyy.ww}}-000001>"
	var orgAlias string
	if len(orgId) > 0 {
		orgAlias = AddonLogIndexPrefix + "org-" + orgId
		esClient.CreateIndexTemplate(indexPrefix, indexPrefix+"-*", orgAlias)
	}

	return esClient.CreateIndexWithAlias(index, indexPrefix, orgAlias)
}

func (p *provider) deleteIndex(orgId string, logKey string, clusterName string) error {
	logDeployment, err := p.LogDeploymentDb.GetByClusterNameAndOrgId(clusterName, orgId, db.LogTypeLogAnalytics)
	if err != nil {
		return err
	}

	if logDeployment == nil {
		return fmt.Errorf("could not found logDeployment for cluster: %s, orgId: %s", clusterName, orgId)
	}

	esClient := utils.GetESClientsFromLogAnalytics(logDeployment, logKey)
	if esClient == nil {
		return fmt.Errorf("can not build esclient")
	}

	indexPrefix := AddonLogIndexPrefix + logKey
	indices := indexPrefix + "-*"

	resp, err := esClient.DeleteIndex(indices).Do(context.Background())
	if err != nil {
		return err
	}

	if !resp.Acknowledged {
		return fmt.Errorf("response code error")
	}

	return nil
}
