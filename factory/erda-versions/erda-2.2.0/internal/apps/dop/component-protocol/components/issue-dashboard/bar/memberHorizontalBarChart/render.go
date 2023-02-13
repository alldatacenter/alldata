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

package memberHorizontalBarChart

import (
	"context"
	"encoding/json"

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
	base.InitProviderWithCreator("issue-dashboard", "memberHorizontalBarChart",
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

	members := helper.GetMembers()
	memberMap := make(map[string]*apistructs.Member)
	for i := range members {
		m := &members[i]
		memberMap[m.UserID] = m
	}

	issueList := helper.GetIssueList()
	var bugList []interface{}
	for i := range issueList {
		bugList = append(bugList, &issueList[i])
	}

	handler := stackhandlers.NewStackRetriever(
		stackhandlers.WithIssueStateList(helper.GetIssueStateList()),
		stackhandlers.WithIssueStageList(helper.GetIssueStageList()),
	).GetRetriever(f.State.Values.Type)

	if c.Name == "reopenByOwner" {
		handler = stackhandlers.NewStackRetriever().GetRetriever(cputil.I18n(ctx, "numReopen"))
	}

	builder := &chartbuilders.BarBuilder{
		Items:        bugList,
		StackHandler: handler,
		FixedXAxisOrTop: chartbuilders.FixedXAxisOrTop{
			Top:      500,
			XIndexer: getXIndexer(c),
			XDisplayConverter: func(opt *chartbuilders.FixedXAxisOrTop) opts.XAxis {
				return opts.XAxis{
					Type: "value",
					Max:  opt.MaxValue,
				}
			},
		},
		YAxisOpt: chartbuilders.YAxisOpt{
			YDisplayConverter: func(opt *chartbuilders.YAxisOpt) opts.YAxis {
				var names []string
				for _, userID := range opt.YAxis {
					var name string
					m, ok := memberMap[userID]
					if ok && m != nil {
						name = m.Nick
					} else {
						name = cputil.I18n(ctx, "user-not-exist")
					}
					names = append(names, name)
				}
				return opts.YAxis{
					Type: "category",
					Data: names,
				}
			},
		},
		StackOpt: chartbuilders.StackOpt{
			SkipEmpty: true,
		},
		DataHandleOpt: chartbuilders.DataHandleOpt{
			SeriesConverter: chartbuilders.GetStackBarSingleSeriesConverter(),
			DataWhiteList:   f.State.Values.Value,
			StatsProperty:   getStatesProperty(c),
		},
		Result: chartbuilders.Result{
			PostProcessor: chartbuilders.GetHorizontalPostProcessor(),
		},
	}

	if err := builder.Generate(ctx); err != nil {
		return err
	}

	props := make(map[string]interface{})
	switch c.Name {
	case "owner":
		props["title"] = cputil.I18n(ctx, "ownerBarChartTitle")
	case "reopenByOwner":
		props["title"] = cputil.I18n(ctx, "ownerReopenBarChartTitle")
	}
	props["chartType"] = "bar"
	props["option"] = builder.Result.Bb
	props["style"] = map[string]interface{}{"height": 400}

	c.Props = props
	return nil
}

func getXIndexer(c *cptype.Component) func(interface{}) string {
	return func(item interface{}) string {
		switch c.Name {
		case "owner", "reopenByOwner":
			return item.(*dao.IssueItem).Owner
		default:
			return ""
		}
	}
}

func getStatesProperty(c *cptype.Component) func(interface{}) int {
	return func(item interface{}) int {
		switch c.Name {
		case "reopenByOwner":
			return item.(*dao.IssueItem).ReopenCount
		default:
			return 1
		}
	}
}
