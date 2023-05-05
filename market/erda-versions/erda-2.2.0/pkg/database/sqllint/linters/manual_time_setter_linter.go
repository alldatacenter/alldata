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

	"github.com/pingcap/parser/ast"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

var (
	createdAt = "created_at"
	updatedAt = "updated_at"
)

// manualTimeSetterLinter lints if the user manually set the column created_at, updated_at
type manualTimeSetterLinter struct {
	baseLinter
	meta manualTimeSetterLinterMeta
}

// ManualTimeSetterLinter returns a manualTimeSetterLinter
// manualTimeSetterLinter lints if the user manually set the column created_at, updated_at
func (hub) ManualTimeSetterLinter(script script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var l = manualTimeSetterLinter{
		baseLinter: newBaseLinter(script),
	}
	if err := yaml.Unmarshal(c.Meta, &l.meta); err != nil {
		return nil, errors.Wrap(err, "failed to parse ManualTimeSetterLinter.meta")
	}
	return &l, nil
}

func (l *manualTimeSetterLinter) Enter(in ast.Node) (out ast.Node, skipChildren bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	getLint := func(line []byte) bool {
		return bytes.Contains(line, []byte(createdAt)) || bytes.Contains(line, []byte(l.meta.ColumnName))
	}

	switch stmt := in.(type) {
	case *ast.InsertStmt:
		for _, col := range stmt.Columns {
			if col.Name.String() == l.meta.ColumnName {
				l.err = linterror.New(l.s, l.text, fmt.Sprintf("not allowed to manual insert value into column %s", l.meta.ColumnName), getLint)
				return in, true
			}
		}
	case *ast.UpdateStmt:
		for _, col := range stmt.List {
			if col.Column.String() == l.meta.ColumnName {
				l.err = linterror.New(l.s, l.text, fmt.Sprintf("not allowed to manual insert value into column %s", l.meta.ColumnName), getLint)
				return in, true
			}
		}
	default:
		return in, true
	}

	return in, true
}

func (l *manualTimeSetterLinter) Leave(in ast.Node) (out ast.Node, ok bool) {
	return in, true
}

type manualTimeSetterLinterMeta struct {
	ColumnName string `json:"columnName" yaml:"columnName"`
}
