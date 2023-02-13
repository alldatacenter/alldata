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

const manualTimeSetterLinterConfig = `# 显式插入时间校验
- name: ManualTimeSetterLinter
  alias: 禁止显示插入时间.created_at
  switchOn: true
  white:
    patterns:
      - ".*-base$"
  meta:
    columnName: created_at

# 显式插入时间校验
- name: ManualTimeSetterLinter
  alias: 禁止显示插入时间.updated_at
  switchOn: true
  white:
    patterns:
      - ".*-base"
  meta:
    columnName: updated_at`

func TestNewManualTimeSetterLinter(t *testing.T) {
	var (
		s1 = script{
			Name:    "s1",
			Content: "insert into t1 (created_at) values ('2021-01-01 00:00:00')",
		}
		s2 = script{
			Name:    "s2",
			Content: "insert into t1 (updated_at) values ('2021-01-01 00:00:00')",
		}
		s3 = script{
			Name:    "s3",
			Content: "update t1 set created_at = '2021-01-01 00:00:00'",
		}
		s4 = script{
			Name:    "s4",
			Content: "update t2 set updated_at = '2021-01-01 00:00:00'",
		}
		s5 = script{
			Name:    "s5",
			Content: "create table t1 (id bigint)",
		}
		s6 = script{
			Name:    "s6",
			Content: "insert into t1 (col1) values (0)",
		}
	)

	cfg, err := sqllint.LoadConfig([]byte(manualTimeSetterLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	linter := sqllint.New(cfg)
	for _, s := range []script{s1, s2, s3, s4, s5, s6} {
		if err := linter.Input("", s.Name, s.GetContent()); err != nil {
			t.Fatalf("faield to Input, name: %s, err: %v", s.Name, err)
		}
	}
	if lints := linter.Errors()[s1.Name].Lints; len(lints) == 0 {
		t.Fatal("there should be errors")
	}
	if lints := linter.Errors()[s2.Name].Lints; len(lints) == 0 {
		t.Fatal("there should be errors")
	}
	if lints := linter.Errors()[s3.Name].Lints; len(lints) == 0 {
		t.Fatal("there should be errors")
	}
	if lints := linter.Errors()[s4.Name].Lints; len(lints) == 0 {
		t.Fatal("there should be errors")
	}
	if lints := linter.Errors()[s5.Name].Lints; len(lints) > 0 {
		t.Log(lints)
		t.Fatal("there should be no error")
	}
	if lints := linter.Errors()[s6.Name].Lints; len(lints) > 0 {
		t.Log(lints)
		t.Fatal("there should be no error")
	}
}
