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

package appsitebreadcrumb

import (
	"context"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/edge-app-site/i18n"
)

func (c ComponentBreadCrumb) Render(ctx context.Context, component *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	if err := c.SetBundle(bdl); err != nil {
		return err
	}
	i18nLocale := c.ctxBundle.Bdl.GetLocale(c.ctxBundle.Locale)

	if err := c.SetComponent(component); err != nil {
		return err
	}

	if event.Operation == apistructs.EdgeOperationClick {
	}

	c.component.Operations = getOperations()
	c.component.Data = map[string]interface{}{
		"list": []map[string]interface{}{
			{
				"key":  "appName",
				"item": i18nLocale.Get(i18n.I18nKeySiteList),
			},
		},
	}
	return nil
}

func getOperations() apistructs.EdgeOperations {
	return apistructs.EdgeOperations{
		apistructs.EdgeOperationClick: apistructs.EdgeOperation{
			Key:      "selectItem",
			Reload:   true,
			FillMeta: "key",
			Meta: map[string]interface{}{
				"key": "",
			},
		},
	}
}
