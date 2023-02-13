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

import "time"

// AddonAudit 第三方addon操作审计信息
type AddonAudit struct {
	ID        int64     `gorm:"primary_key"`        // 唯一Id
	OrgID     string    `gorm:"type:varchar(16)"`   // 企业ID
	ProjectID string    `gorm:"type:varchar(16)"`   // 项目ID
	Workspace string    `gorm:"type:varchar(16)"`   // 环境
	Operator  string    `gorm:"type:varchar(255)"`  // 操作人
	OpName    string    `gorm:"type:varchar(64)"`   // 操作类型
	AddonName string    `gorm:"type:varchar(128)"`  // 属性值
	InsID     string    `gorm:"type:varchar(64)"`   // 属性值
	InsName   string    `gorm:"type:varchar(128)"`  // 属性值
	Params    string    `gorm:"type:varchar(4096)"` // 属性值
	Deleted   string    `gorm:"column:is_deleted"`
	CreatedAt time.Time `gorm:"column:create_time"`
	UpdatedAt time.Time `gorm:"column:update_time"`
}

// TableName 数据库表名
func (AddonAudit) TableName() string {
	return "tb_addon_audit"
}

// CreateAddonAudit insert AddonAudit
func (db *DBClient) CreateAddonAudit(addonAudit AddonAudit) error {
	return db.Create(addonAudit).Error
}
