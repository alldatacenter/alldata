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

type GatewayRoute struct {
	RouteId   string `json:"route_id" xorm:"not null default '' comment('路由id') index VARCHAR(128)"`
	Protocols string `json:"protocols" xorm:"comment('协议列表') VARCHAR(128)"`
	Methods   string `json:"methods" xorm:"comment('方法列表') VARCHAR(128)"`
	Hosts     string `json:"hosts" xorm:"comment('主机列表') VARCHAR(256)"`
	Paths     string `json:"paths" xorm:"comment('路径列表') VARCHAR(256)"`
	ServiceId string `json:"service_id" xorm:"not null default '' comment('绑定服务id') VARCHAR(32)"`
	Config    string `json:"config" xorm:"default '' comment('选填配置') VARCHAR(1024)"`
	ApiId     string `json:"api_id" xorm:"not null default '' comment('apiid') VARCHAR(32)"`
	BaseRow   `xorm:"extends"`
}
