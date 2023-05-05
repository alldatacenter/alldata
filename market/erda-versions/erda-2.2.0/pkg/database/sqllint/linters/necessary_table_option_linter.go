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
	"strings"

	"github.com/pingcap/parser/ast"
	"github.com/pkg/errors"
	"sigs.k8s.io/yaml"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

var tableOptionTypes = map[ast.TableOptionType]string{
	ast.TableOptionNone:                "TableOptionNone",
	ast.TableOptionEngine:              "TableOptionEngine",
	ast.TableOptionCharset:             "TableOptionCharset",
	ast.TableOptionCollate:             "TableOptionCollate",
	ast.TableOptionAutoIdCache:         "TableOptionAutoIdCache",
	ast.TableOptionAutoIncrement:       "TableOptionAutoIncrement",
	ast.TableOptionAutoRandomBase:      "TableOptionAutoRandomBase",
	ast.TableOptionComment:             "TableOptionComment",
	ast.TableOptionAvgRowLength:        "TableOptionAvgRowLength",
	ast.TableOptionCheckSum:            "TableOptionCheckSum",
	ast.TableOptionCompression:         "TableOptionCompression",
	ast.TableOptionConnection:          "TableOptionConnection",
	ast.TableOptionPassword:            "TableOptionPassword",
	ast.TableOptionKeyBlockSize:        "TableOptionKeyBlockSize",
	ast.TableOptionMaxRows:             "TableOptionMaxRows",
	ast.TableOptionMinRows:             "TableOptionMinRows",
	ast.TableOptionDelayKeyWrite:       "TableOptionDelayKeyWrite",
	ast.TableOptionRowFormat:           "TableOptionRowFormat",
	ast.TableOptionStatsPersistent:     "TableOptionStatsPersistent",
	ast.TableOptionStatsAutoRecalc:     "TableOptionStatsAutoRecalc",
	ast.TableOptionShardRowID:          "TableOptionShardRowID",
	ast.TableOptionPreSplitRegion:      "TableOptionPreSplitRegion",
	ast.TableOptionPackKeys:            "TableOptionPackKeys",
	ast.TableOptionTablespace:          "TableOptionTablespace",
	ast.TableOptionNodegroup:           "TableOptionNodegroup",
	ast.TableOptionDataDirectory:       "TableOptionDataDirectory",
	ast.TableOptionIndexDirectory:      "TableOptionIndexDirectory",
	ast.TableOptionStorageMedia:        "TableOptionStorageMedia",
	ast.TableOptionStatsSamplePages:    "TableOptionStatsSamplePages",
	ast.TableOptionSecondaryEngine:     "TableOptionSecondaryEngine",
	ast.TableOptionSecondaryEngineNull: "TableOptionSecondaryEngineNull",
	ast.TableOptionInsertMethod:        "TableOptionInsertMethod",
	ast.TableOptionTableCheckSum:       "TableOptionTableCheckSum",
	ast.TableOptionUnion:               "TableOptionUnion",
	ast.TableOptionEncryption:          "TableOptionEncryption",
}

type necessaryTableOptionLinter struct {
	baseLinter
	meta necessaryTableOptionLinterMeta
}

func (hub) NecessaryTableOptionLinter(s script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var meta necessaryTableOptionLinterMeta
	if err := yaml.Unmarshal(c.Meta, &meta); err != nil {
		return nil, errors.Wrap(err, "failed to parse NecessaryTableOptionLinter.meta")
	}
	var (
		ok    = false
		types []string
	)
	for _, v := range tableOptionTypes {
		types = append(types, v)
		if v == meta.Key {
			ok = true
		}
	}
	if !ok {
		return nil, errors.Errorf("undefined tableOption: %s. Optional tableOptions: %s", meta.Key, strings.Join(types, ", "))
	}

	return &necessaryTableOptionLinter{
		baseLinter: newBaseLinter(s),
		meta:       meta,
	}, nil
}

func (l *necessaryTableOptionLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	stmt, ok := in.(*ast.CreateTableStmt)
	if !ok {
		return in, true
	}

	var m = make(map[string]string)
	for _, opt := range stmt.Options {
		m[tableOptionTypes[opt.Tp]] = opt.StrValue
	}
	v, ok := m[l.meta.Key]
	if !ok {
		l.err = linterror.New(l.s, l.text, fmt.Sprintf("missing necessary tableOption: %s", l.meta.Key),
			func(line []byte) bool {
				return false
			})
		return in, true
	}

	if len(l.meta.Values) == 0 {
		return in, true
	}
	for _, value := range l.meta.Values {
		if strings.EqualFold(v, value) {
			return in, true
		}
	}
	l.err = linterror.New(l.s, l.text, fmt.Sprintf("tableOption %s's value %s dose not match the given value %s",
		l.meta.Key, v, strings.Join(l.meta.Values, ",")), func(line []byte) bool {
		return bytes.Contains(line, []byte(l.meta.Key))
	})
	return in, true
}

func (l *necessaryTableOptionLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, l.err == nil
}

func (l *necessaryTableOptionLinter) Error() error {
	return l.err
}

type necessaryTableOptionLinterMeta struct {
	Key    string   `yaml:"key"`
	Values []string `yaml:"values"`
}
