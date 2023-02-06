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

package waitEditor

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type ComponentAction struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "waitEditor",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	ca.sdk = cputil.SDK(ctx)
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	v, err := json.Marshal(c.State["stepId"])
	if err != nil {
		return err
	}
	var stepID int
	err = json.Unmarshal(v, &stepID)
	if err != nil {
		return nil
	}
	if stepID <= 0 {
		return nil
	}
	switch event.Operation {
	case "submit":
		formDataJson, err := json.Marshal(c.State["formData"])
		if err != nil {
			return err
		}
		formData := map[string]interface{}{}
		err = json.Unmarshal(formDataJson, &formData)
		if err != nil {
			return err
		}

		var req apistructs.AutotestSceneRequest
		req.ID = uint64(stepID)
		req.Value = string(formDataJson)
		req.UserID = ca.sdk.Identity.UserID
		_, err = ca.bdl.UpdateAutoTestSceneStep(req)
		if err != nil {
			return err
		}
		c.State["drawVisible"] = false
	case "cancel":
		c.State["drawVisible"] = false
	case cptype.InitializeOperation, cptype.RenderingOperation:
		req := apistructs.AutotestGetSceneStepReq{
			ID:     uint64(stepID),
			UserID: ca.sdk.Identity.UserID,
		}
		step, err := ca.bdl.GetAutoTestSceneStep(req)
		if err != nil {
			return err
		}
		var waitTimeSec int
		if step.Value == "" {
			waitTimeSec = 0
		} else {
			var value apistructs.AutoTestRunWait
			if err := json.Unmarshal([]byte(step.Value), &value); err != nil {
				return err
			}
			if value.WaitTime > 0 {
				value.WaitTimeSec = value.WaitTime
			}
			waitTimeSec = value.WaitTimeSec
		}
		c.State["formData"] = map[string]interface{}{
			"waitTimeSec": waitTimeSec,
		}
		c.State["drawVisible"] = true
		c.Props = map[string]interface{}{
			"fields": []map[string]interface{}{
				{
					"label":          cputil.I18n(ctx, "waitTime"),
					"component":      "inputNumber",
					"required":       true,
					"key":            "waitTimeSec",
					"componentProps": map[string]interface{}{"min": 1},
				},
			},
		}
		c.Operations = map[string]interface{}{
			"submit": map[string]interface{}{
				"key":    "submit",
				"reload": true,
			},
			"cancel": map[string]interface{}{
				"reload": true,
				"key":    "cancel",
			},
		}
	}
	return nil
}
