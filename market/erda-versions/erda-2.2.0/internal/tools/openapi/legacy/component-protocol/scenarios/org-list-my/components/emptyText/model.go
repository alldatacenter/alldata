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

package emptyText

import (
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentEmptyText struct {
	CtxBdl     protocol.ContextBundle
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
	Props      Props                  `json:"props"`
}

type Props struct {
	Visible     bool                     `json:"visible"`
	RenderType  string                   `json:"renderType"`
	StyleConfig Config                   `json:"styleConfig"`
	Value       map[string][]interface{} `json:"value"`
}

type State struct {
	IsEmpty bool `json:"isEmpty"`
}

type Config struct {
	FontSize   int `json:"fontSize"`
	LineHeight int `json:"lineHeight"`
}

type Redirect struct {
	Text         string         `json:"text"`
	OperationKey string         `json:"operationKey"`
	StyleConfig  RedirectConfig `json:"styleConfig"`
}

type RedirectConfig struct {
	Bold bool `json:"bold"`
}

type Operation struct {
	Key     string  `json:"key"`
	Reload  bool    `json:"reload"`
	Command Command `json:"command"`
}

type Command struct {
	Key          string `json:"key"`
	ScenarioType string `json:"scenarioType"`
	ScenarioKey  string `json:"scenarioKey"`
}
