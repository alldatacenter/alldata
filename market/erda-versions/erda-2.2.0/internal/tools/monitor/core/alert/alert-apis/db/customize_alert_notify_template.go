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

// CustomizeAlertNotifyTemplateDB .
type CustomizeAlertNotifyTemplateDB struct {
	*gorm.DB
}

// QueryByAlertIDs .
func (db *CustomizeAlertNotifyTemplateDB) QueryByAlertIDs(alertIDs []uint64) ([]*CustomizeAlertNotifyTemplate, error) {
	var rules []*CustomizeAlertNotifyTemplate
	if err := db.
		Where("customize_alert_id IN (?)", alertIDs).
		Find(&rules).Error; err != nil {
		return nil, err
	}

	return rules, nil
}

// QueryEnabledByTypesAndIndexes .
func (db *CustomizeAlertNotifyTemplateDB) QueryEnabledByTypesAndIndexes(
	types, indexes []string) ([]*CustomizeAlertNotifyTemplate, error) {
	var templates []*CustomizeAlertNotifyTemplate
	if err := db.
		Where("alert_type IN (?)", types).
		Where("alert_index IN (?)", indexes).
		Where("enable=?", true).
		Find(&templates).Error; err != nil {
		return nil, err
	}
	return templates, nil
}

// Insert .
func (db *CustomizeAlertNotifyTemplateDB) Insert(notify *CustomizeAlertNotifyTemplate) error {
	notify.CreateTime = time.Now()
	notify.UpdateTime = time.Now()
	return db.Create(&notify).Error
}

// Update .
func (db *CustomizeAlertNotifyTemplateDB) Update(notify *CustomizeAlertNotifyTemplate) error {
	notify.UpdateTime = time.Now()
	return db.Model(&notify).Update(&notify).Error
}

// UpdateEnableByAlertID .
func (db *CustomizeAlertNotifyTemplateDB) UpdateEnableByAlertID(alertID uint64, enable bool) error {
	return db.Model(&CustomizeAlertNotifyTemplate{}).
		Where("customize_alert_id=?", alertID).
		Update("update_time", time.Now()).
		Update("enable", enable).Error
}

// DeleteByAlertID .
func (db *CustomizeAlertNotifyTemplateDB) DeleteByAlertID(alertID uint64) error {
	return db.Where("customize_alert_id=?", alertID).Delete(CustomizeAlertNotifyTemplate{}).Error
}

// DeleteByIDs .
func (db *CustomizeAlertNotifyTemplateDB) DeleteByIDs(ids []uint64) error {
	return db.Where("id IN (?)", ids).Delete(CustomizeAlertNotifyTemplate{}).Error
}

func (db *CustomizeAlertNotifyTemplateDB) QueryCustomizeAlertTemplate() ([]*CustomizeAlertNotifyTemplate, error) {
	var templates []*CustomizeAlertNotifyTemplate
	err := db.Where("enable = ?", true).Order("create_time").Find(&templates).Error
	if err != nil {
		return nil, err
	}
	return templates, nil
}
