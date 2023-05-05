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

type RuntimeSource string

const (
	PIPELINE     RuntimeSource = "PIPELINE"
	ABILITY      RuntimeSource = "ABILITY"
	RUNTIMEADDON RuntimeSource = "RUNTIMEADDON"
	RELEASE      RuntimeSource = "RELEASE"
)

const (
	RuntimeStatusHealthy     = "Healthy"
	RuntimeStatusUnHealthy   = "UnHealthy"
	RuntimeStatusProgressing = "Progressing"
	RuntimeStatusInit        = "Init"
	RuntimeStatusUnknown     = "Unknown" // It should be not exist
	RuntimeStatusStopped     = "Stopped" // 已停止未删除副本数全为0

	ServiceStatusHealthy   = "Healthy"   // 运行中，预期实例数与实际实例数相等，且都通过健康检查
	ServiceStatusUnHealthy = "UnHealthy" // 预期实例数与实际实例数不相等，或者至少一个副本的健康检查未收到或未通过
	ServiceStatusUnknown   = "Unknown"

	RuntimeEventTypeTotal = "total"
)

// Flow:
//
// WAITAPPROVE(optional) -> I/W -> DEPLOYING -> OK
//                           |          `---> FAILED
//                           |          `---> CANCELING -> CANCELED
//                           |                       `---> FAILED
//                           `-> CANCELED
type DeploymentStatus string

const (
	DeploymentStatusWaitApprove DeploymentStatus = "WAITAPPROVE"
	DeploymentStatusInit        DeploymentStatus = "INIT"
	DeploymentStatusWaiting     DeploymentStatus = "WAITING"
	DeploymentStatusDeploying   DeploymentStatus = "DEPLOYING"
	DeploymentStatusOK          DeploymentStatus = "OK"
	DeploymentStatusFailed      DeploymentStatus = "FAILED"
	DeploymentStatusCanceling   DeploymentStatus = "CANCELING"
	DeploymentStatusCanceled    DeploymentStatus = "CANCELED"
)

type DeploymentPhase string

const (
	DeploymentPhaseInit      DeploymentPhase = "INIT"
	DeploymentPhaseAddon     DeploymentPhase = "ADDON_REQUESTING"
	DeploymentPhaseScript    DeploymentPhase = "SCRIPT_APPLYING"
	DeploymentPhaseService   DeploymentPhase = "SERVICE_DEPLOYING"
	DeploymentPhaseRegister  DeploymentPhase = "DISCOVERY_REGISTER"
	DeploymentPhaseCompleted DeploymentPhase = "COMPLETED"
)
