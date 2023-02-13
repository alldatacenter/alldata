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

package at_num_api

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/overview_group/blocks/at/pkg"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "at_num_api", func() servicehub.Provider {
		return &Text{}
	})
}

type Text struct {
}

func (t *Text) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	h := gshelper.NewGSHelper(gs)
	atSvc := ctx.Value(types.AutoTestPlanService).(*autotestv2.Service)

	stepCount, err := atSvc.CountAutoTestSceneSteps(func() []uint64 {
		selectPlans := h.GetAtBlockFilterTestPlanList()
		selectPlanIDs := make([]uint64, 0, len(selectPlans))
		for _, v := range selectPlans {
			selectPlanIDs = append(selectPlanIDs, v.ID)
		}
		return selectPlanIDs
	}())
	if err != nil {
		return err
	}
	tv := pkg.TextValue{
		Value: strutil.String(func() int {
			return stepCount.Count
		}()),
		Kind: cputil.I18n(ctx, "auto-test-api-num"),
	}
	c.Props = tv.ConvertToProps()
	return nil
}
