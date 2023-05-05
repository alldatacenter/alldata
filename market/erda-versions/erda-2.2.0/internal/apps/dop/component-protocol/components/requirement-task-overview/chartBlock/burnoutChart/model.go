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

package burnoutChart

import (
	"context"
	"encoding/json"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

type BurnoutChart struct {
	Type   string          `json:"type"`
	Props  Props           `json:"props"`
	Issues []dao.IssueItem `json:"-"`
}

type (
	Props struct {
		ChartType string `json:"chartType"`
		Title     string `json:"title"`
		PureChart bool   `json:"pureChart"`
		Option    Option `json:"option"`
	}

	Option struct {
		XAxis   XAxis                  `json:"xAxis"`
		YAxis   YAxis                  `json:"yAxis"`
		Legend  Legend                 `json:"legend"`
		Tooltip map[string]interface{} `json:"tooltip"`
		Series  []Series               `json:"series"`
	}

	XAxis struct {
		Type string   `json:"type"`
		Data []string `json:"data"`
	}

	YAxis struct {
		Type      string                 `json:"type"`
		AxisLine  map[string]interface{} `json:"axisLine"`
		AxisLabel map[string]interface{} `json:"axisLabel"`
		Max       string                 `json:"max"`
	}

	Legend struct {
		Show   bool     `json:"show"`
		Bottom bool     `json:"bottom"`
		Data   []string `json:"data"`
	}

	Series struct {
		Data      []string               `json:"data"`
		Name      string                 `json:"name"`
		Type      string                 `json:"type"`
		Smooth    bool                   `json:"smooth"`
		ItemStyle map[string]interface{} `json:"itemStyle"`
		LineStyle map[string]interface{} `json:"lineStyle,omitempty"`
	}

	Data struct {
		Name   string   `json:"name,omitempty"`
		Coord  []string `json:"coord"`
		Symbol string   `json:"symbol,omitempty"`
	}
)

func (f *BurnoutChart) SetToProtocolComponent(c *cptype.Component) error {
	b, err := json.Marshal(f)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, &c)
}

func (f *BurnoutChart) InitFromProtocol(ctx context.Context, c *cptype.Component) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	return json.Unmarshal(b, f)
}
