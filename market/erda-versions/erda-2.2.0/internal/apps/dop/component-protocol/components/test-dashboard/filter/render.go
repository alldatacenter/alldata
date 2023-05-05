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
	"encoding/base64"
	"encoding/json"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/test-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func (f *Filter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.initFromProtocol(ctx, c); err != nil {
		return err
	}

	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		if f.InParams.FrontendUrlQuery != "" {
			if err := f.initDefaultOperation(); err != nil {
				return err
			}
		}
	}

	// all iterations
	allIterations, _, err := f.iteration.Paging(apistructs.IterationPagingRequest{
		PageNo:              1,
		PageSize:            100,
		ProjectID:           f.InParams.ProjectID,
		WithoutIssueSummary: true,
	})
	if err != nil {
		return err
	}
	allIterationsByID := make(map[uint64]dao.Iteration)
	var allIterationIDs []uint64
	// selected iterations
	selectedIterationsByID := make(map[uint64]dao.Iteration)
	for _, itr := range allIterations {
		allIterationsByID[itr.ID] = itr
		allIterationIDs = append(allIterationIDs, itr.ID)
	}
	for _, sid := range f.State.Values.IterationIDs {
		selectedIterationsByID[sid] = allIterationsByID[sid]
	}
	if len(f.State.Values.IterationIDs) == 0 {
		selectedIterationsByID = allIterationsByID
	}

	// set state
	f.State.Conditions = []filter.PropCondition{
		{
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "iteration",
			Label:     cputil.I18n(ctx, "iteration"),
			Options: func() (opts []filter.PropConditionOption) { // options always show all iterations under project
				for _, itr := range allIterations {
					opts = append(opts, filter.PropConditionOption{
						Label: itr.Title,
						Value: itr.ID,
					})
				}
				return
			}(),
			Type: filter.PropConditionTypeSelect,
		},
	}

	// set to global state
	h := gshelper.NewGSHelper(gs)
	// set global testplans
	var selectedItrIDs []uint64
	for sid := range selectedIterationsByID {
		selectedItrIDs = append(selectedItrIDs, sid)
	}
	tpData, err := f.mtTestPlan.Paging(apistructs.TestPlanPagingRequest{
		ProjectID: f.InParams.ProjectID,
		IterationIDs: func() []uint64 {
			if len(f.State.Values.IterationIDs) > 0 {
				return f.State.Values.IterationIDs
			}
			return allIterationIDs
		}(),
		Type:         apistructs.TestPlanTypeManual,
		IsArchived:   nil,
		PageNo:       1,
		PageSize:     999,
		IdentityInfo: apistructs.IdentityInfo{InternalClient: "dop-test-dashboard"},
	})
	if err != nil {
		return err
	}
	h.SetGlobalManualTestPlanList(tpData.List)
	// set global selected itrs: set to all iterationIDs if non-selected
	h.SetGlobalSelectedIterationIDs(selectedItrIDs)
	h.SetGlobalSelectedIterationsByID(selectedIterationsByID)

	// set global auto test plan
	atData, err := f.atTestPlan.PagingTestPlansV2(&apistructs.TestPlanV2PagingRequest{
		ProjectID: f.InParams.ProjectID,
		IterationIDs: func() []uint64 {
			if len(f.State.Values.IterationIDs) > 0 {
				return f.State.Values.IterationIDs
			}
			return allIterationIDs
		}(),
		IsArchived:   nil,
		PageNo:       1,
		PageSize:     999,
		IdentityInfo: apistructs.IdentityInfo{InternalClient: "dop-test-dashboard"},
	})
	if err != nil {
		return err
	}
	h.SetGlobalAutoTestPlanList(atData.List)
	h.SetGlobalAutoTestPlanIDs(func() []uint64 {
		planIDs := make([]uint64, 0, len(atData.List))
		for _, v := range atData.List {
			planIDs = append(planIDs, v.ID)
		}
		return planIDs
	}())

	//  set global auto test scene and step
	if err = f.SetGlobalAtStep(h); err != nil {
		return err
	}

	urlParam, err := f.generateUrlQueryParams()
	if err != nil {
		return err
	}
	f.State.Base64UrlQueryParams = urlParam

	if err = f.setToComponent(c); err != nil {
		return err
	}
	return nil
}

func (f *Filter) SetGlobalAtStep(h *gshelper.GSHelper) error {
	steps, err := f.atTestPlan.ListStepByPlanID(h.GetGlobalAutoTestPlanIDs()...)
	if err != nil {
		return err
	}

	h.SetGlobalAtStep(steps)
	return nil
}

func (f *Filter) generateUrlQueryParams() (string, error) {
	fb, err := json.Marshal(f.State.Values)
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(fb), nil
}

func (f *Filter) initDefaultOperation() error {
	b, err := base64.StdEncoding.DecodeString(f.InParams.FrontendUrlQuery)
	if err != nil {
		return err
	}
	f.State.Values = SelectedValues{}
	if err = json.Unmarshal(b, &f.State.Values); err != nil {
		return err
	}
	return nil
}
