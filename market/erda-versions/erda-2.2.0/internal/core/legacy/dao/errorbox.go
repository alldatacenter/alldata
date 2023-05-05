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
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

// CreateErrorLog 创建错误日志
func (client *DBClient) CreateErrorLog(errorLog *model.ErrorLog) error {
	return client.Create(errorLog).Error
}

// GetErrorLogByDedupIndex 根据唯一索引获取错误日志记录
func (client *DBClient) GetErrorLogByDedupIndex(resourceID string, resourceType apistructs.ErrorResourceType,
	dedupID string) (*model.ErrorLog, error) {
	var errorLog model.ErrorLog
	if err := client.Table("dice_error_box").Where("resource_type = ?", resourceType).Where("resource_id = ?", resourceID).
		Where("dedup_id = ?", dedupID).First(&errorLog).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return &errorLog, nil
}

// UpdateErrorLog 更新错误日志记录
func (client *DBClient) UpdateErrorLog(errorLog *model.ErrorLog) error {
	return client.Table("dice_error_box").Save(errorLog).Error
}

// ListErrorLogByResources 根据多个resource信息查看错误日志
func (client *DBClient) ListErrorLogByResources(resourceTypes []apistructs.ErrorResourceType, resourceIDs []string) ([]model.ErrorLog, error) {
	var errorLogs []model.ErrorLog
	db := client.Where("resource_type in (?)", resourceTypes).Where("resource_id in (?)", resourceIDs)

	if err := db.Order("occurrence_time DESC").Find(&errorLogs).Error; err != nil {
		return nil, err
	}

	return errorLogs, nil
}

// ListErrorLogByResourcesAndStartTime 根据多个resource信息和开始时间查看错误日志
func (client *DBClient) ListErrorLogByResourcesAndStartTime(resourceTypes []apistructs.ErrorResourceType,
	resourceIDs []string, startTime time.Time) ([]model.ErrorLog, error) {
	var errorLogs []model.ErrorLog
	db := client.Where("resource_type in (?)", resourceTypes).Where("resource_id in (?)", resourceIDs).
		Where("occurrence_time > ?", startTime)

	if err := db.Order("occurrence_time DESC").Find(&errorLogs).Error; err != nil {
		return nil, err
	}

	return errorLogs, nil
}
