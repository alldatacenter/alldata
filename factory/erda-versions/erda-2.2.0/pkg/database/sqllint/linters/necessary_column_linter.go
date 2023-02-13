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
	"fmt"
	"strings"

	"github.com/pingcap/parser/ast"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

type necessaryColumnLinter struct {
	baseLinter
	meta necessaryColumnLinterMeta
	c    sqllint.Config
}

// NecessaryColumnLinter checks if there is the necessary column.
func (hub) NecessaryColumnLinter(s script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var meta necessaryColumnLinterMeta
	if err := yaml.Unmarshal(c.Meta, &meta); err != nil {
		return nil, errors.Wrap(err, "failed to parse NecessaryColumnLinter.meta")
	}
	if len(meta.ColumnName) == 0 {
		return nil, errors.Errorf("no column name in NecessaryColumnLinter.meta")
	}
	return &necessaryColumnLinter{baseLinter: newBaseLinter(s), meta: meta, c: c}, nil
}

func (l *necessaryColumnLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	createStmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true
	}

	// to find the goal column which named l.meta.ColumnName
	for _, col := range createStmt.Cols {
		for _, name := range l.meta.ColumnName {
			if col.Name != nil && strings.EqualFold(col.Name.String(), name) {
				return in, true
			}
		}
	}

	l.err = linterror.New(l.s, l.text, fmt.Sprintf("missing necessary column, alias: %s, meta.ColumnName: %s", l.c.Alias, l.meta.ColumnName),
		func(line []byte) bool {
			return false
		})

	return in, true
}

func (l *necessaryColumnLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, true
}

func (l *necessaryColumnLinter) Error() error {
	return l.err
}

type necessaryColumnLinterMeta struct {
	ColumnName []string `json:"columnName" yaml:"columnName"`
}
