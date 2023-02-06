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

package apis

import (
	"database/sql/driver"
	"encoding/json"

	dicestructs "github.com/erda-project/erda/apistructs"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
)

type reportFrequency string

const (
	daily   reportFrequency = "daily"
	weekly  reportFrequency = "weekly"
	monthly reportFrequency = "monthly"
)

type reportTaskDTO struct {
	ID                     uint64                   `json:"id"`
	Name                   string                   `json:"name"`
	Scope                  string                   `json:"scope"`
	ScopeID                string                   `json:"scopeId"`
	Type                   reportFrequency          `json:"type"`
	DashboardId            string                   `json:"dashboardId"`
	DashboardBlockTemplate *block.DashboardBlockDTO `json:"dashboardBlockTemplate,omitempty"`
	Enable                 bool                     `json:"enable"`
	NotifyTarget           *notify                  `json:"notifyTarget"`
	CreatedAt              int64                    `json:"createdAt"`
	UpdatedAt              int64                    `json:"updatedAt"`
}

type notify struct {
	Type        string                   `json:"type"`
	GroupID     uint64                   `json:"groupId"`
	GroupType   string                   `json:"groupType"`
	NotifyGroup *dicestructs.NotifyGroup `json:"notifyGroup"`
}

type reportTypeResp struct {
	Types []reportType `json:"list"`
	Total int          `json:"total"`
}
type reportType struct {
	Name  string          `json:"name"`
	Value reportFrequency `json:"value"`
}
type reportTaskOnly struct {
	ID           uint64          `json:"id"`
	Name         string          `json:"name" `
	Scope        string          `json:"scope"`
	ScopeID      string          `json:"scopeId"`
	Type         reportFrequency `json:"type"`
	Enable       bool            `json:"enable"`
	NotifyTarget *notify         `json:"notifyTarget"`
	CreatedAt    int64           `json:"createdAt"`
	UpdatedAt    int64           `json:"updatedAt"`
}

type reportTaskUpdate struct {
	Name         *string `json:"name"`
	DashboardId  *string `json:"dashboardId"`
	NotifyTarget *notify `json:"notifyTarget"`
}

type reportTaskResp struct {
	ReportTasks []reportTaskDTO `json:"list"`
	Total       int             `json:"total"`
}

type reportHistoryDTO struct {
	ID             uint64                   `json:"id"`
	Scope          string                   `json:"scope"`
	ScopeID        string                   `json:"scopeId"`
	ReportTask     *reportTaskOnly          `json:"reportTask,omitempty"`
	DashboardBlock *block.DashboardBlockDTO `json:"dashboardBlock,omitempty"`
	Start          int64                    `json:"start"`
	End            int64                    `json:"end,omitempty"`
}

type reportHistoriesResp struct {
	ReportHistories []reportHistoryDTO `json:"list"`
	Total           int                `json:"total"`
}

// Scan .
func (ls *notify) Scan(value interface{}) error {
	if value == nil {
		*ls = notify{}
		return nil
	}
	t := notify{}
	if e := json.Unmarshal(value.([]byte), &t); e != nil {
		return e
	}
	*ls = t
	return nil
}

// Value .
func (ls *notify) Value() (driver.Value, error) {
	if ls == nil {
		return nil, nil
	}
	b, e := json.Marshal(*ls)
	return b, e
}
