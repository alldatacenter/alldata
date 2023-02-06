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

package trendChartFilter

import (
	"context"
	"encoding/json"
	"time"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/issue-dashboard/common/stackhandlers"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func init() {
	base.InitProviderWithCreator("issue-dashboard", "trendChartFilter",
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
	// f.bdl = ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	// f.issueSvc = ctx.Value(types.IssueService).(*issue.Issue)
	// if err := f.setInParams(ctx); err != nil {
	// 	return err
	// }

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

	switch event.Operation {
	case cptype.InitializeOperation, cptype.RenderingOperation:
		f.State.Values.Time = nil
	case cptype.OperationKey(f.Operations[OperationKeyFilter].Key):
	}

	if err := f.InitDefaultOperation(ctx, f.State); err != nil {
		return err
	}

	return f.SetToProtocolComponent(c)
}

func (f *ComponentFilter) InitDefaultOperation(ctx context.Context, state State) error {
	if f.State.Values.Type == "" {
		f.State.Values.Type = stackhandlers.Priority
	}
	if f.State.FrontendChangedKey == "type" {
		f.State.Values.Value = nil
	}
	if f.State.Values.Time == nil {
		f.State.Values.Time = []int64{makeTimestamp(time.Now().AddDate(0, -1, 0)), makeTimestamp(time.Now())}
	}
	handler := stackhandlers.NewStackRetriever().GetRetriever(f.State.Values.Type)
	f.State.Conditions = []filter.PropCondition{
		{
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "type",
			Label:     cputil.I18n(ctx, "type"),
			Options: []filter.PropConditionOption{
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
			},
			Required: true,
			Type:     filter.PropConditionTypeSelect,
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
		{
			EmptyText: cputil.I18n(ctx, "all"),
			Fixed:     true,
			Key:       "time",
			Label:     cputil.I18n(ctx, "time"),
			Type:      filter.PropConditionTypeRangePicker,
			CustomProps: map[string]interface{}{
				"allowClear":     false,
				"selectableTime": []int64{makeTimestamp(time.Now().AddDate(0, -2, 0)), makeTimestamp(time.Now())},
			},
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

func makeTimestamp(t time.Time) int64 {
	return t.UnixNano() / int64(time.Millisecond)
}
