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

type MonitorDb struct {
	*gorm.DB
}

func (db *MonitorDb) SelectProjectIdByTk(tk string) (string, error) {
	var monitor Monitor
	err := db.
		Select("*").
		Where("sp_monitor.terminus_key = ?", tk).
		Find(&monitor).
		Order("created", false).
		Limit(1).
		Error
	return monitor.ProjectId, err
}

func (db *MonitorDb) GetInstanceByTk(tk string) (Monitor, error) {
	var monitor Monitor
	err := db.Table("sp_monitor").
		Select("*").
		Where("terminus_key = ?", tk).
		Order("created DESC").
		Limit(1).
		Find(&monitor).
		Error
	return monitor, err
}

func (db *MonitorDb) GetTkByProjectIdAndWorkspace(projectId string, workspace string) (string, error) {
	var monitor Monitor
	err := db.Table("sp_monitor").
		Select("terminus_key").
		Where("project_id = ?", projectId).Where("workspace = ?", workspace).
		Find(&monitor).
		Order("id DESC").
		Limit(1).
		Error
	return monitor.TerminusKey, err
}
