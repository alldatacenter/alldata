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

import (
	"time"

	"github.com/jinzhu/gorm"
)

const TableLogInstance = "sp_log_instance"

type LogInstance struct {
	ID              int       `gorm:"column:id;primary_key"`
	LogKey          string    `gorm:"column:log_key"`
	OrgId           string    `gorm:"column:org_id"`
	OrgName         string    `gorm:"column:org_name"`
	ClusterName     string    `gorm:"column:cluster_name"`
	ProjectId       string    `gorm:"column:project_id"`
	ProjectName     string    `gorm:"column:project_name"`
	Workspace       string    `gorm:"column:workspace"`
	ApplicationId   string    `gorm:"column:application_id"`
	ApplicationName string    `gorm:"column:application_name"`
	RuntimeId       string    `gorm:"column:runtime_id"`
	RuntimeName     string    `gorm:"column:runtime_name"`
	Config          string    `gorm:"column:config"`
	Version         string    `gorm:"column:version"`
	Plan            string    `gorm:"column:plan"`
	IsDelete        int       `gorm:"column:is_delete"`
	Created         time.Time `gorm:"column:created"`
	Updated         time.Time `gorm:"column:updated"`
	LogType         string    `gorm:"column:log_type;default:'log-analytics'"`
}

func (LogInstance) TableName() string {
	return TableLogInstance
}

type LogInstanceDB struct {
	*gorm.DB
}

func (db *LogInstanceDB) GetByLogKey(logKey string) (*LogInstance, error) {
	var instance LogInstance
	result := db.Table(TableLogInstance).
		Where("`log_key`=?", logKey).
		Where("`is_delete`=0").
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
