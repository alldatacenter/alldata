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

type GatewayKongInfo struct {
	Az              string `json:"az" xorm:"not null comment('集群名') VARCHAR(32)"`
	Env             string `json:"env" xorm:"default '' comment('环境名') VARCHAR(32)"`
	ProjectId       string `json:"project_id" xorm:"not null comment('项目id') VARCHAR(32)"`
	ProjectName     string `json:"project_name" xorm:"not null comment('项目名') VARCHAR(256)"`
	KongAddr        string `json:"kong_addr" xorm:"not null comment('kong admin地址') VARCHAR(256)"`
	Endpoint        string `json:"endpoint" xorm:"not null comment('kong gateway地址') VARCHAR(256)"`
	NeedUpdate      int    `json:"need_update" xorm:"default 1 comment('待更新标识') TINYINT(1)"`
	InnerAddr       string `json:"inner_addr" xorm:"not null default '' comment('kong内网地址') varchar(1024)"`
	ServiceName     string `josn:"service_name" xorm:"not null default '' comment('kong的服务名称') varchar(32)"`
	AddonInstanceId string `json:"addon_instance_id" xorm:"not null default '' comment('addon id') varchar(32)"`
	TenantId        string `json:"tenant_id" xorm:"not null default '' comment('租户id') VARCHAR(128)"`
	TenantGroup     string `json:"tenant_group" xorm:"not null default '' comment('租户分组') VARCHAR(128)"`
	BaseRow         `xorm:"extends"`
}
