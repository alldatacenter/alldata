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

	"github.com/pingcap/parser/ast"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

type indexNameLinter struct {
	baseLinter
	meta indexNameLinterMeta
}

func (hub) IndexNameLinter(script script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var l = indexNameLinter{
		baseLinter: newBaseLinter(script),
		meta:       indexNameLinterMeta{},
	}
	if err := yaml.Unmarshal(c.Meta, &l.meta); err != nil {
		return nil, errors.Wrap(err, "failed to parse IndexNameLinter.meta")
	}
	return &l, nil
}

func (l *indexNameLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	constraint, ok := in.(*ast.Constraint)
	if !ok {
		return in, false
	}

	switch constraint.Tp {
	case ast.ConstraintIndex:
		if ok, _ := regexp.Match(l.meta.IndexPattern, []byte(constraint.Name)); !ok {
			l.err = linterror.New(l.s, l.text, fmt.Sprintf("index name should be like %s", l.meta.IndexPattern),
				func(line []byte) bool {
					return bytes.Contains(bytes.ToLower(line), []byte("index")) &&
						bytes.Contains(bytes.ToLower(line), bytes.ToLower([]byte(constraint.Name)))
				})
			return in, true
		}
	case ast.ConstraintUniq, ast.ConstraintUniqKey, ast.ConstraintUniqIndex:
		if ok, _ := regexp.Match(l.meta.UniqPattern, []byte(constraint.Name)); !ok {
			l.err = linterror.New(l.s, l.text, fmt.Sprintf("unique index name should be like %s", l.meta.UniqPattern),
				func(line []byte) bool {
					return bytes.Contains(bytes.ToLower(line), []byte("unique")) &&
						bytes.Contains(bytes.ToLower(line), bytes.ToLower([]byte(constraint.Name)))
				})
			return in, true
		}
	}

	return in, true
}

func (l *indexNameLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *indexNameLinter) Error() error {
	return l.err
}

type indexNameLinterMeta struct {
	IndexPattern string `json:"indexPattern" yaml:"indexPattern"`
	UniqPattern  string `json:"uniqPattern" yaml:"uniqPattern"`
}
