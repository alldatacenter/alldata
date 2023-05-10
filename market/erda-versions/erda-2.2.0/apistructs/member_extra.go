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

type MemeberLabelName string

const (
	// LabelNameOutsource 外包人员
	LabelNameOutsource MemeberLabelName = "Outsource"
	// LabelNamePartner 合作伙伴
	LabelNamePartner MemeberLabelName = "Partner"
)

// ResourceKey member关联字段的key
type ExtraResourceKey string

const (
	// LabelResourceKey 成员的标签
	LabelResourceKey ExtraResourceKey = "label"
	// RoleResourceKey 成员的角色
	RoleResourceKey ExtraResourceKey = "role"
)

func (rk ExtraResourceKey) String() string {
	return string(rk)
}

// MemberLabelListResponse 查询成员标签列表 GET /api/members/actions/list-labels
type MemberLabelListResponse struct {
	Header
	Data MemberLabelList `json:"data"`
}

// MemberLabelList 成员标签列表
type MemberLabelList struct {
	// 角色标签
	List []MemberLabelInfo `json:"list"`
}

// MemberLabelInfo 成员标签
type MemberLabelInfo struct {
	Label MemeberLabelName `json:"label"`
	Name  string           `json:"name"`
}
