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

package siteadddrawer

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-application/i18n"
)

type DrawerRendering struct {
	Visible       bool   `json:"visible"`
	OperationType string `json:"operationType"`
}

func (c *ComponentAddAppDrawer) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	var (
		drawer DrawerRendering
	)

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)

	if component.State == nil {
		component.State = map[string]interface{}{}
	}

	if event.Operation == apistructs.RenderingOperation {

		jsonData, err := json.Marshal(component.State)
		if err != nil {
			return err
		}

		if err = json.Unmarshal(jsonData, &drawer); err != nil {
			return err
		}

		component.State = map[string]interface{}{
			"visible": drawer.Visible,
		}

		edgeProps := apistructs.EdgeDrawerProps{
			Size: "l",
		}

		switch drawer.OperationType {
		case apistructs.EdgeOperationUpdate:
			edgeProps.Title = i18nLocale.Get(i18n.I18nKeyEditedgeApplication)
			break
		case apistructs.EdgeOperationViewDetail:
			edgeProps.Title = i18nLocale.Get(i18n.I18nKeyEdgeApplicationDetail)
			break
		case apistructs.EdgeOperationAddApp:
			edgeProps.Title = i18nLocale.Get(i18n.I18nKeyCreateEdgeApplication)
			break
		default:
			edgeProps.Title = i18nLocale.Get(i18n.I18nKeyEdgeApplication)
		}

		component.Props = edgeProps
	}

	return nil
}
