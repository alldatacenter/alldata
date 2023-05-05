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

package chart

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

type BarProps struct {
	ChartType string `json:"chartType"`
	Option    Option `json:"option"`
	Title     string `json:"title"`
	PureChart bool   `json:"pureChart"`
}

type Option struct {
	Animation bool                   `json:"animation"`
	DataZoom  []DataZoom             `json:"dataZoom"`
	Grid      Grid                   `json:"grid"`
	Series    []Series               `json:"series"`
	XAxis     []XAxis                `json:"xAxis"`
	YAxis     []YAxis                `json:"yAxis"`
	Tooltip   map[string]interface{} `json:"tooltip"`
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
	Data      []Data                 `json:"data"`
	Label     map[string]interface{} `json:"label"`
	AreaStyle map[string]interface{} `json:"areaStyle"`
	Emphasis  map[string]interface{} `json:"emphasis"`
	ItemStyle map[string]interface{} `json:"itemStyle"`
	Stack     string                 `json:"stack"`
	Tooltip   map[string]interface{} `json:"tooltip"`
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

func NewBarProps(values1, values2 []int64, categories []string, title string) cptype.ComponentProps {
	zooms := make([]DataZoom, 0)
	if len(categories) < 10 {
		for i := 10 - len(categories); i > 0; i-- {
			categories = append(categories, "")
			values1 = append(values1, 0)
			values2 = append(values2, 0)
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
	data1 := make([]Data, 0, len(values1))
	data2 := make([]Data, 0, len(values1))
	for _, v := range values1 {
		data1 = append(data1, Data{
			Operations: nil,
			Value:      v,
		})
	}
	for _, v := range values2 {
		data2 = append(data2, Data{
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
					Data:      data1,
					AreaStyle: map[string]interface{}{"opacity": 0.1},
					Emphasis: map[string]interface{}{
						"itemStyle": map[string]interface{}{
							"barBorderColor": "rgba(0,0,0,0)",
							"color":          "rgba(0,0,0,0)",
						},
					},
					ItemStyle: map[string]interface{}{
						"barBorderColor": "rgba(0,0,0,0)",
						"color":          "rgba(0,0,0,0)",
					},
					Stack:   "总量",
					Tooltip: map[string]interface{}{"show": false},
				},
				{
					Data:      data2,
					AreaStyle: map[string]interface{}{"opacity": 0.1},
					Stack:     "总量",
					Tooltip:   map[string]interface{}{"show": true},
					Label: map[string]interface{}{
						"position": "top",
						"show":     false,
					},
				},
			},
			Tooltip: map[string]interface{}{
				"show": true,
			},
			XAxis: []XAxis{
				{
					AxisLabel: map[string]interface{}{"formatter": "{value}s"},
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
		Title:     title,
		PureChart: true,
	})
}
