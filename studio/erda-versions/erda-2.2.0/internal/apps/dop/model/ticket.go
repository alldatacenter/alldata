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

import (
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// Ticket 工单数据结构
type Ticket struct {
	dbengine.BaseModel

	Title        string
	Content      string                `json:"content" gorm:"type:text"`
	Type         apistructs.TicketType `gorm:"type:varchar(20);index:idx_type"`
	Priority     apistructs.TicketPriority
	Status       apistructs.TicketStatus
	RequestID    string `gorm:"type:varchar(60);index:idx_request_id"`
	Key          string `gorm:"type:varchar(64);index:idx_key"` // 告警使用，作为唯一 key
	OrgID        string
	Metric       string
	MetricID     string
	Count        int64 // 相同类型工单聚合
	Creator      string
	LastOperator string
	Label        string                  `json:"label" gorm:"type:text"`
	TargetType   apistructs.TicketTarget `gorm:"type:varchar(40);index:idx_target_type"`
	TargetID     string
	TriggeredAt  *time.Time
	ClosedAt     *time.Time
}

// TableName 设置模型对应数据库表名称
func (Ticket) TableName() string {
	return "ps_tickets"
}
