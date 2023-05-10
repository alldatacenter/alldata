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

type GatewayPluginInstance struct {
	PluginId   string `json:"plugin_id" xorm:"not null default '' comment('插件id') index VARCHAR(128)"`
	PluginName string `json:"plugin_name" xorm:"not null default '' comment('插件名称') VARCHAR(128)"`
	PolicyId   string `json:"policy_id" xorm:"not null default '' comment('策略id') VARCHAR(32)"`
	ConsumerId string `json:"consumer_id" xorm:"comment('消费者id') VARCHAR(32)"`
	GroupId    string `json:"group_id" xorm:"comment('组id') VARCHAR(32)"`
	RouteId    string `json:"route_id" xorm:"comment('路由id') VARCHAR(32)"`
	ServiceId  string `json:"service_id" xorm:"comment('服务id') VARCHAR(32)"`
	ApiId      string `json:"api_id" xorm:"default '' comment('apiID') VARCHAR(32)"`
	BaseRow    `xorm:"extends"`
}
