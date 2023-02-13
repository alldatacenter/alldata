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

package ddlreverser_test

import (
	"flag"
	"testing"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"
	"github.com/pingcap/parser/model"
	_ "github.com/pingcap/tidb/types/parser_driver"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"

	"github.com/erda-project/erda/pkg/database/sqlparser/ddlreverser"
)

const createTable = `
CREATE TABLE IF NOT EXISTS dice_api_doc_lock (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  session_id char(36) NOT NULL COMMENT '会话标识',
  is_locked tinyint(1) NOT NULL DEFAULT '0' COMMENT '会话所有者是否持有文档锁',
  expired_at datetime NOT NULL COMMENT '会话过期时间',
  application_id bigint(20) NOT NULL COMMENT '应用 id',
  branch_name varchar(191) NOT NULL COMMENT '分支名',
  doc_name varchar(191) NOT NULL COMMENT '文档名, 也即服务名',
  creator_id varchar(191) NOT NULL COMMENT '创建者 id',
  updater_id varchar(191) NOT NULL COMMENT '更新者 id',
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc (application_id, branch_name,doc_name)
) ENGINE=InnoDB AUTO_INCREMENT=399 DEFAULT CHARSET=utf8mb4 COMMENT='API 设计中心文档锁表';

RENAME TABLE dice_api_doc_lock to doc_lock;

ALTER TABLE doc_lock
	COMMENT='修改了的 table option';

ALTER TABLE doc_lock
	ADD (
		new_col1 varchar(16) not null comment 'this is a new column',
		new_col2 bigint not null
	);

ALTER TABLE doc_lock 
	ADD CONSTRAINT new_constraint UNIQUE KEY (doc_name);

ALTER TABLE doc_lock
	MODIFY new_col1 varchar(1024) default 'default text' comment 'modified the column';

ALTER TABLE doc_lock
	CHANGE new_col1 new_col1 text not null comment 'change the column';

ALTER TABLE doc_lock
	RENAME COLUMN new_col2 to new_col3;

ALTER TABLE doc_lock
	ALTER new_col3 set default 99;

ALTER TABLE doc_lock
	RENAME INDEX uk_doc TO uk_doc2;
`

var dsn = flag.String("dsn", "", "mysql dsn")

func openDB() (*gorm.DB, error) {
	db, err := gorm.Open(mysql.Open(*dsn))
	if err != nil {
		return nil, err
	}

	return db, nil
}

func TestReverseDDLWithSnapshot(t *testing.T) {
	if dsn == nil || *dsn == "" {
		return
	}
	db, err := openDB()
	if err != nil {
		t.Fatalf("failed to openDB, err: %v", err)
	}

	defer db.Migrator().DropTable("dice_api_doc_lock", "doc_lock")

	nodes, warns, err := parser.New().Parse(createTable, "", "")
	if err != nil {
		t.Fatalf("failed to Pasrse, err: %v", err)
	}
	for _, warn := range warns {
		t.Log("warn:", warn)
	}

	for i, node := range nodes {
		t.Logf("node text: %s", node.Text())
		reversed, _, err := ddlreverser.ReverseDDLWithSnapshot(db, node.(ast.DDLNode))
		if err != nil {
			t.Fatalf("failed to ReverseDDLWithSnapshot [%v], err: %v", i, err)
		}
		t.Logf("reversed SQL: %s", reversed)

		if err := db.Exec(node.Text()).Error; err != nil {
			t.Fatalf("failed to Exec [%v]: nerr: %v", i, err)
		}
	}

}

const (
	createWithIndex = `
CREATE TABLE t1 (
  id bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'primary key',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  session_id char(36) NOT NULL COMMENT '会话标识',
  is_locked tinyint(1) NOT NULL DEFAULT '0' COMMENT '会话所有者是否持有文档锁',
  expired_at datetime NOT NULL COMMENT '会话过期时间',
  application_id bigint(20) NOT NULL COMMENT '应用 id',
  branch_name varchar(191) NOT NULL COMMENT '分支名',
  doc_name varchar(191) NOT NULL COMMENT '文档名, 也即服务名',
  creator_id varchar(191) NOT NULL COMMENT '创建者 id',
  updater_id varchar(191) NOT NULL COMMENT '更新者 id',
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc (application_id,branch_name,doc_name)
) ENGINE=InnoDB AUTO_INCREMENT=418 DEFAULT CHARSET=utf8mb4 COMMENT='API 设计中心文档锁表'
`
	dropPrimary = "DROP INDEX `PRIMARY` ON t1"
	dropIndex   = "DROP INDEX uk_doc ON t1"

	alterTableOption         = "alter table t1 comment 'new comment'"
	alterTableAddColumn      = "alter table t1 add column new_col bigint"
	alterTableAddConstraint  = "ALTER TABLE t1 ADD CONSTRAINT MyUniqueConstraint UNIQUE(session_id, doc_name);"
	alterTableDropColumn     = "alter table t1 drop doc_name"
	alterTableDropPrimaryKey = "alter table t1 drop primary key"
	alterTableDropIndex      = "ALTER TABLE t1 DROP INDEX uk_doc"
	alterTableDropForeignKey = "alter table t1 drop foreign key k1"
	alterTableModifyColumn   = "alter table t1 modify doc_name bigint"
	alterTableRenameColumn   = "alter table t1 rename column doc_name to docname"
	alterTableRenameTable    = "alter table t1 rename to t2"
	alterTableRenameIndex    = "alter table t1 rename index uk_doc to uk_doc2"
)

func TestReverseAlterWithCompares(t *testing.T) {
	node, err := parser.New().ParseOneStmt(createWithIndex, "", "")
	if err != nil {
		t.Error(err)
	}
	createNode := node.(*ast.CreateTableStmt)

	for _, alter := range []string{
		alterTableOption,
		alterTableAddColumn,
		alterTableAddConstraint,
		alterTableDropColumn,
		alterTableDropPrimaryKey,
		alterTableDropIndex,
		alterTableDropForeignKey,
		alterTableModifyColumn,
		alterTableRenameColumn,
		alterTableRenameTable,
		alterTableRenameIndex,
	} {
		stmt, err := parser.New().ParseOneStmt(alter, "", "")
		if err != nil {
			t.Fatal("failed to ParseOneStmt", alter, err)
		}
		reversing, _, err := ddlreverser.ReverseAlterWithCompares(createNode, stmt.(*ast.AlterTableStmt))
		if err == nil {
			t.Log(reversing)
		} else {
			t.Log(err)
		}
	}

}

func TestReverseAlterWithCompares_EarlyReturn(t *testing.T) {
	if _, _, err := ddlreverser.ReverseAlterWithCompares(nil, new(ast.AlterTableStmt)); err == nil {
		t.Fatal(err)
	}
	if _, _, err := ddlreverser.ReverseAlterWithCompares(new(ast.CreateTableStmt), nil); err == nil {
		t.Fatal(err)
	}
	create := new(ast.CreateTableStmt)
	create.Table = new(ast.TableName)
	create.Table.Name = model.NewCIStr("t1")
	alter := new(ast.AlterTableStmt)
	alter.Table = new(ast.TableName)
	alter.Table.Name = model.NewCIStr("t2")
	if _, _, err := ddlreverser.ReverseAlterWithCompares(create, alter); err == nil {
		t.Fatal(err)
	}
}

type S struct {
	nodes []ast.DDLNode
}

func (s S) DDLNodes() []ast.DDLNode {
	return s.nodes
}

func TestReverseCreateTableStmtsToDropTableStmts(t *testing.T) {
	node, err := parser.New().ParseOneStmt(createWithIndex, "", "")
	if err != nil {
		t.Fatal(err)
	}

	var s = S{nodes: []ast.DDLNode{node.(*ast.CreateTableStmt)}}
	reversing := ddlreverser.ReverseCreateTableStmts(s)
	t.Log(reversing)
}

func TestReverseDropIndexStmtWithCompares(t *testing.T) {
	node, err := parser.New().ParseOneStmt(createWithIndex, "", "")
	if err != nil {
		t.Fatal(err)
	}
	createNode := node.(*ast.CreateTableStmt)

	dropPrimaryNode, err := parser.New().ParseOneStmt(dropPrimary, "", "")
	if err != nil {
		t.Fatal(err)
	}
	if _, _, err = ddlreverser.ReverseDropIndexStmtWithCompares(createNode, dropPrimaryNode.(*ast.DropIndexStmt)); err == nil {
		t.Fatal("fails")
	}

	dropIndexNode, err := parser.New().ParseOneStmt(dropIndex, "", "")
	if err != nil {
		t.Fatal(err)
	}
	reversing, _, err := ddlreverser.ReverseDropIndexStmtWithCompares(createNode, dropIndexNode.(*ast.DropIndexStmt))
	if err != nil {
		t.Fatal(err)
	}
	t.Logf("reversed drop index: %s", reversing)
}

func TestReverseCreateTableStmt(t *testing.T) {
	if reversing := ddlreverser.ReverseCreateTableStmt(nil); reversing != "" {
		t.Fatal("fails: ", reversing)
	}

	create := new(ast.CreateTableStmt)
	create.Table = new(ast.TableName)
	create.Table.Name = model.NewCIStr("t1")
	reversing := ddlreverser.ReverseCreateTableStmt(create)
	t.Log(reversing)
}
