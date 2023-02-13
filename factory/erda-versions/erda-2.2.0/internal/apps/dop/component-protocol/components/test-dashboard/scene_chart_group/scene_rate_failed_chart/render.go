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

package scene_rate_failed_chart

import (
	"context"
	"sort"
	"strconv"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "scene_rate_failed_chart", func() servicehub.Provider {
		return &Chart{}
	})
}

type Chart struct {
	Values     []string `json:"values"`
	Categories []string `json:"categories"`
}

func (f *Chart) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	h := gshelper.NewGSHelper(gs)
	atSvc := ctx.Value(types.AutoTestPlanService).(*autotestv2.Service)

	timeFilter := h.GetAtSceneAndApiTimeFilter()
	projectID, _ := strconv.ParseUint(cputil.GetInParamByKey(ctx, "projectId").(string), 10, 64)
	statusCounts, err := atSvc.ExecHistorySceneStatusCount(apistructs.StatisticsExecHistoryRequest{
		TimeStart:    timeFilter.TimeStart,
		TimeEnd:      timeFilter.TimeEnd,
		IterationIDs: h.GetGlobalSelectedIterationIDs(),
		PlanIDs:      h.GetGlobalAutoTestPlanIDs(),
		SceneSetIDs:  nil,
		SceneIDs:     nil,
		StepIDs:      nil,
		ProjectID:    projectID,
	})
	if err != nil {
		return err
	}
	for i := range statusCounts {
		total := statusCounts[i].FailCount + statusCounts[i].SuccessCount
		if total == 0 {
			statusCounts[i].FailRate = 0
		} else {
			statusCounts[i].FailRate = float64(statusCounts[i].FailCount) / float64(total) * 100
		}
	}
	sort.Slice(statusCounts, func(i, j int) bool {
		return statusCounts[i].FailRate > statusCounts[j].FailRate
	})

	var (
		values     []int64
		categories []string
	)
	for i, v := range statusCounts {
		if i == 500 {
			break
		}
		values = append(values, int64(v.FailRate))
		categories = append(categories, v.Name)
	}

	c.Props = common.NewBarProps(values, categories, cputil.I18n(ctx, "scene-failed-rate"), "{value}%")
	return nil
}
