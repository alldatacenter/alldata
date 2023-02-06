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

// EdgeCfgSetItemInfo 边缘站点配置信息
type EdgeCfgSetItemInfo struct {
	ID              int64     `json:"id"`
	ConfigSetID     int64     `json:"configSetID"`
	SiteID          int64     `json:"siteID"`
	SiteName        string    `json:"siteName"`
	SiteDisplayName string    `json:"siteDisplayName"`
	ItemKey         string    `json:"itemKey"`
	ItemValue       string    `json:"itemValue"`
	Scope           string    `json:"scope"`
	CreatedAt       time.Time `json:"createdAt"`
	UpdatedAt       time.Time `json:"updatedAt"`
}

// EdgeCfgSetItemCreateRequest 创建边缘站点请求
type EdgeCfgSetItemCreateRequest struct {
	ConfigSetID int64   `json:"configSetID"`
	Scope       string  `json:"scope"`
	SiteIDs     []int64 `json:"siteIDs"`
	ItemKey     string  `json:"itemKey"`
	ItemValue   string  `json:"itemValue"`
}

// EdgeCfgSetItemUpdateRequest 更新边缘站点请求
type EdgeCfgSetItemUpdateRequest struct {
	EdgeCfgSetItemCreateRequest
}

// EdgeCfgSetItemListPageRequest 分页查询请求
type EdgeCfgSetItemListPageRequest struct {
	Scope       string
	ConfigSetID int64
	Search      string
	SiteID      int64
	NotPaging   bool
	PageNo      int `query:"pageNo"`
	PageSize    int `query:"pageSize"`
}

// EdgeCfgSetItemListResponse 站点列表响应体
type EdgeCfgSetItemListResponse struct {
	Total int                  `json:"total"`
	List  []EdgeCfgSetItemInfo `json:"list"`
}
