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
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter"
	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/pkg/strutil"
)

func init() {
	cpregister.RegisterComponent("issue-gantt", "filter", func() cptype.IComponent { return &ComponentFilter{} })
}

func (f *ComponentFilter) BeforeHandleOp(sdk *cptype.SDK) {
	f.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	f.sdk = sdk
	cputil.MustObjJSONTransfer(&f.StdStatePtr, &f.State)
	projectID, err := strconv.ParseUint(cputil.GetInParamByKey(sdk.Ctx, "projectId").(string), 10, 64)
	if err != nil {
		panic(err)
	}
	f.projectID = projectID
	if fixedIterationIDStr := strutil.String(cputil.GetInParamByKey(sdk.Ctx, "fixedIteration")); fixedIterationIDStr != "" {
		fixedIterationID, err := strconv.ParseUint(fixedIterationIDStr, 10, 64)
		if err != nil {
			panic(err)
		}
		f.fixedIterationID = fixedIterationID
	}
	if q := cputil.GetInParamByKey(sdk.Ctx, "filter__urlQuery"); q != nil {
		f.FrontendUrlQuery = q.(string)
	}
}

func (f *ComponentFilter) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		var iterations *model.SelectCondition
		var conditions []interface{}
		iteration, iterationOptions, err := f.getPropIterationsOptions()
		if err != nil {
			panic(err)
		}
		if f.fixedIterationID == 0 {
			iterations = model.NewSelectCondition("iterationIDs", cputil.I18n(f.sdk.Ctx, "sprint"), iterationOptions).WithPlaceHolder(cputil.I18n(f.sdk.Ctx, "choose-sprint"))
			conditions = append(conditions, iterations)
		}

		if f.FrontendUrlQuery != "" {
			if err := f.flushOptsByFilter(f.FrontendUrlQuery); err != nil {
				panic(err)
			}
		} else {
			f.State.Values.IterationIDs = []int64{iteration}
		}
		if f.fixedIterationID > 0 {
			f.State.Values.IterationIDs = []int64{iteration}
		}

		memberOptions, err := f.getProjectMemberOptions()
		if err != nil {
			panic(err)
		}
		assignee := model.NewSelectCondition("assignee", cputil.I18n(f.sdk.Ctx, "assignee"), memberOptions)
		labelOptions, err := f.getProjectLabelsOptions()
		if err != nil {
			panic(err)
		}
		labels := model.NewTagsSelectCondition("label", cputil.I18n(f.sdk.Ctx, "label"), labelOptions)
		labels.ConditionBase.Placeholder = cputil.I18n(f.sdk.Ctx, "please-choose-label")
		conditions = append(conditions, assignee, labels)
		f.StdDataPtr.Conditions = conditions
		f.StdDataPtr.HideSave = true
		f.StdDataPtr.Operations = map[cptype.OperationKey]cptype.Operation{
			filter.OpFilter{}.OpKey(): cputil.NewOpBuilder().Build(),
		}
		return nil
	}
}

func (f *ComponentFilter) flushOptsByFilter(filterEntity string) error {
	b, err := base64.StdEncoding.DecodeString(filterEntity)
	if err != nil {
		return err
	}
	f.State.Values = FrontendConditions{}
	return json.Unmarshal(b, &f.State.Values)
}

func (f *ComponentFilter) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return f.RegisterInitializeOp()
}

func (f *ComponentFilter) RegisterFilterOp(opData filter.OpFilter) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		if f.fixedIterationID > 0 {
			f.State.Values.IterationIDs = []int64{int64(f.fixedIterationID)}
		}
		return nil
	}
}

func (f *ComponentFilter) AfterHandleOp(sdk *cptype.SDK) {
	query, err := common.GenerateUrlQueryParams(f.State.Values)
	if err != nil {
		panic(err)
	}
	f.State.Base64UrlQueryParams = query
	cputil.MustObjJSONTransfer(&f.State, &f.StdStatePtr)
}

func (f *ComponentFilter) RegisterFilterItemSaveOp(opData filter.OpFilterItemSave) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (f *ComponentFilter) RegisterFilterItemDeleteOp(opData filter.OpFilterItemDelete) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (f *ComponentFilter) getProjectMemberOptions() ([]model.SelectOption, error) {
	members, err := f.bdl.ListMembers(apistructs.MemberListRequest{
		ScopeType: apistructs.ProjectScope,
		ScopeID:   int64(f.projectID),
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

func (f *ComponentFilter) getProjectLabelsOptions() ([]model.TagsSelectOption, error) {
	labels, err := f.bdl.Labels(string(apistructs.LabelTypeIssue), f.projectID, f.sdk.Identity.UserID)
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

func (f *ComponentFilter) getPropIterationsOptions() (int64, []model.SelectOption, error) {
	iterations, err := f.bdl.ListProjectIterations(apistructs.IterationPagingRequest{
		PageNo: 1, PageSize: 1000,
		ProjectID: f.projectID, State: apistructs.IterationStateUnfiled,
		WithoutIssueSummary: true,
	}, f.sdk.Identity.OrgID)
	if err != nil {
		return -1, nil, err
	}
	iterations = append(iterations, apistructs.Iteration{
		ID:    -1,
		Title: f.sdk.I18n("iterationUnassigned"),
	})
	var options []model.SelectOption
	for _, iteration := range iterations {
		options = append(options, *model.NewSelectOption(iteration.Title, iteration.ID))
	}
	// fixed iteration
	if f.fixedIterationID > 0 {
		found := false
		var fixedIteration apistructs.Iteration
		for i, itr := range iterations {
			if itr.ID == int64(f.fixedIterationID) {
				found = true
				fixedIteration = iterations[i]
				break
			}
		}
		if !found {
			return -1, nil, fmt.Errorf("fixedIteration: %d not belong to project", f.fixedIterationID)
		}
		options = []model.SelectOption{*model.NewSelectOption(fixedIteration.Title, fixedIteration.ID)}
		return fixedIteration.ID, options, nil
	}
	return defaultIterationRetriever(iterations), options, nil
}

func defaultIterationRetriever(iterations []apistructs.Iteration) int64 {
	for _, iteration := range iterations {
		if iteration.StartedAt != nil && !time.Now().Before(*iteration.StartedAt) &&
			iteration.FinishedAt != nil && !time.Now().After(*iteration.FinishedAt) {
			return iteration.ID
		}
	}
	return -1
}
