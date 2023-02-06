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

package model

import "time"

type BaseModel struct {
	ID        int64     `json:"id" gorm:"primary_key"`
	CreatedAt time.Time `json:"createdAt" gorm:"created_at"`
	UpdatedAt time.Time `json:"updatedAt" gorm:"updated_at"`
}

type FilterStatusRequest struct {
	OrgId     int
	ScopeType string
	ScopeId   string
	StartTime string
	EndTime   string
}

type FilterStatusResult struct {
	Status string
	Count  int64
}

type NotifyValue struct {
	Field     string    `json:"field"`
	Count     int64     `json:"count"`
	RoundTime time.Time `json:"round_time"`
}

type QueryNotifyHistoriesRequest struct {
	PageNo      int64  `json:"pageNo"`
	PageSize    int64  `json:"pageSize"`
	Channel     string `json:"channel"`
	NotifyName  string `json:"notifyName"`
	StartTime   string `json:"startTime"`
	EndTime     string `json:"endTime"`
	Label       string `json:"label"`
	ClusterName string `json:"clusterName"`
	OrgID       int64  `json:"orgID"`
}

type QueryAlertNotifyIndexRequest struct {
	ScopeType  string   `json:"scopeType"`
	ScopeID    string   `json:"scopeID"`
	NotifyName string   `json:"notifyName"`
	Status     string   `json:"status"`
	Channel    string   `json:"channel"`
	AlertID    int64    `json:"alertID"`
	SendTime   []string `json:"sendTime"`
	OrgID      int64    `json:"orgID"`
	PageNo     int64    `json:"pageNo"`
	PageSize   int64    `json:"pageSize"`
	TimeOrder  bool     `json:"timeOrder"`
}

type NotifySourceData struct {
	Params SourceDataParam `json:"params"`
}

type SourceDataParam struct {
	Content string `json:"content"`
	Message string `json:"message"`
	Title   string `json:"title"`
}

type AlertIndexAttribute struct {
	AlertID   int64  `json:"alertId"`
	AlertName string `json:"alertName"`
	GroupID   int64  `json:"groupId"`
}
