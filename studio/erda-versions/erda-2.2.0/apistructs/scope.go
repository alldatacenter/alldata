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

type ScopeType string
type PublisherType string

const (
	// SysScope 系统范围
	SysScope ScopeType = "sys"
	// OrgScope 企业范围
	OrgScope ScopeType = "org"
	// ProjectScope 项目范围
	ProjectScope ScopeType = "project"
	// AppScope 应用范围
	AppScope ScopeType = "app"
	// AppScope Publisher范围
	PublisherScope ScopeType = "publisher"
)

// 答疑用户的固定 ID
const SupportID string = "2020"

//MSP
const (
	MSPScope         string = "msp_env"
	MSPMemberScope   string = "member_scopeType"
	MSPMemberScopeId string = "member_scopeID"
)

// Publisher 类型
const (
	// 移动应用
	MobilePublisher PublisherType = "mobile"
)

// 最大scope数量限制
const (
	// MaxOrgNum 最大企业数量限制
	MaxOrgNum uint64 = 5
	// MaxProjectNum 最大项目数量限制
	MaxProjectNum uint64 = 5
	// MaxAppNum 最大应用数量限制
	MaxAppNum uint64 = 5
)

// Scope 范围 (作用域)
type Scope struct {
	// 范围类型
	// 可选值: sys, org, project, app
	Type ScopeType `json:"type"`

	// 范围对应的实例 ID (orgID, projectID, applicationID ...)
	// 比如 type == "org" 时, id 即为 orgID
	ID string `json:"id,omitempty"`
}
