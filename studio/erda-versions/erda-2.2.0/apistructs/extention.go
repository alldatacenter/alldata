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

package apistructs

import (
	"strconv"
	"time"

	"github.com/Masterminds/semver"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/pkg/i18n"
)

const DicehubExtensionsMenu = "dicehub.extensions.menu"

type SpecType string

const SpecActionType SpecType = "action"
const SpecAddonType SpecType = "addon"

var (
	ExtensionSpecDisableECILabel = "eci_disable"
)

var CategoryTypes = map[string][]string{
	"action": {
		"source_code_management",
		"build_management",
		"deploy_management",
		"version_management",
		"test_management",
		"data_management",
		"custom_task",
	},
	"addon": {
		"database",
		"distributed_cooperation",
		"search",
		"message",
		"content_management",
		"security",
		"traffic_load",
		"monitoring&logging",
		"content",
		"image_processing",
		"document_processing",
		"sound_processing",
		"custom",
		"general_ability",
		"new_retail",
		"srm",
		"solution",
	},
}

// Spec spec.yml 格式
type Spec struct {
	Name              string            `json:"name" yaml:"name"`
	DisplayName       string            `json:"displayName" yaml:"displayName"`
	Version           string            `json:"version" yaml:"version"`
	Type              string            `json:"type" yaml:"type"`
	Category          string            `json:"category" yaml:"category"`
	Desc              string            `json:"desc" yaml:"desc"`
	Labels            map[string]string `json:"labels" yaml:"labels"`
	LogoUrl           string            `json:"logoUrl" yaml:"logoUrl"`
	SupportedVersions []string          `json:"supportedErdaVersions" yaml:"supportedErdaVersions"`
	Public            bool              `json:"public" yaml:"public"`
	IsDefault         bool              `json:"isDefault" yaml:"isDefault"`

	Locale map[string]map[string]string `json:"locale" yaml:"locale"`
}

const specDisplayName = "displayName"
const specDesc = "desc"

func (spec *Spec) GetLocaleDisplayName(lang string) string {
	if lang == "" {
		lang = i18n.ZH
	}
	if spec.Locale == nil || spec.Locale[lang] == nil {
		return spec.DisplayName
	}
	if spec.Locale[lang][specDisplayName] == "" {
		return spec.DisplayName
	}
	return spec.Locale[lang][specDisplayName]
}

func (spec *Spec) GetLocaleDesc(lang string) string {
	if lang == "" {
		lang = i18n.ZH
	}
	if spec.Locale == nil || spec.Locale[lang] == nil {
		return spec.Desc
	}
	if spec.Locale[lang][specDesc] == "" {
		return spec.Desc
	}

	return spec.Locale[lang][specDesc]
}

func (spec *Spec) IsDisableECI() bool {
	if spec.Labels == nil {
		return false
	}
	if disableECIStr, ok := spec.Labels[ExtensionSpecDisableECILabel]; ok {
		disable, err := strconv.ParseBool(disableECIStr)
		if err != nil {
			return false
		}
		return disable
	}
	return false
}

// CheckDiceVersion 检查版本是否支持
func (spec *Spec) CheckDiceVersion(versionStr string) bool {
	// 遇到异常不做删除,防止误删
	if versionStr == "" {
		return true
	}
	semVersion, err := semver.NewVersion(versionStr)
	if err != nil {
		logrus.Error(err)
		return true
	}
	// The result is always false when comparing any version ends with pre-release tag with a constraint in golang implementation of semver.
	// For instance 1.3.0-alpha doesn't match the constraint ">= 1.2".
	// Here we use the Erda version to compare with the supported version of action to find compatible actions.
	// Actually the version without pre-release tag is what we want the supported version compare to.
	newSemVersion, err := semVersion.SetPrerelease("")
	if err != nil {
		logrus.Error(err)
		return true
	}
	if spec.SupportedVersions == nil || len(spec.SupportedVersions) == 0 {
		return true
	}
	for _, v := range spec.SupportedVersions {
		constraint, err := semver.NewConstraint(v)
		// 无效条件不处理
		if err != nil {
			continue
		}
		if !constraint.Check(&newSemVersion) {
			return false
		}
	}
	return true
}

// ExtensionCreateRequest 创建Extension
type ExtensionCreateRequest struct {
	// 类型 addon|action
	Type        string `json:"type"`
	Name        string `json:"name"`
	DisplayName string `json:"displayName"`
	Desc        string `json:"desc"`
	Category    string `json:"category"`
	LogoUrl     string `json:"logoUrl"`
	Public      bool   `json:"public"`
	Labels      string `json:"labels"`
}

// ExtensionCreateResponse 创建扩展返回数据
type ExtensionCreateResponse struct {
	Header
	Data Extension `json:"data"`
}

// ExtensionVersionCreateRequest 创建Extension版本
type ExtensionVersionCreateRequest struct {
	Name       string `json:"name"`
	Version    string `json:"version"`
	SpecYml    string `json:"specYml"`
	DiceYml    string `json:"diceYml"`
	SwaggerYml string `json:"swaggerYml"`
	Readme     string `json:"readme"`
	//是否公开
	Public bool `json:"public"`
	// 为true的情况如果已经存在相同版本会覆盖更新,不会报错
	ForceUpdate bool `json:"forceUpdate"`
	//是否一起更新ext和version,默认只更新version,只在forceUpdate=true有效
	All       bool `json:"all"`
	IsDefault bool `json:"isDefault"`
}

// ExtensionVersionCreateResponse 创建扩展版本返回数据
type ExtensionVersionCreateResponse struct {
	Header
	Data ExtensionVersion `json:"data"`
}

// ExtensionQueryRequest 查询extension请求
type ExtensionQueryRequest struct {
	//默认false查询公开的扩展, true查询所有扩展
	All bool `query:"all"`
	// 可选值: action、addon
	Type string `query:"type"`
	// 根据标签查询 key:value 查询满足条件的 ^key:value 查询不满足条件的
	Labels string `query:"labels"`
}

// ExtensionQueryResponse 查询extension响应
type ExtensionQueryResponse struct {
	Header
	Data []Extension `json:"data"`
}

// ExtensionVersionGetRequest 获取指定extension版本信息
type ExtensionVersionGetRequest struct {
	Name       string `path:"name"`
	Version    string `path:"version"`
	YamlFormat bool   `json:"yamlFormat"`
}

// ExtensionVersionQueryRequest 查询extension版本
type ExtensionVersionQueryRequest struct {
	Name       string
	YamlFormat bool `query:"yamlFormat"`
	//默认false查询有效版本, true查询所有版本
	All                bool `query:"all"`
	OrderByVersionDesc bool `query:"orderByVersionDesc"`
}

// ExtensionVersionGetResponse Extension详情API返回数据结构
type ExtensionVersionGetResponse struct {
	Header
	Data ExtensionVersion `json:"data"`
}

// ExtensionVersionQueryResponse 查询ExtensionVersion列表返回数据
type ExtensionVersionQueryResponse struct {
	Header
	Data []ExtensionVersion `json:"data"`
}

// ExtensionSearchRequest 批量查询extension请求
type ExtensionSearchRequest struct {
	YamlFormat bool `json:"yamlFormat"`
	// 支持格式 name:获取默认版本 name@version:获取指定版本
	Extensions []string `json:"extensions"`
}

// ExtensionSearchResponse 批量查询extension响应
type ExtensionSearchResponse struct {
	Header
	Data map[string]ExtensionVersion `json:"data"`
}

type ExtensionMenu struct {
	Name        string       `json:"name"`
	DisplayName string       `json:"displayName"`
	Items       []*Extension `json:"items"`
}

// Extension
type Extension struct {
	ID          uint64    `json:"id"`
	Type        string    `json:"type"`
	Name        string    `json:"name"`
	Desc        string    `json:"desc"`
	DisplayName string    `json:"displayName"`
	Category    string    `json:"category"`
	LogoUrl     string    `json:"logoUrl"`
	Public      bool      `json:"public"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

// ExtensionVersion
type ExtensionVersion struct {
	Name      string      `json:"name"`
	Version   string      `json:"version"`
	Type      string      `json:"type"`
	Spec      interface{} `json:"spec"`
	Dice      interface{} `json:"dice"`
	Swagger   interface{} `json:"swagger"`
	Readme    string      `json:"readme"`
	CreatedAt time.Time   `json:"createdAt"`
	UpdatedAt time.Time   `json:"updatedAt"`
	IsDefault bool        `json:"isDefault"`
	Public    bool        `json:"public"`
}

func (v *ExtensionVersion) NotExist() bool {
	return v.Name == ""
}

type ActionSpec struct {
	Spec              `yaml:",inline"`
	Concurrency       *ActionConcurrency  `json:"concurrency" yaml:"concurrency"`
	Params            []ActionSpecParam   `json:"params" yaml:"params"`
	FormProps         []FormPropItem      `json:"formProps" yaml:"formProps"`
	AccessibleAPIs    []AccessibleAPI     `json:"accessibleAPIs" yaml:"accessibleAPIs"`
	Outputs           []ActionSpecOutput  `json:"outputs" yaml:"outputs"`
	OutputsFromParams []OutputsFromParams `json:"outputsFromParams" yaml:"outputsFromParams"`
	Loop              *PipelineTaskLoop   `json:"loop" yaml:"loop"`
	Executor          *ActionExecutor     `json:"executor" yaml:"executor"`
}

type ActionExecutor struct {
	Kind string `json:"kind" yaml:"kind"`
	Name string `json:"name" yaml:"name"`
}

type ActionMatchOutputType string

const JqActionMatchOutputType = "jq"

type OutputsFromParams struct {
	Type       ActionMatchOutputType `json:"type" yaml:"type"`
	Expression string                `json:"keyExpr" yaml:"keyExpr"`
}

type LoopStrategy struct {
	MaxTimes        int64   `json:"max_times,omitempty" yaml:"max_times,omitempty"`                 // 最大重试次数，-1 表示不限制
	DeclineRatio    float64 `json:"decline_ratio,omitempty" yaml:"decline_ratio,omitempty"`         // 重试衰退速率  2s - 4s - 8s - 16s
	DeclineLimitSec int64   `json:"decline_limit_sec,omitempty" yaml:"decline_limit_sec,omitempty"` // 重试衰退最大值  2s - 4s - 8s - 8s - 8s
	IntervalSec     uint64  `json:"interval_sec,omitempty" yaml:"interval_sec,omitempty"`           // 重试间隔时间 2s - 2s - 2s - 2s
}

type ActionConcurrency struct {
	Enable bool                 `json:"enable" yaml:"enable"`
	V1     *ActionConcurrencyV1 `json:"v1" yaml:"v1"`
	// 之后可以有多个版本
	// Use: v1/v2/v3
	// V2 *ActionConcurrencyV2 `json:"v2" yaml:"v2"`
	// V3 *ActionConcurrencyV3 `json:"v3" yaml:"v3"`
}

type ActionConcurrencyV1 struct {
	Default  ActionConcurrencyV1Item            `json:"default" yaml:"default"`
	Clusters map[string]ActionConcurrencyV1Item `json:"clusters" yaml:"clusters"`
}

type ActionConcurrencyV1Item struct {
	Max int `json:"max" yaml:"max"`
}

type ActionSpecParam struct {
	Name     string            `json:"name" yaml:"name"`
	Required bool              `json:"required" yaml:"required"`
	Default  interface{}       `json:"default" yaml:"default"`
	Desc     string            `json:"desc" yaml:"desc"`
	Type     string            `json:"type" yaml:"type"`
	Struct   []ActionSpecParam `json:"struct" yaml:"struct"`
}

type ActionSpecOutput struct {
	Name string `json:"name" yaml:"name"`
	Desc string `json:"desc" yaml:"desc"`
}

type FormPropItem struct {
	Label          string      `json:"label,omitempty" yaml:"label,omitempty"`
	Component      string      `json:"component,omitempty" yaml:"component,omitempty"`
	Required       bool        `json:"required,omitempty" yaml:"required,omitempty"`
	Key            string      `json:"key,omitempty" yaml:"key,omitempty"`
	ComponentProps interface{} `json:"componentProps,omitempty" yaml:"componentProps,omitempty"`
	Group          string      `json:"group,omitempty" yaml:"group,omitempty"`
	DefaultValue   interface{} `json:"defaultValue,omitempty" yaml:"defaultValue,omitempty"`
	LabelTip       string      `json:"labelTip,omitempty" yaml:"labelTip,omitempty"`
}
