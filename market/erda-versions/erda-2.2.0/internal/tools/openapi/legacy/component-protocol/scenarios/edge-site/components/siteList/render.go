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

package sitelist

import (
	"context"
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-site/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

type EdgeSiteItem struct {
	ID             int64                         `json:"id"`
	SiteName       apistructs.EdgeItemOperations `json:"siteName"`
	NodeNum        string                        `json:"nodeNum"`
	RelatedCluster string                        `json:"relatedCluster"`
	Operate        apistructs.EdgeItemOperations `json:"operate"`
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

	orgID, err := strconv.ParseInt(c.ctxBundle.Identity.OrgID, 10, 64)
	if err != nil {
		return fmt.Errorf("component %s parse org id error: %v", component.Name, err)
	}

	identity := c.ctxBundle.Identity

	if c.component.State == nil {
		c.component.State = make(map[string]interface{})
	}

	c.component.State["sitePreviewVisible"] = false
	c.component.State["siteAddDrawerVisible"] = false
	c.component.State["siteFormModalVisible"] = false
	c.component.State["formClear"] = false
	c.component.State["siteID"] = 0

	if strings.HasPrefix(event.Operation.String(), apistructs.EdgeOperationChangePage) ||
		event.Operation == apistructs.InitializeOperation ||
		event.Operation == apistructs.RenderingOperation {

		err = c.OperateChangePage(orgID, false, identity)
		if err != nil {
			return err
		}

	} else if event.Operation.String() == apistructs.EdgeOperationDelete {
		err = c.OperateDelete(orgID, event.OperationData, identity)
		if err != nil {
			return err
		}
	} else if event.Operation.String() == apistructs.EdgeOperationAdd {
		err = c.OperateReload(event.OperationData, event.Operation.String())
		if err != nil {
			return err
		}
	} else if event.Operation.String() == apistructs.EdgeOperationUpdate {
		err = c.OperateReload(event.OperationData, event.Operation.String())
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
			{Title: lr.Get(i18n.I18nKeySiteName), DataIndex: "siteName", Width: 150},
			{Title: lr.Get(i18n.I18nKeyNodeNumber), DataIndex: "nodeNum", Width: 150},
			{Title: lr.Get(i18n.I18nKeyAssociatedCluster), DataIndex: "relatedCluster", Width: 150},
			{Title: lr.Get(i18n.I18nKeyOperator), DataIndex: "operate", Width: 150},
		},
	}
}

func getSiteItemOperate(info apistructs.EdgeSiteInfo, lr *i18r.LocaleResource) apistructs.EdgeItemOperations {
	return apistructs.EdgeItemOperations{
		RenderType: "tableOperation",
		Operations: map[string]apistructs.EdgeItemOperation{
			apistructs.EdgeOperationUpdate: {
				ShowIndex: 2,
				Key:       apistructs.EdgeOperationUpdate,
				Text:      lr.Get(i18n.I18nKeyEdit),
				Reload:    true,
				Meta: map[string]interface{}{
					"id": info.ID,
				},
				Command: apistructs.EdgeJumpCommand{
					Key:    "set",
					Target: "siteFormModal",
					State: apistructs.EdgeJumpCommandState{
						Visible: true,
					},
				},
			},
			apistructs.EdgeOperationAdd: {
				ShowIndex: 1,
				Key:       apistructs.EdgeOperationAdd,
				Text:      lr.Get(i18n.I18nKeyAddNode),
				Reload:    true,
				Meta: map[string]interface{}{
					"id": info.ID,
				},
				Command: apistructs.EdgeJumpCommand{
					Key:    "add",
					Target: "siteAddDrawer",
					State: apistructs.EdgeJumpCommandState{
						Visible: true,
					},
				},
			},
			apistructs.EdgeOperationDelete: {
				ShowIndex:   3,
				Key:         apistructs.EdgeOperationDelete,
				Text:        lr.Get(i18n.I18nKeyDelete),
				Confirm:     lr.Get(i18n.I18nKeyDeleteSiteConfigSetTip),
				Disabled:    false,
				DisabledTip: lr.Get(i18n.I18nKeyNotDelete),
				Reload:      true,
				Meta: map[string]interface{}{
					"id": info.ID,
				},
			},
		},
	}
}

func renderSiteName(clusterName, name string, id int64) apistructs.EdgeItemOperations {
	return apistructs.EdgeItemOperations{
		RenderType: "linkText",
		Value:      name,
		Operations: map[string]apistructs.EdgeItemOperation{
			"click": {
				Key:    "gotoMachine",
				Reload: false,
				Command: apistructs.EdgeJumpCommand{
					Key:     "goto",
					Target:  "ecpSiteMachine",
					JumpOut: false,
					State: apistructs.EdgeJumpCommandState{
						Params: map[string]interface{}{
							"id": id,
						},
						Query: map[string]interface{}{
							"siteName":    name,
							"clusterName": clusterName,
						},
					},
				},
			},
		},
	}
}
