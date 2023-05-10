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

package ddlconv_test

import (
	"os"
	"testing"

	"github.com/erda-project/erda/pkg/swagger/ddlconv"
)

const createsql = `
create table base_model (
id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  created_at datetime DEFAULT NULL,
  updated_at datetime DEFAULT NULL
	);

CREATE TABLE dice_api_assets (
  
  asset_id varchar(191) DEFAULT NULL comment 'asset id comment',
  asset_name varchar(191) DEFAULT NULL,
  -- desc varchar(1024) DEFAULT NULL,
  logo varchar(1024) DEFAULT NULL,
  org_id bigint(20) DEFAULT NULL,
  project_id bigint(20) DEFAULT NULL,
  app_id bigint(20) DEFAULT NULL,
  creator_id varchar(191) DEFAULT NULL,
  updater_id varchar(191) DEFAULT NULL,

  PRIMARY KEY (id)
) ENGINE = InnoDB AUTO_INCREMENT = 1 DEFAULT CHARSET = utf8mb4 comment 'haha';`

const altertable = `alter table dice_api_assets
    add public  boolean default false comment 'public',
	drop column id,
	modify asset_id varchar(191) null comment 'this is asset id',
	modify creator_id int,
	change asset_name asset_name_2 varchar(191) null comment 'asset name';
`

// go test -v -run TestNewOnlySchemaOpenapi
func TestNewOnlySchemaOpenapi(t *testing.T) {
	openapi, err := ddlconv.New(createsql + "\n" + altertable)
	if err != nil {
		t.Errorf("failed to NewOnlySchemaOpenapi: %v", err)
	}
	t.Log(string(openapi.YAML()))
	file, err := os.OpenFile("test1.yml", os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0644)
	if err != nil {
		t.Error(err)
	}
	defer file.Close()

	if _, err := file.Write(openapi.YAML()); err != nil {
		t.Error(err)
	}

	t.Log(string(openapi.JSON()))
}
