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

var columnOptionNames = map[string]ast.ColumnOptionType{
	"ColumnOptionNoOption":      ast.ColumnOptionNoOption,
	"ColumnOptionPrimaryKey":    ast.ColumnOptionPrimaryKey,
	"ColumnOptionNotNull":       ast.ColumnOptionNotNull,
	"ColumnOptionAutoIncrement": ast.ColumnOptionAutoIncrement,
	"ColumnOptionDefaultValue":  ast.ColumnOptionUniqKey,
	"ColumnOptionUniqKey":       ast.ColumnOptionUniqKey,
	"ColumnOptionNull":          ast.ColumnOptionNull,
	"ColumnOptionOnUpdate":      ast.ColumnOptionOnUpdate,
	"ColumnOptionFulltext":      ast.ColumnOptionFulltext,
	"ColumnOptionComment":       ast.ColumnOptionComment,
	"ColumnOptionGenerated":     ast.ColumnOptionGenerated,
	"ColumnOptionReference":     ast.ColumnOptionReference,
	"ColumnOptionCollate":       ast.ColumnOptionCollate,
	"ColumnOptionCheck":         ast.ColumnOptionCheck,
	"ColumnOptionColumnFormat":  ast.ColumnOptionColumnFormat,
	"ColumnOptionStorage":       ast.ColumnOptionStorage,
	"ColumnOptionAutoRandom":    ast.ColumnOptionAutoRandom,
}

type necessaryColumnOptionLinter struct {
	baseLinter
	meta necessaryColumnOptionLinterMeta
}

// NecessaryColumnOptionLinter checks if there is the necessary column option.
func (hub) NecessaryColumnOptionLinter(s script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var meta necessaryColumnOptionLinterMeta
	if err := yaml.Unmarshal(c.Meta, &meta); err != nil {
		meta = necessaryColumnOptionLinterMeta{ColumnOptionType: []string{"example"}}
		out, _ := yaml.Marshal(meta)
		return nil, errors.Wrapf(err, "failed to parse NecessaryColumnOptionLinter, the correct format is like:\n%s\n",
			string(out))
	}
	for _, typ := range meta.ColumnOptionType {
		if _, ok := columnOptionNames[typ]; !ok {
			return nil, errors.Errorf("failed to find the matched column option %s. The optional items: %+v", meta.ColumnOptionType, columnOptionNames)
		}
	}

	return &necessaryColumnOptionLinter{newBaseLinter(s), meta}, nil
}

func (l *necessaryColumnOptionLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	// AlterTableStmt is always valid for this linter, return
	if _, ok := in.(*ast.AlterTableStmt); ok {
		return in, true
	}

	col, ok := in.(*ast.ColumnDef)
	if !ok {
		return in, false
	}
	if l.meta.ColumnName != "" && l.meta.ColumnName != col.Name.String() {
		return in, true
	}

	for _, opt := range col.Options {
		for _, typ := range l.meta.ColumnOptionType {
			if opt.Tp == columnOptionNames[typ] {
				return in, true
			}
		}
	}
	l.err = linterror.New(l.s, l.text, fmt.Sprintf("missing necessary column option: %v", l.meta.ColumnOptionType), func(line []byte) bool {
		if col.Name == nil {
			return true
		}
		return bytes.Contains(line, bytes.ToLower([]byte(col.Name.String())))
	})
	return in, true
}

func (l *necessaryColumnOptionLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, true
}

func (l *necessaryColumnOptionLinter) Error() error {
	return l.err
}

type necessaryColumnOptionLinterMeta struct {
	ColumnName       string   `json:"columnName" yaml:"columnName"`
	ColumnOptionType []string `json:"columnOptionType" yaml:"columnOptionType"`
}
