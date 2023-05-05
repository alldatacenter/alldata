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

package orm

type GatewayUpstream struct {
	ZoneId           string `json:"zone_id" xorm:"default '' comment('所属的zone') VARCHAR(32)"`
	OrgId            string `json:"org_id" xorm:"not null comment('企业标识id') VARCHAR(32)"`
	ProjectId        string `json:"project_id" xorm:"not null comment('项目标识id') VARCHAR(32)"`
	UpstreamName     string `json:"upstream_name" xorm:"not null comment('应用名称') VARCHAR(128)"`
	DiceApp          string `json:"dice_app" xorm:"default '' comment('dice应用名') varchar(128)"`
	DiceService      string `json:"dice_service" xorm:"default '' comment('dice服务名') varchar(128)"`
	Env              string `json:"env" xorm:"not null comment('应用所属环境') VARCHAR(32)"`
	Az               string `json:"az" xorm:"not null comment('集群名') VARCHAR(32)"`
	LastRegisterId   string `json:"last_register_id" xorm:"not null comment('应用最近一次注册id') VARCHAR(64)"`
	ValidRegisterId  string `json:"valid_register_id" xorm:"not null comment('应用当前生效的注册id') VARCHAR(64)"`
	AutoBind         int    `json:"auto_bind" xorm:"not null default 1 comment('api是否自动绑定') TINYINT(1)"`
	RuntimeServiceId string `json:"runtime_service_id" xorm:"not null default '' comment('关联的service的id') VARCHAR(32)"`
	BaseRow          `xorm:"extends"`
}
