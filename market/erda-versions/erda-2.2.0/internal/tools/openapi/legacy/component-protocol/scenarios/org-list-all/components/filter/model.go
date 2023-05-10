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

package filter

import (
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentFilter struct {
	CtxBdl     protocol.ContextBundle
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
	Props      Props                  `json:"props"`
}

type Props struct {
	Delay int `json:"delay"`
}

type State struct {
	Conditions    []Condition       `json:"conditions"`
	Values        map[string]string `json:"values"`
	SearchEntry   string            `json:"searchEntry"`
	SearchRefresh bool              `json:"searchRefresh"`
}

type Condition struct {
	Key         string `json:"key"`
	Label       string `json:"label"`
	EmptyText   string `json:"emptyText"`
	Fixed       bool   `json:"fixed"`
	ShowIndex   int    `json:"showIndex"`
	Placeholder string `json:"placeholder"`
	Type        string `json:"type"`
}

type Operation struct {
	Key    string `json:"key"`
	Reload bool   `json:"reload"`
}
