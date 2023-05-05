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
	"github.com/erda-project/erda/internal/core/legacy/model"
)

// GetActivitiesByRuntime 根据runtimeID获取活动列表
func (client *DBClient) GetActivitiesByRuntime(runtimeID int64, pageNo, pageSize int) (int, []model.Activity, error) {
	var (
		total      int
		activities []model.Activity
	)
	if err := client.Where("type = ?", "R").
		Where("runtime_id = ?", runtimeID).
		Order("created_at DESC").
		Offset((pageNo - 1) * pageSize).Limit(pageSize).Find(&activities).Error; err != nil {
		return 0, nil, err
	}

	// 获取总量
	if err := client.Model(&model.Activity{}).
		Where("type = ?", "R").
		Where("runtime_id = ?", runtimeID).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, activities, nil
}
