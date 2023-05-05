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
	"encoding/json"
	"errors"
	"fmt"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apierrors"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

var UC_USER_UPDATE_LOGIN_METHOD = apis.ApiSpec{
	Path:         "/api/users/<userID>/actions/update-login-method",
	Scheme:       "http",
	Method:       "POST",
	Custom:       updateLoginMethod,
	CheckLogin:   true,
	CheckToken:   true,
	RequestType:  apistructs.UserUpdateLoginMethodRequest{},
	ResponseType: apistructs.UserUpdateLoginMethodResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 修改用户登录方式",
}

func updateLoginMethod(w http.ResponseWriter, r *http.Request) {
	operatorID, err := user.GetUserID(r)
	if err != nil {
		apierrors.ErrAdminUser.NotLogin().Write(w)
		return
	}

	if err := checkPermission(operatorID, apistructs.UpdateAction); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	token, err := auth.GetDiceClientToken()
	if err != nil {
		logrus.Errorf("failed to get token: %v", err)
		apierrors.ErrBatchFreezeUser.InternalError(err).
			Write(w)
		return
	}
	// check login & permission
	_, err = mustManageUsersPerm(r, apierrors.ErrBatchFreezeUser)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	// get req
	var req apistructs.UserUpdateLoginMethodRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	userID := strutil.Split(r.URL.Path, "/", true)[2]
	req.ID = userID

	// handle
	if err := handleUpdateLoginMethod(req, operatorID.String(), token); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	httpserver.WriteData(w, nil)
}

func handleUpdateLoginMethod(req apistructs.UserUpdateLoginMethodRequest, operatorID string, token ucauth.OAuthToken) error {
	var resp struct {
		Success bool   `json:"success"`
		Result  bool   `json:"result"`
		Error   string `json:"error"`
	}
	r, err := httpclient.New().Post(discover.UC()).
		Path("/api/user/admin/change-full-info").
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		Header("operatorID", operatorID).
		JSONBody(&req).
		Do().JSON(&resp)
	if err != nil {
		logrus.Errorf("failed to invoke change user login method, (%v)", err)
		return apierrors.ErrUpdateLoginMethod.InternalError(err)
	}
	if !r.IsOK() {
		logrus.Debugf("failed to change user login method, statusCode: %d, %v", r.StatusCode(), string(r.Body()))
		return apierrors.ErrUpdateLoginMethod.InternalError(fmt.Errorf("internal status code: %v", r.StatusCode()))
	}
	if !resp.Success {
		logrus.Debugf("failed to change user login method: %+v", resp.Error)
		return apierrors.ErrUpdateLoginMethod.InternalError(errors.New(resp.Error))
	}
	return nil
}
