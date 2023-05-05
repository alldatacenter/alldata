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

package monitor

import (
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/database/gormutil"
)

// MoniroeDB .
type MonitorDB struct {
	*gorm.DB
}

func (db *MonitorDB) query() *gorm.DB {
	return db.Table(TableMonitor).Where("is_delete=0")
}

func (db *MonitorDB) GetByFields(fields map[string]interface{}) (*Monitor, error) {
	query := db.query()
	query, err := gormutil.GetQueryFilterByFields(query, monitorFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*Monitor
	if err := query.Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}

// GetByTerminusKey .
func (db *MonitorDB) GetByTerminusKey(terminusKey string) (*Monitor, error) {
	var monitor Monitor
	result := db.query().
		Where("`terminus_key`=?", terminusKey).
		Limit(1).
		Last(&monitor)
	if result.RecordNotFound() {
		return nil, nil
	}
	return &monitor, result.Error
}

// CompatibleTerminusKey .
type CompatibleTerminusKey struct {
	TerminusKey        string `gorm:"column:terminus_key"`
	TerminusKeyRuntime string `gorm:"column:terminus_key_runtime"`
}

func (db *MonitorDB) GetMonitorByProjectId(projectID int64) ([]*Monitor, error) {
	var monitors []*Monitor
	err := db.Where("`project_id` = ?", projectID).Where("`is_delete` = ?", 0).Find(&monitors).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return monitors, nil
}

func (db *MonitorDB) GetMonitorByProjectIdAndWorkspace(projectID int64, workspace string) (*Monitor, error) {
	monitor := Monitor{}
	err := db.Where("`project_id` = ?", projectID).Where("`workspace` = ?", workspace).Where("`is_delete` = ?", 0).Find(&monitor).Error
	if err == gorm.ErrRecordNotFound {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return &monitor, nil
}

func (db *MonitorDB) ListCompatibleTKs() ([]*CompatibleTerminusKey, error) {
	var list []*CompatibleTerminusKey
	if err := db.Raw("SELECT terminus_key,terminus_key_runtime FROM sp_monitor WHERE terminus_key_runtime is not null AND is_delete = 0").
		Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

func (db *MonitorDB) UpdateStatusByMonitorId(monitorId string, delete int) error {
	return db.Table(TableMonitor).
		Where("`monitor_id`=?", monitorId).
		Update(map[string]interface{}{
			"is_delete": delete,
			"updated":   time.Now(),
		}).Error
}
