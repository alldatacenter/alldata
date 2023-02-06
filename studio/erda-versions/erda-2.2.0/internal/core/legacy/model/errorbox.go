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
)

// ErrorLog 错误日志模型
type ErrorLog struct {
	BaseModel
	ResourceType   apistructs.ErrorResourceType `gorm:"column:resource_type"`
	ResourceID     string                       `gorm:"column:resource_id"`
	Level          apistructs.ErrorLogLevel     `gorm:"column:level"`
	OccurrenceTime time.Time                    `gorm:"column:occurrence_time"`
	HumanLog       string                       `gorm:"column:human_log"`
	PrimevalLog    string                       `gorm:"column:primeval_log"`
	DedupID        string                       `gorm:"column:dedup_id"`
}

// TableName 设置模型对应数据库表名称
func (ErrorLog) TableName() string {
	return "dice_error_box"
}
