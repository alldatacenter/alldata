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

package panel

type Props struct {
	Visible        bool                   `json:"visible"`
	Fields         []Field                `json:"fields"`
	Column         int                    `json:"column"`
	Colon          bool                   `json:"colon"`
	ColumnNum      int                    `json:"columnNum"`
	IsMultiColumn  bool                   `json:"isMultiColumn"`
	Layout         PropsLayout            `json:"layout"` // 'vertical' | 'horizontal'
	Data           map[string]interface{} `json:"data"`
	Type           PropsType              `json:"type"` // 'Z' | 'N'
	NumOfRowsLimit int                    `json:"numOfRowsLimit"`
}

type Field struct {
	Label      string      `json:"label"`
	ValueKey   interface{} `json:"valueKey"`
	RenderType string      `json:"renderType"` // ellipsis
}

type PropsLayout string

const (
	PropsLayoutVertical   PropsLayout = "vertical"
	PropsLayoutHorizontal PropsLayout = "horizontal"
)

type PropsType string

const (
	PropsTypeZ PropsType = "Z"
	PropsTypeN PropsType = "N"
)

type Data struct {
	Data map[string]interface{} `json:"data"`
}
