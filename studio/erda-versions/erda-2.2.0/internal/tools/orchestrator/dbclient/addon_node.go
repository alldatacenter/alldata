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

	"github.com/erda-project/erda/apistructs"
)

// AddonNode addon node信息
type AddonNode struct {
	ID         string `gorm:"type:varchar(64)"`
	InstanceID string `gorm:"type:varchar(64)"` // AddonInstance 主键
	Namespace  string `gorm:"type:text"`
	NodeName   string
	CPU        float64
	Mem        uint64
	Deleted    string    `gorm:"column:is_deleted"` // Y: 已删除 N: 未删除
	CreatedAt  time.Time `gorm:"column:create_time"`
	UpdatedAt  time.Time `gorm:"column:update_time"`
}

// TableName 数据库表名
func (AddonNode) TableName() string {
	return "tb_middle_node"
}

// CreateAddonNode insert addonNode
func (db *DBClient) CreateAddonNode(addonNode *AddonNode) error {
	return db.Create(addonNode).Error
}

// GetAddonNodesByInstanceID 根据instanceID获取addonNode信息
func (db *DBClient) GetAddonNodesByInstanceID(instanceID string) (*[]AddonNode, error) {
	// 获取匹配搜索结果总量
	var addonNodes []AddonNode
	if err := db.
		Where("instance_id = ?", instanceID).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addonNodes).Error; err != nil {
		return nil, err
	}
	return &addonNodes, nil
}

// GetAddonNodesByInstanceIDs 根据instanceID列表获取addonNode信息
func (db *DBClient) GetAddonNodesByInstanceIDs(instanceIDs []string) (*[]AddonNode, error) {
	// 获取匹配搜索结果总量
	var addonNodes []AddonNode
	if err := db.
		Where("instance_id in (?)", instanceIDs).
		Where("is_deleted = ?", apistructs.AddonNotDeleted).
		Find(&addonNodes).Error; err != nil {
		return nil, err
	}
	return &addonNodes, nil
}
