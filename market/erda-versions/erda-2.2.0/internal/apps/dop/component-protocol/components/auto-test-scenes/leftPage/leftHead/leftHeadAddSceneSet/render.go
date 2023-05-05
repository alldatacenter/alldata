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

package leftHeadAddSceneSet

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
)

type LeftHeadAddSceneSet struct {
	Name       string                 `json:"name"`
	Type       string                 `json:"type"`
	State      State                  `json:"state"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
}

type State struct {
	ActionType  string `json:"actionType"`
	FormVisible bool   `json:"formVisible"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "leftHeadAddSceneSet",
		func() servicehub.Provider { return &LeftHeadAddSceneSet{} })
}

func (l *LeftHeadAddSceneSet) GenComponentState(c *cptype.Component) error {
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
	l.State = state
	return nil
}

func (l *LeftHeadAddSceneSet) marshal(c *cptype.Component) error {
	stateValue, err := json.Marshal(l.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	propValue, err := json.Marshal(l.Props)
	if err != nil {
		return err
	}
	var props cptype.ComponentProps
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	c.Operations = l.Operations
	c.Props = props
	c.State = state
	c.Type = l.Type
	c.Name = l.Name
	return nil
}

func (l *LeftHeadAddSceneSet) Import(c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, l); err != nil {
		return err
	}
	return nil
}

func (l *LeftHeadAddSceneSet) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (err error) {
	if err := l.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}

	defer func() {
		fail := l.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	l.Type = "Icon"
	l.Name = "leftHeadAddSceneSet"
	l.Props = map[string]interface{}{
		"iconType": "plus",
		"size":     18,
		"visible":  true,
	}
	l.Operations = map[string]interface{}{
		"click": map[string]interface{}{
			"key":    "ClickAddSceneSet",
			"reload": true,
		},
	}
	switch apistructs.OperationKey(event.Operation) {
	case apistructs.ClickAddSceneSeButtonOperationKey:
		l.State.ActionType = "ClickAddSceneSetButton"
		l.State.FormVisible = true
	}
	return nil
}
