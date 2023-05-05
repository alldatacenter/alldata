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

package mt_block_header_filter

import (
	"context"
	"encoding/json"
	"time"

	"github.com/recallsong/go-utils/container/slice"

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
	base.InitProviderWithCreator(common.ScenarioKeyTestDashboard, "mt_block_header_filter",
		func() servicehub.Provider { return &Filter{} },
	)
}

type Filter struct {
	State State `json:"state,omitempty"`
}

type State struct {
	Conditions []filter.PropCondition  `json:"conditions,omitempty"`
	Values     MtPlanFilterStateValues `json:"values,omitempty"`
}

type MtPlanFilterStateValues struct {
	MtPlanIDs []uint64 `json:"mtPlanIDs,omitempty"`
}

// Render is empty implement.
func (f *Filter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.initFromProtocol(ctx, c); err != nil {
		return err
	}

	h := gshelper.NewGSHelper(gs)
	globalMtPlans := h.GetGlobalManualTestPlanList()
	selectedItrsByID := h.GetGlobalSelectedIterationsByID()
	f.State.Conditions = []filter.PropCondition{
		{
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "mtPlanIDs",
			Label:     cputil.I18n(ctx, "Test Plan"),
			Options: func() (opts []filter.PropConditionOption) {
				type Obj struct {
					filter.PropConditionOption
					itrCreateTime time.Time
				}
				var beforeOpts []Obj // used for order
				for _, plan := range globalMtPlans {
					itrPrefix := cputil.I18n(ctx, "[${iteration}: %s]", plan.IterationName)
					if plan.IterationID <= 0 {
						itrPrefix = cputil.I18n(ctx, "[${no-iteration}]")
					}
					beforeOpts = append(beforeOpts, Obj{
						PropConditionOption: filter.PropConditionOption{
							Label: cputil.I18n(ctx, "%s %s", itrPrefix, plan.Name),
							Value: plan.ID,
						},
						itrCreateTime: func() time.Time {
							itr, ok := selectedItrsByID[plan.IterationID]
							if !ok {
								return time.Time{}
							}
							return itr.CreatedAt
						}(),
					})
				}
				slice.Sort(beforeOpts, func(i, j int) bool {
					return beforeOpts[i].itrCreateTime.After(beforeOpts[j].itrCreateTime)
				})
				for _, bo := range beforeOpts {
					opts = append(opts, bo.PropConditionOption)
				}
				return
			}(),
			Type: filter.PropConditionTypeSelect,
		},
	}

	// put selected values into global state
	h.SetMtBlockFilterTestPlanList(func() (selectedMtPlans []apistructs.TestPlan) {
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
