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

const IsDeleteValue = "Y"

// UpdateOrAddNamespace 更新/添加 namespace
func (client *DBClient) UpdateOrAddNamespace(namespace *model.ConfigNamespace) error {
	return client.Save(namespace).Error
}

// SoftDeleteNamespace 软删除 namespace
func (client *DBClient) SoftDeleteNamespace(namespace *model.ConfigNamespace) error {
	return client.Model(namespace).Update("is_deleted", IsDeleteValue).Error
}

// GetNamespaceByName 根据 name 获取 namespac
func (client *DBClient) GetNamespaceByName(name string) (*model.ConfigNamespace, error) {
	namespace := &model.ConfigNamespace{}
	if err := client.Where("name = ?", name).Where("is_deleted = ?", "N").
		First(namespace).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return namespace, nil
}

func (client *DBClient) ListNamespaceByName(names []string) ([]model.ConfigNamespace, error) {
	namespaces := []model.ConfigNamespace{}
	if err := client.Where("name in (?)", names).Where("is_deleted = ?", "N").
		Find(&namespaces).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return namespaces, nil
}

// GetNamespacesByNames 根据 多个names 获取 多个namespace
func (client *DBClient) GetNamespacesByNames(names []string) ([]model.ConfigNamespace, error) {
	var namespaces []model.ConfigNamespace
	if err := client.Where("name in (?)", names).Where("is_deleted = ?", "N").
		Find(&namespaces).Error; err != nil {
		return nil, err
	}
	return namespaces, nil
}

// UpdateOrAddNamespaceRelation 更新/添加命名空间关联关系
func (client *DBClient) UpdateOrAddNamespaceRelation(namespaceRelation *model.ConfigNamespaceRelation) error {
	return client.Save(namespaceRelation).Error
}

// SoftDeleteNamespace 软删除 namespace
func (client *DBClient) SoftDeleteNamespaceRelation(namespaceRelation *model.ConfigNamespaceRelation) error {
	return client.Model(namespaceRelation).Update("is_deleted", IsDeleteValue).Error
}

// GetNamespaceRelationByName 根据 name 获取 namespace 关联关系
func (client *DBClient) GetNamespaceRelationByName(name string) (*model.ConfigNamespaceRelation, error) {
	namespaceRelation := &model.ConfigNamespaceRelation{}
	if err := client.Where("namespace = ?", name).Where("is_deleted = ?", "N").
		First(namespaceRelation).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return namespaceRelation, nil
}

// GetNamespaceRelationByDefaultName 根据 defaultName 获取 namespace 关联关系
func (client *DBClient) GetNamespaceRelationsByDefaultName(name string) ([]model.ConfigNamespaceRelation, error) {
	var namespaceRelations []model.ConfigNamespaceRelation
	if err := client.Where("default_namespace = ?", name).Where("is_deleted = ?", "N").
		Find(&namespaceRelations).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return namespaceRelations, nil
}

// ListNamespaceByAppID 修复数据库老数据使用 name = app-2079
func (client *DBClient) ListNamespaceByAppID(name string) ([]model.ConfigNamespace, error) {
	namespaces := []model.ConfigNamespace{}
	if err := client.Where("name LIKE ?", name+"%").Find(&namespaces).Error; err != nil {
		return nil, err
	}

	return namespaces, nil
}
