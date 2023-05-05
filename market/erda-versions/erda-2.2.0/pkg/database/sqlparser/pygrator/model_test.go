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

package pygrator_test

import (
	"os"
	"testing"

	"github.com/pingcap/parser"
	"github.com/pingcap/parser/ast"
	_ "github.com/pingcap/tidb/types/parser_driver"

	"github.com/erda-project/erda/pkg/database/sqlparser/pygrator"
)

var createStmt = `
CREATE TABLE dice_api_access
(
    id                bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    asset_id          varchar(191) DEFAULT NULL COMMENT 'asset id',
    asset_name        varchar(191) DEFAULT NULL COMMENT 'asset name',
    org_id            bigint(20) DEFAULT NULL COMMENT 'organization id',
    swagger_version   varchar(16)  DEFAULT NULL COMMENT 'swagger version',
    major             int(11) DEFAULT NULL COMMENT 'version major number',
    minor             int(11) DEFAULT NULL COMMENT 'version minor number',
    project_id        bigint(20) DEFAULT NULL COMMENT 'project id',
    app_id            bigint(20) DEFAULT NULL COMMENT 'application id',
    workspace         varchar(32)  DEFAULT NULL COMMENT 'DEV, TEST, STAGING, PROD',
    endpoint_id       varchar(32)  DEFAULT NULL COMMENT 'gateway endpoint id',
    authentication    varchar(32)  DEFAULT NULL COMMENT 'api-key, parameter-sign, auth2',
    authorization     varchar(32)  DEFAULT NULL COMMENT 'auto, manual',
    addon_instance_id varchar(128) DEFAULT NULL COMMENT 'addon instance id',
    bind_domain       varchar(256) DEFAULT NULL COMMENT 'bind domains',
    creator_id        varchar(191) DEFAULT NULL COMMENT 'creator user id',
    updater_id        varchar(191) DEFAULT NULL COMMENT 'updater user id',
    created_at        datetime     DEFAULT NULL COMMENT 'created datetime',
    updated_at        datetime     DEFAULT NULL COMMENT 'last updated datetime',
    project_name      varchar(191) DEFAULT NULL COMMENT 'project name',
    app_name          varchar(191) DEFAULT NULL COMMENT 'app name',
    default_sla_id    bigint(20) DEFAULT NULL COMMENT 'default SLA id',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='API 集市资源访问管理表';
`

func TestGenModel(t *testing.T) {
	stmt, err := parser.New().ParseOneStmt(createStmt, "", "")
	if err != nil {
		t.Fatalf("failed to ParseOneStmt, err: %v", err)
	}
	create := stmt.(*ast.CreateTableStmt)
	model, err := pygrator.CreateTableStmtToModel(create)
	if err != nil {
		t.Fatalf("failed to CreateTableStmtToModel: %v", err)
	}
	if err = pygrator.GenModel(os.Stdout, *model); err != nil {
		t.Fatalf("failed to GenModel: %v", err)
	}
}
