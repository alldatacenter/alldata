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

type SceneSetOperation struct {
	Key        string                `json:"key"`
	Text       string                `json:"text"`
	Reload     bool                  `json:"reload"`
	Show       bool                  `json:"show"`
	SuccessMsg string                `json:"successMsg"`
	Meta       SceneSetOperationMeta `json:"meta"`
}

type SceneSetOperationMeta struct {
	ParentKey int `json:"parentKey,omitempty"`
	Key       int `json:"key,omitempty"`
}

// type ClickSceneSetOperation struct {
// 	BaseOperation
// 	Meta SceneSetOperationMeta `json:"meta"`
// }

type DeleteOperation struct {
	Key     string                `json:"key"`
	Text    string                `json:"text"`
	Confirm string                `json:"confirm"`
	Reload  bool                  `json:"reload"`
	Show    bool                  `json:"show"`
	Meta    SceneSetOperationMeta `json:"meta"`
}

type DeleteOperationData struct {
	Key string `json:"key"`
}
