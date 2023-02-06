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

const indexNameLinterConfig = `
- name: IndexNameLinter
  switchOn: true
  white:
    patterns:
      - ".*-base$"
  meta:
    indexPattern: "^idx_.*"
    uniqPattern: "^uk_.*"`

const indexNameLinterSQL = `
create table some_table (
	id bigint,
	primary key id (id)
);

create table some_table (
	first_name varchar(10),
	unique index (first_name)
);

create table some_table (
	last_name varchar(10),
	index (last_name)
);

create table some_table (
	id bigint,
	first_name varchar(10),
	last_name varchar(10),
	primary key pk_id (id),
	unique index uk_first_name (first_name),
	index idx_last_name (last_name)
);
`

func TestNewIndexNameLinter(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(indexNameLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	name := "indexNameLinterSQL"
	linter := sqllint.New(cfg)
	if err := linter.Input("", name, []byte(indexNameLinterSQL)); err != nil {
		t.Error(err)
	}
	errors := linter.Errors()[name].Lints
	t.Logf("errors: %v", errors)
	if len(errors) == 0 {
		t.Fatal("failed")
	}
}
