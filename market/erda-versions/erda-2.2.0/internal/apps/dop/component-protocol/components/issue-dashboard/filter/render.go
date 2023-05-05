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
	"encoding/base64"
	"encoding/json"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func init() {
	base.InitProviderWithCreator("issue-dashboard", "filter",
		func() servicehub.Provider { return &ComponentFilter{} })
}

func (f *ComponentFilter) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
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
	f.issueSvc = ctx.Value(types.IssueService).(query.Interface)
	if err := f.setInParams(ctx); err != nil {
		return err
	}

	return nil
}

func (f *ComponentFilter) setInParams(ctx context.Context) error {
	b, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &f.InParams); err != nil {
		return err
	}

	if f.InParams.FrontendFixedIteration != "" {
		f.InParams.IterationID, err = strconv.ParseInt(f.InParams.FrontendFixedIteration, 10, 64)
		if err != nil {
			return err
		}
	}
	f.InParams.ProjectID, err = strconv.ParseUint(f.InParams.FrontEndProjectID, 10, 64)
	return err
}

// func (f *ComponentFilter) GenComponentState(c *cptype.Component) error {
// 	if c == nil || c.State == nil {
// 		return nil
// 	}
// 	var state State
// 	cont, err := json.Marshal(c.State)
// 	if err != nil {
// 		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
// 		return err
// 	}
// 	err = json.Unmarshal(cont, &state)
// 	if err != nil {
// 		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
// 		return err
// 	}
// 	f.State = state
// 	return nil
// }

func (f *ComponentFilter) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &c); err != nil {
		return err
	}
	return nil
}

func (f *ComponentFilter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	iterations, iterationOptions, err := f.getPropIterationsOptions()
	if err != nil {
		return err
	}

	if len(iterations) == 0 {
		start := time.Now().AddDate(0, -1, 0)
		end := time.Now()
		iterations[-1] = apistructs.Iteration{
			StartedAt:  &start,
			FinishedAt: &end,
		}
	}

	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		if err := f.InitDefaultOperation(ctx, iterations); err != nil {
			return err
		}
	case cptype.OperationKey(f.Operations[OperationKeyFilter].Key):
	}

	if f.InParams.IterationID != 0 {
		f.State.Values.IterationIDs = []int64{f.InParams.IterationID}
	}
	data, err := f.issueSvc.GetAllIssuesByProject(pb.IssueListRequest{
		Type: []string{
			pb.IssueTypeEnum_BUG.String(),
		},
		ProjectID:    f.InParams.ProjectID,
		IterationIDs: f.State.Values.IterationIDs,
		Assignee:     f.State.Values.AssigneeIDs,
		// StateBelongs: []apistructs.IssueStateBelong{apistructs.IssueStateBelongOpen, apistructs.IssueStateBelongWorking, apistructs.IssueStateBelongWontfix, apistructs.IssueStateBelongReopen, apistructs.IssueStateBelongResolved},
	})
	if err != nil {
		return err
	}

	projectMemberOptions, err := f.getProjectMemberOptions()
	if err != nil {
		return err
	}

	f.State.Conditions = []filter.PropCondition{
		{
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "iteration",
			Label:     cputil.I18n(ctx, "iteration"),
			Options:   iterationOptions,
			Type:      filter.PropConditionTypeSelect,
			// Required:  true,
			// CustomProps: map[string]interface{}{
			// 	"mode": "single",
			// },
			HaveFilter: true,
			Disabled:   f.InParams.IterationID != 0,
		},
		{
			EmptyText:  cputil.I18n(ctx, "all"),
			Fixed:      true,
			Key:        "member",
			Label:      cputil.I18n(ctx, "member"),
			Options:    projectMemberOptions,
			Type:       filter.PropConditionTypeSelect,
			HaveFilter: true,
		},
	}

	// todo modify data format
	f.IssueList = data

	states, err := f.issueSvc.GetIssuesStatesByProjectID(f.InParams.ProjectID, pb.IssueTypeEnum_BUG.String())
	if err != nil {
		return err
	}
	f.IssueStateList = states

	orgID, err := strconv.Atoi(f.sdk.Identity.OrgID)
	if err != nil {
		return err
	}

	stages, err := f.issueSvc.GetIssueStage(&pb.IssueStageRequest{
		OrgID:     int64(orgID),
		IssueType: pb.IssueTypeEnum_BUG.String(),
	})
	if err != nil {
		return err
	}
	f.Stages = stages

	helper := gshelper.NewGSHelper(gs)
	helper.SetIterations(f.Iterations)
	helper.SetMembers(f.Members)
	helper.SetIssueList(f.IssueList)
	helper.SetIssueStateList(f.IssueStateList)
	helper.SetIssueStageList(f.Stages)

	urlParam, err := f.generateUrlQueryParams()
	if err != nil {
		return err
	}
	f.State.Base64UrlQueryParams = urlParam
	return f.SetToProtocolComponent(c)
}

func (f *ComponentFilter) InitDefaultOperation(ctx context.Context, iterations map[int64]apistructs.Iteration) error {
	f.Props = filter.Props{
		Delay: 1000,
	}
	f.Operations = map[filter.OperationKey]filter.Operation{
		OperationKeyFilter: {
			Key:    OperationKeyFilter,
			Reload: true,
		},
	}
	if f.InParams.FrontendUrlQuery != "" {
		b, err := base64.StdEncoding.DecodeString(f.InParams.FrontendUrlQuery)
		if err != nil {
			return err
		}
		f.State.Values = common.FrontendConditions{}
		if err := json.Unmarshal(b, &f.State.Values); err != nil {
			return err
		}
	} else {
		// f.State.Values.IterationIDs = defaultIterationRetriever(iterations)
	}
	return nil
}

func defaultIterationRetriever(iterations map[int64]apistructs.Iteration) int64 {
	for i, iteration := range iterations {
		if !time.Now().Before(*iteration.StartedAt) && !time.Now().After(*iteration.FinishedAt) {
			return i
		}
	}
	return -1
}

func (f *ComponentFilter) generateUrlQueryParams() (string, error) {
	fb, err := json.Marshal(f.State.Values)
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(fb), nil
}

func (f *ComponentFilter) getPropIterationsOptions() (map[int64]apistructs.Iteration, []filter.PropConditionOption, error) {
	iterations, err := f.bdl.ListProjectIterations(apistructs.IterationPagingRequest{PageNo: 1, PageSize: 1000, ProjectID: f.InParams.ProjectID, WithoutIssueSummary: true}, f.sdk.Identity.OrgID)
	if err != nil {
		return nil, nil, err
	}
	f.Iterations = append(iterations, apistructs.Iteration{
		ID:    -1,
		Title: f.sdk.I18n("iterationUnassigned"),
	})
	var options []filter.PropConditionOption
	iterationMap := make(map[int64]apistructs.Iteration)
	for _, iteration := range iterations {
		options = append(options, filter.PropConditionOption{
			Label: iteration.Title,
			Value: iteration.ID,
		})
		iterationMap[iteration.ID] = iteration
	}
	return iterationMap, options, nil
}

func (f *ComponentFilter) getProjectMemberOptions() ([]filter.PropConditionOption, error) {
	members, err := f.bdl.ListMembers(apistructs.MemberListRequest{
		ScopeType: apistructs.ProjectScope,
		ScopeID:   int64(f.InParams.ProjectID),
		PageNo:    1,
		PageSize:  500,
	})
	if err != nil {
		return nil, err
	}
	f.Members = members
	var results []filter.PropConditionOption
	for _, member := range members {
		results = append(results, filter.PropConditionOption{
			Label: func() string {
				if member.Nick != "" {
					return member.Nick
				}
				if member.Name != "" {
					return member.Name
				}
				return member.Mobile
			}(),
			Value: member.UserID,
		})
	}
	return results, nil
}
