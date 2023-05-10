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

package pygrator_test

import (
	"bytes"
	"os"
	"testing"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"

	"github.com/erda-project/erda/pkg/database/sqlparser/pygrator"
)

func TestGenMigration(t *testing.T) {
	stmt, err := parser.New().ParseOneStmt(createStmt, "", "")
	if err != nil {
		t.Fatalf("failed to ParseOneStmt, err: %v", err)
	}
	create := stmt.(*ast.CreateTableStmt)
	model, err := pygrator.CreateTableStmtToModel(create)
	if err != nil {
		t.Fatalf("failed to CreateTableStmtToModel: %v", err)
	}
	var buf = bytes.NewBuffer(nil)
	if err = pygrator.GenModel(buf, *model); err != nil {
		t.Fatalf("failed to GenModel: %v", err)
	}

	var migration = pygrator.DeveloperScript{
		Models: []string{buf.String(), buf.String()},
	}
	if err = pygrator.GenDeveloperScript(os.Stdout, migration); err != nil {
		t.Fatalf("failed to GenDeveloperScript: %v", err)
	}
}
