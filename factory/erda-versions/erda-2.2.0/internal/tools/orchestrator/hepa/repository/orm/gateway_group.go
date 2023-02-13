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

type GatewayGroup struct {
	GroupName   string `json:"group_name" xorm:"not null default '' comment('组名') unique(group_consumer) VARCHAR(128)"`
	DispalyName string `json:"dispaly_name" xorm:"not null default '' comment('展示名称') VARCHAR(128)"`
	ConsumerId  string `json:"consumer_id" xorm:"not null default '' comment('所属消费者id') unique(group_consumer) VARCHAR(32)"`
	Policies    string `json:"policies" xorm:"comment('策略配置，存策略id') VARCHAR(1024)"`
	BaseRow     `xorm:"extends"`
}

func (group GatewayGroup) IsEmpty() bool {
	return len(group.GroupName) == 0
}
