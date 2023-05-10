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

import "github.com/erda-project/erda/apistructs"

type AddNodeOperation struct {
	Key      string                  `json:"key"`
	Text     string                  `json:"text"`
	Reload   bool                    `json:"reload"`
	Command  AddNodeOperationCommand `json:"command"`
	Disabled bool                    `json:"disabled"`
}

type AddNodeOperationCommand struct {
	Key    string                       `json:"key"`
	Target string                       `json:"target"`
	State  AddNodeOperationCommandState `json:"state"`
}

type AddNodeOperationCommandState struct {
	Visible  bool                                 `json:"visible"`
	FormData AddNodeOperationCommandStateFormData `json:"formData"`
	// NodeFormModal 传递的数据
	NodeFormModalAddNode NodeFormModalAddNode `json:"nodeFormModalAddNode"`
}

type ClickBranchNodeOperation struct {
	Key    string                       `json:"key"`
	Text   string                       `json:"text"`
	Reload bool                         `json:"reload"`
	Show   bool                         `json:"show"` // 是否在菜单里展示
	Meta   ClickBranchNodeOperationMeta `json:"meta"`
}

type ClickBranchNodeOperationMeta struct {
	ParentKey string `json:"parentKey,omitempty"`
}

type NodeFormModalAddNode struct {
	Results apistructs.UnifiedFileTreeNode `json:"nodeFormModalAddNode"`
	Branch  string                         `json:"branch"`
}

type AddNodeOperationCommandStateFormData struct {
	Branch    string `json:"branch"`
	Name      string `json:"name"`
	AddResult apistructs.UnifiedFileTreeNode
}

type DeleteOperation struct {
	Key         string              `json:"key"`
	Text        string              `json:"text"`
	Confirm     string              `json:"confirm"`
	Reload      bool                `json:"reload"`
	Disabled    bool                `json:"disabled"`
	DisabledTip string              `json:"disabledTip"`
	Meta        DeleteOperationData `json:"meta"`
}

type DeleteOperationData struct {
	Key string `json:"key"`
}

type AddDefaultOperations struct {
	Key      string                  `json:"key"`
	Text     string                  `json:"text"`
	Reload   bool                    `json:"reload"`
	Show     bool                    `json:"show"`
	Meta     AddDefaultOperationData `json:"meta"`
	Disabled bool                    `json:"disabled"`
}

type AddDefaultOperationData struct {
	Key string `json:"key"`
}
