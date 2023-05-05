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

package notifyConfigModal

import (
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

var (
	TypeOperation = map[string][]Option{
		"role": {
			{
				Name:  "email",
				Value: "email",
			},
			{
				Name:  "message",
				Value: "mbox",
			},
		},
		"user": {
			{
				Name:  "邮箱",
				Value: "email",
			},
			{
				Name:  "站内信",
				Value: "zhanneixin",
			},
		},
		"dingding": {
			{
				Name:  "DingTalk",
				Value: "dingding",
			},
		},
		"webhook": {
			{
				Name:  "webhook",
				Value: "webhook",
			},
		},
		"external_user": {
			{
				Name:  "邮箱",
				Value: "email",
			},
		},
	}
)

type ComponentModel struct {
	CtxBdl     protocol.ContextBundle
	Type       string         `json:"type"`
	Operations ModalOperation `json:"operations"`
	Props      Props          `json:"props"`
	State      State          `json:"state"`
}

type ModalOperation struct {
	Submit Submit `json:"submit"`
}

type Submit struct {
	Key    string `json:"key"`
	Reload bool   `json:"reload"`
}

type Props struct {
	Name   string  `json:"name"`
	Fields []Field `json:"fields"`
}

type State struct {
	Operation string                 `json:"operation"`
	EditId    uint64                 `json:"editId"`
	Visible   bool                   `json:"visible"`
	FormData  map[string]interface{} `json:"formData"`
}

type Field struct {
	Key            string         `json:"key"`
	Label          string         `json:"label"`
	Component      string         `json:"component"`
	Required       bool           `json:"required"`
	ComponentProps ComponentProps `json:"componentProps"`
	RemoveWhen     [][]RemoveWhen `json:"removeWhen"`
	Disabled       bool           `json:"disabled"`
}

type RemoveWhen struct {
	Field    string `json:"field"`
	Operator string `json:"operator"`
	Value    string `json:"value"`
}
type ComponentProps struct {
	Mode        string   `json:"mode"`
	PlaceHolder string   `json:"placeHolder"`
	Options     []Option `json:"options"`
	MaxLength   int64    `json:"maxLength"`
}
type Option struct {
	Name  string `json:"name"`
	Value string `json:"value"`
}

type TargetInfo struct {
	Channels []string `json:"channels"`
	GroupId  int64    `json:"group_id"`
}
