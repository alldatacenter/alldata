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

func TestHub_NecessaryTableOptionLinter(t *testing.T) {
	t.Run("testHub_NecessaryTableOptionLinter1", testHub_NecessaryTableOptionLinter1)
	t.Run("testHub_NecessaryTableOptionLinter2", testHub_NecessaryTableOptionLinter2)
	t.Run("testHub_NecessaryTableOptionLinter3", testHub_NecessaryTableOptionLinter3)
	t.Run("testHub_NecessaryTableOptionLinter4", testHub_NecessaryTableOptionLinter4)
}

func testHub_NecessaryTableOptionLinter1(t *testing.T) {
	var config = `
- name: NecessaryTableOptionLinter
  meta:
    key: "TableOptionComment"
`
	var s = script{
		Name:    "stmt-1",
		Content: "create table t1 (col1 datetime)",
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
	if len(lints) == 0 {
		t.Fatal("there should be errors")
	}
	t.Log(lints)
}

func testHub_NecessaryTableOptionLinter2(t *testing.T) {
	var config = `
- name: NecessaryTableOptionLinter
  meta:
    key: "TableOptionComment"
`
	var s = script{
		Name:    "stmt-1",
		Content: "create table t1 (col1 datetime) comment 'some info'",
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

func testHub_NecessaryTableOptionLinter3(t *testing.T) {
	var config = `
- name: NecessaryTableOptionLinter
  meta:
    key: "TableOptionCharset"
    values:
    - utf8
    - utf8mb4
`
	var s = script{
		Name:    "stmt-1",
		Content: "create table t1 (col1 datetime)",
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
	if len(lints) == 0 {
		t.Fatal("there should be errors")
	}
	t.Log(lints)
}

func testHub_NecessaryTableOptionLinter4(t *testing.T) {
	var config = `
- name: NecessaryTableOptionLinter
  meta:
    key: "TableOptionCharset"
    values:
    - utf8
    - utf8mb4
`
	var s = script{
		Name:    "stmt-1",
		Content: "create table t1 (col1 datetime) charset utf8",
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
