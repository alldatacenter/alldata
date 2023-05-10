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

type GatewayZoneInPackage struct {
	PackageId     string `json:"package_id" xorm:"default '' comment('所属的产品包id') VARCHAR(32)"`
	PackageZoneId string `json:"package_zone_id" xorm:"default '' comment('产品包的zone id') VARCHAR(32)"`
	RoutePrefix   string `json:"route_prefix" xorm:"not null comment('路由前缀') VARCHAR(128)"`
	ZoneId        string `json:"zone_id" xorm:"default '' comment('依赖的zone id') VARCHAR(32)"`
	BaseRow       `xorm:"extends"`
}
