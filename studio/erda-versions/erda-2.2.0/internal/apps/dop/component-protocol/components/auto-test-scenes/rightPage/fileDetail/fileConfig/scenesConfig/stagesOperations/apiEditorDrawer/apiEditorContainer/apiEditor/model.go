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

package apiEditor

type State struct {
	// 小试一把
	AttemptTest            AttemptTestAll `json:"attemptTest"`
	ConfigManageNamespaces string         `json:"configManageNamespaces"`

	// 步骤信息
	StepId    uint64 `json:"stepId"`
	SceneId   uint64 `json:"sceneId"`
	IsFirstIn bool   `json:"isFirstIn"` // 是否从步骤中点击进入
}

type AttemptTestAll struct {
	Visible bool                   `json:"visible"`
	Status  string                 `json:"status"`
	Data    map[string]interface{} `json:"data"`
}

type Menu struct {
	Text       string                 `json:"text"`
	Key        string                 `json:"key"`
	Operations map[string]interface{} `json:"operations"`
}

type ClickOperation struct {
	Key    string      `json:"key"`
	Reload bool        `json:"reload"`
	Meta   interface{} `json:"meta"`
}

type Meta struct {
	Env       string `json:"env"`
	ScenesID  uint64 `json:"scenesID"`
	ConfigEnv string `json:"configEnv"`
}

type OpMetaInfo struct {
	Meta Meta `json:"meta"`
}
