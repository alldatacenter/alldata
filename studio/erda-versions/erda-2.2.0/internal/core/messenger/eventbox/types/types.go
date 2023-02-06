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

package types

import (
	"strings"

	"github.com/erda-project/erda/apistructs"
)

// LabelKey `Message.Labels' key
type LabelKey string

// Message 通用消息结构体
type Message struct {
	Sender        string                   `json:"sender"`
	Content       interface{}              `json:"content"`
	Labels        map[LabelKey]interface{} `json:"labels"`
	Time          int64                    `json:"time,omitempty"` // UnixNano
	CreateHistory *apistructs.CreateNotifyHistoryRequest

	originContent interface{} `json:"-"`
}

// Before 是否早于 `t'
func (m *Message) Before(t int64) bool {
	return m.Time < t
}

// OriginContent get `Message.originContent'
func (m *Message) OriginContent() interface{} {
	return m.originContent
}

// SetOriginContent set `Message.originContent'
func (m *Message) SetOriginContent(content interface{}) {
	m.originContent = content
}

// HasPrefix 格式化 labelkey & `s' 之后，判断是否有 `s' 前缀
func (k LabelKey) HasPrefix(s string) bool {
	k_ := k.Normalize()
	s_ := LabelKey(s).Normalize()
	return strings.HasPrefix(string(k_), s_)
}

// Equal 格式化 labelkey & `s' 之后，判断是否相等
func (k LabelKey) Equal(s string) bool {
	k_ := k.Normalize()
	s_ := LabelKey(s).Normalize()
	return s_ == k_
}

// Normalize 格式化 labelkey
func (k LabelKey) Normalize() string {
	if !strings.HasPrefix(string(k), "/") {
		return "/" + string(k)
	}
	return string(k)
}

// NormalizeLabelKey 格式化 labelkey & 转换类型
func (k LabelKey) NormalizeLabelKey() LabelKey {
	return LabelKey(k.Normalize())
}
