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

package list

import (
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentList struct {
	CtxBdl     protocol.ContextBundle
	Operations map[string]interface{} `json:"operations"`
	State      State                  `json:"state"`
	Props      Props                  `json:"props"`
	Data       []Org                  `json:"data"`
}

type State struct {
	PageNo        int    `json:"pageNo"`
	PageSize      int    `json:"pageSize"`
	Total         int    `json:"total"`
	SearchEntry   string `json:"searchEntry"`
	SearchRefresh bool   `json:"searchRefresh"`
}

type Org struct {
	Id          uint64                 `json:"id"`
	Title       string                 `json:"title"`
	Description string                 `json:"description"`
	PrefixImg   string                 `json:"prefixImg"`
	ExtraInfos  []ExtraInfo            `json:"extraInfos"`
	Operations  map[string]interface{} `json:"operations"`
}

type ExtraInfo struct {
	Icon    string `json:"icon"`
	Text    string `json:"text"`
	ToolTip string `json:"tooltip"`
}

type Option struct {
	Key  string `json:"key"`
	Text string `json:"text"`
}

type OperationBase struct {
	Key    string `json:"key"`
	Reload bool   `json:"reload"`
}

type ClickOperation struct {
	Key     string  `json:"key"`
	Show    bool    `json:"show"`
	Reload  bool    `json:"reload"`
	Command Command `json:"command"`
}

type ManageOperation struct {
	Key     string  `json:"key"`
	Text    string  `json:"text"`
	Reload  bool    `json:"reload"`
	Command Command `json:"command"`
}

type ExitOperation struct {
	Key     string `json:"key"`
	Text    string `json:"text"`
	Reload  bool   `json:"reload"`
	Confirm string `json:"confirm"`
	Meta    Meta   `json:"meta"`
}

type Meta struct {
	Id uint64 `json:"id"`
}

type Command struct {
	Key     string       `json:"key"`
	Target  string       `json:"target"`
	JumpOut bool         `json:"jumpOut"`
	State   CommandState `json:"state"`
}

type CommandState struct {
	Params Params `json:"params"`
}

type Params struct {
	OrgName string `json:"orgName"`
}

type Props struct {
	PageSizeOptions []string `json:"pageSizeOptions"`
}
