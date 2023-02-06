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

	"github.com/pingcap/parser/ast"
	"gorm.io/gorm"

	"github.com/erda-project/erda/pkg/database/sqlparser/snapshot"
)

// Schema is the set of TableDefinitions.
// Is presents the status of the set of some tables.
type Schema struct {
	TableDefinitions map[string]*TableDefinition
}

func NewSchema() *Schema {
	return &Schema{TableDefinitions: make(map[string]*TableDefinition)}
}

func (s *Schema) Enter(in ast.Node) (ast.Node, bool) {
	switch stmt := in.(type) {
	case *ast.CreateTableStmt:
		s.TableDefinitions[stmt.Table.Name.String()] = NewTableDefinition(stmt)

	case *ast.DropTableStmt:
		for _, table := range stmt.Tables {
			delete(s.TableDefinitions, table.Name.String())
		}

	case *ast.AlterTableStmt:
		tableDefinition, ok := s.TableDefinitions[stmt.Table.Name.String()]
		if ok {
			in.Accept(tableDefinition)
		}

	default:

	}

	return in, false
}

func (s *Schema) Leave(in ast.Node) (ast.Node, bool) {
	return in, true
}

func (s *Schema) Equal(o *Schema) *Equal {
	if len(s.TableDefinitions) != len(o.TableDefinitions) {
		return &Equal{
			equal:  false,
			reason: fmt.Sprintf("expected numbers of tables: %v, actual: %v", len(s.TableDefinitions), len(o.TableDefinitions)),
		}
	}

	var (
		reasons string
		eq      = true
	)
	for tableName, sDef := range s.TableDefinitions {
		oDef, ok := o.TableDefinitions[tableName]
		if !ok {
			eq = false
			reasons += fmt.Sprintf("table %s is expected but missing in actual\n", tableName)
			continue
		}
		if equal := sDef.Equal(oDef); !equal.Equal() {
			eq = false
			reasons += fmt.Sprintf("table: %s, %s\n", tableName, equal.Reason())
		}
	}

	return &Equal{equal: eq, reason: reasons}
}

func (s *Schema) EqualWith(db *gorm.DB) *Equal {
	if len(s.TableDefinitions) == 0 {
		return &Equal{equal: true}
	}

	cloud := NewSchema()
	for tableName := range s.TableDefinitions {
		raw := "SHOW CREATE TABLE " + tableName
		this := db.Raw(raw)
		if err := this.Error; err != nil {
			return &Equal{
				equal:  false,
				reason: fmt.Sprintf("failed to exec %s", raw),
			}
		}
		var _ig, create string
		if err := this.Row().Scan(&_ig, &create); err != nil {
			return &Equal{
				equal:  false,
				reason: fmt.Sprintf("failed to Scan create table stmt, raw: %s", raw),
			}
		}
		node, err := snapshot.ParseCreateTableStmt(create)
		if err != nil {
			return &Equal{
				equal:  false,
				reason: fmt.Sprintf("failed to ParseOneStmt: %s, raw: %s", create, raw),
			}
		}
		node.Accept(cloud)
	}

	return s.Equal(cloud)
}
