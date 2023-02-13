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

package form_modal

type Props struct {
	Width      int                    `json:"width,omitempty"`
	Name       string                 `json:"name"`
	Title      string                 `json:"title"`
	Visible    bool                   `json:"visible"`
	Fields     []Field                `json:"fields,omitempty"`
	FormData   map[string]interface{} `json:"formData,omitempty"`
	FormRef    interface{}            `json:"formRef,omitempty"`
	ModalProps map[string]interface{} `json:"modalProps,omitempty"`
}

type Field struct {
	Key            string         `json:"key"`
	Label          string         `json:"label"`
	Required       bool           `json:"required"`
	Component      string         `json:"component"`
	Rules          []FieldRule    `json:"rules,omitempty"`
	ComponentProps ComponentProps `json:"componentProps"`
}

type FieldRule struct {
	Pattern string `json:"pattern"`
	Msg     string `json:"msg"`
}

type ComponentProps struct {
	Options   []Option ` json:"options,omitempty"`
	MaxLength int      `json:"maxLength"`
}

type Option struct {
	Name  string      `json:"name"`
	Value interface{} `json:"value"`
}
