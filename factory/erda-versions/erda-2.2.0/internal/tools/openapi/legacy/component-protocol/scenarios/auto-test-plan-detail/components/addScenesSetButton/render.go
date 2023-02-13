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

package addScenesSetButton

import (
	"context"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
)

type ComponentAction struct{}

func (ca *ComponentAction) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)

	i18nLocale := bdl.Bdl.GetLocale(bdl.Locale)
	switch event.Operation {
	case apistructs.ExecuteAddApiOperationKey:
		resp, err := bdl.Bdl.GetTestPlanV2(uint64(c.State["testPlanId"].(float64)))
		if err != nil {
			return err
		}
		steps := resp.Data.Steps
		var lastStepID uint64
		if len(steps) > 0 {
			lastStepID = steps[len(steps)-1].ID
		}

		var req apistructs.TestPlanV2StepAddRequest
		req.UserID = bdl.Identity.UserID
		req.TestPlanID = uint64(c.State["testPlanId"].(float64))
		req.PreID = lastStepID
		req.GroupID = 0
		stepID, err := bdl.Bdl.CreateTestPlansV2Step(req)
		if err != nil {
			return err
		}
		c.State["showScenesSetDrawer"] = true
		fmt.Println(stepID)
		fmt.Printf("\n\n\\n\n\\n\n\\n\n\\n\n\n")

		c.State["testPlanStepId"] = stepID
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
		c.Type = "Button"
		c.Props = map[string]interface{}{
			"text": fmt.Sprintf("+ %s", i18nLocale.Get(i18n.I18nKeySceneSet)),
		}
		c.Operations = map[string]interface{}{
			"click": map[string]interface{}{
				"key":    "addApi",
				"reload": true,
			},
		}
	}
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}
