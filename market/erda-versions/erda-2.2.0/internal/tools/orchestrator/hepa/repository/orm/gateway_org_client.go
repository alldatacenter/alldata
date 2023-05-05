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

type GatewayOrgClient struct {
	OrgId        string `json:"org_id" xorm:"not null VARCHAR(32)"`
	Name         string `json:"name" xorm:"not null default '' comment('消费者名称') VARCHAR(128)"`
	ClientSecret string `json:"client_secret" xorm:"not null default '' comment('客户端凭证') VARCHAR(32)"`
	BaseRow      `xorm:"extends"`
}
