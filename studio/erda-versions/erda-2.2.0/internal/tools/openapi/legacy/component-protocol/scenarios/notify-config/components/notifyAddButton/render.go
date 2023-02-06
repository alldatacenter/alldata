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

package notifyAddButton

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentButton struct {
	ctxBdl     protocol.ContextBundle
	Type       string             `json:"type"`
	Operations AddButtonOperation `json:"operations"`
	Props      Props              `json:"props"`
	State      State              `json:"state"`
}

type State struct {
	Operation string `json:"operation"`
	Visible   bool   `json:"visible"`
	EditId    uint64 `json:"editId"`
}

type Props struct {
	Text string `json:"text"`
	Type string `json:"type"`
}

type AddButtonOperation struct {
	Click Click `json:"click"`
}

type Click struct {
	key    string `json:"key"`
	Reload bool   `json:"reload"`
}

type CommandState struct {
	Visible  bool        `json:"visible"`
	FormData interface{} `json:"formData"`
}

func (cb *ComponentButton) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	if err := cb.Import(c); err != nil {
		logrus.Errorf("import button component is failed err is %v", err)
		return err
	}
	cb.ctxBdl = ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	if err := cb.handlerAddOperation(); err != nil {
		return err
	}
	if err := cb.Export(c); err != nil {
		logrus.Errorf("export button component is failed err is %v", err)
		return err
	}
	return nil
}

func (cb *ComponentButton) handlerAddOperation() error {
	buttonResp := ComponentButton{
		Type: "Button",
		Operations: AddButtonOperation{
			Click: Click{
				key:    "addNotify",
				Reload: true,
			},
		},
		Props: Props{
			Text: "新建通知",
			Type: "primary",
		},
	}
	cb.Props = buttonResp.Props
	cb.Type = buttonResp.Type
	//使弹窗可见
	cb.State.Visible = true
	data, err := json.Marshal(buttonResp.Operations)
	if err != nil {
		return err
	}
	err = json.Unmarshal(data, &cb.Operations)
	if err != nil {
		return err
	}
	return nil
}

func (cb *ComponentButton) Import(c *apistructs.Component) error {
	com, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(com, cb); err != nil {
		return err
	}
	return nil
}

func (cb *ComponentButton) Export(c *apistructs.Component) error {
	b, err := json.Marshal(cb)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, c); err != nil {
		return err
	}
	return nil
}

func RenderCreator() protocol.CompRender {
	return &ComponentButton{}
}
