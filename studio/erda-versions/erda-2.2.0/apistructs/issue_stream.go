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

// IssueStream 事件流返回数据结构
type IssueStream struct {
	ID         int64           `json:"id"`
	IssueID    int64           `json:"issueID"`
	Operator   string          `json:"operator"`
	StreamType IssueStreamType `json:"streamType"`
	Content    string          `json:"content"` // 事件流展示内容
	MRInfo     MRCommentInfo   `json:"mrInfo"`
	CreatedAt  time.Time       `json:"createdAt"`
	UpdatedAt  time.Time       `json:"updatedAt"`
}

// IssueStreamType issue 事件流类型
type IssueStreamType string

// issue 事件流类型
const (
	ISTCreate                        IssueStreamType = "Create" // 创建事件
	ISTComment                       IssueStreamType = "Comment"
	ISTRelateMR                      IssueStreamType = "RelateMR" // 关联 MR
	ISTAssign                        IssueStreamType = "Assign"
	ISTTransferState                 IssueStreamType = "TransferState" // 状态迁移
	ISTChangeTitle                   IssueStreamType = "ChangeTitle"
	ISTChangePlanStartedAt           IssueStreamType = "ChangePlanStartedAt"           // 更新计划开始时间
	ISTChangePlanFinishedAt          IssueStreamType = "ChangePlanFinishedAt"          // 更新计划结束时间
	ISTChangeAssignee                IssueStreamType = "ChangeAssignee"                // 更新处理人
	ISTChangeIteration               IssueStreamType = "ChangeIteration"               // 更新迭代
	ISTChangeIterationFromUnassigned IssueStreamType = "ChangeIterationFromUnassigned" // change iteration from unassigned iteration
	ISTChangeIterationToUnassigned   IssueStreamType = "ChangeIterationToUnassigned"   // change iteration to unassigned iteration
	ISTChangeManHour                 IssueStreamType = "ChangeManHour"                 // 更新工时信息
	ISTChangeOwner                   IssueStreamType = "ChangeOwner"                   // 更新责任人
	ISTChangeTaskType                IssueStreamType = "ChangeTaskType"                // 更新任务类型/引用源
	ISTChangeBugStage                IssueStreamType = "ChangeBugStage"                // 更新引用源
	ISTChangePriority                IssueStreamType = "ChangePriority"                // 更新优先级
	ISTChangeComplexity              IssueStreamType = "ChangeComplexity"              // 更新复杂度
	ISTChangeSeverity                IssueStreamType = "ChangeSeverity"                // 更新严重度
	ISTChangeContent                 IssueStreamType = "ChangeContent"                 // 更新内容
	ISTChangeLabel                   IssueStreamType = "ChangeLabel"                   // 更新标签
)

// GetEventAction get event action by issue stream type
func (ist IssueStreamType) GetEventAction() string {
	switch ist {
	case ISTCreate:
		return "create"
	default:
		return "update"
	}
}

// IssueStreamCreateRequest 事件流创建请求
type IssueStreamCreateRequest struct {
	IssueID      int64             `json:"issueID"`
	Operator     string            `json:"operator"`
	StreamType   IssueStreamType   `json:"streamType"`
	StreamParams ISTParam          `json:"streamParams"`
	StreamTypes  []IssueStreamType `json:"streamTypes"`

	// internal use, get from *http.Request
	IdentityInfo
}

// IssueStreamPagingRequest 事件流列表请求
type IssueStreamPagingRequest struct {
	IssueID  uint64 `json:"issueID"`
	PageNo   uint64 `json:"pageNo"`
	PageSize uint64 `json:"pageSize"`
}

// IssueStreamPagingResponse 事件流列表响应
type IssueStreamPagingResponse struct {
	Header
	UserInfoHeader
	Data IssueStreamPagingResponseData `json:"data"`
}

// IssueStreamPagingResponseData 事件流列表响应数据
type IssueStreamPagingResponseData struct {
	Total int64         `json:"total"`
	List  []IssueStream `json:"list"`
}

// CommentIssueStreamCreateRequest 评论创建请求
type CommentIssueStreamCreateRequest struct {
	IssueID int64           `json:"-"`
	Type    IssueStreamType `json:"type"`
	Content string          `json:"content"`
	MRInfo  MRCommentInfo   `json:"mrInfo"`

	// internal use, get from *http.Request
	IdentityInfo
}

// MRCommentInfo MR 评论内容
type MRCommentInfo struct {
	AppID   int64  `json:"appID"`
	MRID    int64  `json:"mrID"` // 应用内唯一
	MRTitle string `json:"mrTitle"`
}

// IssueCommentTestCaseInfo Issue 评论：关联测试用例
type IssueCommentTestCaseInfo struct {
	TestCaseID   uint64 `json:"testCaseID"`
	TestCaseName string `json:"testCaseName"`
}

const SystemOperator = "system"
