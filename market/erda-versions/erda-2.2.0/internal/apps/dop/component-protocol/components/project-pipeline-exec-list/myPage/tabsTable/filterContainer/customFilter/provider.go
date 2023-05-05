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

package customFilter

import (
	"fmt"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter"
	"github.com/erda-project/erda-infra/providers/component-protocol/components/filter/impl"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline-exec-list/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline-exec-list/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/util"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type CustomFilter struct {
	impl.DefaultFilter

	bdl      *bundle.Bundle
	gsHelper *gshelper.GSHelper
	sdk      *cptype.SDK
	State    State     `json:"-"`
	InParams *InParams `json:"-"`
	URLQuery *FrontendConditions
}

type State struct {
	FrontendConditionValues FrontendConditions `json:"values,omitempty"`
	SelectedFilterSet       string             `json:"selectedFilterSet,omitempty"`
}

type FrontendConditions struct {
	Status            []string `json:"status,omitempty"`
	AppList           []uint64 `json:"appList,omitempty"`
	Executor          []string `json:"executor,omitempty"`
	StartedAtStartEnd []int64  `json:"startedAtStartEnd,omitempty"`
	Title             string   `json:"title,omitempty"`
}

func (p *CustomFilter) AfterHandleOp(sdk *cptype.SDK) {
	cputil.SetURLQuery(sdk, p.State.FrontendConditionValues)
	cputil.MustObjJSONTransfer(&p.State, &p.StdStatePtr)
}

func (p *CustomFilter) BeforeHandleOp(sdk *cptype.SDK) {
	p.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	p.gsHelper = gshelper.NewGSHelper(sdk.GlobalState)
	p.sdk = sdk
	if p.sdk.Identity.OrgID != "" {
		var err error
		p.InParams.OrgIDInt, err = strconv.ParseUint(p.sdk.Identity.OrgID, 10, 64)
		if err != nil {
			panic(err)
		}
	}

	var urlQuery FrontendConditions
	err := cputil.GetURLQuery(sdk, &urlQuery)
	if err != nil {
		logrus.Errorf("GetURLQuery error %v", err)
	} else {
		p.URLQuery = &urlQuery
	}

	cputil.MustObjJSONTransfer(&p.StdStatePtr, &p.State)
}

func (p *CustomFilter) RegisterInitializeOp() (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		conditions, err := p.ConditionRetriever()
		if err != nil {
			panic(err)
		}

		p.StdDataPtr = &filter.Data{
			Conditions: conditions,
			Operations: map[cptype.OperationKey]cptype.Operation{
				filter.OpFilter{}.OpKey():         cputil.NewOpBuilder().Build(),
				filter.OpFilterItemSave{}.OpKey(): cputil.NewOpBuilder().Build(),
			},
			HideSave: true,
		}

		if p.URLQuery != nil {
			p.State.FrontendConditionValues = *p.URLQuery
		} else {
			p.setDefaultValues()
		}

		p.gsHelper.SetStatuesFilter(p.State.FrontendConditionValues.Status)
		p.gsHelper.SetAppsFilter(p.State.FrontendConditionValues.AppList)
		p.gsHelper.SetExecutorsFilter(p.State.FrontendConditionValues.Executor)
		p.gsHelper.SetPipelineNameFilter(p.State.FrontendConditionValues.Title)

		if len(p.State.FrontendConditionValues.StartedAtStartEnd) > 0 {
			p.gsHelper.SetBeginTimeStartFilter(p.State.FrontendConditionValues.StartedAtStartEnd[0])
		}
		if len(p.State.FrontendConditionValues.StartedAtStartEnd) == 2 {
			p.gsHelper.SetBeginTimeEndFilter(p.State.FrontendConditionValues.StartedAtStartEnd[1])
		}
		return nil
	}
}

func (p *CustomFilter) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *CustomFilter) RegisterFilterOp(opData filter.OpFilter) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		state := p.State.FrontendConditionValues

		var realSearchStatus []string
		for _, status := range state.Status {
			realSearchStatus = append(realSearchStatus, util.TransferStatus(status)...)
		}

		p.gsHelper.SetStatuesFilter(realSearchStatus)
		p.gsHelper.SetAppsFilter(state.AppList)
		p.gsHelper.SetExecutorsFilter(state.Executor)
		p.gsHelper.SetPipelineNameFilter(state.Title)

		if len(state.StartedAtStartEnd) > 0 {
			p.gsHelper.SetBeginTimeStartFilter(state.StartedAtStartEnd[0])
		}
		if len(state.StartedAtStartEnd) == 2 {
			p.gsHelper.SetBeginTimeEndFilter(state.StartedAtStartEnd[1])
		}
		return nil
	}
}

func (p *CustomFilter) RegisterFilterItemSaveOp(opData filter.OpFilterItemSave) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		fmt.Println("op come", opData.ClientData)
		return nil
	}
}

func (p *CustomFilter) RegisterFilterItemDeleteOp(opData filter.OpFilterItemDelete) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		fmt.Println("op come", opData.ClientData.DataRef)
		return nil
	}
}

func (p *CustomFilter) setDefaultValues() {
	if p.InParams.AppIDInt != 0 {
		p.State.FrontendConditionValues.AppList = []uint64{p.InParams.AppIDInt}
	} else {
		p.State.FrontendConditionValues.AppList = []uint64{common.Participated}
	}
}
