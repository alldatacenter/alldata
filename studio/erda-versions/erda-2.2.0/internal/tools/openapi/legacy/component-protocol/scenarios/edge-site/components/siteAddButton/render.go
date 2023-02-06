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

package siteaddbutton

import (
	"context"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	edgesite "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-site"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-site/i18n"
)

func (c *ComponentSiteAddButton) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if component.State == nil {
		component.State = make(map[string]interface{})
	}

	if event.Operation.String() == apistructs.EdgeOperationAddSite {
		component.State["siteFormModalVisible"] = true
		component.State["formClear"] = true
	}

	component.Props = edgesite.StructToMap(apistructs.EdgeButtonProps{
		Type: "primary",
		Text: i18nLocale.Get(i18n.I18nKeyCreateSite),
	})
	component.Operations = apistructs.EdgeOperations{
		"click": apistructs.EdgeOperation{
			Key:    "addSite",
			Reload: true,
			Command: apistructs.EdgeJumpCommand{
				Key: "set",
				State: apistructs.EdgeJumpCommandState{
					Visible: true,
				},
				Target: "siteFormModal",
			},
		},
	}
	return nil
}
