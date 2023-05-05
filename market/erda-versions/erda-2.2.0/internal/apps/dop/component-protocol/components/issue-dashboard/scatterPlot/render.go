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

package scatterPlot

import (
	"context"
	"encoding/json"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	base.InitProviderWithCreator("issue-dashboard", "scatterPlot",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (f *ComponentAction) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, f); err != nil {
		return err
	}

	return nil
}

func (f *ComponentAction) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &c); err != nil {
		return err
	}
	return nil
}

func (f *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	helper := gshelper.NewGSHelper(gs)
	f.IssueList = helper.GetIssueList()
	issues := common.IssueListRetriever(f.IssueList, func(i int) bool {
		v := f.IssueList[i].FilterPropertyRetriever(f.State.Values.Type)
		return f.State.Values.Value == nil || strutil.Exist(f.State.Values.Value, v)
	})
	data := ScatterData(issues)
	f.Props = Props{
		Title: cputil.I18n(ctx, "scatterPlotTitle"),
		Option: Option{
			XAxis: common.XAxis{
				Type:  "value",
				Name:  cputil.I18n(ctx, "resolvedTime"),
				Scale: true,
				AxisLabel: common.AxisLabel{
					Fortmatter: "{value} " + cputil.I18n(ctx, "hour"),
				},
				SplitLine: common.SplitLine{
					Show: false,
				},
			},
			YAxis: common.YAxis{
				XAxis: common.XAxis{
					Type:  "value",
					Name:  cputil.I18n(ctx, "responseTime"),
					Scale: true,
					AxisLabel: common.AxisLabel{
						Fortmatter: "{value} " + cputil.I18n(ctx, "hour"),
					},
					SplitLine: common.SplitLine{
						Show: true,
					},
				},
			},
			Grid: common.Grid{
				Top:   40,
				Right: 70,
			},
			Series: []Series{
				{
					Type: "scatter",
					Name: "缺陷",
					Data: data,
					MarkPoint: common.MarkPoint{
						SymbolSize: 40,
						Data: []common.MarkItem{
							{
								Type: "max",
								Name: "最大值",
							},
							{
								Type: "min",
								Name: "最小值",
							},
						},
					},
					MarkLine: common.MarkLine{
						LineStyle: common.LineStyle{
							Type: "solid",
						},
						Data: []common.MarkItem{
							{
								Type:       "average",
								Name:       "平均值",
								ValueIndex: 0,
							},
							{
								Type:       "average",
								Name:       "平均值",
								ValueIndex: 1,
							},
						},
					},
				},
			},
		},
	}
	return f.SetToProtocolComponent(c)
}

func ScatterData(issues []dao.IssueItem) [][]float32 {
	data := make([][]float32, 0)
	for _, issue := range issues {
		if issue.FinishTime == nil || issue.StartTime == nil {
			continue
		}
		items := make([]float32, 0)
		solveTime := (issue.FinishTime.UnixNano() - issue.StartTime.UnixNano()) / int64(time.Millisecond)
		responseTime := (issue.StartTime.UnixNano() - issue.CreatedAt.UnixNano()) / int64(time.Millisecond)
		if solveTime < 0 || responseTime < 0 {
			continue
		}
		items = append(items, milliToHour(solveTime), milliToHour(responseTime))
		data = append(data, items)
	}

	return data
}

func milliToHour(m int64) float32 {
	return float32(m) / (1000 * 60 * 60)
}
