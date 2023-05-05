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

type Ini struct {
	IniName  string `json:"ini_name" xorm:"not null default '' comment('配置信息名称') index VARCHAR(128)"`
	IniDesc  string `json:"ini_desc" xorm:"not null default '' comment('配置信息介绍') VARCHAR(256)"`
	IniValue string `json:"ini_value" xorm:"not null default '' comment('配置信息参数值') VARCHAR(1024)"`
	BaseRow  `xorm:"extends"`
}
