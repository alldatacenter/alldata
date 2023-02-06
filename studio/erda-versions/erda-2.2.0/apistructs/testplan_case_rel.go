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

// TestPlanCaseRel
type TestPlanCaseRel struct {
	ID         uint64             `json:"id"`
	Name       string             `json:"name"`
	Priority   TestCasePriority   `json:"priority"`
	TestPlanID uint64             `json:"testPlanID"`
	TestSetID  uint64             `json:"testSetID"`
	TestCaseID uint64             `json:"testCaseID"`
	ExecStatus TestCaseExecStatus `json:"execStatus"`
	CreatorID  string             `json:"creatorID"`
	UpdaterID  string             `json:"updaterID"`
	ExecutorID string             `json:"executorID"`
	CreatedAt  time.Time          `json:"createdAt"`
	UpdatedAt  time.Time          `json:"updatedAt"`
	APICount   TestCaseAPICount   `json:"apiCount"`

	IssueBugs []TestPlanCaseRelIssueBug `json:"issueBugs"`
}

type TestPlanCaseRelIssueBug struct {
	IssueRelationID uint64           `json:"issueRelationID"`
	IssueID         uint64           `json:"issueID"`
	IterationID     int64            `json:"iterationID"`
	Title           string           `json:"title"`
	State           IssueState       `json:"state"`
	StateBelong     IssueStateBelong `json:"stateBelong"`
	Priority        IssuePriority    `json:"priority"`
	CreatedAt       time.Time        `json:"createdAt"`
}

// TestCaseExecStatus 测试用例执行状态
type TestCaseExecStatus string

const (
	CaseExecStatusInit    TestCaseExecStatus = "INIT"   // 未执行
	CaseExecStatusSucc    TestCaseExecStatus = "PASSED" // 已通过
	CaseExecStatusFail    TestCaseExecStatus = "FAIL"   // 未通过
	CaseExecStatusBlocked TestCaseExecStatus = "BLOCK"  // 阻塞
)

// TestPlanCaseRelCreateRequest 测试计划用例关系创建请求
type TestPlanCaseRelCreateRequest struct {
	TestPlanID  uint64   `json:"testPlanID"`
	TestCaseIDs []uint64 `json:"testCaseIDs"`
	// 若 TestSetIDs 不为空，则添加测试集下所有测试用例到测试集下，与 TestCaseIDs 取合集
	TestSetIDs []uint64 `json:"testSetIDs"`

	IdentityInfo
}
type TestPlanCaseRelCreateResponse struct {
	Header
	Data *TestPlanCaseRelCreateResult `json:"data,omitempty"`
}
type TestPlanCaseRelCreateResult struct {
	TotalCount uint64 `json:"totalCount"`
}

type TestPlanCaseRelGetRequest struct {
	RelationID uint64 `json:"relationID"`
}
type TestPlanCaseRelGetResponse struct {
	Header
	Data *TestPlanCaseRel `json:"data"`
}

// TestPlanCaseRelBatchUpdateRequest 测试计划用例关系更新请求
type TestPlanCaseRelBatchUpdateRequest struct {
	Delete     bool               `json:"delete"`
	ExecutorID string             `json:"executorID"`
	ExecStatus TestCaseExecStatus `json:"execStatus"`

	TestPlanID  uint64   `json:"-"`
	TestSetID   *uint64  `json:"testSetID"` // 批量递归操作测试集下的所有关联；与 relationIDs 的关系为 并集
	RelationIDs []uint64 `json:"relationIDs"`

	ProjectID uint64 `json:"-"`

	IdentityInfo
}

// TestPlanTestCaseRelDeleteRequest 测试计划用例关系删除请求
type TestPlanTestCaseRelDeleteRequest struct {
	// +required
	ProjectID uint64 `json:"projectId"`
	// +required
	TestPlanID  uint64   `json:"testPlanId"`
	UsecaseIDs  []uint64 `json:"usecaseIds"`
	ExcludeIDs  []uint64 `json:"excludeIds"`
	AllSelected bool     `json:"allSelected"` // 为 true 时，usecaseIDs 为空

	IdentityInfo
}

// TestPlanCaseRelListRequest 测试计划用户关系列表请求
type TestPlanCaseRelListRequest struct {
	IDs                   []uint64             `schema:"id"`
	TestPlanIDs           []uint64             `schema:"testPlanID"`
	TestSetIDs            []uint64             `schema:"testSetID"`
	CreatorIDs            []string             `schema:"creatorID"`
	UpdaterIDs            []string             `schema:"updaterID"`
	ExecutorIDs           []string             `schema:"executorID"`
	ExecStatuses          []TestCaseExecStatus `schema:"execStatus"`
	UpdatedAtBeginInclude *time.Time
	UpdatedAtEndInclude   *time.Time
	IDOnly                bool

	IdentityInfo
}

// TestPlanCaseRelListResponse 测试计划测试用例关系响应
type TestPlanCaseRelListResponse struct {
	Header
	Data []TestPlanCaseRel `json:"data"`
}

// TestPlanCaseRelExportRequest 测试计划用例导出请求
type TestPlanCaseRelExportRequest struct {
	TestPlanCaseRelPagingRequest

	Locale   string           `schema:"-"`
	FileType TestCaseFileType `schema:"fileType"`
}
type TestPlanCaseRelExportResponse struct {
	Header
	Data int64 `json:"data"`
}

// TestPlanReportGenerateResponse 测试计划报告生成响应
type TestPlanReportGenerateResponse struct {
	Header
	Data *TestPlanReport `json:"data"`
}
type TestPlanReport struct {
	TestPlan       TestPlan                     `json:"testPlan"`
	RelsCount      TestPlanRelsCount            `json:"relsCount"`
	APICount       TestCaseAPICount             `json:"apiCount"`
	ExecutorStatus map[string]TestPlanRelsCount `json:"executorStatus"`

	UserIDs []string `json:"userIDs"`
}

// TestPlanCaseRelIssueRelationRemoveRequest 解除测试计划用例与缺陷关联关系请求
type TestPlanCaseRelIssueRelationRemoveRequest struct {
	IssueTestCaseRelationIDs []uint64 `json:"issueTestCaseRelationIDs"`

	TestPlanID        uint64 `json:"-"`
	TestPlanCaseRelID uint64 `json:"-"`

	IdentityInfo
}
type TestPlanCaseRelIssueRelationRemoveResponse struct {
	Header
}

// TestPlanCaseRelIssueRelationAddRequest 新增测试计划用例与缺陷关联关系请求
type TestPlanCaseRelIssueRelationAddRequest struct {
	IssueIDs []uint64 `json:"issueIDs"`

	TestPlanID        uint64 `json:"-"`
	TestPlanCaseRelID uint64 `json:"-"`

	IdentityInfo
}
type TestPlanCaseRelIssueRelationAddResponse struct {
	Header
}
