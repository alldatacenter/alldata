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

package siteiplist

import (
	"context"
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-app-site-ip/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

const (
	statusStarting  = "Starting" // 已启动，但未收到健康检查事件，瞬态
	statusRunning   = "Running"
	statusHealthy   = "Healthy"
	statusUnHealthy = "UnHealthy" // 已启动但收到未通过健康检查事件
	statusFinished  = "Finished"  // 已完成，退出码为0
	statusFailed    = "Failed"    // 已退出，退出码非0
	statusKilled    = "Killed"    // 已被杀
	statusStopped   = "Stopped"   // 已停止，Scheduler与DCOS断连期间事件丢失，后续补偿时，需将Healthy置为Stopped
	statusUnknown   = "Unknown"
	statusOOM       = "OOM"
	statusDead      = "Dead"
)

type EdgeSiteMachineItem struct {
	ID        int64                         `json:"id"`
	IP        string                        `json:"ip"`
	Address   string                        `json:"address"`
	Status    apistructs.EdgeTextBadge      `json:"status"`
	CreatedAt string                        `json:"createdAt"`
	Operate   apistructs.EdgeItemOperations `json:"operate"`
}

func (c *ComponentList) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if err := c.SetComponent(component); err != nil {
		return err
	}

	if c.component.State == nil {
		c.component.State = make(map[string]interface{})
	}

	orgID, err := strconv.ParseInt(c.ctxBundle.Identity.OrgID, 10, 64)
	if err != nil {
		return fmt.Errorf("component %s parse org id error: %v", component.Name, err)
	}

	identity := c.ctxBundle.Identity

	if strings.HasPrefix(event.Operation.String(), apistructs.EdgeOperationChangePage) ||
		event.Operation == apistructs.InitializeOperation ||
		event.Operation == apistructs.RenderingOperation {
		err = c.OperateChangePage(orgID, identity)
		if err != nil {
			return err
		}
	}

	c.component.Props = getProps(i18nLocale)
	c.component.Operations = getOperations()

	return nil
}

func getOperations() apistructs.EdgeOperations {
	return apistructs.EdgeOperations{
		"changePageNo": apistructs.EdgeOperation{
			Key:    "changePageNo",
			Reload: true,
		},
		"changePageSize": apistructs.EdgeOperation{
			Key:    "changePageSize",
			Reload: true,
		},
	}
}

func getProps(lr *i18r.LocaleResource) apistructs.EdgeTableProps {
	return apistructs.EdgeTableProps{
		PageSizeOptions: []string{"10", "20", "50", "100"},
		RowKey:          "id",
		Columns: []apistructs.EdgeColumns{
			{Title: lr.Get(i18n.I18nKeyInstanceIp), DataIndex: "ip", Width: 150},
			{Title: lr.Get(i18n.I18nKeyHostAddress), DataIndex: "address", Width: 150},
			{Title: lr.Get(i18n.I18nKeyStatus), DataIndex: "status", Width: 150},
			{Title: lr.Get(i18n.I18nKeyCreateTime), DataIndex: "createdAt", Width: 150},
			{Title: lr.Get(i18n.I18nKeyOperator), DataIndex: "operate", Width: 150},
		},
	}
}

func getItemOperations(containerID, ip, clusterName string, lr *i18r.LocaleResource) apistructs.EdgeItemOperations {
	return apistructs.EdgeItemOperations{
		RenderType: "tableOperation",
		Operations: map[string]apistructs.EdgeItemOperation{
			"viewTerminal": {
				Key:    "viewTerminal",
				Text:   lr.Get(i18n.I18nKeyConsole),
				Reload: false,
				Meta: map[string]interface{}{
					"clusterName": clusterName,
					"instance": map[string]interface{}{
						"id":          containerID,
						"containerId": containerID,
						"clusterName": clusterName,
						"hostIP":      ip,
					},
					"type": "terminal",
				},
			},
			"viewMonitor": {
				Key:    "viewMonitor",
				Text:   lr.Get(i18n.I18nKeyContinerMonitor),
				Reload: false,
				Meta: map[string]interface{}{
					"api": "/api/orgCenter/metrics",
					"instance": map[string]interface{}{
						"id":          containerID,
						"containerId": containerID,
						"clusterName": clusterName,
						"hostIP":      ip,
					},
					"type": "monitor",
					"extraQuery": map[string]interface{}{
						"filter_cluster_name": clusterName,
					},
				},
			},
			"viewLog": {
				Key:    "viewLog",
				Text:   lr.Get(i18n.I18nKeyLogging),
				Reload: false,
				Meta: map[string]interface{}{
					"fetchApi": "/api/orgCenter/logs",
					"instance": map[string]interface{}{
						"id":          containerID,
						"containerId": containerID,
						"clusterName": clusterName,
						"hostIP":      ip,
					},
					"extraQuery": map[string]interface{}{
						"clusterName": clusterName,
					},
					"sourceType": "container",
					"type":       "log",
				},
			},
		},
	}
}

func getStatus(phase string) apistructs.EdgeTextBadge {
	status := apistructs.EdgeTextBadge{
		RenderType: "textWithBadge",
	}

	switch phase {
	case statusRunning, statusHealthy:
		status.Value = phase
		status.Status = "success"
	case statusStarting, statusUnHealthy, statusUnknown:
		status.Value = phase
		status.Status = "warning"
	case statusOOM, statusFinished, statusFailed, statusStopped, statusKilled, statusDead:
		status.Value = phase
		status.Status = "error"
	}
	return status
}

func GetEdgeApplicationContainerStatus(phase string) string {
	switch phase {
	// Is running
	case statusRunning, statusHealthy, statusStarting, statusUnHealthy, statusUnknown:
		return "success"
	// Stopped
	case statusOOM, statusFinished, statusFailed, statusStopped, statusKilled, statusDead:
		return "error"
	default:
		return "error"
	}
}
