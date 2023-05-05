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

package configsetlist

import (
	"context"
	"fmt"
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-configSet/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

type EdgeConfigsetItem struct {
	ID             int64                         `json:"id"`
	ConfigsetName  string                        `json:"configSetName"`
	RelatedCluster string                        `json:"relatedCluster"`
	Operate        apistructs.EdgeItemOperations `json:"operate"`
}

func (c *ComponentConfigsetList) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

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

	if strings.HasPrefix(event.Operation.String(), apistructs.EdgeOperationChangePage) ||
		event.Operation == apistructs.InitializeOperation ||
		event.Operation == apistructs.RenderingOperation {
		err = c.OperateChangePage(orgID, false, identity)
		if err != nil {
			return fmt.Errorf("operation change page error: %v", err)
		}
	} else if event.Operation.String() == apistructs.EdgeOperationDelete {
		err = c.OperateDelete(orgID, event.OperationData, identity)
		if err != nil {
			return fmt.Errorf("operation delete error: %v", err)
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
			{Title: lr.Get(i18n.I18nKeyConfigSetName), DataIndex: "configSetName", Width: 150},
			{Title: lr.Get(i18n.I18nKeyAssociatedCluster), DataIndex: "relatedCluster", Width: 150},
			{Title: lr.Get(i18n.I18nKeyOperator), DataIndex: "operate", Width: 150},
		},
	}
}

func getConfigsetItem(id int64, configSetName string, lr *i18r.LocaleResource) apistructs.EdgeItemOperations {
	return apistructs.EdgeItemOperations{
		RenderType: "tableOperation",
		Operations: map[string]apistructs.EdgeItemOperation{
			"viewDetail": {
				ShowIndex: 1,
				Key:       "viewDetail",
				Text:      lr.Get(i18n.I18nKeyDetail),
				Reload:    false,
				Meta: map[string]interface{}{
					"id":            id,
					"configSetName": configSetName,
				},
				Command: apistructs.EdgeJumpCommand{
					JumpOut: false,
					Key:     "goto",
					State: apistructs.EdgeJumpCommandState{
						Params: map[string]interface{}{
							"id": id,
						},
						Query: map[string]interface{}{
							"configSetName": configSetName,
						},
					},
					Target: "ecpSettingDetail",
				},
			},
			apistructs.EdgeOperationDelete: {
				ShowIndex:   2,
				Key:         apistructs.EdgeOperationDelete,
				Text:        lr.Get(i18n.I18nKeyDelete),
				Confirm:     lr.Get(i18n.I18nKeyDeleteConfigSetTip),
				Disabled:    false,
				DisabledTip: lr.Get(i18n.I18nKeyNotDelete),
				Reload:      true,
				Meta:        map[string]interface{}{"id": id},
			},
		},
	}
}
