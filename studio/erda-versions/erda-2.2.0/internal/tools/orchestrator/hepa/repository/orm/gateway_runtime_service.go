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

type GatewayRuntimeService struct {
	ProjectId        string `json:"project_id" xorm:"not null default '' comment('所属项目') index(idx_config_tenant) VARCHAR(32)"`
	Workspace        string `json:"workspace" xorm:"not null default '' comment('所属环境') index(idx_config_tenant) VARCHAR(32)"`
	ClusterName      string `json:"cluster_name" xorm:"not null default '' comment('所属集群') index(idx_config_tenant) VARCHAR(32)"`
	RuntimeId        string `json:"runtime_id" xorm:"not null default '' comment('所属runtime') index(idx_runtime) VARCHAR(32)"`
	RuntimeName      string `json:"runtime_name" xorm:"not null default '' comment('runtime名称') VARCHAR(128)"`
	ReleaseId        string `json:"release_id" xorm:"not null default '' comment('runtime名称') VARCHAR(128)"`
	GroupNamespace   string `json:"group_namespace" xorm:"not null default '' comment('runtime名称') VARCHAR(128)"`
	GroupName        string `json:"group_name" xorm:"not null default '' comment('runtime名称') VARCHAR(128)"`
	ProjectNamespace string `json:"project_namespace" xorm:"not null default '' comment('项目级 namespace') VARCHAR(128)"`
	AppId            string `json:"app_id" xorm:"not null default '' comment('所属应用') VARCHAR(32)"`
	AppName          string `json:"app_name" xorm:"not null default '' comment('应用名称') VARCHAR(128)"`
	ServiceName      string `json:"service_name" xorm:"not null default '' comment('服务名称') VARCHAR(128)"`
	ServicePort      int    `json:"service_port" xorm:"not null default 0 comment('服务监听端口') INT(11)"`
	InnerAddress     string `json:"inner_address" xorm:"comment('服务内部地址') VARCHAR(1024)"`
	UseApigw         int    `json:"use_apigw" xorm:"not null default 0 comment('是否使用api网关') TINYINT(1)"`
	IsEndpoint       int    `json:"is_endpoint" xorm:"not null default 0 comment('是否是endpoint') TINYINT(1)"`
	IsSecurity       int    `json:"is_security" xorm:"not null default 0 comment('是否需要安全加密') TINYINT(1)"`
	BackendProtocol  string `json:"backend_protocol" xorm:"not null default '' comment('后端协议') VARCHAR(16)"`
	BaseRow          `xorm:"extends"`
}
