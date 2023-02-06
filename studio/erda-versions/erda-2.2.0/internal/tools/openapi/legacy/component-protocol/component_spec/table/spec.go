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

type Props struct {
	PageSizeOptions []string               `json:"pageSizeOptions"`
	Columns         []Column               `json:"columns"`
	RowKey          string                 `json:"rowKey"`
	ClassName       string                 `json:"className"`
	Title           string                 `json:"title"`
	Visible         bool                   `json:"visible"`
	RowSelection    map[string]interface{} `json:"rowSelection"`
}

type Column struct {
	Title           string      `json:"title"`
	DataIndex       string      `json:"dataIndex"`
	TitleRenderType string      `json:"titleRenderType"`
	Width           int         `json:"width,omitempty"`
	TitleTip        []string    `json:"titleTip"`
	Data            interface{} `json:"data"`
}

type Data struct {
	List []RowData `json:"list"`
}

type RowData map[string]interface{}
