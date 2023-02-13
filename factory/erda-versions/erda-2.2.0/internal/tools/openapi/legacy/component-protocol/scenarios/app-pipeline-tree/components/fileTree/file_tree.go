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

package fileTree

import (
	"fmt"

	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

type ComponentFileTree struct {
	CtxBdl   protocol.ContextBundle
	Type     string                 `json:"type"`
	Props    map[string]interface{} `json:"props"`
	State    State                  `json:"state"`
	Data     []Data                 `json:"data"`
	Disabled bool
}

func (a *ComponentFileTree) SetBundle(b protocol.ContextBundle) error {
	if b.Bdl == nil {
		err := fmt.Errorf("invalie bundle")
		return err
	}
	a.CtxBdl = b
	return nil
}

type State struct {
	ExpandedKeys         []string             `json:"expandedKeys"`
	SelectedKeys         []string             `json:"selectedKeys"`
	NodeFormModalAddNode NodeFormModalAddNode `json:"nodeFormModalAddNode"`
}

type Data struct {
	Key           string                 `json:"key"`
	Title         string                 `json:"title"`
	Icon          string                 `json:"icon"`
	IsLeaf        bool                   `json:"isLeaf"`
	ClickToExpand bool                   `json:"clickToExpand"` // always true, 该参数表示前端支持点击目录名进行折叠或展开; false 的话只有点击前面的小三角才能折叠或展开
	Selectable    bool                   `json:"selectable"`
	Operations    map[string]interface{} `json:"operations"`
	Children      []Data                 `json:"children"`
}

type ComponentNodeFormModal struct {
	Type       string                 `json:"type"`
	Props      map[string]interface{} `json:"props"`
	State      map[string]interface{} `json:"state"`
	Operations map[string]interface{} `json:"operations"`
}

type InParams struct {
	AppId        string `json:"appId"`
	ProjectId    string `json:"projectId"`
	SelectedKeys string `json:"selectedKeys"`
}
