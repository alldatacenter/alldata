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

package burnoutChart

import (
	"context"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "burnoutChart", func() servicehub.Provider {
		return &BurnoutChart{}
	})
}

func (f *BurnoutChart) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	h := gshelper.NewGSHelper(gs)

	dates := make([]time.Time, 0)
	dateMap := make(map[time.Time]int)
	itr := h.GetIteration()
	if itr.StartedAt == nil || itr.FinishedAt == nil {
		return nil
	}
	for rd := common.RangeDate(*itr.StartedAt, *itr.FinishedAt); ; {
		date := rd()
		if date.IsZero() {
			break
		}
		dates = append(dates, date)
		dateMap[date] = 0
	}
	if len(dates) == 0 {
		return fmt.Errorf("iterate over no time range selected")
	}

	issueFinishMap := make(map[time.Time][]dao.IssueItem, 0)
	sum := 0
	types := h.GetBurnoutChartType()
	if len(types) == 0 {
		types = []string{"requirement", "task"}
	}
	for _, issue := range h.GetIssueList() {
		if !strutil.InSlice(strings.ToLower(issue.Type), types) {
			continue
		}
		if issue.FinishTime != nil {
			issueFinishMap[common.DateTime(*issue.FinishTime)] = append(issueFinishMap[common.DateTime(*issue.FinishTime)], issue)
		}
		if h.GetBurnoutChartDimension() == "total" {
			sum++
		} else {
			workTime, err := sumWorkTime(issue)
			if err != nil {
				return err
			}
			sum += workTime
		}
		f.Issues = append(f.Issues, issue)
	}

	// Deal with situations that have been completed before the iteration begins
	finishSumBeforeIterBegin := 0
	for k, issues := range issueFinishMap {
		if k.Before(dates[0]) {
			finishSumBeforeIterBegin += func() int {
				if h.GetBurnoutChartDimension() == "total" {
					return len(issues)
				}
				sumHour := 0
				for _, issue := range issues {
					workTime, err := sumWorkTime(issue)
					if err != nil {
						continue
					}
					sumHour += workTime
				}
				return sumHour
			}()
		}
	}

	finishSumAll := finishSumBeforeIterBegin
	for _, date := range dates {
		finishSum := 0
		if _, ok := issueFinishMap[date]; ok {
			if h.GetBurnoutChartDimension() == "total" {
				finishSum += len(issueFinishMap[date])
			}
			if h.GetBurnoutChartDimension() == "workTime" {
				for _, issue := range issueFinishMap[date] {
					workTime, err := sumWorkTime(issue)
					if err != nil {
						return err
					}
					finishSum += workTime
				}
			}
		}
		finishSumAll += finishSum
		dateMap[date] = func() int {
			if h.GetBurnoutChartDimension() == "total" {
				return sum - finishSumAll
			}
			if sum-finishSumAll < 0 {
				return 0
			}
			return (sum - finishSumAll) / 60
		}()
	}

	f.Type = "Chart"
	f.Props = Props{
		ChartType: "line",
		Title:     cputil.I18n(ctx, "burnDown"),
		PureChart: true,
		Option: Option{
			XAxis: XAxis{
				Type: "category",
				Data: func() []string {
					ss := make([]string, 0, len(dates)+1)
					ss = append(ss, cputil.I18n(ctx, "before"))
					for _, v := range dates {
						ss = append(ss, v.Format("01-02"))
					}
					return ss
				}(),
			},
			YAxis: YAxis{
				Type: "value",
				AxisLine: map[string]interface{}{
					"lineStyle": map[string]interface{}{
						"color": "rgba(48,38,71,0.30)",
					},
				},
				AxisLabel: map[string]interface{}{
					"formatter": func() string {
						if h.GetBurnoutChartDimension() == "total" {
							return "{value} 个"
						}
						return "{value} h"
					}(),
				},
				Max: func() string {
					if h.GetBurnoutChartDimension() == "total" {
						return strconv.Itoa(sum)
					} else {
						return strconv.Itoa(sum / 60)
					}
				}(),
			},
			Legend: Legend{
				Show:   true,
				Bottom: true,
				Data:   []string{cputil.I18n(ctx, "remain"), cputil.I18n(ctx, "ideal")},
			},
			Tooltip: map[string]interface{}{
				"trigger": "axis",
			},
			Series: []Series{
				{
					Data: func() []string {
						counts := make([]string, 0, len(dates)+1)
						counts = append(counts, strconv.Itoa(getSum(sum, h.GetBurnoutChartDimension())))
						for _, v := range dates {
							counts = append(counts, strconv.Itoa(dateMap[v]))
						}
						return counts
					}(),
					Name: func() string {
						if h.GetBurnoutChartDimension() == "total" {
							return cputil.I18n(ctx, "remain")
						}
						return cputil.I18n(ctx, "remainHour")
					}(),
					Type:   "line",
					Smooth: false,
					ItemStyle: map[string]interface{}{
						"color": "#D84B65",
					},
				},
				{
					Data: func() []string {
						counts := make([]string, 0, len(dates)+1)
						idealSum := getSum(sum, h.GetBurnoutChartDimension())
						// the before point
						counts = append(counts, strconv.Itoa(idealSum))
						weekDays := getWeekDays(dates)
						lastSum := float64(idealSum)
						var weekCount int
						for i := range dates {
							if isWeekend(dates[i]) {
								counts = append(counts, fmt.Sprintf("%.0f", lastSum))
								continue
							}
							weekCount++
							currentSum := float64(idealSum*(weekDays-weekCount)) / float64(weekDays)
							counts = append(counts, fmt.Sprintf("%.0f", currentSum))
							lastSum = currentSum
						}
						return counts
					}(),
					Name: func() string {
						if h.GetBurnoutChartDimension() == "total" {
							return cputil.I18n(ctx, "ideal")
						}
						return cputil.I18n(ctx, "idealHour")
					}(),
					Type:   "line",
					Smooth: false,
					ItemStyle: map[string]interface{}{
						"color": "rgba(48,38,71,0.20)",
					},
					LineStyle: map[string]interface{}{
						"type": "dashed",
					},
				},
			},
		},
	}

	return f.SetToProtocolComponent(c)
}

func getSum(sum int, burnoutChartDimension string) int {
	if burnoutChartDimension == "total" {
		return sum
	} else {
		return sum / 60
	}
}

func sumWorkTime(issue dao.IssueItem) (int, error) {
	if issue.ManHour == "" {
		return 0, nil
	}

	var manHour apistructs.IssueManHour
	if err := json.Unmarshal([]byte(issue.ManHour), &manHour); err != nil {
		return 0, err
	}
	return int(manHour.ElapsedTime), nil
}

func isWeekend(t time.Time) bool {
	return t.Weekday() == time.Saturday || t.Weekday() == time.Sunday
}

func getWeekDays(dates []time.Time) int {
	days := 0
	for i := range dates {
		if !isWeekend(dates[i]) {
			days++
		}
	}
	return days
}
