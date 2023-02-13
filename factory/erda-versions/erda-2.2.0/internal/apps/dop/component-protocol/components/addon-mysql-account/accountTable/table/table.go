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

package table

var (
	_true  = true
	_false = false
)

func True() *bool {
	return &_true
}

func False() *bool {
	return &_false
}

type Props struct {
	ShowHeader      *bool                  `json:"showHeader,omitempty"`
	ShowPagination  *bool                  `json:"pagination,omitempty"`
	PageSizeOptions []string               `json:"pageSizeOptions"`
	Columns         []*ColumnTitle         `json:"columns"`
	RowKey          string                 `json:"rowKey"`
	ClassName       string                 `json:"className"`
	Title           string                 `json:"title"`
	Size            string                 `json:"size"`
	Visible         *bool                  `json:"visible,omitempty"`
	Bordered        *bool                  `json:"bordered,omitempty"`
	RowSelection    map[string]interface{} `json:"rowSelection"`
	RequestIgnore   []string               `json:"requestIgnore,omitempty"`
}

type ColumnTitle struct {
	Title           string      `json:"title"`
	DataIndex       string      `json:"dataIndex"`
	TitleRenderType string      `json:"titleRenderType"`
	Width           int         `json:"width,omitempty"`
	TitleTip        []string    `json:"titleTip"`
	Data            interface{} `json:"data"`
}

type ColumnData struct {
	Value      string                `json:"value"`
	RenderType string                `json:"renderType,omitempty"`
	Tags       []ColumnDataTag       `json:"tags,omitempty"`
	Operations map[string]*Operation `json:"operations,omitempty"`
}

type ColumnDataTag struct {
	Tag   string `json:"tag"`
	Color string `json:"color"`
}
