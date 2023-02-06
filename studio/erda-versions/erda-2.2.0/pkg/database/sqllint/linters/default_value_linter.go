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
	"fmt"
	"strings"

	"github.com/pingcap/parser/ast"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

type defaultValueLinter struct {
	baseLinter
	meta defaultValueLinterMeta
}

func (hub) DefaultValueLinter(script script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var l = defaultValueLinter{
		baseLinter: newBaseLinter(script),
		meta:       defaultValueLinterMeta{},
	}
	if err := yaml.Unmarshal(c.Meta, &l.meta); err != nil {
		return nil, errors.Wrap(err, "failed to DefaultValueLinter.meta")
	}
	return &l, nil
}

func (l *defaultValueLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	createStmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true
	}

	for _, col := range createStmt.Cols {
		if col.Name == nil || !strings.EqualFold(col.Name.String(), l.meta.ColumnName) {
			continue
		}

		for _, opt := range col.Options {
			if opt.Tp == ast.ColumnOptionDefaultValue {
				if expr, ok := opt.Expr.(*ast.FuncCallExpr); ok &&
					strings.EqualFold(expr.FnName.String(), "CURRENT_TIMESTAMP") {
					return in, true
				}
			}
		}

		l.err = linterror.New(l.s, l.text, fmt.Sprintf("default value error. %s's default value shoud be %s", l.meta.ColumnName, l.meta.DefaultValue),
			func(line []byte) bool {
				return bytes.Contains(bytes.ToLower(line), []byte(l.meta.ColumnName))
			})
		return in, true

	}

	return in, true
}

func (l *defaultValueLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, true
}

func (l *defaultValueLinter) Error() error {
	return l.err
}

type defaultValueLinterMeta struct {
	ColumnName   string `json:"columnName" yaml:"columnName"`
	DefaultValue string `json:"defaultValue" yaml:"defaultValue"`
}
