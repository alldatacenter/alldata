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
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
	"github.com/erda-project/erda/pkg/swagger/ddlconv"
)

type keywordsLinter struct {
	baseLinter
	meta map[string]bool
}

func (hub) KeywordsLinter(script script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var l = keywordsLinter{
		baseLinter: newBaseLinter(script),
		meta:       make(map[string]bool),
	}
	if err := yaml.Unmarshal(c.Meta, &l.meta); err != nil {
		return nil, errors.Wrap(err, "failed to parse KeywordsLinter.meta")
	}
	for k, v := range l.meta {
		l.meta[strings.ToUpper(k)] = v
	}
	return &l, nil
}

func (l *keywordsLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	switch stmt := in.(type) {
	case *ast.CreateTableStmt:
		name := ddlconv.ExtractCreateName(stmt)
		if name == "" {
			return in, false
		}
		if v, ok := l.meta[strings.ToUpper(name)]; ok && v {
			l.err = linterror.New(l.s, l.text, "invalid table name: can not use MySQL keywords to be table name",
				func(_ []byte) bool {
					return false
				})
			return in, true
		}
	case *ast.ColumnDef:
		name := ddlconv.ExtractColName(stmt)
		if name == "" {
			return in, false
		}
		if v, ok := l.meta[strings.ToUpper(name)]; ok && v {
			l.err = linterror.New(l.s, l.text, "invalid column name: can not use MySQL keywords to be column name",
				func(line []byte) bool {
					return bytes.Contains(bytes.ToLower(line), bytes.ToLower([]byte(name)))
				})
			return in, true
		}
	default:
		return in, false
	}

	return in, false
}

func (l *keywordsLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *keywordsLinter) Error() error {
	return l.err
}
