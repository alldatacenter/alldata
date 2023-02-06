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

package db

import "github.com/jinzhu/gorm"

type LogInstanceDB struct {
	*gorm.DB
}

func (db *LogInstanceDB) GetLatestByLogKey(logKey string, logType LogType) (*LogInstance, error) {
	var instance LogInstance
	query := db.Table(TableLogInstance).
		Where("log_key=?", logKey).
		Where("is_delete=0")

	if len(logType) > 0 {
		query = query.Where("`log_type`=?", logType)
	}

	result := query.
		Order("id DESC").
		Limit(1).
		Find(&instance)

	if result.RecordNotFound() {
		return nil, nil
	}

	if result.Error != nil {
		return nil, result.Error
	}

	return &instance, nil
}

func (db *LogInstanceDB) GetListByClusterAndProjectIdAndWorkspace(clusterName, projectId, workspace string) ([]LogInstance, error) {
	var list []LogInstance
	result := db.Table(TableLogInstance).
		Where("`cluster_name`=?", clusterName).
		Where("`project_id`=?", projectId).
		Where("`workspace`=?", workspace).
		Where("`is_delete`=0").
		Find(&list)

	if result.Error != nil {
		return nil, result.Error
	}

	return list, nil
}
