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

package notifyTitle

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentAction struct {
	CtxBdl protocol.ContextBundle
}

type NotifyTitle struct {
	Type  string `json:"type"`
	Props Props  `json:"props"`
}

type Props struct {
	Title string `json:"title"`
}

func RenderCreator() protocol.CompRender {
	return &ComponentAction{}
}

func (ca *ComponentAction) Render(ctx context.Context, c *apistructs.Component, scenario apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) error {
	if err := ca.Import(c); err != nil {
		logrus.Errorf("import component is failed err is %v", err)
		return err
	}
	notifyTitle := NotifyTitle{
		Type: "Title",
		Props: Props{
			Title: "帮助您更好地组织通知项",
		},
	}
	c.Props = notifyTitle.Props
	c.Type = notifyTitle.Type
	return nil
}

func (ca *ComponentAction) Import(c *apistructs.Component) error {
	com, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(com, ca); err != nil {
		return err
	}
	return nil
}
