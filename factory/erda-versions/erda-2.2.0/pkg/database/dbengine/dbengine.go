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

package dbengine

import (
	"github.com/jinzhu/gorm"
	_ "github.com/jinzhu/gorm/dialects/mysql"
)

type DBEngine struct {
	*gorm.DB
}

// Open 构造引擎，打开数据库连接池
func Open(c ...*Conf) (*DBEngine, error) {
	cfg, err := loadCfg(c...)
	if err != nil {
		return nil, err
	}
	db, err := gorm.Open("mysql", cfg.url())
	if err != nil {
		return nil, err
	}
	// connection pool
	db.DB().SetMaxIdleConns(cfg.maxIdleConns())
	db.DB().SetMaxOpenConns(cfg.maxOpenConns())
	db.DB().SetConnMaxLifetime(cfg.maxLifeTime())

	// debug
	if cfg.Debug {
		db.LogMode(true)
	}

	engine := DBEngine{
		DB: db,
	}
	return &engine, nil
}

// MustOpen 强制打开，err 时 panic
func MustOpen(c ...*Conf) *DBEngine {
	engine, err := Open(c...)
	if err != nil {
		panic(err)
	}
	return engine
}

// Close 关闭数据库连接池
func (e *DBEngine) Close() error {
	if e == nil || e.DB == nil {
		return nil
	}
	return e.DB.Close()
}

func loadCfg(c ...*Conf) (*Conf, error) {
	if len(c) > 0 {
		return c[0], nil
	}
	return LoadDefaultConf()
}

// Ping ping mysql server
func (e *DBEngine) Ping() error {
	return e.DB.DB().Ping()
}
