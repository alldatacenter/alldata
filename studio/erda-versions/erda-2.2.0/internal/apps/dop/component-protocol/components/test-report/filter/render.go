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

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
)

type ComponentAction struct {
	Name       string                 `json:"name"`
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	State      State                  `json:"state"`
	Operations map[string]interface{} `json:"operations"`
	InParams   InParams               `json:"-"`
}

type InParams struct {
	ProjectID uint64 `json:"projectId,omitempty"`
}

type State struct {
	Conditions []interface{} `json:"conditions"`
	Values     struct {
		Type      string   `json:"type"`
		Name      string   `json:"name"`
		Iteration []uint64 `json:"iteration"`
	} `json:"values"`
}

func (i *ComponentAction) setInParams(ctx context.Context) error {
	b, err := json.Marshal(cputil.SDK(ctx).InParams)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &i.InParams); err != nil {
		return err
	}

	return err
}

func (i *ComponentAction) GenComponentState(c *cptype.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	cont, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
		return err
	}
	err = json.Unmarshal(cont, &state)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return err
	}
	i.State = state
	return nil
}

func (ca *ComponentAction) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	if err := ca.GenComponentState(c); err != nil {
		return err
	}
	if err := ca.setInParams(ctx); err != nil {
		return err
	}
	bdl := ctx.Value(types.GlobalCtxKeyBundle).(*bundle.Bundle)
	sdk := cputil.SDK(ctx)
	ca.Name = "filter"
	ca.Type = "ContractiveFilter"
	ca.Operations = map[string]interface{}{
		"filter": map[string]interface{}{
			"key":    "filter",
			"reload": true,
		},
	}
	ca.Props = map[string]interface{}{
		"delay": 1000,
	}
	iterations, err := bdl.ListProjectIterations(apistructs.IterationPagingRequest{ProjectID: ca.InParams.ProjectID, PageSize: 999, WithoutIssueSummary: true}, sdk.Identity.OrgID)
	if err != nil {
		return err
	}
	iterationOptions := make([]interface{}, 0)
	for _, iteration := range iterations {
		iterationOptions = append(iterationOptions, map[string]interface{}{
			"label": iteration.Title,
			"value": iteration.ID,
		})
	}
	ca.State.Conditions = []interface{}{
		map[string]interface{}{
			"fixed":       true,
			"key":         "name",
			"placeholder": cputil.I18n(ctx, "filterByName"),
			"type":        "input",
		},
		map[string]interface{}{
			"emptyText": cputil.I18n(ctx, "all"),
			"fixed":     true,
			"key":       "iteration",
			"label":     cputil.I18n(ctx, "iteration"),
			"options":   iterationOptions,
			"type":      "select",
		},
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("test-report", "filter", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
