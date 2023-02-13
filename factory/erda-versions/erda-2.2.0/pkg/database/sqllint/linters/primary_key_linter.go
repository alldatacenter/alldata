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

type primaryKeyLinter struct {
	baseLinter
	meta primaryKeyLinterMeta
}

// PrimaryKeyLinter checks if the primary key is named as the specified name.
func (hub) PrimaryKeyLinter(script script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var l = primaryKeyLinter{
		baseLinter: newBaseLinter(script),
		meta:       primaryKeyLinterMeta{},
	}
	if err := yaml.Unmarshal(c.Meta, &l.meta); err != nil {
		l.meta.ColumnName = "example"
		out, _ := yaml.Marshal(l.meta)
		return nil, errors.Wrapf(err, fmt.Sprintf("failed to parse PrimaryKeyLinter.meta. Example:\n%s\n", string(out)))
	}
	return &l, nil
}

func (l *primaryKeyLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	stmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true
	}

	// check l.meta.ColumnName column is whether primary key or not
	for _, constraint := range stmt.Constraints {
		if constraint.Tp == ast.ConstraintPrimaryKey {
			for _, key := range constraint.Keys {
				if key.Column != nil && key.Column.Name.String() == l.meta.ColumnName {
					return in, true
				}
			}
		}
	}

	for _, col := range stmt.Cols {
		if col.Name == nil || !strings.EqualFold(col.Name.String(), l.meta.ColumnName) {
			continue
		}

		// check id column is whether defined to be primary key in ColDef
		for _, opt := range col.Options {
			if opt.Tp == ast.ColumnOptionPrimaryKey {
				return in, true
			}
		}

		// check id column is whether defined bo be primary key in constraint
		l.err = linterror.New(l.s, l.text, fmt.Sprintf("primary key error, it should be: PRIMARY KEY (%s)", l.meta.ColumnName), func(line []byte) bool {
			return bytes.Contains(bytes.ToLower(line), []byte(l.meta.ColumnName))
		})
	}

	return in, true
}

func (l *primaryKeyLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *primaryKeyLinter) Error() error {
	return l.err
}

type primaryKeyLinterMeta struct {
	ColumnName string `json:"columnName" yaml:"columnName"`
}
