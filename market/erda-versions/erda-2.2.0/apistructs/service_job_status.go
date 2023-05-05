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

import "strings"

// StatusCode 是调度器资源对象(service,job)的状态码
type StatusCode string

const (
	// StatusError 底层调度器异常，或者容器异常导致无法拿到 status
	StatusError StatusCode = "Error"
	// StatusCreated 已创建状态
	StatusCreated StatusCode = "Created"
	// StatusStopped 已停止状态
	StatusStopped StatusCode = "Stopped"

	// StatusUnschedulable means that the scheduler can't schedule the job right now.
	// for example:
	// 1. due to resources;
	// 2. due to scheduler inactive (chronos / metronome..)
	// StatusUnschedulable 无法调度状态
	StatusUnschedulable StatusCode = "Unschedulable"
	// StatusNotFoundInCluster 在集群中未找到该job
	StatusNotFoundInCluster StatusCode = "NotFoundInCluster"
	// StatusRunning 正在运行状态
	StatusRunning StatusCode = "Running"
	// StatusStoppedOnOK 已成功退出状态
	StatusStoppedOnOK StatusCode = "StoppedOnOK"
	// StatusStoppedOnFailed 失败退出状态
	StatusStoppedOnFailed StatusCode = "StoppedOnFailed"
	// StatusStoppedByKilled 因被杀而退出状态
	StatusStoppedByKilled StatusCode = "StoppedByKilled"

	// status only for services, TODO: refactor or union with jobs

	// StatusReady 已就绪状态
	StatusReady StatusCode = "Ready"
	// StatusProgressing 正在处理中状态
	StatusProgressing StatusCode = "Progressing"
	// StatusFailing 未成功启动状态
	StatusFailing StatusCode = "Failing"
	// StatusStarting 是实例状态，已运行但未收到健康检查事件，瞬态
	StatusStarting StatusCode = "Starting"
	// StatusHealthy 对实例而言，表示已启动并收到已通过健康检查事件
	// StatusHealthy 对服务而言，表示服务下所有实例均收到通过健康检查事件，且没有Starting状态的实例
	StatusHealthy StatusCode = "Healthy"
	// StatusUnHealthy 对实例而言，表示已启动并收到未通过健康检查事件
	// StatusUnHealthy 对服务而言，表示预期实例数与实际实例数不相等，或者至少一个副本的健康检查未收到或未通过
	StatusUnHealthy StatusCode = "UnHealthy"
	// StatusErrorAndDeleted 表示服务创建过程中出错，系统清理并删除了runtime
	StatusErrorAndDeleted StatusCode = "Error"
	// StatusFinished 已完成状态
	StatusFinished StatusCode = "Finished"
	// StatusFailed 已失败状态
	StatusFailed StatusCode = "Failed"
	// StatusUnknown 未知状态
	StatusUnknown StatusCode = "Unknown"
)

const (
	// LabelMatchTags 表示需要匹配的标签
	LabelMatchTags = "MATCH_TAGS"
	// LabelExcludeTags 标签不去匹配的标签
	LabelExcludeTags = "EXCLUDE_TAGS"
	// LabelPack 标识打包类型
	LabelPack = "PACK"
	// LabelJobKind 标示 job 类型，目前大数据使用
	LabelJobKind = "JOB_KIND"
)

const (
	// TagAny 标识any标签
	TagAny = "any"
	// TagLocked 标识locked标签，不允许新的任务调度上来
	TagLocked = "locked"
	// TagPlatform 标识platform标签，只允许平台组件调度
	TagPlatform = "platform"
	// TagPack 标识pack标签，打包任务
	TagPack = "pack"
	// TagJob 标示job标签
	TagJob = "job"
	// TagServiceStateless 标识service-stateless标签，允许无状态服务调度
	TagServiceStateless = "service-stateless"
	// TagServiceStateful 标识service-stateful标签，允许有状态服务调度
	TagServiceStateful = "service-stateful"
	// TagProjectPrefix 标识project-，项目标签的前缀
	TagProjectPrefix = "project-" // is a prefix, dynamic tag, e.g. project-41
	// TagWorkspacePrefix 标识workspace-，工作区标签的前缀
	TagWorkspacePrefix = "workspace-"
	// TagBigdata 标识bigdata标签
	TagBigdata = "bigdata"
	// TagLocationPrefix location 前缀
	TagLocationPrefix = "location-"
	// TagLocationOnly location 独占
	TagLocationOnly = "locationonly"
)

const (
	// HealthCheckDuration 最小健康检查时间，单位为秒
	HealthCheckDuration int = 420
)

// ResourceInsufficientInfo 描述部署过程中资源不足的信息
type ResourceInsufficientInfo struct {
	Code        string `json:"code,omitempty"`
	Description string `json:"description,omitempty"`
}

// String return the detailed description of insufficient info
func (r *ResourceInsufficientInfo) String() string {
	return r.Description
}

func generateDescription(resourceType string) string {
	switch resourceType {
	case UNFULFILLEDROLE:
		return "无匹配的节点角色"
	case UNFULFILLEDCONSTRAINT:
		return "约束条件不满足"
	case INSUFFICIENTCPUS:
		return "CPU资源不足"
	case INSUFFICIENTMEMORY:
		return "内存资源不足"
	case INSUFFICIENTDISK:
		return "磁盘资源不足"
	case INSUFFICIENTPORTS:
		return "端口资源不足"
	}
	return "unknown resource:" + resourceType
}

// AddResourceInfo 添加资源信息
func (r *ResourceInsufficientInfo) AddResourceInfo(info string) {
	if r.Code == "" {
		r.Code = info
		r.Description = generateDescription(info)
		return
	}
	r.Code = r.Code + "," + info
	r.Description = r.Description + "," + generateDescription(info)
}

// IsRoleUnfulfilled 判断是否节点角色不满足
func (r *ResourceInsufficientInfo) IsRoleUnfulfilled() bool {
	return strings.Contains(r.Code, UNFULFILLEDROLE)
}

// IsConstraintUnfulfilled 判断是否约束条件不满足
func (r *ResourceInsufficientInfo) IsConstraintUnfulfilled() bool {
	return strings.Contains(r.Code, UNFULFILLEDCONSTRAINT)
}

// IsCPUInsufficient 判断是否 CPU 资源不足
func (r *ResourceInsufficientInfo) IsCPUInsufficient() bool {
	return strings.Contains(r.Code, INSUFFICIENTCPUS)
}

// IsMemoryInsufficient 判断是否内存资源不足
func (r *ResourceInsufficientInfo) IsMemoryInsufficient() bool {
	return strings.Contains(r.Code, INSUFFICIENTMEMORY)
}

// IsDiskInsufficient 判断是否磁盘资源不足
func (r *ResourceInsufficientInfo) IsDiskInsufficient() bool {
	return strings.Contains(r.Code, INSUFFICIENTDISK)
}

// IsPortInsufficient 判断是否端口资源不足
func (r *ResourceInsufficientInfo) IsPortInsufficient() bool {
	return strings.Contains(r.Code, INSUFFICIENTPORTS)
}

const (
	// UNFULFILLEDROLE 指节点角色不满足, 如都是 slave_public 的机器
	UNFULFILLEDROLE = "UnfulfilledRole"
	// UNFULFILLEDCONSTRAINT 指约束条件不满足
	UNFULFILLEDCONSTRAINT = "UnfulfilledConstraint"
	// INSUFFICIENTCPUS 指 CPU 资源不足
	INSUFFICIENTCPUS = "InsufficientCpus"
	// INSUFFICIENTMEMORY 指 MEMORY 资源不足
	INSUFFICIENTMEMORY = "InsufficientMemory"
	// INSUFFICIENTDISK 指磁盘资源不足
	INSUFFICIENTDISK = "InsufficientDisk"
	// INSUFFICIENTPORTS 指端口资源不足
	INSUFFICIENTPORTS = "InsufficientPorts"
)

// StatusDesc 封装状态描述
type StatusDesc struct {
	// Status 描述状态
	Status StatusCode `json:"status"`
	// LastMessage 描述状态的额外信息
	LastMessage string `json:"last_message,omitempty"`
	Reason      string `json:"reason"`
	// [DEPRECATED] UnScheduledReasons 描述具体资源不足的信息
	UnScheduledReasons ResourceInsufficientInfo `json:"unScheduledReasons,omitempty"`
}

// Bind 定义宿主机上的路径挂载到容器中
type Bind struct {
	// ContainerPath 指容器路径
	ContainerPath string `json:"containerPath"`
	// HostPath 指宿主机路径
	HostPath string `json:"hostPath"`
	// ReadOnly 是可选的，默认值是 false (read/write)
	ReadOnly bool `json:"readOnly,omitempty"`
}

// MultiLevelStatus 定义多维度状态，如 runtime 状态，runtime下的service 状态，service下的实例状态
type MultiLevelStatus struct {
	// Namespace 指 runtime namespace
	Namespace string `json:"namespace"`
	// Name 指 runtime name
	Name string `json:"name"`
	// Status 指 runtime status
	Status string `json:"status,omitempty"`
	// More 是扩展字段，比如存储runtime下每个服务的名字及状态
	More map[string]string `json:"more,omitempty"`
}
