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

package endpoints

import (
	"context"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/core/legacy/types"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/desensitize"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
)

// CreateOrUpdateMember 添加成员/修改成员角色
func (e *Endpoints) CreateOrUpdateMember(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrAddMember.NotLogin().ToResp(), nil
	}

	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrAddMember.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var memberCreateReq apistructs.MemberAddRequest
	if err := json.NewDecoder(r.Body).Decode(&memberCreateReq); err != nil {
		return apierrors.ErrAddMember.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("create request: %+v", memberCreateReq)

	// 创建/更新成员信息至DB
	if err := e.member.CreateOrUpdate(userID.String(), memberCreateReq); err != nil {
		return apierrors.ErrAddMember.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("add members succ")
}

// UpdateMemberUserInfo 更新成员 user info 的数据，和 uc 同步
func (e *Endpoints) UpdateMemberUserInfo(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 鉴权，创建接口只允许内部调用
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		return apierrors.ErrUpdateMemeberUserInfo.AccessDenied().ToResp(), nil
	}
	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrUpdateMemeberUserInfo.MissingParameter("body").ToResp(), nil
	}
	var updateUserInfoReq apistructs.MemberUserInfoUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&updateUserInfoReq); err != nil {
		return apierrors.ErrUpdateMemeberUserInfo.InvalidParameter(err).ToResp(), nil
	}

	// 创建/更新成员信息至DB
	if err := e.member.UpdateMemberUserInfo(updateUserInfoReq); err != nil {
		return apierrors.ErrUpdateMemeberUserInfo.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("update member's userinfo succ")
}

// GetMemberByToken 根据token查询用户
func (e *Endpoints) GetMemberByToken(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	token := r.URL.Query().Get("token")
	item, err := e.member.GetByToken(token)
	if err != nil {
		return apierrors.ErrListMember.InvalidParameter(err).ToResp(), nil
	}
	member := apistructs.Member{
		UserID: item.UserID,
		Name:   item.Name,
		Nick:   item.Nick,
		Avatar: item.Avatar,
		Scope: apistructs.Scope{
			Type: item.ScopeType,
			ID:   strconv.FormatInt(item.ScopeID, 10),
		},
		Roles:  item.Roles,
		Labels: make([]string, 0),
	}
	// 添加成员标签
	if member.Scope.Type == apistructs.OrgScope {
		labels, err := e.member.ListMemberLabel(member.UserID, item.ScopeID)
		if err != nil {
			return apierrors.ErrListMember.InvalidParameter(err).ToResp(), nil
		}
		member.Labels = labels
	}
	return httpserver.OkResp(member)
}

// ListMemberRoles 获取企业下面的角色列表
func (e *Endpoints) ListMemberRoles(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 检查参数scopeType
	v := r.URL.Query().Get("scopeType")
	scopeType := apistructs.ScopeType(v)
	if _, ok := types.AllScopeRoleMap[scopeType]; !ok {
		return nil, errors.Errorf("invalid request, scopeType is invalid")
	}
	scopeIDStr := r.URL.Query().Get("scopeId")

	// 检查是否为发布商企业
	var (
		orgID int64
		err   error
	)

	if scopeType == apistructs.OrgScope && scopeIDStr != "" {
		scopeID, err := strconv.ParseInt(scopeIDStr, 10, 64)
		if err != nil {
			return nil, errors.Errorf("invalid param, scopeID is invalid")
		}
		orgID = scopeID
	} else {
		// 尝试从头里获取
		orgIDStr := r.Header.Get(httputil.OrgHeader)
		if orgIDStr != "" {
			orgID, err = strconv.ParseInt(orgIDStr, 10, 64)
			if err != nil {
				return nil, errors.Errorf("invalid param, orgId is invalid")
			}
		}
	}

	if orgID == 0 && scopeType != apistructs.SysScope {
		return nil, errors.Errorf("invalid param, orgId is empty")
	}

	// 只是获取角色列表，不需要鉴权
	l := e.GetLocale(r)
	lenth := len(types.AllScopeRoleMap[scopeType])
	tmpRoles := make([]apistructs.RoleInfo, lenth)
	for _, info := range types.AllScopeRoleMap[scopeType] {
		if !info.IsHide {
			tmpRoles[info.Level] = apistructs.RoleInfo{
				Role: info.Role,
				Name: l.Get(info.I18nKey),
			}
		}
	}

	return httpserver.OkResp(apistructs.RoleList{List: tmpRoles, Total: len(tmpRoles)})
}

// ListMemberRolesByUser 查看某个用户的角色
func (e *Endpoints) ListMemberRolesByUser(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListMemberRoles.NotLogin().ToResp(), nil
	}

	// 检查请求
	var pageReq apistructs.ListMemberRolesByUserRequest
	if err := e.queryStringDecoder.Decode(&pageReq, r.URL.Query()); err != nil {
		return apierrors.ErrListMemberRoles.InvalidParameter(err).ToResp(), nil
	}
	if err := pageReq.Check(); err != nil {
		return apierrors.ErrListMemberRoles.InvalidParameter(err).ToResp(), nil
	}

	local := e.GetLocale(r)
	total, roleList, err := e.member.ListMemberRolesByUser(local, identityInfo, pageReq)
	if err != nil {
		return apierrors.ErrListMemberRoles.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(apistructs.UserRoleListResponseData{List: roleList, Total: total})
}

// ListMeberLabels 查询dice所有的成员标签
func (e *Endpoints) ListMeberLabels(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	l := e.GetLocale(r)
	labels := make([]apistructs.MemberLabelInfo, 0)
	for _, info := range types.AllLabelsMap {
		labels = append(labels, apistructs.MemberLabelInfo{
			Label: info.Label,
			Name:  l.Get(info.I18nKey),
		})
	}
	return httpserver.OkResp(apistructs.MemberLabelList{List: labels})
}

// ListMember 获取成员列表/查询成员
func (e *Endpoints) ListMember(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取查询参数 & 参数合法性校验
	param, err := getMemberQueryParam(r)
	if err != nil {
		return apierrors.ErrListMember.InvalidParameter(err).ToResp(), nil
	}

	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		// 获取当前用户
		userID, err := user.GetUserID(r)
		if err != nil {
			return apierrors.ErrListMember.NotLogin().ToResp(), nil
		}

		// 操作鉴权
		req := apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    param.ScopeType,
			ScopeID:  uint64(param.ScopeID),
			Resource: apistructs.MemberResource,
			Action:   apistructs.ListAction,
		}
		if access, err := e.permission.CheckPermission(&req); err != nil || !access {
			// 企业管理员也能list member
			if err = e.member.CheckPermission(userID.String(), param.ScopeType, param.ScopeID); err != nil {
				return apierrors.ErrListMember.AccessDenied().ToResp(), nil
			}
		}
	}

	total, members, err := e.member.List(param)
	if err != nil {
		return apierrors.ErrListMember.InternalError(err).ToResp(), nil
	}

	// 组装成API所需格式
	memberList := make([]apistructs.Member, 0, len(members))
	for _, item := range members {
		var email, mobile string
		if internalClient == "" {
			email = desensitize.Email(item.Email)
			mobile = desensitize.Mobile(item.Mobile)
		} else {
			email = item.Email
			mobile = item.Mobile
		}

		member := apistructs.Member{
			UserID: item.UserID,
			Email:  email,
			Mobile: mobile,
			Name:   item.Name,
			Nick:   item.Nick,
			Avatar: item.Avatar,
			Scope: apistructs.Scope{
				Type: item.ScopeType,
				ID:   strconv.FormatInt(item.ScopeID, 10),
			},
			Roles:   item.Roles,
			Labels:  item.Labels,
			Deleted: item.Deleted,
		}
		memberList = append(memberList, member)
	}

	return httpserver.OkResp(apistructs.MemberList{List: memberList, Total: total})
}

// DeleteMember 移除成员
func (e *Endpoints) DeleteMember(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 获取当前用户
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrDeleteMember.NotLogin().ToResp(), nil
	}

	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrDeleteMember.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var memberDeleteReq apistructs.MemberRemoveRequest
	if err := json.NewDecoder(r.Body).Decode(&memberDeleteReq); err != nil {
		return apierrors.ErrDeleteMember.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("user: %s, delete request: %+v", userID.String(), memberDeleteReq)

	// 最后一位成员时不能删除
	// scopeID, _ := strconv.ParseInt(memberDeleteReq.Scope.ID, 10, 64)
	// queryParam := &apistructs.MemberListRequest{
	// 	ScopeType: memberDeleteReq.Scope.Type,
	// 	ScopeID:   scopeID,
	// }
	// total, _, err := e.member.List(queryParam)
	// if err != nil {
	// 	return apierrors.ErrDeleteMember.InternalError(err).ToResp(), nil
	// }
	// if total <= 1 {
	// 	return apierrors.ErrDeleteMember.InvalidState("唯一成员不能删除").ToResp(), nil
	// }

	if err := e.member.Delete(userID.String(), memberDeleteReq); err != nil {
		return apierrors.ErrDeleteMember.InternalError(err).ToResp(), nil
	}

	if memberDeleteReq.Scope.Type == apistructs.OrgScope {
		// set currentOrgID to any org user can visit
		for _, uid := range memberDeleteReq.UserIDs {
			members, err := e.member.ListByScopeTypeAndUser(apistructs.OrgScope, uid)
			if err != nil {
				continue
			}
			if len(members) == 0 {
				e.db.DeleteCurrentOrg(uid)
			} else if len(members) > 0 {
				e.db.UpdateCurrentOrg(uid, members[0].ScopeID)
			}
		}
	}

	return httpserver.OkResp("delete member success")
}

// DestroyMember 删除用户的一切成员信息
func (e *Endpoints) DestroyMember(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		return apierrors.ErrDeleteMember.AccessDenied().ToResp(), nil
	}
	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrDeleteMember.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var memberDestroyRequest apistructs.MemberDestroyRequest
	if err := json.NewDecoder(r.Body).Decode(&memberDestroyRequest); err != nil {
		return apierrors.ErrDeleteMember.InvalidParameter(err).ToResp(), nil
	}
	logrus.Infof("destroy request: %+v", memberDestroyRequest)
	members, err := e.member.ListOrgByUserIDs(memberDestroyRequest.UserIDs)
	if err != nil {
		return apierrors.ErrDeleteMember.InternalError(err).ToResp(), nil
	}

	orgMemberMap := make(map[int64][]string)
	for _, member := range members {
		orgMemberMap[member.ScopeID] = append(orgMemberMap[member.ScopeID], member.UserID)
	}

	for orgID, userIDs := range orgMemberMap {
		memberDeleteReq := apistructs.MemberRemoveRequest{
			Scope: apistructs.Scope{
				Type: apistructs.OrgScope,
				ID:   strconv.FormatInt(orgID, 10),
			},
			UserIDs: userIDs,
		}

		if err := e.member.Delete("onlyYou", memberDeleteReq); err != nil {
			return apierrors.ErrDeleteMember.InternalError(err).ToResp(), nil
		}
		for _, uid := range memberDeleteReq.UserIDs {
			members, err := e.member.ListByScopeTypeAndUser(apistructs.OrgScope, uid)
			if err != nil {
				continue
			}
			if len(members) == 0 {
				e.db.DeleteCurrentOrg(uid)
			} else if len(members) > 0 {
				e.db.UpdateCurrentOrg(uid, members[0].ScopeID)
			}
		}
	}

	return httpserver.OkResp("destroy member success")
}

// CreateMemberByInviteCode 通过邀请验证码添加成员
func (e *Endpoints) CreateMemberByInviteCode(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	// 检查body是否为空
	if r.Body == nil {
		return apierrors.ErrAddMember.MissingParameter("body").ToResp(), nil
	}
	// 检查body格式
	var req apistructs.MemberAddByInviteCodeRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		return apierrors.ErrAddMember.InvalidParameter(err).ToResp(), nil
	}
	if req.OrgID == "" {
		return apierrors.ErrAddMember.MissingParameter("orgId").ToResp(), nil
	}
	logrus.Infof("create request: %+v", req)

	userID, err := e.member.CheckInviteCode(req.VerifyCode, req.OrgID)
	if err != nil {
		return apierrors.ErrAddMember.ErrorVerificationCode(err).ToResp(), nil
	}

	// 创建/更新成员信息至DB
	if err := e.member.CreateOrUpdate(userID, apistructs.MemberAddRequest{
		Scope: apistructs.Scope{
			Type: apistructs.OrgScope,
			ID:   req.OrgID,
		},
		Roles:   []string{"Dev"},
		UserIDs: req.UserIDs,
	}); err != nil {
		return apierrors.ErrAddMember.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp("add members succ")
}

// GetAllOrganizational 获取所有的人员组织架构
func (e *Endpoints) GetAllOrganizational(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		// 获取当前用户
		userID, err := user.GetUserID(r)
		if err != nil {
			return apierrors.ErrListMember.NotLogin().ToResp(), nil
		}
		// 操作鉴权，只有admin有权限
		access, err := e.permission.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   userID.String(),
			Scope:    apistructs.SysScope,
			ScopeID:  1,
			Resource: apistructs.OrgResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return apierrors.ErrListMember.InternalError(err).ToResp(), nil
		}
		if !access {
			return apierrors.ErrListMember.AccessDenied().ToResp(), nil
		}
	}

	result, err := e.member.GetAllOrganizational()
	if err != nil {
		return apierrors.ErrListMember.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(result)
}

// 查询成员时获取查询参数
func getMemberQueryParam(r *http.Request) (*apistructs.MemberListRequest, error) {
	// 检查参数scopeType
	v := r.URL.Query().Get("scopeType")
	scopeType := apistructs.ScopeType(v)
	if _, ok := types.AllScopeRoleMap[scopeType]; !ok {
		return nil, errors.Errorf("invalid request, scopeType is invalid")
	}

	// 检查参数scopeId
	scopeIDStr := r.URL.Query().Get("scopeId")
	if scopeIDStr == "" {
		return nil, errors.Errorf("invalid request, scopeId is empty")
	}
	scopeID, err := strutil.Atoi64(scopeIDStr)
	if err != nil {
		return nil, errors.Errorf("invalid request, scopeId is invalid")
	}

	// 检查参数roles
	roles := r.URL.Query()["roles"]
	for _, role := range roles {
		if !types.CheckIfRoleIsValid(role) {
			return nil, errors.Errorf("invalid request, role is invalid")
		}
	}

	labels := r.URL.Query()["label"]
	for _, label := range labels {
		if !types.CheckIfMemberLabelIsValid(label) {
			return nil, errors.Errorf("invalid request, label is invalid")
		}
	}

	// 检查参数q
	keyword := r.URL.Query().Get("q")

	// 检查参数pageNo
	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, errors.Errorf("invalid request, pageNo is invalid")
	}
	// 检查参数pageSize
	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "20"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, errors.Errorf("invalid request, pageSize is invalid")
	}
	// TODO 组件化协议选人组件暂不支持 show more，先不做限制
	//if pageSize > 50 {
	//	pageSize = 50
	//}

	return &apistructs.MemberListRequest{
		ScopeType: scopeType,
		ScopeID:   scopeID,
		Roles:     roles,
		Labels:    labels,
		Q:         keyword,
		PageNo:    pageNo,
		PageSize:  pageSize,
	}, nil
}

// ListScopeManagersByScopeID list managers by scopeID
func (e *Endpoints) ListScopeManagersByScopeID(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	internalClient := r.Header.Get(httputil.InternalHeader)
	if internalClient == "" {
		return apierrors.ErrListMemberRoles.NotLogin().ToResp(), nil
	}

	var req apistructs.ListScopeManagersByScopeIDRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ErrListMemberRoles.InvalidParameter(err).ToResp(), nil
	}

	list, err := e.member.GetScopeManagersByScopeID(req.ScopeType, req.ScopeID)
	if err != nil {
		return apierrors.ErrListMember.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(list)
}

// CountMembersWithoutExtraByScope .
func (e *Endpoints) CountMembersWithoutExtraByScope(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ListMembersWithoutExtraByScope.NotLogin().ToResp(), nil
	}

	var req apistructs.ListMembersWithoutExtraByScopeRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.ListMembersWithoutExtraByScope.InvalidParameter(err).ToResp(), nil
	}

	total, _, err := e.db.GetMembersWithoutExtraByScope(req.ScopeType, req.ScopeID)
	if err != nil {
		return apierrors.ListMembersWithoutExtraByScope.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(total)
}

// GetMemberByUserAndScope get member by user and scope
func (e *Endpoints) GetMemberByUserAndScope(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	_, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.GetMemberByUserAndScope.NotLogin().ToResp(), nil
	}

	var req apistructs.GetMemberByUserAndScopeRequest
	if err := e.queryStringDecoder.Decode(&req, r.URL.Query()); err != nil {
		return apierrors.GetMemberByUserAndScope.InvalidParameter(err).ToResp(), nil
	}

	members, err := e.member.GetByUserAndScope(req.UserID, req.ScopeType, req.ScopeID)
	if err != nil {
		return apierrors.GetMemberByUserAndScope.InternalError(err).ToResp(), nil
	}
	membersDto := make([]apistructs.Member, 0, len(members))
	for _, v := range members {
		membersDto = append(membersDto, v.Convert2APIDTO())
	}

	return httpserver.OkResp(membersDto)
}
