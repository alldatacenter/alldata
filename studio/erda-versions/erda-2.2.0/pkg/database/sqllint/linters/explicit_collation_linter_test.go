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

const explicitCollationLinterConfig = `
- name: ExplicitCollationLinter
  switchOn: true
  white:
    patterns:
      - ".*-base$"`

func TestNewExplicitCollationLinter(t *testing.T) {
	var (
		sqlA = script{
			Name: "sql-a",
			Content: `
create table t0 (
	left_key varchar(1024) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'asset id'
);
`,
		}
		sqlB = script{
			Name: "sql-b",
			Content: `
create table t0 (
	left_key varchar(1024)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API';
`,
		}
		sqlC = script{
			Name: "sql-c",
			Content: `
create table t0 (
	left_key varchar(1024) character set utf8mb4  COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'asset id'
)
`,
		}

		sqlD = script{
			Name: "sql-d",
			Content: `
create table t0 (
	left_key varchar(1024)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API';
`,
		}
	)

	cfg, err := sqllint.LoadConfig([]byte(explicitCollationLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}

	linter := sqllint.New(cfg)
	if err := linter.Input("", sqlA.Name, sqlA.GetContent()); err != nil {
		t.Fatalf("failed to Input sqlA to linter: %v", err)
	}
	lints := linter.Errors()[sqlA.Name].Lints
	if len(lints) == 0 {
		t.Fatal("failed to lint, there should be some errors")
	}
	t.Logf("sqlA's error: %+v", lints)

	linter = sqllint.New(cfg)
	if err := linter.Input("", sqlB.Name, sqlB.GetContent()); err != nil {
		t.Fatalf("failed to Input sqlB to linter: %v", err)
	}
	lints = linter.Errors()[sqlB.Name].Lints
	if len(lints) == 0 {
		t.Fatal("failed to lint, there should be some errors")
	}
	t.Logf("sqlB's error: %+v", lints)

	linter = sqllint.New(cfg)
	if err := linter.Input("", sqlC.Name, sqlC.GetContent()); err != nil {
		t.Fatalf("failed to Input sqlC to linter: %v", err)
	}
	lints = linter.Errors()[sqlC.Name].Lints
	if len(lints) == 0 {
		t.Fatal("failed to lint, there should be some errors")
	}
	t.Logf("sqlC's error: %v", lints)

	linter = sqllint.New(cfg)
	if err := linter.Input("", sqlD.Name, sqlD.GetContent()); err != nil {
		t.Fatalf("failed to Input sqlC to linter: %v", err)
	}
	lints = linter.Errors()[sqlD.Name].Lints
	if len(lints) != 0 {
		t.Fatalf("failed to lint, there should be no errors, but errors: %+v", lints)
	}

}
