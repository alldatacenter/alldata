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

package monitor

import (
	"context"
	"time"

	"github.com/erda-project/erda-proto-go/core/monitor/settings/pb"
	"github.com/erda-project/erda/internal/apps/msp/instance/db"
	monitordb "github.com/erda-project/erda/internal/apps/msp/instance/db/monitor"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/handlers"
	"github.com/erda-project/erda/internal/apps/msp/resource/utils"
)

func (p *provider) IsMatch(tmc *db.Tmc) bool {
	return tmc.Engine == handlers.ResourceMonitor
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

	return p.createMonitor(tmcInstance.Engine, tenant.ID, tenant.TenantGroup, options)
}

func (p *provider) DeleteTenant(tenant *db.InstanceTenant, tmcInstance *db.Instance, clusterConfig map[string]string) error {

	p.MonitorDb.UpdateStatusByMonitorId(tenant.ID, 1)
	return p.DefaultDeployHandler.DeleteTenant(tenant, tmcInstance, clusterConfig)
}

func (p *provider) createMonitor(engine, requestId, requestGroup string, options map[string]string) (map[string]string, error) {

	key, _ := p.TmcIniDb.GetMicroServiceEngineJumpKey(engine)
	tenantId := requestGroup

	console := map[string]string{
		"tenantId":    requestId,
		"tenantGroup": tenantId,
		"terminusKey": tenantId,
		"key":         key,
	}

	phstr, err := utils.JsonConvertObjToString(console)

	config := map[string]string{
		"TERMINUS_KEY":              tenantId,
		"TERMINUS_AGENT_ENABLE":     "true",
		"TERMINUS_TA_ENABLE":        "true",
		"TERMINUS_TA_URL":           p.Cfg.TaStaticUrl,
		"TERMINUS_TA_COLLECTOR_URL": p.Cfg.TaCollectUrl,
		"PUBLIC_HOST":               phstr,
	}

	// create project record for status monitor
	statusConfig, err := p.registerStatus(requestId, options)
	if err != nil {
		return nil, err
	}
	utils.AppendMap(config, statusConfig)

	data, err := p.MonitorDb.GetByTerminusKey(requestId)
	if err != nil {
		return nil, err
	}
	if data != nil {
		return config, nil
	}

	configStr, err := utils.JsonConvertObjToString(config)

	data = &monitordb.Monitor{
		TerminusKey:   tenantId,
		MonitorId:     tenantId,
		CallbackUrl:   "",
		Plan:          "",
		IsDelete:      0,
		Config:        configStr,
		Version:       options["version"],
		ClusterName:   options["clusterName"],
		OrgId:         options["orgId"],
		OrgName:       options["orgName"],
		ProjectId:     options["projectId"],
		ProjectName:   options["projectName"],
		Workspace:     options["workspace"],
		ApplicationId: options["applicationId"],
		RuntimeId:     options["runtimeId"],
		RuntimeName:   options["runtimeName"],
		Created:       time.Now(),
		Updated:       time.Now(),
	}

	err = p.MonitorDb.Save(&data).Error
	if err != nil {
		return nil, err
	}

	err = p.registerMonitor(data.TerminusKey, data.Workspace, data.OrgId)
	if err != nil {
		return nil, err
	}

	return config, nil
}

func (p *provider) registerStatus(id string, options map[string]string) (map[string]string, error) {
	projectId := options["projectId"]
	projectName := options["projectName"]

	project, err := p.ProjectDb.GetByProjectId(projectId)
	if err != nil {
		return nil, err
	}
	if project == nil {
		project = &db.Project{
			ProjectId:   projectId,
			Name:        projectName,
			Identity:    projectName,
			Description: "Create Project From Addon Register Callback",
			Ats:         "",
			Callback:    "",
		}
		if p.ProjectDb.Save(project).Error != nil {
			return nil, err
		}
	}

	// now we got the old or new-created project record
	config := map[string]string{
		"STATUS_PAGE_INDEX": "",
	}

	return config, nil
}

func (p *provider) registerMonitor(tk string, workspace string, orgId string) error {
	desc := "tmc"
	var configList = []*pb.MonitorConfig{
		p.newMonitorConfig(workspace, orgId, "application_*", "[{\"key\":\"target_terminus_key\",\"value\":\""+tk+"\"}]"),
		p.newMonitorConfig(workspace, orgId, "application_*", "[{\"key\":\"source_terminus_key\",\"value\":\""+tk+"\"}]"),
		p.newMonitorConfig(workspace, orgId, "jvm_*,nodejs_*,trace_*,error_count,service_node,status_page", "[{\"key\":\"terminus_key\",\"value\":\""+tk+"\"}]"),
		p.newMonitorConfig(workspace, orgId, "ta_*", "[{\"key\":\"type\",\"value\":\"browser\"}, {\"key\":\"tk\",\"value\":\""+tk+"\"}]"),
	}
	_, err := p.SettingClient.RegisterMonitorConfig(context.Background(), &pb.RegisterMonitorConfigRequest{
		Data: configList,
		Desc: desc,
	})
	return err
}

func (p *provider) newMonitorConfig(workspace, orgId, names, filters string) *pb.MonitorConfig {
	return &pb.MonitorConfig{
		Scope:     "org",
		ScopeID:   orgId,
		Namespace: workspace,
		Type:      "metric",
		Enable:    true,
		Names:     names,
		Filters:   filters,
	}
}
