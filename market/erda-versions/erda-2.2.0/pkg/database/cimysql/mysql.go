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

package cimysql

import (
	"fmt"
	"time"

	"github.com/caarlos0/env"
	_ "github.com/go-sql-driver/mysql"
	"github.com/sirupsen/logrus"
	"github.com/xormplus/core"
	"github.com/xormplus/xorm"
)

var Engine *xorm.Engine

type config struct {
	Host     string `env:"MYSQL_HOST" envDefault:"127.0.0.1"`
	Port     int    `env:"MYSQL_PORT" envDefault:"3306"`
	Username string `env:"MYSQL_USERNAME" envDefault:"root"`
	Password string `env:"MYSQL_PASSWORD" envDefault:"anywhere"`
	Database string `env:"MYSQL_DATABASE" envDefault:"ci"`
	Charset  string `env:"MYSQL_CHARSET" envDefault:"utf8mb4"`

	MaxIdle int `env:"MYSQL_MAXIDLE" envDefault:"10"`
	MaxConn int `env:"MYSQL_MAXCONN" envDefault:"10"`

	ConnMaxLifetime time.Duration `env:"MYSQL_CONNMAXLIFETIME" envDefault:"10s"`

	LogLevel string `env:"MYSQL_LOG_LEVEL" envDefault:"INFO"`
}

func init() {
	cfg := config{}
	err := env.Parse(&cfg)
	if err != nil {
		logrus.Fatalf("get mysql configuration failed: %v", err)
	}

	Engine, err = xorm.NewEngine("mysql", fmt.Sprintf("%s:%s@tcp(%s:%d)/%s?charset=%s&parseTime=True&loc=Local",
		cfg.Username, cfg.Password, cfg.Host, cfg.Port, cfg.Database, cfg.Charset))
	if err != nil {
		logrus.Fatalf("connect to mysql failed: %v", err)
	}

	// if err := Engine.Ping(); err != nil {
	// 	logrus.Fatalf("ping mysql failed: %v", err)
	// }

	Engine.ShowSQL(false)

	logLevel := core.LOG_INFO
	if cfg.LogLevel == "DEBUG" {
		logLevel = core.LOG_DEBUG
	}
	Engine.SetLogLevel(logLevel)

	Engine.SetMapper(core.GonicMapper{})
	Engine.SetMaxOpenConns(cfg.MaxConn)
	Engine.SetMaxIdleConns(cfg.MaxIdle)
	Engine.SetConnMaxLifetime(cfg.ConnMaxLifetime)
}
