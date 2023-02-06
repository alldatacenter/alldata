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

type EdgeSiteInfo struct {
	ID          int64     `json:"id"`
	OrgID       int64     `json:"orgID"`
	Name        string    `json:"name"`
	DisplayName string    `json:"displayName"`
	ClusterID   int64     `json:"clusterID"`
	ClusterName string    `json:"clusterName"`
	Logo        string    `json:"logo"`
	Description string    `json:"description"`
	NodeCount   string    `json:"nodeCount"`
	Status      int64     `json:"status"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

// EdgeSiteCreateRequest 创建边缘站点请求
type EdgeSiteCreateRequest struct {
	OrgID       int64  `json:"orgID"`
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	ClusterID   int64  `json:"clusterID"`
	Logo        string `json:"logo"`
	Description string `json:"description"`
	Status      int64  `json:"status"`
}

// EdgeSiteUpdateRequest 更新边缘站点请求
type EdgeSiteUpdateRequest struct {
	DisplayName string `json:"displayName"`
	Logo        string `json:"logo"`
	Description string `json:"description"`
	Status      int64  `json:"status"`
}

// EdgeSiteListPageRequest 分页查询请求, NotPaging 参数默认为 false，开启分页
type EdgeSiteListPageRequest struct {
	OrgID     int64
	ClusterID int64
	NotPaging bool
	Search    string
	PageNo    int `query:"pageNo"`
	PageSize  int `query:"pageSize"`
}

// EdgeSiteListResponse 站点列表响应体
type EdgeSiteListResponse struct {
	Total int            `json:"total"`
	List  []EdgeSiteInfo `json:"list"`
}
