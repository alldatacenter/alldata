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

package appsitemanage

import (
	"context"
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-app-site/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

type EdgeAppDetailItem struct {
	ID           int                           `json:"id"`
	SiteName     map[string]interface{}        `json:"siteName"`
	DeployStatus apistructs.EdgeTextBadge      `json:"deployStatus"`
	Operate      apistructs.EdgeItemOperations `json:"operate"`
}

type EdgeAppDetailExItem struct {
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

	identity := c.ctxBundle.Identity

	if strings.HasPrefix(event.Operation.String(), apistructs.EdgeOperationChangePage) ||
		event.Operation == apistructs.InitializeOperation ||
		event.Operation == apistructs.RenderingOperation {
		if err := c.OperateChangePage(false, "", identity); err != nil {
			return fmt.Errorf("change page operation error: %v", err)
		}
	} else if event.Operation == apistructs.EdgeOperationOffline {
		if err := c.OperateOffline(event.OperationData, identity); err != nil {
			return fmt.Errorf("offline operation error: %v", err)
		}
	} else if event.Operation == apistructs.EdgeOperationRestart {
		if err := c.OperateRestart(event.OperationData, identity); err != nil {
			return fmt.Errorf("restart operation error: %v", err)
		}
	}

	c.component.Operations = getOperations()
	c.component.Props = getProps(i18nLocale)

	return nil
}

func getProps(lr *i18r.LocaleResource) apistructs.EdgeTableProps {
	return apistructs.EdgeTableProps{
		PageSizeOptions: []string{"10", "20", "50", "100"},
		RowKey:          "siteName",
		Columns: []apistructs.EdgeColumns{
			{Title: lr.Get(i18n.I18nKeySiteName), DataIndex: "siteName", Width: 150},
			{Title: lr.Get(i18n.I18nKeyDeployStatus), DataIndex: "deployStatus", Width: 150},
			{Title: lr.Get(i18n.I18nKeyOperator), DataIndex: "operate", Width: 150},
		},
	}
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

func getSiteItemOperate(inParam EdgeAppSiteInParam, siteName string, isAllOperate bool, lr *i18r.LocaleResource) apistructs.EdgeItemOperations {
	return apistructs.EdgeItemOperations{
		RenderType: "tableOperation",
		Operations: map[string]apistructs.EdgeItemOperation{
			apistructs.EdgeOperationRestart: {
				ShowIndex:   1,
				Key:         apistructs.EdgeOperationRestart,
				Text:        lr.Get(i18n.I18nKeyReboot),
				Confirm:     lr.Get(i18n.I18nKeyRebootConfirm),
				Disabled:    !isAllOperate,
				DisabledTip: lr.Get(i18n.I18nKeyNotReboot),
				Reload:      true,
				Meta: map[string]interface{}{
					"appID":    inParam.ID,
					"siteName": siteName,
				},
			},
			apistructs.EdgeOperationOffline: {
				ShowIndex:   2,
				Key:         apistructs.EdgeOperationOffline,
				Text:        lr.Get(i18n.I18nKeyOffline),
				Confirm:     lr.Get(i18n.I18nKeyOfflineConfirm),
				Disabled:    !isAllOperate,
				DisabledTip: lr.Get(i18n.I18nKeyNotOffline),
				Reload:      true,
				Meta: map[string]interface{}{
					"appID":    inParam.ID,
					"siteName": siteName,
				},
			},
		},
	}
}

func renderSiteName(id int64, siteName, appName string) map[string]interface{} {
	return map[string]interface{}{
		"renderType": "linkText",
		"value":      siteName,
		"operations": apistructs.EdgeOperations{
			"click": apistructs.EdgeOperation{
				Key:    "gotoSiteManage",
				Reload: false,
				Command: apistructs.EdgeJumpCommand{
					Key:     "goto",
					Target:  "ecpAppSiteIpManage",
					JumpOut: false,
					State: apistructs.EdgeJumpCommandState{
						Params: map[string]interface{}{
							"id":       id,
							"siteName": siteName,
							"appName":  appName,
						},
						Query: map[string]interface{}{
							"appName": appName,
						},
						Visible: false,
					},
				},
			},
		},
	}
}
