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

package linters

import (
	"bytes"

	"github.com/pingcap/parser/ast"
	"github.com/pingcap/parser/mysql"
	"github.com/pingcap/parser/types"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
	"github.com/erda-project/erda/pkg/swagger/ddlconv"
)

const (
	maxSingleIndexLength = 767
	maxJointIndexLength  = 3072

	varcharDefaultFlen = 255
	charDefaultFlen    = 255
)

type indexLengthLinter struct {
	baseLinter
}

func (hub) IndexLengthLinter(script script.Script, _ sqllint.Config) (sqllint.Rule, error) {
	return &indexLengthLinter{newBaseLinter(script)}, nil
}

func (l *indexLengthLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	stmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true
	}

	var (
		// colStorage key is col name, value is col storage size
		colStorage      = make(map[string]int)
		colsCanNotIndex = make(map[string]error)
		colNames        = make(map[string]*ast.ColumnDef, len(stmt.Cols))
	)
	for _, col := range stmt.Cols {
		colName := ddlconv.ExtractColName(col)
		if colName == "" || col.Tp == nil {
			continue
		}
		colNames[colName] = col

		switch col.Tp.Tp {
		case mysql.TypeDecimal, mysql.TypeNewDecimal:
			flen := col.Tp.Flen
			if col.Tp.Decimal > col.Tp.Flen {
				flen = col.Tp.Decimal
			}
			colStorage[colName] = flen + 2

		case mysql.TypeTiny:
			colStorage[colName] = 1

		// TypeShort is type SMALLINT
		case mysql.TypeShort:
			colStorage[colName] = 2

		// TypeLong is type INT
		case mysql.TypeLong:
			colStorage[colName] = 4

		case mysql.TypeFloat:
			colStorage[colName] = 4
			if col.Tp.Flen >= 25 {
				colStorage[colName] = 8
			}

		case mysql.TypeDouble:
			colStorage[colName] = 8

		case mysql.TypeNull:
			colsCanNotIndex[colName] = linterror.New(l.s, l.text, "do not index on type NULL",
				func(line []byte) bool { return bytes.Contains(bytes.ToLower(line), []byte(colName)) })

		// For TIME, DATETIME, and TIMESTAMP columns, the storage required for tables created before MySQL 5.6.4 differs
		// from tables created from 5.6.4 on. This is due to a change in 5.6.4 that permits these types to have a
		// fractional part, which requires from 0 to 3 bytes
		case mysql.TypeTimestamp:
			colStorage[colName] = 4 + 3

		// TypeLongLong is BIGINT
		case mysql.TypeLonglong:
			colStorage[colName] = 8

		// TypeInt24 is MEDIUMINT
		case mysql.TypeInt24:
			colStorage[colName] = 3

		case mysql.TypeDate, mysql.TypeNewDate:
			colStorage[colName] = 3

		// TypeDuration is TIME
		case mysql.TypeDuration:
			colStorage[colName] = 3 + 3

		case mysql.TypeDatetime:
			colStorage[colName] = 5 + 3

		case mysql.TypeYear:
			colStorage[colName] = 1

		case mysql.TypeVarchar:
			flen := varcharDefaultFlen
			if col.Tp.Flen != 0 {
				flen = col.Tp.Flen
			}
			colStorage[colName] = flen*4 + 2

		// DO NOT INDEX ON BIT !
		case mysql.TypeBit:
			colsCanNotIndex[colName] = linterror.New(l.s, l.text, "do not index on type BIT",
				func(line []byte) bool { return bytes.Contains(bytes.ToLower(line), []byte(colName)) })

		// DO NOT INDEX ON JSON
		case mysql.TypeJSON:
			colsCanNotIndex[colName] = linterror.New(l.s, l.text, "do not index on type JSON",
				func(line []byte) bool { return bytes.Contains(bytes.ToLower(line), []byte(colName)) })

		// 1 or 2 bytes, depending on the number of enumeration values (65,535 values maximum)
		case mysql.TypeEnum:
			colStorage[colName] = 2

		// 1, 2, 3, 4, or 8 bytes, depending on the number of set members (64 members maximum)
		case mysql.TypeSet:
			colStorage[colName] = 8

		// DO NOT INDEX ON BLOB
		case mysql.TypeTinyBlob, mysql.TypeMediumBlob, mysql.TypeLongBlob, mysql.TypeBlob:
			colsCanNotIndex[colName] = linterror.New(l.s, l.text, "do not index on type BLOB",
				func(line []byte) bool { return bytes.Contains(bytes.ToLower(line), []byte(colName)) })

		case mysql.TypeVarString:
			colsCanNotIndex[colName] = linterror.New(l.s, l.text, "do not index on type VAR_STRING",
				func(line []byte) bool { return bytes.Contains(bytes.ToLower(line), []byte(colName)) })

		// TypeString is type CHAR
		case mysql.TypeString:
			flen := charDefaultFlen
			if col.Tp.Flen != 0 {
				flen = col.Tp.Flen
			}
			colStorage[colName] = flen * 4

		// DO NOT INDEX ON GEOMETRY
		case mysql.TypeGeometry:
			colsCanNotIndex[colName] = linterror.New(l.s, l.text, "do not index on type GEOMETRY",
				func(line []byte) bool { return bytes.Contains(bytes.ToLower(line), []byte(colName)) })

		default:
			colsCanNotIndex[colName] = linterror.New(l.s, l.text, "do not index on type "+types.TypeStr(col.Tp.Tp),
				func(line []byte) bool { return bytes.Contains(bytes.ToLower(line), []byte(colName)) })
		}
	}
	for _, c := range stmt.Constraints {
		var indexLength int
		for _, key := range c.Keys {
			if key.Column == nil {
				l.err = linterror.New(l.s, l.text, "index key's column name not found", func(line []byte) bool {
					return true
				})
				return in, true
			}

			col, ok := colNames[key.Column.Name.String()]
			if !ok {
				l.err = linterror.New(l.s, l.text, "index key out of columns definitions", func(line []byte) bool {
					return bytes.Contains(bytes.ToLower(line), []byte(key.Column.Name.String()))
				})
				return in, true
			}

			if err, ok := colsCanNotIndex[key.Column.Name.String()]; ok {
				l.err = err
				return in, true
			}

			storage, ok := colStorage[key.Column.Name.String()]
			if !ok {
				l.err = linterror.New(l.s, l.text, "storage of the key not found", func(line []byte) bool {
					return bytes.Contains(bytes.ToLower(line), []byte(key.Column.Name.String()))
				})
				return in, true
			}

			// if the length is specified when defining the index and col type is VARCHAR or CHAR, re-calculate storage
			if key.Length > 0 {
				switch col.Tp.Tp {
				case mysql.TypeVarchar:
					storage = key.Length*4 + 2
				case mysql.TypeString:
					storage = key.Length * 4
				}
			}

			indexLength += storage
		}

		// single index
		if len(c.Keys) == 1 && indexLength > maxSingleIndexLength {
			l.err = linterror.New(l.s, l.text, "index length error: single column index length can not bigger than 767",
				func(line []byte) bool { return bytes.Contains(bytes.ToLower(line), []byte(c.Keys[0].Column.String())) })
			return in, true
		}
		// joint index
		if len(c.Keys) > 1 && indexLength > maxJointIndexLength {
			_, num := linterror.CalcLintLine(l.s.Data(), []byte(l.text), func(_ []byte) bool {
				return false
			})
			l.err = linterror.LintError{
				Stmt:   l.text,
				Lint:   "index length error: joint index length can not bigger than 3072",
				LintNo: num,
			}
			return in, true
		}
	}

	return in, true
}

func (l *indexLengthLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *indexLengthLinter) Error() error {
	return l.err
}
