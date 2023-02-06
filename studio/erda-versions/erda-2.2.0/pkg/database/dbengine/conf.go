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
	"fmt"
	"time"

	"github.com/erda-project/erda/pkg/envconf"
)

type Conf struct {
	MySQLURL            string `env:"MYSQL_URL"`
	MySQLHost           string `env:"MYSQL_HOST"`
	MySQLPort           string `env:"MYSQL_PORT"`
	MySQLUsername       string `env:"MYSQL_USERNAME"`
	MySQLPassword       string `env:"MYSQL_PASSWORD"`
	MySQLDatabase       string `env:"MYSQL_DATABASE"`
	MySQLCharset        string `env:"MYSQL_CHARSET" default:"utf8mb4"`
	MySQLMaxIdleConns   int    `env:"MYSQL_MAXIDLECONNS"`
	MySQLMaxOpenConns   int    `env:"MYSQL_MAXOPENCONNS"`
	MySQLMaxLifeTime    int64  `env:"MYSQL_MAXLIFETIME"` // 单位秒 (s)
	Debug               bool   `env:"DEBUG"`
	MySQLTLS            string `env:"MYSQL_TLS"`
	MySQLCaCertPath     string `env:"MYSQL_CACERTPATH"`
	MySQLClientCertPath string `env:"MYSQL_CLIENTCERTPATH"`
	MySQLClientKeyPath  string `env:"MYSQL_CLIENTKEYPATH"`
}

func LoadDefaultConf() (*Conf, error) {
	cfg := Conf{}
	if err := envconf.Load(&cfg); err != nil {
		return nil, err
	}
	return &cfg, nil
}

// url 返回 MySQL 连接地址
func (cfg *Conf) url() string {
	if cfg.MySQLURL != "" {
		return cfg.MySQLURL
	}

	url := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?charset=%s&parseTime=True&loc=Local",
		cfg.MySQLUsername, cfg.MySQLPassword, cfg.MySQLHost, cfg.MySQLPort, cfg.MySQLDatabase, cfg.MySQLCharset)
	if cfg.MySQLTLS != "" {
		url = fmt.Sprintf("%v&tls=%s", url, cfg.MySQLTLS)
	}
	return url
}

// maxIdleConns 返回 MySQL 最大连接池
func (cfg *Conf) maxIdleConns() int {
	if cfg.MySQLMaxIdleConns > 0 {
		return cfg.MySQLMaxIdleConns
	}
	return 5
}

// maxOpenConns 返回 MySQL 最大连接数
func (cfg *Conf) maxOpenConns() int {
	if cfg.MySQLMaxOpenConns > 0 {
		return cfg.MySQLMaxOpenConns
	}
	return 50
}

// maxLifeTime 返回 MySQL 连接最大存活时间
func (cfg *Conf) maxLifeTime() time.Duration {
	if cfg.MySQLMaxLifeTime > 0 {
		return time.Duration(cfg.MySQLMaxLifeTime) * time.Second
	}
	return 180 * time.Second
}
