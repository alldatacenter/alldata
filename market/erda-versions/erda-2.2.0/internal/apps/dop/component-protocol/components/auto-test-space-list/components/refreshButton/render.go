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

package refreshButton

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

type Props struct {
	Visible bool   `json:"visible"`
	Text    string `json:"text"`
}

type State struct {
	AutoRefresh bool `json:"autoRefresh"`
}

type RefreshButton struct {
	Type       string                 `json:"type"`
	Props      Props                  `json:"props"`
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
}

func (r *RefreshButton) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	var autoRefresh bool
	r.Type = "Button"
	r.Props.Visible = false
	r.Operations = map[string]interface{}{
		"autoRefresh": map[string]interface{}{
			"key":         "autoRefresh",
			"reload":      true,
			"showLoading": false,
		},
	}
	switch event.Operation {
	case "autoRefresh":
		autoRefresh = true
	}
	r.State.AutoRefresh = autoRefresh
	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-space-list", "refreshButton",
		func() servicehub.Provider { return &RefreshButton{} })
}
