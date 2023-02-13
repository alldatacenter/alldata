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

package common

import (
	"bytes"
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/providers/i18n"
	corepb "github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/stream/pb"
)

const (
	ISTCreate                        string = "Create" // 创建事件
	ISTComment                       string = "Comment"
	ISTRelateMR                      string = "RelateMR" // 关联 MR
	ISTAssign                        string = "Assign"
	ISTTransferState                 string = "TransferState" // 状态迁移
	ISTChangeTitle                   string = "ChangeTitle"
	ISTChangePlanStartedAt           string = "ChangePlanStartedAt"           // 更新计划开始时间
	ISTChangePlanFinishedAt          string = "ChangePlanFinishedAt"          // 更新计划结束时间
	ISTChangeAssignee                string = "ChangeAssignee"                // 更新处理人
	ISTChangeIteration               string = "ChangeIteration"               // 更新迭代
	ISTChangeIterationFromUnassigned string = "ChangeIterationFromUnassigned" // change iteration from unassigned iteration
	ISTChangeIterationToUnassigned   string = "ChangeIterationToUnassigned"   // change iteration to unassigned iteration
	ISTChangeManHour                 string = "ChangeManHour"                 // 更新工时信息
	ISTChangeOwner                   string = "ChangeOwner"                   // 更新责任人
	ISTChangeTaskType                string = "ChangeTaskType"                // 更新任务类型/引用源
	ISTChangeBugStage                string = "ChangeBugStage"                // 更新引用源
	ISTChangePriority                string = "ChangePriority"                // 更新优先级
	ISTChangeComplexity              string = "ChangeComplexity"              // 更新复杂度
	ISTChangeSeverity                string = "ChangeSeverity"                // 更新严重度
	ISTChangeContent                 string = "ChangeContent"                 // 更新内容
	ISTChangeLabel                   string = "ChangeLabel"                   // 更新标签
)

// ISTParam issue stream template params, 字段名称须与模板内占位符匹配
type ISTParam struct {
	Comment     string `json:",omitempty"` // 评论内容
	CommentTime string `json:",omitempty"` // comment time
	UserName    string `json:",omitempty"` // 用户名

	MRInfo pb.MRCommentInfo `json:",omitempty"` // MR 类型评论内容

	CurrentState string `json:",omitempty"` // 当前状态
	NewState     string `json:",omitempty"` // 新状态

	CurrentTitle string `json:",omitempty"` // 当前标题
	NewTitle     string `json:",omitempty"` // 新标题

	CurrentPlanStartedAt string `json:",omitempty"` // 当前计划开始时间
	NewPlanStartedAt     string `json:",omitempty"` // 新计划开始时间

	CurrentPlanFinishedAt string `json:",omitempty"` // 当前计划结束时间
	NewPlanFinishedAt     string `json:",omitempty"` // 新计划结束时间

	CurrentAssignee string `json:",omitempty"` // 当前处理人
	NewAssignee     string `json:",omitempty"` // 新处理人

	CurrentIteration string `json:",omitempty"` // 当前迭代
	NewIteration     string `json:",omitempty"` // 新迭代

	CurrentEstimateTime  string `json:",omitempty"` //当前预估时间
	CurrentElapsedTime   string `json:",omitempty"` //当前已用时间
	CurrentRemainingTime string `json:",omitempty"` //当前剩余时间
	CurrentStartTime     string `json:",omitempty"` //当前开始时间
	CurrentWorkContent   string `json:",omitempty"` //当前工作内容
	NewEstimateTime      string `json:",omitempty"` //新预估时间
	NewElapsedTime       string `json:",omitempty"` //新已用时间
	NewRemainingTime     string `json:",omitempty"` //新剩余时间
	NewStartTime         string `json:",omitempty"` //新开始时间
	NewWorkContent       string `json:",omitempty"` //新工作内容

	CurrentOwner string `json:",omitempty"` // 当前责任人
	NewOwner     string `json:",omitempty"` // 新责任人

	CurrentStage string `json:",omitempty"` // 当前任务类型/引用源
	NewStage     string `json:",omitempty"` // 新任务类型/引用源

	CurrentPriority string `json:",omitempty"` // 当前优先级
	NewPriority     string `json:",omitempty"` // 新优先级

	CurrentComplexity string `json:",omitempty"` // 当前复杂度
	NewComplexity     string `json:",omitempty"` // 新复杂度

	CurrentSeverity string `json:",omitempty"` // 当前严重性
	NewSeverity     string `json:",omitempty"` // 新严重性

	CurrentContent string `json:",omitempty"` // 当前内容
	NewContent     string `json:",omitempty"` // 新内容

	CurrentLabel string `json:",omitempty"` // 当前标签
	NewLabel     string `json:",omitempty"` // 新标签

	ReasonDetail string `json:",omitempty"`
}

type IssueStreamCreateRequest struct {
	IssueID      int64    `json:"issueID"`
	Operator     string   `json:"operator"`
	StreamType   string   `json:"streamType"`
	StreamParams ISTParam `json:"streamParams"`
	StreamTypes  []string `json:"streamTypes"`
	// internal use, get from *http.Request
	// IdentityInfo
}

// IssueTemplate issue 事件模板, key 为 language
var IssueTemplate = map[string]map[string]string{
	"zh": {
		ISTCreate:                        `该事项由 {{.UserName}} 创建`,
		ISTComment:                       `{{.Comment}}`,
		ISTRelateMR:                      `mrInfo: {{.MRInfo}}`,
		ISTAssign:                        `分派给 "{{.UserName}}" 处理`,
		ISTTransferState:                 `状态自 "{{.CurrentState}}" 迁移至 "{{.NewState}}"`,
		ISTChangeTitle:                   `标题自 "{{.CurrentTitle}}" 更新为 "{{.NewTitle}}"`,
		ISTChangePlanStartedAt:           `计划开始时间自 "{{.CurrentPlanStartedAt}}" 调整为 "{{.NewPlanStartedAt}}"`,
		ISTChangePlanFinishedAt:          `计划结束时间自 "{{.CurrentPlanFinishedAt}}" 调整为 "{{.NewPlanFinishedAt}}"`,
		ISTChangeAssignee:                `处理人由 "{{.CurrentAssignee}}" 变更为 "{{.NewAssignee}}"`,
		ISTChangeIteration:               `迭代由 "{{.CurrentIteration}}" 变更为 "{{.NewIteration}}"`,
		ISTChangeIterationFromUnassigned: `迭代由 "待处理" 变更为 "{{.NewIteration}}"`,
		ISTChangeIterationToUnassigned:   `迭代由 "{{.CurrentIteration}}" 变更为 "待处理"`,
		ISTChangeManHour:                 `工时信息由【预估时间：{{.CurrentEstimateTime}}，已用时间：{{.CurrentElapsedTime}}，剩余时间：{{.CurrentRemainingTime}}，开始时间：{{.CurrentStartTime}}，工作内容：{{.CurrentWorkContent}}】变更为【预估时间：{{.NewEstimateTime}}，已用时间：{{.NewElapsedTime}}，剩余时间：{{.NewRemainingTime}}，开始时间：{{.NewStartTime}}，工作内容：{{.NewWorkContent}}】`,
		ISTChangeOwner:                   `责任人由 "{{.CurrentOwner}}" 变更为 "{{.NewOwner}}"`,
		ISTChangeTaskType:                `任务类型由 "{{.CurrentStage}}" 变更为 "{{.NewStage}}"`,
		ISTChangeBugStage:                `引入源由 "{{.CurrentStage}}" 变更为 "{{.NewStage}}"`,
		ISTChangePriority:                `优先级由 "{{.CurrentPriority}}" 变更为 "{{.NewPriority}}"`,
		ISTChangeComplexity:              `复杂度由 "{{.CurrentComplexity}}" 变更为 "{{.NewComplexity}}"`,
		ISTChangeSeverity:                `严重程度由 "{{.CurrentSeverity}}" 变更为 "{{.NewSeverity}}"`,
		ISTChangeContent:                 `内容发生变更`,
		ISTChangeLabel:                   `标签发生变更`,
	},
	`en`: {
		ISTCreate:                        `Created by {{.UserName}}`,
		ISTComment:                       `{{.Comment}}`,
		ISTRelateMR:                      `mrInfo: {{.MRInfo}}`,
		ISTAssign:                        `assigned to "{{.UserName}}"`,
		ISTTransferState:                 `transfer state from "{{.CurrentState}}" to "{{.NewState}}"`,
		ISTChangeTitle:                   `change title "{{.CurrentTitle}}" to "{{.NewTitle}}"`,
		ISTChangePlanStartedAt:           `adjust Planned Start Time from "{{.CurrentPlanStartedAt}}" to "{{.NewPlanStartedAt}}"`,
		ISTChangePlanFinishedAt:          `adjust Planned Finished Time from "{{.CurrentPlanFinishedAt}}" to "{{.NewPlanFinishedAt}}"`,
		ISTChangeAssignee:                `adjust Assignee from "{{.CurrentAssignee}}" to "{{.NewAssignee}}"`,
		ISTChangeIteration:               `adjust Iteration from "{{.CurrentIteration}}" to "{{.NewIteration}}"`,
		ISTChangeIterationFromUnassigned: `adjust Iteration from "unassigned" to "{{.NewIteration}}"`,
		ISTChangeIterationToUnassigned:   `adjust Iteration from "{{.CurrentIteration}}" to "unassigned"`,
		ISTChangeManHour:                 `adjust man-hour from【EstimateTime: {{.CurrentEstimateTime}}, ElapsedTime: {{.CurrentElapsedTime}}, RemainingTime: {{.CurrentRemainingTime}}, StartTime: {{.CurrentStartTime}}, WorkContent: {{.CurrentWorkContent}}】to【EstimateTime: {{.NewEstimateTime}}, ElapsedTime: {{.NewElapsedTime}}, RemainingTime: {{.NewRemainingTime}}, StartTime: {{.NewStartTime}}, WorkContent: {{.NewWorkContent}}】`,
		ISTChangeOwner:                   `adjust owner from "{{.CurrentOwner}}" to "{{.NewOwner}}"`,
		ISTChangeTaskType:                `adjust task type from "{{.CurrentStage}}" to "{{.NewStage}}"`,
		ISTChangeBugStage:                `adjust bug stage from "{{.CurrentStage}}" to "{{.NewStage}}"`,
		ISTChangePriority:                `adjust priority from "{{.CurrentPriority}}" to "{{.NewPriority}}"`,
		ISTChangeComplexity:              `adjust complexity from "{{.CurrentComplexity}}" to "{{.NewComplexity}}"`,
		ISTChangeSeverity:                `adjust severity from "{{.CurrentSeverity}}" to "{{.NewSeverity}}"`,
		ISTChangeContent:                 `content changed`,
		ISTChangeLabel:                   `label changed`,
	},
}

const (
	ChildrenInProgress     = "childrenInProgress"
	MrCreated              = "mrCreated"
	IterationChanged       = "iterationChanged"
	PlanFinishedAtChanged  = "planFinishedAtChanged"
	ChildrenPlanUpdated    = "childrenPlanUpdated"
	ParentLabelsChanged    = "parentLabelsChanged"
	ParentIterationChanged = "parentIterationChanged"
)

// IssueTemplateOverrideForMsgSending override IssueTemplate for better event message sending
var IssueTemplateOverrideForMsgSending = map[string]map[string]string{
	"zh": {
		ISTComment: `添加了备注: {{.Comment}}`,
	},
	"en": {
		ISTComment: `added a comment: {{.Comment}}`,
	},
}

func (p ISTParam) Value() (driver.Value, error) {
	b, err := json.Marshal(p)
	if err != nil {
		return nil, errors.Errorf("failed to marshal ISTParam, err: %v", err)
	}
	return string(b), nil
}

func (p *ISTParam) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.Errorf("invalid scan source for ISTParam")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, p); err != nil {
		return err
	}
	return nil
}

func (p *ISTParam) Localize(tran i18n.Translator, lang i18n.LanguageCodes) *ISTParam {
	//// CurrentState
	//
	//p.CurrentState = IssueState(p.CurrentState).Desc(locale)
	//
	//// NewStatue
	// p.NewState = IssueState(p.NewState).Desc(locale)

	// old data CN to i18n key
	p.CurrentComplexity = tran.Text(lang, p.CurrentComplexity)
	p.NewComplexity = tran.Text(lang, p.NewComplexity)
	p.CurrentSeverity = tran.Text(lang, strings.ToLower(p.CurrentSeverity))
	p.NewSeverity = tran.Text(lang, strings.ToLower(p.NewSeverity))
	p.CurrentPriority = tran.Text(lang, strings.ToLower(p.CurrentPriority))
	p.NewPriority = tran.Text(lang, strings.ToLower(p.NewPriority))
	return p
}

func GetEventAction(ist string) string {
	switch ist {
	case ISTCreate:
		return "create"
	default:
		return "update"
	}
}

type IssueEventData struct {
	Title        string            `json:"title"`
	Content      string            `json:"content"`
	AtUserIDs    string            `json:"atUserIds"`
	Receivers    []string          `json:"receivers"`
	IssueType    string            `json:"issueType"`
	StreamTypes  []string          `json:"streamTypes"`
	StreamType   string            `json:"streamType"`
	StreamParams ISTParam          `json:"streamParams"`
	Params       map[string]string `json:"params"`
}

func GetFormartTime(imh *corepb.IssueManHour, key string) string {
	if imh == nil {
		return ""
	}
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

const (
	Minute int64 = 1
	Hour   int64 = 60 * Minute
	Day    int64 = 8 * Hour
	Week   int64 = 5 * Day
)

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

const SystemOperator = "system"
