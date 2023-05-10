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

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

// explicitCollationLinter lints if user set the collation explicitly
type explicitCollationLinter struct {
	baseLinter
}

// ExplicitCollationLinter returns an explicitCollationLinter
func (hub) ExplicitCollationLinter(script script.Script, _ sqllint.Config) (sqllint.Rule, error) {
	return &explicitCollationLinter{baseLinter: newBaseLinter(script)}, nil
}

func (l *explicitCollationLinter) Enter(in ast.Node) (node ast.Node, skipChildren bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	stmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true
	}

	// check if the collation is in the table options
	for _, opt := range stmt.Options {
		if opt == nil {
			continue
		}
		if opt.Tp == ast.TableOptionCollate {
			l.err = linterror.New(l.s, l.text, "should not set the collation in CreateTableStmt", func(line []byte) bool {
				return bytes.Contains(line, []byte(opt.StrValue))
			})
			return in, true
		}
	}

	// check if the collation is in the columns' options
	for _, col := range stmt.Cols {
		if col == nil {
			continue
		}
		if col.Tp != nil && col.Tp.Charset != "" {
			l.err = linterror.New(l.s, l.text, "should not set the character in ColumnDef", func(line []byte) bool {
				return bytes.Contains(bytes.ToLower(line), bytes.ToLower([]byte("character"))) && bytes.Contains(line, []byte(col.Tp.Charset))
			})
			return in, true
		}
		for _, opt := range col.Options {
			if opt == nil {
				continue
			}
			if opt.Tp == ast.ColumnOptionCollate {
				l.err = linterror.New(l.s, l.text, "should not set the collation in ColumnDef", func(line []byte) bool {
					return bytes.Contains(line, []byte(opt.StrValue))
				})
				return in, true
			}
		}
	}

	return in, true
}

func (l *explicitCollationLinter) Leave(in ast.Node) (node ast.Node, ok bool) {
	return in, true
}
