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

type GatewayMock struct {
	Az      string `json:"az" xorm:"VARCHAR(256)"`
	HeadKey string `json:"head_key" xorm:"not null pk VARCHAR(256)"`
	Body    string `json:"body" xorm:"TEXT"`
	Pathurl string `json:"pathurl" xorm:"not null pk VARCHAR(256)"`
	Method  string `json:"method" xorm:"VARCHAR(32)"`
	BaseRow `xorm:"extends"`
}

func (row *GatewayMock) GetPK() map[string]interface{} {
	res := row.BaseRow.GetPK()
	res["head_key"] = row.HeadKey
	return res
}
