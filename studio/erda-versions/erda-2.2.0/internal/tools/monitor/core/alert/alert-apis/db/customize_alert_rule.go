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

// CustomizeAlertRuleDB .
type CustomizeAlertRuleDB struct {
	*gorm.DB
}

// QueryByAlertIDs .
func (db *CustomizeAlertRuleDB) QueryByAlertIDs(alertIDs []uint64) ([]*CustomizeAlertRule, error) {
	var rules []*CustomizeAlertRule
	if err := db.
		Where("customize_alert_id IN (?)", alertIDs).
		Find(&rules).Error; err != nil {
		return nil, err
	}
	return rules, nil
}

// QueryEnabledByScope .
func (db *CustomizeAlertRuleDB) QueryEnabledByScope(scope, scopeID string) ([]*CustomizeAlertRule, error) {
	var rules []*CustomizeAlertRule
	if err := db.
		Where("alert_scope=?", scope).
		Where("alert_scope_id=?", scopeID).
		Where("enable=?", true).
		Find(&rules).Error; err != nil {
		return nil, err
	}

	return rules, nil
}

// QueryEnabledByScopeAndIndices .
func (db *CustomizeAlertRuleDB) QueryEnabledByScopeAndIndices(
	scope, scopeID string, indices []string) ([]*CustomizeAlertRule, error) {
	var rules []*CustomizeAlertRule
	if err := db.
		Where("alert_scope=?", scope).
		Where("alert_scope_id=?", scopeID).
		Where("alert_index IN (?)", indices).
		Where("enable=?", true).
		Find(&rules).Error; err != nil {
		return nil, err
	}
	return rules, nil
}

// Insert .
func (db *CustomizeAlertRuleDB) Insert(rule *CustomizeAlertRule) error {
	rule.CreateTime = time.Now()
	rule.UpdateTime = time.Now()
	return db.Create(&rule).Error
}

// Update .
func (db *CustomizeAlertRuleDB) Update(rule *CustomizeAlertRule) error {
	rule.UpdateTime = time.Now()
	return db.Model(&rule).Update(&rule).Error
}

// UpdateEnableByAlertID .
func (db *CustomizeAlertRuleDB) UpdateEnableByAlertID(alertID uint64, enable bool) error {
	return db.Model(&CustomizeAlertRule{}).
		Where("customize_alert_id=?", alertID).
		Update("update_time", time.Now()).
		Update("enable", enable).Error
}

// DeleteByAlertID .
func (db *CustomizeAlertRuleDB) DeleteByAlertID(alertID uint64) error {
	return db.Where("customize_alert_id=?", alertID).Delete(CustomizeAlertRule{}).Error
}

// DeleteByIDs .
func (db *CustomizeAlertRuleDB) DeleteByIDs(ids []uint64) error {
	return db.Where("id IN (?)", ids).Delete(CustomizeAlertRule{}).Error
}
