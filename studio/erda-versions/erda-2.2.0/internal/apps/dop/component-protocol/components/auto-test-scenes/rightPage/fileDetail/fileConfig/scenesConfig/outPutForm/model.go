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

package outPutForm

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

type ComponentOutPutForm struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle

	CommonOutPutForm
}

type CommonOutPutForm struct {
	Version    string                                           `json:"version,omitempty"`
	Name       string                                           `json:"name,omitempty"`
	Type       string                                           `json:"type,omitempty"`
	Props      map[string]interface{}                           `json:"props,omitempty"`
	State      State                                            `json:"state,omitempty"`
	Operations map[apistructs.OperationKey]apistructs.Operation `json:"operations,omitempty"`
	Data       Data                                             `json:"data"`
	InParams   InParams                                         `json:"inParams,omitempty"`
}

type Data struct {
	List []ParamData `json:"list"`
}

type ParamData struct {
	Name        string                 `json:"name"`
	Description string                 `json:"description"`
	Value       string                 `json:"value"`
	ID          uint64                 `json:"id"`
	Operations  map[string]interface{} `json:"operations"`
}

type InParams struct {
	SceneSetID uint64 `json:"sceneSetId__urlQuery"`
}

type PropColumn struct {
	Title  string     `json:"title"`
	Key    PropsKey   `json:"key"`
	Width  int64      `json:"width"`
	Flex   int64      `json:"flex"`
	Render PropRender `json:"render"`
}

type PropRender struct {
	Type             string                 `json:"type,omitempty"`
	ValueConvertType string                 `json:"valueConvertType,omitempty"`
	Options          []PropChangeOption     `json:"options,omitempty"`
	Required         bool                   `json:"required,omitempty"`
	UniqueValue      bool                   `json:"uniqueValue,omitempty"`
	Operations       map[string]interface{} `json:"operations,omitempty"`
	Rules            []PropRenderRule       `json:"rules,omitempty"`
	Props            PropRenderProp         `json:"props,omitempty"`
}

type PropRenderProp struct {
	MaxLength int64              `json:"maxLength,omitempty"`
	Options   []PropChangeOption `json:"options,omitempty"`
}

type PropRenderRule struct {
	Pattern string `json:"pattern,omitempty"`
	Msg     string `json:"msg,omitempty"`
}

type PropChangeOption struct {
	Label string `json:"label"`
	Value string `json:"value"`
}

type PropsKey string

const (
	PropsKeyParamsName PropsKey = "name"
	PropsKeyDesc       PropsKey = "description"
	PropsKeyValue      PropsKey = "value"
)

type State struct {
	AutotestSceneRequest apistructs.AutotestSceneRequest  `json:"autotestSceneRequest"`
	List                 []apistructs.AutoTestSceneOutput `json:"list"`
}

type OperationBaseInfo struct {
	Key string `json:"key"`
	// 操作展示名称
	Text string `json:"text,omitempty"`
	// 确认提示
	Confirm string `json:"confirm,omitempty"`
	// 前端操作是否需要触发后端
	Reload      bool   `json:"reload"`
	Disabled    bool   `json:"disabled"`
	DisabledTip string `json:"disabledTip"`

	FillMeta string `json:"fillMeta"`
}

type OpMetaInfo struct {
	apistructs.AutotestSceneRequest
	SelectOption []PropChangeOption `json:"selectOption,omitempty"`
}

type OperationInfo struct {
	OperationBaseInfo
	Meta OpMetaInfo `json:"meta"`
}

type DeleteOperation OperationInfo
type OnChangeOperation OperationInfo
