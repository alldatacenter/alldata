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

package edit_list

type Props struct {
	Visible bool   `json:"visible"`
	Temp    []Temp `json:"temp"`
}

type Temp struct {
	Title  string `json:"title"`
	Key    string `json:"key"`
	Width  int    `json:"width"`
	Flex   int    `json:"flex"`
	Render Render `json:"render"`
}

type Render struct {
	Type             string                 `json:"type,omitempty"` // 'input' | 'text' | 'select' | 'inputSelect'
	ValueConvertType string                 `json:"valueConvertType,omitempty"`
	Options          []PropChangeOption     `json:"options,omitempty"`
	Required         bool                   `json:"required,omitempty"`
	UniqueValue      bool                   `json:"uniqueValue,omitempty"`
	Operations       map[string]interface{} `json:"operations,omitempty"`
	Rules            []PropRenderRule       `json:"rules,omitempty"`
	Props            PropRenderProp         `json:"props,omitempty"`
	DefaultValue     interface{}            `json:"defaultValue,omitempty"`
}

type PropRenderProp struct {
	MaxLength   int64              `json:"maxLength,omitempty"`
	Placeholder string             `json:"placeholder,omitempty"`
	Options     []PropChangeOption `json:"options,omitempty"`
}

type PropRenderRule struct {
	Pattern string `json:"pattern,omitempty"`
	Msg     string `json:"msg,omitempty"`
}

type PropChangeOption struct {
	Label    string             `json:"label"`
	Value    string             `json:"value"`
	IsLeaf   bool               `json:"isLeaf"`
	Children []PropChangeOption `json:"children"`
}

func (pct *PropChangeOption) FindValue(v string) *PropChangeOption {
	if pct.Value == v {
		return pct
	}
	for i := range pct.Children {
		k := pct.Children[i].FindValue(v)
		if k != nil {
			return k
		}
	}
	return nil
}
