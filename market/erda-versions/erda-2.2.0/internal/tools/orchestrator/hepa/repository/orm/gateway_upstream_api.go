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

type GatewayUpstreamApi struct {
	UpstreamId  string `json:"upstream_id" xorm:"not null comment('应用标识id') VARCHAR(32)"`
	RegisterId  string `json:"register_id" xorm:"not null comment('应用注册id') VARCHAR(64)"`
	ApiName     string `json:"api_name" xorm:"not null comment('标识api的名称，应用下唯一') VARCHAR(256)"`
	Path        string `json:"path" xorm:"not null comment('注册的api路径') VARCHAR(256)"`
	GatewayPath string `json:"gateway_path" xorm:"not null comment('网关api路径') VARCHAR(256)"`
	Method      string `json:"method" xorm:"not null comment('注册的api方法') VARCHAR(256)"`
	Address     string `json:"address" xorm:"not null comment('注册的转发地址') VARCHAR(256)"`
	Doc         []byte `json:"doc" xorm:"comment('api描述') BLOB"`
	ApiId       string `json:"api_id" xorm:"default '' comment('api标识id') VARCHAR(32)"`
	IsInner     int    `json:"is_inner" xorm:"not null default 0 comment('是否是内部api') TINYINT(1)"`
	BaseRow     `xorm:"extends"`
}
