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
	"github.com/erda-project/erda/pkg/swagger/ddlconv"
)

type floatDoubleLinter struct {
	baseLinter
}

// FloatDoubleLinter
// You can not use fload and double type, please use decimal.
func (hub) FloatDoubleLinter(script script.Script, _ sqllint.Config) (sqllint.Rule, error) {
	return &floatDoubleLinter{newBaseLinter(script)}, nil
}

func (l *floatDoubleLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	col, ok := in.(*ast.ColumnDef)
	if !ok {
		return in, false
	}

	t := ddlconv.ExtractColType(col)
	t = strings.ToLower(t)
	if strings.Contains(t, "float") || strings.Contains(t, "double") {
		l.err = linterror.New(l.s, l.text, "field type error: decimal type should use DECIMAL instead of FLOAT OR DOUBLE",
			func(line []byte) bool {
				return bytes.Contains(bytes.ToLower(line), []byte("float")) ||
					bytes.Contains(bytes.ToLower(line), []byte("double"))
			})
	}

	return in, false
}

func (l *floatDoubleLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, true
}

func (l *floatDoubleLinter) Error() error {
	return l.err
}
