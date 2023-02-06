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

package addappbutton

import (
	"context"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-application/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

func (c ComponentAddAppButton) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)
	if component.State == nil {
		component.State = make(map[string]interface{})
	}

	if event.Operation.String() == apistructs.EdgeOperationAddApp {
		component.State["appConfigFormVisible"] = true
		component.State["addAppDrawerVisible"] = true
		component.State["keyValueListTitleVisible"] = false
		component.State["keyValueListVisible"] = false
		component.State["formClear"] = true
		component.State["operationType"] = event.Operation
	}

	component.Props = getProps(i18nLocale)
	component.Operations = getOperations()
	return nil
}

func getProps(lr *i18r.LocaleResource) apistructs.EdgeButtonProps {
	return apistructs.EdgeButtonProps{
		Type: "primary",
		Text: lr.Get(i18n.I18nKeyCreateApplication),
	}
}

func getOperations() apistructs.EdgeOperations {
	return apistructs.EdgeOperations{
		"click": apistructs.EdgeOperation{
			Key:    apistructs.EdgeOperationAddApp,
			Reload: true,
			Command: apistructs.EdgeJumpCommand{
				Key: "set",
				State: apistructs.EdgeJumpCommandState{
					Visible:  true,
					ReadOnly: false,
				},
				Target: "appFormModal",
			},
		},
	}
}
