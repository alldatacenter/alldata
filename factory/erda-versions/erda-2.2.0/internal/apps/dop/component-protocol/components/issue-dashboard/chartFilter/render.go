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

package chartFilter

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/stackhandlers"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func init() {
	base.InitProviderWithCreator("issue-dashboard", "chartFilter",
		func() servicehub.Provider { return &ComponentFilter{} })
}

func (f *ComponentFilter) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
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
	if err := f.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	if err := f.InitDefaultOperation(ctx, c, gs); err != nil {
		return err
	}

	return f.SetToProtocolComponent(c)
}

func (f *ComponentFilter) InitDefaultOperation(ctx context.Context, c *cptype.Component, gs *cptype.GlobalStateData) error {
	if f.State.Values.Type == "" {
		f.State.Values.Type = stackhandlers.Priority
	}
	if f.State.FrontendChangedKey == "type" {
		f.State.Values.Value = nil
	}
	helper := gshelper.NewGSHelper(gs)
	handler := stackhandlers.NewStackRetriever(
		stackhandlers.WithIssueStateList(helper.GetIssueStateList()),
		stackhandlers.WithIssueStageList(helper.GetIssueStageList()),
	).GetRetriever(f.State.Values.Type)

	options := []filter.PropConditionOption{
		{
			Label: cputil.I18n(ctx, "priority"),
			Value: stackhandlers.Priority,
		},
		{
			Label: cputil.I18n(ctx, "complexity"),
			Value: stackhandlers.Complexity,
		},
		{
			Label: cputil.I18n(ctx, "severity"),
			Value: stackhandlers.Severity,
		},
	}
	if c.Name == "stateVerticalBar" || c.Name == "scatter" {
		options = append(options, filter.PropConditionOption{
			Label: cputil.I18n(ctx, "import-source"),
			Value: stackhandlers.Stage,
		})
	} else {
		options = append(options, []filter.PropConditionOption{
			{
				Label: cputil.I18n(ctx, "state"),
				Value: stackhandlers.State,
			},
			{
				Label: cputil.I18n(ctx, "import-source"),
				Value: stackhandlers.Stage,
			},
		}...)
	}
	f.State.Conditions = []filter.PropCondition{
		{
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "type",
			Label:     cputil.I18n(ctx, "type"),
			Options:   options,
			Required:  true,
			Type:      filter.PropConditionTypeSelect,
			CustomProps: map[string]interface{}{
				"mode": "single",
			},
		},
		{
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "value",
			Label:     cputil.I18n(ctx, "value"),
			Options:   handler.GetFilterOptions(ctx),
			Type:      filter.PropConditionTypeSelect,
		},
	}

	f.Props = filter.Props{
		Delay: 1000,
	}
	f.Operations = map[filter.OperationKey]filter.Operation{
		OperationKeyFilter: {
			Key:    OperationKeyFilter,
			Reload: true,
		},
	}

	return nil
}
