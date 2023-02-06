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

package chart

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "at_plan_latest_waterfall_chart", func() servicehub.Provider {
		return &Chart{}
	})
}

type Chart struct {
	Values     []string `json:"values"`
	Categories []string `json:"categories"`
}

func (f *Chart) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	h := gshelper.NewGSHelper(gs)

	steps := h.GetGlobalAtStep()
	sceneSetIDs := make([]uint64, 0, len(steps))
	sceneSetMap := make(map[uint64]string, 0)
	for _, v := range steps {
		sceneSetMap[v.SceneSetID] = v.SceneSetName
		sceneSetIDs = append(sceneSetIDs, v.SceneSetID)
	}

	atSvc := ctx.Value(types.AutoTestPlanService).(*autotestv2.Service)
	historyList, err := atSvc.ListExecHistorySceneSetByParentPID(h.GetWaterfallChartPipelineID())
	if err != nil {
		return err
	}
	var (
		indexes    []int64
		values     []int64
		categories []string
		offset     int64
	)
	if len(historyList) != 0 {
		offset = historyList[0].ExecuteTime.Unix()
	}
	for _, v := range historyList {
		indexes = append(indexes, v.ExecuteTime.Unix()-offset)
		values = append(values, v.CostTimeSec)
		categories = append(categories, sceneSetMap[v.SceneSetID])
	}

	c.Props = NewBarProps(indexes, values, categories, "")
	return nil
}
