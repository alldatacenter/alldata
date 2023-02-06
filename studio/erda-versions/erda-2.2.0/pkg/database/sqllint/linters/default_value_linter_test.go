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

func TestHub_DefaultValueLinter(t *testing.T) {
	var config = `
- name: DefaultValueLinter
  alias: 默认值校验.created_at
  switchOn: true
  white:
    patterns:
      - ".*-base$"
    modules: [ ]
    committedAt: [ ]
    filenames: [ ]
  meta:
    columnName: created_at
    defaultValue: CURRENT_TIMESTAMP`
	var s = script{
		Name:    "stmt-1",
		Content: "create table t1 (created_at datetime default '1970-01-01')",
	}
	cfg, err := sqllint.LoadConfig([]byte(config))
	if err != nil {
		t.Fatalf("faield to LoadConfig: %v", err)
	}
	linter := sqllint.New(cfg)
	if err = linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatalf("failed to Input: %v", err)
	}
	if len(linter.Errors()[s.Name].Lints) == 0 {
		t.Fatal("there should be errors")
	}
	t.Log(linter.Errors()[s.Name].Lints)

	s.Name = "stmt-2"
	s.Content = "create table t1 (created_at datetime default CURRENT_TIMESTAMP)"
	linter = sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatalf("failed to Input: %v", err)
	}
	if lints := linter.Errors()[s.Name].Lints; len(lints) > 0 {
		t.Log(lints)
		t.Fatal("there should be no error")
	}
}
