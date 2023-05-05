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

package snapshot_test

import (
	"bytes"
	"os"
	"strings"
	"testing"

	"github.com/pingcap/parser/ast"
	"github.com/pingcap/parser/format"
	_ "github.com/pingcap/tidb/types/parser_driver"

	"github.com/pingcap/parser"

	"github.com/erda-project/erda/pkg/database/sqlparser/snapshot"
)

const sqlWithCollate = "CREATE TABLE `fdp_master_reco_scenario_workflows` (`id` BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键id',`org_id` BIGINT(20) DEFAULT NULL COMMENT '企业 id',`scenario_name` VARCHAR(128) NOT NULL DEFAULT '',`scenario_code` VARCHAR(128) NOT NULL DEFAULT '',`process_type` VARCHAR(128) NOT NULL COMMENT '处理类型',`name` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '工作流名',`name_pinyin` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '工作流拼音',`description` VARCHAR(1024) NOT NULL DEFAULT '' COMMENT '工作流描述',`source` VARCHAR(32) DEFAULT '' COMMENT 'workflow 来源: dl/cdp',`run_type` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '运行类型: ATONCE、SCHEDULE',`cron` VARCHAR(128) DEFAULT '' COMMENT '任务周期',`cron_start_from` DATETIME DEFAULT NULL COMMENT '延时执行时间',`category_id` BIGINT(20) NOT NULL COMMENT '工作流目录 ID',`pipeline_name` VARCHAR(128) DEFAULT '' COMMENT 'pipeline 名称',`pipeline` MEDIUMTEXT NOT NULL COMMENT 'pipeline 内容',`pipeline_id` BIGINT(20) DEFAULT NULL COMMENT 'pipeline id',`node_params` TEXT COMMENT '工作流节点参数',`locations` VARCHAR(1024) DEFAULT NULL,`creator_id` VARCHAR(128) CHARACTER SET UTF8MB4 COLLATE utf8mb4_0900_ai_ci DEFAULT '' COMMENT '创建者 ID',`updater_id` VARCHAR(128) DEFAULT NULL COMMENT '更新者ID',`extra` MEDIUMTEXT COMMENT '扩展信息',`delete_yn` TINYINT(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',`created_at` DATETIME DEFAULT NULL COMMENT '创建时间',`updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',PRIMARY KEY(`id`),INDEX `idx_category_id_name`(`scenario_code`, `process_type`)) ENGINE = InnoDB DEFAULT CHARACTER SET = UTF8 COMMENT = '场景工作流表'"
const sqlWithConstraintCheck = "CREATE TABLE `migration_records` (`version` VARCHAR(10) NOT NULL COMMENT '服务版本号-',`module` VARCHAR(50) NOT NULL COMMENT '服务名',`version_b` VARCHAR(10) NOT NULL COMMENT '服务 B 版本号',`done` VARCHAR(1) NOT NULL DEFAULT '0',`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() COMMENT '创建时间',PRIMARY KEY(`version`, `module`, `version_b`),CONSTRAINT `migration_records_chk_1` CHECK(((`done`='0') OR (`done`='1'))) ENFORCED) ENGINE = InnoDB DEFAULT CHARACTER SET = UTF8MB4"
const (
	sqlWithUtf32_1 = "CREATE TABLE `migration_records` (`version` VARCHAR(10) NOT NULL COMMENT '服务版本号-',`module` VARCHAR(50) NOT NULL COMMENT '服务名',`version_b` VARCHAR(10) NOT NULL COMMENT '服务 B 版本号',`done` VARCHAR(1) NOT NULL DEFAULT '0',`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() COMMENT '创建时间',PRIMARY KEY(`version`, `module`, `version_b`),CONSTRAINT `migration_records_chk_1` CHECK(((`done`='0') OR (`done`='1'))) ENFORCED) ENGINE = InnoDB DEFAULT CHARACTER SET = UTF32"
	sqlWithUtf32_2 = "CREATE TABLE `migration_records` (`version` VARCHAR(10) NOT NULL COMMENT '服务版本号-',`module` VARCHAR(50) NOT NULL COMMENT '服务名',`version_b` VARCHAR(10) NOT NULL COMMENT '服务 B 版本号',`done` VARCHAR(1) NOT NULL DEFAULT '0',`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() COMMENT '创建时间',PRIMARY KEY(`version`, `module`, `version_b`),CONSTRAINT `migration_records_chk_1` CHECK(((`done`='0') OR (`done`='1'))) ENFORCED) ENGINE = InnoDB character SET = UTF32"
	sqlWithUtf8mb3 = `
CREATE TABLE ci_v3_build_caches (
  id bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(200) DEFAULT NULL COMMENT '缓存名',
  cluster_name varchar(200) DEFAULT NULL COMMENT '集群名',
  last_pull_at datetime DEFAULT NULL COMMENT '缓存最近一次被拉取的时间',
  created_at datetime DEFAULT NULL COMMENT '创建时间',
  updated_at datetime DEFAULT NULL COMMENT '更新时间',
  deleted_at datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb3 COMMENT='buildpack action 使用的构建缓存';
`
	sqlWithUtf8 = `
CREATE TABLE ci_v3_build_caches (
  id bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(200) DEFAULT NULL COMMENT '缓存名',
  cluster_name varchar(200) DEFAULT NULL COMMENT '集群名',
  last_pull_at datetime DEFAULT NULL COMMENT '缓存最近一次被拉取的时间',
  created_at datetime DEFAULT NULL COMMENT '创建时间',
  updated_at datetime DEFAULT NULL COMMENT '更新时间',
  deleted_at datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET utf8 COMMENT='buildpack action 使用的构建缓存';
`

	sqlWithUtf8mb4 = `
CREATE TABLE ci_v3_build_caches (
  id bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  name varchar(200) DEFAULT NULL COMMENT '缓存名',
  cluster_name varchar(200) DEFAULT NULL COMMENT '集群名',
  last_pull_at datetime DEFAULT NULL COMMENT '缓存最近一次被拉取的时间',
  created_at datetime DEFAULT NULL COMMENT '创建时间',
  updated_at datetime DEFAULT NULL COMMENT '更新时间',
  deleted_at datetime DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=23 DEFAULT CHARSET=utf8mb4 COMMENT='buildpack action 使用的构建缓存';
`
)

const blockFormatCase = `
CREATE TABLE t1 (
  id varchar(32) NOT NULL DEFAULT '' COMMENT '唯一id'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 BLOCK_FORMAT=ENCRYPTED COMMENT='Kong Route 配置信息'   
`

func TestTrimCollate(t *testing.T) {
	stmt, err := parser.New().ParseOneStmt(sqlWithCollate, "", "")
	if err != nil {
		t.Fatal(err)
	}
	create := stmt.(*ast.CreateTableStmt)
	for i := range create.Cols {
		t.Log(create.Cols[i].Name)
		for j := range create.Cols[i].Options {
			t.Log("\t", create.Cols[i].Options[j])
		}
	}
}

func TestTrimCollation(t *testing.T) {
	stmt, err := parser.New().ParseOneStmt(sqlWithCollate, "", "")
	if err != nil {
		t.Fatal(err)
	}
	create := stmt.(*ast.CreateTableStmt)
	testTrimCollateOptionFromCols(t, create)
	testTrimCollateOptionFromCreateTable(t, create)

	var buf = bytes.NewBuffer(nil)
	if err := create.Restore(&format.RestoreCtx{
		Flags:     format.DefaultRestoreFlags,
		In:        buf,
		JoinLevel: 0,
	}); err != nil {
		t.Fatal(err)
	}

	sql := buf.String()
	t.Log(sql)

	stmt, err = parser.New().ParseOneStmt(sql, "", "")
	if err != nil {
		t.Fatal(err)
	}
	create = stmt.(*ast.CreateTableStmt)
	for _, opt := range create.Options {
		if opt.Tp == ast.TableOptionCollate {
			t.Fatal("table option collate was not trimmed")
		}
	}

	for _, col := range create.Cols {
		for _, opt := range col.Options {
			if opt.Tp == ast.ColumnOptionCollate {
				t.Fatal("col optoin collate was not trimmed")
			}
		}
	}
}

func testTrimCollateOptionFromCols(t *testing.T, create *ast.CreateTableStmt) {
	snapshot.TrimCollateOptionFromCols(create)
}

func testTrimCollateOptionFromCreateTable(t *testing.T, create *ast.CreateTableStmt) {
	snapshot.TrimCollateOptionFromCreateTable(create)
}

func TestTrimConstraintCheckFromCreateTable(t *testing.T) {
	stmt, err := parser.New().ParseOneStmt(sqlWithConstraintCheck, "", "")
	if err != nil {
		t.Fatal(err)
	}
	create := stmt.(*ast.CreateTableStmt)
	snapshot.TrimConstraintCheckFromCreateTable(create)

	var buf = bytes.NewBuffer(nil)
	if err := create.Restore(&format.RestoreCtx{
		Flags:     format.DefaultRestoreFlags,
		In:        buf,
		JoinLevel: 0,
	}); err != nil {
		t.Fatal(err)
	}

	sql := buf.String()
	t.Log(sql)

	if strings.Contains(strings.ToLower(sql), "check") {
		t.Fatal("constraint check was not trimmed")
	}

	stmt, err = parser.New().ParseOneStmt(sql, "", "")
	if err != nil {
		t.Fatal(err)
	}
	create = stmt.(*ast.CreateTableStmt)
	for _, con := range create.Constraints {
		if con.Tp == ast.ConstraintCheck {
			t.Fatal("constraint check was not trimmed")
		}
	}
}

func TestTrimCharacterSetFromRawCreateTableSQL(t *testing.T) {
	for _, sql := range []string{sqlWithUtf32_1, sqlWithUtf32_2, sqlWithUtf8mb3} {
		sql := snapshot.TrimCharacterSetFromRawCreateTableSQL(sql, "utf8", "utf8mb4")
		if strings.Contains(strings.ToLower(sql), "utf32") {
			t.Fatal("failed to trim character from sql")
		}
		if _, err := parser.New().ParseOneStmt(sql, "", ""); err != nil {
			t.Fatalf("err: %v, sql: %s", err, sql)
		}
		t.Log(sql)
	}
}

func TestTrimCharacterSetFromRawCreateTableSQL2(t *testing.T) {
	for charset, sql := range map[string]string{
		"utf8":    sqlWithUtf8,
		"utf8mb4": sqlWithUtf8mb4,
	} {
		sql := snapshot.TrimCharacterSetFromRawCreateTableSQL(sql, "utf8", "utf8mb4")
		t.Logf("sql: %s", sql)
		if !strings.Contains(sql, charset) {
			t.Fatal("failed to trim character from sql")
		}
		if _, err := parser.New().ParseOneStmt(sql, "", ""); err != nil {
			t.Fatalf("failed to parse create table stmt: %v", err)
		}
	}
}

func TestParseCreateTableStmt(t *testing.T) {
	for i, sql := range []string{sqlWithUtf32_1, sqlWithUtf32_2, sqlWithUtf8mb3} {
		stmt, err := snapshot.ParseCreateTableStmt(sql)
		if err != nil {
			t.Fatalf("failed to ParseCreateTableStmt: [%v]: %s: %v", i, sql, err)
		}
		t.Log("raw text:", sql, "\nparsed text:", stmt.Text())
	}
}

func TestTrimBlockFormat(t *testing.T) {
	trimBlockFormat := snapshot.TrimBlockFormat(blockFormatCase)
	t.Log(trimBlockFormat)
}

func TestCharsetWhite(t *testing.T) {
	white := snapshot.CharsetWhite()
	var m = make(map[string]struct{})
	for _, v := range white {
		m[v] = struct{}{}
	}
	if _, ok := m["utf8"]; !ok {
		t.Fatal("utf8 is in default")
	}
	if _, ok := m["utf8mb4"]; !ok {
		t.Fatal("utf8mb4 is in default")
	}

	if err := os.Setenv("PIPELINE_MIGRATION_CHARSET_WHITE", "utf8mb4"); err != nil {
		t.Fatal(err)
	}
	white = snapshot.CharsetWhite()
	if len(white) != 1 || white[0] != "utf8mb4" {
		t.Fatal("utf8mb4 is set", white)
	}
}
