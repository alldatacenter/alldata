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

package notifyConfigTable

import (
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/components/table"
)

var (
	roleMap = map[string]string{
		"Owner": "应用所有者",
		"Lead":  "应用主管",
		"Ops":   "运维",
		"Dev":   "开发工程师",
		"QA":    "测试工程师",
	}
)

type Notify struct {
	CtxBdl protocol.ContextBundle
	table.CommonTable
}

type DeleteNotifyOperation struct {
	Meta DeleteOperationData `json:"meta"`
}

type DeleteOperationData struct {
	Id uint64 `json:"id"`
}

type EditOperation struct {
	Key    string `json:"key"`
	Text   string `json:"text"`
	Reload bool   `json:"reload"`
	Meta   Meta   `json:"meta"`
}

type Meta struct {
	Id uint64 `json:"id"`
}

type InParams struct {
	ScopeType string `json:"scopeType"`
	ScopeId   string `json:"scopeId"`
}

func (n *Notify) genProps() {
	props := table.Props{
		RowKey: "id",
		Columns: []table.PropColumn{
			{
				Title:     "通知名称",
				DataIndex: "name",
			},
			{
				Title:     "通知对象",
				DataIndex: "targets",
				Width:     200,
			},
			{
				Title:     "创建时间",
				DataIndex: "createdAt",
			},
			{
				Title:     "操作",
				DataIndex: "operate",
				Width:     150,
			},
		},
	}
	n.Props = props
}
