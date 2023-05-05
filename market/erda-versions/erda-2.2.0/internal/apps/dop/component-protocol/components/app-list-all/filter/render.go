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
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/app-list-all/common/gshelper"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type ComponentFilter struct {
	sdk *cptype.SDK
	filter.CommonFilter
	State    State `json:"state,omitempty"`
	gsHelper *gshelper.GSHelper
}

func init() {
	base.InitProviderWithCreator("app-list-all", "filter",
		func() servicehub.Provider { return &ComponentFilter{} },
	)
}

type State struct {
	FrontendConditionProps  []filter.PropCondition `json:"conditions,omitempty"`
	FrontendConditionValues FrontendConditions     `json:"values,omitempty"`
}

type FrontendConditions struct {
	Title string `json:"title,omitempty"`
}

func (f *ComponentFilter) InitFromProtocol(ctx context.Context, c *cptype.Component, gs *cptype.GlobalStateData) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, f); err != nil {
		return err
	}

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
				Key:         "title",
				Label:       "title",
				Placeholder: cputil.I18n(ctx, "searchAppName"),
				Type:        filter.PropConditionTypeInput,
			},
		}
	}

	if req, ok := f.gsHelper.GetAppPagingRequest(); ok {
		req.Query = f.State.FrontendConditionValues.Title
		f.gsHelper.SetAppPagingRequest(*req)
	}
	return f.SetToProtocolComponent(c)
}
