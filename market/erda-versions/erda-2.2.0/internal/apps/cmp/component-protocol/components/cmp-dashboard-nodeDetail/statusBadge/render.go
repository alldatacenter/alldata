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

package statusBadge

import (
	"context"
	"sort"

	"github.com/rancher/wrangler/pkg/data"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/component-protocol/types"
)

func (statusBadge *StatusBadge) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	statusBadge.CtxBdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	statusBadge.Ctx = ctx
	statusBadge.SDK = cputil.SDK(ctx)
	statuses := map[string][]string{}
	node := (*gs)["node"].(data.Object)
	conds := node.Slice("status", "conditions")
	for _, cond := range conds {
		t := cond.String("type")
		if t != "Ready" {
			statuses[t] = make([]string, 2)
			s := cond.String("status")
			if s == "False" {
				statuses[t][0] = "success"
			} else {
				statuses[t][0] = "error"
			}
			statuses[t][1] = cond.String("reason")
		}
	}
	bars := make([]Bar, 0)
	for k, v := range statuses {
		bars = append(bars, Bar{
			Text:    statusBadge.SDK.I18n(k) + "(" + k + ")",
			Status:  v[0],
			WhiteBg: true,
			Tip:     v[1],
		})
	}
	sort.Slice(bars, func(i, j int) bool {
		return bars[i].Text < bars[j].Text
	})
	c.Data = map[string]interface{}{"list": bars}
	c.Type = statusBadge.Type
	return nil
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-nodeDetail", "statusBadge", func() servicehub.Provider {
		return &StatusBadge{
			Type: "Badge",
		}
	})
}
