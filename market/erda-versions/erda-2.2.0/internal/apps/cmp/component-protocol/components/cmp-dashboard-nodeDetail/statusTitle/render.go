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

package statusTitle

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
)

func (statusTitle *StatusTitle) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	statusTitle.CtxBdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	statusTitle.Ctx = ctx
	statusTitle.SDK = cputil.SDK(ctx)
	statusTitle.Props.Title = statusTitle.SDK.I18n("nodeStatus")
	c.Props = cputil.MustConvertProps(statusTitle.Props)
	return nil
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodeDetail", "statusTitle", func() servicehub.Provider {
		return &StatusTitle{Type: "Title", Props: Props{
			Size: "small",
		}}
	})
}
