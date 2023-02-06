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

// AlertNotifyDB .
type AlertNotifyDB struct {
	*gorm.DB
}

// QueryByAlertIDs .
func (db *AlertNotifyDB) QueryByAlertIDs(alertIDs []uint64) ([]*AlertNotify, error) {
	var notifies []*AlertNotify
	if err := db.Where("alert_id IN (?)", alertIDs).Find(&notifies).Error; err != nil {
		return nil, err
	}
	return notifies, nil
}

// Insert .
func (db *AlertNotifyDB) Insert(notify *AlertNotify) error {
	notify.Created = time.Now()
	notify.Updated = time.Now()
	return db.Create(&notify).Error
}

// Update .
func (db *AlertNotifyDB) Update(notify *AlertNotify) error {
	notify.Updated = time.Now()
	return db.Model(&notify).Updates(&notify).Error
}

// UpdateEnableByAlertID .
func (db *AlertNotifyDB) UpdateEnableByAlertID(alertID uint64, enable bool) error {
	return db.
		Model(&AlertNotify{}).
		Where("alert_id=?", alertID).
		Update("updated", time.Now()).
		Update("enable", enable).Error
}

// DeleteByAlertID .
func (db *AlertNotifyDB) DeleteByAlertID(alertID uint64) error {
	return db.Where("alert_id=?", alertID).Delete(AlertNotify{}).Error
}

// DeleteByIDs .
func (db *AlertNotifyDB) DeleteByIDs(ids []uint64) error {
	return db.Where("id IN (?)", ids).Delete(AlertNotify{}).Error
}

func (db *AlertNotifyDB) QueryAlertNotify(pageNo, pageSize int64) ([]*AlertNotify, int64, error) {
	var alertNotifies []*AlertNotify
	var count int64
	query := db.Model(&AlertNotify{}).Where("enable = ?", true)
	err := query.Count(&count).Error
	if err != nil {
		return nil, 0, err
	}
	err = query.Offset((pageNo - 1) * pageSize).Limit(pageSize).
		Find(&alertNotifies).Error
	if err != nil {
		return nil, 0, err
	}
	return alertNotifies, count, nil
}
