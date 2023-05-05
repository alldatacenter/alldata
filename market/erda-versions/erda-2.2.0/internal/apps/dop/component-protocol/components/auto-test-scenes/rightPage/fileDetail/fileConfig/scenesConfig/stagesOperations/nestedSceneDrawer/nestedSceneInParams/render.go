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

package nestedSceneInParams

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
	sdk        *cptype.SDK
	bdl        *bundle.Bundle
	State      state                 `json:"state"`
	Props      props                 `json:"props"`
	Operations map[string]operations `json:"operations"`
	Type       string                `json:"type"`
}

type props struct {
	Fields  []interface{} `json:"fields"`
	Title   string        `json:"title"`
	Visible bool          `json:"visible"`
}
type state struct {
	FormData      map[string]interface{} `json:"formData"`
	StepID        uint64                 `json:"stepId"`
	ConfigSheetID string                 `json:"configSheetId"`
	Visible       bool                   `json:"visible"`
}
type operations struct {
	Key      string                 `json:"key"`
	Reload   bool                   `json:"reload"`
	FillMeta string                 `json:"fillMeta"`
	Meta     interface{}            `json:"meta"`
	Command  map[string]interface{} `json:"command"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "nestedSceneInParams",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (a *ComponentAction) marshal(c *cptype.Component) error {
	stateValue, err := json.Marshal(a.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	propValue, err := json.Marshal(a.Props)
	if err != nil {
		return err
	}
	var props map[string]interface{}
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	operationsValue, err := json.Marshal(a.Operations)
	if err != nil {
		return err
	}
	var operations map[string]interface{}
	err = json.Unmarshal(operationsValue, &operations)
	if err != nil {
		return err
	}

	c.State = state
	c.Type = a.Type
	c.Props = props
	c.Operations = operations
	return nil
}

func (a *ComponentAction) unmarshal(c *cptype.Component) error {
	stateValue, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	var state state
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	propValue, err := json.Marshal(c.Props)
	if err != nil {
		return err
	}
	var prop props
	err = json.Unmarshal(propValue, &prop)
	if err != nil {
		return err
	}

	a.State = state
	a.Type = c.Type
	a.Props = prop
	//a.Operations = operation
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {

	err := ca.unmarshal(c)
	if err != nil {
		return err
	}

	if ca.State.StepID <= 0 {
		ca.Props.Fields = nil
		ca.Props.Title = ""
		return nil
	}
	if v, ok := c.State["visible"]; ok {
		if v == false {
			return nil
		}
	}
	defer func() {
		fail := ca.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()
	ca.sdk = cputil.SDK(ctx)
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)

	switch event.Operation {
	case cptype.OperationKey(apistructs.OnSubmit):
		if err := ca.HandlerSubmitValue(); err != nil {
			return err
		}
	case cptype.OperationKey(apistructs.OnCancel):
		if err := ca.HandlerCancelValue(); err != nil {
			return err
		}
	case cptype.InitializeOperation, cptype.RenderingOperation:
		err := ca.handleDefault()
		if err != nil {
			return err
		}
	}
	return nil
}

type StepValue struct {
	RunParams map[string]interface{} `json:"runParams"`
	SceneID   uint64                 `json:"sceneID"`
}

func (i *ComponentAction) handleDefault() error {
	// 选中的 step
	var autotestGetSceneStepReq apistructs.AutotestGetSceneStepReq
	autotestGetSceneStepReq.ID = i.State.StepID
	autotestGetSceneStepReq.UserID = i.sdk.Identity.UserID
	step, err := i.bdl.GetAutoTestSceneStep(autotestGetSceneStepReq)
	if err != nil {
		return err
	}

	var stepValue StepValue
	err = json.Unmarshal([]byte(step.Value), &stepValue)
	if err != nil {
		return err
	}

	var autotestSceneRequest apistructs.AutotestSceneRequest
	autotestSceneRequest.SceneID = stepValue.SceneID
	autotestSceneRequest.UserID = i.sdk.Identity.UserID
	inputs, err := i.bdl.ListAutoTestSceneInput(autotestSceneRequest)
	if err != nil {
		return err
	}

	var fromData = stepValue.RunParams
	i.Props.Fields = []interface{}{}
	for _, v := range inputs {
		i.Props.Fields = append(i.Props.Fields, map[string]interface {
		}{
			"label":     v.Name,
			"component": "input",
			"required":  true,
			"key":       v.Name,
		})
	}

	i.Props.Title = "嵌套入参"
	i.State.FormData = fromData
	i.Props.Visible = true
	i.Operations = map[string]operations{
		"submit": {
			Key:    "submit",
			Reload: true,
		},
		"cancel": {
			Key:    "cancel",
			Reload: true,
			Command: map[string]interface{}{
				"key":    "set",
				"target": "configSheetDrawer",
				"state":  map[string]interface{}{"visible": false},
			},
		},
	}
	return nil
}

func (i *ComponentAction) HandlerSubmitValue() error {
	var autotestGetSceneStepReq apistructs.AutotestGetSceneStepReq
	autotestGetSceneStepReq.ID = i.State.StepID
	autotestGetSceneStepReq.UserID = i.sdk.Identity.UserID
	step, err := i.bdl.GetAutoTestSceneStep(autotestGetSceneStepReq)
	if err != nil {
		return err
	}

	var stepValue StepValue
	err = json.Unmarshal([]byte(step.Value), &stepValue)
	if err != nil {
		return err
	}

	var autotestSceneRequest apistructs.AutotestSceneRequest
	autotestSceneRequest.UserID = i.sdk.Identity.UserID
	autotestSceneRequest.SceneID = stepValue.SceneID
	scene, err := i.bdl.GetAutoTestScene(autotestSceneRequest)
	if err != nil {
		return err
	}

	stepValue.RunParams = i.State.FormData
	valueJson, err := json.Marshal(stepValue)
	if err != nil {
		return err
	}

	var req apistructs.AutotestSceneRequest
	req.ID = i.State.StepID
	req.Value = string(valueJson)
	req.Name = scene.Name
	req.UserID = i.sdk.Identity.UserID
	_, err = i.bdl.UpdateAutoTestSceneStep(req)
	if err != nil {
		return err
	}

	i.Props.Visible = false
	i.Props.Fields = nil
	i.State.Visible = false
	i.State.StepID = 0
	return nil
}

func (i *ComponentAction) HandlerCancelValue() error {
	i.State.Visible = false
	i.Props.Visible = false
	i.Props.Fields = nil
	i.State.StepID = 0
	return nil
}
