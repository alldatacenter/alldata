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

package mysql

import (
	"github.com/go-sql-driver/mysql"
	"github.com/jinzhu/gorm"
	// "github.com/mattn/go-sqlite3"
)

// Query .
type Query interface {
	Supplements(db *gorm.DB) *gorm.DB
}

// GenerateListDb .
func GenerateListDb(
	db *gorm.DB,
	q Query,
	pageSize int64,
	pageNo int64,
) *gorm.DB {
	if pageSize == 0 {
		return q.Supplements(db)
	} else {
		return q.Supplements(db).Limit(pageSize).Offset((pageNo - 1) * pageSize)
	}
}

// GenerateGetDb .
func GenerateGetDb(db *gorm.DB, q Query) *gorm.DB {
	return q.Supplements(db)
}

// IsUniqueConstraintError .
func IsUniqueConstraintError(err error) bool {
	const (
		ErrMySQLDupEntry            = 1062
		ErrMySQLDupEntryWithKeyName = 1586
	)
	/*
		if sqliteErr, ok := err.(sqlite3.Error); ok {
			if sqliteErr.ExtendedCode == sqlite3.ErrConstraintUnique ||
				sqliteErr.ExtendedCode == sqlite3.ErrConstraintPrimaryKey {
				return true
			}
		} else */
	if mysqlErr, ok := err.(*mysql.MySQLError); ok {
		if mysqlErr.Number == ErrMySQLDupEntry ||
			mysqlErr.Number == ErrMySQLDupEntryWithKeyName {
			return true
		}
	}
	return false
}
