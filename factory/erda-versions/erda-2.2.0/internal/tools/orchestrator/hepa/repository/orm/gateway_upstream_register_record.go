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

type GatewayUpstreamRegisterRecord struct {
	UpstreamId   string `json:"upstream_id" xorm:"not null comment('应用标识id') VARCHAR(32)"`
	RegisterId   string `json:"register_id" xorm:"not null comment('应用注册id') VARCHAR(64)"`
	UpstreamApis []byte `json:"upstream_apis" xorm:"comment('api注册列表') BLOB"`
	BaseRow      `xorm:"extends"`
}
