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

type columnNameLinter struct {
	baseLinter
	meta columnNameLinterMeta
}

func (hub) ColumnNameLinter(script script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var l = columnNameLinter{
		baseLinter: newBaseLinter(script),
		meta:       columnNameLinterMeta{},
	}
	if err := yaml.Unmarshal(c.Meta, &l.meta); err != nil {
		return nil, errors.Wrap(err, "failed to parse ColumnNameLinter.meta")
	}
	return &l, nil
}

func (l *columnNameLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	col, ok := in.(*ast.ColumnDef)
	if !ok {
		return in, false
	}

	if col.Name == nil {
		return in, true
	}

	name := col.Name.OrigColName()
	if name == "" {
		return in, true
	}

	defer func() {
		if l.err == nil {
			return
		}
		l.err = linterror.New(l.s, l.text, l.err.(linterror.LintError).Lint,
			func(line []byte) bool {
				return bytes.Contains(bytes.ToLower(line), bytes.ToLower([]byte(name)))
			})
	}()

	for _, pat := range l.meta.Patterns {
		if ok, _ := regexp.Match(pat, []byte(name)); ok {
			return in, true
		}
	}
	l.err = linterror.LintError{
		ScriptName: l.s.Name(),
		Stmt:       l.text,
		Lint:       fmt.Sprintf("invalid column name, it should match one of these patterns:\n%s", strings.Join(l.meta.Patterns, "\n")),
		Line:       "",
		LintNo:     0,
	}
	return in, true
}

func (l *columnNameLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *columnNameLinter) Error() error {
	return l.err
}

type columnNameLinterMeta struct {
	Patterns []string `json:"patterns" yaml:"patterns"`
}
