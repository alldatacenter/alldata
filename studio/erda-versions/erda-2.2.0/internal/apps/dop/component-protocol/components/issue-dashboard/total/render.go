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

package total

import (
	"context"
	"encoding/json"
	"strconv"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

type ComponentAction struct {
	common.OverviewProps `json:"props,omitempty"`
	State                State `json:"state,omitempty"`
}

type State struct {
	Stats common.Stats `json:"stats,omitempty"`
}

func init() {
	base.InitProviderWithCreator("issue-dashboard", "total",
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

	f.DataRetriever(ctx, gs)
	return f.SetToProtocolComponent(c)
}

func (f *ComponentAction) DataRetriever(ctx context.Context, gs *cptype.GlobalStateData) {
	helper := gshelper.NewGSHelper(gs)
	issueList := helper.GetIssueList()
	total := len(issueList)
	f.OverviewProps = common.OverviewProps{
		RenderType: "linkText",
		Value: common.OverviewValue{
			Direction: "col",
			Text: []common.OverviewText{
				{
					Text: strconv.Itoa(total),
					StyleConfig: common.StyleConfig{
						FontSize: 20,
						Bold:     true,
						Color:    "text-main",
					},
				},
				{
					Text: cputil.I18n(ctx, "totalBug"),
					StyleConfig: common.StyleConfig{
						Color: "text-desc",
					},
				},
			},
		},
	}
	f.State.Stats = StatsRetriever(issueList)
}

func StatsRetriever(issueList []dao.IssueItem) common.Stats {
	var open, expire, today, tomorrow, week, month, undefined, reopen int
	for _, i := range issueList {
		if i.ReopenCount > 0 {
			reopen += 1
		}
		if i.Belong == string(apistructs.IssueStateBelongClosed) {
			continue
		}
		open += 1
		switch i.ExpiryStatus {
		case dao.ExpireTypeExpired:
			expire += 1
		case dao.ExpireTypeExpireIn1Day:
			today += 1
		case dao.ExpireTypeExpireIn2Days:
			tomorrow += 1
		case dao.ExpireTypeExpireIn7Days:
			week += 1
		case dao.ExpireTypeExpireIn30Days:
			month += 1
		case dao.ExpireTypeUndefined:
			undefined += 1
		}
	}

	return common.Stats{
		Open:      strconv.Itoa(open),
		Expire:    strconv.Itoa(expire),
		Today:     strconv.Itoa(today),
		Tomorrow:  strconv.Itoa(tomorrow),
		Week:      strconv.Itoa(week),
		Month:     strconv.Itoa(month),
		Undefined: strconv.Itoa(undefined),
		Reopen:    strconv.Itoa(reopen),
	}
}
