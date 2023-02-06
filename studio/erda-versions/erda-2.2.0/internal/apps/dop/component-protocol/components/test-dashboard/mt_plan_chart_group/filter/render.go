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
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "mt_plan_chart_filter",
		func() servicehub.Provider { return &Filter{} })
}

type Filter struct {
	State State `json:"state,omitempty"`
}

type State struct {
	Conditions []filter.PropCondition `json:"conditions,omitempty"`
	Values     FilterStateValues      `json:"values,omitempty"`
}

type FilterStateValues struct {
	MtPlanIDs    []uint64                        `json:"mtPlanIDs,omitempty"`
	CaseStatuses []apistructs.TestCaseExecStatus `json:"caseStatuses,omitempty"`
}

func (f *Filter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.initFromProtocol(ctx, c); err != nil {
		return err
	}

	h := gshelper.NewGSHelper(gs)
	globalMtPlans := h.GetGlobalManualTestPlanList()
	f.State.Conditions = []filter.PropCondition{
		// testplans
		{
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "mtPlanIDs",
			Label:     cputil.I18n(ctx, "Test Plan"),
			Options: func() (opts []filter.PropConditionOption) {
				for _, plan := range globalMtPlans {
					itrPrefix := cputil.I18n(ctx, "[${iteration}: %s]", plan.IterationName)
					if plan.IterationID <= 0 {
						itrPrefix = cputil.I18n(ctx, "[${no-iteration}]")
					}
					opts = append(opts, filter.PropConditionOption{
						Label: cputil.I18n(ctx, "%s %s", itrPrefix, plan.Name),
						Value: plan.ID,
					})
				}
				return
			}(),
			Type: filter.PropConditionTypeSelect,
		},
		//// statuses
		//{
		//	EmptyText: cputil.I18n(ctx, "all"),
		//	Fixed:     true,
		//	Key:       "statuses",
		//	Label:     cputil.I18n(ctx, "test-case-status"),
		//	Options: func() (opts []filter.PropConditionOption) {
		//		return []filter.PropConditionOption{
		//			{Label: cputil.I18n(ctx, string(apistructs.CaseExecStatusInit)), Value: apistructs.CaseExecStatusInit},
		//			{Label: cputil.I18n(ctx, string(apistructs.CaseExecStatusSucc)), Value: apistructs.CaseExecStatusSucc},
		//			{Label: cputil.I18n(ctx, string(apistructs.CaseExecStatusFail)), Value: apistructs.CaseExecStatusFail},
		//			{Label: cputil.I18n(ctx, string(apistructs.CaseExecStatusBlocked)), Value: apistructs.CaseExecStatusBlocked},
		//		}
		//	}(),
		//	Type: filter.PropConditionTypeSelect,
		//},
	}

	// put selected values into global state
	h.SetMtPlanChartFilterTestPlanList(func() (selectedMtPlans []apistructs.TestPlan) {
		// not selected, return all
		if len(f.State.Values.MtPlanIDs) == 0 {
			return globalMtPlans
		}
		globalMtPlanMap := make(map[uint64]apistructs.TestPlan)
		for _, plan := range globalMtPlans {
			globalMtPlanMap[plan.ID] = plan
		}
		for _, planID := range f.State.Values.MtPlanIDs {
			selectedMtPlans = append(selectedMtPlans, globalMtPlanMap[planID])
		}
		return
	}())
	//h.SetMtPlanChartFilterStatusList(f.State.Values.CaseStatuses)

	if err := f.setToComponent(c); err != nil {
		return err
	}
	return nil
}

func (f *Filter) initFromProtocol(ctx context.Context, c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, f); err != nil {
		return err
	}
	return nil
}

func (f *Filter) setToComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &c); err != nil {
		return err
	}
	return nil
}
