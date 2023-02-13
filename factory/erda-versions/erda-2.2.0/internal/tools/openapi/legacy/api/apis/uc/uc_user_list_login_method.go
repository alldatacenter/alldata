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
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apierrors"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

var UC_USER_LIST_LOGIN_METHOD = apis.ApiSpec{
	Path:         "/api/users/actions/list-login-method",
	Scheme:       "http",
	Method:       "GET",
	Custom:       listLoginMethod,
	CheckLogin:   true,
	CheckToken:   true,
	ResponseType: apistructs.UserListLoginMethodResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 获取当前环境支持的登录方式",
}

// todo 废弃
// 因为uc现在的登录方式实现上害不规范。之后uc会规范起来，不用source字段表示，临时先这样国际化一下
var ucLoginMethodI18nMap = map[string]map[string]string{
	"username": {"en-US": "username", "zh-CN": "账密登录", "marks": "external"},
	"sso":      {"en-US": "sso", "zh-CN": "单点登录", "marks": "internal"},
	"email":    {"en-US": "default", "zh-CN": "默认登录方式", "marks": ""},
	"mobile":   {"en-US": "default", "zh-CN": "默认登录方式", "marks": ""},
}

func getLoginTypeByUC(key string) map[string]string {
	if v, ok := ucLoginMethodI18nMap[key]; ok {
		return v
	}

	return nil
}

func listLoginMethod(w http.ResponseWriter, r *http.Request) {
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
		apierrors.ErrListLoginMethod.InternalError(err).
			Write(w)
		return
	}
	// check login & permission
	_, err = mustManageUsersPerm(r, apierrors.ErrListLoginMethod)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	// handle
	res, err := handleListLoginMethod(token)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	var source []apistructs.UserListLoginMethodData
	local := i18n.GetLocaleNameByRequest(r)
	if local == "" {
		local = "zh-CN"
	}

	deDubMap := make(map[string]string, 0)
	for _, v := range res.RegistryType {
		tmp := getLoginTypeByUC(v)
		if _, ok := deDubMap[tmp["marks"]]; ok {
			continue
		}
		source = append(source, apistructs.UserListLoginMethodData{
			DisplayName: tmp[local],
			Value:       tmp["marks"],
		})
		deDubMap[tmp["marks"]] = ""
	}

	httpserver.WriteData(w, source)
}

type listLoginTypeResult struct {
	RegistryType []string `json:"registryType"`
}

func handleListLoginMethod(token ucauth.OAuthToken) (*listLoginTypeResult, error) {
	// TODO: password oidc
	if token.TokenType == ucauth.OryCompatibleClientId {
		return &listLoginTypeResult{
			RegistryType: []string{"email"},
		}, nil
	}
	var resp struct {
		Success bool                 `json:"success"`
		Result  *listLoginTypeResult `json:"result"`
		Error   string               `json:"error"`
	}
	r, err := httpclient.New().Get(discover.UC()).
		Path("/api/home/admin/login/style").
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		Do().JSON(&resp)
	if err != nil {
		logrus.Errorf("failed to invoke list user login method, (%v)", err)
		return nil, apierrors.ErrListLoginMethod.InternalError(err)
	}
	if !r.IsOK() {
		logrus.Debugf("failed to list user login method, statusCode: %d, %v", r.StatusCode(), string(r.Body()))
		return nil, apierrors.ErrListLoginMethod.InternalError(fmt.Errorf("internal status code: %v", r.StatusCode()))
	}
	if !resp.Success {
		logrus.Debugf("failed to list user login method: %+v", resp.Error)
		return nil, apierrors.ErrListLoginMethod.InternalError(errors.New(resp.Error))
	}

	return resp.Result, nil
}
