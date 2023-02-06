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

func TestFieldTypeEqual(t *testing.T) {
	var create = `create table t (
                   a varchar(255),
                   b bigint(20),
                   c varchar(32),
                   d bigint,
                   e decimal(5,2),
                   f decimal(10),
                   g varchar(255) COLLATE latin1_german2_ci,
                   h varchar(255) COLLATE utf8_unicode_ci,
                   i ENUM('x-small', 'small', 'medium', 'large', 'x-large'),
                   j ENUM('x-small', 'small', 'medium', 'large'),
                   k ENUM('x-small', 'small', 'medium', 'large', 'x-big'),
                   l bigint(20) unsigned
)`
	stmt, err := parser.New().ParseOneStmt(create, "", "")
	if err != nil {
		t.Fatal("failed to ParseOneStmt", err)
	}
	createTableStmt := stmt.(*ast.CreateTableStmt)
	a := createTableStmt.Cols[0]
	b := createTableStmt.Cols[1]
	c := createTableStmt.Cols[2]
	d := createTableStmt.Cols[3]
	e := createTableStmt.Cols[4]
	f := createTableStmt.Cols[5]
	//g := createTableStmt.Cols[6]
	//h := createTableStmt.Cols[7]
	i := createTableStmt.Cols[8]
	j := createTableStmt.Cols[9]
	k := createTableStmt.Cols[10]
	l := createTableStmt.Cols[11]

	if eq := migrator.FieldTypeEqual(a.Tp, b.Tp); eq.Equal() {
		t.Fatalf("error assert equal between col a and col b")
	}
	if eq := migrator.FieldTypeEqual(a.Tp, c.Tp); eq.Equal() {
		t.Fatal("error assert equal between col a and col c", eq.Reason())
	}
	if eq := migrator.FieldTypeEqual(b.Tp, d.Tp); !eq.Equal() {
		t.Fatalf("error assert equal between col b and col c, reason: %s", eq.Reason())
	}
	if eq := migrator.FieldTypeEqual(a.Tp, d.Tp); eq.Equal() {
		t.Fatal("error assert equal between col a and col d")
	}
	if eq := migrator.FieldTypeEqual(e.Tp, f.Tp); eq.Equal() {
		t.Fatal("error assert equal between col e and col f")
	}
	//if eq := migrator.FieldTypeEqual(a.Tp, g.Tp); eq.Equal() {
	//	t.Logf("a collate: %+v, g collate: %+v", a.Tp, g.Tp)
	//	t.Fatal("error assert equal between col a and col g")
	//}
	//if eq := migrator.FieldTypeEqual(g.Tp, h.Tp); eq.Equal() {
	//	t.Fatal("error assert equal between col g and col h")
	//}
	if eq := migrator.FieldTypeEqual(i.Tp, j.Tp); eq.Equal() {
		t.Fatal("error assert equal between col i and col j")
	}
	if eq := migrator.FieldTypeEqual(i.Tp, k.Tp); eq.Equal() {
		t.Fatal("error assert equal between col i and col k")
	}
	if eq := migrator.FieldTypeEqual(b.Tp, l.Tp); eq.Equal() {
		t.Fatal("error assert equal between col b and bol l")
	}
}
