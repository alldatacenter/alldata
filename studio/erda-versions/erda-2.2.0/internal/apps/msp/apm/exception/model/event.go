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

package model

type Event struct {
	EventId        string            `json:"event_id"`
	Timestamp      int64             `json:"timestamp"`
	RequestId      string            `json:"request_id"`
	ErrorId        string            `json:"error_id"`
	Stacks         []string          `json:"stacks"`
	Tags           map[string]string `json:"tags"`
	MetaData       map[string]string `json:"meta_data"`
	RequestContext map[string]string `json:"request_context"`
	RequestHeaders map[string]string `json:"request_headers"`
}
