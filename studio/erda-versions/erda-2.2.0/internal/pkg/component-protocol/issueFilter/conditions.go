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
	"context"
	"strings"

	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/component-protocol/condition"
	"github.com/erda-project/erda/internal/pkg/component-protocol/issueFilter/gshelper"
)

const (
	PropConditionKeyFilterID           string = "filterID" // special, need emit it when hashing filter
	PropConditionKeyIterationIDs       string = "iterationIDs"
	PropConditionKeyTitle              string = "title"
	PropConditionKeyStateBelongs       string = "stateBelongs"
	PropConditionKeyStates             string = "states"
	PropConditionKeyLabelIDs           string = "labelIDs"
	PropConditionKeyPriorities         string = "priorities"
	PropConditionKeySeverities         string = "severities"
	PropConditionKeyCreatorIDs         string = "creatorIDs"
	PropConditionKeyAssigneeIDs        string = "assigneeIDs"
	PropConditionKeyOwnerIDs           string = "ownerIDs"
	PropConditionKeyBugStages          string = "bugStages"
	PropConditionKeyCreatedAtStartEnd  string = "createdAtStartEnd"
	PropConditionKeyFinishedAtStartEnd string = "finishedAtStartEnd"
	PropConditionKeyClosed             string = "closedAtStartEnd"
	PropConditionKeyComplexity         string = "complexities"
)

type FrontendConditions struct {
	FilterID           string   `json:"filterID,omitempty"`
	IterationIDs       []int64  `json:"iterationIDs,omitempty"`
	Title              string   `json:"title,omitempty"`
	StateBelongs       []string `json:"stateBelongs,omitempty"`
	States             []int64  `json:"states,omitempty"`
	LabelIDs           []uint64 `json:"labelIDs,omitempty"`
	Priorities         []string `json:"priorities,omitempty"`
	Severities         []string `json:"severities,omitempty"`
	CreatorIDs         []string `json:"creatorIDs,omitempty"`
	AssigneeIDs        []string `json:"assigneeIDs,omitempty"`
	OwnerIDs           []string `json:"ownerIDs,omitempty"`
	BugStages          []string `json:"bugStages,omitempty"`
	CreatedAtStartEnd  []*int64 `json:"createdAtStartEnd,omitempty"`
	FinishedAtStartEnd []*int64 `json:"finishedAtStartEnd,omitempty"`
	ClosedAtStartEnd   []*int64 `json:"closedAtStartEnd,omitempty"`
	Complexities       []string `json:"complexities,omitempty"`
}

func (f *IssueFilter) ConditionRetriever() ([]interface{}, error) {
	needIterationCond := true
	if f.InParams.FrontendFixedIteration != "" {
		needIterationCond = false
	}
	var iterations *model.SelectCondition
	iterationOptions, err := f.getPropIterationsOptions()
	if err != nil {
		return nil, err
	}
	f.gsHelper.SetIterationOptions(gshelper.KeyIterationOptions, iterationOptions)
	if needIterationCond {
		iterations = model.NewSelectCondition(PropConditionKeyIterationIDs, cputil.I18n(f.sdk.Ctx, "sprint"), iterationOptions).WithPlaceHolder(cputil.I18n(f.sdk.Ctx, "choose-sprint"))
	}

	labelOptions, err := f.getPropLabelsOptions()
	if err != nil {
		return nil, err
	}
	labels := model.NewTagsSelectCondition(PropConditionKeyLabelIDs, cputil.I18n(f.sdk.Ctx, "label"), labelOptions)
	labels.ConditionBase.Placeholder = cputil.I18n(f.sdk.Ctx, "please-choose-label")

	priorityOptions := []model.SelectOption{
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "urgent"), "URGENT"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "high"), "HIGH"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "normal"), "NORMAL"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "low"), "LOW"),
	}
	priority := model.NewSelectCondition(PropConditionKeyPriorities, cputil.I18n(f.sdk.Ctx, "priority"), priorityOptions).WithPlaceHolder(cputil.I18n(f.sdk.Ctx, "choose-priorities"))

	severityOptions := []model.SelectOption{
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "fatal"), "FATAL"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "serious"), "SERIOUS"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "ordinary"), "NORMAL"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "slight"), "SLIGHT"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "suggest"), "SUGGEST"),
	}
	severity := model.NewSelectCondition(PropConditionKeySeverities, cputil.I18n(f.sdk.Ctx, "severity"), severityOptions).WithPlaceHolder(cputil.I18n(f.sdk.Ctx, "choose-severity"))

	memberOptions, err := f.getProjectMemberOptions()
	if err != nil {
		return nil, err
	}
	creator := model.NewSelectCondition(PropConditionKeyCreatorIDs, cputil.I18n(f.sdk.Ctx, "creator"), memberOptions)
	assignee := model.NewSelectCondition(PropConditionKeyAssigneeIDs, cputil.I18n(f.sdk.Ctx, "assignee"), memberOptions)
	owner := model.NewSelectCondition(PropConditionKeyOwnerIDs, cputil.I18n(f.sdk.Ctx, "responsible-person"), memberOptions)

	stageOptions := []model.SelectOption{
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "demand-design"), "demandDesign"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "architecture-design"), "architectureDesign"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "code-development"), "codeDevelopment"),
	}
	if f.InParams.FrontendFixedIssueType == apistructs.IssueTypeTask.String() || f.InParams.FrontendFixedIssueType == apistructs.IssueTypeBug.String() {
		stageOptions, err = f.getPropStagesOptions(f.InParams.FrontendFixedIssueType)
		if err != nil {
			return nil, err
		}
	}
	stage := model.NewSelectCondition(PropConditionKeyBugStages, func() string {
		switch f.InParams.FrontendFixedIssueType {
		case "ALL":
		case apistructs.IssueTypeEpic.String():
		case apistructs.IssueTypeRequirement.String():
		case apistructs.IssueTypeTask.String():
			return cputil.I18n(f.sdk.Ctx, "task-type")
		case apistructs.IssueTypeBug.String():
			return cputil.I18n(f.sdk.Ctx, "import-source")
		}
		return ""
	}(), stageOptions)

	created := model.NewDateRangeCondition(PropConditionKeyCreatedAtStartEnd, cputil.I18n(f.sdk.Ctx, "created-at"))
	finished := model.NewDateRangeCondition(PropConditionKeyFinishedAtStartEnd, cputil.I18n(f.sdk.Ctx, "deadline"))
	closed := model.NewDateRangeCondition(PropConditionKeyClosed, cputil.I18n(f.sdk.Ctx, "closed-at"))

	var leftGroup, rightGroup []interface{}
	if needIterationCond {
		leftGroup = []interface{}{iterations}
	}

	complexityOptions := []model.SelectOption{
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "HARD"), "HARD"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "NORMAL"), "NORMAL"),
		*model.NewSelectOption(cputil.I18n(f.sdk.Ctx, "EASY"), "EASY"),
	}
	complexity := model.NewSelectCondition(PropConditionKeyComplexity, cputil.I18n(f.sdk.Ctx, "complexity"), complexityOptions)

	var status interface{}
	if f.State.WithStateCondition {
		statesMap, err := f.issueSvc.GetIssueStatesMap(&pb.GetIssueStatesRequest{
			ProjectID: f.InParams.ProjectID,
		})
		if err != nil {
			return nil, err
		}

		status = func() interface{} {
			switch f.InParams.FrontendFixedIssueType {
			case "ALL":
				return model.NewSelectConditionWithChildren(PropConditionKeyStates, cputil.I18n(f.sdk.Ctx, "state"), convertAllConditions(f.sdk.Ctx, statesMap))
			case apistructs.IssueTypeRequirement.String():
				return model.NewSelectCondition(PropConditionKeyStates, cputil.I18n(f.sdk.Ctx, "state"), convertConditions(statesMap[pb.IssueTypeEnum_REQUIREMENT.String()]))
			case apistructs.IssueTypeTask.String():
				return model.NewSelectCondition(PropConditionKeyStates, cputil.I18n(f.sdk.Ctx, "state"), convertConditions(statesMap[pb.IssueTypeEnum_TASK.String()]))
			case apistructs.IssueTypeBug.String():
				return model.NewSelectCondition(PropConditionKeyStates, cputil.I18n(f.sdk.Ctx, "state"), convertConditions(statesMap[pb.IssueTypeEnum_BUG.String()]))
			case apistructs.IssueTypeTicket.String():
				return model.NewSelectCondition(PropConditionKeyStates, cputil.I18n(f.sdk.Ctx, "state"), convertConditions(statesMap[pb.IssueTypeEnum_TICKET.String()]))
			}
			return nil
		}()
		leftGroup = append(leftGroup, status)
	}

	leftGroup = append(leftGroup, labels, priority, complexity)
	rightGroup = []interface{}{assignee, finished, creator, created}
	switch f.InParams.FrontendFixedIssueType {
	case apistructs.IssueTypeRequirement.String():
	case apistructs.IssueTypeTask.String():
		leftGroup = append(leftGroup, stage)
	case apistructs.IssueTypeBug.String():
		leftGroup = append(leftGroup, severity, stage)
		rightGroup = append(rightGroup, owner, closed)
	case "ALL":
		rightGroup = append(rightGroup, owner)
	case apistructs.IssueTypeTicket.String():
		leftGroup = []interface{}{status, labels, priority, severity}
	}

	conditions := Zigzag(leftGroup, rightGroup)
	conditions = append(conditions, condition.ExternalInputCondition("title", "title", cputil.I18n(f.sdk.Ctx, "searchByName")))
	return conditions, nil
}

func Zigzag(l1, l2 []interface{}) []interface{} {
	var res []interface{}
	i, j := 0, 0
	for i < len(l1) && j < len(l2) {
		res = append(res, l1[i], l2[j])
		i += 1
		j += 1
	}
	for i < len(l1) {
		res = append(res, l1[i])
		i += 1
	}
	for j < len(l2) {
		res = append(res, l2[i])
		j += 1
	}
	return res
}

func convertConditions(status []pb.IssueStatus) []model.SelectOption {
	options := make([]model.SelectOption, 0, len(status))
	for _, i := range status {
		options = append(options, *model.NewSelectOption(i.StateName, i.StateID))
	}
	return options
}

func convertAllConditions(ctx context.Context, stateMap map[string][]pb.IssueStatus) []model.SelectOptionWithChildren {
	options := make([]model.SelectOptionWithChildren, 0, len(stateMap))
	for i, v := range stateMap {
		options = append(options, model.SelectOptionWithChildren{
			SelectOption: *model.NewSelectOption(cputil.I18n(ctx, strings.ToLower(i)), i),
			Children:     convertConditions(v),
		})
	}
	return options
}
