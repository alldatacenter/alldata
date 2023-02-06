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

package fileConfig

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentAction struct {
	State State `json:"state"`
}

type State struct {
	ActiveKey apistructs.TestPlanActiveKey `json:"activeKey"`
	Visible   bool                         `json:"visible"`
}

func (ca *ComponentAction) GenComponentState(c *apistructs.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	cont, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
		return err
	}
	err = json.Unmarshal(cont, &state)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return err
	}
	ca.State = state
	return nil
}

func (ca *ComponentAction) marshal(c *apistructs.Component) error {
	// state
	stateValue, err := json.Marshal(ca.State)
	if err != nil {
		return err
	}
	var stateMap map[string]interface{}
	err = json.Unmarshal(stateValue, &stateMap)
	if err != nil {
		return err
	}
	c.State = stateMap
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	if err := ca.GenComponentState(c); err != nil {
		return err
	}
	props := make(map[string]interface{})
	if ca.State.ActiveKey == apistructs.ConfigTestPlanActiveKey {
		props["visible"] = true
		ca.State.Visible = true
	} else {
		props["visible"] = false
		ca.State.Visible = false
	}

	c.Props = props

	if err := ca.marshal(c); err != nil {
		return err
	}

	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}
