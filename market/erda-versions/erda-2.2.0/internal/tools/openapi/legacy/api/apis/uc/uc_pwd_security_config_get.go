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
	"errors"
	"fmt"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
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

var UC_PWD_SECURITY_CONFIG_GET = apis.ApiSpec{
	Path:         "/api/users/actions/get-pwd-security-config",
	Scheme:       "http",
	Method:       "GET",
	Custom:       getPwdSecurityConfig,
	CheckLogin:   true,
	CheckToken:   true,
	ResponseType: apistructs.PwdSecurityConfigGetResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 查询密码安全配置",
}

func getPwdSecurityConfig(w http.ResponseWriter, r *http.Request) {
	operatorID, err := user.GetUserID(r)
	if err != nil {
		apierrors.ErrAdminUser.NotLogin().Write(w)
		return
	}

	if err := checkPermission(operatorID, apistructs.GetAction); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	token, err := auth.GetDiceClientToken()
	if err != nil {
		logrus.Errorf("failed to get token: %v", err)
		apierrors.ErrGetPwdSecurityConfig.InternalError(err).
			Write(w)
		return
	}

	// check login & permission
	_, err = mustManageUsersPerm(r, apierrors.ErrGetPwdSecurityConfig)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	config, err := handleGetPwdSecurityConfig(token)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	httpserver.WriteData(w, config)
}

func handleGetPwdSecurityConfig(token ucauth.OAuthToken) (*apistructs.PwdSecurityConfig, error) {
	var resp struct {
		Success bool                          `json:"success"`
		Result  *apistructs.PwdSecurityConfig `json:"result"`
		Error   string                        `json:"error"`
	}
	r, err := httpclient.New().Get(discover.UC()).
		Path("/api/user/admin/pwd-security-config").
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		Do().JSON(&resp)
	if err != nil {
		return nil, apierrors.ErrGetPwdSecurityConfig.InternalError(err)
	}
	if !r.IsOK() {
		return nil, apierrors.ErrGetPwdSecurityConfig.InternalError(fmt.Errorf("internal status code: %v", r.StatusCode()))
	}
	if !resp.Success {
		return nil, apierrors.ErrGetPwdSecurityConfig.InternalError(errors.New(resp.Error))
	}
	return resp.Result, nil
}

// checkPermission 检查权限
func checkPermission(userID user.ID, action string) error {
	permissionResult, err := bundle.New(bundle.WithCoreServices()).CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID.String(),
		Scope:    apistructs.SysScope,
		ScopeID:  1,
		Resource: apistructs.OrgResource,
		Action:   action,
	})
	if err != nil {
		return err
	}
	if !permissionResult.Access {
		return apierrors.ErrAdminUser.AccessDenied()
	}
	return nil
}
