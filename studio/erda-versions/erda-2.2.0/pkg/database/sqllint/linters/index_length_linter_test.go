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
)

const indexLengthLinterConfig = `- name: IndexLengthLinter
  switchOn: true
  white:
    patterns:
      - ".*-base$"`

const indexLengthLinterSQL = `
create table some_table (
	name varchar(101),
	index idx_name (name(200))
);

create table some_table (
	some_text varchar(200),
	index idx_some_text (some_text(100))
);

create table some_table (
	some_text varchar(200),
	index idx_some_text (some_text)
);

create table some_table (
	name varchar(300),
	some_text varchar(500),
	index idx_name (name, some_text)
);
`

const indexLengthLinterSQL2 = `CREATE TABLE fdp_metadata_request_error_msg (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  action varchar(255)  NOT NULL COMMENT '执行步骤',
  url varchar(255) NOT NULL COMMENT '请求路径',
  error_msg text NOT NULL COMMENT '错误信息',
  query_type varchar(255) NOT NULL COMMENT 'httt请求类型',
  body text NOT NULL COMMENT 'body内容',
  params varchar(255) NOT NULL DEFAULT '' COMMENT '参数内容',
  header varchar(255) NOT NULL DEFAULT '' COMMENT 'header内容',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE current_timestamp() COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='元数据异常信息记录表';`

const indexLengthLinterSQL3 = `CREATE TABLE fdp_metadata_request_error_msg (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'id',
  action varchar(255)  NOT NULL COMMENT '执行步骤',
  url varchar(255) NOT NULL COMMENT '请求路径',
  error_msg text NOT NULL COMMENT '错误信息',
  query_type varchar(255) NOT NULL COMMENT 'httt请求类型',
  body text NOT NULL COMMENT 'body内容',
  params varchar(255) NOT NULL DEFAULT '' COMMENT '参数内容',
  header varchar(255) NOT NULL DEFAULT '' COMMENT 'header内容',
  created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE current_timestamp() COMMENT '更新时间',
  PRIMARY KEY (id),
  index idx_name (body)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='元数据异常信息记录表';`

func TestNewIndexLengthLinter(t *testing.T) {
	t.Run("testNewIndexLengthLinter1", testNewIndexLengthLinter1)
	t.Run("testNewIndexLengthLinter2", testNewIndexLengthLinter2)
	t.Run("testNewIndexLengthLinter3", testNewIndexLengthLinter3)
}

func testNewIndexLengthLinter1(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(indexLengthLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	linter := sqllint.New(cfg)
	if err := linter.Input("", "indexLengthLinterSQL", []byte(indexLengthLinterSQL)); err != nil {
		t.Error(err)
	}
	errors := linter.Errors()
	t.Logf("errors: %v", errors)
	if len(errors) == 0 {
		t.Fatal("failed")
	}
}

func testNewIndexLengthLinter2(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(indexLengthLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	name := "indexLengthLinterSQL2"
	linter := sqllint.New(cfg)
	if err := linter.Input("", name, []byte(indexLengthLinterSQL2)); err != nil {
		t.Fatal(err)
	}
	errors := linter.Errors()[name].Lints
	if len(errors) > 0 {
		t.Fatal(errors)
	}
}

func testNewIndexLengthLinter3(t *testing.T) {
	cfg, err := sqllint.LoadConfig([]byte(indexLengthLinterConfig))
	if err != nil {
		t.Fatal("failed to LoadConfig", err)
	}
	name := "indexLengthLinterSQL3"
	linter := sqllint.New(cfg)
	if err := linter.Input("", name, []byte(indexLengthLinterSQL3)); err != nil {
		t.Fatal(err)
	}
	errors := linter.Errors()[name].Lints
	t.Logf("errors: %v", errors)
	if len(errors) == 0 {
		t.Fatal("fails")
	}
}
