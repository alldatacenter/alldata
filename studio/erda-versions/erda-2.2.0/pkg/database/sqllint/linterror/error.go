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

package linterror

import (
	"bufio"
	"bytes"
	"fmt"
	"strings"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"

	"github.com/erda-project/erda/pkg/database/sqllint/script"
	"github.com/erda-project/erda/pkg/terminal/color"
)

type LintError struct {
	ScriptName string // the script name
	Stmt       string // the SQL statement
	Lint       string // the error lint info
	Line       string // the error line content
	LintNo     int    // the error line No
}

func New(script script.Script, stmt string, lint string, getLine func(line []byte) bool) LintError {
	line, num := CalcLintLine(script.Data(), []byte(stmt), getLine)
	return LintError{
		ScriptName: script.Name(),
		Stmt:       stmt,
		Lint:       lint,
		Line:       line,
		LintNo:     num,
	}
}

func (e LintError) Error() string {
	scanner := bufio.NewScanner(bytes.NewBufferString(strings.TrimLeft(e.Stmt, "\n")))
	buf := bytes.NewBuffer(nil)
	for scanner.Scan() {
		if line := scanner.Bytes(); bytes.Equal(line, []byte(e.Line)) {
			buf.WriteString("\n~~~> ")
			buf.WriteString(strings.TrimLeft(color.Red(e.Line), "\n"))
		} else {
			buf.WriteString("\n|->  ")
			buf.Write(bytes.TrimPrefix(scanner.Bytes(), []byte("\n")))
		}
	}
	buf.WriteString("\n")
	return fmt.Sprintf("%s:%v: %s: %s\n", e.ScriptName, e.LintNo, e.Lint, buf.String())
}

func (e LintError) StmtName() string {
	p := parser.New()
	node, err := p.ParseOneStmt(e.Stmt, "", "")
	if err != nil {
		return ""
	}
	switch n := node.(type) {
	case *ast.CreateTableStmt:
		if n.Table == nil {
			return ""
		}
		return n.Table.Name.String()
	case *ast.AlterTableStmt:
		if n.Table == nil {
			return ""
		}
		return n.Table.Name.String()
	}

	return ""
}
