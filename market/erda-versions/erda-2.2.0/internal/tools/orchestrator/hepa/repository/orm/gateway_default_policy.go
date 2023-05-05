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

const (
	POLICY_PROJECT_LEVEL = "project" //已废弃
	POLICY_PACKAGE_LEVEL = "package"
)

type GatewayDefaultPolicy struct {
	Config    []byte `json:"config" xorm:"comment('具体配置') BLOB"`
	DiceApp   string `json:"dice_app" xorm:"default '' comment('dice应用名') VARCHAR(128)"`
	Level     string `json:"level" xorm:"not null comment('策略级别') VARCHAR(32)"`
	Name      string `json:"name" xorm:"not null default '' comment('名称') VARCHAR(32)"`
	TenantId  string `json:"tenant_id" xorm:"not null default '' comment('租户id') VARCHAR(128)"`
	PackageId string `json:"package_id" xorm:"not null default '' comment('流量入口id') VARCHAR(32)"`
	BaseRow   `xorm:"extends"`
}
