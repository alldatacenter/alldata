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

package searchFilter

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/msp/apm/alert/components/msp-notify-list/common"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type ComponentFilter struct {
	sdk *cptype.SDK
	filter.CommonFilter

	State State           `json:"state,omitempty"`
	I18n  i18n.Translator `autowired:"i18n" translator:"msp-alert-event-list"`
}

func init() {
	name := fmt.Sprintf("component-protocol.components.%s.%s", common.ScenarioKey, common.ComponentNameSearchFilter)
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator(common.ScenarioKey, common.ComponentNameSearchFilter,
		func() servicehub.Provider { return &ComponentFilter{} },
	)
}

type State struct {
	Base64UrlQueryParams    string                 `json:"inputFilter__urlQuery,omitempty"`
	FrontendConditionProps  []filter.PropCondition `json:"conditions,omitempty"`
	FrontendConditionValues FrontendConditions     `json:"values,omitempty"`
}

type FrontendConditions struct {
	Name string `json:"name,omitempty"`
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
	if f.I18n != nil {
		f.sdk.Tran = f.I18n
	}
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

const OperationKeyFilter filter.OperationKey = "filter"

func (f *ComponentFilter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c, gs); err != nil {
		return err
	}
	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		f.Operations = map[filter.OperationKey]filter.Operation{
			OperationKeyFilter: {Key: OperationKeyFilter, Reload: true},
		}
		f.State.FrontendConditionProps = []filter.PropCondition{
			{
				EmptyText:   "all",
				Fixed:       true,
				Key:         "name",
				Label:       f.sdk.I18n("name"),
				Placeholder: f.sdk.I18n("searchByName"),
				Type:        filter.PropConditionTypeInput,
			},
		}
	}

	var state State
	cputil.MustObjJSONTransfer(c.State, &state)

	common.NewConfigurableFilterOptions().
		GetFromGlobalState(*gs).
		UpdateName(state.FrontendConditionValues.Name).
		SetToGlobalState(*gs)

	return f.SetToProtocolComponent(c)
}
