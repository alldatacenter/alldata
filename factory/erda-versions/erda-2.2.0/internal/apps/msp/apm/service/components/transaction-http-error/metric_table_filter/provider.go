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

package metric_table_filter

import (
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-kanban/common/gshelper"
	error_transaction "github.com/erda-project/erda/internal/apps/msp/apm/service/common/error-transaction"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type ComponentFilter struct {
	sdk *cptype.SDK
	filter.CommonFilter
	State    State `json:"state,omitempty"`
	gsHelper *gshelper.GSHelper

	I18n i18n.Translator `autowired:"i18n" translator:"msp-i18n"`
}

func init() {
	name := "component-protocol.components.transaction-http-error.metricTableFilter"
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator("transaction-http-error", "metricTableFilter",
		func() servicehub.Provider { return &ComponentFilter{} },
	)
}

type State struct {
	Base64UrlQueryParams    string                 `json:"inputFilter__urlQuery,omitempty"`
	FrontendConditionProps  []filter.PropCondition `json:"conditions,omitempty"`
	FrontendConditionValues FrontendConditions     `json:"values,omitempty"`
}

type FrontendConditions struct {
	Duration []struct {
		Timer float64 `json:"timer,omitempty"`
		Unit  string  `json:"unit,omitempty"`
	} `json:"duration,omitempty"`
}

func (f *FrontendConditions) convertToTransactionFilter() error_transaction.ErrorTransactionFilter {
	errorTransactionFilter := error_transaction.ErrorTransactionFilter{}
	if len(f.Duration) != 2 {
		return errorTransactionFilter
	}
	d, err := time.ParseDuration(fmt.Sprintf("%f%s", f.Duration[0].Timer, f.Duration[0].Unit))
	if err == nil {
		errorTransactionFilter.MinDuration = float64(d)
	}
	d, err = time.ParseDuration(fmt.Sprintf("%f%s", f.Duration[1].Timer, f.Duration[1].Unit))
	if err == nil {
		errorTransactionFilter.MaxDuration = float64(d)
	}
	return errorTransactionFilter
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
				Key:         "duration",
				Label:       cputil.I18n(ctx, "duration"),
				Placeholder: cputil.I18n(ctx, "duration"),
				Type:        filter.PropConditionTypeTimespanRange,
			},
		}
	}

	var state State
	cputil.MustObjJSONTransfer(c.State, &state)
	error_transaction.SetFilterToGlobalState(*gs, state.FrontendConditionValues.convertToTransactionFilter())

	return f.SetToProtocolComponent(c)
}
