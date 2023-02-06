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

package labelHorizontalBarChart

import (
	"context"
	"encoding/json"
	"strconv"

	"github.com/go-echarts/go-echarts/v2/opts"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/chartbuilders"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/model"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/stackhandlers"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func init() {
	base.InitProviderWithCreator("issue-dashboard", "labelHorizontalBarChart",
		func() servicehub.Provider { return &ComponentAction{} })
}

func (f *ComponentAction) setInParams(ctx context.Context) error {
	b, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &f.InParams); err != nil {
		return err
	}

	f.InParams.ProjectID, err = strconv.ParseUint(f.InParams.FrontEndProjectID, 10, 64)
	return err
}

func (f *ComponentAction) getState(c *cptype.Component) error {
	d, err := json.Marshal(c.State)
	if err != nil {
		return err
	}
	return json.Unmarshal(d, &f.State)
}

func (f *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	f.issueSvc = ctx.Value(types.IssueService).(query.Interface)
	if err := f.setInParams(ctx); err != nil {
		return err
	}

	if err := f.getState(c); err != nil {
		return err
	}

	helper := gshelper.NewGSHelper(gs)

	issueList := helper.GetIssueList()
	bugMap := make(map[uint64]*dao.IssueItem)
	for i := range issueList {
		issue := issueList[i]
		bugMap[issue.ID] = &issue
	}

	labels, err := f.issueSvc.GetIssueLabelsByProjectID(f.InParams.ProjectID)
	if err != nil {
		return err
	}
	var labelList []interface{}
	for i := range labels {
		l := &labels[i]
		refID, err := strconv.ParseUint(l.RefID, 10, 64)
		if err != nil {
			logrus.Errorf("failed to parse refID for label %s(ID: %d), %v", l.Name, l.ID, err)
			continue
		}
		bug, ok := bugMap[refID]
		if !ok {
			continue
		}
		labelList = append(labelList, &model.LabelIssueItem{
			LabelRel: l,
			Bug:      bug,
		})
	}

	handler := stackhandlers.NewStackRetriever(
		stackhandlers.WithIssueStateList(helper.GetIssueStateList()),
		stackhandlers.WithIssueStageList(helper.GetIssueStageList()),
	).GetRetriever(f.State.Values.Type)

	builder := &chartbuilders.BarBuilder{
		Items:        labelList,
		StackHandler: handler,
		FixedXAxisOrTop: chartbuilders.FixedXAxisOrTop{
			Top:      500,
			XIndexer: getXIndexer(),
			XDisplayConverter: func(opt *chartbuilders.FixedXAxisOrTop) opts.XAxis {
				return opts.XAxis{
					Type: "value",
					Max:  opt.MaxValue,
				}
			},
		},
		YAxisOpt: chartbuilders.YAxisOpt{
			YDisplayConverter: func(opt *chartbuilders.YAxisOpt) opts.YAxis {
				return opts.YAxis{
					Type: "category",
					Data: opt.YAxis,
				}
			},
		},
		StackOpt: chartbuilders.StackOpt{
			SkipEmpty: true,
		},
		DataHandleOpt: chartbuilders.DataHandleOpt{
			SeriesConverter: chartbuilders.GetStackBarSingleSeriesConverter(),
			DataWhiteList:   f.State.Values.Value,
		},
		Result: chartbuilders.Result{
			PostProcessor: chartbuilders.GetHorizontalPostProcessor(),
		},
	}

	if err := builder.Generate(ctx); err != nil {
		return err
	}

	props := make(map[string]interface{})
	props["title"] = cputil.I18n(ctx, "labelBarChartTitle")
	props["chartType"] = "bar"
	props["option"] = builder.Result.Bb
	props["style"] = map[string]interface{}{"height": 400}

	c.Props = props
	c.State = nil
	return nil
}

func getXIndexer() func(interface{}) string {
	return func(item interface{}) string {
		l := item.(*model.LabelIssueItem)
		if l == nil {
			return ""
		}
		return l.LabelRel.Name
	}
}
