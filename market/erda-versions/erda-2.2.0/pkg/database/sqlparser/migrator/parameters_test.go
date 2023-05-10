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

package migrator_test

import (
	"testing"

	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
)

type DSNParameters_Format struct {
	P   migrator.DSNParameters
	DSN string
}

func TestDSNParameters_Format(t *testing.T) {
	p1 := migrator.DSNParameters{
		Username:  "root",
		Password:  "12345678",
		Host:      "0.0.0.0",
		Port:      3306,
		Database:  "erda",
		ParseTime: true,
		Timeout:   150,
	}
	p2 := p1
	p2.Password = ""

	cases := []DSNParameters_Format{
		{p1, "root:12345678@tcp(0.0.0.0:3306)/erda?charset=utf8mb4%2Cutf8&loc=Local&multiStatements=true&parseTime=true&timeout=0s"},
		{p2, "root@tcp(0.0.0.0:3306)/erda?charset=utf8mb4%2Cutf8&loc=Local&multiStatements=true&parseTime=true&timeout=0s"},
	}
	for _, case_ := range cases {
		if dsn := case_.P.Format(true); dsn != case_.DSN {
			t.Fatalf("failed to format, correct: %s, actural: %s", case_.DSN, dsn)
		}
	}
}
