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

package pygrator_test

import (
	"os"
	"testing"

	"github.com/erda-project/erda/pkg/database/sqlparser/pygrator"
)

var settings = pygrator.Settings{
	Engine:   pygrator.MySQLConnectorEngine,
	User:     "root",
	Password: "12345678",
	Host:     "localhost",
	Port:     3306,
	Name:     "erda",
	TimeZone: pygrator.TimeZoneAsiaShanghai,
}

func TestGenSettings(t *testing.T) {
	if err := pygrator.GenSettings(os.Stdout, settings); err != nil {
		t.Fatal(err)
	}
}

func TestParseDSN(t *testing.T) {
	var dsn = "dspo:12345678@(localhost:3307)/dbname"
	settings, err := pygrator.ParseDSN(dsn)
	if err != nil {
		t.Fatal(err)
	}
	t.Logf("%+v", settings)
}
