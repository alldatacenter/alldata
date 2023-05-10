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

package template

import (
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/pkg/mysql"
)

var (
	tableTemplate = "sp_dashboard_template"
)

type Template struct {
	ID          string         `gorm:"not null;unique_index: Name, Scope, ScopeID,ID;type:varchar(64)" json:"id"`
	Name        string         `gorm:"not null;type:varchar(32);unique_index: Name, Scope, ScopeID,ID"  json:"name" binding:"required"`
	Description string         `json:"description"`
	Scope       string         `gorm:"unique_index: Name, Scope, ScopeID,ID" json:"scope"`
	ScopeID     string         `gorm:"unique_index: Name, Scope, ScopeID,ID" json:"scopeId"`
	ViewConfig  *ViewConfigDTO `gorm:"type:text;not null" json:"viewConfig"`
	CreatedAt   time.Time      `json:"createdAt"`
	UpdatedAt   time.Time      `json:"updatedAt"`
	Version     string         `json:"version"`
	Type        int64          `json:"type" default:"1"` //TODO auth
}

func (Template) TableName() string { return tableTemplate }

type DB struct {
	*gorm.DB
	templateDB *templateDB
}

func newDB(db *gorm.DB) *DB {
	return &DB{
		DB:         db,
		templateDB: &templateDB{db},
	}
}

func (db *DB) Begin() *DB {
	tx := db.DB.Begin()
	return newDB(tx)
}

type templateQuery struct {
	ID      string
	Scope   string
	ScopeID string
	Type    int64
	Name    string
}

// Supplements Query condition .
func (q *templateQuery) Supplements(db *gorm.DB) *gorm.DB {
	if len(q.ID) != 0 {
		db = db.Where("id = ?", q.ID)
	}
	if len(q.Scope) != 0 {
		db = db.Where("scope = ?", q.Scope)
	}
	if len(q.ScopeID) != 0 {
		db = db.Where("scope_id = ?", q.ScopeID)
	}
	if len(q.Name) != 0 {
		db = db.Where("name like ?", "%"+q.Name+"%")
	}
	if q.Type != 0 {
		db = db.Where("type = ?", q.Type)
	}
	return db
}

type templateDB struct {
	*gorm.DB
}

func (db *templateDB) Save(obj *Template) error {
	return db.DB.Save(obj).Error
}

// Get template
func (db *templateDB) Get(query *templateQuery) (obj *Template, err error) {
	obj = &Template{}
	err = mysql.GenerateGetDb(db.DB, query).First(&obj).Error
	if err != nil {
		return obj, err
	}
	return obj, nil
}

// Delete template
func (db *templateDB) Delete(query *templateQuery) (err error) {
	err = mysql.GenerateGetDb(db.DB, query).Delete(&Template{}).Error
	if err != nil {
		return err
	}
	return nil
}

// List template
func (db *templateDB) List(query *templateQuery, pageSize int64, pageNo int64) (objs []*Template, total int, err error) {
	err = mysql.GenerateListDb(db.DB, query, pageSize, pageNo).Find(&objs).Offset(0).Limit(-1).Count(&total).Error
	if err != nil {
		return nil, 0, err
	}
	return objs, total, nil
}
