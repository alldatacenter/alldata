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

type GatewayPolicy struct {
	ZoneId      string `json:"zone_id" xorm:"default '' comment('所属的zone') VARCHAR(32)"`
	PolicyName  string `json:"policy_name" xorm:"default '' comment('策略名称') VARCHAR(128)"`
	DisplayName string `json:"display_name" xorm:"not null default '' comment('策略展示名称') VARCHAR(128)"`
	Category    string `json:"category" xorm:"not null default '' comment('策略类目') VARCHAR(128)"`
	Description string `json:"description" xorm:"not null default '' comment('描述类目') VARCHAR(128)"`
	PluginId    string `json:"plugin_id" xorm:"default '' comment('插件id') VARCHAR(128)"`
	PluginName  string `json:"plugin_name" xorm:"not null default '' comment('插件名称') VARCHAR(128)"`
	Config      []byte `json:"config" xorm:"comment('具体配置') BLOB"`
	ConsumerId  string `json:"consumer_id" xorm:"default null comment('消费者id') VARCHAR(32)"`
	Enabled     int    `json:"enabled" xorm:"default 1 comment('插件开关') TINYINT(1)"`
	ApiId       string `json:"api_id" xorm:"comment('api id') VARCHAR(32)"`
	BaseRow     `xorm:"extends"`
}
