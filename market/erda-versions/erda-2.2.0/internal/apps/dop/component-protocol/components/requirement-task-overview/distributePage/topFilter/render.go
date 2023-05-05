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

package topFilter

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"sort"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/pkg/component-protocol/issueFilter"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "topFilter",
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
	return f.setInParams(ctx)
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

func (f *ComponentFilter) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, &c)
}

func (f *ComponentFilter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	iterations, iterationOptions, err := f.getPropIterationsOptions()
	if err != nil {
		return err
	}

	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		if err = f.InitDefaultOperation(ctx, f.Iterations); err != nil {
			return err
		}
	case cptype.OperationKey(f.Operations[OperationKeyFilter].Key):
	}

	if f.InParams.IterationID != 0 {
		f.State.Values.IterationID = f.InParams.IterationID
	}
	data, err := f.issueSvc.GetAllIssuesByProject(pb.IssueListRequest{
		Type: []string{
			pb.IssueTypeEnum_REQUIREMENT.String(),
			pb.IssueTypeEnum_TASK.String(),
		},
		ProjectID:    f.InParams.ProjectID,
		IterationIDs: []int64{f.State.Values.IterationID},
		Assignee:     f.State.Values.AssigneeIDs,
		Label:        f.State.Values.LabelIDs,
	})
	if err != nil {
		return err
	}

	projectMemberOptions, err := f.getProjectMemberOptions()
	if err != nil {
		return err
	}

	labelOptions, err := f.getProjectLabelsOptions()
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
			Required:  true,
			CustomProps: map[string]interface{}{
				"mode": "single",
			},
			HaveFilter: true,
			Disabled:   f.InParams.IterationID != 0,
		},
		{
			EmptyText:  cputil.I18n(ctx, "all"),
			Fixed:      true,
			Key:        "label",
			Label:      cputil.I18n(ctx, "label"),
			Options:    labelOptions,
			Type:       filter.PropConditionTypeSelect,
			HaveFilter: true,
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

	f.IssueList = data
	stateIDs, err := f.issueSvc.GetIssueStateIDsByTypes(&apistructs.IssueStatesRequest{
		ProjectID: f.InParams.ProjectID,
		IssueType: []apistructs.IssueType{apistructs.IssueTypeTask, apistructs.IssueTypeRequirement},
	})
	if err != nil {
		return err
	}
	conditions := issueFilter.FrontendConditions{
		IterationIDs: []int64{f.State.Values.IterationID},
		AssigneeIDs:  f.State.Values.AssigneeIDs,
		LabelIDs:     f.State.Values.LabelIDs,
		States:       stateIDs,
	}

	helper := gshelper.NewGSHelper(gs)
	helper.SetIteration(iterations[f.State.Values.IterationID])
	helper.SetMembers(f.Members)
	helper.SetIssueList(f.IssueList)
	helper.SetIssueConditions(conditions)

	urlParam, err := f.generateUrlQueryParams()
	if err != nil {
		return err
	}
	f.State.Base64UrlQueryParams = urlParam
	return f.SetToProtocolComponent(c)
}

func (f *ComponentFilter) InitDefaultOperation(ctx context.Context, iterations []apistructs.Iteration) error {
	f.Props = filter.Props{
		Delay: 300,
	}
	f.Operations = map[filter.OperationKey]filter.Operation{
		OperationKeyFilter: {
			Key:    OperationKeyFilter,
			Reload: true,
		},
		OperationOwnerSelectMe: {
			Key:    OperationOwnerSelectMe,
			Reload: true,
		},
	}
	if f.InParams.FrontendUrlQuery != "" {
		b, err := base64.StdEncoding.DecodeString(f.InParams.FrontendUrlQuery)
		if err != nil {
			return err
		}
		f.State.Values = common.FrontendConditions{}
		if err = json.Unmarshal(b, &f.State.Values); err != nil {
			return err
		}
	} else {
		f.State.Values.IterationID = defaultIterationRetriever(iterations)
	}
	return nil
}

func defaultIterationRetriever(iterations []apistructs.Iteration) int64 {
	sort.Slice(iterations, func(i, j int) bool {
		return iterations[i].ID > iterations[j].ID
	})
	for _, v := range iterations {
		if !time.Now().Before(*v.StartedAt) &&
			!time.Now().After(*v.FinishedAt) {
			return v.ID
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
		StartedAt: func() *time.Time {
			t := time.Now().AddDate(0, -1, 0)
			return &t
		}(),
		FinishedAt: func() *time.Time {
			t := time.Now()
			return &t
		}(),
	})
	var options []filter.PropConditionOption
	itrMap := make(map[int64]apistructs.Iteration)
	for _, iteration := range f.Iterations {
		options = append(options, filter.PropConditionOption{
			Label: iteration.Title,
			Value: iteration.ID,
		})
		itrMap[iteration.ID] = iteration
	}
	return itrMap, options, nil
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

func (f *ComponentFilter) getProjectLabelsOptions() ([]filter.PropConditionOption, error) {
	labels, err := f.bdl.Labels(string(apistructs.LabelTypeIssue), f.InParams.ProjectID, f.sdk.Identity.UserID)
	if err != nil {
		return nil, err
	}
	if labels == nil {
		return nil, nil
	}
	var options []filter.PropConditionOption
	for _, label := range labels.List {
		options = append(options, filter.PropConditionOption{
			Label: label.Name,
			Value: label.ID,
		})
	}
	return options, nil
}
