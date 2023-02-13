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
	model "github.com/erda-project/erda-infra/providers/component-protocol/components/filter/models"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/project-pipeline/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/util"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline/deftype"
	"github.com/erda-project/erda/internal/pkg/component-protocol/condition"
	"github.com/erda-project/erda/pkg/limit_sync_group"
)

func (p *CustomFilter) ConditionRetriever() ([]interface{}, error) {
	conditions := make([]interface{}, 0)
	conditions = append(conditions, p.StatusCondition())

	var (
		appCondition    *model.SelectCondition
		branchCondition *model.SelectCondition
		memberCondition MemberCondition
	)

	worker := limit_sync_group.NewWorker(3)
	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		// If err is defined outside, there will be a data race
		var err error
		appCondition, err = p.AppCondition()
		return err
	})
	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		var err error
		memberCondition, err = p.MemberCondition()
		return err
	})
	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		var err error
		branchCondition, err = p.BranchCondition()
		return err
	})
	if err := worker.Do().Error(); err != nil {
		return nil, err
	}
	if p.InParams.AppID == 0 {
		conditions = append(conditions, appCondition)
	}
	conditions = append(conditions, branchCondition)
	conditions = append(conditions, memberCondition.executorCondition)
	conditions = append(conditions, model.NewDateRangeCondition("startedAtStartEnd", cputil.I18n(p.sdk.Ctx, "start-time")))
	conditions = append(conditions, memberCondition.creatorCondition)
	conditions = append(conditions, model.NewDateRangeCondition("createdAtStartEnd", cputil.I18n(p.sdk.Ctx, "creationTime")))
	conditions = append(conditions, condition.ExternalInputCondition("title", "title", cputil.I18n(p.sdk.Ctx, "searchByPipelineName")))
	return conditions, nil
}

func (p *CustomFilter) StatusCondition() *model.SelectCondition {
	statuses := util.PipelineDefinitionStatus
	var opts []model.SelectOption
	for _, status := range statuses {
		opts = append(opts, *model.NewSelectOption(cputil.I18n(p.sdk.Ctx, common.ColumnPipelineStatus+status.String()), status.String()))
	}
	condition := model.NewSelectCondition("status", cputil.I18n(p.sdk.Ctx, "status"), opts)
	condition.ConditionBase.Placeholder = cputil.I18n(p.sdk.Ctx, "please-choose-status")
	return condition
}

type MemberCondition struct {
	executorCondition *model.SelectCondition
	creatorCondition  *model.SelectCondition
}

func (p *CustomFilter) MemberCondition() (MemberCondition, error) {
	members, err := p.bdl.ListMembers(apistructs.MemberListRequest{
		ScopeType: apistructs.ProjectScope,
		ScopeID:   int64(p.InParams.ProjectID),
		PageNo:    1,
		PageSize:  500,
	})
	if err != nil {
		return MemberCondition{}, err
	}

	executorCondition := model.NewSelectCondition("executor", cputil.I18n(p.sdk.Ctx, "executor"), func() []model.SelectOption {
		selectOptions := make([]model.SelectOption, 0, len(members)+1)
		for _, v := range members {
			selectOptions = append(selectOptions, *model.NewSelectOption(
				v.GetUserName(),
				v.UserID,
			))
		}
		selectOptions = append(selectOptions, *model.NewSelectOption(cputil.I18n(p.sdk.Ctx, "choose-yourself"), p.sdk.Identity.UserID).WithFix(true))
		return selectOptions
	}())
	executorCondition.ConditionBase.Placeholder = cputil.I18n(p.sdk.Ctx, "please-choose-executor")

	creatorCondition := model.NewSelectCondition("creator", cputil.I18n(p.sdk.Ctx, "creator"), func() []model.SelectOption {
		selectOptions := make([]model.SelectOption, 0, len(members)+1)
		for _, v := range members {
			selectOptions = append(selectOptions, *model.NewSelectOption(
				v.GetUserName(),
				v.UserID,
			))
		}
		selectOptions = append(selectOptions, *model.NewSelectOption(cputil.I18n(p.sdk.Ctx, "choose-yourself"), p.sdk.Identity.UserID).WithFix(true))
		return selectOptions
	}())
	creatorCondition.ConditionBase.Placeholder = cputil.I18n(p.sdk.Ctx, "please-choose-creator")

	return MemberCondition{
		executorCondition: executorCondition,
		creatorCondition:  creatorCondition,
	}, nil
}

func (p *CustomFilter) AppCondition() (*model.SelectCondition, error) {
	if p.InParams.AppID != 0 {
		return p.AppConditionWithInParamsAppID()
	}
	return p.AppConditionWithNoInParamsAppID()
}

func (p *CustomFilter) AppConditionWithNoInParamsAppID() (*model.SelectCondition, error) {
	var (
		allAppNames []string
		myAppNames  []string
	)

	worker := limit_sync_group.NewWorker(2)
	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		allAppResp, err := p.bdl.GetAppList(p.sdk.Identity.OrgID, p.sdk.Identity.UserID, apistructs.ApplicationListRequest{
			ProjectID: p.InParams.ProjectID,
			PageNo:    1,
			PageSize:  999,
			IsSimple:  true,
		})
		if err != nil {
			return err
		}
		for _, v := range allAppResp.List {
			allAppNames = append(allAppNames, v.Name)
		}
		return nil
	})
	worker.AddFunc(func(locker *limit_sync_group.Locker, i ...interface{}) error {
		myAppResp, err := p.bdl.GetMyAppsByProject(p.sdk.Identity.UserID, p.InParams.OrgID, p.InParams.ProjectID, "")
		if err != nil {
			return err
		}
		for _, v := range myAppResp.List {
			myAppNames = append(myAppNames, v.Name)
		}
		return nil
	})
	if err := worker.Do().Error(); err != nil {
		return nil, err
	}
	p.gsHelper.SetGlobalMyAppNames(myAppNames)

	cond := model.NewSelectCondition("app", cputil.I18n(p.sdk.Ctx, "application"), func() []model.SelectOption {
		selectOptions := make([]model.SelectOption, 0, len(allAppNames)+1)
		selectOptions = append(selectOptions, *model.NewSelectOption(cputil.I18n(p.sdk.Ctx, "participated"), common.Participated))
		for _, v := range allAppNames {
			selectOptions = append(selectOptions, *model.NewSelectOption(v, v))
		}
		return selectOptions
	}())
	cond.ConditionBase.Disabled = false
	cond.ConditionBase.Placeholder = cputil.I18n(p.sdk.Ctx, "please-choose-application")
	return cond, nil
}

func (p *CustomFilter) AppConditionWithInParamsAppID() (*model.SelectCondition, error) {
	app, err := p.bdl.GetApp(p.InParams.AppID)
	if err != nil {
		return nil, err
	}
	p.AppName = app.Name
	cond := model.NewSelectCondition("app", cputil.I18n(p.sdk.Ctx, "application"), []model.SelectOption{*model.NewSelectOption(app.Name, app.Name)})
	cond.ConditionBase.Disabled = true
	cond.ConditionBase.Placeholder = cputil.I18n(p.sdk.Ctx, "please-choose-application")
	return cond, nil
}

func (p *CustomFilter) BranchCondition() (*model.SelectCondition, error) {
	branches, err := p.ProjectPipelineSvc.ListUsedRefs(p.sdk.Ctx, deftype.ProjectPipelineUsedRefList{
		ProjectID:    p.InParams.ProjectID,
		IdentityInfo: apistructs.IdentityInfo{UserID: p.sdk.Identity.UserID},
		AppID:        p.InParams.AppID,
	})
	if err != nil {
		return nil, err
	}
	cond := model.NewSelectCondition("branch", cputil.I18n(p.sdk.Ctx, "branch"), func() []model.SelectOption {
		selectOptions := make([]model.SelectOption, 0, len(branches))
		for _, v := range branches {
			selectOptions = append(selectOptions, *model.NewSelectOption(v, v))
		}
		return selectOptions
	}())
	cond.ConditionBase.Disabled = false
	cond.ConditionBase.Placeholder = cputil.I18n(p.sdk.Ctx, "please-choose-branch")
	return cond, nil
}
