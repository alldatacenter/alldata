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

package issueFilter

import (
	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
)

func (f *IssueFilter) getPropIterationsOptions() ([]model.SelectOption, error) {
	iterations, err := f.bdl.ListProjectIterations(apistructs.IterationPagingRequest{PageNo: 1, PageSize: 1000, ProjectID: f.InParams.ProjectID, WithoutIssueSummary: true}, f.sdk.Identity.OrgID)
	if err != nil {
		return nil, err
	}
	iterations = append(iterations, apistructs.Iteration{
		ID:    -1,
		Title: f.sdk.I18n("iterationUnassigned"),
	})
	var options []model.SelectOption
	for _, iteration := range iterations {
		options = append(options, *model.NewSelectOption(iteration.Title, iteration.ID))
	}
	return options, nil
}

func (f *IssueFilter) getPropLabelsOptions() ([]model.TagsSelectOption, error) {
	labels, err := f.bdl.Labels(string(apistructs.LabelTypeIssue), f.InParams.ProjectID, f.sdk.Identity.UserID)
	if err != nil {
		return nil, err
	}
	if labels == nil {
		return nil, nil
	}
	var options []model.TagsSelectOption
	for _, label := range labels.List {
		options = append(options, *model.NewTagsSelectOption(label.Name, label.ID, label.Color))
	}
	return options, nil
}

func (f *IssueFilter) getProjectMemberOptions() ([]model.SelectOption, error) {
	members, err := f.bdl.ListMembers(apistructs.MemberListRequest{
		ScopeType: apistructs.ProjectScope,
		ScopeID:   int64(f.InParams.ProjectID),
		PageNo:    1,
		PageSize:  500,
	})
	if err != nil {
		return nil, err
	}
	var results []model.SelectOption
	for _, member := range members {
		results = append(results, *model.NewSelectOption(func() string {
			if member.Nick != "" {
				return member.Nick
			}
			if member.Name != "" {
				return member.Name
			}
			return member.Mobile
		}(),
			member.UserID,
		))
	}
	selectMe := model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "choose-yourself"), f.sdk.Identity.UserID).WithFix(true)
	results = append(results, *selectMe)
	return results, nil
}

func (f *IssueFilter) getPropStagesOptions(tp string) ([]model.SelectOption, error) {
	stages, err := f.bdl.GetIssueStage(int64(f.InParams.OrgID), apistructs.IssueType(tp))
	if err != nil {
		return nil, err
	}
	var options []model.SelectOption
	for _, stage := range stages {
		options = append(options, *model.NewSelectOption(stage.Name, stage.Value))
	}
	return options, nil
}
