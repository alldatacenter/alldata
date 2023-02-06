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

type IssueStatus struct {
	ProjectID   uint64           `json:"projectID"`
	IssueType   IssueType        `json:"issueType"`
	StateID     int64            `json:"stateID"`
	StateName   string           `json:"stateName"`
	StateBelong IssueStateBelong `json:"stateBelong"`
	Index       int64            `json:"index"`
}

// 事件主状态
type IssueStateBelong string

const (
	IssueStateBelongOpen     IssueStateBelong = "OPEN"     // 待处理
	IssueStateBelongWorking  IssueStateBelong = "WORKING"  // 进行中
	IssueStateBelongDone     IssueStateBelong = "DONE"     // 已完成
	IssueStateBelongWontfix  IssueStateBelong = "WONTFIX"  // 无需修复
	IssueStateBelongReopen   IssueStateBelong = "REOPEN"   // 重新打开
	IssueStateBelongResolved IssueStateBelong = "RESOLVED" // 已解决
	IssueStateBelongClosed   IssueStateBelong = "CLOSED"   // 已关闭
)

type IssueStateRelation struct {
	IssueStatus
	StateRelation []int64 `json:"stateRelation"`
}

type IssueTypeState struct {
	IssueType IssueType `json:"issueType"`
	State     []string  `json:"state"`
}

type IssueTypeStateID struct {
	IssueType IssueType `json:"issueType"`
	State     []string  `json:"state"`
}

type IssueStateName struct {
	Name string `json:"name"`
	ID   int64  `json:"id"`
}
type IssueStateState struct {
	StateBelong IssueStateBelong `json:"stateBelong"`
	States      []IssueStateName
}
type IssueStateTypeBelong struct {
	Type   IssueType       `json:"type"`
	States IssueStateState `json:"states"`
}

type IssueStateTypeBelongResponse struct {
	Header
	Data []IssueStateState `json:"data"`
}

// 项目下工作流查询请求
type IssueStateRelationGetRequest struct {
	ProjectID uint64    `json:"projectID"`
	IssueType IssueType `json:"issueType"`
	IdentityInfo
}

// 删除状态请求
type IssueStateDeleteRequest struct {
	ProjectID int64 `json:"projectID"`
	ID        int64 `json:"id"`
	IdentityInfo
}

// 更新工作流请求
type IssueStateUpdateRequest struct {
	ProjectID int64                `json:"projectID"`
	Data      []IssueStateRelation `json:"data"`
	IdentityInfo
}

// 创建状态请求
type IssueStateCreateRequest struct {
	ProjectID   uint64           `json:"projectID"`
	IssueType   IssueType        `json:"issueType"`
	StateName   string           `json:"stateName"`
	StateBelong IssueStateBelong `json:"stateBelong"`
	IdentityInfo
}

// 获取项目下状态请求
type IssueStatesGetRequest struct {
	ProjectID    uint64             `json:"projectID"`
	IssueType    IssueType          `json:"issueTypes"`
	StateBelongs []IssueStateBelong `json:"stateBelongs"`
	IdentityInfo
}

// 删除状态请求
type IssueStateDeleteResponse struct {
	Header
	Data IssueStatus `json:"data"`
}

// 按项目下任务类型分类的工作流详情
type IssueStateRelationGetResponse struct {
	Header
	Data []IssueStateRelation `json:"data"`
}

// 项目下状态列表
type IssueStatesGetResponse struct {
	Header
	Data []IssueTypeState `json:"data"`
}

// 事件主状态列表
type IssueStateTypeBelongGetResponse struct {
	Header
	Data []IssueStateState `json:"data"`
}

type IssueStateNameGetResponse struct {
	Header
	Data []IssueStatus `json:"data"`
}

type IssueStateCustomRelation struct {
	From int `json:"from"`
	To   int `json:"to"`
}

type StateDefinitionCustomizeData struct {
	IssueType `json:"issueType"`
	States    []IssueStateData           `json:"states"`
	Relations []IssueStateCustomRelation `json:"relations"`
}

type IssueStateData struct {
	Index            int64  `json:"index"`
	Name             string `json:"name"`
	IssueStateBelong `json:"belong"`
}

type IssueStatesRequest struct {
	ProjectID    uint64             `json:"projectID"`
	IssueType    []IssueType        `json:"issueTypes"`
	StateBelongs []IssueStateBelong `json:"stateBelongs"`
}
