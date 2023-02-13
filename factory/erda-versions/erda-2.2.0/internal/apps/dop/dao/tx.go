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

package dao

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/apps/dop/conf"
)

type TX struct {
	*gorm.DB
}

func (tx *TX) Sq() *gorm.DB {
	if tx == nil {
		return nil
	}
	return tx.DB
}

func (db *DBClient) Tx() *TX {
	if conf.Debug() {
		return &TX{DB: db.Begin().Debug()}
	}
	return &TX{DB: db.Begin()}
}

func (db *DBClient) Sq() *gorm.DB {
	if conf.Debug() {
		return db.Debug()
	}
	return db.DB
}
