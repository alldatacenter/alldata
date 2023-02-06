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

package sitepreview

import (
	"context"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-site/i18n"
	i18r "github.com/erda-project/erda/pkg/i18n"
)

func (c *ComponentSitePreview) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	if err := c.SetBundle(bdl); err != nil {
		return err
	}

	if err := c.SetComponent(component); err != nil {
		return err
	}

	identity := c.ctxBundle.Identity

	if event.Operation == apistructs.RenderingOperation {
		if err := c.OperationRendering(identity); err != nil {
			return fmt.Errorf("oepration rendering error: %v", err)
		}

	}

	return nil
}

func getProps(lr *i18r.LocaleResource) map[string]interface{} {
	return map[string]interface{}{
		"render": []PropsRender{
			{Type: "Desc", DataIndex: "siteName", Props: map[string]interface{}{"title": lr.Get(i18n.I18nKeySite)}},
			{Type: "Desc", DataIndex: "firstStep", Props: map[string]interface{}{"title": lr.Get(i18n.I18nKeyStep1)}},
			{Type: "Desc", DataIndex: "secondStep", Props: map[string]interface{}{"title": lr.Get(i18n.I18nKeyStep2)}},
			{Type: "FileEditor", DataIndex: "operationCode", Props: map[string]interface{}{"actions": map[string]interface{}{"copy": true}}},
		},
	}
}
