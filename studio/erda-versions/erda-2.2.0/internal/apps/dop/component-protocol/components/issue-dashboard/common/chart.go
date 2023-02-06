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

type Chart struct {
	Props Props `json:"props,omitempty"`
}

type Props struct {
	Title     string `json:"title,omitempty"`
	ChartType string `json:"chartType,omitempty"`
	Option    Option `json:"option,omitempty"`
}

type Option struct {
	XAxis  XAxis    `json:"xAxis,omitempty"`
	YAxis  YAxis    `json:"yAxis,omitempty"`
	Series []Item   `json:"series,omitempty"`
	Color  []string `json:"color,omitempty"`
	Legend Legend   `json:"legend,omitempty"`
	Grid   Grid     `json:"grid,omitempty"`
}

type Grid struct {
	Bottom int `json:"bottom,omitempty"`
	Top    int `json:"top,omitempty"`
	Right  int `json:"right,omitempty"`
}

type Legend struct {
	Show         bool `json:"show,omitempty"`
	Bottom       int  `json:"bottom,omitempty"`
	SelectedMode bool `json:"selectedMode,omitempty"`
}

type XAxis struct {
	Data      []string  `json:"data,omitempty"`
	Scale     bool      `json:"scale,omitempty"`
	AxisLabel AxisLabel `json:"axisLabel,omitempty"`
	SplitLine SplitLine `json:"splitLine,omitempty"`
	Type      string    `json:"type,omitempty"`
	Name      string    `json:"name,omitempty"`
}

type SplitLine struct {
	Show bool `json:"show"`
}

type AxisLabel struct {
	Fortmatter string `json:"formatter,omitempty"`
}

type YAxis struct {
	Max float32 `json:"max,omitempty"`
	XAxis
}

type Item struct {
	Name      string    `json:"name,omitempty"`
	Stack     string    `json:"stack,omitempty"`
	Label     Label     `json:"label,omitempty"`
	Data      []int     `json:"data,omitempty"`
	AreaStyle AreaStyle `json:"areaStyle,omitempty"`
}

type Label struct {
	Fortmatter string `json:"formatter,omitempty"`
	Normal     Normal `json:"normal,omitempty"`
}

type Normal struct {
	Position string `json:"position,omitempty"`
	Show     bool   `json:"show,omitempty"`
}

type AreaStyle struct {
	Opacity float32 `json:"opacity,omitempty"`
}

const PieChartFormat = "{b}\n{d}%"

type PieChart struct {
	Props PieChartProps `json:"props,omitempty"`
}

type PieChartProps struct {
	Title     string         `json:"title,omitempty"`
	ChartType string         `json:"chartType,omitempty"`
	Option    PieChartOption `json:"option,omitempty"`
}
type PieChartOption struct {
	Series []PieChartItem `json:"series,omitempty"`
	Color  []string       `json:"color,omitempty"`
	Legend []string       `json:"legend,omitempty"`
}

type PieChartItem struct {
	Name string         `json:"name,omitempty"`
	Data []PieChartPart `json:"data,omitempty"`
}

type PieChartPart struct {
	Name  string `json:"name,omitempty"`
	Value int    `json:"value,omitempty"`
	Label Label  `json:"label,omitempty"`
}

type MarkPoint struct {
	Data       []MarkItem `json:"data,omitempty"`
	SymbolSize int        `json:"symbolSize,omitempty"`
}

type MarkLine struct {
	LineStyle LineStyle  `json:"lineStyle,omitempty"`
	Data      []MarkItem `json:"data,omitempty"`
}

type LineStyle struct {
	Type string `json:"type,omitempty"`
}

type MarkItem struct {
	Name       string `json:"name,omitempty"`
	Type       string `json:"type,omitempty"`
	ValueIndex int    `json:"valueIndex"`
}
