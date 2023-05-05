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
	"github.com/jinzhu/gorm"
)

// AlertRuleDB .
type AlertRuleDB struct {
	*gorm.DB
}

// DistinctAlertTypeByScope .
func (db *AlertRuleDB) DistinctAlertTypeByScope(scope string) ([]string, error) {
	rows, err := db.Table(TableAlertRules).
		Where("alert_scope=?", scope).
		Where("enable=?", true).
		Select("distinct(alert_type)").Rows()
	if err != nil {
		return nil, err
	}

	var alertTypes []string
	for rows.Next() {
		var alertType string
		if err := rows.Scan(&alertType); err != nil {
			return nil, err
		}
		alertTypes = append(alertTypes, alertType)
	}
	return alertTypes, nil
}

// QueryEnabledByScope .
func (db *AlertRuleDB) QueryEnabledByScope(scope string) ([]*AlertRule, error) {
	var rules []*AlertRule
	if err := db.
		Where("alert_scope=?", scope).
		Where("enable=?", true).
		Find(&rules).Error; err != nil {
		return nil, err
	}

	return rules, nil
}

// QueryEnabledByScopeAndIndices .
func (db *AlertRuleDB) QueryEnabledByScopeAndIndices(scope string, indices []string) ([]*AlertRule, error) {
	var rules []*AlertRule
	if err := db.
		Where("alert_scope=?", scope).
		Where("alert_index IN (?)", indices).
		Where("enable=?", true).
		Find(&rules).Error; err != nil {
		return nil, err
	}
	return rules, nil
}

// QueryByIndexes .
func (db *AlertRuleDB) QueryByIndexes(indexes []string) ([]*AlertRule, error) {
	var rules []*AlertRule
	if err := db.Where("alert_index IN (?)", indexes).Find(&rules).Error; err != nil {
		return nil, err
	}
	return rules, nil
}
