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

package at_rate_passed

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/overview_group/blocks/at/pkg"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "at_rate_passed", func() servicehub.Provider {
		return &Text{}
	})
}

type Text struct {
}

func (t *Text) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	h := gshelper.NewGSHelper(gs)
	tv := pkg.TextValue{
		Value: func() string {
			var (
				successApiNum, totalApiNum int64
				passRate                   float64
			)
			for _, v := range h.GetAtBlockFilterTestPlanList() {
				successApiNum += v.SuccessApiNum
				totalApiNum += v.TotalApiNum
			}
			if totalApiNum == 0 {
				passRate = 0.00
			} else {
				passRate = float64(successApiNum) / float64(totalApiNum) * 100
			}
			return fmt.Sprintf("%.2f", passRate) + "%"
		}(),
		Kind: cputil.I18n(ctx, "test-case-rate-passed"),
	}
	c.Props = tv.ConvertToProps()
	return nil
}
