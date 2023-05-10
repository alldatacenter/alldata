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

func TestHub_KeywordsLinter(t *testing.T) {
	var config = `
- name: KeywordsLinter
  switchOn: true
  white:
    patterns:
      - ".*-base$"
  meta:
    "ALL": true
    "ALTER": true
    "AND": true
    "ANY": true
    "AS": true
    "ENABLE": true
    "DISABLE": true
    "ASC": true
    "BETWEEN": true
    "BY": true
    "CASE": true
    "CAST": true
    "CHECK": true
    "CONSTRAINT": true
    "CREATE": true
    "DATABASE": true
    "DEFAULT": true
    "COLUMN": true
    "TABLESPACE": true
    "PROCEDURE": true
    "FUNCTION": true
    "DELETE": true
    "DESC": true
    "DISTINCT": true
    "DROP": true
    "ELSE": true
    "EXPLAIN": true
    "EXCEPT": true
    "END": true
    "ESCAPE": true
    "EXISTS": true
    "FOR": true
    "FOREIGN": true
    "FROM": true
    "FULL": true
    "GROUP": true
    "HAVING": true
    "IN": true
    "INDEX": true
    "INNER": true
    "INSERT": true
    "INTERSECT": true
    "INTERVAL": true
    "INTO": true
    "IS": true
    "JOIN": true
    "KEY": true
    "LEFT": true
    "LIKE": true
    "LOCK": true
    "MINUS": true
    "NOT": true
    "NULL": true
    "ON": true
    "OR": true
    "ORDER": true
    "OUTER": true
    "PRIMARY": true
    "REFERENCES": true
    "RIGHT": true
    "SCHEMA": true
    "SELECT": true
    "SET": true
    "SOME": true
    "TABLE": true
    "THEN": true
    "TRUNCATE": true
    "UNION": true
    "UNIQUE": true
    "UPDATE": true
    "VALUES": true
    "VIEW": true
    "SEQUENCE": true
    "TRIGGER": true
    "USER": true
    "WHEN": true
    "WHERE": true
    "XOR": true
    "OVER": true
    "TO": true
    "USE": true
    "REPLACE": true
    "COMMENT": true
    "COMPUTE": true
    "WITH": true
    "GRANT": true
    "REVOKE": true
    "WHILE": true
    "DO": true
    "DECLARE": true
    "LOOP": true
    "LEAVE": true
    "ITERATE": true
    "REPEAT": true
    "UNTIL": true
    "OPEN": true
    "CLOSE": true
    "CURSOR": true
    "FETCH": true
    "OUT": true
    "INOUT": true
    "LIMIT": true
    "DUAL": true
    "FALSE": true
    "IF": true
    "KILL": true
    "TRUE": true
    "BINARY": true
    "SHOW": true
    "CACHE": true
    "ANALYZE": true
    "OPTIMIZE": true
    "ROW": true
    "BEGIN": true
    "DIV": true
    "MERGE": true
    "PARTITION": true
    "CONTINUE": true
    "UNDO": true
    "SQLSTATE": true
    "CONDITION": true
    "MOD": true
    "CONTAINS": true
    "RLIKE": true
    "FULLTEXT": true
`
	cfg, err := sqllint.LoadConfig([]byte(config))
	if err != nil {
		t.Fatal("failed to LoadConfig")
	}
	var s = script{
		Name:    "stmt-1",
		Content: "create table `show` (col1 datetime)",
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

	s = script{
		Name:    "stmt-2",
		Content: "create table t1 (`show` datetime)",
	}
	if err = linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatalf("failed to Input: %v", err)
	}
	lints = linter.Errors()[s.Name].Lints
	if len(lints) == 0 {
		t.Fatal("there should be errors")
	}
	t.Log(lints)
}
