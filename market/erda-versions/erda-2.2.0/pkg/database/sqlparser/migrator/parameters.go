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

package migrator

import (
	"fmt"
	"net/url"
	"time"
)

const smallestRetryTimeout = 60

type Parameters interface {
	ScriptsParameters

	// MySQLParameters returns MySQL DSN configuration
	MySQLParameters() *DSNParameters

	// SandboxParameters returns Sandbox DSN configuration
	SandboxParameters() *DSNParameters

	// DebugSQL gets weather to debug SQL executing
	DebugSQL() bool

	// RetryTimeout unit: second
	RetryTimeout() uint64

	SkipMigrationLint() bool
	SkipSandbox() bool
	SkipPreMigrate() bool
	SkipMigrate() bool
}

type DSNParameters struct {
	Username  string
	Password  string
	Host      string
	Port      int
	Database  string
	ParseTime bool
	Timeout   time.Duration
}

func (c DSNParameters) Format(database bool) (dsn string) {
	dsnPat := "%s:%s@tcp(%s:%v)/%s"
	if c.Password == "" {
		dsnPat = "%s%s@tcp(%s:%v)/%s"
	}
	if database {
		dsn = fmt.Sprintf(dsnPat, c.Username, c.Password, c.Host, c.Port, c.Database)
	} else {
		dsn = fmt.Sprintf(dsnPat, c.Username, c.Password, c.Host, c.Port, "")
	}

	var params = make(url.Values)
	if c.ParseTime {
		params.Add("parseTime", "true")
	}
	if c.Timeout == 0 {
		c.Timeout = time.Second * 150
	}
	params.Add("timeout", fmt.Sprintf("%vs", int(c.Timeout.Seconds())))
	params.Add("multiStatements", "true")
	params.Add("charset", "utf8mb4,utf8")
	params.Add("loc", "Local")
	return dsn + "?" + params.Encode()
}

type SQLCollectorDir interface {
	SQLCollectorDir() string
}

func SQLCollectorFilename() string {
	return fmt.Sprintf("%s_erda-migrator.sql.log", time.Now().Format("20060102150405"))
}

func RetryTimeout(timeout uint64) time.Duration {
	if timeout > smallestRetryTimeout {
		return time.Second * time.Duration(timeout)
	}
	return time.Second * smallestRetryTimeout
}
