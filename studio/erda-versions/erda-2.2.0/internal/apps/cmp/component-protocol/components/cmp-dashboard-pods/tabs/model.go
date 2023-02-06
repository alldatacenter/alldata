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

package tabs

type Tabs struct {
	Type       string                 `json:"type,omitempty"`
	Props      Props                  `json:"props"`
	State      State                  `json:"state"`
	Operations map[string]interface{} `json:"operations"`
}

type Props struct {
	ButtonStyle string   `json:"buttonStyle,omitempty"`
	Options     []Option `json:"options,omitempty"`
	RadioType   string   `json:"radioType,omitempty"`
	Size        string   `json:"size,omitempty"`
}

type Option struct {
	Key  string `json:"key,omitempty"`
	Text string `json:"text,omitempty"`
}

type State struct {
	Value string `json:"value"`
}

type Operation struct {
	Key    string `json:"key"`
	Reload bool   `json:"reload"`
}
