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
	"reflect"

	"github.com/pingcap/parser/ast"
	"github.com/pkg/errors"
	"gopkg.in/yaml.v3"

	"github.com/erda-project/erda/pkg/database/sqllint"
	"github.com/erda-project/erda/pkg/database/sqllint/linterror"
	"github.com/erda-project/erda/pkg/database/sqllint/script"
)

var alterTableTypes = map[ast.AlterTableType]string{
	ast.AlterTableOption:                     "AlterTableOption",
	ast.AlterTableAddColumns:                 "AlterTableAddColumns",
	ast.AlterTableAddConstraint:              "AlterTableAddConstraint",
	ast.AlterTableDropColumn:                 "AlterTableDropColumn",
	ast.AlterTableDropPrimaryKey:             "AlterTableDropPrimaryKey",
	ast.AlterTableDropIndex:                  "AlterTableDropIndex",
	ast.AlterTableDropForeignKey:             "AlterTableDropForeignKey",
	ast.AlterTableModifyColumn:               "AlterTableModifyColumn",
	ast.AlterTableChangeColumn:               "AlterTableChangeColumn",
	ast.AlterTableRenameColumn:               "AlterTableRenameColumn",
	ast.AlterTableRenameTable:                "AlterTableRenameTable",
	ast.AlterTableAlterColumn:                "AlterTableAlterColumn",
	ast.AlterTableLock:                       "AlterTableLock",
	ast.AlterTableAlgorithm:                  "AlterTableAlgorithm",
	ast.AlterTableRenameIndex:                "AlterTableRenameIndex",
	ast.AlterTableForce:                      "AlterTableForce",
	ast.AlterTableAddPartitions:              "AlterTableAddPartitions",
	ast.AlterTableCoalescePartitions:         "AlterTableCoalescePartitions",
	ast.AlterTableDropPartition:              "AlterTableDropPartition",
	ast.AlterTableTruncatePartition:          "AlterTableTruncatePartition",
	ast.AlterTablePartition:                  "AlterTablePartition",
	ast.AlterTableEnableKeys:                 "AlterTableEnableKeys",
	ast.AlterTableDisableKeys:                "AlterTableDisableKeys",
	ast.AlterTableRemovePartitioning:         "AlterTableRemovePartitioning",
	ast.AlterTableWithValidation:             "AlterTableWithValidation",
	ast.AlterTableWithoutValidation:          "AlterTableWithoutValidation",
	ast.AlterTableSecondaryLoad:              "AlterTableSecondaryLoad",
	ast.AlterTableSecondaryUnload:            "AlterTableSecondaryUnload",
	ast.AlterTableRebuildPartition:           "AlterTableRebuildPartition",
	ast.AlterTableReorganizePartition:        "AlterTableReorganizePartition",
	ast.AlterTableCheckPartitions:            "AlterTableCheckPartitions",
	ast.AlterTableExchangePartition:          "AlterTableExchangePartition",
	ast.AlterTableOptimizePartition:          "AlterTableOptimizePartition",
	ast.AlterTableRepairPartition:            "AlterTableRepairPartition",
	ast.AlterTableImportPartitionTablespace:  "AlterTableImportPartitionTablespace",
	ast.AlterTableDiscardPartitionTablespace: "AlterTableDiscardPartitionTablespace",
	ast.AlterTableAlterCheck:                 "AlterTableAlterCheck",
	ast.AlterTableDropCheck:                  "AlterTableDropCheck",
	ast.AlterTableImportTablespace:           "AlterTableImportTablespace",
	ast.AlterTableDiscardTablespace:          "AlterTableDiscardTablespace",
	ast.AlterTableIndexInvisible:             "AlterTableIndexInvisible",
	ast.AlterTableOrderByColumns:             "AlterTableOrderByColumns",
	ast.AlterTableSetTiFlashReplica:          "AlterTableSetTiFlashReplica",
}

type allowedStmtLinter struct {
	baseLinter
	meta map[string]allowedStmtLinterMetaItem
}

// AllowedStmtLinter
// Principle 1: "Everything is allowed if it is not prohibited",
// that is, if a sentence pattern is not configured, it is considered to be allowed
func (hub) AllowedStmtLinter(s script.Script, c sqllint.Config) (sqllint.Rule, error) {
	var l = allowedStmtLinter{
		baseLinter: newBaseLinter(s),
		meta:       make(map[string]allowedStmtLinterMetaItem),
	}
	var metas []allowedStmtLinterMetaItem
	if err := yaml.Unmarshal(c.Meta, &metas); err != nil {
		return nil, errors.Wrapf(err, "failed to paerse AllowedStmtLinter.meta, raw meta: %s", string(c.Meta))
	}
	for _, item := range metas {
		l.meta[item.StmtType] = item
	}
	return &l, nil
}

func (l *allowedStmtLinter) Enter(in ast.Node) (ast.Node, bool) {
	if l.text == "" || in.Text() != "" {
		l.text = in.Text()
	}

	switch stmt := in.(type) {
	case *ast.AlterTableStmt:
		if stop := l.ok("AlterTableStmt", in); stop {
			return in, true
		}

		for _, spec := range stmt.Specs {
			key, ok := alterTableTypes[spec.Tp]
			if !ok {
				continue
			}
			if stop := l.ok("AlterTableStmt."+key, in); stop {
				return in, true
			}
		}

	default:
		name := reflect.TypeOf(in).Elem().Name()
		l.ok(name, in)
	}

	return in, true
}

func (l *allowedStmtLinter) Leave(in ast.Node) (ast.Node, bool) {
	return in, true
}

func (l *allowedStmtLinter) Error() error {
	return l.err
}

func (l *allowedStmtLinter) ok(key string, in ast.Node) (stop bool) {
	meta, ok := l.meta[key]
	if !ok {
		return true
	}
	if !meta.ok(l.s.Name()) {
		l.err = linterror.New(l.s, l.text, "not allowed "+key, func([]byte) bool { return true })
		return true
	}
	return false
}

type allowedStmtLinterMetaItem struct {
	StmtType  string        `json:"stmtType" yaml:"stmtType"`
	Forbidden bool          `json:"forbidden" yaml:"forbidden"`
	White     sqllint.White `json:"white" yaml:"white"`
}

func (meta allowedStmtLinterMetaItem) ok(scriptName string) bool {
	if !meta.Forbidden {
		return true
	}
	return meta.White.Match("", scriptName)
}
