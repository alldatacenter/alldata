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

package stages

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

type ComponentStageForm struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle
	CommonStageForm
}

type CommonStageForm struct {
	Version    string                 `json:"version,omitempty"`
	Name       string                 `json:"name,omitempty"`
	Type       string                 `json:"type,omitempty"`
	State      State                  `json:"state,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
	Data       Data                   `json:"data,omitempty"`
	InParams   InParams               `json:"inParams,omitempty"`
	Props      map[string]interface{} `json:"props,omitempty"`
}

type Data struct {
	List []StageData `json:"value"`
	Type string      `json:"type"`
}

type Tag struct {
	Label string `json:"label"`
	Color string `json:"color"`
}

type StageData struct {
	Title      string                 `json:"title"`
	ID         uint64                 `json:"id"`
	GroupID    int64                  `json:"groupId"`
	Disabled   bool                   `json:"disabled"`
	Operations map[string]interface{} `json:"operations"`
	Tags       []Tag                  `json:"tags"`
}

type InParams struct {
	SceneSetID uint64 `json:"sceneSetId__urlQuery"`
}

type DragParams struct {
	DragGroupKey int64 `json:"dragGroupKey"`
	DragKey      int64 `json:"dragKey"`
	DropGroupKey int64 `json:"dropGroupKey"`
	DropKey      int64 `json:"dropKey"`
	Position     int64 `json:"position"`
}
type State struct {
	AutotestSceneRequest apistructs.AutotestSceneRequest `json:"autotestSceneRequest"`
	DragParams           DragParams                      `json:"dragParams"`

	StepId                 uint64 `json:"stepId"`
	ShowApiEditorDrawer    bool   `json:"showApiEditorDrawer"`
	ShowConfigSheetDrawer  bool   `json:"showConfigSheetDrawer"`
	ShowWaitEditorDrawer   bool   `json:"showWaitEditorDrawer"`
	ShowNestedSceneDrawer  bool   `json:"showNestedSceneDrawer"`
	ShowCustomEditorDrawer bool   `json:"showCustomEditorDrawer"`

	IsClickItem bool `json:"isClickItem"`
}

type OperationBaseInfo struct {
	FillMeta  string `json:"fillMeta"`
	Key       string `json:"key"`
	Icon      string `json:"icon"`
	HoverTip  string `json:"hoverTip"`
	HoverShow bool   `json:"hoverShow"`
	Group     string `json:"group"`
	// 操作展示名称
	Text     string `json:"text"`
	CopyText string `json:"copyText"`
	// 确认提示
	Confirm string `json:"confirm,omitempty"`
	// 前端操作是否需要触发后端
	Reload      bool   `json:"reload"`
	Disabled    bool   `json:"disabled"`
	DisabledTip string `json:"disabledTip"`
	SuccessMsg  string `json:"successMsg"`
}

type OpMetaData struct {
	Type      apistructs.StepAPIType   `json:"type"`      // 类型
	Method    apistructs.StepAPIMethod `json:"method"`    // method
	Value     string                   `json:"value"`     // 值
	Name      string                   `json:"name"`      // 名称
	APISpecID uint64                   `json:"apiSpecID"` // api集市id
	ID        uint64                   `json:"id"`
	Disable   bool                     `json:"disable"`
}

type OpMetaInfo struct {
	apistructs.AutotestSceneRequest
	Data OpMetaData `json:"data"`
}

type OperationInfo struct {
	OperationBaseInfo
	Meta OpMetaInfo `json:"meta"`
}

type CreateOperation OperationInfo
type OnChangeOperation OperationInfo
