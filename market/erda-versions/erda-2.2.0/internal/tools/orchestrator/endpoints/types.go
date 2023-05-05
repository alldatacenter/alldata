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

package endpoints

type RuntimeStatusEventReq struct {
	RuntimeName     string               `json:"runtimeName"`
	IsDeleted       bool                 `json:"isDeleted"`
	EventType       string               `json:"eventType"` // total(全量事件)/increment(增量事件)
	ServiceStatuses []ServiceStatusEvent `json:"serviceStatuses"`
}

type ServiceStatusEvent struct {
	ServiceName      string                `json:"serviceName"`
	Status           string                `json:"serviceStatus"`
	Replica          int                   `json:"replica"`
	InstanceStatuses []InstanceStatusEvent `json:"instanceStatuses"`
}

type InstanceStatusEvent struct {
	TaskId string                 `json:"id"` // TaskId
	IP     string                 `json:"ip"`
	Status string                 `json:"instanceStatus"`
	Stage  string                 `json:"stage"`
	Extra  map[string]interface{} `json:"extra"`
}
