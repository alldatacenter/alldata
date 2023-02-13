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

package applicationlist

import (
	"context"
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-application/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

type EdgeAPPItem struct {
	ID              int64                         `json:"id"`
	ApplicationName apistructs.EdgeItemOperations `json:"appName"`
	Cluster         string                        `json:"cluster"`
	DeployResource  string                        `json:"deployResource"`
	Operate         apistructs.EdgeItemOperations `json:"operate"`
}

func (c ComponentList) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	var (
		isKeyValueListVisible bool
	)

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if err := c.SetComponent(component); err != nil {
		return err
	}

	orgID, err := strconv.ParseInt(c.ctxBundle.Identity.OrgID, 10, 64)
	if err != nil {
		return fmt.Errorf("component %s parse org id error: %v", component.Name, err)
	}

	if c.component.State == nil {
		c.component.State = map[string]interface{}{}
	}

	c.component.State["addAppDrawerVisible"] = false
	c.component.State["appConfigFormVisible"] = false
	c.component.State["formClear"] = false
	c.component.State["operationType"] = ""
	c.component.State["appID"] = 0

	identity := c.ctxBundle.Identity

	if strings.HasPrefix(event.Operation.String(), apistructs.EdgeOperationChangePage) ||
		event.Operation == apistructs.InitializeOperation ||
		event.Operation == apistructs.RenderingOperation {
		err = c.OperateChangePage(orgID, identity)
		if err != nil {
			return err
		}
	} else if event.Operation.String() == apistructs.EdgeOperationDelete {
		err = c.OperateDelete(orgID, event.OperationData, identity)
		if err != nil {
			return err
		}
	} else if event.Operation.String() == apistructs.EdgeOperationUpdate {
		err = c.OperateReload(event.OperationData)
		if err != nil {
			return fmt.Errorf("operation reload error: %v", err)
		}
		c.component.State["operationType"] = apistructs.EdgeOperationUpdate
	} else if event.Operation.String() == apistructs.EdgeOperationViewDetail {
		err = c.OperateReload(event.OperationData)
		if err != nil {
			return fmt.Errorf("operation reload error: %v", err)
		}
		c.component.State["operationType"] = apistructs.EdgeOperationViewDetail
		isKeyValueListVisible = true
	}

	c.component.State["keyValueListVisible"] = isKeyValueListVisible
	c.component.State["keyValueListTitleVisible"] = isKeyValueListVisible
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
			{Title: lr.Get(i18n.I18nKeyApplacationName), DataIndex: "appName", Width: 150},
			{Title: lr.Get(i18n.I18nKeyClusterBelonging), DataIndex: "cluster", Width: 150},
			{Title: lr.Get(i18n.I18nKeyDeploySource), DataIndex: "deployResource", Width: 150},
			{Title: lr.Get(i18n.I18nKeyOperator), DataIndex: "operate", Width: 150},
		},
	}
}

func getAPPItemOperate(appName, deployResource string, id int64, lr *i18r.LocaleResource) apistructs.EdgeItemOperations {
	return apistructs.EdgeItemOperations{
		RenderType: "tableOperation",
		Operations: map[string]apistructs.EdgeItemOperation{
			apistructs.EdgeOperationViewDetail: {
				ShowIndex: 1,
				Key:       apistructs.EdgeOperationViewDetail,
				Text:      lr.Get(i18n.I18nKeyDetail),
				Reload:    true,
				Meta:      map[string]interface{}{"id": id},
				Command: apistructs.EdgeJumpCommand{
					Key:    "set",
					Target: "addAppDrawer",
					State: apistructs.EdgeJumpCommandState{
						FormData: map[string]interface{}{
							"id":             id,
							"appName":        appName,
							"deployResource": deployResource,
						},
					},
				},
			},
			apistructs.EdgeOperationUpdate: {
				ShowIndex: 2,
				Key:       apistructs.EdgeOperationUpdate,
				Text:      lr.Get(i18n.I18nKeyEdit),
				Reload:    true,
				Meta:      map[string]interface{}{"id": id},
				Command: apistructs.EdgeJumpCommand{
					Key:    "set",
					Target: "addAppDrawer",
					State: apistructs.EdgeJumpCommandState{
						FormData: map[string]interface{}{
							"id":             id,
							"appName":        appName,
							"deployResource": deployResource,
						},
					},
				},
			},
			apistructs.EdgeOperationDelete: {
				ShowIndex:   3,
				Key:         apistructs.EdgeOperationDelete,
				Text:        lr.Get(i18n.I18nKeyDelete),
				Confirm:     lr.Get(i18n.I18nKeyDeleteConfirm),
				Disabled:    false,
				DisabledTip: lr.Get(i18n.I18nKeyNotDelete),
				Reload:      true,
				Meta:        map[string]interface{}{"id": id},
			},
		},
	}
}

func renderAppName(name string, id int64) apistructs.EdgeItemOperations {
	return apistructs.EdgeItemOperations{
		RenderType: "linkText",
		Value:      name,
		Operations: map[string]apistructs.EdgeItemOperation{
			"click": {
				Key:    "gotoDetail",
				Reload: false,
				Command: apistructs.EdgeJumpCommand{
					Key:     "goto",
					Target:  "ecpAppSiteManage",
					JumpOut: false,
					State: apistructs.EdgeJumpCommandState{
						Params: map[string]interface{}{
							"id": id,
						},
						Query: map[string]interface{}{
							"appName": name,
						},
					},
				},
			},
		},
	}
}
