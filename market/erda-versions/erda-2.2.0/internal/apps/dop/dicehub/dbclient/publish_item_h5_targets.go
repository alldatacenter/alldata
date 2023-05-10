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
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

// PublishItemH5Targets H5的目标版本记录表
type PublishItemH5Targets struct {
	dbengine.BaseModel
	H5VersionID      uint64 `gorm:"column:h5_version_id"`
	TargetVersion    string `gorm:"column:target_version"`
	TargetBuildID    string `gorm:"column:target_build_id"`
	TargetMobileType string `gorm:"column:target_mobile_type"`
}

// TableName 设置模型对应数据库表名称
func (PublishItemH5Targets) TableName() string {
	return "dice_publish_item_h5_targets"
}

// CreateH5Targets 创建h5目标app版本关系
func (client *DBClient) CreateH5Targets(target *PublishItemH5Targets) error {
	return client.Create(target).Error
}

// GetH5VersionsByTarget 根据移动应用版本获取对应的H5版本
func (client *DBClient) GetH5VersionsByTarget(itemID uint64, mobileType apistructs.ResourceType, appVersion, packageName string) ([]*PublishItemVersion, error) {
	var h5versions []*PublishItemVersion
	if err := client.Table("dice_publish_item_versions").Joins("inner join dice_publish_item_h5_targets on dice_publish_item_versions.id = dice_publish_item_h5_targets.h5_version_id").
		Where("dice_publish_item_h5_targets.target_version = ?", appVersion).
		Where("dice_publish_item_h5_targets.target_mobile_type = ?", mobileType).
		Where("dice_publish_item_versions.publish_item_id = ?", itemID).
		Where("dice_publish_item_versions.package_name = ?", packageName).
		Scan(&h5versions).Error; err != nil {
		return nil, err
	}

	return h5versions, nil
}

// GetTargetsByH5Version 返回H5包版本的目标版本信息
func (client *DBClient) GetTargetsByH5Version(versionID uint64) ([]PublishItemH5Targets, error) {
	var targetRelations []PublishItemH5Targets
	if err := client.Table("dice_publish_item_h5_targets").Where("h5_version_id = ?", versionID).
		Find(&targetRelations).Error; err != nil {
		return nil, err
	}

	return targetRelations, nil
}
