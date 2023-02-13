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

type foreignKeyLinter struct {
	baseLinter
}

func (hub) ForeignKeyLinter(script script.Script, _ sqllint.Config) (sqllint.Rule, error) {
	return &foreignKeyLinter{newBaseLinter(script)}, nil
}

func (l *foreignKeyLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	constraint, ok := in.(*ast.Constraint)
	if !ok {
		return in, false
	}

	if constraint.Tp == ast.ConstraintForeignKey {
		l.err = linterror.New(l.s, l.text, "used foreign key: all foreign key concept must be expressed at the application layer not at model layer",
			func(line []byte) bool {
				return bytes.Contains(bytes.ToLower(line), []byte("foreign"))
			})
	}

	return in, false
}

func (l *foreignKeyLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *foreignKeyLinter) Error() error {
	return l.err
}
