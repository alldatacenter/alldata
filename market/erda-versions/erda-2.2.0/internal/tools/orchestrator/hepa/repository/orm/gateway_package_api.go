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

type GatewayPackageApi struct {
	PackageId        string `json:"package_id" xorm:"default '' comment('所属的产品包id') VARCHAR(32)"`
	ApiPath          string `json:"api_path" xorm:"not null default '' comment('api路径') VARCHAR(256)"`
	Method           string `json:"method" xorm:"not null default '' comment('方法') VARCHAR(128)"`
	RedirectAddr     string `json:"redirect_addr" xorm:"not null default '' comment('转发地址') VARCHAR(256)"`
	RedirectPath     string `json:"redirect_path" xorm:"not null default '' comment('转发路径') VARCHAR(256)"`
	Description      string `json:"description" xorm:"comment('描述') VARCHAR(256)"`
	DiceApp          string `json:"dice_app" xorm:"default '' comment('dice应用名') VARCHAR(128)"`
	DiceService      string `json:"dice_service" xorm:"default '' comment('dice服务名') VARCHAR(128)"`
	AclType          string `json:"acl_type" xorm:"default '' comment('独立的授权类型') VARCHAR(16)"`
	Origin           string `json:"origin" xorm:"not null default 'custom' comment('来源') VARCHAR(16)"`
	DiceApiId        string `json:"dice_api_id" xorm:"comment('对应dice服务api的id') VARCHAR(32)"`
	RedirectType     string `json:"redirect_type" xorm:"not null default 'url' comment('转发类型') VARCHAR(32)"`
	RuntimeServiceId string `json:"runtime_service_id" xorm:"not null default '' comment('关联的service的id') VARCHAR(32)"`
	ZoneId           string `json:"zone_id" xorm:"comment('所属的zone') VARCHAR(32)"`
	CloudapiApiId    string `json:"cloudapi_api_id" xorm:"not null default '' comment('阿里云API网关的api id') VARCHAR(128)"`
	BaseRow          `xorm:"extends"`
}
