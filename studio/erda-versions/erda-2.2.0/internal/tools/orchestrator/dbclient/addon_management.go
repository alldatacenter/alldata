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

package dbclient

import (
	"time"
)

// relay on the ops modules（ops/dbclient/addon_management.go）
// remove the dependency when ops complete the migration

// addon management
type AddonManagement struct {
	ID          uint64 `gorm:"primary_key"`
	AddonID     string `gorm:"type:varchar(64)"` // 主键
	Name        string `gorm:"type:varchar(64)"`
	ProjectID   string
	OrgID       string
	AddonConfig string `gorm:"type:text"`
	CPU         float64
	Mem         uint64
	Nodes       int
	CreateTime  time.Time `gorm:"column:create_time"`
	UpdateTime  time.Time `gorm:"column:update_time"`
}

func (AddonManagement) TableName() string {
	return "tb_addon_management"
}
