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

package trendChart

import (
	"context"
	"encoding/json"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	base.InitProviderWithCreator("issue-dashboard", "trendChart",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (f *ComponentAction) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
	// component 序列化
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, f); err != nil {
		return err
	}

	// sdk
	f.sdk = cputil.SDK(ctx)
	f.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	// if err := f.setInParams(ctx); err != nil {
	// 	return err
	// }

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
	c.State = nil
	return nil
}

func (f *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	helper := gshelper.NewGSHelper(gs)
	f.IssueList = helper.GetIssueList()
	// iterations := helper.GetIterations()
	f.ChartDataRetriever(ctx, f.State.Values.Time)
	return f.SetToProtocolComponent(c)
}

func rangeDate(start, end time.Time) func() time.Time {
	y, m, d := start.Date()
	start = time.Date(y, m, d, 0, 0, 0, 0, start.Location())
	y, m, d = end.Date()
	end = time.Date(y, m, d, 0, 0, 0, 0, end.Location())

	return func() time.Time {
		if start.After(end) {
			return time.Time{}
		}
		date := start
		start = start.AddDate(0, 0, 1)
		return date
	}
}

func timeFromMilli(millis int64) time.Time {
	return time.Unix(0, millis*int64(time.Millisecond))
}

func maxUInt(nums ...int) int {
	max := 0
	for _, n := range nums {
		if n > max {
			max = n
		}
	}
	return max
}

func (f *ComponentAction) ChartDataRetriever(ctx context.Context, timeRange []int64) {
	if len(timeRange) < 2 {
		return
	}
	start, end := timeFromMilli(timeRange[0]), timeFromMilli(timeRange[1])
	dates := make([]string, 0)
	cMap := make(map[time.Time][]int)
	for rd := rangeDate(start, end); ; {
		date := rd()
		if date.IsZero() {
			break
		}
		cMap[date] = []int{0, 0, 0}
	}

	first, last := [3]int{0, 0, 0}, [3]int{0, 0, 0}
	issues := common.IssueListRetriever(f.IssueList, func(i int) bool {
		v := f.IssueList[i].FilterPropertyRetriever(f.State.Values.Type)
		return f.State.Values.Value == nil || strutil.Exist(f.State.Values.Value, v)
	})
	for _, i := range issues {
		created := time.Date(i.CreatedAt.Year(), i.CreatedAt.Month(), i.CreatedAt.Day(), 0, 0, 0, 0, i.CreatedAt.Location())
		if created.Before(start) {
			first[0] += 1
		} else if created.After(end) {
			last[0] += 1
		} else {
			if _, ok := cMap[created]; ok {
				cMap[created][0] += 1
			}
		}

		if i.FinishTime != nil {
			closed := time.Date(i.FinishTime.Year(), i.FinishTime.Month(), i.FinishTime.Day(), 0, 0, 0, 0, i.FinishTime.Location())
			if closed.Before(start) {
				first[1] += 1
			} else if closed.After(end) {
				last[1] += 1
			} else {
				if _, ok := cMap[closed]; ok {
					cMap[closed][1] += 1
				}
			}
		} else {
			if i.Belong == string(apistructs.IssueStateBelongClosed) {
				first[1] += 1
			}
		}
	}

	newIssue, closedIssue, unClosedIssue := make([]int, 0), make([]int, 0), make([]int, 0)
	// dates = append(dates, "更早")
	// newIssue = append(newIssue, first[0])
	// closedIssue = append(closedIssue, first[1])
	// first[2] = first[0] - first[1]
	// unClosedIssue = append(unClosedIssue, first[2])
	// var maxIssue int
	for rd := rangeDate(start, end); ; {
		date := rd()
		if date.IsZero() {
			break
		}

		x := date.Format("1/2")
		dates = append(dates, x)
		newIssue = append(newIssue, cMap[date][0])
		closedIssue = append(closedIssue, cMap[date][1])
		unclose := cMap[date][0] - cMap[date][1]
		if len(unClosedIssue) == 0 {
			unclose += first[0] - first[1]
		} else {
			unclose += unClosedIssue[len(unClosedIssue)-1]
		}
		unClosedIssue = append(unClosedIssue, unclose)
		// maxIssue = maxUInt(maxIssue, cMap[date][0], cMap[date][1], unclose)
	}

	// dates = append(dates, "未来")
	// newIssue = append(newIssue, last[0])
	// closedIssue = append(closedIssue, last[1])
	// last[2] = unClosedIssue[len(unClosedIssue)-1] + last[0] - last[1]
	// unClosedIssue = append(unClosedIssue, last[2])
	label := common.Label{
		Normal: common.Normal{
			Position: "top",
			Show:     true,
		},
	}
	f.Chart = common.Chart{
		Props: common.Props{
			Title:     cputil.I18n(ctx, "trendChartTitle"),
			ChartType: "line",
			Option: common.Option{
				XAxis: common.XAxis{
					Data: dates,
				},
				YAxis: common.YAxis{
					// Max: float32(maxIssue) * 1.2,
				},
				Grid: common.Grid{
					Bottom: 30,
					Top:    20,
				},
				Legend: common.Legend{
					Show:         true,
					SelectedMode: false,
					Bottom:       0,
				},
				Color: []string{"blue", "green", "red"},
				Series: []common.Item{
					{
						Name: cputil.I18n(ctx, "trendNew"),
						Data: newIssue,
						AreaStyle: common.AreaStyle{
							Opacity: 0.1,
						},
						Label: label,
					},
					{
						Name: cputil.I18n(ctx, "trendClose"),
						Data: closedIssue,
						AreaStyle: common.AreaStyle{
							Opacity: 0.1,
						},
						Label: label,
					},
					{
						Name: cputil.I18n(ctx, "trendOpen"),
						Data: unClosedIssue,
						AreaStyle: common.AreaStyle{
							Opacity: 0.1,
						},
						Label: label,
					},
				},
			},
		},
	}
}
