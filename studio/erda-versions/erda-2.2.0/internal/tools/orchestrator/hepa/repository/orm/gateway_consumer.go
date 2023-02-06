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
	APIM_CLIENT_CONSUMER string = "apim_client"
)

type GatewayConsumer struct {
	ClientId      string `json:"client_id" xorm:"not null default '' comment('对应的客户端id') unique VARCHAR(32)"`
	ConsumerId    string `json:"consumer_id" xorm:"not null default '' comment('消费者id') unique VARCHAR(128)"`
	ConsumerName  string `json:"consumer_name" xorm:"not null default '' comment('消费者名称') VARCHAR(128)"`
	Config        string `json:"config" xorm:"comment('配置信息，存放key等') VARCHAR(1024)"`
	Endpoint      string `json:"endpoint" xorm:"not null default '' comment('终端') VARCHAR(256)"`
	OrgId         string `json:"org_id" xorm:"not null VARCHAR(32)"`
	ProjectId     string `json:"project_id" xorm:"not null VARCHAR(32)"`
	Env           string `json:"env" xorm:"not null VARCHAR(32)"`
	Az            string `json:"az" xorm:"not null VARCHAR(32)"`
	Description   string `json:"description" xorm:"comment('备注') VARCHAR(256)"`
	Type          string `json:"type" xorm:"not null default 'project' comment('调用方类型') VARCHAR(16)"`
	CloudapiAppId string `json:"cloudapi_app_id" xorm:"not null default '' comment('阿里云API网关的app id') VARCHAR(128)"`
	BaseRow       `xorm:"extends"`
}
