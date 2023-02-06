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

func TestHub_NecessaryColumnLinter(t *testing.T) {
	var config = `
- name: NecessaryColumnLinter
  alias: 必要字段.id
  switchOn: true
  meta:
    columnName:
    - "id"
`
	var s = script{
		Name:    "stmt-1",
		Content: "create table t1 (name varchar(225))",
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
