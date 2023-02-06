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
	"bytes"
	"encoding/json"
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/providers/i18n"
)

type Issue struct {
	ID               int64              `json:"id"`
	CreatedAt        time.Time          `json:"createdAt"`
	UpdatedAt        time.Time          `json:"updatedAt"`
	PlanStartedAt    *time.Time         `json:"planStartedAt"`
	PlanFinishedAt   *time.Time         `json:"planFinishedAt"`
	ProjectID        uint64             `json:"projectID"`
	IterationID      int64              `json:"iterationID"`
	AppID            *uint64            `json:"appID"`
	RequirementID    *int64             `json:"requirementID"` // 即将废弃
	RequirementTitle string             `json:"requirementTitle"`
	Type             IssueType          `json:"type"`
	Title            string             `json:"title"`
	Content          string             `json:"content"`
	State            int64              `json:"state"`
	Priority         IssuePriority      `json:"priority"`
	Complexity       IssueComplexity    `json:"complexity"`
	Severity         IssueSeverity      `json:"severity"`
	Creator          string             `json:"creator"`
	Assignee         string             `json:"assignee"`
	IssueButton      []IssueStateButton `json:"issueButton"` // 状态流转按钮
	IssueSummary     *IssueSummary      `json:"issueSummary"`
	Labels           []string           `json:"labels"` // label 列表
	LabelDetails     []ProjectLabel     `json:"labelDetails,omitempty"`
	ManHour          IssueManHour       `json:"issueManHour"`
	Source           string             `json:"source"`
	TaskType         string             `json:"taskType"` // 任务类型
	BugStage         string             `json:"bugStage"` // BUG阶段
	Owner            string             `json:"owner"`    // 责任人
	Subscribers      []string           `json:"subscribers"`

	// 切换到已完成状态的时间 （等事件可以记录历史信息了 删除该字段）
	FinishTime *time.Time `json:"finishTime"`

	TestPlanCaseRels []TestPlanCaseRel `json:"testPlanCaseRels"`
	relatedIssueIDs  []uint64
	ReopenCount      int
}

// GetStage 获取任务状态或者Bug阶段
func (s *Issue) GetStage() string {
	var stage string
	if s.Type == IssueTypeTask {
		stage = s.TaskType
	} else if s.Type == IssueTypeBug {
		stage = s.BugStage
	}
	return stage
}

func (s Issue) ConvertToIssueUpdateReq() IssueUpdateRequest {
	var req IssueUpdateRequest
	cont, _ := json.Marshal(s)
	_ = json.Unmarshal(cont, &req)
	return req
}

// SetRelatedIssueIDs set RelatedIssueIDs from excel
func (s *Issue) SetRelatedIssueIDs(ids string) error {
	if ids == "" {
		return nil
	}
	idStrs := strings.Split(ids, ",")
	dp := map[uint64]bool{}
	relatedIssueIDs := make([]uint64, 0)
	for _, id := range idStrs {
		issueID, err := strconv.Atoi(id)
		if err != nil {
			return err
		}
		if dp[uint64(issueID)] {
			continue
		}
		dp[uint64(issueID)] = true
		relatedIssueIDs = append(relatedIssueIDs, uint64(issueID))
	}
	s.relatedIssueIDs = relatedIssueIDs
	return nil
}

func (s *Issue) GetRelatedIssueIDs() []uint64 {
	return s.relatedIssueIDs
}

// IssueType 事件类型
type IssueType string

var IssueTypes = []IssueType{IssueTypeRequirement, IssueTypeTask, IssueTypeBug, IssueTypeTicket, IssueTypeEpic}

const (
	IssueTypeRequirement IssueType = "REQUIREMENT" // 需求
	IssueTypeTask        IssueType = "TASK"        // 任务
	IssueTypeBug         IssueType = "BUG"         // 缺陷
	IssueTypeTicket      IssueType = "TICKET"      // 工单
	IssueTypeEpic        IssueType = "EPIC"        // 里程碑
)

func (t IssueType) String() string {
	return string(t)
}

func (t IssueType) GetZhName() string {
	switch t {
	case IssueTypeRequirement:
		return "需求"
	case IssueTypeTask:
		return "任务"
	case IssueTypeBug:
		return "缺陷"
	case IssueTypeTicket:
		return "工单"
	case IssueTypeEpic:
		return "里程碑"
	default:
		panic(fmt.Sprintf("invalid issue type: %s", string(t)))
	}
}

func (t IssueType) GetEnName(s string) IssueType {
	switch s {
	case "需求":
		return IssueTypeRequirement
	case "任务":
		return IssueTypeTask
	case "缺陷":
		return IssueTypeBug
	case "工单":
		return IssueTypeTicket
	case "史诗":
		return IssueTypeEpic
	default:
		return ""
	}
}

func (t IssueType) GetCorrespondingResource() string {
	switch t {
	case IssueTypeRequirement:
		return IssueRequirementResource
	case IssueTypeTask:
		return IssueTaskResource
	case IssueTypeBug:
		return IssueBugResource
	case IssueTypeTicket:
		return IssueTicketResource
	case IssueTypeEpic:
		return IssueEpicResource
	default:
		panic(fmt.Sprintf("invalid issue type: %s", string(t)))
	}
}

type IssueState string // 事件状态

const (
	IssueStateOpen    IssueState = "OPEN"    // 待处理
	IssueStateWorking IssueState = "WORKING" // 进行中
	IssueStateTesting IssueState = "TESTING" // 测试中
	IssueStateDone    IssueState = "DONE"    // 已完成 (requirement/task 唯一终态)

	IssueStateResolved IssueState = "RESOLVED" // 已解决
	IssueStateReopen   IssueState = "REOPEN"   // 重新打开
	IssueStateWontfix  IssueState = "WONTFIX"  // 拒绝修复
	IssueStateDup      IssueState = "DUP"      // 重复提交
	IssueStateClosed   IssueState = "CLOSED"   // 已关闭 (bug 唯一终态)
)

var issueStateTranslate = map[IssueState]map[string]string{
	IssueStateOpen: {
		"en": "Open",
		"zh": "待处理",
	},
	IssueStateWorking: {
		"en": "Working",
		"zh": "进行中",
	},
	IssueStateTesting: {
		"en": "Testing",
		"zh": "测试中",
	},
	IssueStateDone: {
		"en": "Done",
		"zh": "已完成",
	},
	IssueStateResolved: {
		"en": "Resolved",
		"zh": "已解决",
	},
	IssueStateReopen: {
		"en": "Reopen",
		"zh": "重新打开",
	},
	IssueStateWontfix: {
		"en": "WontFix",
		"zh": "无需修复",
	},
	IssueStateDup: {
		"en": "Duplicated",
		"zh": "重复提交",
	},
	IssueStateClosed: {
		"en": "Closed",
		"zh": "已关闭",
	},
}

func (state IssueState) Desc(locale string) string {
	return issueStateTranslate[state][locale]
}

var (
	IssueRequirementStates = []IssueState{IssueStateOpen, IssueStateWorking, IssueStateTesting,
		IssueStateDone} // DONE 为唯一终态
	IssueTaskStates = []IssueState{IssueStateOpen, IssueStateWorking,
		IssueStateDone} // DONE 为唯一终态
	IssueBugStates = []IssueState{IssueStateOpen, IssueStateResolved, IssueStateReopen, IssueStateWontfix, IssueStateDup,
		IssueStateClosed} // CLOSED 为唯一终态
	IssueTicketStates = []IssueState{IssueStateOpen, IssueStateResolved, IssueStateReopen, IssueStateWontfix, IssueStateDup,
		IssueStateClosed} // CLOSED 为唯一终态
	IssueEpicStates = []IssueState{IssueStateOpen, IssueStateWorking,
		IssueStateDone} // DONE 为唯一终态
)

// ValidState 校验新状态是否合法
func (s *Issue) ValidState(newState string) bool {
	switch s.Type {
	case IssueTypeRequirement:
		return IssueState(newState).ValidRequirementState()
	case IssueTypeTask:
		return IssueState(newState).ValidTaskState()
	case IssueTypeBug:
		return IssueState(newState).ValidBugState()
	case IssueTypeTicket:
		// 工单和bug的状态流程一样
		return IssueState(newState).ValidTicketState()
	case IssueTypeEpic:
		// 史诗和task的状态流程一样
		return IssueState(newState).ValidEpicState()
	default:
		return false
	}
}

// GetPermResForUpdate 获取 状态 用于权限校验的资源
func (state IssueState) GetPermResForUpdate() string {
	return fmt.Sprintf("update-state-to-%s", string(state))
}

func (state IssueState) ValidRequirementState() bool {
	return state.validIssueState(IssueTypeRequirement)
}
func (state IssueState) ValidTaskState() bool {
	return state.validIssueState(IssueTypeTask)
}
func (state IssueState) ValidBugState() bool {
	return state.validIssueState(IssueTypeBug)
}
func (state IssueState) ValidTicketState() bool {
	return state.validIssueState(IssueTypeTicket)
}
func (state IssueState) ValidEpicState() bool {
	return state.validIssueState(IssueTypeEpic)
}
func (state IssueState) validIssueState(issueType IssueType) bool {
	var validStates []IssueState
	switch issueType {
	case IssueTypeRequirement:
		validStates = IssueRequirementStates
	case IssueTypeTask:
		validStates = IssueTaskStates
	case IssueTypeBug:
		validStates = IssueBugStates
	case IssueTypeTicket:
		validStates = IssueTicketStates
	case IssueTypeEpic:
		validStates = IssueEpicStates
	default:
		return false
	}
	for _, s := range validStates {
		if state == s {
			return true
		}
	}
	return false
}

// IssuePriority 事件优先级
type IssuePriority string

const (
	IssuePriorityUrgent IssuePriority = "URGENT" // 紧急
	IssuePriorityHigh   IssuePriority = "HIGH"   // 高
	IssuePriorityNormal IssuePriority = "NORMAL" // 中
	IssuePriorityLow    IssuePriority = "LOW"    // 低
)

var IssuePriorityList = []IssuePriority{IssuePriorityUrgent, IssuePriorityHigh, IssuePriorityNormal, IssuePriorityLow}

func (i IssuePriority) GetEnName(zh string) IssuePriority {
	switch zh {
	case "紧急":
		return IssuePriorityUrgent
	case "高":
		return IssuePriorityHigh
	case "中":
		return IssuePriorityNormal
	case "低":
		return IssuePriorityLow
	default:
		return IssuePriorityLow
	}
}

func (i IssuePriority) GetZhName() string {
	switch i {
	case IssuePriorityUrgent:
		return "紧急"
	case IssuePriorityHigh:
		return "高"
	case IssuePriorityNormal:
		return "中"
	case IssuePriorityLow:
		return "低"
	default:
		return string(i)
	}
}

func IssueStreamPriorityName(priority, locale string) string {
	if len(priority) == 0 {
		return ""
	}
	c := IssuePriorityUrgent.GetEnName(priority)
	if strings.Contains(locale, "en") {
		return strings.ToLower(string(c))
	}
	return c.GetZhName()
}

// IssueComplexity 事件复杂度
type IssueComplexity string

const (
	IssueComplexityHard   IssueComplexity = "HARD"   // 复杂
	IssueComplexityNormal IssueComplexity = "NORMAL" // 中
	IssueComplexityEasy   IssueComplexity = "EASY"   // 容易
)

func (is IssueComplexity) GetEnName(zh string) IssueComplexity {
	switch zh {
	case "复杂":
		return IssueComplexityHard
	case "中":
		return IssueComplexityNormal
	case "容易":
		return IssueComplexityEasy
	default:
		return ""
	}
}
func (is IssueComplexity) GetZhName() string {
	switch is {
	case IssueComplexityHard:
		return "复杂"
	case IssueComplexityNormal:
		return "中"
	case IssueComplexityEasy:
		return "容易"
	default:
		panic(fmt.Sprintf("invalid issue complexity: %s", is))
	}
}

func IssueStreamComplexityName(complexity, locale string) string {
	if len(complexity) == 0 {
		return ""
	}
	c := IssueComplexityHard.GetEnName(complexity)
	if strings.Contains(locale, "en") {
		return strings.ToLower(string(c))
	}
	return c.GetZhName()
}

// IssueSeverity 事件严重程度
type IssueSeverity string

const (
	IssueSeverityFatal   IssueSeverity = "FATAL"   // 致命
	IssueSeveritySerious IssueSeverity = "SERIOUS" // 严重
	IssueSeverityNormal  IssueSeverity = "NORMAL"  // 一般
	IssueSeveritySlight  IssueSeverity = "SLIGHT"  // 轻微
	IssueSeverityLow     IssueSeverity = "SUGGEST" // 建议
)

func (is IssueSeverity) GetEnName(zh string) IssueSeverity {
	switch zh {
	case "致命":
		return IssueSeverityFatal
	case "严重":
		return IssueSeveritySerious
	case "一般":
		return IssueSeverityNormal
	case "轻微":
		return IssueSeveritySlight
	case "建议":
		return IssueSeverityLow
	default:
		return IssueSeverityLow
	}
}

func (is IssueSeverity) GetZhName() string {
	switch is {
	case IssueSeverityFatal:
		return "致命"
	case IssueSeveritySerious:
		return "严重"
	case IssueSeverityNormal:
		return "一般"
	case IssueSeveritySlight:
		return "轻微"
	case IssueSeverityLow:
		return "建议"
	default:
		return string(is)
	}
}

func (is IssueSeverity) GetI18nKeyAlias() string {
	if is == IssueSeverityNormal {
		return "ordinary"
	}
	return strings.ToLower(string(is))
}

func IssueStreamSeverityName(severity, locale string) string {
	if len(severity) == 0 {
		return ""
	}
	c := IssueSeverityFatal.GetEnName(severity)
	if strings.Contains(locale, "en") {
		return strings.ToLower(string(c))
	}
	return c.GetZhName()
}

var IssueSeveritys = []IssueSeverity{IssueSeverityFatal, IssueSeveritySerious, IssueSeverityNormal, IssueSeveritySlight, IssueSeverityLow}

// IssueButton 状态流转按钮
type IssueButton struct {
	CanOpen     bool `json:"canOpen"`     // op: 未开始
	CanWorking  bool `json:"canWorking"`  // op: 开始
	CanTesting  bool `json:"canTesting"`  // op: 测试中
	CanDone     bool `json:"canDone"`     // op: 完成
	CanResolved bool `json:"canResolved"` // op: 已修复
	CanReopen   bool `json:"canReOpen"`   // op: 重新打开
	CanWontfix  bool `json:"canWontfix"`  // op: 不修复
	CanDup      bool `json:"canDup"`      // op: 不修复，重复提交
	CanClosed   bool `json:"canClosed"`   // op: 关闭
}

type IssueStateButton struct {
	StateID     int64            `json:"stateID"`
	StateName   string           `json:"stateName"`
	StateBelong IssueStateBelong `json:"stateBelong"`
	Permission  bool             `json:"permission"`
}

// IssueManHour 工时信息，task和bug有该信息。单位统一是分钟
type IssueManHour struct {
	EstimateTime            int64  `json:"estimateTime"`            // 预估工时
	ThisElapsedTime         int64  `json:"thisElapsedTime"`         // 本次已用工时
	ElapsedTime             int64  `json:"elapsedTime"`             // 已用工时
	RemainingTime           int64  `json:"remainingTime"`           // 剩余工时
	StartTime               string `json:"startTime"`               // 这次录入工时的工作开始时间
	WorkContent             string `json:"workContent"`             // 工作内容
	IsModifiedRemainingTime bool   `json:"isModifiedRemainingTime"` // 剩余时间是否被修改过的标记
}

// Convert2String .....
func (imh *IssueManHour) Convert2String() string {
	mh, _ := json.Marshal(*imh)

	return string(mh)
}

var estimateRegexp, _ = regexp.Compile("^[0-9]+[wdhm]+$")

func NewManhour(manhour string) (IssueManHour, error) {
	if manhour == "" {
		return IssueManHour{}, nil
	}
	if !estimateRegexp.MatchString(manhour) {
		return IssueManHour{}, fmt.Errorf("invalid estimate time: %s", manhour)
	}
	timeType := manhour[len(manhour)-1]
	timeSet := manhour[:len(manhour)-1]
	timeVal, err := strconv.ParseUint(timeSet, 10, 64)
	if err != nil {
		return IssueManHour{}, fmt.Errorf("invalid man hour: %s, err: %v", manhour, err)
	}
	switch timeType {
	case 'm':
		val := int64(timeVal)
		return IssueManHour{EstimateTime: val, RemainingTime: val}, nil
	case 'h':
		val := int64(timeVal) * 60
		return IssueManHour{EstimateTime: val, RemainingTime: val}, nil
	case 'd':
		val := int64(timeVal) * 60 * 8
		return IssueManHour{EstimateTime: val, RemainingTime: val}, nil
	case 'w':
		val := int64(timeVal) * 60 * 8 * 5
		return IssueManHour{EstimateTime: val, RemainingTime: val}, nil
	default:
		return IssueManHour{}, fmt.Errorf("invalid man hour: %s", manhour)
	}
}

// Clean get,list事件返回时 开始时间，工作内容都需要为空
func (imh *IssueManHour) Clean() IssueManHour {
	imh.ThisElapsedTime = 0
	imh.StartTime = ""
	imh.WorkContent = ""
	return *imh
}

const (
	Minute int64 = 1
	Hour   int64 = 60 * Minute
	Day    int64 = 8 * Hour
	Week   int64 = 5 * Day
)

// GetFormartTime 时间修改时生成的活动记录需要将分钟带上单位
func (imh *IssueManHour) GetFormartTime(key string) string {
	var result bytes.Buffer
	var minutes int64
	switch key {
	case "EstimateTime":
		minutes = imh.EstimateTime
	case "ElapsedTime":
		minutes = imh.ElapsedTime
	case "RemainingTime":
		minutes = imh.RemainingTime
	}

	formartTime(minutes, &result)
	return result.String()
}

func formartTime(minutes int64, result *bytes.Buffer) {
	if minutes > 0 && minutes < Hour {
		result.WriteString(fmt.Sprintf("%vm", minutes))
		return
	}
	if minutes >= Hour && minutes < Day {
		result.WriteString(fmt.Sprintf("%dh", minutes/Hour))
		formartTime(minutes%Hour, result)
	}
	if minutes >= Day && minutes < Week {
		result.WriteString(fmt.Sprintf("%vd", minutes/Day))
		formartTime(minutes%Day, result)
	}
	if minutes >= Week {
		result.WriteString(fmt.Sprintf("%dw", minutes/Week))
		formartTime(minutes%Week, result)
	}
}

// IssueCreateRequest 事件创建请求
type IssueCreateRequest struct {
	// +optional 计划开始时间
	PlanStartedAt IssueTime `json:"planStartedAt"`
	// +optional 计划结束时间
	PlanFinishedAt IssueTime `json:"planFinishedAt"`
	// +required 所属项目 ID
	ProjectID uint64 `json:"projectID"`
	// +required 所属迭代 ID
	IterationID int64 `json:"iterationID"`
	// +optional 所属应用 ID
	AppID *uint64 `json:"appID"`
	// +optional 关联的测试计划用例关联 ID 列表
	TestPlanCaseRelIDs []uint64 `json:"testPlanCaseRelIDs"`
	// +required issue 类型
	Type IssueType `json:"type"`
	// +required 标题
	Title string `json:"title"`
	// +optional 内容
	Content string `json:"content"`
	// +optional 优先级
	Priority IssuePriority `json:"priority"`
	// +optional 复杂度
	Complexity IssueComplexity `json:"complexity"`
	// +optional 严重程度
	Severity IssueSeverity `json:"severity"`
	// +required 当前处理人
	Assignee string `json:"assignee"`
	// +optional 第三方创建时头里带不了userid，用这个参数显式指定一下
	Creator string `json:"creator"`
	// +optional 标签名称列表
	Labels []string `json:"labels"`
	// +optional 创建来源，目前只有工单使用了该字段
	Source string `json:"source"`
	// +optional 工时信息，当事件类型为任务和缺陷时生效
	ManHour *IssueManHour `json:"issueManHour"`
	// +optionaln 任务类型
	TaskType string `json:"taskType"`
	// +optionaln bug阶段
	BugStage string `json:"bugStage"`
	// +optionaln 负责人
	Owner string `json:"owner"`
	// +optional issue subscribers
	Subscribers []string `json:"subscribers"`
	// internal use, get from *http.Request
	IdentityInfo
	// 用来区分是通过ui还是bundle创建的
	External bool `json:"-"`
}

// GetStage 获取任务状态或者Bug阶段
func (icr *IssueCreateRequest) GetStage() string {
	var stage string
	if icr.Type == IssueTypeTask {
		stage = icr.TaskType
	} else if icr.Type == IssueTypeBug {
		stage = icr.BugStage
	}
	return stage
}

// GetDBManHour 获取工时信息
func (icr *IssueCreateRequest) GetDBManHour() string {
	if icr.ManHour == nil {
		return ""
	}

	return icr.ManHour.Convert2String()
}

// IssueCreateResponse 事件创建响应
type IssueCreateResponse struct {
	Header
	Data uint64 `json:"data"` // issue id
}

// IssuePagingRequest 事件分页查询请求
type IssuePagingRequest struct {
	// +optional default 1
	PageNo uint64 `json:"pageNo"`
	// +optional default 10
	PageSize uint64 `json:"pageSize"`
	// +required 企业id
	OrgID int64 `json:"orgID"`
	// +optional
	ProjectIDs []uint64 `json:"projectIDs"`
	IssueListRequest
}

// GetUserIDs 分页查询时，第一页就需要返回全量的 userInfo 给前端
func (ipr *IssuePagingRequest) GetUserIDs() []string {
	var userIDs []string
	userIDs = append(userIDs, ipr.Assignees...)
	userIDs = append(userIDs, ipr.Creators...)
	userIDs = append(userIDs, ipr.Owner...)
	return userIDs
}

// IssueExportExcelRequest 事件导出 excel 请求
type IssueExportExcelRequest struct {
	IssuePagingRequest
	Locale     string `json:"locale"`
	IsDownload bool   `json:"isDownload"`
}

// IssueImportExcelRequest 事件导入excel请求
type IssueImportExcelRequest struct {
	ProjectID uint64            `json:"projectID"`
	OrgID     int64             `json:"orgID"`
	Type      PropertyIssueType `json:"type"`
	FileID    string            `json:"fileID"`
	IdentityInfo
}

// IssueListRequest 事件列表查询请求
type IssueListRequest struct {
	// +optional
	Title string `schema:"title" json:"title"`
	// +optional
	Type []IssueType `json:"type"`
	// +required
	ProjectID uint64 `schema:"projectID" json:"projectID"`
	// +required 迭代id为-1时，即是显示待办事件
	IterationID int64 `schema:"iterationID" json:"iterationID"`
	// +required 支持多迭代查询
	IterationIDs []int64 `schema:"iterationIDs" json:"iterationIDs"`
	// +optional
	AppID *uint64 `schema:"appID" json:"appID"`
	// +optional
	RequirementID *int64 `schema:"requirementID" json:"requirementID"`
	// +optional
	State []int64 `schema:"state" json:"state"`
	// +optional
	StateBelongs []IssueStateBelong `schema:"stateBelongs" json:"stateBelongs"`
	// +optional
	Creators []string `schema:"creator" json:"creator"`
	// +optional
	Assignees []string `schema:"assignee" json:"assignee"`
	// +optional
	Label []uint64 `schema:"label" json:"label"`
	// +optional ms
	StartCreatedAt int64 `schema:"startCreatedAt" json:"startCreatedAt"`
	// +optional ms
	EndCreatedAt int64 `schema:"endCreatedAt" json:"endCreatedAt"`
	// +optional ms
	StartFinishedAt int64 `schema:"startFinishedAt" json:"startFinishedAt"`
	// +optional ms
	EndFinishedAt int64 `schema:"endFinishedAt" json:"endFinishedAt"`
	// +optional 是否只筛选截止日期为空的事项
	IsEmptyPlanFinishedAt bool `schema:"isEmptyPlanFinishedAt" json:"isEmptyPlanFinishedAt"`
	// +optional ms
	StartClosedAt int64 `schema:"startClosedAt" json:"startClosedAt"`
	// +optional ms
	EndClosedAt int64 `schema:"endClosedAt" json:"endClosedAt"`
	// +optional 优先级
	Priority []IssuePriority `schema:"priority" json:"priority"`
	// +optional 复杂度
	Complexity []IssueComplexity `schema:"complexity" json:"complexity"`
	// +optional 严重程度
	Severity []IssueSeverity `json:"severity" json:"severity"`
	// +optional
	RelatedIssueIDs []uint64 `schema:"relatedIssueId" json:"relatedIssueId"`
	// +optional 来源
	Source string `schema:"source" json:"source"`
	// +optional 排序字段, 支持 planStartedAt & planFinishedAt
	OrderBy string `schema:"orderBy" json:"orderBy"`
	// +optionaln 任务类型
	TaskType []string `json:"taskType"`
	// +optionaln bug阶段
	BugStage []string `json:"bugStage"`
	// +optionaln 负责人
	Owner []string `json:"owner"`
	// +optional 是否需要进度统计
	WithProcessSummary bool `schema:"withProcessSummary"`
	// +optional 排除的id
	ExceptIDs []int64 `json:"exceptIDs"`
	// +optional 是否升序排列
	Asc bool `schema:"asc" json:"asc"`

	// +optional 包含的ID
	IDs []int64 `json:"IDs"`
	// internal use, get from *http.Request
	IdentityInfo
	// 用来区分是通过ui还是bundle创建的
	External bool `json:"external"`
	// Optional custom panel id for issues
	CustomPanelID int64 `json:"customPanelID"`

	OnlyIDResult bool `json:"onlyIdResult"`
	// issues not included by others
	NotIncluded bool `json:"notIncluded"`
}

func (ipr *IssuePagingRequest) UrlQueryString() map[string][]string {
	query := make(map[string][]string)
	query["pageNo"] = []string{strconv.FormatInt(int64(ipr.PageNo), 10)}
	query["pageSize"] = []string{strconv.FormatInt(int64(ipr.PageSize), 10)}
	if ipr.Title != "" {
		query["title"] = []string{ipr.Title}
	}
	for _, v := range ipr.Type {
		query["type"] = append(query["type"], string(v))
	}
	if ipr.ProjectID != 0 {
		query["projectID"] = []string{strconv.FormatInt(int64(ipr.ProjectID), 10)}
	}
	if ipr.IterationID > 0 {
		query["iterationID"] = []string{strconv.FormatInt(ipr.IterationID, 10)}
	}
	for _, v := range ipr.IterationIDs {
		query["iterationIDs"] = append(query["iterationIDs"], strconv.FormatInt(v, 10))
	}
	if ipr.AppID != nil {
		query["appID"] = []string{strconv.FormatInt(int64(*ipr.AppID), 10)}
	}
	if ipr.RequirementID != nil {
		query["requirementID"] = []string{strconv.FormatInt(*ipr.RequirementID, 10)}
	}
	for _, v := range ipr.State {
		query["state"] = append(query["state"], strconv.FormatInt(v, 10))
	}
	for _, v := range ipr.Creators {
		query["creator"] = append(query["creator"], v)
	}
	for _, v := range ipr.Assignees {
		query["assignee"] = append(query["assignee"], v)
	}
	for _, v := range ipr.Label {
		query["label"] = append(query["label"], strconv.FormatInt(int64(v), 10))
	}
	if ipr.StartCreatedAt > 0 {
		query["startCreatedAt"] = append(query["startCreatedAt"], strconv.FormatInt(ipr.StartCreatedAt, 10))
	}
	if ipr.EndCreatedAt > 0 {
		query["endCreatedAt"] = append(query["endCreatedAt"], strconv.FormatInt(ipr.EndCreatedAt, 10))
	}
	if ipr.StartFinishedAt > 0 {
		query["startFinishedAt"] = append(query["startFinishedAt"], strconv.FormatInt(ipr.StartFinishedAt, 10))
	}
	if ipr.EndFinishedAt > 0 {
		query["endFinishedAt"] = append(query["endFinishedAt"], strconv.FormatInt(ipr.EndFinishedAt, 10))
	}
	if ipr.IsEmptyPlanFinishedAt == true {
		query["isEmptyPlanFinishedAt"] = append(query["isEmptyPlanFinishedAt"], "true")
	}
	if ipr.StartClosedAt > 0 {
		query["startClosedAt"] = append(query["startClosedAt"], strconv.FormatInt(ipr.StartClosedAt, 10))
	}
	if ipr.EndClosedAt > 0 {
		query["endClosedAt"] = append(query["endClosedAt"], strconv.FormatInt(ipr.EndClosedAt, 10))
	}
	for _, v := range ipr.Priority {
		query["priority"] = append(query["priority"], string(v))
	}
	for _, v := range ipr.Complexity {
		query["complexity"] = append(query["complexity"], string(v))
	}
	for _, v := range ipr.Severity {
		query["severity"] = append(query["severity"], string(v))
	}
	for _, v := range ipr.RelatedIssueIDs {
		query["relatedIssueId"] = append(query["relatedIssueId"], strconv.FormatUint(v, 10))
	}
	for _, v := range ipr.IDs {
		query["IDs"] = append(query["IDs"], strconv.FormatInt(v, 10))
	}
	for _, v := range ipr.ExceptIDs {
		query["exceptIDs"] = append(query["exceptIDs"], strconv.FormatInt(v, 10))
	}
	if ipr.Source != "" {
		query["source"] = []string{ipr.Source}
	}
	if ipr.OrderBy != "" {
		query["orderBy"] = []string{ipr.OrderBy}
	}
	for _, v := range ipr.TaskType {
		query["taskType"] = append(query["taskType"], v)
	}
	for _, v := range ipr.BugStage {
		query["bugStage"] = append(query["bugStage"], v)
	}
	for _, v := range ipr.Owner {
		query["owner"] = append(query["owner"], v)
	}
	for _, v := range ipr.StateBelongs {
		query["stateBelongs"] = append(query["stateBelongs"], string(v))
	}
	query["asc"] = append(query["asc"], strconv.FormatBool(ipr.Asc))
	query["withProcessSummary"] = append(query["withProcessSummary"], strconv.FormatBool(ipr.WithProcessSummary))

	return query
}

// IssuePagingResponse 事件分页查询响应
type IssuePagingResponse struct {
	Header
	UserInfoHeader
	Data *IssuePagingResponseData `json:"data"`
}

type IssuePagingResponseData struct {
	Total uint64  `json:"total"`
	List  []Issue `json:"list"`
}

// IssueGetRequest 事件查询请求
type IssueGetRequest struct {
	ID uint64

	// internal use, get from *http.Request
	IdentityInfo
}

// IssueGetResponse 事件查询响应
type IssueGetResponse struct {
	Header
	Data *Issue `json:"data"`
}

type IssueTime time.Time

func (m *IssueTime) UnmarshalJSON(data []byte) error {
	s := string(data)
	if s == "null" {
		return nil
	}
	if s == `""` {
		*m = IssueTime(time.Unix(0, 0))
		return nil
	}
	return json.Unmarshal(data, (*time.Time)(m))
}

func (m *IssueTime) MarshalJSON() ([]byte, error) {
	return json.Marshal((*time.Time)(m))
}

func (m *IssueTime) IsEmpty() bool {
	if m == nil {
		return true
	}
	t := time.Time(*m)
	return t.IsZero()
}

func (m *IssueTime) Value() *time.Time {
	if m == nil {
		return nil
	}
	t := time.Time(*m)
	if t.IsZero() || t.Equal(time.Unix(0, 0)) {
		return nil
	}
	return &t
}

func (m *IssueTime) Time() *time.Time {
	if m == nil {
		return nil
	}
	t := time.Time(*m)
	if t.Equal(time.Unix(0, 0)) {
		return nil
	}
	return &t
}

// IssueUpdateRequest 事件更新请求
type IssueUpdateRequest struct {
	Title                 *string          `json:"title"`
	Content               *string          `json:"content"`
	State                 *int64           `json:"state"`
	Priority              *IssuePriority   `json:"priority"`
	Complexity            *IssueComplexity `json:"complexity"`
	Severity              *IssueSeverity   `json:"severity"`
	PlanStartedAt         IssueTime        `json:"planStartedAt"`
	PlanFinishedAt        IssueTime        `json:"planFinishedAt"`
	Assignee              *string          `json:"assignee"`
	IterationID           *int64           `json:"iterationID"`
	Source                *string          `json:"source"`        // 来源
	Labels                []string         `json:"labels"`        // label 名称列表
	RelatedIssues         []int64          `json:"relatedIssues"` // 已关联的issue
	TaskType              *string          `json:"taskType"`      // 任务类型
	BugStage              *string          `json:"bugStage"`      // bug阶段
	Owner                 *string          `json:"owner"`         // 负责人
	WithChildrenIteration bool             `json:"withChildrenIteration"`
	//工时信息，当事件类型为任务和缺陷时生效
	ManHour *IssueManHour `json:"issueManHour"`

	TestPlanCaseRelIDs       []uint64 `json:"testPlanCaseRelIDs"`       // 关联的测试计划用例 ID 列表，全量更新
	RemoveTestPlanCaseRelIDs bool     `json:"removeTestPlanCaseRelIDs"` // 是否清空所有关联的测试计划用例

	ID uint64 `json:"-"`
	// internal use, get from *http.Request
	IdentityInfo
	// Lang used for i18n
	Lang i18n.LanguageCodes
}

// IsEmpty 判断更新请求里的字段是否均为空
func (r *IssueUpdateRequest) IsEmpty() bool {
	return r.Title == nil && r.Content == nil && r.State == nil &&
		r.Priority == nil && r.Complexity == nil && r.Severity == nil &&
		r.PlanStartedAt.IsEmpty() && r.PlanFinishedAt.IsEmpty() &&
		r.Assignee == nil && r.IterationID == nil && r.ManHour == nil
}

// GetChangedFields 从 IssueUpdateRequest 中找出需要更新(不为空)的字段
// 注意：map 的 value 需要与 dao.Issue 字段类型一致
func (r *IssueUpdateRequest) GetChangedFields(manHour string) map[string]interface{} {
	fields := make(map[string]interface{})
	if r.Title != nil {
		fields["title"] = *r.Title
	}
	if r.Content != nil {
		fields["content"] = *r.Content
	}
	if r.State != nil {
		fields["state"] = *r.State
	}
	if r.Priority != nil {
		fields["priority"] = *r.Priority
	}
	if r.Complexity != nil {
		fields["complexity"] = *r.Complexity
	}
	if r.Severity != nil {
		fields["severity"] = *r.Severity
	}
	if !r.PlanStartedAt.IsEmpty() {
		fields["plan_started_at"] = r.PlanStartedAt.Time()
	}
	if !r.PlanFinishedAt.IsEmpty() {
		fields["plan_finished_at"] = r.PlanFinishedAt.Time()
	}
	if r.Assignee != nil {
		fields["assignee"] = *r.Assignee
	}
	if r.IterationID != nil {
		fields["iteration_id"] = *r.IterationID
	}
	if r.Source != nil {
		fields["source"] = *r.Source
	}
	if r.Owner != nil {
		fields["owner"] = *r.Owner
	}
	// TaskType和BugStage必定有一个为nil
	if r.BugStage != nil && len(*r.BugStage) != 0 {
		fields["stage"] = *r.BugStage
	} else if r.TaskType != nil && len(*r.TaskType) > 0 {
		fields["stage"] = *r.TaskType
	}
	if r.ManHour != nil {
		// if r.ManHour.ThisElapsedTime != 0 {
		// 	// 开始时间为当天0点
		// 	timeStr := time.Now().Format("2006-01-02")
		// 	t, _ := time.ParseInLocation("2006-01-02", timeStr, time.Local)
		// 	fields["plan_started_at"] = t
		// }
		// ManHour 是否改变提前特殊处理
		// 只有当预期时间或剩余时间发生改变时，才认为工时信息发生了改变
		// 所花时间，开始时间，工作内容是实时内容，只在事件动态里记录就好，在dice_issue表中是没有意义的数据
		var oldManHour IssueManHour
		json.Unmarshal([]byte(manHour), &oldManHour)
		if r.ManHour.ThisElapsedTime != 0 || r.ManHour.StartTime != "" || r.ManHour.WorkContent != "" ||
			r.ManHour.EstimateTime != oldManHour.EstimateTime || r.ManHour.RemainingTime != oldManHour.RemainingTime {
			// 剩余时间被修改过的话，需要标记一下
			if r.ManHour.RemainingTime != oldManHour.RemainingTime {
				r.ManHour.IsModifiedRemainingTime = true
			}
			// 已用时间累加上
			r.ManHour.ElapsedTime = oldManHour.ElapsedTime + r.ManHour.ThisElapsedTime
			fields["man_hour"] = r.ManHour.Convert2String()
		}
	}
	return fields
}

// GetFormartIssueRelations 获取以逗号分割的字符串形式的issueID串
func (r *IssueUpdateRequest) GetFormartIssueRelations() string {
	if r.RelatedIssues == nil {
		return ""
	}

	var issueIDStr []string
	for _, v := range r.RelatedIssues {
		issueIDStr = append(issueIDStr, strconv.FormatInt(v, 10))
	}
	return strings.Join(issueIDStr, ",")
}

// 更新事件类型请求
type IssueTypeUpdateRequest struct {
	ProjectID int64     `json:"projectID"`
	ID        int64     `json:"id"`
	Type      IssueType `json:"type"`
	IdentityInfo
}

// IssueUpdateResponse 事件更新响应
type IssueUpdateResponse struct {
	Header
	Data uint64 `json:"data"`
}

// IssueBatchUpdateRequest 批量更新事件请求
type IssueBatchUpdateRequest struct {
	All            bool     `json:"all"`      // 是否全选
	Mine           bool     `json:"mine"`     // all 为 true 时使用, mine 为 true，表示仅操作处理人为自己的
	IDs            []uint64 `json:"ids"`      // 待更新事件 id, 若全选，则此为空
	Assignee       string   `json:"assignee"` // 处理人
	State          int64    `json:"state"`    // 状态
	NewIterationID int64    `json:"newIterationID"`
	TaskType       string   `json:"taskType"` // 任务类型
	BugStage       string   `json:"bugStage"` // bug阶段
	Owner          string   `json:"owner"`    // 负责人

	// 以下字段用于鉴权, 不可更改
	CurrentIterationID  int64     `json:"currentIterationID"`
	CurrentIterationIDs []int64   `json:"currentIterationIDs"`
	Type                IssueType `json:"type"`
	ProjectID           uint64    `json:"projectID"`

	IdentityInfo
}

// CheckValid 仅需求、缺陷的处理人/状态可批量更新
func (r *IssueBatchUpdateRequest) CheckValid() error {
	if !r.All && len(r.IDs) == 0 {
		return errors.Errorf("none selected")
	}

	if r.Assignee == "" && r.State == 0 && r.NewIterationID == 0 {
		return errors.Errorf("none updated")
	}

	if r.Type != IssueTypeRequirement && r.Type != IssueTypeBug {
		return errors.Errorf("only requirement/bug can batch update")
	}

	return nil
}

// 任务统计
type IssueSummary struct {
	ProcessingCount int `json:"processingCount"` // 需求下未完成的关联事件数
	DoneCount       int `json:"doneCount"`       // 需求下已完成的关联事件数
}

// RequirementGroupResult 需求下任务统计
type RequirementGroupResult struct {
	ID    uint64 `json:"id"`    // 需求 id
	State int64  `json:"state"` // 任务状态
	Count int    `json:"count"` // 任务状态任务数
}

// IssueTestCaseRelationsListRequest 缺陷用例关联关系查询
type IssueTestCaseRelationsListRequest struct {
	IssueID           uint64 `json:"issueID"`
	TestPlanID        uint64 `json:"testPlanID"`
	TestPlanCaseRelID uint64 `json:"testPlanCaseRelID"`
	TestCaseID        uint64 `json:"testCaseID"`
}

// IssueManHourSumRequest 事件下所有的任务总和请求
type IssuesStageRequest struct {
	StatisticRange string `json:"statisticRange"` //事件类型 项目/迭代
	RangeID        int64  `json:"rangeId"`        //项目id/迭代id
}

// IssueManHourSumResponse 事件下所有的任务总和响应
type IssueManHourSumResponse struct {
	// Header
	DesignManHour    int64 `json:"designManHour"`
	DevManHour       int64 `json:"devManHour"`
	TestManHour      int64 `json:"testManHour"`
	ImplementManHour int64 `json:"implementManHour"`
	DeployManHour    int64 `json:"deployManHour"`
	OperatorManHour  int64 `json:"operatorManHour"`
	SumManHour       int64 `json:"sumManHour"`
}

// IssueBugPercentageResponse 缺陷率响应
type IssueBugPercentageResponse struct {
	BugPercentage []Percentage `json:"bugPercentage"`
}

// IssueBugStatusPercentageResponse 缺陷状态分布响应
type IssueBugStatusPercentage struct {
	Status []Percentage `json:"status"`
}

type IssueBugStatusPercentageResponse struct {
	StageName string                   `json:"stageName"`
	Status    IssueBugStatusPercentage `json:"status"`
}

// IssueBugSeverityPercentageResponse 缺陷等级分布响应
type IssueBugSeverityPercentage struct {
	Severity []Percentage `json:"severity"`
}

type IssueBugSeverityPercentageResponse struct {
	StageName string                     `json:"stageName"`
	Severity  IssueBugSeverityPercentage `json:"severity"`
}

type Percentage struct {
	Name  string  `json:"name"`
	Value float32 `json:"value"`
}

type IssueImportExcelResponse struct {
	SuccessNumber int    `json:"successNumber"`
	FalseNumber   int    `json:"falseNumber"`
	UUID          string `json:"uuid"`
}

// IssueSubscriberBatchUpdateRequest batch update the requests of issue subscribers
type IssueSubscriberBatchUpdateRequest struct {
	Subscribers  []string `json:"subscribers"`
	IssueID      int64    `json:"-"`
	IdentityInfo `json:"-"`
}

// IssueNum workbench special issue num
type IssueNum struct {
	IssueNum  uint64
	ProjectID uint64
}
