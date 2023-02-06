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

const (
	// 测试集、用例是否回收
	RecycledYes           bool = true
	RecycledNo            bool = false
	TestCaseDescMaxLength int  = 3000
)

// TestCase 测试用例详情
type TestCase struct {
	ID             uint64                  `json:"id"`
	Name           string                  `json:"name"`           // 用例名称
	Priority       TestCasePriority        `json:"priority"`       // 优先级
	PreCondition   string                  `json:"preCondition"`   // 前置条件
	Desc           string                  `json:"desc"`           // 补充说明
	Recycled       *bool                   `json:"recycled"`       // 是否回收，0：不回收，1：回收
	TestSetID      uint64                  `json:"testSetID"`      // 所属测试集 ID
	ProjectID      uint64                  `json:"projectID"`      // 当前项目id，用于权限校验
	CreatorID      string                  `json:"creatorID"`      // 创建者 ID
	UpdaterID      string                  `json:"updaterID"`      // 更新者 ID
	BugIDs         []uint64                `json:"bugIDs"`         // 关联缺陷 IDs
	LabelIDs       []uint64                `json:"labelIDs"`       // 关联缺陷 IDs
	Attachments    []string                `json:"attachments"`    // 上传附件 uuid 列表,仅供创建时使用
	StepAndResults []TestCaseStepAndResult `json:"stepAndResults"` // 步骤及结果
	Labels         []ProjectLabel          `json:"labels"`         // 标签
	APIs           []*ApiTestInfo          `json:"apis"`           // 接口测试集合
	APICount       TestCaseAPICount        `json:"apiCount"`
	CreatedAt      time.Time               `json:"createdAt"`
	UpdatedAt      time.Time               `json:"updatedAt"`
}

// TestCaseWithSimpleSetInfo testcase with simple set info
type TestCaseWithSimpleSetInfo struct {
	TestCase
	Directory string `json:"directory"`
}

// TestCaseAPICount 用例接口状态个数
type TestCaseAPICount struct {
	Total   uint64 `json:"total"`
	Created uint64 `json:"created"`
	Running uint64 `json:"running"`
	Passed  uint64 `json:"passed"`
	Failed  uint64 `json:"failed"`
}

// TestCaseFrom 测试用例来源
type TestCaseFrom string

// TestCasePriority 测试用例优先级
type TestCasePriority string

// TestCaseStepAndResult 操作步骤信息
type TestCaseStepAndResult struct {
	Step   string `json:"step"`   // 操作步骤
	Result string `json:"result"` // 预期结果
}

var (
	TestCaseFromManual TestCaseFrom = "人工"

	TestCasePriorityP0 TestCasePriority = "P0"
	TestCasePriorityP1 TestCasePriority = "P1"
	TestCasePriorityP2 TestCasePriority = "P2"
	TestCasePriorityP3 TestCasePriority = "P3"
)

func (priority TestCasePriority) IsValid() bool {
	switch priority {
	case TestCasePriorityP0, TestCasePriorityP1, TestCasePriorityP2, TestCasePriorityP3:
		return true
	default:
		return false
	}
}

// TestCaseFileType 用例导出类型
type TestCaseFileType string

var (
	TestCaseFileTypeExcel TestCaseFileType = "excel"
	TestCaseFileTypeXmind TestCaseFileType = "xmind"
)

func (t TestCaseFileType) Valid() bool {
	switch t {
	case TestCaseFileTypeExcel, TestCaseFileTypeXmind:
		return true
	default:
		return false
	}
}

type TestCaseGetResponse struct {
	Header
	Data *TestCase `json:"data,omitempty"`
}

// TestCaseCreateRequest POST 创建测试用例请求
type TestCaseCreateRequest struct {
	ProjectID      uint64                  `json:"projectID"`      // 当前项目 ID，用于权限校验
	TestSetID      uint64                  `json:"testSetID"`      // 所属测试集 ID
	Name           string                  `json:"name"`           // 用例名称
	PreCondition   string                  `json:"preCondition"`   // 前置条件
	StepAndResults []TestCaseStepAndResult `json:"stepAndResults"` // 步骤及结果
	APIs           []*ApiTestInfo          `json:"apis"`           // 接口测试集合
	Desc           string                  `json:"desc"`           // 补充说明
	Priority       TestCasePriority        `json:"priority"`       // 优先级
	LabelIDs       []uint64                `json:"labelIDs"`       // 关联缺陷 IDs

	IdentityInfo
}

// TestCaseCreateResponse POST /api/usecases 创建测试用例响应
type TestCaseCreateResponse struct {
	Header
	Data uint64 `json:"data"`
}

type TestCaseBatchCreateRequest struct {
	ProjectID uint64                  `json:"projectID"` // 当前项目 ID，用于权限校验
	TestCases []TestCaseCreateRequest `json:"testCases"`

	IdentityInfo
}

type TestCaseBatchCreateResponse struct {
	Header
	Data []uint64 `json:"data,omitempty"` // 批量创建出来的 test case id 列表
}

type TestCaseListRequest struct {
	IDs []uint64

	AllowMissingProjectID bool
	ProjectID             uint64

	// AllowEmptyTestSetIDs 是否允许 testSetIDs 为空，默认为 false
	AllowEmptyTestSetIDs bool
	TestSetIDs           []uint64

	Recycled bool
	IDOnly   bool
}
type TestCaseListResponse struct {
	Header
	Data []TestCase `json:"data"`
}

// TestCasePagingRequest 测试用例分页查询
type TestCasePagingRequest struct {
	// 分页参数
	PageNo   int64 `schema:"pageNo"`
	PageSize int64 `schema:"pageSize"`

	// 项目 ID，目前必填，因为测试用例的 testSetID 可以为 0，若无 projectID 只有 testSetID，会查到别的 project
	ProjectID        uint64              `schema:"projectID"`       // 当前项目 ID，用于权限校验
	TestSetID        uint64              `schema:"testSetID"`       // 所属测试集 ID
	NoSubTestSet     bool                `schema:"noSubTestSet"`    // 是否包括子测试集，默认包括
	NotInTestPlanIDs []uint64            `schema:"notInTestPlanID"` // 不在指定的测试计划中
	TestCaseIDs      []uint64            `schema:"testCaseID"`      // 内部使用，全量测试用例列表，最终结果为子集
	NotInTestCaseIDs []uint64            `schema:"-"`               // 内部使用，NotInTestPlanIDs 会转换为 NotInTestCaseIDs 列表
	TestSetCaseMap   map[uint64][]uint64 `schema:"-"`               // 内部使用,测试集和用例关系

	Query      string             `schema:"query"`     // title 过滤
	Priorities []TestCasePriority `schema:"priority"`  // 优先级
	UpdaterIDs []string           `schema:"updaterID"` // 更新人 ID 列表

	// 更新时间，外部传参使用时间戳
	TimestampSecUpdatedAtBegin *time.Duration `schema:"timestampSecUpdatedAtBegin"` // 更新时间左值, 包含区间值
	TimestampSecUpdatedAtEnd   *time.Duration `schema:"timestampSecUpdatedAtEnd"`   // 更新时间右值, 包含区间值
	// 更新时间，内部使用直接赋值
	UpdatedAtBeginInclude *time.Time `schema:"-"`
	UpdatedAtEndInclude   *time.Time `schema:"-"`

	// TODO 用例类型
	Labels []uint64 `schema:"label"` // 标签

	Recycled bool `schema:"recycled"` // 是否回收

	// order by field
	OrderFields            []string `schema:"orderField"` // order by 的字段顺序，影响排序先后过程
	OrderByPriorityAsc     *bool    `schema:"orderByPriorityAsc"`
	OrderByPriorityDesc    *bool    `schema:"orderByPriorityDesc"`
	OrderByUpdaterIDAsc    *bool    `schema:"orderByUpdaterIDAsc"`
	OrderByUpdaterIDDesc   *bool    `schema:"orderByUpdaterIDDesc"`
	OrderByUpdatedAtAsc    *bool    `schema:"orderByUpdatedAtAsc"`
	OrderByUpdatedAtDesc   *bool    `schema:"orderByUpdatedAtDesc"`
	OrderByIDAsc           *bool    `schema:"orderByIDAsc"`
	OrderByIDDesc          *bool    `schema:"orderByIDDesc"`
	OrderByTestSetIDAsc    *bool    `schema:"orderByTestSetIDAsc"`
	OrderByTestSetIDDesc   *bool    `schema:"orderByTestSetIDDesc"`
	OrderByTestSetNameAsc  *bool    `schema:"orderByTestSetNameAsc"`
	OrderByTestSetNameDesc *bool    `schema:"orderByTestSetNameDesc"`

	IdentityInfo
}

type TestCasePagingResponse struct {
	Header
	Data *TestCasePagingResponseData `json:"data"`
}

type TestCasePagingResponseData struct {
	Total    uint64             `json:"total"`
	TestSets []TestSetWithCases `json:"testSets"`
	UserIDs  []string           `json:"userIDs,omitempty"`
}

// TestSetWithCases 测试集且包含测试用例
type TestSetWithCases struct {
	TestSetID uint64     `json:"testSetID"` // 所属测试集 ID
	Recycled  bool       `json:"recycled"`  // 用例集是否回收
	Directory string     `json:"directory"` // 展示用例集路径,拼接/项目名称或者/项目名称/回收站
	TestCases []TestCase `json:"testCases"` // 当前用例集路径下的测试用例集合
}

type TestSetWithPlanCaseRels struct {
	TestSetID uint64            `json:"testSetID"`
	Directory string            `json:"directory"`
	TestCases []TestPlanCaseRel `json:"testCases"`

	// 当前测试集下所有测试用例的个数，不考虑过滤条件；
	// 场景：分页查询，当前页只能显示部分用例，批量删除这部分用例后，前端需要根据这个参数值判断当前测试集下是否还有用例。
	//      若已全部删除，则前端删除目录栏里的当前目录。
	TestCaseCountWithoutFilter uint64 `json:"testCaseCountWithoutFilter"`
}

// TestCaseUpdateRequest 更新测试用例请求
type TestCaseUpdateRequest struct {
	ID             uint64                  `json:"-"`                  // 程序内部赋值使用
	Name           string                  `json:"name"`               // 用例名称
	Priority       TestCasePriority        `json:"priority"`           // 优先级
	PreCondition   string                  `json:"preCondition"`       // 前置条件，即使为空也会被更新
	StepAndResults []TestCaseStepAndResult `json:"stepAndResults"`     // 步骤及结果，即使为空也会被更新
	APIs           []*ApiTestInfo          `json:"apis"`               // 接口测试集合，更新、创建或删除
	Desc           string                  `json:"desc"`               // 补充说明
	LabelIDs       []uint64                `json:"labelIDs,omitempty"` // 标签列表

	IdentityInfo
}

//  TestCaseUpdateResponse 更新测试用例响应
type TestCaseUpdateResponse struct {
	Header
}

// TestCaseBatchUpdateRequest 测试用例批量更新请求
type TestCaseBatchUpdateRequest struct {
	Priority        TestCasePriority `json:"priority"`
	Recycled        *bool            `json:"recycled,omitempty"`
	MoveToTestSetID *uint64          `json:"moveToTestSetID,omitempty"`
	// labelIDs

	TestCaseIDs []uint64 `json:"testCaseIDs"`

	IdentityInfo
}

// TestCaseBatchCleanFromRecycleBinRequest 从回收站彻底删除测试用例
type TestCaseBatchCleanFromRecycleBinRequest struct {
	TestCaseIDs []uint64 `json:"testCaseIDs"`

	IdentityInfo
}
type TestCaseBatchCleanFromRecycleBinResponse struct {
	Header
}

// TestCaseQueryParams 测试用例查询基本参数
type TestCaseQueryParams struct {
	Exclude         []uint64 `query:"exclude"`
	UsecaseIDs      []uint64 `query:"usecaseIds"`
	PageNo          uint64   `query:"pageNo"`
	TestSetID       uint64   `query:"testSetId"`
	ProjectID       uint64   `query:"projectId"`
	SelectProjectID uint64   `query:"selectProjectId"`
	TargetTestSetID uint64   `query:"targetTestSetId"`
	TargetProjectID uint64   `query:"targetProjectId"`
	Recycled        bool     `query:"recycled"`
}

//  TestCaseBatchUpdateResponse PUT /api/usecases/batch 批量更新测试用例响应
type TestCaseBatchUpdateResponse struct {
	Header
	Data bool `json:"data"`
}

type TestCaseBatchCopyRequest struct {
	CopyToTestSetID uint64 `json:"copyToTestSetID"`

	ProjectID   uint64   `json:"projectID"`
	TestCaseIDs []uint64 `json:"testCaseIDs"`

	IdentityInfo
}

type TestCaseBatchCopyResponse struct {
	Header
	Data []uint64 `json:"data,omitempty"`
}

// BasicTestCase 测试用例Basic DTO
type BasicTestCase struct {
	Id            uint64     `json:"id"`
	TestSetId     uint64     `json:"testSetId"`     // 所属测试集id
	ProjectId     uint64     `json:"projectId"`     // 当前项目id
	UpdatedId     uint64     `json:"updatedID"`     // 更新者id
	CreatorId     uint64     `json:"creatorID"`     // 创建者id
	Recycled      bool       `json:"recycled"`      // 是否回收，0：不回收，1：回收
	Desc          string     `json:"desc"`          // 注释
	Name          string     `json:"name"`          // 用例名称
	From          string     `json:"from"`          // 来源
	Priority      string     `json:"priority"`      // 优先级
	PreCondition  string     `json:"preCondition"`  // 前置条件
	TagIds        string     `json:"tagIds"`        // 目标id列表
	Result        string     `json:"result"`        // 测试执行结果
	StepAndResult string     `json:"stepAndResult"` // 步骤及结果
	CreatedAt     *time.Time `json:"createdAt"`     // 创建时间
	UpdatedAt     *time.Time `json:"updatedAt"`     // 更新时间
}

// TestCaseListDaoData 测试用例List  数据库Data
type TestCaseListDaoData struct {
	BasicTestCase
	Directory string `json:"directory"` // 展示用例集路径,拼接/项目名称或者/项目名称/回收站
}

// TestCaseExportRequest 用例导出请求
type TestCaseExportRequest struct {
	TestCasePagingRequest

	FileType TestCaseFileType `schema:"fileType"`

	Locale string `schema:"-"`
}
type TestCaseExportResponse struct {
	Header
	Data uint64 `json:"data"`
}

// TestCaseImportRequest 用例从 Excel 导入请求
type TestCaseImportRequest struct {
	TestSetID uint64           `schema:"testSetID"`
	ProjectID uint64           `schema:"projectID"`
	FileType  TestCaseFileType `schema:"fileType"`

	IdentityInfo
}
type TestCaseImportResponse struct {
	Header
	Data *TestCaseImportResult `json:"data"`
}
type TestCaseImportResult struct {
	SuccessCount uint64 `json:"successCount"`
	Id           uint64 `json:"id"`
}

// TestCaseExcel 测试用例 Excel
type TestCaseExcel struct {
	Title          string                  `title:"用例名称"`
	DirectoryName  string                  `title:"测试集"`
	PriorityName   string                  `title:"优先级"`
	PreCondition   string                  `title:"前置条件"`
	StepAndResults []TestCaseStepAndResult `title:"步骤与结果" group:"StepAndResults"`
	ApiInfos       []APIInfo               `title:"接口测试" group:"ApiInfos"`
}

// TestCaseXmind 测试用例 Xmind
type TestCaseXmind struct {
	TestCaseExcel
}
