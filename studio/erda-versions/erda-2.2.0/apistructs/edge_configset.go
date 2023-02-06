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

// EdgeConfigSetInfo 边缘站点配置信息
type EdgeConfigSetInfo struct {
	ID          int64     `json:"id"`
	OrgID       int64     `json:"orgID"`
	Name        string    `json:"name"`
	DisplayName string    `json:"displayName"`
	ClusterID   int64     `json:"clusterID"`
	ClusterName string    `json:"clusterName"`
	Description string    `json:"description"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

// EdgeConfigSetCreateRequest 创建边缘站点请求
type EdgeConfigSetCreateRequest struct {
	ClusterID   int64  `json:"clusterID"`
	OrgID       int64  `json:"orgID"`
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Description string `json:"description"`
}

// EdgeConfigSetUpdateRequest 更新边缘站点请求
type EdgeConfigSetUpdateRequest struct {
	DisplayName string `json:"displayName"`
	Description string `json:"description"`
}

// EdgeConfigSetListPageRequest 分页查询请求
type EdgeConfigSetListPageRequest struct {
	OrgID     int64
	ClusterID int64
	NotPaging bool
	PageNo    int `query:"pageNo"`
	PageSize  int `query:"pageSize"`
}

// EdgeConfigSetListResponse 站点列表响应体
type EdgeConfigSetListResponse struct {
	Total int                 `json:"total"`
	List  []EdgeConfigSetInfo `json:"list"`
}
