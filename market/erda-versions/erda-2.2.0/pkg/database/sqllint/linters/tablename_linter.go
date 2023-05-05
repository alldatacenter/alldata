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
	"regexp"
	"strings"

	"github.com/pingcap/parser/ast"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

type tableNameLinter struct {
	baseLinter
	meta tableNameLinterMeta
}

func (hub) TableNameLinter(script script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var l = tableNameLinter{
		baseLinter: newBaseLinter(script),
		meta:       tableNameLinterMeta{},
	}
	if err := yaml.Unmarshal(c.Meta, &l.meta); err != nil {
		return nil, errors.Wrap(err, "failed to parse TableNameLinter.meta")
	}
	if len(l.meta.Patterns) == 0 {
		return nil, errors.New("no table name pattern in TableNameLinter.meta")
	}
	return &l, nil
}

func (l *tableNameLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	stmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true // only to check table name on CreateStmt
	}

	name := extractCreateName(stmt)
	if name == "" {
		return in, true
	}
	for _, pat := range l.meta.Patterns {
		if ok, _ := regexp.Match(pat, []byte(name)); ok {
			return in, true
		}
	}
	l.err = linterror.New(l.s, l.text, fmt.Sprintf("invalid table name, it should match one of these patterns:\n%s", strings.Join(l.meta.Patterns, "\n")),
		func(line []byte) bool {
			return bytes.Contains(bytes.ToLower(line), bytes.ToLower([]byte(name)))
		})

	return in, true
}

func (l *tableNameLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *tableNameLinter) Error() error {
	return l.err
}

type tableNameLinterMeta struct {
	Patterns []string `json:"patterns" yaml:"patterns"`
}

func extractCreateName(stmt *ast.CreateTableStmt) string {
	if stmt == nil {
		return ""
	}
	if stmt.Table == nil {
		return ""
	}
	return stmt.Table.Name.String()
}
