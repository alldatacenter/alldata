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

package tree_select

type INode struct {
	Key        string `json:"key"`
	Id         string `json:"id"`
	PId        string `json:"pId"`
	Title      string `json:"title"`
	IsLeaf     bool   `json:"isLeaf"`
	Value      string `json:"value"`
	Selectable bool   `json:"selectable"`
	Disabled   bool   `json:"disabled"`
}

type Data struct {
	TreeData []INode `json:"treeData"`
}

type Props struct {
	Visible     bool   `json:"visible"`
	Placeholder string `json:"placeholder"`
	Title       string `json:"title"`
}
