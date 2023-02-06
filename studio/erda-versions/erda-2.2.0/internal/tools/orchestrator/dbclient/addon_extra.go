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

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
)

// AddonExtra 存储addon额外信息
type AddonExtra struct {
	ID        string    `gorm:"type:varchar(64)"` // 唯一Id
	AddonName string    `gorm:"type:varchar(64)"` // addon名称
	Field     string    `gorm:"type:varchar(64)"` // 属性名称
	Value     string    `gorm:"type:text"`        // 属性值
	Deleted   string    `gorm:"column:is_deleted"`
	CreatedAt time.Time `gorm:"column:create_time"`
	UpdatedAt time.Time `gorm:"column:update_time"`
}

// TableName 数据库表名
func (AddonExtra) TableName() string {
	return "tb_addon_extra"
}

// CreateAddonExtra insert AddonExtra
func (db *DBClient) CreateAddonExtra(addonExtra *AddonExtra) error {
	return db.Create(addonExtra).Error
}

// UpdateAddonExtra update AddonExtra
func (db *DBClient) UpdateAddonExtra(addonExtra *AddonExtra) error {
	if err := db.
		Save(addonExtra).Error; err != nil {
		return errors.Wrapf(err, "failed to update addonExtra info, id: %v", addonExtra.ID)
	}
	return nil
}

// GetByAddonNameAndField 根据addonName、field获取AddonExtra信息
func (db *DBClient) GetByAddonNameAndField(addonName, field string) (*AddonExtra, error) {
	var addonExtra AddonExtra
	if err := db.
		Where("addon_name = ?", addonName).
		Where("field = ?", field).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		First(&addonExtra).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon addonExtras info, addon_name : %s, field : %s",
			addonName, field)
	}
	return &addonExtra, nil
}

// GetByAddonName 根据addonName获取AddonExtra信息
func (db *DBClient) GetExtraByAddonName(addonName string) (*[]AddonExtra, error) {
	var addonExtras []AddonExtra
	if err := db.
		Where("addon_name = ?", addonName).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addonExtras).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon addonExtras info, addon_name : %s",
			addonName)
	}
	return &addonExtras, nil
}
