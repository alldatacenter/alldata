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

package block

import (
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/pkg/mysql"
)

// table names
var (
	tableSystemBlock = "sp_dashboard_block_system"
	tableBlock       = "sp_dashboard_block"
)

// systemBlock .
type SystemBlock struct {
	ID         string         `gorm:"not null;unique_index:id;type:varchar(64)" json:"id"`
	Name       string         `gorm:"not null;type:varchar(32);unique_index: Name, Scope, ScopeID" json:"name" binding:"required"`
	Desc       string         `json:"desc"`
	Domain     string         `json:"domain"`
	Scope      string         `gorm:"unique_index: Name, Scope, ScopeID" json:"scope"`
	ScopeID    string         `gorm:"unique_index: Name, Scope, ScopeID" json:"scopeId"`
	ViewConfig *ViewConfigDTO `gorm:"not null;type:text" json:"viewConfig"`
	DataConfig *dataConfigDTO `gorm:"type:text" json:"dataConfig"`
	CreatedAt  time.Time      `json:"createdAt"`
	UpdatedAt  time.Time      `json:"updatedAt"`
	Version    string         `json:"version"`
}

// TableName .
func (SystemBlock) TableName() string { return tableSystemBlock }

// userBlock .
type UserBlock struct {
	ID         string         `gorm:"not null;unique_index: Name, Scope, ScopeID,ID;type:varchar(64)" json:"id"`
	Name       string         `gorm:"not null;type:varchar(32);unique_index: Name, Scope, ScopeID,ID" json:"name" binding:"required"`
	Desc       string         `json:"desc"`
	Domain     string         `json:"domain"`
	Scope      string         `gorm:"unique_index: Name, Scope, ScopeID,ID" json:"scope"`
	ScopeID    string         `gorm:"unique_index: Name, Scope, ScopeID,ID" json:"scopeId"`
	ViewConfig *ViewConfigDTO `gorm:"type:text;not null" json:"viewConfig"`
	DataConfig *dataConfigDTO `gorm:"type:text;not nul" json:"dataConfig"`
	CreatedAt  time.Time      `json:"createdAt"`
	UpdatedAt  time.Time      `json:"updatedAt"`
	Version    string         `json:"version"`
}

// TableName .
func (UserBlock) TableName() string { return tableBlock }

// db .
type DB struct {
	*gorm.DB
	userBlock   *User
	systemBlock *System
}

// newDB .
func newDB(db *gorm.DB) *DB {
	return &DB{
		DB:          db,
		userBlock:   &User{db},
		systemBlock: &System{db},
	}
}

// Begin .
func (db *DB) Begin() *DB {
	tx := db.DB.Begin()
	return newDB(tx)
}

// DashboardBlockQuery .
type DashboardBlockQuery struct {
	ID            string
	Scope         string
	ScopeID       string
	CreatedAtDesc bool
}

// Supplements Query condition .
func (q *DashboardBlockQuery) Supplements(db *gorm.DB) *gorm.DB {
	if len(q.ID) != 0 {
		db = db.Where("id = ?", q.ID)
	}
	if len(q.Scope) != 0 {
		db = db.Where("scope = ?", q.Scope)
	}
	if len(q.ScopeID) != 0 {
		db = db.Where("scope_id = ?", q.ScopeID)
	}
	if q.CreatedAtDesc {
		db = db.Order("created_at desc")
	}
	return db
}

// User .
type User struct {
	*gorm.DB
}

// Save .
func (db *User) Save(obj *UserBlock) error {
	return db.DB.Save(obj).Error
}

// Get user dashboard block
func (db *User) Get(query *DashboardBlockQuery) (obj *UserBlock, err error) {
	obj = &UserBlock{}
	err = mysql.GenerateGetDb(db.DB, query).First(&obj).Error
	if err != nil {
		return obj, err
	}
	return obj, nil
}

// Delete user dashboard block
func (db *User) Delete(query *DashboardBlockQuery) (err error) {
	err = mysql.GenerateGetDb(db.DB, query).Delete(&UserBlock{}).Error
	if err != nil {
		return err
	}
	return nil
}

// List user dashboard block
func (db *User) List(query *DashboardBlockQuery, pageSize int64, pageNo int64) (objs []*UserBlock, total int, err error) {
	err = mysql.GenerateListDb(db.DB, query, pageSize, pageNo).Find(&objs).Offset(0).Limit(-1).Count(&total).Error
	if err != nil {
		return nil, 0, err
	}
	return objs, total, nil
}

// System .
type System struct {
	*gorm.DB
}

// Save system dashboard block
func (db *System) Save(obj *SystemBlock) error {
	return db.DB.Save(obj).Error
}

// Get system dashboard block
func (db *System) Get(query *DashboardBlockQuery) (obj *SystemBlock, err error) {
	obj = &SystemBlock{}
	err = mysql.GenerateGetDb(db.DB, query).First(&obj).Error
	if err != nil {
		return obj, err
	}
	return obj, nil
}

// Delete system dashboard block
func (db *System) Delete(query *DashboardBlockQuery) (err error) {
	err = mysql.GenerateGetDb(db.DB, query).Delete(&SystemBlock{}).Error
	if err != nil {
		return err
	}
	return nil
}

// List system dashboard block
func (db *System) List(query *DashboardBlockQuery, pageSize int64, pageNo int64) (objs []*SystemBlock, total int, err error) {
	err = mysql.GenerateListDb(db.DB, query, pageSize, pageNo).Find(&objs).Offset(0).Limit(-1).Count(&total).Error
	if err != nil {
		return nil, 0, err
	}
	return objs, total, nil
}
