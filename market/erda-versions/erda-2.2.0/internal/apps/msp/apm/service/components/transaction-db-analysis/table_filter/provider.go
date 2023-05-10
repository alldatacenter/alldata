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

package table_filter

import (
	"context"
	"encoding/base64"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-kanban/common/gshelper"
	"github.com/erda-project/erda/internal/apps/msp/apm/service/common/transaction"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type ComponentFilter struct {
	sdk *cptype.SDK
	filter.CommonFilter
	State State `json:"state,omitempty"`

	gsHelper *gshelper.GSHelper
}

func init() {
	name := "component-protocol.components.transaction-db-analysis.tableFilter"
	cpregister.AllExplicitProviderCreatorMap[name] = nil
	base.InitProviderWithCreator("transaction-db-analysis", "tableFilter",
		func() servicehub.Provider { return &ComponentFilter{} },
	)
}

type State struct {
	Base64UrlQueryParams    string                 `json:"inputFilter__urlQuery,omitempty"`
	FrontendConditionProps  []filter.PropCondition `json:"conditions,omitempty"`
	FrontendConditionValues FrontendConditions     `json:"values,omitempty"`
}

type FrontendConditions struct {
	Title string `json:"title,omitempty"`
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

func (f *ComponentFilter) generateUrlQueryParams() (string, error) {
	fb, err := json.Marshal(f.State.FrontendConditionValues)
	if err != nil {
		return "", err
	}
	return base64.StdEncoding.EncodeToString(fb), nil
}

func (f *ComponentFilter) flushOptsByFilter(filterEntity string) error {
	b, err := base64.StdEncoding.DecodeString(filterEntity)
	if err != nil {
		return err
	}
	f.State.FrontendConditionValues = FrontendConditions{}
	return json.Unmarshal(b, &f.State.FrontendConditionValues)
}

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
				Key:         "title",
				Label:       "title",
				Placeholder: f.sdk.I18n("SearchByTransactionName"),
				Type:        filter.PropConditionTypeInput,
			},
		}
		if f.State.FrontendConditionValues.Title == "" {
			if urlquery := f.sdk.InParams.String("inputFilter__urlQuery"); urlquery != "" {
				if err := f.flushOptsByFilter(urlquery); err != nil {
					return err
				}
			}
		}
	}
	nameMap := map[string]interface{}{}
	if x, ok := c.State["values"]; ok {
		nameMap = x.(map[string]interface{})
	}

	if name, ok := nameMap["title"]; ok {
		(*gs)[transaction.StateKeyTransactionLayerPathFilter] = name
	}
	return f.SetToProtocolComponent(c)
}
