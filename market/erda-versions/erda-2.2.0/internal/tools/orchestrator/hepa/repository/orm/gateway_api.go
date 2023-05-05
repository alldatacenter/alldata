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

type GatewayApi struct {
	ZoneId           string `json:"zone_id" xorm:"default '' comment('所属的zone') VARCHAR(32)"`
	ConsumerId       string `json:"consumer_id" xorm:"not null default '' comment('消费者id') VARCHAR(32)"`
	ApiPath          string `json:"api_path" xorm:"not null default '' comment('api路径') VARCHAR(256)"`
	Method           string `json:"method" xorm:"not null default '' comment('方法') VARCHAR(128)"`
	RedirectAddr     string `json:"redirect_addr" xorm:"not null default '' comment('转发地址') VARCHAR(256)"`
	Description      string `json:"description" xorm:"comment('描述') VARCHAR(256)"`
	GroupId          string `json:"group_id" xorm:"not null default '' comment('服务Id') VARCHAR(32)"`
	Policies         string `json:"policies" xorm:"comment('策略配置') VARCHAR(1024)"`
	UpstreamApiId    string `json:"upstream_api_id" xorm:"not null default '' comment('对应的后端api') VARCHAR(32)"`
	DiceApp          string `json:"dice_app" xorm:"default '' comment('dice应用名') varchar(128)"`
	DiceService      string `json:"dice_service" xorm:"default '' comment('dice服务名') varchar(128)"`
	RegisterType     string `json:"register_type" xorm:"not null default 'auto' comment('注册类型') varchar(16)"`
	NetType          string `json:"net_type" xorm:"not null default 'outer' comment('网络类型') varchar(16)"`
	NeedAuth         int    `json:"need_auth" xorm:"not null default 0 comment('需要鉴权标识') TINYINT(1)"`
	RedirectType     string `json:"redirect_type" xorm:"not null default 'url' comment('转发类型') VARCHAR(32)"`
	RuntimeServiceId string `json:"runtime_service_id" xorm:"not null default '' comment('关联的service的id') VARCHAR(32)"`
	Swagger          []byte `json:"swagger" xorm:"comment('swagger文档') BLOB"`
	BaseRow          `xorm:"extends"`
}
