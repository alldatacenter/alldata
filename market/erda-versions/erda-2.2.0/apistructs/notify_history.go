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

// NotifyHistory
type NotifyHistory struct {
	ID         int64  `json:"id"`
	NotifyName string `json:"notifyName"`
	// todo json key名需要cdp前端配合修改后再改
	NotifyItemDisplayName string         `json:"notifyItemName"`
	Channel               string         `json:"channel"`
	NotifyTargets         []NotifyTarget `json:"notifyTargets"`
	NotifySource          NotifySource   `json:"notifySource"`
	Status                string         `json:"status"`
	ErrorMsg              string         `json:"errorMsg"`
	Label                 string         `json:"label"`
	CreatedAt             time.Time      `json:"createdAt"`
}

// QueryNotifyHistoryRequest 查询通知发送记录请求
type QueryNotifyHistoryRequest struct {
	PageNo      int64  `query:"pageNo"`
	PageSize    int64  `query:"pageSize"`
	NotifyName  string `query:"notifyName"`
	StartTime   string `query:"startTime"`
	EndTime     string `query:"endTime"`
	Channel     string `query:"channel"`
	Label       string `query:"label"`
	ClusterName string `query:"clusterName"`
	OrgID       int64  `json:"-"`
}

// QueryNotifyHistoryResponse 查询通知历史纪录响应
type QueryNotifyHistoryResponse struct {
	Header
	Data QueryNotifyHistoryData `json:"data"`
}

// QueryNotifyHistoryData 通知发送记录结构
type QueryNotifyHistoryData struct {
	List  []*NotifyHistory `json:"list"`
	Total int              `json:"total"`
}

// CreateNotifyHistoryRequest 创建通知发送记录请求
type CreateNotifyHistoryRequest struct {
	NotifyName            string                 `json:"notifyName"`
	NotifyItemDisplayName string                 `json:"notifyItemDisplayName"`
	Channel               string                 `json:"channel"`
	NotifyTargets         []NotifyTarget         `json:"notifyTargets"`
	NotifySource          NotifySource           `json:"notifySource"`
	Status                string                 `json:"status"`
	ErrorMsg              string                 `json:"errorMsg"`
	OrgID                 int64                  `json:"orgId"`
	NotifyTags            map[string]interface{} `json:"notifyTags"`
	Label                 string                 `json:"label"`
	ClusterName           string                 `json:"clusterName" query:"clusterName"`
}

// CreateNotifyHistoryResponse 创建通知发送记录响应
type CreateNotifyHistoryResponse struct {
	Header
	Data int64
}
