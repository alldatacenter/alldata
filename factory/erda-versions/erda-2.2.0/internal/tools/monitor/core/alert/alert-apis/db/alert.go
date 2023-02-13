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

// AlertDB .
type AlertDB struct {
	*gorm.DB
}

// GetByID .
func (db *AlertDB) GetByID(id uint64) (*Alert, error) {
	var alert Alert
	if err := db.Where("id=?", id).Find(&alert).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &alert, nil
}

// GetByScopeAndScopeIDAndName .
func (db *AlertDB) GetByScopeAndScopeIDAndName(scope, scopeID, name string) (*Alert, error) {
	var alert Alert
	if err := db.
		Where("alert_scope=?", scope).
		Where("alert_scope_id=?", scopeID).
		Where("name=?", name).
		Find(&alert).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &alert, nil
}

func (db *AlertDB) GetAllAvailableAlertIds() ([]uint64, error) {
	return db.GetAvailableAlertIdByScope("", "")
}

func (db *AlertDB) GetAvailableAlertIdByScope(scope, scopeID string) ([]uint64, error) {
	var alerts []*Alert
	query := db.Where("enable=?", true)
	if len(scope) > 0 {
		query = query.Where("scope=?", scope)
	}
	if len(scopeID) > 0 {
		query = query.Where("scope_id=?", scopeID)
	}
	err := query.Select("id").Find(&alerts).Error
	if err != nil && gorm.IsRecordNotFoundError(err) {
		return nil, nil
	}

	var ids []uint64
	for _, alert := range alerts {
		ids = append(ids, alert.ID)
	}
	return ids, err
}

// QueryByScopeAndScopeID .
func (db *AlertDB) QueryByScopeAndScopeID(scope, scopeID string, pageNo, pageSize uint64, name string) ([]*Alert, error) {
	var alerts []*Alert
	query := db.Where("alert_scope=?", scope).Where("alert_scope_id=?", scopeID)
	if name != "" {
		query = query.Where("name like ?", "%"+name+"%")
	}
	if err := query.Order("id DESC").
		Offset((pageNo - 1) * pageSize).Limit(pageSize).
		Find(&alerts).Error; err != nil {
		return nil, err
	}
	return alerts, nil
}

// CountByScopeAndScopeID .
func (db *AlertDB) CountByScopeAndScopeID(scope, scopeID, name string) (int, error) {
	var count int
	query := db.Table(TableAlert).Where("alert_scope=?", scope).Where("alert_scope_id=?", scopeID)
	if name != "" {
		query = query.Where("name like ?", "%"+name+"%")
	}
	if err := query.Count(&count).Error; err != nil {
		return 0, err
	}
	return count, nil
}

// Insert .
func (db *AlertDB) Insert(alert *Alert) error {
	alert.Created = time.Now()
	alert.Updated = time.Now()
	return db.Create(&alert).Error
}

// Update .
func (db *AlertDB) Update(alert *Alert) error {
	alert.Updated = time.Now()
	return db.Model(&alert).Update(&alert).Error
}

// UpdateEnable .
func (db *AlertDB) UpdateEnable(id uint64, enable bool) error {
	return db.Table(TableAlert).
		Where("id=?", id).
		Update("updated", time.Now()).
		Update("enable", enable).Error
}

// DeleteByID .
func (db *AlertDB) DeleteByID(id uint64) error {
	return db.Delete(&Alert{ID: id}).Error
}
