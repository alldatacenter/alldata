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

type GatewayPackageRule struct {
	ApiId           string `json:"api_id" xorm:"comment('产品包api id') VARCHAR(32)"`
	Category        string `json:"category" xorm:"not null default '' comment('插件类目') VARCHAR(32)"`
	Config          []byte `json:"config" xorm:"comment('具体配置') BLOB"`
	ConsumerId      string `json:"consumer_id" xorm:"not null default '' comment('消费者id') VARCHAR(32)"`
	ConsumerName    string `json:"consumer_name" xorm:"not null default '' comment('消费者名称') VARCHAR(128)"`
	DiceEnv         string `json:"dice_env" xorm:"not null comment('dice环境') VARCHAR(32)"`
	DiceOrgId       string `json:"dice_org_id" xorm:"not null comment('dice企业标识id') VARCHAR(32)"`
	DiceProjectId   string `json:"dice_project_id" xorm:"default '' comment('dice项目标识id') VARCHAR(32)"`
	DiceClusterName string `json:"dice_cluster_name" xorm:"not null comment('dice集群名') VARCHAR(32)"`
	Enabled         int    `json:"enabled" xorm:"default 1 comment('插件开关') TINYINT(1)"`
	PackageZoneNeed int    `json:"package_zone_need" xorm:"default 1 comment('是否在pcakge的zone生效') TINYINT(1)"`
	PackageId       string `json:"package_id" xorm:"not null default '' comment('产品包id') VARCHAR(32)"`
	PackageName     string `json:"package_name" xorm:"not null comment('产品包名称') VARCHAR(32)"`
	PluginId        string `json:"plugin_id" xorm:"not null default '' comment('插件id') VARCHAR(128)"`
	PluginName      string `json:"plugin_name" xorm:"not null default '' comment('插件名称') VARCHAR(128)"`
	BaseRow         `xorm:"extends"`
}
