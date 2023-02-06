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
	"github.com/jinzhu/gorm"
)

// GetNamespaceByName 根据 name 获取 namespac
func (db *DBClient) GetNamespaceByName(name string) (*ConfigNamespace, error) {
	namespace := &ConfigNamespace{}
	if err := db.Where("name = ?", name).Where("is_deleted = ?", "N").
		First(namespace).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return namespace, nil
}

// GetEnvConfigsByNamespaceID 根据 namespaceID 获取所有配置信息
func (db *DBClient) GetEnvConfigsByNamespaceID(namespaceID int64) ([]ConfigItem, error) {
	var configItems []ConfigItem
	if err := db.Where("namespace_id = ?", namespaceID).Where("is_deleted = ?", "N").
		Find(&configItems).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return configItems, nil
}

// GetNamespaceRelationByName 根据 name 获取 namespace 关联关系
func (db *DBClient) GetNamespaceRelationByName(name string) (*ConfigNamespaceRelation, error) {
	namespaceRelation := &ConfigNamespaceRelation{}
	if err := db.Where("namespace = ?", name).Where("is_deleted = ?", "N").
		First(namespaceRelation).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return namespaceRelation, nil
}
