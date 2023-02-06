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

package addPodButton

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

func init() {
	base.InitProviderWithCreator("cmp-dashboard-pods", "addPodButton", func() servicehub.Provider {
		return &ComponentAddPodButton{}
	})
}

func (b *ComponentAddPodButton) Render(ctx context.Context, component *cptype.Component, _ cptype.Scenario,
	event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	b.InitComponent(ctx)
	b.Props.Text = b.sdk.I18n("createPod")
	b.Props.Type = "primary"
	b.Props.PrefixIcon = "add"
	b.Operations = map[string]interface{}{
		"click": Operation{
			Key:    "addPod",
			Reload: true,
		},
	}
	if event.Operation.String() == "addPod" {
		(*gs)["drawerOpen"] = true
	}
	b.Transfer(component)
	return nil
}

func (b *ComponentAddPodButton) InitComponent(ctx context.Context) {
	sdk := cputil.SDK(ctx)
	b.sdk = sdk
}

func (b *ComponentAddPodButton) Transfer(c *cptype.Component) {
	c.Props = cputil.MustConvertProps(b.Props)
	c.Operations = b.Operations
}
