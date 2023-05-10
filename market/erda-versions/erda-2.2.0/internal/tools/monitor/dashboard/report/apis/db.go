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

package apis

import (
	"time"

	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/pkg/mysql"
	block "github.com/erda-project/erda/internal/tools/monitor/core/dataview/v1-chart-block"
)

// tables name
const (
	tableReportTask    = "sp_report_task"
	tableReportHistory = "sp_report_history"
)

// ReportTask .
type reportTask struct {
	ID             uint64             `gorm:"primary_key" json:"id"`
	Name           string             `gorm:"not null;type:varchar(32);unique_index: Name, Scope, ScopeID"  json:"name"`
	Scope          string             `gorm:"unique_index: Name, Scope, ScopeID" json:"scope"`
	ScopeID        string             `gorm:"unique_index: Name, Scope, ScopeID" json:"scopeId"`
	Type           reportFrequency    `gorm:"not null"  json:"type"`
	DashboardId    string             `gorm:"not null" json:"dashboardId" `
	DashboardBlock *block.SystemBlock `gorm:"foreignkey:ID;association_foreignkey:DashboardId" json:"dashboardBlock,omitempty"`
	Enable         bool               `json:"enable"`
	NotifyTarget   *notify            `gorm:"not null;type:varchar(1024)" json:"notifyTarget"`
	PipelineCronId uint64             `json:"-"`
	CreatedAt      time.Time          `json:"createdAt"`
	UpdatedAt      time.Time          `json:"updatedAt"`

	// for test
	RunAtOnce bool `gorm:"-" json:"runAtOnce"`
}

// ReportHistory .
type reportHistory struct {
	ID             uint64          `gorm:"primary_key" json:"id"`
	Scope          string          `json:"scope"`
	ScopeID        string          `json:"scopeId"`
	TaskId         uint64          `gorm:"not null;"  json:"taskId"`
	ReportTask     reportTask      `gorm:"foreignkey:ID;association_foreignkey:TaskId" json:"reportTask"`
	DashboardId    string          `gorm:"not null;" json:"dashboardId" `
	DashboardBlock block.UserBlock `gorm:"foreignkey:ID;association_foreignkey:DashboardId" json:"dashboardBlock"`
	CreatedAt      time.Time       `json:"createdAt"`
	Start          int64           `json:"start"`
	End            int64           `json:"end"`
}

// TableName .
func (reportHistory) TableName() string { return tableReportHistory }

// TableName .
func (reportTask) TableName() string { return tableReportTask }

type Task struct {
	*gorm.DB
}
type History struct {
	*gorm.DB
}

// db .
type DB struct {
	*gorm.DB
	reportTask    *Task
	reportHistory *History
	userBlock     *block.User
	systemBlock   *block.System
}

// newDB .
func newDB(db *gorm.DB) *DB {
	return &DB{
		DB:            db,
		reportTask:    &Task{db},
		reportHistory: &History{db},
		userBlock:     &block.User{DB: db},
		systemBlock:   &block.System{DB: db},
	}
}

// Begin transaction 。
func (db *DB) Begin() *DB {
	tx := db.DB.Begin()
	return newDB(tx)
}

// ReportTaskQuery .
type reportTaskQuery struct {
	ID                    *uint64
	Scope                 string
	ScopeID               string
	PreLoadDashboardBlock bool
	CreatedAtDesc         bool
	Type                  string
}

// ReportHistoryQuery .
type reportHistoryQuery struct {
	ID                    *uint64
	TaskId                *uint64
	Scope                 string
	ScopeID               string
	StartTime             *int64
	EndTime               *int64
	CreatedAtDesc         bool
	PreLoadDashboardBlock bool
	PreLoadTask           bool
}

// Supplements report task query condition .
func (q *reportTaskQuery) Supplements(db *gorm.DB) *gorm.DB {
	if q.PreLoadDashboardBlock {
		db = db.Preload("DashboardBlock")
	}

	if q.ID != nil {
		db = db.Where("id = ?", q.ID)
	}

	if len(q.Scope) != 0 {
		db = db.Where("scope = ?", q.Scope)
	}

	if len(q.Type) != 0 {
		db = db.Where("type = ?", q.Type)
	}

	if len(q.ScopeID) != 0 {
		db = db.Where("scope_id = ?", q.ScopeID)
	}

	if q.CreatedAtDesc {
		db = db.Order("created_at desc")
	}
	return db
}

// Supplements report history query condition .
func (q *reportHistoryQuery) Supplements(db *gorm.DB) *gorm.DB {
	if q.PreLoadDashboardBlock {
		db = db.Preload("DashboardBlock")
	}

	if q.PreLoadTask {
		db = db.Preload("ReportTask")
	}

	if q.ID != nil {
		db = db.Where("id = ?", q.ID)
	}

	if q.CreatedAtDesc {
		db = db.Order("created_at desc")
	}

	if q.StartTime != nil {
		db = db.Where("start >= ?", *q.StartTime)
	}

	if q.EndTime != nil {
		db = db.Where("end < ?", *q.EndTime)
	}

	if q.TaskId != nil {
		db = db.Where("task_id = ?", q.TaskId)
	}

	if len(q.Scope) != 0 {
		db = db.Where("scope = ?", q.Scope)
	}

	if len(q.ScopeID) != 0 {
		db = db.Where("scope_id = ?", q.ScopeID)
	}

	return db
}

// Save .
func (t *Task) Save(obj *reportTask) error {
	return t.DB.Save(obj).Error
}

// Get .
func (t *Task) Get(query *reportTaskQuery) (obj *reportTask, err error) {
	obj = &reportTask{}
	err = mysql.GenerateGetDb(t.DB, query).First(&obj).Error
	if err != nil {
		logrus.Error(err.Error())
		return obj, err
	}
	return obj, nil
}

// Del report task .
func (t *Task) Del(query *reportTaskQuery) (err error) {
	err = mysql.GenerateGetDb(t.DB, query).Delete(&reportTask{}).Error
	if err != nil {
		logrus.Error(err.Error())
		return err
	}
	return nil
}

// List report task .
func (t *Task) List(query *reportTaskQuery, pageSize int64, pageNo int64) (objs []reportTask, total int, err error) {
	err = mysql.GenerateListDb(t.DB, query, pageSize, pageNo).Find(&objs).Offset(0).Limit(-1).Count(&total).Error
	if err != nil {
		return nil, 0, err
	}
	return objs, total, nil
}

// Save report history .
func (t *History) Save(obj *reportHistory) error {
	return t.DB.Save(obj).Error
}

// Get report history
func (t *History) Get(query *reportHistoryQuery) (obj *reportHistory, err error) {
	obj = &reportHistory{}
	err = mysql.GenerateGetDb(t.DB, query).First(&obj).Error
	if err != nil {
		logrus.Error(err.Error())
		return obj, err
	}
	return obj, nil
}

// Del report history .
func (t *History) Del(query *reportHistoryQuery) (err error) {
	err = mysql.GenerateGetDb(t.DB, query).Delete(&reportHistory{}).Error
	if err != nil {
		logrus.Error(err.Error())
		return err
	}
	return nil
}

// List report history .
func (t *History) List(query *reportHistoryQuery, pageSize int64, pageNo int64) (objs []reportHistory, total int, err error) {
	err = mysql.GenerateListDb(t.DB, query, pageSize, pageNo).Find(&objs).Offset(0).Limit(-1).Count(&total).Error
	if err != nil {
		return nil, 0, err
	}
	return objs, total, nil
}
