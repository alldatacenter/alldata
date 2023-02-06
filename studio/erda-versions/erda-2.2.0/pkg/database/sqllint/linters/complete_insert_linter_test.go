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

func TestNewCompleteInsertLinter(t *testing.T) {
	var config = `
- name: CompleteInsertLinter
  switchOn: true
  white:
    patterns:
      - ".*-base$"
    modules: [ ]
    committedAt: [ ]
    filenames: [ ]
  meta: { }`
	cfg, err := sqllint.LoadConfig([]byte(config))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}

	var s = script{
		Name:    "insert-1",
		Content: "insert into t1 \nvalues (1, 2, 3)",
	}
	linter := sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatalf("failed to Input data to linter: %v", err)
	}
	if len(linter.Errors()[s.Name].Lints) == 0 {
		t.Fatal("there should be errors")
	}
	t.Log(linter.Errors()[s.Name].Lints)

	s.Name = "insert-2"
	s.Content = "insert into t2 (col1, col2, col3) \nvalues (1, 2, 3)"
	linter = sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatalf("failed to Input data to linter: %v", err)
	}
	if len(linter.Errors()[s.Name].Lints) > 0 {
		t.Fatal("there should be no errors")
	}
}
