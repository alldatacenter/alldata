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

const (
	DT_SERVICE_DEFAULT = "service_default"
	DT_SERVICE_CUSTOM  = "service_custom"
	DT_PACKAGE         = "package"
	DT_COMPONENT       = "component"
)

type GatewayDomain struct {
	Domain           string `json:"domain" xorm:"not null comment('域名') index(idx_cluster_domain) VARCHAR(255)"`
	ClusterName      string `json:"cluster_name" xorm:"not null default '' comment('所属集群') index(idx_cluster_domain) VARCHAR(32)"`
	Type             string `json:"type" xorm:"not null comment('域名类型') VARCHAR(32)"`
	RuntimeServiceId string `json:"runtime_service_id" xorm:"comment('所属服务id') index(idx_runtime_service) VARCHAR(32)"`
	PackageId        string `json:"package_id" xorm:"comment('所属流量入口id') index(idx_package) VARCHAR(32)"`
	ComponentName    string `json:"component_name" xorm:"comment('所属平台组件的名称') VARCHAR(32)"`
	IngressName      string `json:"ingress_name" xorm:"comment('所属平台组件的ingress的名称') VARCHAR(32)"`
	OrgId            string `json:"org_id" xorm:"not null comment('组织标识id') VARCHAR(32)"`
	ProjectId        string `json:"project_id" xorm:"not null comment('项目标识id') VARCHAR(32)"`
	ProjectName      string `json:"project_name" xorm:"not null comment('项目名称') VARCHAR(50)"`
	Workspace        string `json:"workspace" xorm:"not null comment('所属环境') VARCHAR(32)"`
	BaseRow          `xorm:"extends"`
}
