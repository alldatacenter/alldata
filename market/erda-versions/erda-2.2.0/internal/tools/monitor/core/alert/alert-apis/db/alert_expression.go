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

// AlertExpressionDB .
type AlertExpressionDB struct {
	*gorm.DB
}

// AlertRuleCount .
type AlertRuleCount struct {
	AlertId uint64 `gorm:"alert_id"`
	Count   int64  `gorm:"count"`
}

// QueryByIDs .
func (db *AlertExpressionDB) QueryByIDs(ids []uint64) ([]*AlertExpression, error) {
	var expressions []*AlertExpression
	if err := db.
		Where("id IN (?)", ids).
		Find(&expressions).Error; err != nil {
		return nil, err
	}

	return expressions, nil
}

// QueryByAlertIDs .
func (db *AlertExpressionDB) QueryByAlertIDs(alertIDs []uint64) ([]*AlertExpression, error) {
	var expressions []*AlertExpression
	if err := db.Where("alert_id IN (?)", alertIDs).Find(&expressions).Error; err != nil {
		return nil, err
	}
	return expressions, nil
}

// Insert .
func (db *AlertExpressionDB) Insert(expression *AlertExpression) error {
	expression.Created = time.Now()
	expression.Updated = time.Now()
	return db.Create(&expression).Error
}

// Update .
func (db *AlertExpressionDB) Update(expression *AlertExpression) error {
	expression.Updated = time.Now()
	return db.Model(&expression).Updates(&expression).Error
}

// UpdateEnableByAlertID .
func (db *AlertExpressionDB) UpdateEnableByAlertID(alertID uint64, enable bool) error {
	return db.
		Model(&AlertExpression{}).
		Where("alert_id=?", alertID).
		Update("updated", time.Now()).
		Update("enable", enable).Error
}

// DeleteByAlertID .
func (db *AlertExpressionDB) DeleteByAlertID(alertID uint64) error {
	return db.Where("alert_id=?", alertID).Delete(AlertExpression{}).Error
}

// DeleteByIDs .
func (db *AlertExpressionDB) DeleteByIDs(ids []uint64) error {
	return db.Where("id IN (?)", ids).Delete(AlertExpression{}).Error
}

// GetAllAlertExpression
func (db *AlertExpressionDB) GetAllAlertExpression(pageNo, pageSize int64) ([]*AlertExpression, int64, error) {
	var expressions []*AlertExpression
	var count int64
	query := db.Model(&AlertExpression{}).Where("enable = ?", true)
	err := query.Count(&count).Error
	if err != nil {
		return nil, 0, err
	}
	err = db.Where("enable = ?", true).
		Offset((pageNo - 1) * pageSize).Limit(pageSize).
		Find(&expressions).Error
	if err != nil {
		return nil, 0, err
	}
	return expressions, count, nil
}

// QueryRuleCount .
func (db *AlertExpressionDB) QueryRuleCount(alertIds []uint64) (map[uint64]int64, error) {
	rulesCount := make([]*AlertRuleCount, 0)
	err := db.Model(&AlertExpression{}).Select("alert_id,count(*) as count").
		Where("alert_id in (?)", alertIds).
		Group("alert_id").
		Scan(&rulesCount).
		Error
	if err != nil {
		return nil, err
	}
	ruleCountMap := make(map[uint64]int64)
	for _, v := range rulesCount {
		ruleCountMap[v.AlertId] = v.Count
	}
	return ruleCountMap, nil
}
