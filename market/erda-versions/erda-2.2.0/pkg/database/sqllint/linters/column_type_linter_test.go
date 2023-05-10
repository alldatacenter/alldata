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

func TestHub_ColumnTypeLinter(t *testing.T) {
	var configuration = `
- name: ColumnTypeLinter
  alias: 字段类型校验.created_at
  switchOn: true
  white:
    patterns:
      - ".*-base"
    modules: [ ]
    committedAt: [ ]
    filenames: [ ]
  meta:
    columnName: created_at
    types:
      - type: datetime
`
	var s = script{
		Name:    "createTableWithInvalidColumn",
		Content: "Create Table t1 (created_at Bigint)",
	}
	cfg, err := sqllint.LoadConfig([]byte(configuration))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	linter := sqllint.New(cfg)
	if err = linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatal("failed to Input: ", err)
	}
	if len(linter.Errors()[s.Name].Lints) == 0 {
		t.Fatal("there should be errors")
	}
	t.Log(linter.Errors()[s.Name].Lints)
}

func TestHub_ColumnTypeLinter2(t *testing.T) {
	var config = `
- name: ColumnTypeLinter
  alias: "字段类型校验: id 应当为 varchar(36) 或 char(36)"
  white:
    patterns:
      - ".*-base$"
    committedAt:
      - "<=20220104"
  meta:
    columnName: id
    types:
      - type: varchar
        flen: 36
      - type: char
        flen: 36
`
	var s = script{
		Name:    "stmt-1",
		Content: "create table t1 (id varchar(36));",
	}
	cfg, err := sqllint.LoadConfig([]byte(config))
	if err != nil {
		t.Fatalf("failed to LoadConfig: %v", err)
	}
	linter := sqllint.New(cfg)
	if err = linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatalf("failed to Input: %v", err)
	}
	lints := linter.Errors()[s.Name].Lints
	if len(lints) > 0 {
		t.Log(lints)
		t.Fatal("there should be no error")
	}
}
