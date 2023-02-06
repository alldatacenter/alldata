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

package migrator_test

import (
	"testing"

	"github.com/pingcap/parser/ast"

	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
)

func TestNewScript(t *testing.T) {
	script, err := migrator.NewScript("..", "testdata/dice_base.sql")
	if err != nil {
		t.Fatal(err)
	}

	for _, dml := range script.DMLNodes() {
		stmt, ok := dml.(*ast.SetStmt)
		if ok {
			t.Log(stmt.Text())
		}
	}
}

func TestScript_IsEmpty(t *testing.T) {
	var s = new(migrator.Script)
	if !s.IsEmpty() {
		t.Fatal("the script is empty")
	}

	s.Rawtext = []byte("select 1 from t;")
	if s.IsEmpty() {
		t.Fatal("the script is not empty")
	}
}
