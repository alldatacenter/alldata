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

import "time"

// ApprovalStatus 审批流状态
type ApprovalStatus string

// 审批流状态集
const (
	ApprovalStatusPending  ApprovalStatus = "pending"
	ApprovalStatusApproved ApprovalStatus = "approved"
	ApprovalStatusDeined   ApprovalStatus = "denied"
)

// ApproveType 证书类型
type ApproveType string

// 审批流状态集
const (
	ApproveCeritficate       ApproveType = "certificate"
	ApproveLibReference      ApproveType = "lib-reference"
	ApproveUnblockAppication ApproveType = "unblock-application"
)

// ApproveCreateRequest POST /api/approves 创建审批请求结构
type ApproveCreateRequest struct {
	OrgID      uint64            `json:"orgId"`
	TargetID   uint64            `json:"targetId"`   // 审批目标 ID，如 appId
	EntityID   uint64            `json:"entityId"`   // 证书 ID
	TargetName string            `json:"targetName"` // 审批目标名称，如 appName
	Type       ApproveType       `json:"type"`       // 审批类型:certificate/lib-reference/unblock-application
	Extra      map[string]string `json:"extra"`
	Title      string            `json:"title"`
	Priority   string            `json:"priority"`
	Desc       string            `json:"desc"`
}

// ApproveCreateResponse POST /api/approves 创建审批响应结构
type ApproveCreateResponse struct {
	Header
	Data ApproveDTO `json:"data"`
}

// ApproveUpdateRequest PUT /api/approves/{approveId} 更新审批请求结构
type ApproveUpdateRequest struct {
	OrgID    uint64            `json:"orgId"`
	Extra    map[string]string `json:"extra"`
	Priority string            `json:"priority"`
	Desc     string            `json:"desc"`
	Status   ApprovalStatus    `json:"status"`
	Approver string            `json:"approver"`
}

// ApproveUpdateResponse PUT /api/approves/{approveId} 更新审批响应结构
type ApproveUpdateResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ApproveDeleteResponse DELETE /api/approves/{approveId} 取消审批响应结构
type ApproveDeleteResponse struct {
	Header
	Data uint64 `json:"data"`
}

// ApproveDetailResponse GET /api/approves/{approveId} 审批详情响应结构
type ApproveDetailResponse struct {
	Header
	Data ApproveDTO `json:"data"`
}

// ApproveListRequest GET /api/Approve 获取证书列表请求
type ApproveListRequest struct {
	OrgID    uint64   `json:"orgId"`
	Status   []string `query:"status"`
	PageNo   int      `query:"pageNo"`
	PageSize int      `query:"pageSize"`
	ID       *int64   `query:"id"`
}

type ApproveListResponse struct {
	Header
	UserInfoHeader
	Data PagingApproveDTO `json:"data"`
}

// PagingApproveDTO 查询审批列表响应Body
type PagingApproveDTO struct {
	Total int          `json:"total"`
	List  []ApproveDTO `json:"list"`
}

// ApproveDTO 审批信息结构
type ApproveDTO struct {
	ID           uint64            `json:"id"`
	OrgID        uint64            `json:"orgId"`
	EntityID     uint64            `json:"entityId"`
	TargetID     uint64            `json:"targetId"`   // 审批目标 ID，如 appId
	TargetName   string            `json:"targetName"` // 审批目标名称，如 appName
	Type         ApproveType       `json:"type"`       // 审批类型:certificate/lib-reference
	Extra        map[string]string `json:"extra"`
	Title        string            `json:"title"`
	Priority     string            `json:"priority"`
	Desc         string            `json:"desc"`
	Status       ApprovalStatus    `json:"status"`
	Submitter    string            `json:"submitter"`
	Approver     string            `json:"approver"`
	ApprovalTime *time.Time        `json:"approvalTime"` // 审批时间
	CreatedAt    time.Time         `json:"createdAt"`    // 创建时间
	UpdatedAt    time.Time         `json:"updatedAt"`    // 更新时间
}
