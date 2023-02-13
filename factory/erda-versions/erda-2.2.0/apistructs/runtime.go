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
)

// Request for API `GET /api/runtimes/{idOrName}`
//
// 两种使用场景:
//  1) idOrName is id: 只需要传 id 即可
//      e.g. GET /api/runtimes/123
//  2) idOrName is name: 此时需要传 applicationId 和 workspace
//      e.g. GET /api/runtimes/test.develop?applicationId=456&workspace=TEST
type RuntimeInspectRequest struct {
	// 应用实例 ID / Name
	IDOrName string `path:"idOrName"`

	// 环境, idOrName 为 Name 时必传, 为 ID 时不必传
	Workspace string `query:"workspace"`

	// 应用 ID, idOrName 为 Name 时必传, 为 ID 时不必传
	ApplicationID uint64 `query:"applicationId"`
}

// Response for API `GET /api/runtimes/{idOrName}`
type RuntimeInspectResponse struct {
	Header
	Data RuntimeInspectDTO `json:"data"`
}

type RuntimeListResponse struct {
	Header
	UserInfoHeader
	Data []RuntimeSummaryDTO `json:"data"`
}

// RuntimeCreateV2Request 创建 Runtime 请求
type RuntimeCreateV2Request struct {
	ApplicationID uint64        `json:"applicationID"`
	Workspace     string        `json:"workspace"`
	Name          string        `json:"name"`
	ClusterName   string        `json:"clusterName"`
	Operator      string        `json:"operator"`
	Source        RuntimeSource `json:"source"`
}

// RuntimeCreateV2Response 创建 Runtime 响应
type RuntimeCreateV2Response struct {
	Header
	Data *RuntimeCreateV2ResponseData `json:"data"`
}

// RuntimeCreateV2Response 创建 Runtime 响应数据
type RuntimeCreateV2ResponseData struct {
	RuntimeID uint64 `json:"runtimeID"`
}

type RuntimeCreateRequest struct {
	Name              string                    `json:"name"`
	ReleaseID         string                    `json:"releaseId"`
	Operator          string                    `json:"operator"`
	ClusterName       string                    `json:"clusterName"`
	Source            RuntimeSource             `json:"source"`
	Extra             RuntimeCreateRequestExtra `json:"extra,omitempty"`
	SkipPushByOrch    bool                      `json:"skipPushByOrch"`
	Param             string                    `json:"param"`
	DeploymentOrderId string                    `json:"deploymentOrderId,omitempty"`
	ReleaseVersion    string                    `json:"releaseVersion,omitempty"`
	ExtraParams       string                    `json:"extraParams,omitempty"`
}

type RuntimeKillPodRequest struct {
	RuntimeID uint64 `json:"runtimeID"`
	PodName   string `json:"podName"`
}
type RuntimeReleaseCreatePipelineResponse struct {
	PipelineID uint64 `json:"pipelineId"`
}

type RuntimeReleaseCreateRequest struct {
	// 制品ID
	ReleaseID string `json:"releaseId"`
	// 环境
	Workspace string `json:"workspace"`
	// 项目ID
	ProjectID uint64 `json:"projectId"`
	// 应用ID
	ApplicationID uint64 `json:"applicationId"`
}

type RuntimeCreateRequestExtra struct {
	OrgID           uint64      `json:"orgId,omitempty"`
	ProjectID       uint64      `json:"projectId,omitempty"`
	ApplicationID   uint64      `json:"applicationId,omitempty"`
	ApplicationName string      `json:"applicationName,omitempty"`
	Workspace       string      `json:"workspace,omitempty"`
	BuildID         uint64      `json:"buildId,omitempty"`
	DeployType      string      `json:"deployType,omitempty"`
	InstanceID      json.Number `json:"instanceId,omitempty"`
	// Deprecated
	ClusterId json.Number `json:"clusterId,omitempty"`
	// for addon actions
	AddonActions map[string]interface{} `json:"actions,omitempty"`
}

type RuntimeReleaseCreateResponse struct {
	Header
	Data RuntimeDeployDTO `json:"data"`
}

type RuntimeCreateResponse struct {
	Header
	Data DeploymentCreateResponseDTO `json:"data"`
}

type RuntimeRedeployResponse struct {
	Header
	Data DeploymentCreateResponseDTO `json:"data"`
}

type RuntimeRollbackRequest struct {
	DeploymentID uint64 `json:"deploymentId"`
}

type RuntimeRollbackResponse struct {
	Header
	Data DeploymentCreateResponseDTO `json:"data"`
}

type RuntimeDeleteResponse struct {
	Header
	Data RuntimeDTO `json:"data"`
}

type RuntimeDeployResponse struct {
	Header
	Data RuntimeDeployDTO `json:"data"`
}

type PageInfo struct {
	// 页码
	PageNO int `query:"pageNo"`

	// 每页大小
	PageSize int `query:"pageSize"`
}

func (p PageInfo) GetOffset() int {
	offset := (p.PageNO - 1) * p.PageSize
	if offset > 0 {
		return offset
	}
	return 0
}

func (p PageInfo) GetLimit() int {
	if p.PageSize > 0 {
		return p.PageSize
	}
	return 0
}
