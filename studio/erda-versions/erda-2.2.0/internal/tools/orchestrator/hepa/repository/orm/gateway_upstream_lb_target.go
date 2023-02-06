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

type GatewayUpstreamLbTarget struct {
	LbId         string `json:"lb_id" xorm:"not null comment('关联的lb id') VARCHAR(32)"`
	DeploymentId int    `json:"deployment_id" xorm:"not null comment('上线时的deployment_id') INT(11)"`
	KongTargetId string `json:"kong_target_id" xorm:"comment('kong的target_id') VARCHAR(128)"`
	Target       string `json:"target" xorm:"comment('目的地址') VARCHAR(64)"`
	Healthy      int    `json:"healthy" xorm:"not null default 1 comment('是否健康') TINYINT(1)"`
	Weight       int    `json:"weight" xorm:"not null default 100 comment('轮询权重') INT(11)"`
	BaseRow      `xorm:"extends"`
}
