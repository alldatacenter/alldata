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

type GatewayService struct {
	ServiceId   string `json:"service_id" xorm:"not null default '' comment('服务id') index VARCHAR(128)"`
	ServiceName string `json:"service_name" xorm:"comment('服务名称') VARCHAR(64)"`
	Url         string `json:"url" xorm:"comment('具体路径') VARCHAR(256)"`
	Protocol    string `json:"protocol" xorm:"comment('协议') VARCHAR(32)"`
	Host        string `json:"host" xorm:"comment('主机') VARCHAR(128)"`
	Port        string `json:"port" xorm:"comment('端口') VARCHAR(32)"`
	Path        string `json:"path" xorm:"comment('路径') VARCHAR(128)"`
	Config      string `json:"config" xorm:"comment('选填配置') VARCHAR(1024)"`
	ApiId       string `json:"api_id" xorm:"not null default '' comment('apiid') VARCHAR(32)"`
	BaseRow     `xorm:"extends"`
}
