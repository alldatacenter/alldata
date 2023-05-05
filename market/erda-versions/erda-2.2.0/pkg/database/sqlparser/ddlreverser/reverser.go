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

package ddlreverser

import (
	"bytes"
	"strings"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"
	"github.com/pingcap/parser/format"
	"github.com/pkg/errors"
	"gorm.io/gorm"

	"github.com/erda-project/erda/pkg/database/sqlparser/snapshot"
)

// ReverseDDLWithSnapshot reverses a DDL.
// e.g.
// 	CreateTableStmt ==reverse-to==> DropTableStmt
//	RenameTableStmt a to b ==reverse-to==> RenameTableStmt b to a
// 	AlterTableAddColumns ==reverse-to==> AlterTableDropColumn
// 	AlterTableOption to new ==reverse-to==> AlterTableOption to old
// 	... ....
//
// How to do this ?
// First, to snapshot the table definition from database, then generate the reversed DDL.
//
// tx *gorm.DB is used to connect to database for getting table's snapshot;
// ddl ast.DDLNode is is the stmt node for revering.
//
// reversing is the reversed DDL text;
// ok is whether reversing is success, if not ok, reversing is "";
// err is whether an error has occurred, if err is not nil, ok is false, reversing is "".
//
// If the ddl is not allowed (e.g. DropDatabaseStmt, DropTableStmt, AlterTableDropColumnSpec),
// the function will not process it, and returned err is nil, ok is false, reversing is "".
func ReverseDDLWithSnapshot(tx *gorm.DB, ddl ast.DDLNode) (reversing string, ok bool, err error) {
	switch ddl.(type) {

	// CREATE [TEMPORARY] TABLE [IF NOT EXISTS] tbl_name
	//    (create_definition,...)
	//    [table_options]
	//    [partition_options]
	case *ast.CreateTableStmt:
		tableName := ddl.(*ast.CreateTableStmt).Table.Name.String()
		return "DROP TABLE IF EXISTS " + tableName + ";\n", true, nil

	// RENAME TABLE
	//    tbl_name TO new_tbl_name
	//    [, tbl_name2 TO new_tbl_name2] ...
	case *ast.RenameTableStmt:
		stmt := ddl.(*ast.RenameTableStmt)
		var buf = bytes.NewBufferString("RENAME TABLE ")
		for i, t2t := range stmt.TableToTables {
			if i > 0 {
				buf.WriteString(",")
			}
			buf.WriteString(t2t.NewTable.Name.String())
			buf.WriteString(" TO ")
			buf.WriteString(t2t.OldTable.Name.String())
		}
		buf.WriteString(";\n")
		return buf.String(), true, nil

	// not allowed ddl
	case *ast.DropDatabaseStmt, *ast.DropTableStmt:
		return "", false, nil

	case *ast.DropIndexStmt:
		stmt := ddl.(*ast.DropIndexStmt)

		tableName := stmt.Table.Name.String()
		createTableSQL, err := ShowCreateTable(tx, tableName)
		if err != nil {
			return "", false, errors.Wrapf(err, "failed to ShowCreateTable %s", tableName)
		}
		createStmt, err := parser.New().ParseOneStmt(createTableSQL, "", "")
		if err != nil {
			return "", false, err
		}
		snap := createStmt.(*ast.CreateTableStmt)
		return ReverseDropIndexStmtWithCompares(snap, stmt)

	// AlterTableStmt
	// https://dev.mysql.com/doc/refman/8.0/en/alter-table.html
	case *ast.AlterTableStmt:
		alterTableStmt := ddl.(*ast.AlterTableStmt)
		tableName := alterTableStmt.Table.Name.String()

		// snapshot table's definition
		creatTableSQL, err := ShowCreateTable(tx, tableName)
		if err != nil {
			return "", false, errors.Wrapf(err, "failed to ShowCreateTable %s", tableName)
		}
		createStmt, err := parser.New().ParseOneStmt(creatTableSQL, "", "")
		if err != nil {
			return "", false, err
		}
		snap := createStmt.(*ast.CreateTableStmt)

		return ReverseAlterWithCompares(snap, alterTableStmt)
	}
	return "", false, nil
}

// ReverseCreateTableStmts reverses DDLs without snapshot.
// Generally, this function is used to process the baseline,
// because when processing the baseline, it only needs to Drop all the newly created tables.
func ReverseCreateTableStmts(nodes interface{ DDLNodes() []ast.DDLNode }) string {
	var buffer = bytes.NewBuffer(nil)
	for _, ddl := range nodes.DDLNodes() {
		if create, ok := ddl.(*ast.CreateTableStmt); ok {
			name := create.Table.Name.String()
			buffer.WriteString("DROP TABLE IF EXISTS " + name + ";\n")
		}
	}

	return buffer.String()
}

// ShowCreateTable gets create table stmt by executing SHOW CREATE TABLE .
func ShowCreateTable(tx *gorm.DB, tableName string) (create string, err error) {
	if tableName == "" {
		return "", errors.New("the tableName can not be \"\"")
	}
	tx = tx.Raw("SHOW CREATE TABLE " + tableName)
	if err = tx.Error; err != nil {
		return "", err
	}
	if err = tx.Row().Scan(&tableName, &create); err != nil {
		return "", err
	}
	create = snapshot.TrimCharacterSetFromRawCreateTableSQL(create, snapshot.CharsetWhite()...)
	create = snapshot.TrimBlockFormat(create)
	return create, nil
}

// ReverseAlterWithCompares for giving compares, returns the reversed SQL of AlterTableStmt
func ReverseAlterWithCompares(create *ast.CreateTableStmt, alter *ast.AlterTableStmt) (string, bool, error) {
	if create == nil {
		return "", false, errors.New("the raw CreateTableStmt is nill")
	}
	if alter == nil {
		return "", false, errors.New("the AlterTableStmt is nil")
	}

	if create.Table.Name.String() != alter.Table.Name.String() {
		return "", false, errors.New("the altered table name is not equal with the compared table")
	}

	var restore = ast.AlterTableStmt{
		Table: create.Table,
		Specs: nil,
	}

	for _, spec := range alter.Specs {
		switch spec.Tp {
		// spec syntax:
		// tableOption [[,] tableOption]
		// processing caliber: take out the tableOption in the snapshot,
		// as long as the tableOption is modified,
		// roll it back to the snapshot state
		//
		// note: if the tableOption of the table is changed multiple times in a script,
		// the same rollback statement is generated multiple times and executed repeatedly,
		// but considering that repeated execution of the statement will not cause damage,
		// no better generation logic is sought.
		case ast.AlterTableOption:
			reverse := *spec
			reverse.Options = create.Options
			restore.Specs = append(restore.Specs, &reverse)

		// processing caliber: drop the new column
		case ast.AlterTableAddColumns:
			for _, col := range spec.NewColumns {
				var reverse ast.AlterTableSpec
				reverse.Tp = ast.AlterTableDropColumn
				reverse.OldColumnName = col.Name
				restore.Specs = append(restore.Specs, &reverse)
			}

		// processing caliber: drop the new constraint
		case ast.AlterTableAddConstraint:
			var reverse ast.AlterTableSpec
			reverse.Tp = ast.AlterTableDropIndex
			reverse.Name = spec.Constraint.Name
			restore.Specs = append(restore.Specs, &reverse)

		// processing caliber: add the index by AlterTableAddConstraint
		case ast.AlterTableDropIndex:
			if strings.EqualFold(spec.Name, "PRIMARY") {
				return "", false, errors.New("not allowed to AlterTableDropIndex PRIMARY")
			}

			var reverse ast.AlterTableSpec
			reverse.Tp = ast.AlterTableAddConstraint

			for _, constraint := range create.Constraints {
				if constraint.Name == spec.Name {
					reverse.Constraint = constraint
					break
				}
			}

			if reverse.Constraint != nil {
				restore.Specs = append(restore.Specs, &reverse)
			}

		// spec syntax:
		// MODIFY [ColumnKeywordOpt] {ColumnDef} [ColumnPosition]
		//
		// CHANGE [ColumnKeywordOpt] {ColumnName} {ColumnDef} [ColumnPosition]
		//
		// note: the renaming of columns by AlterTableChangeColumn is not considered here,
		// because column renaming is not compliant and will be filtered out in ErdaMySQLLint.
		// However, this processing loses versatility and will be repaired later.
		//
		// note: the change of AlterTableChangeColumn to ColumnPosition is not considered here.
		// the first reason is that the change to ColumnPosition will not affect the table structure,
		// the second reason is that the implementation of restoring the column position is slightly more complicated.
		//
		// ALTER [ColumnKeywordOpt] {ColumnName} SET DEFAULT  {SignedLiteral}
		//                                      │            └--(Expression)--┘
		//                                      └-DROP DEFAULT----------------┘
		//
		// processing caliber: whether it is SET DEFAULT or DROP DEFAULT, use AlterTableChangeColumn to fall back.
		case ast.AlterTableModifyColumn, ast.AlterTableChangeColumn, ast.AlterTableAlterColumn:
			var reverse ast.AlterTableSpec
			reverse.Tp = ast.AlterTableChangeColumn
			reverse.OldColumnName = spec.NewColumns[0].Name // 此处不考虑列被重命名的情况
			for _, col := range create.Cols {
				if reverse.OldColumnName.String() == col.Name.String() {
					reverse.NewColumns = append(reverse.NewColumns, col)
					break
				}
			}
			reverse.Position = &ast.ColumnPosition{Tp: 0, RelativeColumn: nil}
			restore.Specs = append(restore.Specs, &reverse)

		// spec syntax:
		// RENAME COLUMN {identifier} TO {identifier}
		//
		// note: AlterTableRenameColumn will not appear under normal circumstances
		// it will be filtered out in ErdaMySQLLint.
		case ast.AlterTableRenameColumn:
			var reverse ast.AlterTableSpec
			reverse.Tp = ast.AlterTableRenameColumn
			reverse.NewColumnName = spec.OldColumnName
			reverse.OldColumnName = spec.NewColumnName
			restore.Specs = append(restore.Specs, &reverse)

		// spec syntax:
		// RENAME------TO------{TableName}
		//      ├-----EqOpt----┤
		//		└------AS------┘
		//
		// note: AlterTableRenameTable will not appear under normal circumstances
		// it will be filtered out in ErdaMySQLLint.
		case ast.AlterTableRenameTable:
			var reverse ast.AlterTableSpec
			reverse.Tp = ast.AlterTableRenameTable
			reverse.NewTable = restore.Table
			restore.Table = spec.NewTable
			restore.Specs = append(restore.Specs, &reverse)

		// spec syntax:
		// RENAME {INDEX|KEY} {identifier} TO {identifier}
		case ast.AlterTableRenameIndex:
			var reverse ast.AlterTableSpec
			reverse.Tp = ast.AlterTableRenameIndex
			reverse.ToKey = spec.FromKey
			reverse.FromKey = spec.ToKey
			restore.Specs = append(restore.Specs, &reverse)

		case ast.AlterTableDropColumn, ast.AlterTableDropPrimaryKey,
			ast.AlterTableDropForeignKey, ast.AlterTableLock, ast.AlterTableAlgorithm,
			ast.AlterTableForce, ast.AlterTableAddPartitions, ast.AlterTableCoalescePartitions,
			ast.AlterTableDropPartition, ast.AlterTableTruncatePartition, ast.AlterTablePartition,
			ast.AlterTableEnableKeys, ast.AlterTableDisableKeys, ast.AlterTableRemovePartitioning,
			ast.AlterTableWithValidation, ast.AlterTableWithoutValidation, ast.AlterTableSecondaryLoad,
			ast.AlterTableSecondaryUnload, ast.AlterTableRebuildPartition, ast.AlterTableReorganizePartition,
			ast.AlterTableCheckPartitions, ast.AlterTableExchangePartition, ast.AlterTableOptimizePartition,
			ast.AlterTableRepairPartition, ast.AlterTableImportPartitionTablespace, ast.AlterTableDiscardPartitionTablespace,
			ast.AlterTableAlterCheck, ast.AlterTableDropCheck, ast.AlterTableImportTablespace,
			ast.AlterTableDiscardTablespace, ast.AlterTableIndexInvisible, ast.AlterTableOrderByColumns,
			ast.AlterTableSetTiFlashReplica:
			var m = map[ast.AlterTableType]string{
				ast.AlterTableOrderByColumns:             "AlterTableOrderByColumns",
				ast.AlterTableIndexInvisible:             "AlterTableIndexInvisible",
				ast.AlterTableDiscardTablespace:          "AlterTableDiscardTablespace",
				ast.AlterTableImportTablespace:           "AlterTableImportTablespace",
				ast.AlterTableDropCheck:                  "AlterTableDropCheck",
				ast.AlterTableAlterCheck:                 "AlterTableAlterCheck",
				ast.AlterTableDiscardPartitionTablespace: "AlterTableDiscardPartitionTablespace",
				ast.AlterTableImportPartitionTablespace:  "AlterTableImportPartitionTablespace",
				ast.AlterTableRepairPartition:            "AlterTableRepairPartition",
				ast.AlterTableOptimizePartition:          "AlterTableOptimizePartition",
				ast.AlterTableExchangePartition:          "AlterTableExchangePartition",
				ast.AlterTableCheckPartitions:            "AlterTableCheckPartitions",
				ast.AlterTableReorganizePartition:        "AlterTableReorganizePartition",
				ast.AlterTableRebuildPartition:           "AlterTableRebuildPartition",
				ast.AlterTableSecondaryUnload:            "AlterTableSecondaryUnload",
				ast.AlterTableSecondaryLoad:              "AlterTableSecondaryLoad",
				ast.AlterTableWithoutValidation:          "AlterTableWithoutValidation",
				ast.AlterTableWithValidation:             "AlterTableWithValidation",
				ast.AlterTableRemovePartitioning:         "AlterTableRemovePartitioning",
				ast.AlterTableDisableKeys:                "AlterTableDisableKeys",
				ast.AlterTableEnableKeys:                 "AlterTableEnableKeys",
				ast.AlterTablePartition:                  "AlterTablePartition",
				ast.AlterTableTruncatePartition:          "AlterTableTruncatePartition",
				ast.AlterTableDropPartition:              "AlterTableDropPartition",
				ast.AlterTableCoalescePartitions:         "AlterTableCoalescePartitions",
				ast.AlterTableAddPartitions:              "AlterTableAddPartitions",
				ast.AlterTableForce:                      "AlterTableForce",
				ast.AlterTableAlgorithm:                  "AlterTableAlgorithm,",
				ast.AlterTableLock:                       "AlterTableLock",
				ast.AlterTableDropForeignKey:             "AlterTableDropForeignKey",
				ast.AlterTableDropPrimaryKey:             "AlterTableDropPrimaryKey",
				ast.AlterTableDropColumn:                 "AlterTableDropColumn",
			}
			return "", false, errors.Errorf("not allowed to %s", m[spec.Tp])
		}
	}

	if len(restore.Specs) == 0 {
		return "", false, nil
	}
	var buffer = bytes.NewBuffer(nil)
	if err := restore.Restore(&format.RestoreCtx{
		Flags:     format.DefaultRestoreFlags,
		In:        buffer,
		JoinLevel: 0,
	}); err != nil {
		return "", false, err
	}

	return buffer.String(), buffer.Len() != 0, nil
}

func ReverseDropIndexStmtWithCompares(compares *ast.CreateTableStmt, drop *ast.DropIndexStmt) (string, bool, error) {
	if strings.EqualFold(drop.IndexName, "PRIMARY") {
		return "", false, errors.New("not allowed to drop primary key")
	}

	var restore = ast.AlterTableStmt{
		Table: compares.Table,
		Specs: make([]*ast.AlterTableSpec, 1),
	}

	var spec ast.AlterTableSpec
	spec.Tp = ast.AlterTableAddConstraint

	for _, cst := range compares.Constraints {
		if cst.Name == drop.IndexName {
			spec.Constraint = cst
			break
		}
	}
	if spec.Constraint == nil {
		return "", false, errors.Errorf("the index you droped not found, table name: %s, index name: %s", drop.Table.Name, drop.IndexName)
	}

	restore.Specs[0] = &spec
	var buffer = bytes.NewBuffer(nil)
	if err := restore.Restore(&format.RestoreCtx{
		Flags:     format.DefaultRestoreFlags,
		In:        buffer,
		JoinLevel: 0,
	}); err != nil {
		return "", false, errors.Wrap(err, "failed to Restore SQL node")
	}

	return buffer.String(), buffer.String() != "", nil
}

func ReverseCreateTableStmt(in *ast.CreateTableStmt) string {
	if in == nil || in.Table == nil {
		return ""
	}
	name := in.Table.Name.String()
	return "DROP TABLE IF EXISTS " + name + ";\n"
}
