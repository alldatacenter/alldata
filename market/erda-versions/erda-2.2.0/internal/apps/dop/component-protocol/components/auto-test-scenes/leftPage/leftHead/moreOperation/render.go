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

package moreOperation

import (
	"context"
	"encoding/json"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/component-protocol/cpregister/base"
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

type MoreOperation struct {
	Name       string                 `json:"name"`
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	Operations map[string]interface{} `json:"operations"`
}

func init() {
	base.InitProviderWithCreator("auto-test-scenes", "moreOperation",
		func() servicehub.Provider { return &MoreOperation{} })
}

func (m *MoreOperation) marshal(c *cptype.Component) error {

	propValue, err := json.Marshal(m.Props)
	if err != nil {
		return err
	}
	var props cptype.ComponentProps
	err = json.Unmarshal(propValue, &props)
	if err != nil {
		return err
	}

	c.Operations = m.Operations
	c.Props = props
	c.Type = m.Type
	c.Name = m.Name
	return nil
}

func (l *MoreOperation) Import(c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, l); err != nil {
		return err
	}
	return nil
}

func (m *MoreOperation) Render(ctx context.Context, c *cptype.Component, scenario cptype.Scenario, event cptype.ComponentEvent, gs *cptype.GlobalStateData) (err error) {
	if err := m.Import(c); err != nil {
		logrus.Errorf("import component failed, err:%v", err)
		return err
	}

	defer func() {
		fail := m.marshal(c)
		if err == nil && fail != nil {
			err = fail
		}
	}()
	m.Name = "moreOperation"
	m.Type = "Dropdown"
	m.Props = map[string]interface{}{
		"menus": []interface{}{
			map[string]interface{}{
				"key":   "import",
				"label": cputil.I18n(ctx, "import"),
			},
			map[string]interface{}{
				"key":   "record",
				"label": cputil.I18n(ctx, "import-export-record"),
			},
		},
	}
	m.Operations = map[string]interface{}{
		"import": map[string]interface{}{
			"key":    "import",
			"reload": false,
		},
		"record": map[string]interface{}{
			"key":    "record",
			"reload": false,
		},
	}
	return nil
}
