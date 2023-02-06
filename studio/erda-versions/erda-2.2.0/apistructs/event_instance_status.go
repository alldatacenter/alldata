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

// InstanceStatusData 是调度器为实例状态变化事件而定义的结构体
type InstanceStatusData struct {
	ClusterName string `json:"clusterName,omitempty"`
	RuntimeName string `json:"runtimeName,omitempty"`
	ServiceName string `json:"serviceName,omitempty"`

	// 事件id
	// k8s 中是 containerID
	// marathon 中是 taskID
	ID string `json:"id,omitempty"`

	// 容器ip
	IP string `json:"ip,omitempty"`

	// 包含Running,Killed,Failed,Healthy,UnHealthy等状态
	InstanceStatus string `json:"instanceStatus,omitempty"`

	// 宿主机ip
	Host string `json:"host,omitempty"`

	// 事件额外描述，可能为空
	Message string `json:"message,omitempty"`

	// 时间戳到纳秒级
	Timestamp int64 `json:"timestamp"`
}
