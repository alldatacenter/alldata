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
	"strings"

	"github.com/pingcap/parser/ast"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

const (
	UTF8MB4 = "utf8mb4"
	UTF8    = "utf8"
)

type charsetLinter struct {
	baseLinter
	meta charsetLinterMeta
}

func (hub) CharsetLinter(script script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var meta charsetLinterMeta
	if err := yaml.Unmarshal(c.Meta, &meta); err != nil {
		meta.TableOptionCharset = map[string]bool{UTF8: true, UTF8MB4: true}
		out, _ := yaml.Marshal(meta)
		return nil, errors.Wrapf(err, "failed to parse CharsetLinter.meta, the structure of CharsetLinter.meta should be like: %s",
			string(out))
	}
	for k, v := range meta.TableOptionCharset {
		meta.TableOptionCharset[strings.ToLower(k)] = v
	}
	var count = 0
	for k := range meta.TableOptionCharset {
		if meta.TableOptionCharset[k] {
			count++
		}
	}
	if count == 0 {
		return nil, errors.Errorf("CharsetLinter.meta.TableOptionCharset configurated error, it atleast be one charset")
	}
	return &charsetLinter{newBaseLinter(script), meta}, nil
}

func (l *charsetLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	stmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true
	}

	for _, opt := range stmt.Options {
		if opt.Tp == ast.TableOptionCharset {
			if ok := l.meta.TableOptionCharset[strings.ToLower(opt.StrValue)]; ok {
				return in, true
			}
		}
	}
	l.err = linterror.New(l.s, l.text, "table charset error, please see the charset in your configuration of CharsetLinter",
		func(line []byte) bool {
			return false
		})
	return in, true
}

func (l *charsetLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *charsetLinter) Error() error {
	return l.err
}

type charsetLinterMeta struct {
	TableOptionCharset map[string]bool `json:"tableOptionCharset" yaml:"tableOptionCharset"`
}
