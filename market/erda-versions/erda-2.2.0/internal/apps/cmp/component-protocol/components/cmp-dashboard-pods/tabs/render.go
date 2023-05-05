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

package tabs

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

func (t *Tabs) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := t.GenComponentState(c); err != nil {
		return fmt.Errorf("failed to gen tabs component state, %v", err)
	}
	if event.Operation == cptype.InitializeOperation {
		t.State.Value = "cpu"
	}

	t.Props.ButtonStyle = "solid"
	t.Props.Options = []Option{
		{
			Key:  "cpu",
			Text: cputil.I18n(ctx, "cpu-analysis"),
		},
		{
			Key:  "mem",
			Text: cputil.I18n(ctx, "mem-analysis"),
		},
	}
	t.Props.RadioType = "button"
	t.Props.Size = "small"
	t.Operations = map[string]interface{}{
		"onChange": Operation{
			Key:    "changeTab",
			Reload: true,
		},
	}
	t.Transfer(c)
	return nil
}

func (t *Tabs) GenComponentState(component *cptype.Component) error {
	if component == nil || component.State == nil {
		return nil
	}
	var state State
	data, err := json.Marshal(component.State)
	if err != nil {
		return err
	}
	if err = json.Unmarshal(data, &state); err != nil {
		return err
	}
	t.State = state
	return nil
}

func (t *Tabs) Transfer(component *cptype.Component) {
	component.Props = cputil.MustConvertProps(t.Props)
	component.State = map[string]interface{}{
		"value": t.State.Value,
	}
	component.Operations = t.Operations
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-pods", "tabs", func() servicehub.Provider {
		return &Tabs{}
	})
}
