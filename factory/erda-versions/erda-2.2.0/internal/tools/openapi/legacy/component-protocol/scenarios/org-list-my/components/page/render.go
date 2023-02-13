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

package page

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

func (i *ComponentPage) marshal(c *apistructs.Component) error {
	stateValue, err := json.Marshal(i.State)
	if err != nil {
		return err
	}
	var state map[string]interface{}
	err = json.Unmarshal(stateValue, &state)
	if err != nil {
		return err
	}

	c.State = state
	c.Props = i.Props
	return nil
}

func (i *ComponentPage) Render(ctx context.Context, c *apistructs.Component, s apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	defer func() {
		fail := i.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()

	i.initProperty(s)

	return nil
}

func (i *ComponentPage) initProperty(s apistructs.ComponentProtocolScenario) {
	myOrgs := Option{
		Key:  "my",
		Name: "我的组织",
		Operations: map[string]interface{}{
			"click": ClickOperation{
				Reload: false,
				Key:    "myOrg",
				Command: Command{
					Key:          "changeScenario",
					ScenarioKey:  s.ScenarioKey,
					ScenarioType: s.ScenarioType,
				},
			},
		},
	}
	publicOrgs := Option{
		Key:  "public",
		Name: "公开组织",
		Operations: map[string]interface{}{
			"click": ClickOperation{
				Reload: false,
				Key:    "publicOrg",
				Command: Command{
					Key:          "changeScenario",
					ScenarioKey:  "org-list-all",
					ScenarioType: "org-list-all",
				},
			},
		},
	}

	i.Props = Props{
		TabMenu: []Option{myOrgs, publicOrgs},
	}
	i.State.ActiveKey = myOrgs.Key
}

func RenderCreator() protocol.CompRender {
	return &ComponentPage{}
}
