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

package uc

import (
	"net/http"
	"strconv"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apierrors"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/ucauth"
)

var UC_USER_PAGING = apis.ApiSpec{
	Path:         "/api/users/actions/paging",
	Scheme:       "http",
	Method:       "GET",
	Custom:       pagingUsers,
	CheckLogin:   true,
	CheckToken:   true,
	RequestType:  apistructs.UserPagingRequest{},
	ResponseType: apistructs.UserPagingResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 用户分页",
}

func pagingUsers(w http.ResponseWriter, r *http.Request) {
	operatorID, err := user.GetUserID(r)
	if err != nil {
		apierrors.ErrAdminUser.NotLogin().Write(w)
		return
	}

	if err := checkPermission(operatorID, apistructs.GetAction); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	// TODO: nothing to do when oryEnabled
	token, err := auth.GetDiceClientToken()
	if err != nil {
		logrus.Errorf("failed to get token: %v", err)
		apierrors.ErrListUser.InternalError(err).
			Write(w)
		return
	}

	// check login & permission
	_, err = mustManageUsersPerm(r, apierrors.ErrListUser)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	req, err := getPagingUsersReq(r)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	data, err := ucauth.HandlePagingUsers(req, token)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	httpserver.WriteData(w, ucauth.ConvertToUserInfoExt(data))
}

func getPagingUsersReq(r *http.Request) (*apistructs.UserPagingRequest, error) {
	req := apistructs.UserPagingRequest{
		Name:  r.URL.Query().Get("name"),
		Nick:  r.URL.Query().Get("nick"),
		Phone: r.URL.Query().Get("phone"),
		Email: r.URL.Query().Get("email"),
	}
	v := r.URL.Query().Get("locked")
	if v != "" {
		var locked int
		if v == "true" {
			locked = 1
		} else if v == "false" {
			locked = 0
		} else {
			return nil, apierrors.ErrListUser.InvalidParameter("invalid parameter locked")
		}
		req.Locked = &locked
	}
	v = r.URL.Query().Get("source")
	if v != "" {
		req.Source = v
	}
	v = r.URL.Query().Get("pageNo")
	if v != "" {
		pageNo, err := strconv.Atoi(v)
		if err != nil {
			return nil, apierrors.ErrListUser.InvalidParameter(err)
		}
		req.PageNo = pageNo
	}
	v = r.URL.Query().Get("pageSize")
	if v != "" {
		pageSize, err := strconv.Atoi(v)
		if err != nil {
			return nil, apierrors.ErrListUser.InvalidParameter(err)
		}
		req.PageSize = pageSize
	}
	return &req, nil
}

func mustManageUsersPerm(r *http.Request, errBuilder *errorresp.APIError) (string, error) {
	// check login
	userID, err := user.GetUserID(r)
	if err != nil {
		logrus.Errorf("failed to get userID, (%v)", err)
		return "", errBuilder.NotLogin()
	}
	// check permission
	if !isManageUsersPerm(userID) {
		return "", errBuilder.AccessDenied()
	}
	return userID.String(), nil
}

func isManageUsersPerm(userID user.ID) bool {
	// TODO: check permission
	return true
}
