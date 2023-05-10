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
	"fmt"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/database/gormutil"
)

// SystemViewDB .
type SystemViewDB struct {
	*gorm.DB
}

func (db *SystemViewDB) query() *gorm.DB      { return db.Table(TableSystemView) }
func (db *SystemViewDB) Begin() *SystemViewDB { return &SystemViewDB{DB: db.DB.Begin()} }

func (db *SystemViewDB) GetByFields(fields map[string]interface{}) (*SystemView, error) {
	query, err := gormutil.GetQueryFilterByFields(db.query(), systemViewFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*SystemView
	if err := query.Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}

func (db *SystemViewDB) ListByFields(fields map[string]interface{}) ([]*SystemView, error) {
	query, err := gormutil.GetQueryFilterByFields(db.query(), systemViewFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*SystemView
	if err := query.Order("created_at DESC").Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

// CustomViewDB .
type CustomViewDB struct {
	*gorm.DB
}

func (db *CustomViewDB) query() *gorm.DB      { return db.Table(TableCustomView) }
func (db *CustomViewDB) Begin() *CustomViewDB { return &CustomViewDB{DB: db.DB.Begin()} }

func (db *CustomViewDB) GetByFields(fields map[string]interface{}) (*CustomView, error) {
	query, err := gormutil.GetQueryFilterByFields(db.query(), customViewFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*CustomView
	if err := query.Limit(1).Find(&list).Error; err != nil {
		return nil, err
	}
	if len(list) <= 0 {
		return nil, nil
	}
	return list[0], nil
}

func (db *CustomViewDB) GetCreatorsByFields(fields map[string]interface{}) ([]string, error) {
	query := db.Select("distinct(`creator_id`)")
	query, err := gormutil.GetQueryFilterByFields(query, customViewFieldColumns, fields)
	if err != nil {
		return nil, err
	}
	var list []*CustomView
	if err := query.Where("creator_id != ?", "").Order("created_at DESC").Find(&list).Error; err != nil {
		return nil, err
	}
	var result []string
	for _, view := range list {
		result = append(result, view.CreatorID)
	}
	return result, nil
}

func (db *CustomViewDB) ListByFieldsAndPage(pageNo, pageSize int64, startTime, endTime int64, creatorId []string, fields map[string]interface{}, likeFields map[string]interface{}) ([]*CustomView, int64, error) {
	query, err := gormutil.GetQueryFilterByFields(db.query(), customViewFieldColumns, fields)
	if err != nil {
		return nil, 0, err
	}

	query, err = gormutil.GetQueryLikeFilterByFields(query, customViewFieldColumns, likeFields)
	if err != nil {
		return nil, 0, err
	}
	startDuration := time.Unix(0, startTime*1e6)
	start := startDuration.Format("2006-01-02 15:04:05")
	endDuration := time.Unix(0, endTime*1e6)
	end := endDuration.Format("2006-01-02 15:04:05")
	if startTime != 0 {
		query = query.Where("created_at >= ?", start)
	}
	if endTime != 0 {
		query = query.Where("created_at <= ?", end)
	}
	if len(creatorId) > 0 {
		query = query.Where(`creator_id in (?)`, creatorId)
	}
	var (
		list  []*CustomView
		total int64
	)
	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	if err := query.Order("created_at DESC").Limit(pageSize).Offset((pageNo - 1) * pageSize).Find(&list).Error; err != nil {
		return nil, 0, err
	}
	return list, total, nil
}

func (db *CustomViewDB) ListByFields(startTime, endTime int64, creatorId []string, fields map[string]interface{}, likeFields map[string]interface{}) ([]*CustomView, error) {
	query, err := gormutil.GetQueryFilterByFields(db.query(), customViewFieldColumns, fields)
	if err != nil {
		return nil, err
	}

	query, err = gormutil.GetQueryLikeFilterByFields(query, customViewFieldColumns, likeFields)
	if err != nil {
		return nil, err
	}
	startDuration := time.Unix(0, startTime*1e6)
	start := startDuration.Format("2006-01-02 15:04:05")
	endDuration := time.Unix(0, endTime*1e6)
	end := endDuration.Format("2006-01-02 15:04:05")
	if startTime != 0 {
		query = query.Where("created_at >= ?", start)
	}
	if endTime != 0 {
		query = query.Where("created_at <= ?", end)
	}
	if len(creatorId) > 0 {
		query = query.Where(`creator_id in (?)`, creatorId)
	}

	var list []*CustomView
	if err := query.Order("created_at DESC").Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

func (db *CustomViewDB) ListByIds(ids []string) ([]*CustomView, error) {
	query := db.query().Where("id in (?)", ids)

	var list []*CustomView
	if err := query.Order("created_at DESC").Find(&list).Error; err != nil {
		return nil, err
	}
	return list, nil
}

func (db *CustomViewDB) UpdateView(id string, fields map[string]interface{}) error {
	updates := make(map[string]interface{})
	for name, value := range fields {
		col, ok := customViewFieldColumns[name]
		if !ok {
			return fmt.Errorf("unknown %q", name)
		}
		updates[col] = value
	}
	return db.query().Where("id=?", id).Updates(updates).Error
}

// ErdaDashboardHistoryDB .
type ErdaDashboardHistoryDB struct {
	*gorm.DB
}

func (db *ErdaDashboardHistoryDB) query() *gorm.DB { return db.Table(TableDashboardHistory) }

func (db *ErdaDashboardHistoryDB) Begin() *ErdaDashboardHistoryDB {
	return &ErdaDashboardHistoryDB{DB: db.DB.Begin()}
}

func (db *ErdaDashboardHistoryDB) Save(model *ErdaDashboardHistory) (*ErdaDashboardHistory, error) {
	err := db.DB.Save(model).Error
	if err != nil {
		return nil, err
	}
	return model, nil
}

func (db *ErdaDashboardHistoryDB) ListByPage(pageNum, pageSize int64, scope, scopeId string) ([]*ErdaDashboardHistory, int64, error) {
	var (
		history []*ErdaDashboardHistory
		total   int64
	)

	query := db.Table(TableDashboardHistory).Where("`scope`=?", scope).Where("`scope_id`=?", scopeId)
	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}
	if err := query.Order("created_at DESC").Limit(pageSize).Offset((pageNum - 1) * pageSize).Find(&history).Error; err != nil {
		return nil, 0, err
	}
	return history, total, nil
}

func (db *ErdaDashboardHistoryDB) FindById(id string) (*ErdaDashboardHistory, error) {
	history := &ErdaDashboardHistory{}
	err := db.query().Where("`id` = ?", id).Find(&history).Error
	if err != nil {
		return nil, err
	}
	return history, nil
}

func (db *ErdaDashboardHistoryDB) UpdateStatusAndFileUUID(id, status, fileUUID, errorMessage string) error {
	byId, err := db.FindById(id)
	if err != nil {
		return err
	}
	byId.Status = status
	byId.FileUUID = fileUUID
	byId.ErrorMessage = errorMessage
	_, err = db.Save(byId)
	if err != nil {
		return err
	}
	return nil
}
