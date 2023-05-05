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

package fileDetail

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
)

type ComponentAction struct {
	State State `json:"state"`

	sceneID uint64
}

type State struct {
	ClickType            string                          `json:"clickType"`
	ActiveKey            apistructs.ActiveKey            `json:"activeKey"`
	AutotestSceneRequest apistructs.AutotestSceneRequest `json:"autotestSceneRequest"`
	IsChangeScene        bool                            `json:"isChangeScene"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "fileDetail",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (ca *ComponentAction) RenderState(c *cptype.Component) error {
	var state State
	b, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &state); err != nil {
		return err
	}
	ca.State = state
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	gh := gshelper.NewGSHelper(gs)

	if c.State == nil {
		c.State = map[string]interface{}{}
	}

	if err := ca.RenderState(c); err != nil {
		return err
	}

	ca.sceneID = gh.GetFileTreeSceneID()

	props := make(map[string]interface{})

	if ca.State.IsChangeScene {
		ca.State.ActiveKey = "fileConfig"
		ca.State.IsChangeScene = false
	}
	if event.Operation == cptype.RenderingOperation {
		executeActiveKey := gh.GetExecuteButtonActiveKey()
		if executeActiveKey != "" {
			ca.State.ActiveKey = executeActiveKey
		}
	}
	if event.Operation == cptype.InitializeOperation {
		ca.State.ActiveKey = "fileConfig"
		//props["visible"] = false
	}
	if event.Operation == "changeActiveKey" {
		ca.State.ActiveKey = c.State["activeKey"].(apistructs.ActiveKey)
	}
	// props

	if gh.GetGlobalSelectedSetID() != 0 || gh.GetFileTreeSceneID() != 0 {
		props["visible"] = true
	} else {
		props["visible"] = false
	}
	// props["visible"] = true
	props["tabMenu"] = []map[string]string{
		{"key": "fileConfig", "name": cputil.I18n(ctx, "configInfo")},
		{"key": "fileExecute", "name": cputil.I18n(ctx, "executeDetail")},
	}
	c.Props = props

	c.Operations = make(map[string]interface{})
	c.Operations["onChange"] = struct {
		Key    string `json:"key"`
		Reload bool   `json:"reload"`
	}{
		Key:    "changeActiveKey",
		Reload: true,
	}
	// set state
	setState(c, ca.State, gs)

	return json.Unmarshal([]byte(`{"onChange":{"key":"changeViewType","reload":true}}`), &c.Operations)
}

func setState(c *cptype.Component, state State, gs *cptype.GlobalStateData) {
	c.State["activeKey"] = state.ActiveKey
	c.State["autotestSceneRequest"] = state.AutotestSceneRequest
	c.State["isChangeScene"] = state.IsChangeScene
	//c.State["activeKey"] = "fileExecute"

	gh := gshelper.NewGSHelper(gs)
	gh.SetFileDetailActiveKey(state.ActiveKey)
	gh.SetFileDetailIsChangeScene(state.IsChangeScene)
	gh.SetExecuteButtonActiveKey(apistructs.ActiveKeyfileConfig)
}
