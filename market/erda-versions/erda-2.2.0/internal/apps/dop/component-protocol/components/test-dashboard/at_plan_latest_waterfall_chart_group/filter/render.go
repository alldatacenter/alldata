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

package filter

import (
	"context"
	"encoding/json"
	"sort"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "at_plan_latest_waterfall_chart_filter", func() servicehub.Provider {
		return &Filter{}
	})
}

type Filter struct {
	State State `json:"state,omitempty"`
}

type State struct {
	Conditions []filter.PropCondition `json:"conditions,omitempty"`
	Values     PipelineIDValues       `json:"values,omitempty"`
	IsClick    bool                   `json:"isClick"`
}

type PipelineIDValues struct {
	PipelineID uint64 `json:"pipelineID"`
}

type AtPlanFilterStateValues struct {
	AtPlanIDs []uint64 `json:"atPlanIDs,omitempty"`
}

// Render is empty implement.
func (f *Filter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.initFromProtocol(ctx, c); err != nil {
		return err
	}

	h := gshelper.NewGSHelper(gs)
	data := h.GetSelectChartHistoryData()
	list := make([]gshelper.SelectChartItemData, 0, len(data))
	f.State.Conditions = []filter.PropCondition{
		{
			CustomProps: map[string]interface{}{
				"mode": "single",
			},
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "pipelineID",
			Label:     cputil.I18n(ctx, "Test Plan"),
			Options: func() (opts []filter.PropConditionOption) {
				for _, v := range data {
					list = append(list, v)
				}
				sort.Slice(list, func(i, j int) bool {
					return list[i].ExecuteTime > list[j].ExecuteTime
				})
				for _, v := range list {
					opts = append(opts, filter.PropConditionOption{
						Label: v.Name,
						Value: v.PipelineID,
					})
				}
				return
			}(),
			Type: filter.PropConditionTypeSelect,
		},
	}
	f.State.Values = PipelineIDValues{
		PipelineID: func() uint64 {
			if f.State.Values.PipelineID != 0 && !f.State.IsClick {
				return f.State.Values.PipelineID
			}
			if len(list) == 0 {
				return 0
			}
			return list[0].PipelineID
		}(),
	}
	h.SetWaterfallChartPipelineID(f.State.Values.PipelineID)

	// clear
	f.State.IsClick = false
	if err := f.setToComponent(c); err != nil {
		return err
	}
	return nil
}

func (f *Filter) initFromProtocol(ctx context.Context, c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, f); err != nil {
		return err
	}
	return nil
}

func (f *Filter) setToComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &c); err != nil {
		return err
	}
	return nil
}
