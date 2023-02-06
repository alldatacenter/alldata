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

package file_tree

type Spec struct {
	Type  string `json:"type"`
	Props Props  `json:"props"`
}

type Props struct {
	Searchable bool `json:"searchable"`
	Draggable  bool `json:"draggable"`
}

type Data struct {
	TreeData []INode `json:"treeData"`
}

type INode struct {
	Key           string                 `json:"key"`
	Title         string                 `json:"title"`
	Icon          string                 `json:"icon"`
	IsColorIcon   bool                   `json:"isColorIcon"`
	Children      []INode                `json:"children"`
	Selectable    bool                   `json:"selectable"`
	ClickToExpand bool                   `json:"clickToExpand"`
	IsLeaf        bool                   `json:"isLeaf"`
	Operations    map[string]interface{} `json:"operations"`
	Type          string                 `json:"type"`
}

type Field struct {
	Label    string      `json:"label"`
	ValueKey interface{} `json:"valueKey"`
}
