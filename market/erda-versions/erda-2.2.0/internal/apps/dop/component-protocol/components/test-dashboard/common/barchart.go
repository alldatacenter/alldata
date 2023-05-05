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

package common

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

type BarProps struct {
	ChartType string `json:"chartType"`
	Option    Option `json:"option"`
	Title     string `json:"title"`
}

type Option struct {
	Animation bool                   `json:"animation"`
	DataZoom  []DataZoom             `json:"dataZoom"`
	Grid      Grid                   `json:"grid"`
	Series    []Series               `json:"series"`
	XAxis     []XAxis                `json:"xAxis"`
	YAxis     []YAxis                `json:"yAxis"`
	Style     map[string]interface{} `json:"style"`
}

type DataZoom struct {
	EndValue       int64  `json:"endValue"`
	HandleSize     int64  `json:"handleSize"`
	Orient         string `json:"orient"`
	ShowDataShadow bool   `json:"showDataShadow"`
	ShowDetail     bool   `json:"showDetail"`
	StartValue     int64  `json:"startValue"`
	Throttle       int64  `json:"throttle"`
	Type           string `json:"type"`
	Width          int64  `json:"width"`
	ZoomLock       bool   `json:"zoomLock"`
}

type Grid struct {
	Right int64 `json:"right"`
}

type Series struct {
	Data  []Data                 `json:"data"`
	Label map[string]interface{} `json:"label"`
}

type Data struct {
	Operations map[string]interface{} `json:"operations"`
	Value      int64                  `json:"value"`
}

type Label struct {
	Show bool `json:"show"`
}

type Tooltip struct {
	Show    bool   `json:"show"`
	Trigger string `json:"trigger"`
}

type XAxis struct {
	Type      string                 `json:"type"`
	AxisLabel map[string]interface{} `json:"axisLabel"`
}

type YAxis struct {
	Type      string                 `json:"type"`
	Data      []string               `json:"data"`
	Inverse   bool                   `json:"inverse"`
	AxisLabel map[string]interface{} `json:"axisLabel"`
}

func NewBarProps(values []int64, categories []string, title, xFormatter string) cptype.ComponentProps {
	zooms := make([]DataZoom, 0)
	if len(categories) < 10 {
		for i := 10 - len(categories); i > 0; i-- {
			categories = append(categories, "")
			values = append(values, 0)
		}
	}
	if len(categories) > 10 {
		zooms = []DataZoom{
			{
				EndValue:   9,
				Orient:     "vertical",
				StartValue: 0,
				Throttle:   0,
				Type:       "inside",
				ZoomLock:   true,
			},
			{
				EndValue:       9,
				HandleSize:     15,
				Orient:         "vertical",
				ShowDataShadow: false,
				ShowDetail:     false,
				StartValue:     0,
				Throttle:       0,
				Type:           "slider",
				Width:          15,
				ZoomLock:       true,
			},
		}
	}
	data := make([]Data, 0, len(values))
	for _, v := range values {
		data = append(data, Data{
			Operations: nil,
			Value:      v,
		})
	}
	return cputil.MustConvertProps(BarProps{
		ChartType: "bar",
		Option: Option{
			Animation: false,
			DataZoom:  zooms,
			Grid: Grid{
				Right: 30,
			},
			Series: []Series{
				{
					Data:  data,
					Label: map[string]interface{}{"show": false},
				},
			},
			XAxis: []XAxis{
				{
					Type:      "value",
					AxisLabel: map[string]interface{}{"formatter": xFormatter},
				},
			},
			YAxis: []YAxis{
				{
					Type:      "category",
					Data:      categories,
					Inverse:   true,
					AxisLabel: map[string]interface{}{"interval": 0},
				},
			},
		},
		Title: title,
	})
}
