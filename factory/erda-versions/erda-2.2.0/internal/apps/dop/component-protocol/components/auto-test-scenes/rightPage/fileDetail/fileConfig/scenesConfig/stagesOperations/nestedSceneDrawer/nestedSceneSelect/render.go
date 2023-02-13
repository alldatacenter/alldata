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

package nestedSceneSelect

import (
	"context"
	"encoding/json"
	"strconv"

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
	Data       Data                  `json:"data"`
	State      state                 `json:"state"`
	Props      props                 `json:"props"`
	Operations map[string]operations `json:"operations"`
	Type       string                `json:"type"`
}

type props struct {
	Placeholder string `json:"placeholder"`
	Title       string `json:"title"`
}
type state struct {
	ConfigSheetId string `json:"configSheetId"`
	StepId        uint64 `json:"stepId"`
	Visible       bool   `json:"visible"`
	Value         Value  `json:"value"`
}

type Value struct {
	Value string `json:"value"`
	Label string `json:"label"`
}

type operations struct {
	Key      string      `json:"key"`
	Reload   bool        `json:"reload"`
	FillMeta string      `json:"fillMeta"`
	Meta     interface{} `json:"meta"`
}
type Data struct {
	TreeData []interface{} `json:"treeData"`
}

type StepValue struct {
	RunParams map[string]interface{} `json:"runParams"`
	SceneID   uint64                 `json:"sceneID"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "nestedSceneSelect",
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

	if a.Operations != nil {
		operationsValue, err := json.Marshal(a.Operations)
		if err != nil {
			return err
		}
		var operations map[string]interface{}
		err = json.Unmarshal(operationsValue, &operations)
		if err != nil {
			return err
		}
		c.Operations = operations
	}

	var data cptype.ComponentData = map[string]interface{}{}
	data["treeData"] = a.Data.TreeData
	c.Data = data
	c.State = state
	c.Type = a.Type
	c.Props = props
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

	var treeData []interface{}
	dataJson, err := json.Marshal(c.Data["treeData"])
	if err != nil {
		return err
	}
	err = json.Unmarshal(dataJson, &treeData)
	if err != nil {
		return err
	}
	var value Data
	value.TreeData = treeData

	operationsJson, err := json.Marshal(c.Operations)
	if err != nil {
		return err
	}
	var operation map[string]operations
	err = json.Unmarshal(operationsJson, &operation)
	if err != nil {
		return err
	}

	a.State = state
	a.Type = c.Type
	a.Data = value
	a.Props = prop
	a.Operations = operation
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	err := ca.unmarshal(c)
	if err != nil {
		return err
	}

	cv, ok := c.State["visible"]
	if !ok {
		return nil
	}
	if !cv.(bool) {
		return nil
	}

	defer func() {
		fail := ca.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	ca.sdk = cputil.SDK(ctx)
	ca.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)

	operationDataJson, err := json.Marshal(event.OperationData)
	if err != nil {
		return err
	}
	var metaMap map[string]interface{}
	err = json.Unmarshal(operationDataJson, &metaMap)
	if err != nil {
		return err
	}

	metaValue, ok := metaMap["meta"]
	var meta map[string]interface{}
	if ok {
		metaJson, err := json.Marshal(metaValue)
		if err != nil {
			return err
		}
		err = json.Unmarshal(metaJson, &meta)
		if err != nil {
			return err
		}
	}

	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		if err := ca.HandlerDefaultValue(meta); err != nil {
			return err
		}
	}
	return nil
}

func (i *ComponentAction) HandlerChangeValue(meta map[string]interface{}) error {
	metaValue := meta["value"].(map[string]interface{})
	i.State.Visible = true
	i.State.Value.Value = metaValue["value"].(string)
	i.State.Value.Label = metaValue["label"].(string)
	return nil
}

func (i *ComponentAction) HandlerDefaultValue(meta map[string]interface{}) error {
	var autotestGetSceneStepReq apistructs.AutotestGetSceneStepReq
	autotestGetSceneStepReq.ID = i.State.StepId
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

	i.State.Value.Value = strconv.Itoa(int(scene.ID))
	i.State.Value.Label = scene.Name
	i.Type = "TreeSelect"
	i.Props = props{
		Placeholder: "嵌套场景",
		Title:       "嵌套场景",
	}
	i.Operations = map[string]operations{}
	i.State.Visible = true
	return nil
}
