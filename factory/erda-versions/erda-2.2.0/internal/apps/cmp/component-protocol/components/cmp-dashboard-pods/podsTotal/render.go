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

package PodTitle

import (
	"context"
	"reflect"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

func (p *PodsTotal) Render(ctx context.Context, c *cptype.Component, s cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if gs == nil {
		return nil
	}
	countValues, ok := (*gs)["countValues"].(map[string]int)
	if !ok {
		logrus.Errorf("invalid count values type: %v", reflect.TypeOf((*gs)["countValues"]))
		return nil
	}

	total := 0
	for _, count := range countValues {
		total += count
	}
	p.Data.Data.Main = total
	p.Data.Data.Desc = cputil.I18n(ctx, "podNum")
	p.Transfer(c)
	return nil
}

func (p *PodsTotal) Transfer(component *cptype.Component) {
	component.Data = map[string]interface{}{
		"data": p.Data.Data,
	}
}

func init() {
	base.InitProviderWithCreator("cmp-dashboard-pods", "podsTotal", func() servicehub.Provider {
		return &PodsTotal{}
	})
}
