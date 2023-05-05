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
	"time"
)

type RuntimeInspectDTO struct {
	ID uint64 `json:"id"`
	// runtime名称
	Name                  string        `json:"name"`
	ServiceGroupName      string        `json:"serviceGroupName"`
	ServiceGroupNamespace string        `json:"serviceGroupNamespace"`
	Source                RuntimeSource `json:"source"`
	// 状态
	Status       string                               `json:"status"`
	DeployStatus DeploymentStatus                     `json:"deployStatus"`
	DeleteStatus string                               `json:"deleteStatus"`
	ReleaseID    string                               `json:"releaseId"`
	ClusterID    uint64                               `json:"clusterId"`
	ClusterName  string                               `json:"clusterName"`
	ClusterType  string                               `json:"clusterType"`
	Resources    RuntimeServiceResourceDTO            `json:"resources"`
	Extra        map[string]interface{}               `json:"extra"` // TODO: move fields out of extra
	ProjectID    uint64                               `json:"projectID"`
	Services     map[string]*RuntimeInspectServiceDTO `json:"services"`
	// 模块发布错误信息
	ModuleErrMsg        map[string]map[string]string `json:"lastMessage"`
	TimeCreated         time.Time                    `json:"timeCreated"` // Deprecated: use CreatedAt instead
	CreatedAt           time.Time                    `json:"createdAt"`
	UpdatedAt           time.Time                    `json:"updatedAt"`
	DeployAt            time.Time                    `json:"deployAt"`
	Errors              []ErrorResponse              `json:"errors"`
	Creator             string                       `json:"creator"`
	ApplicationID       uint64                       `json:"applicationId"`
	ApplicationName     string                       `json:"applicationName"`
	DeploymentOrderId   string                       `json:"deploymentOrderId"`
	DeploymentOrderName string                       `json:"deploymentOrderName"`
	ReleaseVersion      string                       `json:"releaseVersion"`
	RawStatus           string                       `json:"rawStatus"`
	RawDeploymentStatus string                       `json:"rawDeploymentStatus"`
}

type RuntimeInspectServiceDTO struct {
	Status      string                       `json:"status"`
	Type        string                       `json:"type"`
	Deployments RuntimeServiceDeploymentsDTO `json:"deployments"`
	Resources   RuntimeServiceResourceDTO    `json:"resources"`
	Envs        map[string]string            `json:"envs"`
	Addrs       []string                     `json:"addrs"` // TODO: better name?
	Expose      []string                     `json:"expose"`
	Errors      []ErrorResponse              `json:"errors"`
}

type RuntimeSummaryDTO struct {
	RuntimeInspectDTO
	LastOperator       string    `json:"lastOperator"`
	LastOperatorName   string    `json:"lastOperatorName"`   // Deprecated
	LastOperatorAvatar string    `json:"lastOperatorAvatar"` // Deprecated
	LastOperateTime    time.Time `json:"lastOperateTime"`
	LastOperatorId     uint64    `json:"lastOperatorId"`
}

type RuntimeDTO struct {
	ID              uint64          `json:"id"`
	Name            string          `json:"name"`
	GitBranch       string          `json:"gitBranch"` // Deprecated: use name instead
	Workspace       string          `json:"workspace"`
	ClusterName     string          `json:"clusterName"`
	ClusterId       uint64          `json:"clusterId"` // Deprecated: use ClusterName instead
	Status          string          `json:"status"`
	ApplicationID   uint64          `json:"applicationId"`
	ApplicationName string          `json:"applicationName"`
	ProjectID       uint64          `json:"projectId"`
	ProjectName     string          `json:"projectName"`
	OrgID           uint64          `json:"orgId"`
	Errors          []ErrorResponse `json:"errors"`
}

// TODO: currently same as RuntimeInspectServiceDTO, we should combine these two
type RuntimeServiceDTO struct {
	ID          uint64                       `json:"id"`
	RuntimeID   uint64                       `json:"runtimeId"`
	ServiceName string                       `json:"serviceName"`
	Status      string                       `json:"status"`
	Deployments RuntimeServiceDeploymentsDTO `json:"deployments"`
	Resources   RuntimeServiceResourceDTO    `json:"resources"`
	Envs        map[string]string            `json:"envs"`
	Expose      []string                     `json:"expose"`
	Errors      []ErrorResponse              `json:"errors"`
}

type RuntimeServiceDeploymentsDTO struct {
	Replicas int `json:"replicas"`
}

type RuntimeServiceResourceDTO struct {
	CPU  float64 `json:"cpu"`
	Mem  int     `json:"mem"`
	Disk int     `json:"disk"`
}

type DeploymentStatusDTO struct {
	// 发布Id
	DeploymentID uint64 `json:"deploymentId"`
	// 状态
	Status DeploymentStatus `json:"status"`
	// 发布过程
	Phase DeploymentPhase `json:"phase"`
	// 失败原因
	FailCause string `json:"failCause"`
	// 模块错误信息
	ModuleErrMsg map[string]string           `json:"lastMessage"`
	Runtime      *DeploymentStatusRuntimeDTO `json:"runtime"`
}

// Deprecated: use RuntimeInspect api to get ServiceGroup Info
type DeploymentStatusRuntimeDTO struct {
	Services  map[string]*DeploymentStatusRuntimeServiceDTO `json:"services"`
	Endpoints map[string]*DeploymentStatusRuntimeServiceDTO `json:"endpoints"`
}

// Deprecated: use RuntimeInspect api to get ServiceGroup Info
type DeploymentStatusRuntimeServiceDTO struct {
	PublicHosts []string `json:"publicHosts"`
	Host        string   `json:"host"`
	Ports       []int    `json:"ports"`
}

type PreDiceDTO struct {
	Name     string                               `json:"name,omitempty"`
	Envs     map[string]string                    `json:"envs,omitempty"`
	Services map[string]*RuntimeInspectServiceDTO `json:"services,omitempty"`
}

type DeploymentCreateResponseDTO struct {
	DeploymentID  uint64 `json:"deploymentId"`
	ApplicationID uint64 `json:"applicationId"`
	RuntimeID     uint64 `json:"runtimeId"`
}

type DeploymentCreateResponsePipelineDTO struct {
	PipelineID uint64 `json:"pipelineId"`
}

type RuntimeDeployDTO struct {
	PipelineID      uint64   `json:"pipelineId"`
	Workspace       string   `json:"workspace"`
	ClusterName     string   `json:"clusterName"`
	ApplicationID   uint64   `json:"applicationId"`
	ApplicationName string   `json:"applicationName"`
	ProjectID       uint64   `json:"projectId"`
	ProjectName     string   `json:"projectName"`
	OrgID           uint64   `json:"orgId"`
	OrgName         string   `json:"orgName"`
	ServicesNames   []string `json:"servicesNames"`
}

type RuntimeScaleRecords struct {
	// Runtimes 不为空则无需设置 IDs, 二者必选其一
	Runtimes []RuntimeScaleRecord `json:"runtimeRecords,omitempty"`
	// IDs 不为空则无需设置 Runtimes, 二者必选其一
	IDs []uint64 `json:"ids,omitempty"`
}

type RuntimeScaleRecord struct {
	ApplicationId uint64     `json:"applicationId"`
	Workspace     string     `json:"workspace"`
	Name          string     `json:"name"`
	RuntimeID     uint64     `json:"runtimeId,omitempty"`
	PayLoad       PreDiceDTO `json:"payLoad,omitempty"`
	ErrMsg        string     `json:"errorMsg,omitempty"`
}

type BatchRuntimeScaleResults struct {
	Total           int                  `json:"total"`
	Successed       int                  `json:"successed"`
	Faild           int                  `json:"failed"`
	SuccessedScales []PreDiceDTO         `json:"successedRuntimeScales,omitempty"`
	SuccessedIds    []uint64             `json:"successedIds,omitempty"`
	FailedScales    []RuntimeScaleRecord `json:"FailedRuntimeScales,omitempty"`
	FailedIds       []uint64             `json:"FailedIds,omitempty"`
}

type BatchRuntimeDeleteResults struct {
	Total        int          `json:"total"`
	Success      int          `json:"success"`
	Failed       int          `json:"failed"`
	Deleted      []RuntimeDTO `json:"deleted,omitempty"`
	DeletedIds   []uint64     `json:"deletedIds,omitempty"`
	UnDeleted    []RuntimeDTO `json:"deletedFailed,omitempty"`
	UnDeletedIds []uint64     `json:"deletedFailedIds,omitempty"`
	ErrMsg       []string     `json:"errorMsgs,omitempty,omitempty"`
}

type BatchRuntimeReDeployResults struct {
	Total           int                `json:"total"`
	Success         int                `json:"success"`
	Failed          int                `json:"failed"`
	ReDeployed      []RuntimeDeployDTO `json:"reDeployed,omitempty"`
	ReDeployedIds   []uint64           `json:"reDeployedIds,omitempty"`
	UnReDeployed    []RuntimeDTO       `json:"reDeployedFailed,omitempty"`
	UnReDeployedIds []uint64           `json:"reDeployedFailedIds,omitempty"`
	ErrMsg          []string           `json:"errorMsgs,omitempty"`
}

// AddonScaleRecords 表示 Addon 的 scale 请求群信息
type AddonScaleRecords struct {
	// Addons 不为空则无需设置 AddonRoutingIDs, 二者必选其一
	// 格式: map[{addon instance's routing ID}]AddonScaleRecord
	Addons map[string]AddonScaleRecord `json:"addonScaleRecords,omitempty"`
	// AddonRoutingIDs is the list of addon instance's routing ID
	AddonRoutingIDs []string `json:"ids,omitempty"`
}

// AddonScaleRecord is the addon
type AddonScaleRecord struct {
	AddonName                   string                                      `json:"addonName,omitempty"`
	ServiceResourcesAndReplicas map[string]AddonServiceResourcesAndReplicas `json:"services,omitempty"`
}

// AddonServiceResourcesAndReplicas set the desired resources and replicas for addon services
type AddonServiceResourcesAndReplicas struct {
	Resources Resources `json:"resources,omitempty"`
	Replicas  int32     `json:"replicas,omitempty"`
}

type AddonScaleResults struct {
	Total     int `json:"total"`
	Successed int `json:"successed"`
	Faild     int `json:"failed"`
	// FailedInfo   map[{addon routingID}]errMsg
	FailedInfo map[string]string `json:"errors,omitempty"`
}
