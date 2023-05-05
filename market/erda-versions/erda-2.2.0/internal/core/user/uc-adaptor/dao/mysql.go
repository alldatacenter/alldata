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

// Package dbclient 定义数据库操作的方法, orm 等。
package dao

import (
	"github.com/erda-project/erda/internal/core/user/uc-adaptor/conf"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

const BULK_INSERT_CHUNK_SIZE = 3000

type DBClient struct {
	*dbengine.DBEngine
}

func Open() (*DBClient, error) {
	engine, err := dbengine.Open()
	if err != nil {
		return nil, err
	}
	if conf.Debug() {
		engine.LogMode(true)
	}
	db := DBClient{DBEngine: engine}
	// custom init
	if err := db.initOpts(); err != nil {
		return nil, err
	}
	return &db, nil
}

func (db *DBClient) Close() error {
	if db == nil || db.DBEngine == nil {
		return nil
	}
	return db.DBEngine.Close()
}

// TODO: 自定义初始化内容
func (db *DBClient) initOpts() error {
	return nil
}
