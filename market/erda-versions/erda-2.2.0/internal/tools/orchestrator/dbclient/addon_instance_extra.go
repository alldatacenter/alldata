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

// AddonInstanceExtra 存储addon实例额外信息
type AddonInstanceExtra struct {
	ID         string    `gorm:"type:varchar(64)"` // 唯一Id
	InstanceID string    `gorm:"type:varchar(64)"` // addon名称
	Field      string    `gorm:"type:varchar(32)"` // 属性名称
	Value      string    `gorm:"type:text"`        // 属性值
	Deleted    string    `gorm:"column:is_deleted"`
	CreatedAt  time.Time `gorm:"column:create_time"`
	UpdatedAt  time.Time `gorm:"column:update_time"`
}

// TableName 数据库表名
func (AddonInstanceExtra) TableName() string {
	return "tb_middle_instance_extra"
}

// CreateAddonInstanceExtra insert AddonExtra
func (db *DBClient) CreateAddonInstanceExtra(addonInstanceExtra *AddonInstanceExtra) error {
	return db.Create(addonInstanceExtra).Error
}

// UpdateAddonInstanceExtra update AddonInstanceExtra
func (db *DBClient) UpdateAddonInstanceExtra(addonInstanceExtra *AddonInstanceExtra) error {
	if err := db.
		Save(addonInstanceExtra).Error; err != nil {
		return errors.Wrapf(err, "failed to update addonInstanceExtra info, id: %v", addonInstanceExtra.ID)
	}
	return nil
}

// GetByInstanceIDAndField 根据addonName、field获取AddonExtra信息
func (db *DBClient) GetByInstanceIDAndField(instanceID, field string) (*AddonInstanceExtra, error) {
	var addonInstanceExtra AddonInstanceExtra
	if err := db.
		Where("instance_id = ?", instanceID).
		Where("field = ?", field).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		First(&addonInstanceExtra).Error; err != nil {
		return nil, errors.Wrapf(err, "failed to get addon addonInstanceExtra info, instanceID : %s, field : %s",
			instanceID, field)
	}
	return &addonInstanceExtra, nil
}

// GetByAddonName 根据addonName获取AddonExtra信息
func (db *DBClient) GetInstanceExtraByAddonName(addonName string) (*[]AddonExtra, error) {
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
