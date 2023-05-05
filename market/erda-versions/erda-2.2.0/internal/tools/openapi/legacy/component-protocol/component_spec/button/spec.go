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

package button

type Props struct {
	Text       string                 `json:"text"`
	Type       interface{}            `json:"type"`
	Ghost      bool                   `json:"ghost"`
	Menu       []MenuItem             `json:"menu"`
	PrefixIcon string                 `json:"prefixIcon"`
	Style      map[string]interface{} `json:"style"`
	SuffixIcon string                 `json:"suffixIcon"`
	Tooltip    string                 `json:"tooltip"`
	Visible    bool                   `json:"visible,omitempty"`
}

type MenuItem struct {
	Key         string                 `json:"key"`
	Text        string                 `json:"text"`
	Disabled    bool                   `json:"disabled"`
	DisabledTip string                 `json:"disabledTip"`
	PrefixIcon  string                 `json:"prefixIcon"`
	Confirm     string                 `json:"confirm"`
	Operations  map[string]interface{} `json:"operations"`
}
