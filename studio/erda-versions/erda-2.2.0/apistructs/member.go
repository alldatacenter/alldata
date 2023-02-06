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

import "github.com/pkg/errors"

// MemberListRequest 查询成员 GET /api/members
type MemberListRequest struct {
	// 类型 sys, org, project, app
	ScopeType ScopeType `query:"scopeType"`
	// 对应的 orgId, projectId, applicationId
	ScopeID int64 `query:"scopeId"`
	// 过滤角色
	Roles []string `query:"roles"`
	// 过滤标签
	Labels []string `query:"label"`
	// 查询参数
	Q        string `query:"q"`
	PageNo   int    `query:"pageNo"`
	PageSize int    `query:"pageSize"`
}

// MemberListResponse 查询成员 GET /api/members
type MemberListResponse struct {
	Header
	Data MemberList `json:"data"`
}

// MemberList 成员列表
type MemberList struct {
	List  []Member `json:"list"`
	Total int      `json:"total"`
}

// Member 成员信息
type Member struct {
	UserID string `json:"userId"`
	Email  string `json:"email"`
	Mobile string `json:"mobile"`
	Name   string `json:"name"`
	Nick   string `json:"nick"`
	Avatar string `json:"avatar"`
	// Deprecated: 当前用户的状态,兼容老数据
	Status string `json:"status"`
	// 成员的归属
	Scope Scope `json:"scope"`
	// 成员角色，多角色
	Roles []string `json:"roles"`
	// 成员标签，多标签
	Labels []string `json:"labels"`
	// 被移除标记, 延迟删除
	Removed bool `json:"removed"`
	// uc注销用户的标记，用于分页查询member时的返回
	Deleted bool   `json:"deleted"`
	Token   string `json:"token"`
}

func (m *Member) GetUserName() string {
	if m.Nick != "" {
		return m.Nick
	}
	if m.Name != "" {
		return m.Name
	}
	if m.Mobile != "" {
		return m.Mobile
	}
	return m.Email
}

// RoleInfo 角色信息
type RoleInfo struct {
	Role string `json:"role"`
	Name string `json:"name"`
}

// MemberRolesResponse 获取角色列表 GET /api/members/actions/list-roles
type MemberRoleListResponse struct {
	Header
	Data RoleList `json:"data"`
}

// RoleList 成员列表
type RoleList struct {
	List  []RoleInfo `json:"list"`
	Total int        `json:"total"`
}

// MemberUserInfoUpdateRequest 成员用户信息更新请求
type MemberUserInfoUpdateRequest struct {
	Members []Member `json:"members"`
}

// MemberUserInfoUpdateResponse 成员用户信息更新响应
type MemberUserInfoUpdateResponse struct {
	Header
}

// MemberAddRequest 添加成员 POST /api/members
type MemberAddRequest struct {
	// 成员的归属
	Scope Scope `json:"scope"`
	// TargetScopeType，TargetScopeIDs 要加入的scope，当这个参数有时，scope 参数只用来鉴权，不作为目标scope加入
	TargetScopeType ScopeType `json:"targetScopeType"`
	TargetScopeIDs  []int64   `json:"targetScopeIds"`
	// 成员角色，多角色
	Roles []string `json:"roles"`
	// 要添加的用户id列表
	UserIDs []string `json:"userIds"`
	// Deprecated: 可选选项
	Options MemberAddOptions `json:"options"`
	// 成员标签，多标签
	Labels []string `json:"labels"`
	// 邀请成员加入验证码
	VerifyCode string `json:"verifyCode"`
}

// MemberAddOptions 新增成员参数
type MemberAddOptions struct {
	// 是否覆盖已存在的成员
	Rewrite bool `json:"rewrite"`
}

// MemberAddResponse 添加成员 POST /api/members
type MemberAddResponse struct {
	Header
}

// MemberAddByInviteCodeRequest 通过邀请码添加成员请求
type MemberAddByInviteCodeRequest struct {
	VerifyCode string   `json:"verifyCode"`
	UserIDs    []string `json:"userIds"`
	OrgID      string   `json:"orgId"`
}

// MemberAddByInviteCodeResponse 通过邀请码添加成员响应
type MemberAddByInviteCodeResponse struct {
	Header
	Data string `json:"data"`
}

// MemberRemoveRequest 删除成员 POST /api/members/actions/remove
type MemberRemoveRequest struct {
	// 成员的归属
	Scope Scope `json:"scope"`
	// 要添加的用户id列表
	UserIDs []string `json:"userIds"`
	IdentityInfo
}

// MemberRemoveResponse 删除成员 POST /api/members/actions/remove
type MemberRemoveResponse struct {
	Header
	UserInfoHeader
}

// MemberDestroyRequest 删除一切成员信息响应请求
type MemberDestroyRequest struct {
	// 要添加的用户id列表
	UserIDs []string `json:"userIds"`
}

// MemberDestroyResponse 删除一切成员信息响应
type MemberDestroyResponse struct {
	Header
	UserInfoHeader
}

// GetMemberByTokenRequest 根据token查询成员
type GetMemberByTokenRequest struct {
	Token string `query:"token"`
}

// GetMemberByTokenResponse
type GetMemberByTokenResponse struct {
	Header
	Data Member `json:"data"`
}

// GetMemberByOrgRequest 根据企业查询用户 GET api/members/actions/get-by-org
type GetMemberByOrgRequest struct {
	OrgID string `query:"orgId"`
}

// GetMemberByOrgResponse 根据企业查询用户 GET /api/members/actions/get-by-org
type GetMemberByOrgResponse struct {
	Header
	UserInfoHeader
}

// ListMemberRolesByUserRequest 查询用户的角色列表请求
type ListMemberRolesByUserRequest struct {
	UserID    string    `schema:"userId"`
	ScopeType ScopeType `schema:"scopeType"`
	ParentID  int64     `schema:"parentId"`
	PageNo    int       `schema:"pageNo"`
	PageSize  int       `schema:"pageSize"`
}

// Check 检查request是否合法
func (lr *ListMemberRolesByUserRequest) Check() error {
	if lr.UserID == "" {
		return errors.Errorf("userId is empty")
	}

	if lr.ScopeType == "" {
		return errors.Errorf("scopeType is empty")
	}

	if lr.ScopeType != OrgScope && lr.ParentID == 0 {
		return errors.Errorf("parentId is empty")
	}

	if lr.PageNo == 0 {
		lr.PageNo = 1
	}

	if lr.PageSize == 0 {
		lr.PageSize = 15
	}

	return nil
}

// ListMemberRolesByUserResponse 查询用户的角色列表响应
type ListMemberRolesByUserResponse struct {
	Header
	UserInfoHeader
	Data UserRoleListResponseData `json:"data"`
}

type UserRoleListResponseData struct {
	List  []UserScopeRole `json:"list"`
	Total int             `json:"total"`
}

type UserScopeRole struct {
	ScopeType ScopeType `json:"scopeType"`
	ScopeID   int64     `json:"scopeId"`
	ScopeName string    `json:"scopeName"`
	Roles     []string  `json:"roles,omitempty"`
}

// GetAllOrganizationalResponse 获取所有人员组织架构响应
type GetAllOrganizationalResponse struct {
	Header
	Data GetAllOrganizationalData
}

// GetAllOrganizationalData 获取所有人员组织架构数据
type GetAllOrganizationalData struct {
	Organization map[string]map[string][]string          `json:"organization"` // [org_name][prj_name][]app_name
	Members      map[string]map[string]map[string]Member `json:"memberList"`   // [userID][scope_type][scope_name]member
}

// IsInPrj 判断用户是否在某个Prj下
func (od *GetAllOrganizationalData) IsInPrj(scopeNames map[string][]string, scopeType, userID string) bool {
	for name := range scopeNames {
		if _, ok := od.Members[userID][scopeType][name]; ok {
			return true
		}
	}

	return false
}

// IsInApp 判断用户是否在某个App下
func (od *GetAllOrganizationalData) IsInApp(scopeNames []string, scopeType, userID string) bool {
	for _, name := range scopeNames {
		if _, ok := od.Members[userID][scopeType][name]; ok {
			return true
		}
	}

	return false
}

type ListScopeManagersByScopeIDRequest struct {
	ScopeType ScopeType `json:"scopeType"`
	ScopeID   int64     `json:"scopeID"`
}

type ListScopeManagersByScopeIDResponse struct {
	Header
	Data []Member `json:"data"`
}

type ListMembersWithoutExtraByScopeRequest struct {
	ScopeType ScopeType `json:"scopeType"`
	ScopeID   int64     `json:"scopeID"`
}

type ListMembersWithoutExtraByScopeResponse struct {
	Header
	Data int `json:"data"`
}

type GetMemberByUserAndScopeRequest struct {
	ScopeType ScopeType `json:"scopeType"`
	ScopeID   int64     `json:"scopeID"`
	UserID    string    `json:"userID"`
}

type GetMemberByUserAndScopeResponse struct {
	Header
	Data []Member `json:"data"`
}
