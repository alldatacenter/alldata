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

package stateVerticalBarChart

import (
	"context"
	"encoding/json"

	"github.com/go-echarts/go-echarts/v2/charts"
	"github.com/go-echarts/go-echarts/v2/opts"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/chartbuilders"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/stackhandlers"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func init() {
	base.InitProviderWithCreator("issue-dashboard", "stateVerticalBarChart",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (f *ComponentAction) getState(c *cptype.Component) error {
	d, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	return json.Unmarshal(d, &f.State)
}

func (f *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {

	if err := f.getState(c); err != nil {
		return err
	}

	helper := gshelper.NewGSHelper(gs)

	stateList := helper.GetIssueStateList()
	stateMap := make(map[uint64]*dao.IssueState)
	for i := range stateList {
		s := stateList[i]
		stateMap[s.ID] = &s
	}

	issueList := helper.GetIssueList()
	var bugList []interface{}
	for i := range issueList {
		bugList = append(bugList, &issueList[i])
	}

	iterationList := helper.GetIterations()
	iterationMap := make(map[int64]*apistructs.Iteration)
	for i := range iterationList {
		s := iterationList[i]
		iterationMap[s.ID] = &s
	}

	selectedMap := make(map[int64]bool)
	for _, i := range f.State.FilterValues.IterationIDs {
		selectedMap[i] = true
	}

	handler := stackhandlers.NewStackRetriever(
		stackhandlers.WithIssueStageList(helper.GetIssueStageList()),
		stackhandlers.WithIssueStateList(helper.GetIssueStateList()),
	).GetRetriever(f.State.Values.Type)

	builder := &chartbuilders.BarBuilder{
		Items:        bugList,
		StackHandler: handler,
		FixedXAxisOrTop: chartbuilders.FixedXAxisOrTop{
			XAxis: func() []string {
				var xAxis []string
				switch c.Name {
				case "iteration":
					for _, i := range iterationList {
						if len(selectedMap) > 0 {
							if _, ok := selectedMap[i.ID]; !ok {
								continue
							}
						}
						xAxis = append(xAxis, i.Title)
					}
				default:
					for _, i := range stateList {
						xAxis = append(xAxis, i.Name)
					}
				}
				return xAxis
			}(),
			XIndexer: func(item interface{}) string {
				switch c.Name {
				case "iteration":
					return iterationMap[item.(*dao.IssueItem).IterationID].Title
				default:
					return stateMap[uint64(item.(*dao.IssueItem).State)].Name
				}
			},
			XDisplayConverter: func(opt *chartbuilders.FixedXAxisOrTop) opts.XAxis {
				return opts.XAxis{
					Data: opt.XAxis,
				}
			},
		},
		StackOpt: chartbuilders.StackOpt{
			EnableSum: true,
			SkipEmpty: false,
		},
		DataHandleOpt: chartbuilders.DataHandleOpt{
			SeriesConverter: func(name string, data []*int) charts.SingleSeries {
				return charts.SingleSeries{
					Name: name,
					Data: data,
					Label: &opts.Label{
						Show:      true,
						Position:  "top",
						Formatter: "{c}",
					},
				}
			},
			DataWhiteList: f.State.Values.Value,
		},
		Result: chartbuilders.Result{
			PostProcessor: chartbuilders.GetVerticalBarPostProcessor(),
		},
	}

	if err := builder.Generate(ctx); err != nil {
		return err
	}

	props := make(map[string]interface{})
	switch c.Name {
	case "iteration":
		props["title"] = cputil.I18n(ctx, "iterationBarChartTitle")
	default:
		props["title"] = cputil.I18n(ctx, "stateBarChartTitle")
	}
	props["chartType"] = "bar"
	props["option"] = builder.Result.Bb
	if c.Name == "iteration" && len(selectedMap) == 1 {
		props["visible"] = false
	}

	c.Props = props
	c.State = nil
	return nil
}
