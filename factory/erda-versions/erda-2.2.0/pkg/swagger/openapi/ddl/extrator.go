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

package ddl

import (
	"strings"

	"github.com/pingcap/parser/ast"
	driver "github.com/pingcap/tidb/types/parser_driver"
)

func ExtractCreateName(stmt *ast.CreateTableStmt) string {
	if stmt == nil {
		return ""
	}
	if stmt.Table == nil {
		return ""
	}
	return stmt.Table.Name.String()
}

func ExtractRename(stmt *ast.RenameTableStmt) (newName, oldName string) {
	if stmt == nil {
		return "", ""
	}
	if stmt.NewTable != nil {
		newName = stmt.NewTable.Name.String()
	}
	if stmt.OldTable != nil {
		oldName = stmt.OldTable.Name.String()
	}
	return
}

func ExtractDropNames(stmt *ast.DropTableStmt) []string {
	if stmt == nil {
		return nil
	}
	if len(stmt.Tables) == 0 {
		return nil
	}
	var names []string
	for _, t := range stmt.Tables {
		names = append(names, t.Name.String())
	}

	return names
}

func ExtractColName(col *ast.ColumnDef) string {
	if col == nil {
		return ""
	}
	if col.Name == nil {
		return ""
	}
	return col.Name.String()
}

func ExtractColType(col *ast.ColumnDef) string {
	if col == nil {
		return ""
	}
	if col.Tp == nil {
		return ""
	}
	return col.Tp.String()
}

func ExtractColComment(col *ast.ColumnDef) string {
	if col == nil {
		return ""
	}
	for _, opt := range col.Options {
		switch opt.Tp {
		case ast.ColumnOptionComment:
			if opt.Expr != nil {
				return opt.Expr.(*driver.ValueExpr).GetString()
			}
		}
	}
	return ""
}

func ExtractAlterTableName(stmt *ast.AlterTableStmt) string {
	if stmt == nil || stmt.Table == nil {
		return ""
	}
	return stmt.Table.Name.String()
}

func ExtractAlterTableAddColName(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableAddColumns || len(stmt.NewColumns) == 0 {
		return ""
	}
	return ExtractColName(stmt.NewColumns[0])
}

func ExtractAlterTableAddColType(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableAddColumns || len(stmt.NewColumns) == 0 {
		return ""
	}
	return ExtractColType(stmt.NewColumns[0])
}

func ExtractAlterTableAddColComment(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableAddColumns || len(stmt.NewColumns) == 0 {
		return ""
	}
	return ExtractColComment(stmt.NewColumns[0])
}

func ExtractAlterTableDropColName(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableDropColumn || stmt.OldColumnName == nil {
		return ""
	}
	return stmt.OldColumnName.Name.String()
}

func ExtractAlterTableModifyColName(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableModifyColumn || len(stmt.NewColumns) == 0 {
		return ""
	}
	return ExtractColName(stmt.NewColumns[0])
}

func ExtractAlterTableModifyColType(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableModifyColumn || len(stmt.NewColumns) == 0 {
		return ""
	}
	return ExtractColType(stmt.NewColumns[0])
}

func ExtractAlterTableModifyColComment(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableModifyColumn || len(stmt.NewColumns) == 0 {
		return ""
	}
	return ExtractColComment(stmt.NewColumns[0])
}

func ExtractAlterTableChangeColOldName(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableChangeColumn || stmt.OldColumnName == nil {
		return ""
	}
	return stmt.OldColumnName.Name.String()
}

func ExtractAlterTableChangeColNewName(stmt *ast.AlterTableSpec) string {
	if stmt == nil || stmt.Tp != ast.AlterTableChangeColumn || len(stmt.NewColumns) == 0 {
		return ""
	}
	return ExtractColName(stmt.NewColumns[0])
}

func ExtractAlterTableChangeColType(spec *ast.AlterTableSpec) string {
	if spec == nil || spec.Tp != ast.AlterTableChangeColumn || len(spec.NewColumns) == 0 {
		return ""
	}
	return ExtractColType(spec.NewColumns[0])
}

func ExtractAlterTableChangeColComment(spec *ast.AlterTableSpec) string {
	if spec == nil || spec.Tp != ast.AlterTableChangeColumn || len(spec.NewColumns) == 0 {
		return ""
	}
	return ExtractColComment(spec.NewColumns[0])
}

func mysqlType2OpenapiType(t string) string {
	if strings.Contains(t, "bool") || t == "tinyint(1)" {
		return "boolean"
	}
	if strings.Contains(t, "int") {
		return "integer"
	}
	if strings.Contains(t, "decimal") || strings.Contains(t, "float") || strings.Contains(t, "double") {
		return "number"
	}
	if strings.Contains(t, "char") || strings.Contains(t, "text") ||
		strings.Contains(t, "date") || strings.Contains(t, "time") {
		return "string"
	}

	return "string" // 可以都按 string 处理
}

func snake2LowerCamel(snake string) (s string) {
	words := strings.Split(snake, "_")

	for i, w := range words {
		s += snake2LowerCamelUpper(i, w)
	}

	return
}

func snake2LowerCamelUpper(idx int, word string) string {
	switch word {
	case "id":
		if idx == 0 {
			return word
		}
		return strings.ToUpper(word)
	case "http":
		if idx == 0 {
			return word
		}
		return strings.ToUpper(word)
	case "https":
		if idx == 0 {
			return word
		}
		return "HTTPs"
	case "rpc":
		if idx == 0 {
			return word
		}
		return strings.ToUpper(word)
	case "grpc":
		if idx == 0 {
			return "gRPC"
		}
		return strings.ToUpper(word)
	default:
		if idx == 0 {
			return strings.ToLower(word)
		}
		return strings.ToUpper(word[0:1]) + word[1:]
	}
}

func genExample(name, t string) interface{} {
	if t == "string" {
		return name + "_example"
	}
	if t == "boolean" {
		return true
	}
	return 0
}
