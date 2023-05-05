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

const primaryKeyLinterConfig = `
- name: PrimaryKeyLinter
  white:
    patterns:
      - ".*-base$"
  meta:
    columnName: id
`

const createTableWithoutID = `
Create Table some_table (
	created_at datetime,
	updated_at datetime
)
`

const createTableWithID = `
create table some_table (
	id bigint
)
`

const createTableWithBigintID = `
create table some_table (
	id bigint,
	created_at datetime
)
`

const createTableWithCharID = `
create table t1 (
	id char(36)
)
`

const createTableWithVarcharID = `
create table some_table (
	id varchar(199),
	created_at datetime
)
`

const createTableWithErrorIDType = `
create table some_table (
	id datetime,
	created_at datetime
)
`

const idIsPrimary = `
create table some_table (
	id bigint primary key,
	uuid bigint
)
`

const idIsNotPrimary = `
create table some_table (
	id bigint,
	uuid bigint primary key
)
`

const primaryIsNotId = `
create table some_table (
	uuid bigint primary key,
	id bigint
)
`

func TestPrimaryKeyLinter(t *testing.T) {
	t.Run("testNewIDIsPrimaryLinterIDIsPrimary", testNewIDIsPrimaryLinterIDIsPrimary)
	t.Run("testNewIDIsPrimaryLinterIDIsNotPrimary", testNewIDIsPrimaryLinterIDIsNotPrimary)
	t.Run("testNewIDIsPrimaryLinterPrimaryIsNotID", testNewIDIsPrimaryLinterPrimaryIsNotID)
}

func testNewIDIsPrimaryLinterIDIsPrimary(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(primaryKeyLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	var s = script{
		Name:    "stmt-1",
		Content: idIsPrimary,
	}
	linter := sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatal(err)
	}
	lints := linter.Errors()[s.Name].Lints
	if len(lints) > 0 {
		t.Fatal("fails")
	}
}

func testNewIDIsPrimaryLinterIDIsNotPrimary(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(primaryKeyLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	var s = script{
		Name:    "stmt-2",
		Content: idIsNotPrimary,
	}
	linter := sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatal(err)
	}
	lints := linter.Errors()[s.Name].Lints
	if len(lints) == 0 {
		t.Fatal("fails")
	}
}

func testNewIDIsPrimaryLinterPrimaryIsNotID(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(primaryKeyLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	var s = script{
		Name:    "stmt-3",
		Content: primaryIsNotId,
	}
	linter := sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatal(err)
	}
	lints := linter.Errors()[s.Name].Lints
	if len(lints) == 0 {
		t.Fatal("fails")
	}
	t.Log(lints)
}
