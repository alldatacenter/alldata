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
	_ "github.com/erda-project/erda/pkg/database/sqllint/linters"
)

const booleanFieldLinterConfig = `
- name: BooleanFieldLinter
  switchOn: true
  white:
    patterns:
      - ".*-base$"
    modules: [ ]
    committedAt: [ ]
    filenames: [ ]
  meta: { }`

const booleanFieldLinterSQL = `
create table some_table (
 	public boolean
);

create table some_table (
	public tinyint(1)
);

create table some_table (
	is_public int
);
`

func TestNewBooleanFieldLinter(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(booleanFieldLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	linter := sqllint.New(cfg)
	if err := linter.Input("", "booleanFieldLinterSQL", []byte(booleanFieldLinterSQL)); err != nil {
		t.Error(err)
	}
	errors := linter.Errors()
	t.Logf("errors: %v", errors)
	if len(errors) != 1 {
		t.Error("failed")
	}
}
