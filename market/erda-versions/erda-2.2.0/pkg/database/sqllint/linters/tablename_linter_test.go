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

const tableNameLinterConfig = `# 表名称校验(1) 表名应当以 erda 开头
- name: TableNameLinter
  alias: TableNameLinter.以erda_开头仅包含小写英文字母数字下划线
  white:
    patterns:
      - ".*-base"
    committedAt:
      - <=20211230
  meta:
    patterns: 
    - "^erda_[a-z0-9_]{1,59}"
`

const tablenameLinterSQL = `
create table some_0_table (
	id int
);

create table Some_table (
	id int
);

create table erda_某表 (
	id int
);

create table erda_Big (
	id int
);
`

func TestNewTableNameLinter(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(tableNameLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	var s = script{
		Name:    "stmt-1",
		Content: tablenameLinterSQL,
	}
	linter := sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Error(err)
	}
	lints := linter.Errors()[s.Name].Lints
	if len(lints) != 4 {
		t.Fatal("failed", len(lints))
	}
	t.Logf("errors: %v", lints)
}
