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

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"

	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-space-list/common"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-space-list/i18n"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/filter"
)

type ComponentFilter struct {
	sdk *cptype.SDK

	filter.CommonFilter
	State State `json:"state,omitempty"`
}

type State struct {
	Conditions []filter.PropCondition  `json:"conditions,omitempty"`
	Values     common.FilterConditions `json:"values,omitempty"`
}

func (i *ComponentFilter) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	i.sdk = cputil.SDK(ctx)
	i.Props = filter.Props{Delay: 1000}
	i.Operations = map[filter.OperationKey]filter.Operation{
		"filter": {
			Key:    "filter",
			Reload: true,
		},
	}
	i.State.Conditions = []filter.PropCondition{
		{
			EmptyText: cputil.I18n(ctx, "empty-filter-bookmark"),
			Fixed:     true,
			Key:       "order",
			Label:     cputil.I18n(ctx, "sort"),
			Options: []filter.PropConditionOption{
				{
					Label: cputil.I18n(ctx, "timeOrder"),
					Value: "updated_at",
				},
				{
					Label: cputil.I18n(ctx, "timeReverse"),
					Value: "updated_at desc",
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
			Key:       "archiveStatus",
			Label:     cputil.I18n(ctx, "state"),
			Options: []filter.PropConditionOption{
				{
					Label: i.sdk.I18n(i18n.I18nKeyAutoTestSpaceInit),
					Value: apistructs.TestSpaceInit,
				},
				{
					Label: i.sdk.I18n(i18n.I18nKeyAutoTestSpaceInProgress),
					Value: apistructs.TestSpaceInProgress,
				},
				{
					Label: i.sdk.I18n(i18n.I18nKeyAutoTestSpaceCompleted),
					Value: apistructs.TestSpaceCompleted,
				},
			},
			Type: filter.PropConditionTypeSelect,
		},
		{
			EmptyText:   cputil.I18n(ctx, "all"),
			Fixed:       true,
			Key:         "spaceName",
			Label:       cputil.I18n(ctx, "title"),
			Placeholder: cputil.I18n(ctx, "searchByName"),
			Type:        filter.PropConditionTypeInput,
		},
	}

	return nil
}

func init() {
	base.InitProviderWithCreator("auto-test-space-list", "filter",
		func() servicehub.Provider { return &ComponentFilter{} })
}
