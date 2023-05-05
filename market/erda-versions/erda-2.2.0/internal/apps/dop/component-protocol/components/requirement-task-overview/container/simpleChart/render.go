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

package simpleChart

import (
	"context"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	cpcommon "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "simpleChart", func() servicehub.Provider {
		return &SimpleChart{}
	})
}

func (s *SimpleChart) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	h := gshelper.NewGSHelper(gs)

	if err := s.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	s.Issues = h.GetIssueList()
	s.Type = "SimpleChart"
	conditions := h.GetIssueCondtions()
	totalIssueUrlQuery, err := cpcommon.GenerateUrlQueryParams(conditions)
	if err != nil {
		return err
	}
	start, end := cpcommon.MilliFromTime(common.DateTime(time.Now())), cpcommon.MilliFromTime(common.DateTime(time.Now().AddDate(0, 0, 1)))-1
	conditions.CreatedAtStartEnd = []*int64{&start, &end}
	todayIssueUrlQuery, err := cpcommon.GenerateUrlQueryParams(conditions)
	if err != nil {
		return err
	}
	s.Data = Data{
		Main:        strconv.Itoa(len(s.Issues)),
		Sub:         cputil.I18n(ctx, "total"),
		CompareText: cputil.I18n(ctx, "comparedYesterday"),
		CompareValue: func() string {
			count := common.IssueCountIf(s.Issues, func(issue *dao.IssueItem) bool {
				return common.DateTime(issue.CreatedAt).Equal(common.DateTime(time.Now()))
			})
			if count > 0 {
				return "+" + strconv.Itoa(count)
			}
			return strconv.Itoa(count)
		}(),
		MainLink: Link{
			Target: common.IssueTarget,
			Params: map[string]interface{}{
				"issueFilter__urlQuery": totalIssueUrlQuery,
			},
		},
		CompareValueLink: Link{
			Target: common.IssueTarget,
			Params: map[string]interface{}{
				"issueFilter__urlQuery": todayIssueUrlQuery,
			},
		},
	}

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
		if date.After(common.DateTime(time.Now())) {
			break
		}
		dates = append(dates, date)
		dateMap[date] = 0
	}
	for k := range dateMap {
		for _, issue := range s.Issues {
			created := common.DateTime(issue.CreatedAt)
			if !created.After(common.DateTime(k)) {
				dateMap[k]++
			}
		}
	}
	s.Data.Chart = Chart{
		XAxis: func() []string {
			ss := make([]string, 0, len(dates))
			for _, v := range dates {
				ss = append(ss, v.Format("2006-01-02"))
			}
			return ss
		}(),
		Series: []SeriesData{
			{
				Name: "需求&任务总数",
				Data: func() []int {
					counts := make([]int, 0, len(dates))
					for _, v := range dates {
						counts = append(counts, dateMap[v])
					}
					return counts
				}(),
			},
		},
	}

	return s.SetToProtocolComponent(c)
}
