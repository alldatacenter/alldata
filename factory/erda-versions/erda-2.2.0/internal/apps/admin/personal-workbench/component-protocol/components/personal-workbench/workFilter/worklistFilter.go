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

package workListFilter

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/common/gshelper"
	"github.com/erda-project/erda/internal/apps/admin/personal-workbench/component-protocol/components/personal-workbench/i18n"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

const OperationKeyFilter filter.OperationKey = "filter"

type ComponentFilter struct {
	sdk *cptype.SDK

	filter.CommonFilter
	State    State `json:"state,omitempty"`
	gsHelper *gshelper.GSHelper
}

type State struct {
	Tabs                    apistructs.WorkbenchItemType `json:"tabs"`
	FrontendConditionProps  []filter.PropCondition       `json:"conditions,omitempty"`
	FrontendConditionValues FrontendConditions           `json:"values,omitempty"`
}

type FrontendConditions struct {
	Title string `json:"title,omitempty"`
}

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "workListFilter",
		func() servicehub.Provider { return &ComponentFilter{} },
	)
}

func (f *ComponentFilter) InitFromProtocol(ctx context.Context, c *cptype.Component, gs *cptype.GlobalStateData) error {
	// component 序列化
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, f); err != nil {
		return err
	}

	// sdk
	f.sdk = cputil.SDK(ctx)
	f.gsHelper = gshelper.NewGSHelper(gs)
	return nil
}

func (f *ComponentFilter) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &c); err != nil {
		return err
	}
	return nil
}

func (f *ComponentFilter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c, gs); err != nil {
		return err
	}
	f.Operations = map[filter.OperationKey]filter.Operation{
		OperationKeyFilter: {Key: OperationKeyFilter, Reload: true},
	}
	tp, _ := f.gsHelper.GetWorkbenchItemType()
	var phTxt string
	if tp == apistructs.WorkbenchItemApp {
		phTxt = f.sdk.I18n(i18n.I18nKeySearchByAppName)
	} else {
		phTxt = f.sdk.I18n(i18n.I18nKeySearchByProjectName)
	}

	f.State.FrontendConditionProps = []filter.PropCondition{
		{
			Fixed:       true,
			Key:         "title",
			Placeholder: f.sdk.I18n(phTxt),
			Type:        filter.PropConditionTypeInput,
		},
	}
	switch event.Operation {
	case cptype.InitializeOperation:
		// init input when switch tab
		f.State.FrontendConditionValues = FrontendConditions{}
	}
	// set filter name to global state
	f.gsHelper.SetFilterName(f.State.FrontendConditionValues.Title)
	return f.SetToProtocolComponent(c)
}
