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

package bundle

import (
	"bytes"
	"fmt"
	"strconv"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httputil"
)

// GetMemberByToken get member by token
func (b *Bundle) GetMemberByToken(request *apistructs.GetMemberByTokenRequest) (*apistructs.Member, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var fetchResp apistructs.GetMemberByTokenResponse
	resp, err := hc.Get(host).Path("/api/members/actions/get-by-token").
		Param("token", request.Token).
		Header(httputil.InternalHeader, "bundle").Do().JSON(&fetchResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !fetchResp.Success {
		return nil, toAPIError(resp.StatusCode(), fetchResp.Error)
	}

	return &fetchResp.Data, nil
}

func (b *Bundle) ListMembers(req apistructs.MemberListRequest) ([]apistructs.Member, error) {
	r, err := b.GetMembers(req)
	if err != nil {
		return nil, err
	}
	return r.List, nil
}

func (b *Bundle) GetMembers(req apistructs.MemberListRequest) (*apistructs.MemberList, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc
	var r apistructs.MemberListResponse
	request := hc.Get(host).Path("/api/members").
		Header(httputil.InternalHeader, "bundle").
		Param("scopeType", string(req.ScopeType)).
		Param("scopeId", strconv.FormatInt(req.ScopeID, 10)).
		Param("q", req.Q).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize))
	for _, role := range req.Roles {
		request = request.Param("roles", role)
	}
	for _, label := range req.Labels {
		request = request.Param("label", label)
	}
	resp, err := request.Do().JSON(&r)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !r.Success {
		return nil, toAPIError(resp.StatusCode(), r.Error)
	}
	return &r.Data, nil
}

func (b *Bundle) ListMembersAndTotal(req apistructs.MemberListRequest) (*apistructs.MemberList, error) {
	r, err := b.GetMembers(req)
	if err != nil {
		return nil, err
	}
	return r, nil
}

// UpdateMemberUserInfo 更新成员的用户信息
func (b *Bundle) UpdateMemberUserInfo(req apistructs.MemberUserInfoUpdateRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Put(host).Path("/api/members/actions/update-userinfo").
		Header(httputil.InternalHeader, "bundle").JSONBody(&req).Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to update member's user info, status code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

// DeleteMember 移除成员
func (b *Bundle) DeleteMember(req apistructs.MemberRemoveRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/members/actions/remove").Header("User-ID", req.UserID).
		Header(httputil.InternalHeader, "bundle").JSONBody(&req).Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to remove member, status code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

// DestroyUsers 删除用户一切成员信息
func (b *Bundle) DestroyUsers(req apistructs.MemberDestroyRequest) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var buf bytes.Buffer
	resp, err := hc.Post(host).Path("/api/members/actions/destroy").
		Header(httputil.InternalHeader, "bundle").JSONBody(&req).Do().Body(&buf)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to destroy member, status code: %d, body: %v",
				resp.StatusCode(),
				buf.String(),
			))
	}
	return nil
}

// ListMemberRolesByUser 查看某个用户在一个scope下的权限
func (b *Bundle) ListMemberRolesByUser(req apistructs.ListMemberRolesByUserRequest) (
	*apistructs.UserRoleListResponseData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var memberResp apistructs.ListMemberRolesByUserResponse
	resp, err := hc.Get(host).Path("/api/members/actions/list-user-roles").
		Header(httputil.InternalHeader, "bundle").
		Header("lang", "en-US").
		Param("userId", req.UserID).
		Param("scopeType", string(req.ScopeType)).
		Param("parentId", strconv.FormatInt(req.ParentID, 10)).
		Param("pageNo", strconv.Itoa(req.PageNo)).
		Param("pageSize", strconv.Itoa(req.PageSize)).
		Do().JSON(&memberResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to list user's role, status code: %d, body: %v",
				resp.StatusCode(),
				memberResp.Error,
			))
	}

	return &memberResp.Data, nil
}

// GetAllOrganizational 获取所有的组织架构
func (b *Bundle) GetAllOrganizational() (*apistructs.GetAllOrganizationalData, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := httpclient.New(
		httpclient.WithTimeout(time.Second, time.Minute*3),
	)

	var organizationalResp apistructs.GetAllOrganizationalResponse
	resp, err := hc.Get(host).Path("/api/members/actions/get-all-organizational").
		Header(httputil.InternalHeader, "bundle").Do().JSON(&organizationalResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to list all organizational, status code: %d, body: %v",
				resp.StatusCode(),
				organizationalResp.Error,
			))
	}

	return &organizationalResp.Data, nil
}

// ListScopeManagersByScopeID list manages by scopeID
func (b *Bundle) ListScopeManagersByScopeID(req apistructs.ListScopeManagersByScopeIDRequest) (
	[]apistructs.Member, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var memberResp apistructs.ListScopeManagersByScopeIDResponse
	resp, err := hc.Get(host).Path("/api/members/actions/list-by-scopeID").
		Header(httputil.InternalHeader, "bundle").
		Param("scopeType", string(req.ScopeType)).
		Param("scopeID", strconv.FormatInt(req.ScopeID, 10)).
		Do().JSON(&memberResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to ListScopeManagersByScopeID, status code: %d, body: %v",
				resp.StatusCode(),
				memberResp.Error,
			))
	}

	return memberResp.Data, nil
}

// ListMemberRoles list member roles
func (b *Bundle) ListMemberRoles(req apistructs.ListScopeManagersByScopeIDRequest, orgID int64, locale string) (*apistructs.RoleList, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var memberResp apistructs.MemberRoleListResponse
	resp, err := hc.Get(host).Path("/api/members/actions/list-roles").
		Header(httputil.InternalHeader, "bundle").
		Header(httputil.OrgHeader, strconv.FormatInt(orgID, 10)).
		Header(httputil.LangHeader, locale).
		Param("scopeType", string(req.ScopeType)).
		Param("scopeID", strconv.FormatInt(req.ScopeID, 10)).
		Do().JSON(&memberResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to ListMemberRoles, status code: %d, body: %v",
				resp.StatusCode(),
				memberResp.Error,
			))
	}

	return &memberResp.Data, nil
}

func (b *Bundle) AddMember(req apistructs.MemberAddRequest, userID string) error {
	host, err := b.urls.CoreServices()
	if err != nil {
		return err
	}
	hc := b.hc

	var respData apistructs.MemberAddResponse
	resp, err := hc.Post(host).Path("/api/members").
		Header(httputil.UserHeader, userID).
		JSONBody(req).Do().JSON(&respData)
	if err != nil {
		return apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !respData.Success {
		return toAPIError(resp.StatusCode(), respData.Error)
	}

	return nil
}

// CountMembersWithoutExtraByScope count member
func (b *Bundle) CountMembersWithoutExtraByScope(scopeType string, scopeID uint64) (int, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return 0, err
	}
	hc := b.hc

	var memberResp apistructs.ListMembersWithoutExtraByScopeResponse
	resp, err := hc.Get(host).Path("/api/members/actions/count-by-only-scopeID").
		Header(httputil.InternalHeader, "bundle").
		Param("scopeType", scopeType).
		Param("scopeID", strconv.FormatUint(scopeID, 10)).
		Do().JSON(&memberResp)
	if err != nil {
		return 0, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return 0, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to ListMembersWithoutExtraByScope, status code: %d, body: %v",
				resp.StatusCode(),
				memberResp.Error,
			))
	}

	return memberResp.Data, nil
}

// GetMemberByUserAndScope get member by user and scope
func (b *Bundle) GetMemberByUserAndScope(scopeType apistructs.ScopeType, userID string, scopeID uint64) ([]apistructs.Member, error) {
	host, err := b.urls.CoreServices()
	if err != nil {
		return nil, err
	}
	hc := b.hc

	var memberResp apistructs.GetMemberByUserAndScopeResponse
	resp, err := hc.Get(host).Path("/api/members/actions/get-by-user-and-scope").
		Header(httputil.InternalHeader, "bundle").
		Param("scopeType", string(scopeType)).
		Param("userID", userID).
		Param("scopeID", strconv.FormatUint(scopeID, 10)).
		Do().JSON(&memberResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() {
		return nil, apierrors.ErrInvoke.InternalError(
			fmt.Errorf("failed to GetMemberByUserAndScope, status code: %d, body: %v",
				resp.StatusCode(),
				memberResp.Error,
			))
	}

	return memberResp.Data, nil
}
