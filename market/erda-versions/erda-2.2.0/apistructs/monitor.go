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

// GetMonitorResponse .
type GetMonitorAlertResponse struct {
	Header
	Data *Alert `json:"data"`
}

// Alert 告警结构体
type Alert struct {
	ID           uint64                 `json:"id"`
	Name         string                 `json:"name"`
	AlertScope   string                 `json:"alertScope"`
	AlertScopeID string                 `json:"alertScopeId"`
	Enable       bool                   `json:"enable"`
	Attributes   map[string]interface{} `json:"attributes"`
	CreateTime   int64                  `json:"createTime"`
	UpdateTime   int64                  `json:"updateTime"`
}

// GetMonitorReportTaskResponse .
type GetMonitorReportTaskResponse struct {
	Header
	Data *ReportTask `json:"data"`
}

// ReportTask
type ReportTask struct {
	ID          uint64 `json:"id"`
	Name        string `json:"name"`
	Scope       string `json:"scope"`
	ScopeID     string `json:"scopeId"`
	DashboardId string `json:"dashboardId"`
	Enable      bool   `json:"enable"`
	CreatedAt   int64  `json:"createdAt"`
	UpdatedAt   int64  `json:"updatedAt"`
}

// MonitorConfig .
type MonitorConfig struct {
	Scope     string `json:"scope"`
	ScopeId   string `json:"scope_id"`
	Namespace string `json:"namespace"`
	Type      string `json:"type"`
	Names     string `json:"names"`
	Filters   string `json:"filters"`
	Enable    bool   `json:"enable"`
}
