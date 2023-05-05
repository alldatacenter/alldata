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

package sort_group

type Props struct {
	Draggable      bool `json:"draggable"`
	GroupDraggable bool `json:"groupDraggable"`
}

type Data struct {
	Type  string `json:"type"`
	Value []Item `json:"value"`
}

type Item struct {
	Id         int                    `json:"id"`
	GroupId    int                    `json:"groupId"`
	Title      string                 `json:"title"`
	Operations map[string]interface{} `json:"operations"`
}
