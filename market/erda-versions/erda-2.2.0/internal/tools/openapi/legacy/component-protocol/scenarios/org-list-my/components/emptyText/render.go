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

package emptyText

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

func (i *ComponentEmptyText) unmarshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	var state State
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}
	i.State = state
	propValue, err := json.Marshal(i.Props)
	if err != nil {
		return err
	}
	var prop Props
	err = json.Unmarshal(propValue, &prop)
	if err != nil {
		return err
	}
	i.Props = prop
	return nil
}

func (i *ComponentEmptyText) marshal(c *apistructs.Component) error {
	propValue, err := json.Marshal(i.Props)
	if err != nil {
		return err
	}
	var prop map[string]interface{}
	err = json.Unmarshal(propValue, &prop)
	if err != nil {
		return err
	}
	c.Props = i.Props
	c.Operations = i.Operations
	return nil
}

func (i *ComponentEmptyText) Render(ctx context.Context, c *apistructs.Component, s apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	err = i.unmarshal(c)
	if err != nil {
		return err
	}

	defer func() {
		fail := i.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	i.initProperty(s)
	switch event.Operation {
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
		if i.State.IsEmpty {
			i.Props.Visible = true
		}
	}
	return nil
}

func (i *ComponentEmptyText) initProperty(s apistructs.ComponentProtocolScenario) {
	i.Operations = map[string]interface{}{
		"toPublicOrg": Operation{
			Key:    "toPublicOrg",
			Reload: false,
			Command: Command{
				Key:          "changeScenario",
				ScenarioKey:  "org-list-all",
				ScenarioType: "org-list-all",
			},
		},
	}
	i.Props = Props{
		Visible:    false,
		RenderType: "linkText",
		StyleConfig: Config{
			FontSize:   16,
			LineHeight: 24,
		},
		Value: map[string][]interface{}{
			"text": {
				"您还未加入任何组织，可以选择",
				Redirect{
					Text:         "公开组织",
					OperationKey: "toPublicOrg",
					StyleConfig: RedirectConfig{
						Bold: true,
					},
				},
				"开启您的Erda之旅",
			},
		},
	}
}

func RenderCreator() protocol.CompRender {
	return &ComponentEmptyText{}
}
