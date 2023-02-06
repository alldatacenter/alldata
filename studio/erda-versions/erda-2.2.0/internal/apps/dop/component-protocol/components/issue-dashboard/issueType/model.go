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

package issueType

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

type ComponentAction struct {
	sdk        *cptype.SDK
	State      State                  `json:"state,omitempty"`
	Props      Props                  `json:"props,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
}

type Props struct {
	ButtonStyle string   `json:"buttonStyle,omitempty"`
	Options     []Option `json:"options,omitempty"`
	RadioType   string   `json:"radioType,omitempty"`
}

type State struct {
	Value string `json:"value,omitempty"`
}

type Option struct {
	Key      string `json:"key,omitempty"`
	Text     string `json:"text,omitempty"`
	Disabled bool   `json:"disabled,omitempty"`
}

type Operation struct {
	Key    string `json:"key,omitempty"`
	Reload bool   `json:"reload"`
}
