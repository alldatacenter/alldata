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

package migrator_test

import (
	"testing"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"
	_ "github.com/pingcap/tidb/types/parser_driver"

	"github.com/erda-project/erda/pkg/database/sqlparser/migrator"
)

const createStmt = `
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
`

const alters = `
ALTER TABLE dice_api_doc_lock
    COMMENT='修改了的 table option';

ALTER TABLE dice_api_doc_lock
    ADD (
        new_col1 varchar(16) not null comment 'this is a new column',
        new_col2 bigint not null comment ''
    );

ALTER TABLE dice_api_doc_lock
    ADD CONSTRAINT uk_new_constraint UNIQUE KEY (doc_name);

ALTER TABLE dice_api_doc_lock
    MODIFY new_col1 varchar(1024) not null default 'default text' comment 'modified the column';

ALTER TABLE dice_api_doc_lock
    CHANGE new_col1 new_col1 text not null comment 'change the column';

-- ALTER TABLE doc_lock
--     ALTER new_col1 set default 99;

ALTER TABLE dice_api_doc_lock
    RENAME INDEX uk_doc TO uk_doc2;
`

const rawCreate = `
CREATE TABLE dice_api_doc_lock (
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
  new_col1 text NOT NULL COMMENT 'change the column',
  new_col2 bigint(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_doc2 (application_id, branch_name, doc_name),
  UNIQUE KEY uk_new_constraint (doc_name)
) ENGINE=InnoDB AUTO_INCREMENT=399 DEFAULT CHARSET=utf8mb4 COMMENT='修改了的 table option'
`

func TestTableDefinition_Enter(t *testing.T) {
	c, err := parser.New().ParseOneStmt(createStmt, "", "")
	if err != nil {
		t.Fatalf("failed to ParseOneStmt, err: %v", err)
	}

	nodes, _, err := parser.New().Parse(alters, "", "")
	if err != nil {
		t.Fatalf("failed to Parse, err: %v", err)
	}

	def := migrator.TableDefinition{CreateStmt: c.(*ast.CreateTableStmt)}
	for _, node := range nodes {
		node.Accept(&def)
	}

	for _, col := range def.CreateStmt.Cols {
		t.Logf("column name: %s, column type: %s, %+v", col.Name.String(), col.Tp.String(), *col.Tp)
	}
}

func TestSchema_Enter(t *testing.T) {
	schema := migrator.NewSchema()

	nodes, _, err := parser.New().Parse(createStmt+alters, "", "")
	if err != nil {
		t.Logf("failed to Parse, err: %v", err)
	}
	for _, node := range nodes {
		node.Accept(schema)
	}

	for _, tbl := range schema.TableDefinitions {
		for _, col := range tbl.CreateStmt.Cols {
			t.Logf("column name: %s, column type: %s, %+v", col.Name.String(), col.Tp.String(), *col.Tp)
		}
	}
}

func TestSchema_Enter2(t *testing.T) {
	local := migrator.NewSchema()
	db := migrator.NewSchema()

	nodes, _, err := parser.New().Parse(createStmt+alters, "", "")
	if err != nil {
		t.Logf("failed to Parse, err: %v", err)
	}
	for _, node := range nodes {
		node.Accept(local)
	}

	node, err := parser.New().ParseOneStmt(rawCreate, "", "")
	if err != nil {
		t.Errorf("failed to PasrseOneStmt, err: %v", err)
	}
	node.Accept(db)

	t.Logf("is local equal db: %+v", local.Equal(db))
}
