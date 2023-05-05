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

	"github.com/erda-project/erda/pkg/i18n"
)

type NotifyChannel string

// CreateNotifyRequest 创建通知请求
type CreateNotifyRequest struct {
	Name          string         `json:"name"`
	ScopeType     string         `json:"scopeType"`
	ScopeID       string         `json:"scopeId"`
	Enabled       bool           `json:"enabled"`
	Channels      string         `json:"channels"`
	NotifyGroupID int64          `json:"notifyGroupId"`
	NotifyItemIDs []int64        `json:"notifyItemIds"`
	WithGroup     bool           `json:"withGroup"`
	GroupTargets  []NotifyTarget `json:"groupTargets"`
	Label         string         `json:"label"`
	ClusterName   string         `json:"clusterName"`
	NotifySources []NotifySource `json:"notifySources"`
	WorkSpace     string         `json:"workspace"`
	Creator       string         `json:"-"`
	OrgID         int64          `json:"-"`
}

// CreateNotifyResponse 创建通知响应
type CreateNotifyResponse struct {
	Header
	Data *NotifyDetail `json:"data"`
}

// UpdateNotifyRequest 更新通知请求
type UpdateNotifyRequest struct {
	ID            int64          `json:"id"`
	Channels      string         `json:"channels"`
	NotifyGroupID int64          `json:"notifyGroupId"`
	NotifyItemIDs []int64        `json:"notifyItemIds"`
	NotifySources []NotifySource `json:"notifySources"`
	WithGroup     bool           `json:"withGroup"`
	GroupTargets  []NotifyTarget `json:"groupTargets"`
	GroupName     string         `json:"-"`
	OrgID         int64          `json:"-"`
}

// UpdateNotifyResponse 更新通知响应
type UpdateNotifyResponse struct {
	Header
	Data *NotifyDetail `json:"data"`
}

// DeleteNotifyResponse 删除通知响应
type DeleteNotifyResponse struct {
	Header
	Data *NotifyDetail `json:"data"`
}

// QueryNotifyRequest 查询通知列表请求
type QueryNotifyRequest struct {
	PageNo      int64  `query:"pageNo"`
	PageSize    int64  `query:"pageSize"`
	GroupDetail bool   `query:"groupDetail"`
	ScopeType   string `query:"scopeType"`
	ScopeID     string `query:"scopeId"`
	Label       string `query:"label"`
	ClusterName string `query:"clusterName"`
	OrgID       int64  `json:"-"`
}

// QueryNotifyResponse 查询通知列表响应
type QueryNotifyResponse struct {
	Header
	Data QueryNotifyData `json:"data"`
}

// QueryNotifyData 通知列表数据结构
type QueryNotifyData struct {
	List  []*NotifyDetail `json:"list"`
	Total int             `json:"total"`
}

// Notify 通知
type Notify struct {
	Name      string    `json:"name"`
	ScopeType string    `json:"scopeType"`
	ScopeID   string    `json:"scopeID"`
	Channels  string    `json:"channels"`
	Enabled   bool      `json:"enabled"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
}

// NotifyDetail 通知详情
type NotifyDetail struct {
	ID            int64           `json:"id"`
	Name          string          `json:"name"`
	ScopeType     string          `json:"scopeType"`
	ScopeID       string          `json:"scopeId"`
	Channels      string          `json:"channels"`
	NotifyItems   []*NotifyItem   `json:"notifyItems"`
	NotifyGroup   *NotifyGroup    `json:"notifyGroup"`
	NotifySources []*NotifySource `json:"notifySources"`
	Enabled       bool            `json:"enabled"`
	Creator       string          `json:"creator"`
	CreatedAt     time.Time       `json:"createdAt"`
	UpdatedAt     time.Time       `json:"updatedAt"`
}

// QuerySourceNotifyResponse 查询通知列表响应
type QuerySourceNotifyResponse struct {
	Header
	Data []*NotifyDetail `json:"data"`
}

// EnableNotifyResponse 启用通知响应
type EnableNotifyResponse struct {
	Header
	Data *NotifyDetail `json:"data"`
}

// DisableNotifyResponse 禁用通知响应
type DisableNotifyResponse struct {
	Header
	Data *NotifyDetail `json:"data"`
}

// FuzzyQueryNotifiesBySourceRequest 模糊查询通知请求
type FuzzyQueryNotifiesBySourceRequest struct {
	SourceType string
	OrgID      int64
	Locale     *i18n.LocaleResource
	Label      string

	// 查询条件
	PageNo      int64
	PageSize    int64
	ClusterName string
	SourceName  string
	NotifyName  string
	ItemName    string
	Channel     string
}

//消息通知对接组件化
type NotifyPageRequest struct {
	Scope   string `json:"scope"`
	ScopeId string `json:"scopeId"`
	UserId  string `json:"userId"`
	OrgId   string `json:"orgId"`
}

type NotifyListResponse struct {
	Header
	Data NotifyListBody `json:"data"`
}

type NotifyListBody struct {
	List []DataItem `json:"list"`
}

type DataItem struct {
	UserId       []string  `json:"userId"`
	CreatedAt    time.Time `json:"createdAt"`
	Id           int64     `json:"id"`
	NotifyID     string    `json:"notifyId"`
	NotifyName   string    `json:"notifyName"`
	Target       string    `json:"target"`
	NotifyTarget []Value   `json:"groupInfo"`
	Enable       bool      `json:"enable"`
	Items        []string  `json:"items"`
}

type Value struct {
	Type   string       `json:"type"`
	Values []ValueValue `json:"values"`
}

type ValueValue struct {
	Receiver string `json:"receiver"`
	Secret   string `json:"secret"`
}

type SwitchOperation struct {
	Meta SwitchOperationData `json:"meta"`
}

type SwitchOperationData struct {
	Id     uint64 `json:"id"`
	Enable bool   `json:"enable"`
}

type AllGroupResponse struct {
	Header
	Data []AllGroups `json:"data"`
}

type AllGroups struct {
	Name  string `json:"name"`
	Value int64  `json:"value"`
	Type  string `json:"type"`
}
