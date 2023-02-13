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

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"

	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
)

const manyBlocks = `
create table t1 (id bigint);

create table t2 (id bigint);

insert into t1 values(0);

insert into t2 values(1);

alter table t1 add column name varchar(255);

update t1 set id=0;
`

func TestAppendBlock(t *testing.T) {
	nodes, warns, err := parser.New().Parse(manyBlocks, "", "")
	if err != nil {
		t.Fatal("failed to parse", err)
	}
	if len(warns) > 0 {
		t.Logf("warns: %v", warns)
	}

	var blocks []migrator.Block
	for _, node := range nodes {
		switch node.(type) {
		case ast.DDLNode:
			blocks = migrator.AppendBlock(blocks, node, migrator.DDL)
		case ast.DMLNode:
			blocks = migrator.AppendBlock(blocks, node, migrator.DML)
		}
	}
	if length := len(blocks); length != 4 {
		t.Fatalf("length of blocks error: %v", length)
	}

	var (
		actualTypes       = []migrator.StmtType{migrator.DDL, migrator.DML, migrator.DDL, migrator.DML}
		actualNodesLength = []int{2, 2, 1, 1}
	)
	for i := range blocks {
		if typ := blocks[i]; typ.Type() != actualTypes[i] {
			t.Fatalf("blocks[%v] type is error, type: %s", i, typ.Type())
		}
		if length := len(blocks[i].Nodes()); length != actualNodesLength[i] {
			t.Fatalf("blocks[%v] nodes length is error, lenght: %v", i, length)
		}
	}
}
