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

package stackChartFilter

import (
	"context"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/requirement-task-overview/common/gshelper"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

func init() {
	base.InitProviderWithCreator(common.ScenarioKey, "stackChartFilter", func() servicehub.Provider {
		return &Filter{}
	})
}

func (f *Filter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := f.InitFromProtocol(ctx, c); err != nil {
		return err
	}

	f.Props = filter.Props{
		Delay: 500,
	}

	f.Operations = map[filter.OperationKey]filter.Operation{
		OperationKeyFilter: {
			Key:    OperationKeyFilter,
			Reload: true,
		},
		OperationOwnerSelectMe: {
			Key:    OperationOwnerSelectMe,
			Reload: true,
		},
	}

	f.State = State{
		Conditions: []filter.PropCondition{
			{
				CustomProps: map[string]interface{}{
					"mode": "single",
				},
				Key:       "type",
				Label:     cputil.I18n(ctx, "type"),
				EmptyText: cputil.I18n(ctx, "all"),
				Fixed:     true,
				Type:      filter.PropConditionTypeSelect,
				Options: []filter.PropConditionOption{
					{
						Label: cputil.I18n(ctx, "requirement"),
						Value: "requirement",
					},
					{
						Label: cputil.I18n(ctx, "task"),
						Value: "task",
					},
				},
			},
		},
		Values: Values{
			Type: func() string {
				if event.Operation != cptype.OperationKey(f.Operations[OperationKeyFilter].Key) ||
					f.State.Values.Type == "" {
					return "requirement"
				}
				return f.State.Values.Type
			}(),
		},
	}

	h := gshelper.NewGSHelper(gs)
	h.SetStackChartType(f.State.Values.Type)

	return f.SetToProtocolComponent(c)
}
