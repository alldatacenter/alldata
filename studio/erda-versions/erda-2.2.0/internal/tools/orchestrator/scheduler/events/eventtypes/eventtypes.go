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

package eventtypes

type StatusEvent struct {
	Type string `json:"type"`
	// ID Generate by {runtimeName}.{serviceName}.{dockerID}
	ID      string `json:"id,omitempty"`
	IP      string `json:"ip,omitempty"`
	Status  string `json:"status"`
	TaskId  string `json:"taskId,omitempty"`
	Cluster string `json:"cluster,omitempty"`
	Host    string `json:"host,omitempty"`
	Message string `json:"message,omitempty"`
}
