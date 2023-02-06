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
	"strings"

	"github.com/pingcap/parser/ast"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

type varcharLengthLinter struct {
	baseLinter
}

func (hub) VarcharLengthLinter(s script.Script, c sqllint.Config) (sqllint.Rule, error) {
	return &varcharLengthLinter{newBaseLinter(s)}, nil
}

func (l *varcharLengthLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	col, ok := in.(*ast.ColumnDef)
	if !ok {
		return in, false
	}

	if col.Tp != nil &&
		strings.Contains(strings.ToLower(col.Tp.String()), "varchar") &&
		col.Tp.Flen > 5000 {
		l.err = linterror.New(l.s, l.text, "column type error: VARCHAR length can not be bigger than 5000, if you need please use TEXT",
			func(line []byte) bool {
				return bytes.Contains(bytes.ToLower(line), []byte(col.Tp.String()))

			})
	}

	return in, true
}

func (l *varcharLengthLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *varcharLengthLinter) Error() error {
	return l.err
}
