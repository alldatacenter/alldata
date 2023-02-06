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

package info

import (
	"context"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	cpcommon "github.com/erda-project/erda/internal/apps/dop/component-protocol/components/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/container/simpleChart"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/pkg/component-protocol/issueFilter"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "info", func() servicehub.Provider {
		return &Info{}
	})
}

func (i *Info) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := i.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	h := gshelper.NewGSHelper(gs)
	i.Issues = h.GetIssueList()

	conditions := h.GetIssueCondtions()
	issueSvc := ctx.Value(types.IssueService).(query.Interface)
	projectID, err := strconv.ParseUint(cputil.GetInParamByKey(ctx, "projectId").(string), 10, 64)
	if err != nil {
		return err
	}
	stateIDs, err := issueSvc.GetIssueStateIDsByTypes(&apistructs.IssueStatesRequest{
		ProjectID:    projectID,
		IssueType:    []apistructs.IssueType{apistructs.IssueTypeTask, apistructs.IssueTypeRequirement},
		StateBelongs: []apistructs.IssueStateBelong{apistructs.IssueStateBelongOpen, apistructs.IssueStateBelongWorking},
	})
	if err != nil {
		return err
	}
	stats := getStats(i.Issues)

	timeMilli := cpcommon.TimeMilliInDays{
		Today:               cpcommon.MilliFromTime(common.DateTime(time.Now())),
		Tomorrow:            cpcommon.MilliFromTime(common.DateTime(time.Now().AddDate(0, 0, 1))),
		TheDayAfterTomorrow: cpcommon.MilliFromTime(common.DateTime(time.Now().AddDate(0, 0, 2))),
		SevenDays:           cpcommon.MilliFromTime(common.DateTime(time.Now().AddDate(0, 0, 7))),
		ThirtyDays:          cpcommon.MilliFromTime(common.DateTime(time.Now().AddDate(0, 0, 30))),
	}
	req := ConditionsLinkRequest{
		Conditions: conditions,
		StateIDs:   stateIDs,
		TimeMilli:  timeMilli,
	}
	i.Data.Data = [][]Data{
		{
			{
				Main:     strconv.Itoa(stats.Unclose),
				Sub:      cputil.I18n(ctx, "unfinished"),
				MainLink: buildLink(req, dao.ExpireTypeUnfinished),
			},
			{
				Main:     strconv.Itoa(stats.Expire),
				Sub:      cputil.I18n(ctx, "expired"),
				MainLink: buildLink(req, dao.ExpireTypeExpired),
			},
			{
				Main:     strconv.Itoa(stats.Today),
				Sub:      cputil.I18n(ctx, "dueToday"),
				MainLink: buildLink(req, dao.ExpireTypeExpireIn1Day),
			},
			{
				Main:     strconv.Itoa(stats.Tomorrow),
				Sub:      cputil.I18n(ctx, "dueTomorrow"),
				MainLink: buildLink(req, dao.ExpireTypeExpireIn2Days),
			},
		},
		{
			{
				Main:     strconv.Itoa(stats.Week),
				Sub:      cputil.I18n(ctx, "dueThisWeek"),
				Tip:      cputil.I18n(ctx, "notIncludeDueToday"),
				MainLink: buildLink(req, dao.ExpireTypeExpireIn7Days),
			},
			{
				Main:     strconv.Itoa(stats.Month),
				Sub:      cputil.I18n(ctx, "dueThisMonth"),
				Tip:      cputil.I18n(ctx, "notIncludeDueTodayTomorrow"),
				MainLink: buildLink(req, dao.ExpireTypeExpireIn30Days),
			},
			{
				Main: strconv.Itoa(stats.Undefined),
				Sub:  cputil.I18n(ctx, "noDeadlineSpecified"),
				// MainLink: buildLink(dao.ExpireTypeUndefined, conditions),
			},
		},
	}

	return i.SetToProtocolComponent(c)
}

type ConditionsLinkRequest struct {
	Conditions issueFilter.FrontendConditions
	StateIDs   []int64
	TimeMilli  cpcommon.TimeMilliInDays
}

func buildLink(req ConditionsLinkRequest, expiryType dao.ExpireType) simpleChart.Link {
	conditions := req.Conditions
	switch expiryType {
	case dao.ExpireTypeExpired:
		yesterDayEndedAt := req.TimeMilli.Today - 1
		conditions.FinishedAtStartEnd = []*int64{nil, &yesterDayEndedAt}
	case dao.ExpireTypeExpireIn1Day:
		todayStartAt, todayEndAt := req.TimeMilli.Today, req.TimeMilli.Tomorrow-1
		conditions.FinishedAtStartEnd = []*int64{&todayStartAt, &todayEndAt}
	case dao.ExpireTypeExpireIn2Days:
		tomorrowEndAt := req.TimeMilli.TheDayAfterTomorrow - 1
		conditions.FinishedAtStartEnd = []*int64{&req.TimeMilli.Tomorrow, &tomorrowEndAt}
	case dao.ExpireTypeExpireIn7Days:
		sevenDaysEndAt := req.TimeMilli.SevenDays - 1
		conditions.FinishedAtStartEnd = []*int64{&req.TimeMilli.TheDayAfterTomorrow, &sevenDaysEndAt}
	case dao.ExpireTypeExpireIn30Days:
		thirtyDaysEndAt := req.TimeMilli.ThirtyDays - 1
		conditions.FinishedAtStartEnd = []*int64{&req.TimeMilli.SevenDays, &thirtyDaysEndAt}
	// case dao.ExpireTypeUndefined:
	// 	conditions.FinishedAtStartEnd = []*int64{nil, nil}
	case dao.ExpireTypeUnfinished:
	}
	conditions.States = req.StateIDs
	urlQuery, err := cpcommon.GenerateUrlQueryParams(conditions)
	if err != nil {
		logrus.Errorf("fail to get urlquery, conditions: %v", conditions)
		return simpleChart.Link{}
	}
	return simpleChart.Link{
		Target: common.IssueTarget,
		Params: map[string]interface{}{
			"issueFilter__urlQuery": urlQuery,
		},
	}
}

type Stats struct {
	Unclose   int `json:"unclose,omitempty"`
	Expire    int `json:"expire,omitempty"`
	Today     int `json:"today,omitempty"`
	Tomorrow  int `json:"tomorrow,omitempty"`
	Week      int `json:"week,omitempty"`
	Month     int `json:"month,omitempty"`
	Undefined int `json:"undefined,omitempty"`
}

func getStats(issues []dao.IssueItem) (s Stats) {
	for _, v := range issues {
		if v.FinishTime != nil {
			continue
		}
		s.Unclose++
		switch v.ExpiryStatus {
		case dao.ExpireTypeExpired:
			s.Expire++
		case dao.ExpireTypeExpireIn1Day:
			s.Today++
		case dao.ExpireTypeExpireIn2Days:
			s.Tomorrow++
		case dao.ExpireTypeExpireIn7Days:
			s.Week++
		case dao.ExpireTypeExpireIn30Days:
			s.Month++
		case dao.ExpireTypeUndefined:
			s.Undefined++
		}
	}
	return
}
