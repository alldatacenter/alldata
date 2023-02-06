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
	"database/sql/driver"
	"encoding/json"
	"time"

	"github.com/pkg/errors"
)

// TicketType 工单类型 可选值: task/bug/vulnerability/codeSmell/machine/component/addon/trace/glance/exception
type TicketType string

// 工单类型可选项
const (
	TicketTask                TicketType = "task"
	TicketBug                 TicketType = "bug"
	TicketVulnerability       TicketType = "vulnerability"
	TicketCodeSmell           TicketType = "codeSmell"
	TicketMergeRequest        TicketType = "mr"
	TicketMachineAlert        TicketType = "machine"         // 机器告警
	TicketComponentAlert      TicketType = "dice_component"  // 平台组件告警
	TicketAddOnAlert          TicketType = "addon"           // 中间件addon告警
	TicketDiceAddOnAlert      TicketType = "dice_addon"      // 平台addon告警
	TicketKubernetesAlert     TicketType = "kubernetes"      // kubernetes告警
	TicketAppStatusAlert      TicketType = "app_status"      // 主动监控告警
	TicketAppResourceAlert    TicketType = "app_resource"    // 应用(容器)资源告警
	TicketExceptionAlert      TicketType = "app_exception"   // 应用异常告警
	TicketAppTransactionAlert TicketType = "app_transaction" // 应用异常告警
)

// TicketPriority 工单优先级: 紧急/严重/一般
type TicketPriority string

// 工单优先级可选项
const (
	TicketHigh   TicketPriority = "high"
	TicketMedium TicketPriority = "medium"
	TicketLow    TicketPriority = "low"
)

// TicketStatus 工单状态
type TicketStatus string

// 工单状态可选项
const (
	TicketOpen   TicketStatus = "open"
	TicketClosed TicketStatus = "closed"
)

// TicketTarget 工单目标类型
type TicketTarget string

// 工单目标类型可选值
const (
	TicketCluster      TicketTarget = "cluster"
	TicketOrg          TicketTarget = "org"
	TicketProject      TicketTarget = "project"
	TicketMicroService TicketTarget = "micro_service"
	TicketApp          TicketTarget = "application"
)

// 工单虚拟用户
const (
	TicketUserQA string = "qa"
)

// TCType ticket comment type
type TCType string

const (
	// 一般评论
	NormalTCType TCType = "normal"
	// 关联事件评论
	IssueRelationTCType TCType = "issueRelation"
)

// TicketCreateRequest 工单创建请求
type TicketCreateRequest struct {
	// 工单标题
	Title string `json:"title"`

	// 工单内容
	Content string `json:"content"`

	// 工单类型 可选值: task/bug/vulnerability/codeSmell/machine/component/addon/trace/glance/exception
	Type TicketType `json:"type"`

	// 工单优先级，可选值: high/medium/low
	Priority TicketPriority `json:"priority"`

	// 告警工单使用，作为唯一 key 定位工单
	Key string `json:"key"`

	// 企业ID
	OrgID string `json:"orgID,omitempty"`

	// 告警指标，告警使用，其他类型工单不传
	Metric   string `json:"metric,omitempty"`
	MetricID string `json:"metricID,omitempty"`

	// 用户ID
	UserID string `json:"userID"`

	// 标签
	Label map[string]interface{} `json:"label,omitempty"`

	// 工单目标类型，可选值: machine/addon/project/application
	TargetType TicketTarget `json:"targetType,omitempty"`

	// 工单目标ID
	TargetID string `json:"targetID,omitempty"`

	// 触发时间
	TriggeredAt int64 `json:"triggeredAt,omitempty"`

	// 告警恢复时间
	ClosedAt int64 `json:"closedAt,omitempty"`
}

// TicketCreateResponse 工单创建响应
type TicketCreateResponse struct {
	Header

	// 工单ID
	Data int64 `json:"data"`
}

// TicketUpdateRequest 工单更新请求
type TicketUpdateRequest struct {
	TicketID int64                   `json:"-" path:"ticketID"`
	Body     TicketUpdateRequestBody `json:"body"`
}

// TicketUpdateRequestBody 工单更新请求body
type TicketUpdateRequestBody struct {
	// 工单标题
	Title string `json:"title"`

	// 工单内容
	Content string `json:"content"`

	// 工单类型，可选值: task/bug/vulnerability/codeSmell/machine/component/addon/trace/glance/exception
	Type TicketType `json:"type"`

	// 工单优先级，可选值: high/medium/low
	Priority TicketPriority `json:"priority"`
}

// TicketUpdateResponse 工单更新响应
type TicketUpdateResponse struct {
	Header

	// 工单ID
	Data int64 `json:"data"`
}

// TicketDeleteRequest 工单删除请求
type TicketDeleteRequest struct {
	TicketID int64 `path:"ticketID"`
}

// TicketDeleteResponse 工单删除响应
type TicketDeleteResponse struct {
	Header

	// 工单ID
	Data int64 `json:"data"`
}

// TicketCloseRequest 工单关闭请求
type TicketCloseRequest struct {
	TicketID int64 `path:"ticketID"`
}

// TicketCloseResponse 工单关闭响应
type TicketCloseResponse struct {
	Header

	// 工单ID
	Data int64 `json:"data"`
}

// TicketReopenRequest 已关闭工单打开请求
type TicketReopenRequest struct {
	TicketID int64 `path:"ticketID"`
}

// TicketReopenResponse 已关闭工单打开响应
type TicketReopenResponse struct {
	Header

	// 工单ID
	Data int64 `json:"data"`
}

// TicketFetchRequest 工单详情请求
type TicketFetchRequest struct {
	TicketID int64 `path:"ticketID"`
}

// TicketFetchResponse 工单详情响应
type TicketFetchResponse struct {
	Header
	Data Ticket `json:"data"`
}

// TicketListRequest 工单列表请求
type TicketListRequest struct {
	// 工单类型，选填 可选值: task/bug/vulnerability/codeSmell/machine/component/addon/trace/glance/exception
	Type []TicketType `query:"type"`

	// 工单优先级，选填 可选值: high/medium/low
	Priority TicketPriority `query:"priority"`

	// 工单状态，选填 可选值: open/closed
	Status TicketStatus `query:"status"`

	// 工单关联目标类型, 选填 可选值: cluster/project/application
	TargetType TicketTarget `query:"targetType"`

	// 工单关联目标ID，选填
	TargetID string `query:"targetID"`

	// 告警工单 key，选填， 用于定位告警类工单
	Key string `json:"key"`

	// 企业ID, 选填，集群类告警时使用
	OrgID int64 `query:"orgID"`

	// 告警维度，选填(仅供告警类工单使用) eg: cpu/mem/load
	Metric string `query:"metric"`

	// 告警维度取值, 选填
	MetricID []string `query:"metricID"`

	// 起始时间戳(ms)，选填
	StartTime int64 `query:"startTime"`

	// 截止时间戳(ms)，选填 默认为当前时间
	EndTime int64 `query:"endTime"`

	// 是否包含工单最新评论，默认false
	Comment bool `query:"comment"`

	// 查询参数，按title/label模糊匹配
	Q string `query:"q"`

	// 页号, 默认值:1
	PageNo int `query:"pageNo,omitempty"`

	// 分页大小, 默认值20
	PageSize int `query:"pageSize,omitempty"`
}

// TicketListResponse 工单列表响应
type TicketListResponse struct {
	Header
	Data TicketListResponseData `json:"data"`
}

// TicketListResponseData 工单列表响应数据
type TicketListResponseData struct {
	Total   int64    `json:"total"`
	Tickets []Ticket `json:"tickets"`
}

// Ticket 工单数据DTO
type Ticket struct {
	// 工单ID
	TicketID int64 `json:"id"`

	// 工单标题
	Title string `json:"title"`

	// 工单内容
	Content string `json:"content"`

	// 工单类型，可选值: bug/vulnerability/codeSmell/task
	Type TicketType `json:"type"`

	// 工单优先级，可选值: high/medium/low
	Priority TicketPriority `json:"priority"`

	// 工单状态，可选值: open/closed
	Status TicketStatus `json:"status"`

	// 告警工单 key，选填， 用于定位告警类工单
	Key string `json:"key"`

	OrgID string `json:"orgID"`

	// 告警指标，告警使用，其他类型工单不传
	Metric   string `json:"metric"`
	MetricID string `json:"metricID"`

	// 累计告警次数
	Count int64 `json:"count"`

	// 工单创建者ID
	Creator string `json:"creator"`

	// 工单最近操作者ID
	LastOperator string `json:"lastOperator"`

	// 标签
	Label map[string]interface{} `json:"label"`

	// 工单目标类型，可选值: cluster/project/application
	TargetType TicketTarget `json:"targetType"`

	// 工单最新评论，仅主动监控使用
	LastComment *Comment `json:"lastComment,omitempty"`

	// 工单目标ID
	TargetID string `json:"targetID"`

	// 创建时间
	CreatedAt time.Time `json:"createdAt"`

	// 更新时间
	UpdatedAt time.Time `json:"updatedAt"`

	// 关闭时间
	ClosedAt time.Time `json:"closedAt"`

	// 触发时间
	TriggeredAt time.Time `json:"triggeredAt"`
}

// CommentCreateRequest 工单评论创建请求
type CommentCreateRequest struct {
	// 工单ID
	TicketID int64 `json:"ticketID"`
	// 评论类型
	CommentType TCType `json:"commentType"`
	// 评论内容
	Content string `json:"content"`
	// 关联事件评论内容
	IRComment IRComment `json:"irComment"`

	// 评论用户ID
	UserID string `json:"userID"`
}

// CommentCreateResponse 工单评论创建响应
type CommentCreateResponse struct {
	Header

	// 评论ID
	Data int64 `json:"data"`
}

// CommentUpdateRequest 工单评论编辑请求
type CommentUpdateRequest struct {
	CommentID int64                    `json:"-" path:"commentID"`
	Body      CommentUpdateRequestBody `json:"body"`
}

// CommentUpdateRequestBody 工单评论编辑请求body
type CommentUpdateRequestBody struct {
	// 评论内容
	Content string `json:"content"`
}

// CommentUpdateResponse 工单评论编辑响应
type CommentUpdateResponse struct {
	Header

	// 评论ID
	Data int64 `json:"data"`
}

// CommentListRequest 工单评论列表
type CommentListRequest struct {
	TicketID int64 `query:"ticketID"`
}

// CommentListResponse 工单评论响应
type CommentListResponse struct {
	Header
	Data CommentListResponseData `json:"data"`
}

// CommentListResponseData 工单评论响应数据
type CommentListResponseData struct {
	Total    int64     `json:"total"`
	Comments []Comment `json:"comments"`
}

// Comment 评论DTO
type Comment struct {
	// 评论ID
	CommentID int64 `json:"id"`

	// 工单ID
	TicketID int64 `json:"ticketID"`

	// 工单评论类型
	CommentType TCType `json:"commentType"`

	// 评论内容
	Content string `json:"content"`

	// 关联任务工单
	IRComment IRComment `json:"irComment"`

	// 评论用户ID
	UserID string `json:"userID"`

	// 创建时间
	CreatedAt time.Time `json:"createdAt"`

	// 更新时间
	UpdatedAt time.Time `json:"updatedAt"`
}

// IRComment 事件关联评论
type IRComment struct {
	IterationID uint64    `json:"iterationID"`
	IssueID     int64     `json:"issueID"`
	IssueTitle  string    `json:"issueTitle"`
	IssueType   IssueType `json:"issueType"`
	ProjectID   uint64    `json:"projectID"`
}

// Value gorm Marshal
func (i IRComment) Value() (driver.Value, error) {
	c, err := json.Marshal(i)
	if err != nil {
		return nil, errors.Errorf("failed to marshal IRComment, err: %v", err)
	}
	return string(c), nil
}

// Scan gorm Unmarshal
func (i *IRComment) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.Errorf("invalid scan source for IRComment")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, i); err != nil {
		return err
	}
	return nil
}
