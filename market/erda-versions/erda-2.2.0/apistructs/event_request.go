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

package apistructs

import (
	"encoding/json"

	"github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
)

// EventCreateRequest  用于发送 event 的 json request (非 OPENAPI)
// POST: <eventbox>/api/dice/eventbox/message/create
type EventCreateRequest struct {
	EventHeader
	Sender  string
	Content interface{}
}

// MarshalJSON EventCreateRequest 的自定义 marshal 方法
// 将 EventCreateRequest 序列化的结果匹配 internal/eventbox/webhook.EventMessage
// 也就是 使用者只要构造 EventCreateRequest, 而不用去构造 EventMessage
func (r EventCreateRequest) MarshalJSON() ([]byte, error) {
	result := struct {
		Sender  string      `json:"sender"`
		Content interface{} `json:"content"`
		Labels  struct {
			Webhook EventHeader `json:"WEBHOOK"`
		} `json:"labels"`
	}{
		Sender:  r.Sender,
		Content: r.Content,
		Labels: struct {
			Webhook EventHeader `json:"WEBHOOK"`
		}{Webhook: r.EventHeader},
	}
	return json.Marshal(result)
}

func (r *EventCreateRequest) ConvertToPB() (*pb.CreateMessageRequest, error) {
	reqByte, err := json.Marshal(r)
	if err != nil {
		return nil, err
	}
	reqPb := pb.CreateMessageRequest{}
	if err := json.Unmarshal(reqByte, &reqPb); err != nil {
		return nil, err
	}
	return &reqPb, nil
}
