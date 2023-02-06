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
	"fmt"
)

// 创建字段请求
type IssuePropertyCreateRequest struct {
	ScopeID           int64             `json:"scopeID"`           // 系统管理员(sys)/企业(org)/项目(project)/应用(app)
	ScopeType         ScopeType         `json:"scopeType"`         // 企业ID/项目ID/应用ID
	OrgID             int64             `json:"orgID"`             // 企业ID
	PropertyName      string            `json:"propertyName"`      // 属性名称
	DisplayName       string            `json:"displayName"`       // 属性的展示名称
	PropertyType      PropertyType      `json:"propertyType"`      // 属性类型
	Required          bool              `json:"required"`          // 是否必填
	PropertyIssueType PropertyIssueType `json:"propertyIssueType"` // 任务类型
	EnumeratedValues  []Enumerate       `json:"enumeratedValues"`  // 枚举值
	Relation          int64             `json:"relation"`          // 关联的公用字段ID  公有字段则该值为0
	IdentityInfo
}

// 更新字段请求
type IssuePropertyUpdateRequest struct {
	Header
	IssuePropertyIndex
	EnumeratedValues []Enumerate `json:"enumeratedValues"` // 枚举值
	IdentityInfo
}

type IssuePropertyIndexUpdateRequest struct {
	OrgID int64                `json:"orgID"`
	Data  []IssuePropertyIndex `json:"data"`
	IdentityInfo
}
type IssuePropertyTimeGetRequest struct {
	OrgID int64 `json:"orgID"`
}

// 删除字段请求
type IssuePropertyDeleteRequest struct {
	OrgID      int64 `json:"orgID"`
	PropertyID int64 `json:"propertyID"` // 字段ID
	IdentityInfo
}

// 查询企业下全部字段请求
type IssuePropertiesGetRequest struct {
	OrgID             int64             `json:"orgID"`             // 企业ID
	PropertyIssueType PropertyIssueType `json:"propertyIssueType"` // 任务类型
	PropertyName      string            `json:"propertyName"`
	IdentityInfo
}

// 字段枚举值
type Enumerate struct {
	ID    int64  `json:"id"`
	Name  string `json:"name"`
	Index int64  `json:"index"`
}

// 字段类型
type PropertyType string

const (
	PropertyTypeText        PropertyType = "Text"        // 文本
	PropertyTypeNumber      PropertyType = "Number"      // 数字
	PropertyTypeSelect      PropertyType = "Select"      // 单选
	PropertyTypeMultiSelect PropertyType = "MultiSelect" // 多选
	PropertyTypeDate        PropertyType = "Date"        // 日期
	PropertyTypePerson      PropertyType = "Person"
	PropertyTypeCheckBox    PropertyType = "CheckBox"
	PropertyTypeURL         PropertyType = "URL"
	PropertyTypeEmail       PropertyType = "Email"
	PropertyTypePhone       PropertyType = "Phone"
)

func (pt PropertyType) IsOptions() bool {
	if pt == PropertyTypeSelect || pt == PropertyTypeMultiSelect || pt == PropertyTypeCheckBox {
		return true
	}
	return false
}

func (pt PropertyType) IsNumber() bool {
	if pt == PropertyTypeNumber {
		return true
	}
	return false
}

// 字段类型转换的校验
func (pt PropertyType) IsCanChange(newpt PropertyType) bool {
	if pt != newpt {
		// 如果都不是选择型，允许转换
		if pt.IsOptions() == false && newpt.IsOptions() == false {
			return true
		}
		// 如果单选变多选，允许转换
		if pt == PropertyTypeSelect && (newpt == PropertyTypeMultiSelect || newpt == PropertyTypeCheckBox) {
			return true
		}
		// 多选互相转换,允许转换
		if (pt == PropertyTypeMultiSelect || pt == PropertyTypeCheckBox) && (newpt == PropertyTypeMultiSelect || newpt == PropertyTypeCheckBox) {
			return false
		}
	}
	return true
}

// 字段属性详情（包括排序级和枚举值）
type IssuePropertyIndex struct {
	PropertyID        int64             `json:"propertyID"`        // 字段ID
	ScopeID           int64             `json:"scopeID"`           // 系统管理员(sys)/企业(org)/项目(project)/应用(app)
	ScopeType         ScopeType         `json:"scopeType"`         // 企业ID/项目ID/应用ID
	OrgID             int64             `json:"orgID"`             // 企业ID
	PropertyName      string            `json:"propertyName"`      // 属性名称
	DisplayName       string            `json:"displayName"`       // 属性的展示名称
	PropertyType      PropertyType      `json:"propertyType"`      // 属性类型
	Required          bool              `json:"required"`          // 是否必填
	PropertyIssueType PropertyIssueType `json:"propertyIssueType"` // 任务类型
	Relation          int64             `json:"relation"`          // 关联的公用字段ID  公有字段则该值为0
	Index             int64             `json:"index"`             // 排序级
	EnumeratedValues  []Enumerate       `json:"enumeratedValues"`  // 枚举值
	Values            []int64           `json:"values"`            // 默认值
	RelatedIssue      []string          `json:"relatedIssue"`      // 使用该字段的模版任务类型
}
type PropertyIssueType string

const (
	PropertyIssueTypeRequirement PropertyIssueType = "REQUIREMENT" // 需求
	PropertyIssueTypeTask        PropertyIssueType = "TASK"        // 任务
	PropertyIssueTypeBug         PropertyIssueType = "BUG"         // 缺陷
	PropertyIssueTypeEpic        PropertyIssueType = "EPIC"        // 史诗
	PropertyIssueTypeCommon      PropertyIssueType = "COMMON"      // 公用
)

func (t PropertyIssueType) GetZhName() string {
	switch t {
	case PropertyIssueTypeRequirement:
		return "需求"
	case PropertyIssueTypeTask:
		return "任务"
	case PropertyIssueTypeBug:
		return "缺陷"
	case PropertyIssueTypeEpic:
		return "史诗"
	case PropertyIssueTypeCommon:
		return "公用"
	default:
		panic(fmt.Sprintf("invalid issue type: %s", string(t)))
	}
}

type IssuePropertyUpdateTimes struct {
	Task        string `json:"task"`
	Bug         string `json:"bug"`
	Epic        string `json:"epic"`
	Requirement string `json:"requirement"`
}
type IssuePropertyResponse struct {
	Header
	Data IssuePropertyIndex `json:"data"`
}
type IssuePropertiesResponse struct {
	Header
	Data []IssuePropertyIndex `json:"data"`
}

type IssuePropertyUpdateTimesResponse struct {
	Header
	Data IssuePropertyUpdateTimes `json:"data"`
}
