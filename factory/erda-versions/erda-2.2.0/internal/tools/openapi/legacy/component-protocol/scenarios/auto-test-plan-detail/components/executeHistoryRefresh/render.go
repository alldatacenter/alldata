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

package executeHistoryRefresh

import (
	"context"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
)

type ComponentAction struct{}

func (ca *ComponentAction) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	i18nLocale := bdl.Bdl.GetLocale(bdl.Locale)
	c.Operations = map[string]interface{}{
		"click": map[string]interface{}{
			"key":    "toNewRecord",
			"reload": true,
		},
	}
	c.Props = map[string]interface{}{
		"text":       i18nLocale.Get(i18n.I18nKeyBackLatestRecord),
		"type":       "text",
		"prefixIcon": "shuaxin",
		"style":      map[string]interface{}{"width": 140},
	}
	c.State = map[string]interface{}{"pageNo": 1}
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}
