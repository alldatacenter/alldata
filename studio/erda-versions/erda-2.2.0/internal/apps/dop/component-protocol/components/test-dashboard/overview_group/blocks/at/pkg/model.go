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

package pkg

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
)

const (
	ColorTextMain = "text-main"
	ColorTextDesc = "text-desc"
)

type TextValue struct {
	Value      string
	Kind       string
	ValueColor string
}

type (
	Props struct {
		RenderType string     `json:"renderType"`
		Value      PropsValue `json:"value"`
	}
	PropsValue struct {
		Direction string         `json:"direction"`
		Text      PropsValueText `json:"text"`
	}
	PropsValueText     []PropsValueTextItem
	PropsValueTextItem struct {
		StyleConfig PropsValueTextStyleConfig `json:"styleConfig"`
		Text        string                    `json:"text"`
	}
	PropsValueTextStyleConfig struct {
		Bold       bool   `json:"bold,omitempty"`
		Color      string `json:"color"`
		FontSize   uint64 `json:"fontSize,omitempty"`
		LineHeight uint64 `json:"lineHeight,omitempty"`
	}
)

func (tv *TextValue) ConvertToProps() cptype.ComponentProps {
	return cputil.MustConvertProps(Props{
		RenderType: "linkText",
		Value: PropsValue{
			Direction: "col",
			Text: []PropsValueTextItem{
				{
					StyleConfig: PropsValueTextStyleConfig{
						Bold:       true,
						Color:      ColorTextMain,
						FontSize:   20,
						LineHeight: 32,
					},
					Text: tv.Value,
				},
				{
					StyleConfig: PropsValueTextStyleConfig{
						Bold:       false,
						Color:      ColorTextDesc,
						FontSize:   0,
						LineHeight: 22,
					},
					Text: tv.Kind,
				},
			},
		},
	})
}
