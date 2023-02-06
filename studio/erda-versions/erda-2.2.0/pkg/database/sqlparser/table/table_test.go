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

package table_test

import (
	"testing"

	"github.com/pingcap/parser/ast"

	"github.com/erda-project/erda/pkg/database/sqlparser/table"
)

func TestTable_Append(t *testing.T) {
	var (
		tbl  table.Table
		node = new(ast.CreateTableStmt)
	)
	tbl.Append(node)
	nodes := tbl.Nodes()
	if len(nodes) != 1 {
		t.Fatal("error")
	}
	if _, ok := nodes[0].(*ast.CreateTableStmt); !ok {
		t.Fatal("error")
	}
}
