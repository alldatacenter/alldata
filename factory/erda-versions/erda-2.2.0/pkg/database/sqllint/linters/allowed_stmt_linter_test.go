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

package linters_test

import (
	"testing"

	"github.com/erda-project/erda/pkg/database/sqllint"
	_ "github.com/erda-project/erda/pkg/database/sqllint/linters"
)

const allowedStmtLinterConfig = `# allowed SQL list
- name: AllowedStmtLinter
  switch: on
  white:
    patterns:
      - "^.*-base"
  meta:
    - StmtType: CreateDatabaseStmt
      Forbidden: true
    - StmtType: AlterDatabaseStmt
      Forbidden: true
    - StmtType: DropDatabaseStmt
      Forbidden: true
    - StmtType: CreateTableStmt
    - StmtType: DropTableStmt
      Forbidden: true
    - StmtType: DropSequenceStmt
      Forbidden: true
    - StmtType: RenameTableStmt
      Forbidden: true
    - StmtType: CreateViewStmt
      Forbidden: true
    - StmtType: CreateSequenceStmt
      Forbidden: true
    - StmtType: CreateIndexStmt
    - StmtType: DropIndexStmt
    - StmtType: LockTablesStmt
      Forbidden: true
    - StmtType: UnlockTablesStmt
      Forbidden: true
    - StmtType: CleanupTableLockStmt
      Forbidden: true
    - StmtType: RepairTableStmt
      Forbidden: true
    - StmtType: TruncateTableStmt
      Forbidden: true
    - StmtType: RecoverTableStmt
      Forbidden: true
    - StmtType: FlashBackTableStmt
      Forbidden: true
    - StmtType: AlterTableStmt
    - StmtType: AlterTableStmt.AlterTableOption
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableAddColumns
    - StmtType: AlterTableStmt.AlterTableAddConstraint
    - StmtType: AlterTableStmt.AlterTableDropColumn
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableDropPrimaryKey
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableDropIndex
    - StmtType: AlterTableStmt.AlterTableDropForeignKey
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableModifyColumn
    - StmtType: AlterTableStmt.AlterTableChangeColumn
    - StmtType: AlterTableStmt.AlterTableRenameColumn
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableRenameTable
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableAlterColumn
    - StmtType: AlterTableStmt.AlterTableLock
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableAlgorithm
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableRenameIndex
    - StmtType: AlterTableStmt.AlterTableForce
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableAddPartitions
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableCoalescePartitions
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableDropPartition
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableTruncatePartition
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTablePartition
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableEnableKeys
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableDisableKeys
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableRemovePartitioning
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableWithValidation
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableWithoutValidation
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableSecondaryLoad
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableSecondaryUnload
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableRebuildPartition
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableReorganizePartition
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableCheckPartitions
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableExchangePartition
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableOptimizePartition
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableRepairPartition
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableImportPartitionTablespace
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableDiscardPartitionTablespace
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableAlterCheck
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableDropCheck
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableImportTablespace
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableDiscardTablespace
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableIndexInvisible
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableOrderByColumns
      Forbidden: true
    - StmtType: AlterTableStmt.AlterTableSetTiFlashReplica
      Forbidden: true
    - StmtType: SelectStmt
    - StmtType: UnionStmt
    - StmtType: LoadDataStmt
      Forbidden: true
    - StmtType: InsertStmt
    - StmtType: DeleteStmt
    - StmtType: UpdateStmt
    - StmtType: ShowStmt
    - StmtType: SplitRegionStmt
      Forbidden: true
`

const (
	allowedStmtLinter_CreateDatabaseStmt = `create database some_database;`
	// allowedStmtLinter_AlterDatabaseStmt                    = `ALTER DATABASE mydb READ ONLY = 1;`
	allowedStmtLinter_DropDatabaseStmt     = `drop database some_database;`
	allowedStmtLinter_CrateTableStmt       = `create table some_table (	id bigint);`
	allowedStmtLinter_DropTableStmt        = `drop table some_table;`
	allowedStmtLinter_DropSequenceStmt     = `DROP SEQUENCE oe.customers_seq;`
	allowedStmtLinter_RenameTableStmt      = `rename table some_table to any_table;`
	allowedStmtLinter_CreateViewStmt       = `CREATE VIEW test.v AS SELECT * FROM t;`
	allowedStmtLinter_CreateSequenceStmt   = `CREATE SEQUENCE customers_seq START WITH 1000 INCREMENT BY 1 NOCACHE NOCYCLE;`
	allowedStmtLinter_CreateIndexStmt      = `CREATE INDEX part_of_name ON customer (name(10));`
	allowedStmtLinter_DropIndexStmt        = "DROP INDEX `PRIMARY` ON t;"
	allowedStmtLinter_LockTablesStmt       = `LOCK TABLES t1 WRITE, t2 READ;`
	allowedStmtLinter_UnlockTablesStmt     = `UNLOCK TABLES;`
	allowedStmtLinter_CleanupTableLockStmt = `ADMIN CLEANUP TABLE LOCK some_lock;`
	allowedStmtLinter_RepairTableStmt      = `repair table t1 quick;`
	allowedStmtLinter_TruncateTableStmt    = `truncate table some_table;`
	allowedStmtLinter_RecoverTableStmt     = `recover table some_table;`
	allowedStmtLinter_FlashBackTableStmt   = `flashback table some_table;`

	allowedStmtLinter_SplitRegionStmt = ``
)

const (
	begin  = "begin;"
	commit = "commit;"
)

type script struct {
	Name    string
	Content string
}

func (s script) GetContent() []byte {
	return []byte(s.Content)
}

func TestAllowedStmtLinter_LoadConfig(t *testing.T) {
	if _, err := sqllint.LoadConfig([]byte(allowedStmtLinterConfig)); err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
}

func TestAllowedStmtLinter_NotAllowedAlterTableStmt(t *testing.T) {
	var config = `# allowed SQL list
- name: AllowedStmtLinter
  switchOn: true
  white:
    patterns:
      - "^.*-base"
  meta:
    - stmtType: AlterTableStmt
      forbidden: true
`
	var s = script{
		Name:    "stmt",
		Content: "alter table t1 add column col1 bigint;",
	}
	cfg, _ := sqllint.LoadConfig([]byte(config))
	linter := sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatal(err)
	}
	if len(linter.Errors()[s.Name].Lints) == 0 {
		t.Fatal("lint error")
	}
}

func TestAllowedStmtLint_AllowedAlterTableStmt_White(t *testing.T) {
	var config = `# allowed SQL list
- name: AllowedStmtLinter
  switchOn: true
  white:
    patterns:
      - "^.*-base"
  meta:
    - stmtType: AlterTableStmt
      forbidden: true
      white: 
        committedAt:
          - <=20220201
`
	var s = script{
		Name:    "20220101-some-feature",
		Content: "alter table t1 add column col1 bigint;",
	}
	cfg, _ := sqllint.LoadConfig([]byte(config))
	linter := sqllint.New(cfg)
	if err := linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatal(err)
	}
	if len(linter.Errors()[s.Name].Lints) > 0 {
		t.Fatal("there should be no errors")
	}
}

func TestAllowedStmtLinter_CreateDatabaseStmt(t *testing.T) {
	var configurationForbidden = `
- name: AllowedStmtLinter
  switchOn: true
  white:
    patterns:
      - "^.*-base"
  meta:
    - stmtType: CreateDatabaseStmt
      forbidden: true
`
	var s = script{
		Name:    "20220201-some-feature",
		Content: "CREATE DATABASE erda;",
	}
	cfg, err := sqllint.LoadConfig([]byte(configurationForbidden))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	linter := sqllint.New(cfg)
	if err = linter.Input("", s.Name, s.GetContent()); err != nil {
		t.Fatal(err)
	}
	t.Logf("Errors: %+v", linter.Errors())
	if len(linter.Errors()[s.Name].Lints) == 0 {
		t.Fatal("there should be some errors")
	}
}
