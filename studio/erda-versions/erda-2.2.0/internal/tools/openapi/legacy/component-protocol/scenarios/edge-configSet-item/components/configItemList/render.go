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

package configitemlist

import (
	"context"
	"fmt"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-configSet-item/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

type EdgeConfigItem struct {
	ID          int64                         `json:"id"`
	ConfigName  string                        `json:"configName"`
	ConfigValue string                        `json:"configValue"`
	SiteName    string                        `json:"siteName"`
	CreateTime  string                        `json:"createTime"`
	UpdateTime  string                        `json:"updateTime"`
	Operate     apistructs.EdgeItemOperations `json:"operate"`
}

func (c *ComponentConfigItemList) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if err := c.SetComponent(component); err != nil {
		return err
	}

	identity := c.ctxBundle.Identity

	if c.component.State == nil {
		c.component.State = map[string]interface{}{}
	}

	c.component.State["configItemFormModalVisible"] = false
	c.component.State["formClear"] = false
	c.component.State["configSetItemID"] = 0

	if strings.HasPrefix(event.Operation.String(), apistructs.EdgeOperationChangePage) ||
		event.Operation == apistructs.InitializeOperation ||
		event.Operation == apistructs.RenderingOperation {
		err := c.OperateChangePage(false, identity)
		if err != nil {
			return err
		}
	} else if event.Operation.String() == apistructs.EdgeOperationDelete {
		err := c.OperateDelete(event.OperationData, identity)
		if err != nil {
			return err
		}
	} else if event.Operation.String() == apistructs.EdgeOperationUpdate {
		err := c.OperateReload(event.OperationData)
		if err != nil {
			return fmt.Errorf("operation reload error: %v", err)
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
			{Title: lr.Get(i18n.I18nKeyConfigItem), DataIndex: "configName", Width: 150},
			{Title: lr.Get(i18n.I18nKeyValue), DataIndex: "configValue", Width: 150},
			{Title: lr.Get(i18n.I18nKeySite), DataIndex: "siteName", Width: 150},
			{Title: lr.Get(i18n.I18nKeyCreateTime), DataIndex: "createTime", Width: 150},
			{Title: lr.Get(i18n.I18nKeyUpdateTime), DataIndex: "updateTime", Width: 150},
			{Title: lr.Get(i18n.I18nKeyOperator), DataIndex: "operate", Width: 150},
		},
	}
}

func getConfigsetItem(info apistructs.EdgeCfgSetItemInfo, lr *i18r.LocaleResource) apistructs.EdgeItemOperations {
	formData := map[string]interface{}{
		"id":    info.ID,
		"key":   info.ItemKey,
		"value": info.ItemValue,
	}

	if info.Scope == "public" {
		formData["scope"] = "COMMON"
	} else if info.Scope == "site" {
		formData["scope"] = "SITE"
		formData["sites"] = []string{info.SiteName}
	}

	return apistructs.EdgeItemOperations{
		RenderType: "tableOperation",
		Operations: map[string]apistructs.EdgeItemOperation{
			"viewDetail": {
				ShowIndex: 1,
				Key:       apistructs.EdgeOperationUpdate,
				Text:      lr.Get(i18n.I18nKeyEdit),
				Reload:    true,
				Meta: map[string]interface{}{
					"id": info.ID,
				},
				Command: apistructs.EdgeJumpCommand{
					Key:    "set",
					Target: "configItemFormModal",
					State: apistructs.EdgeJumpCommandState{
						Visible: true,
					},
				},
			},
			apistructs.EdgeOperationDelete: {
				ShowIndex:   2,
				Key:         apistructs.EdgeOperationDelete,
				Text:        lr.Get(i18n.I18nKeyDelete),
				Confirm:     lr.Get(i18n.I18nKeyDeleteConfirm),
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
