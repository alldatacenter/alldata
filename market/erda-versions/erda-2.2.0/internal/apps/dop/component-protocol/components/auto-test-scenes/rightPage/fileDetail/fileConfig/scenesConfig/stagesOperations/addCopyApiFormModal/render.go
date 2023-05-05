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

package addCopyApiFormModal

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type ComponentAction struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle
}

type State struct {
	Visible  bool `json:"visible"`
	FormData struct {
		ApiText string `json:"apiText"`
	}
	CreateStepID uint64 `json:"createStepID"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "addCopyApiFormModal",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	gh := gshelper.NewGSHelper(gs)
	ca.sdk = cputil.SDK(ctx)
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	v, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	var state State
	if err := json.Unmarshal(v, &state); err != nil {
		return err
	}
	switch event.Operation {
	case cptype.OperationKey(apistructs.ExecuteSubmitCopyOperationKey):
		copyText := state.FormData.ApiText
		scene, err := ca.bdl.GetAutoTestScene(apistructs.AutotestSceneRequest{
			IdentityInfo: apistructs.IdentityInfo{
				UserID: ca.sdk.Identity.UserID,
			},
			AutoTestSceneParams: apistructs.AutoTestSceneParams{
				ID: gh.GetFileTreeSceneID(),
			},
			Target:  -1,
			GroupID: -1,
			Type:    apistructs.StepTypeAPI,
			SceneID: gh.GetFileTreeSceneID(),
		})
		if err != nil {
			return err
		}
		var req apistructs.AutotestSceneRequest
		if err := json.Unmarshal([]byte(copyText), &req); err != nil {
			return err
		}
		req.ID = 0
		req.Target = -1
		req.GroupID = -1
		req.SceneID = scene.ID
		req.SpaceID = scene.SpaceID
		req.UserID = ca.sdk.Identity.UserID
		req.CreatorID = ca.sdk.Identity.UserID
		req.UpdaterID = ca.sdk.Identity.UserID
		req.APISpecID = 0
		req.RefSetID = 0
		req.PreType = ""
		req.Position = 0
		req.IsGroup = false
		stepID, err := ca.bdl.CreateAutoTestSceneStep(req)
		if err != nil {
			return err
		}
		c.State["createStepID"] = stepID
		c.State["visible"] = false
		c.State["formData"] = map[string]string{
			"apiText": "",
		}
	case cptype.InitializeOperation, cptype.RenderingOperation:
		c.Props = map[string]interface{}{
			"width": 850,
			"title": cputil.I18n(ctx, "addByText"),
			"fields": []interface{}{
				map[string]interface{}{
					"component": "textarea",
					"componentProps": map[string]interface{}{
						"autoSize": map[string]interface{}{
							"minRows": 8,
							"maxRows": 15,
						},
					},
					"key":      "apiText",
					"label":    cputil.I18n(ctx, "apiText"),
					"required": true,
				},
			},
		}
		c.Operations = map[string]interface{}{
			"submit": map[string]interface{}{
				"key":    "submitCopy",
				"reload": true,
			},
		}
	}
	return nil
}
