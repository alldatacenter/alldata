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

package inParamsForm

import (
	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
)

type ComponentInParamsForm struct {
	sdk *cptype.SDK
	bdl *bundle.Bundle

	CommonInParamsForm
}

type CommonInParamsForm struct {
	Version    string                 `json:"version,omitempty"`
	Name       string                 `json:"name,omitempty"`
	Type       string                 `json:"type,omitempty"`
	Props      map[string]interface{} `json:"props,omitempty"`
	State      State                  `json:"state,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
	Data       Data                   `json:"data,omitempty"`
	InParams   InParams               `json:"inParams,omitempty"`
}

type Data struct {
	List []ParamData `json:"list"`
}

type ParamData struct {
	ParamsName   string                 `json:"name"`
	Desc         string                 `json:"description"`
	DefaultValue string                 `json:"value"`
	Value        string                 `json:"temp"`
	ID           uint64                 `json:"id"`
	Operations   map[string]interface{} `json:"operations"`
}

type InParams struct {
	SceneSetID uint64 `json:"sceneSetId__urlQuery"`
	SpaceId    uint64 `json:"spaceId"`
}

type PropColumn struct {
	Title    string     `json:"title"`
	Key      PropsKey   `json:"key"`
	Width    int64      `json:"width"`
	Flex     int64      `json:"flex"`
	Render   PropRender `json:"render"`
	TitleTip string     `json:"titleTip,omitempty"`
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
	MaxLength   int64              `json:"maxLength,omitempty"`
	Placeholder string             `json:"placeholder,omitempty"`
	Options     []PropChangeOption `json:"options,omitempty"`
}

type PropRenderRule struct {
	Pattern string `json:"pattern,omitempty"`
	Msg     string `json:"msg,omitempty"`
}

type PropChangeOption struct {
	Label    string             `json:"label"`
	Value    string             `json:"value"`
	IsLeaf   bool               `json:"isLeaf"`
	ToolTip  string             `json:"tooltip"`
	Children []PropChangeOption `json:"children"`
}

func (pct *PropChangeOption) FindValue(v string) *PropChangeOption {
	if pct.Value == v {
		return pct
	}
	for i := range pct.Children {
		k := pct.Children[i].FindValue(v)
		if k != nil {
			return k
		}
	}
	return nil
}

type PropsKey string

const (
	PropsKeyParamsName   PropsKey = "name"
	PropsKeyDesc         PropsKey = "description"
	PropsKeyDefaultValue PropsKey = "value"
	PropsKeyValue        PropsKey = "temp"
)

type OptionValue string

func (ov OptionValue) String() string {
	return string(ov)
}

const (
	BeforeSceneOutPutOptionValue OptionValue = "beforeSceneOutPut" // 前置场景出参
	SceneOptionValue             OptionValue = "scene"             // 场景 X
	refSceneSetOptionValue       OptionValue = "refSceneSet"       // 引用场景集
	SceneOutPutOptionValue       OptionValue = "sceneOutPut"       // 场景出参 X
	MockOptionValue              OptionValue = "mock"              // MOCK
	MockValueOptionValue         OptionValue = "mockValue"         // MOCK值
	GlobalOptionValue            OptionValue = "global"            // 全局参数
	GlobalValueOptionValue       OptionValue = "globalValue"       // 全局参数环境
	GlobalGlobalOptionValue      OptionValue = "globalGlobal"      // 全局参数环境Global
)

type State struct {
	AutotestSceneRequest apistructs.AutotestSceneRequest `json:"autotestSceneRequest"`
	List                 []apistructs.AutoTestSceneInput `json:"list"`
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
	SelectOption []PropChangeOption `json:"selectOption"`
}

type OperationInfo struct {
	OperationBaseInfo
	Meta OpMetaInfo `json:"meta"`
}

type DeleteOperation OperationInfo
type OnSelectOperation OperationInfo
