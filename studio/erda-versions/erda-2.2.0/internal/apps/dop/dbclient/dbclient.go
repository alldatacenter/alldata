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

// Package dbclient defines operations about database
package dbclient

import (
	"fmt"
	"reflect"

	"github.com/jinzhu/gorm"
	gormbulk "github.com/t-tiger/gorm-bulk-insert"

	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

const BULK_INSERT_CHUNK_SIZE = 3000

var DB *DBClient

type DBClient struct {
	*dbengine.DBEngine
}

func Set(ng *dbengine.DBEngine) {
	if DB == nil {
		DB = new(DBClient)
	}
	DB.DBEngine = ng
}

func Open() error {
	if DB != nil {
		return nil
	}

	engine, err := dbengine.Open()
	if err != nil {
		return err
	}

	if conf.Debug() {
		engine.LogMode(true)
	}

	DB = &DBClient{DBEngine: engine}

	// custom init
	if err := DB.initOpts(); err != nil {
		return err
	}

	return nil
}

func Close() error {
	if DB == nil || DB.DBEngine == nil {
		return nil
	}
	return DB.DBEngine.Close()
}

func (db *DBClient) initOpts() error {
	return nil
}

func (db *DBClient) BulkInsert(objects interface{}, excludeColumns ...string) error {
	v := reflect.ValueOf(objects)
	if v.Kind() != reflect.Slice {
		return fmt.Errorf("invalid objects type, must be a slice of struct")
	}
	var structSlice []interface{}
	for i := 0; i < v.Len(); i++ {
		structSlice = append(structSlice, v.Index(i).Interface())
	}
	return gormbulk.BulkInsert(db.DB, structSlice, BULK_INSERT_CHUNK_SIZE, excludeColumns...)
}

// Transaction Execute Transaction
func (db *DBClient) Transaction(f func(tx *gorm.DB) error) error {
	tx := db.Begin()
	defer func() {
		if r := recover(); r != nil {
			tx.Rollback()
		}
	}()
	if err := f(tx); err != nil {
		tx.Rollback()
		return err
	}

	return tx.Commit().Error
}
