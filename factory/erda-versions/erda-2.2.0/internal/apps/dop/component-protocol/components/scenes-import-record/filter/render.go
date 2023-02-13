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
)

type ComponentAction struct {
	Name       string                 `json:"name"`
	Type       string                 `json:"type"`
	State      State                  `json:"state"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
}

type State struct {
	Conditions []interface{} `json:"conditions"`
	Values     struct {
		Type []string `json:"type"`
	} `json:"values"`
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
	ca.Props = map[string]interface{}{
		"delay": 1000,
	}
	ca.State.Conditions = []interface{}{
		map[string]interface{}{
			"emptyText": cputil.I18n(ctx, "all"),
			"fixed":     true,
			"key":       "type",
			"label":     cputil.I18n(ctx, "type"),
			"options": []interface{}{
				map[string]interface{}{
					"label": cputil.I18n(ctx, "import"),
					"value": "import",
				},
				map[string]interface{}{
					"label": cputil.I18n(ctx, "export"),
					"value": "export",
				},
			},
			"type": "select",
		},
	}
	ca.Operations = map[string]interface{}{
		"filter": map[string]interface{}{
			"key":    "filter",
			"reload": true,
		},
	}
	return nil
}

func init() {
	base.InitProviderWithCreator("scenes-import-record", "filter", func() servicehub.Provider {
		return &ComponentAction{}
	})
}
