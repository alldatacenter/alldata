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

package addPipelineBtn

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

type (
	AddPipelineBtn struct {
		Type       string                 `json:"type"`
		Name       string                 `json:"name"`
		Props      map[string]interface{} `json:"props"`
		Operations map[string]interface{} `json:"operations"`
	}
)

func (a *AddPipelineBtn) SetType() {
	a.Type = "Button"
}

func (a *AddPipelineBtn) SetName() {
	a.Name = "addPipelineBtn"
}

func (a *AddPipelineBtn) SetProps(ctx context.Context) {
	a.Props = map[string]interface{}{
		"prefixIcon": "add",
		"text":       cputil.I18n(ctx, "createPipeline"),
		"type":       "primary",
	}
}

func (a *AddPipelineBtn) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(a)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, &c)
}

func (a *AddPipelineBtn) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, a)
}
