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
	"encoding/base64"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/common"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/services/issuefilterbm"
	"github.com/erda-project/erda/internal/pkg/component-protocol/issueFilter/gshelper"
	"github.com/erda-project/erda/pkg/strutil"
)

type IssueFilter struct {
	impl.DefaultFilter

	bdl              *bundle.Bundle
	issueSvc         query.Interface
	issueFilterBmSvc *issuefilterbm.IssueFilterBookmark
	gsHelper         *gshelper.GSHelper
	sdk              *cptype.SDK

	filterReq apistructs.IssuePagingRequest
	State     State
	InParams  InParams
	Bms       []issuefilterbm.MyFilterBm
}

type State struct {
	Base64UrlQueryParams    string             `json:"issueFilter__urlQuery,omitempty"`
	FrontendConditionValues FrontendConditions `json:"values,omitempty"`
	SelectedFilterSet       string             `json:"selectedFilterSet,omitempty"`
	WithStateCondition      bool               `json:"-"`
	IssueRequestKey         string             `json:"-"`
	DefaultStateValues      []int64            `json:"defaultStateValues,omitempty"`
}

func init() {
	base.InitProviderToDefaultNamespace("issueFilter", func() servicehub.Provider {
		return &IssueFilter{}
	})
}

func (f *IssueFilter) Initial(sdk *cptype.SDK) {
	f.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	f.issueFilterBmSvc = sdk.Ctx.Value(types.IssueFilterBmService).(*issuefilterbm.IssueFilterBookmark)
	f.issueSvc = sdk.Ctx.Value(types.IssueService).(query.Interface)
	f.gsHelper = gshelper.NewGSHelper(sdk.GlobalState)
	f.sdk = sdk
	if err := f.setInParams(); err != nil {
		panic(err)
	}
	cputil.MustObjJSONTransfer(&f.StdStatePtr, &f.State)
	if err := f.initFilterBms(); err != nil {
		panic(err)
	}
}

func (f *IssueFilter) BeforeHandleOp(sdk *cptype.SDK) {
	f.Initial(sdk)
	f.State.IssueRequestKey = gshelper.KeyIssuePagingRequestKanban
}

func (f *IssueFilter) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		conditions, err := f.ConditionRetriever()
		if err != nil {
			panic(err)
		}
		f.StdDataPtr.Conditions = conditions
		if f.InParams.FrontendUrlQuery != "" {
			if err := f.flushOptsByFilter(f.InParams.FrontendUrlQuery); err != nil {
				panic(err)
			}
		}
		if f.State.WithStateCondition && f.State.FrontendConditionValues.States == nil {
			if err := f.setDefaultState(); err != nil {
				panic(err)
			}
		}
		options, err := f.FilterSet()
		if err != nil {
			panic(err)
		}
		f.StdDataPtr.FilterSet = options
		f.StdDataPtr.Operations = map[cptype.OperationKey]cptype.Operation{
			filter.OpFilter{}.OpKey():           cputil.NewOpBuilder().Build(),
			filter.OpFilterItemSave{}.OpKey():   cputil.NewOpBuilder().Build(),
			filter.OpFilterItemDelete{}.OpKey(): cputil.NewOpBuilder().Build(),
		}
		if f.InParams.FrontendFixedIssueType == apistructs.IssueTypeTicket.String() {
			f.StdDataPtr.HideSave = true
		}
		return nil
	}
}

func (f *IssueFilter) flushOptsByFilter(filterEntity string) error {
	b, err := base64.StdEncoding.DecodeString(filterEntity)
	if err != nil {
		return err
	}
	f.State.FrontendConditionValues = FrontendConditions{}
	return json.Unmarshal(b, &f.State.FrontendConditionValues)
}

func (f *IssueFilter) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return f.RegisterInitializeOp()
}

func (f *IssueFilter) RegisterFilterOp(opData filter.OpFilter) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		return nil
	}
}

func (f *IssueFilter) AfterHandleOp(sdk *cptype.SDK) {
	query, err := f.generateUrlQueryParams()
	if err != nil {
		panic(err)
	}
	f.State.Base64UrlQueryParams = query
	cputil.MustObjJSONTransfer(&f.State, &f.StdStatePtr)
}

func (f *IssueFilter) RegisterFilterItemSaveOp(opData filter.OpFilterItemSave) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		var data FilterSetData
		cputil.MustObjJSONTransfer(&opData.ClientData, &data)
		entity, err := f.CreateFilterSetEntity(opData.ClientData.Values)
		if err != nil {
			panic(err)
		}
		pageKey := f.issueFilterBmSvc.GenPageKey(f.InParams.FrontendFixedIteration, f.InParams.FrontendFixedIssueType)
		_, err = f.issueFilterBmSvc.Create(&dao.IssueFilterBookmark{
			Name:         data.Label,
			UserID:       f.sdk.Identity.UserID,
			ProjectID:    strutil.String(f.InParams.ProjectID),
			PageKey:      pageKey,
			FilterEntity: entity,
		})
		if err != nil {
			panic(err)
		}
		if err := f.initFilterBms(); err != nil {
			panic(err)
		}
		options, err := f.FilterSet()
		if err != nil {
			panic(err)
		}
		f.StdDataPtr.FilterSet = options
		return nil
	}
}

func (f *IssueFilter) RegisterFilterItemDeleteOp(opData filter.OpFilterItemDelete) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		// fmt.Println("delete op come", opData.ClientData.DataRef)
		if err := f.issueFilterBmSvc.Delete(opData.ClientData.DataRef.ID); err != nil {
			panic(err)
		}
		if err := f.initFilterBms(); err != nil {
			panic(err)
		}
		options, err := f.FilterSet()
		if err != nil {
			panic(err)
		}
		f.StdDataPtr.FilterSet = options
		return nil
	}
}

func (f *IssueFilter) Finalize(sdk *cptype.SDK) {
	issuePagingRequest := f.generateIssuePagingRequest()
	f.gsHelper.SetIssuePagingRequest(f.State.IssueRequestKey, issuePagingRequest)
}

func (f *IssueFilter) generateIssuePagingRequest() pb.PagingIssueRequest {
	var (
		startCreatedAt, endCreatedAt, startFinishedAt, endFinishedAt, startClosedAt, endClosedAt int64
	)
	if len(f.State.FrontendConditionValues.CreatedAtStartEnd) >= 2 {
		if f.State.FrontendConditionValues.CreatedAtStartEnd[0] != nil {
			startCreatedAt = *f.State.FrontendConditionValues.CreatedAtStartEnd[0]
			if f.State.FrontendConditionValues.CreatedAtStartEnd[1] == nil {
				endCreatedAt = 0
			} else {
				endCreatedAt = *f.State.FrontendConditionValues.CreatedAtStartEnd[1]
			}
		} else if f.State.FrontendConditionValues.CreatedAtStartEnd[1] != nil {
			startCreatedAt = 0
			endCreatedAt = *f.State.FrontendConditionValues.CreatedAtStartEnd[1]
		}
	}
	if len(f.State.FrontendConditionValues.FinishedAtStartEnd) >= 2 {
		if f.State.FrontendConditionValues.FinishedAtStartEnd[0] != nil {
			startFinishedAt = *f.State.FrontendConditionValues.FinishedAtStartEnd[0]
			if f.State.FrontendConditionValues.FinishedAtStartEnd[1] == nil {
				endFinishedAt = 0
			} else {
				endFinishedAt = *f.State.FrontendConditionValues.FinishedAtStartEnd[1]
			}
		} else if f.State.FrontendConditionValues.FinishedAtStartEnd[1] != nil {
			startFinishedAt = 0
			endFinishedAt = *f.State.FrontendConditionValues.FinishedAtStartEnd[1]
		}
	}
	if len(f.State.FrontendConditionValues.ClosedAtStartEnd) >= 2 {
		if f.State.FrontendConditionValues.ClosedAtStartEnd[0] != nil {
			startClosedAt = *f.State.FrontendConditionValues.ClosedAtStartEnd[0]
			if f.State.FrontendConditionValues.ClosedAtStartEnd[1] == nil {
				endClosedAt = 0
			} else {
				endClosedAt = *f.State.FrontendConditionValues.ClosedAtStartEnd[1]
			}
		} else if f.State.FrontendConditionValues.ClosedAtStartEnd[1] != nil {
			startClosedAt = 0
			endClosedAt = *f.State.FrontendConditionValues.ClosedAtStartEnd[1]
		}
	}

	req := pb.PagingIssueRequest{
		PageNo:             1, // 每次走 filter，都需要重新查询，调整 pageNo 为 1
		PageSize:           0,
		OrgID:              int64(f.InParams.OrgID),
		Title:              f.State.FrontendConditionValues.Title,
		Type:               f.InParams.IssueTypes,
		ProjectID:          f.InParams.ProjectID,
		IterationID:        f.InParams.IterationID,
		IterationIDs:       f.State.FrontendConditionValues.IterationIDs,
		AppID:              nil,
		RequirementID:      nil,
		State:              f.State.FrontendConditionValues.States,
		StateBelongs:       nil,
		Creator:            f.State.FrontendConditionValues.CreatorIDs,
		Assignee:           f.State.FrontendConditionValues.AssigneeIDs,
		Label:              f.State.FrontendConditionValues.LabelIDs,
		StartCreatedAt:     startCreatedAt,
		EndCreatedAt:       endCreatedAt,
		StartFinishedAt:    startFinishedAt,
		EndFinishedAt:      endFinishedAt,
		StartClosedAt:      startClosedAt,
		EndClosedAt:        endClosedAt,
		Priority:           f.State.FrontendConditionValues.Priorities,
		Complexity:         f.State.FrontendConditionValues.Complexities,
		Severity:           f.State.FrontendConditionValues.Severities,
		RelatedIssueId:     nil,
		Source:             "",
		OrderBy:            "updated_at",
		TaskType:           nil,
		BugStage:           f.State.FrontendConditionValues.BugStages,
		Owner:              f.State.FrontendConditionValues.OwnerIDs,
		Asc:                false,
		IDs:                nil,
		External:           true,
		WithProcessSummary: f.InParams.FrontendFixedIssueType == apistructs.IssueTypeRequirement.String(),
	}
	return req
}

func (f *IssueFilter) setDefaultState() error {
	stateBelongs := map[string][]string{
		"TASK":        {pb.IssueStateBelongEnum_OPEN.String(), pb.IssueStateBelongEnum_WORKING.String()},
		"REQUIREMENT": {pb.IssueStateBelongEnum_OPEN.String(), pb.IssueStateBelongEnum_WORKING.String()},
		"BUG":         common.UnfinishedStateBelongs,
		"ALL":         common.UnfinishedStateBelongs,
	}[f.InParams.FrontendFixedIssueType]
	types := []string{pb.IssueTypeEnum_REQUIREMENT.String(), pb.IssueTypeEnum_TASK.String(), pb.IssueTypeEnum_BUG.String()}
	res := make(map[string][]int64)
	res["ALL"] = make([]int64, 0)
	for _, v := range types {
		req := &pb.GetIssueStatesRequest{
			ProjectID:    f.InParams.ProjectID,
			StateBelongs: stateBelongs,
			IssueType:    v,
		}
		ids, err := f.issueSvc.GetIssueStateIDs(req)
		if err != nil {
			return err
		}
		res[v] = ids
		res["ALL"] = append(res["ALL"], ids...)
	}
	f.State.FrontendConditionValues.States = res[f.InParams.FrontendFixedIssueType]
	f.State.DefaultStateValues = res[f.InParams.FrontendFixedIssueType]
	return nil
}
