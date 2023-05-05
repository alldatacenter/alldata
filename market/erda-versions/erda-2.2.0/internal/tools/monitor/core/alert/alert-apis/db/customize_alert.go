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

// CustomizeAlertDB .
type CustomizeAlertDB struct {
	*gorm.DB
}

// GetByID .
func (db *CustomizeAlertDB) GetByID(id uint64) (*CustomizeAlert, error) {
	var alert CustomizeAlert
	if err := db.Where("id=?", id).Find(&alert).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &alert, nil
}

// GetByScopeAndScopeIDAndName .
func (db *CustomizeAlertDB) GetByScopeAndScopeIDAndName(scope, scopeID, name string) (*CustomizeAlert, error) {
	var alert CustomizeAlert
	if err := db.
		Where("alert_scope=? AND alert_scope_id=? AND name=?", scope, scopeID, name).
		Find(&alert).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &alert, nil
}

// QueryByScopeAndScopeID .
func (db *CustomizeAlertDB) QueryByScopeAndScopeID(scope, scopeID string, pageNo, pageSize int, name string) ([]*CustomizeAlert, error) {
	var alerts []*CustomizeAlert
	query := db.Where("alert_scope=? AND alert_scope_id=?", scope, scopeID)
	if name != "" {
		query = query.Where("name like ?", "%"+name+"%")
	}
	if err := query.Order("update_time DESC").
		Offset((pageNo - 1) * pageSize).Limit(pageSize).
		Find(&alerts).Error; err != nil {
		return nil, err
	}
	return alerts, nil
}

// CountByScopeAndScopeID .
func (db *CustomizeAlertDB) CountByScopeAndScopeID(scope, scopeID, name string) (int, error) {
	var count int
	query := db.Table(TableCustomizeAlert).
		Where("alert_scope=?", scope).
		Where("alert_scope_id=?", scopeID)
	if name != "" {
		query = query.Where("name like ?", "%"+name+"%")
	}
	if err := query.Count(&count).Error; err != nil {
		return 0, err
	}
	return count, nil
}

// Insert .
func (db *CustomizeAlertDB) Insert(alert *CustomizeAlert) error {
	alert.CreateTime = time.Now()
	alert.UpdateTime = time.Now()
	return db.Create(&alert).Error
}

// Update .
func (db *CustomizeAlertDB) Update(alert *CustomizeAlert) error {
	alert.UpdateTime = time.Now()
	return db.Model(&alert).Update(&alert).Error
}

// UpdateEnable .
func (db *CustomizeAlertDB) UpdateEnable(id uint64, enable bool) error {
	return db.Model(&CustomizeAlert{}).
		Where("id=?", id).
		Update("update_time", time.Now()).
		Update("enable", enable).Error
}

// DeleteByID .
func (db *CustomizeAlertDB) DeleteByID(id uint64) error {
	return db.Delete(&CustomizeAlert{ID: id}).Error
}
