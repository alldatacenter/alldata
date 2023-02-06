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
	"net/http"
	"strconv"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/services/apierrors"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/pkg/desensitize"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

// 限制批量查询最大用户数
const maxUserSize = 100

// ListUser 根据user id列表批量获取用户
func (e *Endpoints) ListUser(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	var (
		users []ucauth.User
		err   error
	)

	// 鉴权
	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListUser.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		// 从 Header 获取 OrgID
		var orgID uint64
		orgIDStr := r.Header.Get(httputil.OrgHeader)
		if orgIDStr != "" {
			orgID, err = strconv.ParseUint(orgIDStr, 10, 64)
			if err != nil {
				return apierrors.ErrListUser.InvalidParameter("orgId is invalid").ToResp(), nil
			}
		}

		access, err := e.permission.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgID,
			Resource: apistructs.MemberResource,
			Action:   apistructs.CreateAction, // 只有企业管理员可以用这个接口，用createAction代替一下
		})
		if err != nil {
			return apierrors.ErrListUser.InternalError(err).ToResp(), nil
		}
		if !access {
			return apierrors.ErrListUser.AccessDenied().ToResp(), nil
		}
	}

	keyword := r.URL.Query().Get("q")
	if keyword != "" { // search by keyword
		users, err = e.uc.FindUsersByKey(keyword)
		if err != nil {
			return apierrors.ErrListUser.InternalError(err).ToResp(), nil
		}
	} else if identityInfo.IsInternalClient() { // 按userID列表批量查找用户，这个接口不能暴露出去，以防通过暴力枚举userid获取userinfo
		// 检查请求参数
		params := r.URL.Query()
		if params == nil {
			return apierrors.ErrListUser.MissingParameter("user id").ToResp(), nil
		}
		userIDs := params["userID"]
		userIDs = strutil.DedupSlice(userIDs, true)
		if len(userIDs) > maxUserSize {
			return apierrors.ErrListUser.InvalidParameter("user id too much").ToResp(), nil
		}

		users, err = e.uc.FindUsers(userIDs)
		if err != nil {
			return apierrors.ErrListUser.InternalError(err).ToResp(), nil
		}
	}

	var plaintext bool
	plaintextStr := r.URL.Query().Get("plaintext")
	if plaintextStr == "true" {
		plaintext = true
	}
	// 用户信息转换
	userInfos := make([]apistructs.UserInfo, 0, len(users))
	for i := range users {
		userInfos = append(userInfos, *convertToUserInfo(&users[i], plaintext))
	}

	return httpserver.OkResp(apistructs.UserListResponseData{Users: userInfos})
}

// GetCurrentUser 获取当前登录用户信息
func (e *Endpoints) GetCurrentUser(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	userID, err := user.GetUserID(r)
	if err != nil {
		return apierrors.ErrGetUser.NotLogin().ToResp(), nil
	}

	// 获取用户详情
	user, err := e.uc.GetUser(userID.String())
	if err != nil {
		return apierrors.ErrGetUser.InternalError(err).ToResp(), nil
	}

	return httpserver.OkResp(*convertToUserInfo(user, false))
}

func (e *Endpoints) GetUcUserID(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	id := r.URL.Query().Get("id")
	userID, err := e.db.GetUcUserID(id)
	if err != nil {
		return apierrors.ErrGetUser.InternalError(err).ToResp(), nil
	}
	return httpserver.OkResp(userID)
}

func convertToUserInfo(user *ucauth.User, plaintext bool) *apistructs.UserInfo {
	if !plaintext {
		user.Phone = desensitize.Mobile(user.Phone)
		user.Email = desensitize.Email(user.Email)
	}
	return &apistructs.UserInfo{
		ID:     user.ID,
		Name:   user.Name,
		Nick:   user.Nick,
		Avatar: user.AvatarURL,
		Phone:  user.Phone,
		Email:  user.Email,
	}
}

func (e *Endpoints) SearchUser(ctx context.Context, r *http.Request, vars map[string]string) (httpserver.Responser, error) {
	var (
		users []ucauth.User
		err   error
	)

	identityInfo, err := user.GetIdentityInfo(r)
	if err != nil {
		return apierrors.ErrListUser.NotLogin().ToResp(), nil
	}
	if !identityInfo.IsInternalClient() {
		var orgID uint64
		orgIDStr := r.Header.Get(httputil.OrgHeader)
		if orgIDStr != "" {
			orgID, err = strconv.ParseUint(orgIDStr, 10, 64)
			if err != nil {
				return apierrors.ErrListUser.InvalidParameter("orgId is invalid").ToResp(), nil
			}
		}

		access, err := e.permission.CheckPermission(&apistructs.PermissionCheckRequest{
			UserID:   identityInfo.UserID,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgID,
			Resource: apistructs.MemberResource,
			Action:   apistructs.CreateAction,
		})
		if err != nil {
			return apierrors.ErrListUser.InternalError(err).ToResp(), nil
		}
		if !access {
			return apierrors.ErrListUser.AccessDenied().ToResp(), nil
		}
	}

	req, err := getUserParam(r)
	if err != nil {
		return apierrors.ErrListUser.InvalidParameter("pageSize").ToResp(), nil
	}
	users, err = e.uc.FindUsersByKey(req.Name)
	if err != nil {
		return apierrors.ErrListUser.InternalError(err).ToResp(), nil
	}

	var plaintext bool
	plaintextStr := r.URL.Query().Get("plaintext")
	if plaintextStr == "true" {
		plaintext = true
	}

	userInfos := make([]apistructs.UserInfo, 0, len(users))
	for i := range users {
		userInfos = append(userInfos, *convertToUserInfo(&users[i], plaintext))
	}

	return httpserver.OkResp(apistructs.UserListResponseData{Users: userInfos})
}

func getUserParam(r *http.Request) (*apistructs.UserPagingRequest, error) {
	keyword := r.URL.Query().Get("q")

	pageSizeStr := r.URL.Query().Get("pageSize")
	if pageSizeStr == "" {
		pageSizeStr = "15"
	}
	pageSize, err := strconv.Atoi(pageSizeStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageSize is invalid")
	}

	pageNoStr := r.URL.Query().Get("pageNo")
	if pageNoStr == "" {
		pageNoStr = "1"
	}
	pageNo, err := strconv.Atoi(pageNoStr)
	if err != nil {
		return nil, errors.Errorf("invalid param, pageNo is invalid")
	}

	return &apistructs.UserPagingRequest{
		Name:     keyword,
		PageNo:   pageNo,
		PageSize: pageSize,
	}, nil
}
