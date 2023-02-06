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

type GatewayTransConfig struct {
	EnvType       string `json:"env_type" xorm:"VARCHAR(32)"`
	RuntimeId     string `json:"runtime_id" xorm:"VARCHAR(64)"`
	ClusterName   string `json:"cluster_name" xorm:"VARCHAR(32)"`
	TargetKey     string `json:"target_key" xorm:"VARCHAR(512)"`
	TargetType    string `json:"target_type" xorm:"VARCHAR(64)"`
	DubboTarget   string `json:"dubbo_target" xorm:"default '' VARCHAR(4096)"`
	OperationType string `json:"operation_type" xorm:"VARCHAR(64)"`
	ZkUrl         string `json:"zk_url" xorm:"VARCHAR(256)"`
	BaseRow       `xorm:"extends"`
}
