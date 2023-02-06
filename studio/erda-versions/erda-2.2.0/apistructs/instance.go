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

// 实例状态合集
const (
	InstanceStatusStarting  string = "Starting" // 已启动，但未收到健康检查事件，瞬态
	InstanceStatusRunning   string = "Running"
	InstanceStatusHealthy   string = "Healthy"
	InstanceStatusUnHealthy string = "UnHealthy" // 已启动但收到未通过健康检查事件

	InstanceStatusDead     string = "Dead"     // TODO Finished 等下面状态后续可以去除，暂未兼容保留
	InstanceStatusFinished string = "Finished" // 已完成，退出码为0
	InstanceStatusFailed   string = "Failed"   // 已退出，退出码非0
	InstanceStatusKilled   string = "Killed"   // 已被杀
	InstanceStatusStopped  string = "Stopped"  // 已停止，Scheduler与DCOS断连期间事件丢失，后续补偿时，需将Healthy置为Stopped
	InstanceStatusUnknown  string = "Unknown"
	InstanceStatusOOM      string = "OOM"
)
