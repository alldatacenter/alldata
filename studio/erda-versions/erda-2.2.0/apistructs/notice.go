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

// Notice 平台公告
type Notice struct {
	ID        uint64       `json:"id"`
	OrgID     uint64       `json:"orgID"`
	Content   string       `json:"content"`
	Status    NoticeStatus `json:"status"`
	Creator   string       `json:"creator"`
	CreatedAt *time.Time   `json:"createdAt"`
	UpdateAt  *time.Time   `json:"updatedAt"`
}

// NoticeStatus 平台公告状态
type NoticeStatus string

// 平台公告状态集
const (
	NoticeUnpublished NoticeStatus = "unpublished"
	NoticePublished   NoticeStatus = "published"
	NoticeDeprecated  NoticeStatus = "deprecated"
)

// NoticeCreateRequest 公告创建请求
type NoticeCreateRequest struct {
	Content string `json:"content"`
	IdentityInfo
}

// NoticeCreateResponse 公告创建响应
type NoticeCreateResponse struct {
	Header
	Data Notice `json:"data"`
}

// NoticeDeleteResponse 公告删除响应
type NoticeDeleteResponse struct {
	Header
	Data Notice `json:"data"`
}

// NoticePublishResponse 公告删除响应
type NoticePublishResponse struct {
	Header
	Data Notice `json:"data"`
}

// NoticeUnPublishResponse 公告删除响应
type NoticeUnPublishResponse struct {
	Header
	Data Notice `json:"data"`
}

// NoticeUpdateRequest 公告更新请求
type NoticeUpdateRequest struct {
	Content string `json:"content"`

	ID uint64 `json:"-"`
}

// NoticeUpdateResponse 公告更新响应
type NoticeUpdateResponse struct {
	Header
	Data uint64 `json:"data"`
}

// NoticeListRequest 公告列表请求
type NoticeListRequest struct {
	// +required 后端赋值
	OrgID uint64
	// +optional
	Content string `schema:"content"`
	// +optional
	Status NoticeStatus `schema:"status"`
	// +optional
	PageNo uint64 `schema:"pageNo"`
	// +optional
	PageSize uint64 `schema:"pageSize"`

	IdentityInfo
}

// NoticeListResponse 公告列表响应
type NoticeListResponse struct {
	Header
	UserInfoHeader
	Data NoticeListResponseData `json:"data"`
}

// NoticeListResponseData 公告列表响应数据
type NoticeListResponseData struct {
	Total uint64   `json:"total"`
	List  []Notice `json:"list"`
}
