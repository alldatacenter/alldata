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

package dao

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/apps/dop/model"
)

// UpdateOrAddEnvConfig 更新环境变量配置
func (client *DBClient) UpdateOrAddEnvConfig(config *model.ConfigItem) error {
	return client.Save(config).Error
}

// UpdateEnvConfig 更新环境变量配置
func (client *DBClient) SoftDeleteEnvConfig(config *model.ConfigItem) error {
	return client.Model(config).Update("is_deleted", IsDeleteValue).Error
}

// GetEnvConfigByID 根据 ID 获取 配置信息
func (client *DBClient) GetEnvConfigByID(configID int64) (*model.ConfigItem, error) {
	configItem := &model.ConfigItem{}
	if err := client.Where("id = ?", configID).Where("is_deleted = ?", "N").
		First(configItem).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return configItem, nil
}

// GetEnvConfigsByNamespaceID 根据 namespaceID 获取所有配置信息
func (client *DBClient) GetEnvConfigsByNamespaceID(namespaceID int64) ([]model.ConfigItem, error) {
	var configItems []model.ConfigItem
	if err := client.Where("namespace_id = ?", namespaceID).Where("is_deleted = ?", "N").
		Find(&configItems).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return configItems, nil
}

func (client *DBClient) ListEnvConfigsByNamespaceID(namespaceIDList []int64) ([]model.ConfigItem, error) {
	var configItems []model.ConfigItem
	if err := client.Where("namespace_id in (?)", namespaceIDList).Where("is_deleted = ?", "N").
		Find(&configItems).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return configItems, nil
}

// GetEnvConfigByKey 根据 namespaceID, key 获取某个配置信息
func (client *DBClient) GetEnvConfigByKey(namespaceID int64, key string) (*model.ConfigItem, error) {
	configItem := &model.ConfigItem{}
	if err := client.Where("namespace_id = ?", namespaceID).Where("is_deleted = ?", "N").
		Where("item_key = ?", key).Find(configItem).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return configItem, nil
}
