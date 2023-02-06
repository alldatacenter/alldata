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

package linters_test

import (
	"testing"

	"github.com/erda-project/erda/pkg/database/sqllint"
)

const columnNameLinterConfig = `
- name: ColumnNameLinter
  white:
    patterns:
    - ".*-base"
  meta:
    patterns: 
    - "^[0-9a-z_]{1,64}$"`

const columnNameLinterSQL = `
create table some_table (
	姓名 varchar(100)
);

create table some_table (
	BigBang varchar(100)
);

create table some_table (
	3d_school varchar(100)
);

create table some_table (
	level_3_vip varchar(100)
);
`

func TestNewColumnNameLinter(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(columnNameLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	linter := sqllint.New(cfg)
	var scriptName = "columnNameLinterSQL"
	if err := linter.Input("", scriptName, []byte(columnNameLinterSQL)); err != nil {
		t.Fatal(err)
	}

	errs := linter.Errors()
	lintInfo := errs[scriptName]
	t.Logf("errs: %v", errs)
	if len(lintInfo.Lints) == 0 {
		t.Fatal("failed to lint")
	}
}
