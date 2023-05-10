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

package dbclient

import (
	"fmt"
	"time"

	"github.com/caarlos0/env"
	_ "github.com/go-sql-driver/mysql"
	"github.com/pkg/errors"
	"github.com/xormplus/core"
	"github.com/xormplus/xorm"
)

type Client struct {
	*xorm.Engine
}

type Session struct {
	*xorm.Session
	AllowZeroAffectedRows bool
	NeedAutoClose         bool
	NeedNoAutoTime        bool
}

func (client *Client) NewSession(ops ...SessionOption) *Session {
	s := &Session{}
	for _, op := range ops {
		op(s)
	}

	if s.Session == nil {
		s.Session = client.Engine.NewSession()
		s.NeedAutoClose = true
	}

	if s.NeedNoAutoTime {
		s.Session.NoAutoTime()
	}

	return s
}

func (session *Session) Close() {
	if session.NeedAutoClose {
		session.Session.Close()
	}
	return
}

type SessionOption func(*Session)

// WithNoAutoTime 仅作用在当前 session
// 若该 op 后接 WithTxSession 等其他从外部传入 session 的 op，则 WithNoAutoTime 不会在传入的 session 上生效
// 因此需要注意 op 顺序
func WithNoAutoTime() SessionOption {
	return func(session *Session) {
		session.NeedNoAutoTime = true
	}
}
func WithAllowZeroAffectedRows(allow bool) SessionOption {
	return func(session *Session) {
		session.AllowZeroAffectedRows = allow
	}
}
func WithTxSession(_session *xorm.Session) SessionOption {
	return func(session *Session) {
		session.Session = _session
	}
}

var (
	ErrZeroAffectedRows = errors.New("affected rows was 0")
	ErrRecordNotFound   = errors.New("not found")
)

func New() (*Client, error) {
	var cfg clientConfig
	if err := env.Parse(&cfg); err != nil {
		return nil, errors.Wrap(err, "failed to get mysql configuration from env")
	}

	engine, err := xorm.NewEngine("mysql", cfg.url())
	if err != nil {
		return nil, errors.Wrap(err, "failed to connect to mysql server")
	}

	engine.SetMapper(core.GonicMapper{})

	engine.ShowSQL(cfg.ShowSQL)

	engine.SetMaxOpenConns(cfg.MaxConn)
	engine.SetMaxIdleConns(cfg.MaxIdle)
	engine.SetConnMaxLifetime(cfg.ConnMaxLifetime)
	engine.SetDisableGlobalCache(true)

	return &Client{engine}, nil
}

type clientConfig struct {
	URL             string        `env:"MYSQL_URL" envDefault:""`
	Host            string        `env:"MYSQL_HOST" envDefault:"127.0.0.1"`
	Port            int           `env:"MYSQL_PORT" envDefault:"3306"`
	Username        string        `env:"MYSQL_USERNAME" envDefault:"root"`
	Password        string        `env:"MYSQL_PASSWORD" envDefault:"anywhere"`
	Database        string        `env:"MYSQL_DATABASE" envDefault:"ci"`
	MaxIdle         int           `env:"MYSQL_MAXIDLE" envDefault:"10"`
	MaxConn         int           `env:"MYSQL_MAXCONN" envDefault:"20"`
	ConnMaxLifetime time.Duration `env:"MYSQL_CONNMAXLIFETIME" envDefault:"10s"`
	LogLevel        string        `env:"MYSQL_LOG_LEVEL" envDefault:"INFO"`
	ShowSQL         bool          `env:"MYSQL_SHOW_SQL" envDefault:"false"`
	PROPERTIES      string        `env:"MYSQL_PROPERTIES" envDefault:"charset=utf8mb4&collation=utf8mb4_unicode_ci&parseTime=True&loc=Local"`
	TLS             string        `env:"MYSQL_TLS"`
	CaCertPath      string        `env:"MYSQL_CACERTPATH"`
	ClientCertPath  string        `env:"MYSQL_CLIENTCERTPATH"`
	ClientKeyPath   string        `env:"MYSQL_CLIENTKEYPATH"`
}

// url judge env mysql_url whether is null
func (cfg *clientConfig) url() string {
	if cfg.URL != "" {
		return cfg.URL
	}

	url := fmt.Sprintf("%s:%s@tcp(%s:%d)/%s?%s",
		cfg.Username, cfg.Password, cfg.Host, cfg.Port, cfg.Database, cfg.PROPERTIES)
	if cfg.TLS != "" {
		url = fmt.Sprintf("%v&tls=%s", url, cfg.TLS)
	}
	return url
}
