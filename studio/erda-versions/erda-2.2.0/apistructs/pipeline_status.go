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

import "fmt"

// PipelineStatus 表示流水线或任务状态
type PipelineStatus string

// PipelineStatusDesc action status and simple description
// desc is an optional filed that describes the reason for the failure and should be empty on success
type PipelineStatusDesc struct {
	Status PipelineStatus `json:"status"`
	Desc   string         `json:"desc"`
}

func (status PipelineStatus) String() string {
	return string(status)
}

func (desc PipelineStatusDesc) String() string {
	if desc.Desc == "" {
		return fmt.Sprintf("status: %s", string(desc.Status))
	}
	return fmt.Sprintf("status: %s, desc: %s", string(desc.Status), desc.Desc)
}

const (
	PipelineEmptyStatus PipelineStatus = "" // 判断状态是否为空

	// 构建相关的状态
	PipelineStatusInitializing  PipelineStatus = "Initializing"  // 初始化中：存在时间一般来说极短，表示 build 刚创建并正在分析中
	PipelineStatusDisabled      PipelineStatus = "Disabled"      // 禁用状态：表示该节点被禁用
	PipelineStatusAnalyzeFailed PipelineStatus = "AnalyzeFailed" // 分析失败：分析结束但是结果失败
	PipelineStatusAnalyzed      PipelineStatus = "Analyzed"      // 分析完毕：build 创建完即开始分析，分析成功则为该状态

	// 流程推进相关的状态
	PipelineStatusBorn    PipelineStatus = "Born"    // 流程推进过程中的初始状态
	PipelineStatusPaused  PipelineStatus = "Paused"  // 暂停状态：表示流程需要暂停，和 Born 同级，不会被 Mark
	PipelineStatusMark    PipelineStatus = "Mark"    // 标记状态：表示流程开始处理
	PipelineStatusCreated PipelineStatus = "Created" // 创建成功：scheduler create + start；可能要区分 Created 和 Started 两个状态
	PipelineStatusQueue   PipelineStatus = "Queue"   // 排队中：介于 启动成功 和 运行中
	PipelineStatusRunning PipelineStatus = "Running" // 运行中
	PipelineStatusSuccess PipelineStatus = "Success" // 成功

	// 流程推进 "正常" 失败：一般是用户侧导致的失败
	PipelineStatusFailed         PipelineStatus = "Failed"         // 业务逻辑执行失败，"正常" 失败
	PipelineStatusTimeout        PipelineStatus = "Timeout"        // 超时
	PipelineStatusStopByUser     PipelineStatus = "StopByUser"     // 用户主动取消
	PipelineStatusNoNeedBySystem PipelineStatus = "NoNeedBySystem" // 无需执行：系统判定无需执行

	// 流程推进 "异常" 失败：一般是平台侧导致的失败
	PipelineStatusCreateError    PipelineStatus = "CreateError"    // 创建节点失败
	PipelineStatusStartError     PipelineStatus = "StartError"     // 开始节点失败
	PipelineStatusError          PipelineStatus = "Error"          // 异常
	PipelineStatusDBError        PipelineStatus = "DBError"        // 平台流程推进时操作数据库异常
	PipelineStatusUnknown        PipelineStatus = "Unknown"        // 未知状态：获取到了无法识别的状态，流程无法推进
	PipelineStatusLostConn       PipelineStatus = "LostConn"       // 在重试指定次数后仍然无法连接
	PipelineStatusCancelByRemote PipelineStatus = "CancelByRemote" // 远端取消

	// 人工审核相关
	PipelineStatusWaitApproval    PipelineStatus = "WaitApprove" // 等待人工审核
	PipelineStatusApprovalSuccess PipelineStatus = "Accept"      // 人工审核通过
	PipelineStatusApprovalFail    PipelineStatus = "Reject"      // 人工审核拒绝
)

var PipelineEndStatuses = []PipelineStatus{
	PipelineStatusSuccess, PipelineStatusAnalyzeFailed, PipelineStatusFailed, PipelineStatusTimeout,
	PipelineStatusStopByUser, PipelineStatusNoNeedBySystem, PipelineStatusCreateError, PipelineStatusStartError, PipelineStatusDBError,
	PipelineStatusError, PipelineStatusUnknown, PipelineStatusLostConn, PipelineStatusCancelByRemote,
}

var PipelineAllStatuses = []PipelineStatus{
	PipelineStatusAnalyzed, PipelineStatusBorn, PipelineStatusCreated, PipelineStatusMark, PipelineStatusQueue, PipelineStatusRunning, PipelineStatusSuccess,
	PipelineStatusFailed, PipelineStatusAnalyzeFailed, PipelineStatusPaused, PipelineStatusCreateError, PipelineStatusStartError, PipelineStatusTimeout,
	PipelineStatusStopByUser, PipelineStatusNoNeedBySystem, PipelineStatusCancelByRemote, PipelineStatusInitializing, PipelineStatusError, PipelineStatusUnknown, PipelineStatusDBError, PipelineStatusLostConn,
	PipelineStatusDisabled, PipelineStatusWaitApproval, PipelineStatusApprovalSuccess, PipelineStatusApprovalFail,
}

func (status PipelineStatus) ToDesc() string {
	switch status {
	case PipelineStatusAnalyzed, PipelineStatusBorn:
		return "初始化成功"
	case PipelineStatusCreated:
		return "创建成功"
	case PipelineStatusMark, PipelineStatusQueue:
		return "排队中"
	case PipelineStatusRunning:
		return "正在执行"
	case PipelineStatusSuccess:
		return "执行成功"
	case PipelineStatusFailed:
		return "执行失败"
	case PipelineStatusAnalyzeFailed:
		return "初始化失败"
	case PipelineStatusPaused:
		return "暂停"
	case PipelineStatusCreateError:
		return "创建失败"
	case PipelineStatusStartError:
		return "启动失败"
	case PipelineStatusTimeout:
		return "超时"
	case PipelineStatusStopByUser, PipelineStatusNoNeedBySystem, PipelineStatusCancelByRemote:
		return "取消"
	case PipelineStatusInitializing:
		return "正在初始化"
	case PipelineStatusError, PipelineStatusUnknown, PipelineStatusDBError, PipelineStatusLostConn:
		return "异常"
	case PipelineStatusDisabled:
		return "禁用"
	case PipelineStatusWaitApproval:
		return "等待审核"
	case PipelineStatusApprovalSuccess:
		return "人工审核通过"
	case PipelineStatusApprovalFail:
		return "人工审核拒绝"
	default:
		return ""
	}
}

func ReconcilerRunningStatuses() []PipelineStatus {
	return []PipelineStatus{PipelineStatusBorn, PipelineStatusPaused, PipelineStatusMark,
		PipelineStatusCreated, PipelineStatusQueue, PipelineStatusRunning}
}

// CanPauseTask 只有在 Born 状态下可以 暂停
func (status PipelineStatus) CanPauseTask() bool {
	return status == PipelineStatusBorn
}

// CanUnPauseTask 只有在 暂停 状态下可以 取消暂停
func (status PipelineStatus) CanUnPauseTask() bool {
	return status == PipelineStatusPaused
}

func (status PipelineStatus) IsReconcilerRunningStatus() bool {
	switch status {
	case PipelineStatusBorn, PipelineStatusPaused, PipelineStatusMark,
		PipelineStatusCreated, PipelineStatusQueue, PipelineStatusRunning:
		return true
	default:
		return false
	}
}

func (status PipelineStatus) IsBeforePressRunButton() bool {
	switch status {
	case PipelineStatusInitializing, PipelineStatusAnalyzed, PipelineStatusAnalyzeFailed:
		return true
	default:
		return false
	}
}

func (status PipelineStatus) CanCancel() bool {
	if status == PipelineStatusCreated || status == PipelineStatusQueue || status == PipelineStatusRunning {
		return true
	}
	return false
}

func (status PipelineStatus) CanEnableDisable() bool {
	switch status {
	case PipelineStatusInitializing, PipelineStatusAnalyzed:
		return true
	default:
		return false
	}
}

func (status PipelineStatus) CanPause() bool {
	switch status {
	case PipelineStatusInitializing, PipelineStatusAnalyzed, PipelineStatusPaused:
		return true
	default:
		return false
	}
}

func (status PipelineStatus) CanUnpause() bool {
	return status == PipelineStatusPaused
}

func (status PipelineStatus) IsEmpty() bool {
	return status == PipelineEmptyStatus
}

func (status PipelineStatus) IsEndStatus() bool {
	return status.IsSuccessStatus() || status.IsFailedStatus()
}

func (status PipelineStatus) IsSuccessStatus() bool {
	return status == PipelineStatusSuccess
}

func (status PipelineStatus) IsRunningStatus() bool {
	return status == PipelineStatusRunning
}

func (status PipelineStatus) InQueue() bool {
	return status == PipelineStatusQueue
}

func (status PipelineStatus) CanDelete() bool {
	// 未开始可删除
	if status == PipelineStatusAnalyzed {
		return true
	}
	// 终态可删除
	if status.IsEndStatus() {
		return true
	}
	return false
}

// IsNormalFailedStatus 表示正常失败，一般由用户侧引起
func (status PipelineStatus) IsNormalFailedStatus() bool {
	switch status {
	// "正常" 失败
	case PipelineStatusAnalyzeFailed, PipelineStatusFailed, PipelineStatusTimeout,
		PipelineStatusStopByUser, PipelineStatusNoNeedBySystem:
		return true
	default:
		return false
	}
}

// IsAbnormalFailedStatus 表示异常失败，一般由平台侧引起
func (status PipelineStatus) IsAbnormalFailedStatus() bool {
	switch status {
	// "异常" 失败
	case PipelineStatusCreateError, PipelineStatusStartError, PipelineStatusDBError,
		PipelineStatusError, PipelineStatusUnknown, PipelineStatusLostConn, PipelineStatusCancelByRemote:
		return true
	default:
		return false
	}
}

func (status PipelineStatus) IsDisabledStatus() bool {
	return status == PipelineStatusDisabled
}

func (status PipelineStatus) IsStopByUser() bool {
	return status == PipelineStatusStopByUser
}

func (status PipelineStatus) IsNoNeedBySystem() bool {
	return status == PipelineStatusNoNeedBySystem
}

func (status PipelineStatus) IsFailedStatus() bool {
	return status.IsNormalFailedStatus() || status.IsAbnormalFailedStatus()
}

func (status PipelineStatus) ChangeStateForManualReview() PipelineStatus {
	if status.IsSuccessStatus() {
		return PipelineStatusApprovalSuccess
	}
	if status == PipelineStatusFailed {
		return PipelineStatusFailed
	}
	if status == PipelineStatusRunning {
		return PipelineStatusWaitApproval
	}
	return status
}

func (status PipelineStatus) AfterPipelineQueue() bool {
	return status == PipelineStatusRunning || status.IsEndStatus()
}

func (status PipelineStatus) IsShouldSkipLoop() bool {
	return !status.IsEndStatus() || status.IsStopByUser()
}
