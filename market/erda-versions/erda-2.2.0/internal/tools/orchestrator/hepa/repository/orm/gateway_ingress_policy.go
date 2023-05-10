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

type GatewayIngressPolicy struct {
	Name            string `json:"name" xorm:"not null default '' comment('名称') VARCHAR(32)"`
	Regions         string `json:"regions" xorm:"not null default '' comment('作用域') VARCHAR(128)"`
	Az              string `json:"az" xorm:"not null comment('集群名') VARCHAR(32)"`
	ZoneId          string `json:"zone_id" xorm:"default null comment('所属的zone') VARCHAR(32)"`
	Config          []byte `json:"config" xorm:"comment('具体配置') BLOB"`
	ConfigmapOption []byte `json:"configmap_option" xorm:"comment('ingress configmap option') BLOB"`
	MainSnippet     []byte `json:"main_snippet" xorm:"comment('ingress configmap main 配置') BLOB"`
	HttpSnippet     []byte `json:"http_snippet" xorm:"comment('ingress configmap http 配置') BLOB"`
	ServerSnippet   []byte `json:"server_snippet" xorm:"comment('ingress configmap server 配置') BLOB"`
	Annotations     []byte `json:"annotations" xorm:"comment('包含的annotations') BLOB"`
	LocationSnippet []byte `json:"location_snippet" xorm:"comment('nginx location 配置') BLOB"`
	BaseRow         `xorm:"extends"`
}
