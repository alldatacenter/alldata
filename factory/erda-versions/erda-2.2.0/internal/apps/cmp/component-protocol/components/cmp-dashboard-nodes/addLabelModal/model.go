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

package AddLabelModal

import (
	"context"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

type AddLabelModal struct {
	*cptype.SDK
	Ctx context.Context

	Type       string                `json:"type,omitempty"`
	Props      Props                 `json:"props,omitempty"`
	State      State                 `json:"state,omitempty"`
	Operations map[string]Operations `json:"operations,omitempty"`
}

type Props struct {
	Fields []Fields `json:"fields,omitempty"`
	Title  string   `json:"title,omitempty"`
}

type State struct {
	FormData map[string]string `json:"formData,omitempty"`
	Visible  bool              `json:"visible"`
}

type Operations struct {
	Key    string `json:"key,omitempty"`
	Reload bool   `json:"reload,omitempty"`
}

type Fields struct {
	Key            string         `json:"key,omitempty"`
	ComponentProps ComponentProps `json:"componentProps,omitempty"`
	Label          string         `json:"label,omitempty"`
	Component      string         `json:"component,omitempty"`
	Rules          Rules          `json:"rules,omitempty"`
	Required       bool           `json:"required,omitempty"`
	RemoveWhen     [][]RemoveWhen `json:"removeWhen,omitempty"`
}

type RemoveWhen struct {
	Field    string `json:"field,omitempty"`
	Operator string `json:"operator,omitempty"`
	Value    string `json:"value,omitempty"`
}

type ComponentProps struct {
	Options []Option `json:"options,omitempty"`
}

type Option struct {
	Name     string   `json:"name,omitempty"`
	Value    string   `json:"value,omitempty"`
	Children []Option `json:"children,omitempty"`
}

type Rules struct {
	Msg     string `json:"msg,omitempty"`
	Pattern string `json:"pattern,omitempty"`
}
