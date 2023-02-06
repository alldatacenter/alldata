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

	"github.com/erda-project/erda/internal/core/legacy/model"
)

// CreateFavoritedResource 创建收藏关系
func (client *DBClient) CreateFavoritedResource(resource *model.FavoritedResource) error {
	return client.Create(resource).Error
}

// GetFavoritedResource 查询收藏关系
func (client *DBClient) GetFavoritedResource(target string, targetID uint64, userID string) (*model.FavoritedResource, error) {
	var resource model.FavoritedResource
	if err := client.Where("target = ?", target).
		Where("target_id = ?", targetID).
		Where("user_id = ?", userID).Find(&resource).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &resource, nil
}

// GetFavoritedResourcesByUser 查询收藏关系
func (client *DBClient) GetFavoritedResourcesByUser(target, userID string) ([]model.FavoritedResource, error) {
	var resources []model.FavoritedResource
	if err := client.Where("target = ?", target).
		Where("user_id = ?", userID).Find(&resources).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return resources, nil
}

// DeleteFavoritedResource 删除收藏关系
func (client *DBClient) DeleteFavoritedResource(id uint64) error {
	return client.Where("id = ?", id).Delete(&model.FavoritedResource{}).Error
}

// DeleteFavoritedResourcesByTarget 根据 target 删除收藏关系
func (client *DBClient) DeleteFavoritedResourcesByTarget(target string, targetID uint64) error {
	return client.Where("target = ?", target).Where("target_id = ?", targetID).Delete(&model.FavoritedResource{}).Error
}
