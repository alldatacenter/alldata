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

type GatewayConsumerApi struct {
	ConsumerId string `json:"consumer_id" xorm:"not null default '' comment('消费者id') VARCHAR(32)"`
	ApiId      string `json:"api_id" xorm:"not null default '' comment('apiId') VARCHAR(32)"`
	Policies   string `json:"policies" xorm:"comment('策略信息') VARCHAR(512)"`
	BaseRow    `xorm:"extends"`
}
