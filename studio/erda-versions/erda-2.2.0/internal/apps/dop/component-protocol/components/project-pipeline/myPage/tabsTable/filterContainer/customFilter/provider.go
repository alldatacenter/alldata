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
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/util"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline"
)

type CustomFilter struct {
	impl.DefaultFilter

	bdl      *bundle.Bundle
	gsHelper *gshelper.GSHelper
	sdk      *cptype.SDK
	State    State     `json:"state"`
	InParams *InParams `json:"-"`
	AppName  string    `json:"-"`

	ProjectPipelineSvc projectpipeline.Service `autowired:"erda.dop.projectpipeline.ProjectPipelineService"`

	URLQuery *FrontendConditions
}

type State struct {
	FrontendConditionValues FrontendConditions `json:"values,omitempty"`
	SelectedFilterSet       string             `json:"selectedFilterSet,omitempty"`
}

type FrontendConditions struct {
	Status            []string `json:"status,omitempty"`
	Creator           []string `json:"creator,omitempty"`
	App               []string `json:"app,omitempty"`
	Executor          []string `json:"executor,omitempty"`
	CreatedAtStartEnd []int64  `json:"createdAtStartEnd,omitempty"`
	StartedAtStartEnd []int64  `json:"startedAtStartEnd,omitempty"`
	Title             string   `json:"title,omitempty"`
	Branch            []string `json:"branch,omitempty"`
}

func (p *CustomFilter) BeforeHandleOp(sdk *cptype.SDK) {
	p.bdl = sdk.Ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	p.gsHelper = gshelper.NewGSHelper(sdk.GlobalState)
	p.sdk = sdk
	var err error
	p.InParams.OrgID, err = strconv.ParseUint(p.sdk.Identity.OrgID, 10, 64)
	if err != nil {
		panic(err)
	}
	p.ProjectPipelineSvc = sdk.Ctx.Value(types.ProjectPipelineService).(*projectpipeline.ProjectPipelineService)

	var urlQuery FrontendConditions
	err = cputil.GetURLQuery(sdk, &urlQuery)
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
		p.clearState()

		if p.URLQuery != nil {
			p.State.FrontendConditionValues = *p.URLQuery
		} else {
			p.setDefaultValues()
		}

		p.gsHelper.SetGlobalTableFilter(gshelper.TableFilter{
			Status:            p.State.FrontendConditionValues.Status,
			Creator:           p.State.FrontendConditionValues.Creator,
			App:               p.State.FrontendConditionValues.App,
			Executor:          p.State.FrontendConditionValues.Executor,
			CreatedAtStartEnd: p.State.FrontendConditionValues.CreatedAtStartEnd,
			StartedAtStartEnd: p.State.FrontendConditionValues.StartedAtStartEnd,
			Title:             p.State.FrontendConditionValues.Title,
			Branch:            p.State.FrontendConditionValues.Branch,
		})
		return nil
	}
}

func (p *CustomFilter) clearState() {
	p.State.FrontendConditionValues = FrontendConditions{}
}

func (p *CustomFilter) AfterHandleOp(sdk *cptype.SDK) {
	cputil.SetURLQuery(sdk, p.State.FrontendConditionValues)

	cputil.MustObjJSONTransfer(&p.State, &p.StdStatePtr)
}

func (p *CustomFilter) RegisterRenderingOp() (opFunc cptype.OperationFunc) {
	return p.RegisterInitializeOp()
}

func (p *CustomFilter) RegisterFilterOp(opData filter.OpFilter) (opFunc cptype.OperationFunc) {
	return func(sdk *cptype.SDK) cptype.IStdStructuredPtr {
		values := p.State.FrontendConditionValues

		var realSearchStatus []string
		for _, status := range values.Status {
			realSearchStatus = append(realSearchStatus, util.TransferStatus(status)...)
		}

		p.gsHelper.SetGlobalTableFilter(gshelper.TableFilter{
			Status:            realSearchStatus,
			Creator:           values.Creator,
			App:               values.App,
			Executor:          values.Executor,
			CreatedAtStartEnd: values.CreatedAtStartEnd,
			StartedAtStartEnd: values.StartedAtStartEnd,
			Title:             values.Title,
			Branch:            values.Branch,
		})
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
	p.State.FrontendConditionValues.App = p.MakeDefaultAppSelect()
	p.State.FrontendConditionValues.Branch = p.MakeDefaultBranchSelect()
}

func (p *CustomFilter) MakeDefaultAppSelect() []string {
	if p.AppName == "" {
		return []string{common.Participated}
	}
	return []string{p.AppName}
}

func (p *CustomFilter) MakeDefaultBranchSelect() []string {
	if p.AppName == "" {
		return common.DefaultBranch
	}
	return nil
}
