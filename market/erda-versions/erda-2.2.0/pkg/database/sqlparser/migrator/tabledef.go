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

package migrator

import (
	"fmt"
	"sort"
	"strings"

	"github.com/pingcap/parser/ast"
)

// TableDefinition is the table definition (CreateTableStmt) object.
// It can be accepted by SQL AST Node and then to update it's status.
type TableDefinition struct {
	CreateStmt *ast.CreateTableStmt
}

func NewTableDefinition(stmt *ast.CreateTableStmt) *TableDefinition {
	return &TableDefinition{CreateStmt: stmt}
}

func (d *TableDefinition) Enter(in ast.Node) (ast.Node, bool) {
	alter, ok := in.(*ast.AlterTableStmt)
	if !ok {
		return in, false
	}

	// note: only AddColumns, DropColumns, ModifyColumn, ChangeColumn are considered to change column and column type.
	// other specs either do not conform to the ErdaMySQLLint or will not change the column type.
	for _, spec := range alter.Specs {
		switch spec.Tp {
		case ast.AlterTableAddColumns:
			d.CreateStmt.Cols = append(d.CreateStmt.Cols, spec.NewColumns...)
		case ast.AlterTableDropColumn:
			if spec.OldColumnName == nil {
				continue
			}
			for i := 0; i < len(d.CreateStmt.Cols); i++ {
				if spec.OldColumnName.String() == d.CreateStmt.Cols[i].Name.String() {
					d.CreateStmt.Cols = append(d.CreateStmt.Cols[:i], d.CreateStmt.Cols[i:]...)
					break
				}
			}
		case ast.AlterTableModifyColumn, ast.AlterTableChangeColumn:
			columnDef := spec.NewColumns[0]
			if columnDef.Tp != nil {
				for _, col := range d.CreateStmt.Cols {
					if col.Name.String() == columnDef.Name.String() {
						col.Tp = columnDef.Tp
					}
				}
			}

		default:
			continue
		}
	}

	return in, false
}

func (d *TableDefinition) Leave(in ast.Node) (ast.Node, bool) {
	return in, true
}

func (d *TableDefinition) Equal(o *TableDefinition) *Equal {
	var (
		reasons string
		eq      = true
	)

	if len(d.CreateStmt.Cols) != len(o.CreateStmt.Cols) {
		sort.Slice(d.CreateStmt.Cols, func(i, j int) bool {
			return d.CreateStmt.Cols[i].Name.String() < d.CreateStmt.Cols[j].Name.String()
		})
		sort.Slice(o.CreateStmt.Cols, func(i, j int) bool {
			return o.CreateStmt.Cols[i].Name.String() < o.CreateStmt.Cols[j].Name.String()
		})
		missing, unexpected := findUnexpected(o.CreateStmt.Cols, d.CreateStmt.Cols)
		return &Equal{
			equal: false,
			reason: fmt.Sprintf("The number of columns in the two tables is inconsistent, %v != %v, missing: %v, unexpected: %v",
				len(o.CreateStmt.Cols), len(d.CreateStmt.Cols), missing, unexpected),
		}
	}

	var (
		dCols = make(map[string]*ast.ColumnDef, len(d.CreateStmt.Cols))
		oCols = make(map[string]*ast.ColumnDef, len(o.CreateStmt.Cols))
	)
	for _, col := range d.CreateStmt.Cols {
		dCols[col.Name.String()] = col
	}
	for _, col := range o.CreateStmt.Cols {
		oCols[col.Name.String()] = col
	}

	for name, dCol := range dCols {
		oCol, ok := oCols[name]
		if !ok {
			eq = false
			reasons += fmt.Sprintf("the column is missing in actual, column name: %s, ", name)
			continue
		}
		if equal := FieldTypeEqual(dCol.Tp, oCol.Tp); !equal.Equal() {
			eq = false
			reasons += fmt.Sprintf("column: %s, %s, ", name, equal.Reason())
		}
	}

	return &Equal{
		equal:  eq,
		reason: strings.TrimRight(reasons, ", "),
	}
}

func findUnexpected(expected, actual []*ast.ColumnDef) (missing, unexpected []string) {
	var (
		expectedM = make(map[string]bool)
		actualM   = make(map[string]bool)
	)
	for _, col := range expected {
		expectedM[col.Name.String()] = true
	}
	for _, col := range actual {
		actualM[col.Name.String()] = true
	}
	for name := range expectedM {
		if _, ok := actualM[name]; !ok {
			missing = append(missing, name)
		}
	}
	for name := range actualM {
		if _, ok := expectedM[name]; !ok {
			unexpected = append(unexpected, name)
		}
	}
	return
}
