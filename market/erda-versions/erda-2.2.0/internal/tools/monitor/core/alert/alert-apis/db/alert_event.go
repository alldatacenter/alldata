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
	"bytes"
	"reflect"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/database/gormutil"
)

// AlertEventDB .
type AlertEventDB struct {
	*gorm.DB
}

type AlertEventQueryCondition struct {
	Name                 string
	Ids                  []string
	AlertLevels          []string
	AlertIds             []uint64
	AlertStates          []string
	SuppressedIds        []string
	AlertSources         []string
	LastTriggerTimeMsMin uint64
	LastTriggerTimeMsMax uint64
}

type AlertEventSort struct {
	SortField  string
	Descending bool
}

type AlertEventScopeCountResult struct {
	OrgID   int64
	Scope   string
	ScopeId string
	AlertId uint64
	Count   int64
}

var columnNameMap = gormutil.GetFieldToColumnMap(reflect.TypeOf(AlertEvent{}))

func (db *AlertEventDB) CreateAlertEvent(data *AlertEvent) error {
	return db.Create(data).Error
}

func (db *AlertEventDB) UpdateAlertEvent(id string, fields map[string]interface{}) error {
	return db.Table(TableAlertEvent).Where("id=?", id).Updates(fields).Error
}

func (db *AlertEventDB) GetById(id string) (*AlertEvent, error) {
	var record AlertEvent
	err := db.Where("id=?", id).Find(&record).Error
	if err == nil {
		return &record, nil
	}
	if gorm.IsRecordNotFoundError(err) {
		return nil, nil
	}
	return nil, err
}

// GetByAlertGroupID .
func (db *AlertEventDB) GetByAlertGroupID(groupID string) (*AlertEvent, error) {
	var record AlertEvent
	if err := db.Where("alert_group_id=?", groupID).Find(&record).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &record, nil
}

// QueryByCondition .
func (db *AlertEventDB) QueryByCondition(scope, scopeId string, condition *AlertEventQueryCondition, sorts []*AlertEventSort, pageNo, pageSize int64) ([]*AlertEvent, error) {
	var result []*AlertEvent
	query := db.Table(TableAlertEvent).Where("scope=?", scope).Where("scope_id=?", scopeId)
	query = db.buildWhereQuery(query, condition)
	query = db.buildSortSqlPart(query, sorts)
	query = query.Offset((pageNo - 1) * pageSize).Limit(pageSize)
	err := query.Find(&result).Error
	if err != nil && !gorm.IsRecordNotFoundError(err) {
		return nil, err
	}
	return result, nil
}

// CountByCondition .
func (db *AlertEventDB) CountByCondition(scope, scopeId string, condition *AlertEventQueryCondition) (int64, error) {
	var count int64
	query := db.Table(TableAlertEvent).Where("scope=?", scope).Where("scope_id=?", scopeId)
	query = db.buildWhereQuery(query, condition)
	err := query.Count(&count).Error
	return count, err
}

func (db *AlertEventDB) CountUnRecoverEventsGroupByScope() ([]*AlertEventScopeCountResult, error) {
	var list []*AlertEventScopeCountResult

	err := db.Table(TableAlertEvent).
		Select("org_id, scope, scope_id, alert_id, count(*) as count").
		Where("alert_state=?", "alert").
		Group("org_id,scope,scope_id,alert_id").
		Scan(&list).Error

	if err != nil && gorm.IsRecordNotFoundError(err) {
		return nil, nil
	}
	return list, err
}

func (db *AlertEventDB) CountUnRecoverEvents(scope, scopeId string, inAlertIds []uint64) (int64, error) {
	var result AlertEventScopeCountResult

	query := db.Table(TableAlertEvent).
		Select("count(*) as count").
		Where("alert_state=?", "alert").
		Where("scope=?", scope).
		Where("scope_id?", scopeId)

	if len(inAlertIds) > 0 {
		query = query.Where("alert_id in (?)", inAlertIds)
	}

	err := query.Find(&result).Error

	if err != nil && gorm.IsRecordNotFoundError(err) {
		return 0, nil
	}
	return result.Count, err
}

func (db *AlertEventDB) buildSortSqlPart(query *gorm.DB, sorts []*AlertEventSort) *gorm.DB {
	if len(sorts) == 0 {
		return query
	}
	var buff bytes.Buffer
	for i, sort := range sorts {
		buff.WriteString(columnNameMap[sort.SortField])
		if sort.Descending {
			buff.WriteString(" DESC")
		}
		if i+1 == len(sorts) {
			break
		}
		buff.WriteString(", ")
	}

	return query.Order(buff.String())
}

func (db *AlertEventDB) buildWhereQuery(query *gorm.DB, condition *AlertEventQueryCondition) *gorm.DB {
	if len(condition.Name) > 0 {
		query = query.Where("name like ?", "%"+condition.Name+"%")
	}
	if len(condition.Ids) > 0 {
		query = query.Where("id in (?)", condition.Ids)
	}
	if len(condition.AlertIds) > 0 {
		query = query.Where("alert_id in (?)", condition.AlertIds)
	}
	if len(condition.AlertLevels) > 0 {
		query = query.Where("alert_level in (?)", condition.AlertLevels)
	}
	if len(condition.AlertStates) > 0 && len(condition.SuppressedIds) > 0 {
		query = query.Where("alert_state in (?) or id in (?)", condition.AlertStates, condition.SuppressedIds)
	} else if len(condition.AlertStates) > 0 {
		query = query.Where("alert_state in (?)", condition.AlertStates)
	} else if len(condition.SuppressedIds) > 0 {
		query = query.Where("id in (?)", condition.SuppressedIds)
	}
	if len(condition.AlertSources) > 0 {
		query = query.Where("alert_source in (?)", condition.AlertSources)
	}
	if condition.LastTriggerTimeMsMin > 0 {
		query = query.Where("last_trigger_time >= ?", time.Unix(int64(condition.LastTriggerTimeMsMin)/1e3, int64(condition.LastTriggerTimeMsMin)%1e3))
	}
	if condition.LastTriggerTimeMsMax > 0 {
		query = query.Where("last_trigger_time < ?", time.Unix(int64(condition.LastTriggerTimeMsMax)/1e3, int64(condition.LastTriggerTimeMsMax)%1e3))
	}
	return query
}
