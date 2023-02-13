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

package addPodButton

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
)

type ComponentAddPodButton struct {
	sdk        *cptype.SDK
	Type       string                 `json:"type,omitempty"`
	Props      Props                  `json:"props"`
	Operations map[string]interface{} `json:"operations,omitempty"`
}

type Props struct {
	Text       string `json:"text,omitempty"`
	Type       string `json:"type,omitempty"`
	PrefixIcon string `json:"prefixIcon,omitempty"`
}

type Operation struct {
	Key    string `json:"key,omitempty"`
	Reload bool   `json:"reload"`
}
