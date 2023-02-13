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

package scenesSetInParams

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentAction struct {
	CtxBdl     protocol.ContextBundle
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
	TestPlanStepId uint64 `json:"testPlanStepId"`
	ScenesSetId    string `json:"scenesSetId"`
	Visible        bool   `json:"visible"`
}
type operations struct {
	Key      string                 `json:"key"`
	Reload   bool                   `json:"reload"`
	FillMeta string                 `json:"fillMeta"`
	Meta     interface{}            `json:"meta"`
	Command  map[string]interface{} `json:"command"`
}

func (a *ComponentAction) marshal(c *apistructs.Component) error {
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

func (a *ComponentAction) unmarshal(c *apistructs.Component) error {
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

	var treeData []interface{}
	dataJson, err := json.Marshal(c.Data["treeData"])
	if err != nil {
		return err
	}
	err = json.Unmarshal(dataJson, &treeData)
	if err != nil {
		return err
	}
	a.State = state
	a.Type = c.Type
	a.Props = prop
	//a.Operations = operation
	return nil
}

func (a *ComponentAction) SetBundle(b protocol.ContextBundle) error {
	if b.Bdl == nil {
		err := fmt.Errorf("invalie bundle")
		return err
	}
	a.CtxBdl = b
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *apistructs.Component,
	scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {

	err := ca.unmarshal(c)
	if err != nil {
		return err
	}

	if ca.State.TestPlanStepId <= 0 || ca.State.ScenesSetId == "" || ca.State.ScenesSetId == "0" {
		ca.Props.Visible = false
		ca.State.Visible = false
		return nil
	}

	defer func() {
		fail := ca.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	err = ca.SetBundle(bdl)
	if err != nil {
		return err
	}

	switch event.Operation {
	case apistructs.OnSubmit:
		if err := ca.HandlerSubmitValue(); err != nil {
			return err
		}
	case apistructs.OnCancel:
		if err := ca.HandlerCancelValue(); err != nil {
			return err
		}
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
		err := ca.handleDefault()
		if err != nil {
			return err
		}
	}
	return nil
}

func (i *ComponentAction) handleDefault() error {
	i.Props.Visible = true
	i.State.Visible = true
	i.Operations = map[string]operations{
		"submit": {
			Key:    "submit",
			Reload: true,
		},
		"cancel": {
			Key:    "cancel",
			Reload: true,
		},
	}
	return nil
}

func (i *ComponentAction) HandlerSubmitValue() error {

	scenesSetIdInt, err := strconv.Atoi(i.State.ScenesSetId)
	if err != nil {
		return err
	}

	var req apistructs.TestPlanV2StepUpdateRequest
	req.StepID = i.State.TestPlanStepId
	req.ScenesSetId = uint64(scenesSetIdInt)
	req.UserID = i.CtxBdl.Identity.UserID
	err = i.CtxBdl.Bdl.UpdateTestPlanV2Step(req)
	if err != nil {
		return err
	}

	i.State.Visible = false
	i.Props.Visible = false
	return nil
}

func (i *ComponentAction) HandlerCancelValue() error {
	i.State.Visible = false
	i.Props.Visible = false
	i.State.ScenesSetId = ""
	i.State.TestPlanStepId = 0
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}
