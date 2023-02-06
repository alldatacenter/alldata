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

var UC_PWD_SECURITY_CONFIG_UPDATE = apis.ApiSpec{
	Path:         "/api/users/actions/update-pwd-security-config",
	Scheme:       "http",
	Method:       "POST",
	Custom:       updatePwdSecurityConfig,
	CheckLogin:   true,
	CheckToken:   true,
	RequestType:  apistructs.PwdSecurityConfigUpdateRequest{},
	ResponseType: apistructs.PwdSecurityConfigUpdateResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 更新密码安全配置",
}

func updatePwdSecurityConfig(w http.ResponseWriter, r *http.Request) {
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
		apierrors.ErrUpdatePwdSecurityConfig.InternalError(err).
			Write(w)
		return
	}

	// check login & permission
	_, err = mustManageUsersPerm(r, apierrors.ErrUpdatePwdSecurityConfig)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	var config apistructs.PwdSecurityConfig
	if err := json.NewDecoder(r.Body).Decode(&config); err != nil {
		logrus.Warnf("failed to decode body when update pwdSecurityConfig, (%v)", err)
		apierrors.ErrUpdatePwdSecurityConfig.InvalidParameter(err.Error()).
			Write(w)
		return
	}
	if err := handleUpdatePwdSecurityConfig(&config, token); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	httpserver.WriteData(w, nil)
}

func handleUpdatePwdSecurityConfig(config *apistructs.PwdSecurityConfig, token ucauth.OAuthToken) error {
	var resp struct {
		Success bool   `json:"success"`
		Error   string `json:"error"`
	}
	r, err := httpclient.New().Post(discover.UC()).
		Path("/api/user/admin/pwd-security-config").
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		JSONBody(config).
		Do().JSON(&resp)
	if err != nil {
		return apierrors.ErrUpdatePwdSecurityConfig.InternalError(err)
	}
	if !r.IsOK() {
		return apierrors.ErrUpdatePwdSecurityConfig.InternalError(fmt.Errorf("internal status code: %v", r.StatusCode()))
	}
	if !resp.Success {
		return apierrors.ErrUpdatePwdSecurityConfig.InternalError(errors.New(resp.Error))
	}
	return nil
}
