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
	AT_K8S     = "k8s"
	AT_DCOS    = "dcos"
	AT_EDAS    = "edas"
	AT_UNKNOWN = "unknown"
)

type GatewayAzInfo struct {
	Az             string `json:"az" xorm:"not null comment('集群名') VARCHAR(32)"`
	Env            string `json:"env" xorm:"not null comment('应用所属环境') VARCHAR(32)"`
	NeedUpdate     int    `json:"need_update" xorm:"default 1 comment('待更新标识') TINYINT(1)"`
	OrgId          string `json:"org_id" xorm:"not null comment('企业标识id') VARCHAR(32)"`
	ProjectId      string `json:"project_id" xorm:"not null comment('项目标识id') VARCHAR(32)"`
	Type           string `json:"type" xorm:"not null comment('集群类型') VARCHAR(16)"`
	WildcardDomain string `json:"wildcard_domain" xorm:"not null comment('集群泛域名') default '' VARCHAR(1024)"`
	MasterAddr     string `josn:"master_addr" xorm:"not null comment('集群管控地址') default '' VARCHAR(1024)"`
	BaseRow        `xorm:"extends"`
}
