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

var UC_USER_CREATE = apis.ApiSpec{
	Path:         "/api/users",
	Scheme:       "http",
	Method:       "POST",
	Custom:       createUsers,
	CheckLogin:   true,
	CheckToken:   true,
	RequestType:  apistructs.UserCreateRequest{},
	ResponseType: apistructs.UserCreateResponse{},
	IsOpenAPI:    true,
	Doc:          "summary: 用户创建",
}

func createUsers(w http.ResponseWriter, r *http.Request) {
	operatorID, err := user.GetUserID(r)
	if err != nil {
		apierrors.ErrAdminUser.NotLogin().Write(w)
		return
	}

	if err := checkPermission(operatorID, apistructs.CreateAction); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	token, err := auth.GetDiceClientToken()
	if err != nil {
		logrus.Errorf("failed to get token: %v", err)
		apierrors.ErrCreateUser.InternalError(err).
			Write(w)
		return
	}

	// check login & permission
	_, err = mustManageUsersPerm(r, apierrors.ErrCreateUser)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	// get req
	var items []apistructs.UserCreateItem
	if err := json.NewDecoder(r.Body).Decode(&items); err != nil {
		apierrors.ErrCreateUser.InvalidParameter(err).
			Write(w)
		return
	}
	if len(items) == 0 {
		apierrors.ErrCreateUser.InvalidParameter("no users to create").
			Write(w)
	}
	req := apistructs.UserCreateRequest{Users: items}

	// handle
	if err := handleCreateUsers(&req, operatorID.String(), token); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	httpserver.WriteData(w, nil)
}

func handleCreateUsers(req *apistructs.UserCreateRequest, operatorID string, token ucauth.OAuthToken) error {
	if token.TokenType == ucauth.OryCompatibleClientId {
		for _, u := range req.Users {
			if _, err := ucauth.CreateUser(ucauth.OryKratosCreateIdentitiyRequest{
				SchemaID: "default",
				Traits: ucauth.OryKratosIdentityTraits{
					Email: u.Email,
					Name:  u.Name,
					Nick:  u.Nick,
					Phone: u.Phone,
				},
				Credentials: ucauth.OryKratosAdminIdentityImportCredentials{
					Password: &ucauth.OryKratosAdminIdentityImportCredentialsPassword{
						Config: ucauth.OryKratosIdentityCredentialsPasswordConfig{
							Password: u.Password,
						},
					},
				},
			}); err != nil {
				return err
			}
		}
		return nil
	}
	var resp struct {
		Success bool   `json:"success"`
		Error   string `json:"error"`
	}
	users := make([]createUserItem, len(req.Users))
	for i, u := range req.Users {
		users[i] = convertCreateUserItem(&u)
	}
	reqBody := createUser{Users: users}
	r, err := httpclient.New().Post(discover.UC()).
		Path("/api/user/admin/batch-create-user").
		Header("Authorization", strutil.Concat("Bearer ", token.AccessToken)).
		Header("operatorId", operatorID).
		JSONBody(&reqBody).
		Do().JSON(&resp)
	if err != nil {
		logrus.Errorf("failed to invoke create user, (%v)", err)
		return apierrors.ErrCreateUser.InternalError(err)
	}
	if !r.IsOK() {
		logrus.Debugf("failed to create user, statusCode: %d, %v", r.StatusCode(), string(r.Body()))
		return apierrors.ErrCreateUser.InternalError(fmt.Errorf("internal status code: %v", r.StatusCode()))
	}
	if !resp.Success {
		logrus.Debugf("failed to create user: %+v", resp.Error)
		return apierrors.ErrCreateUser.InternalError(errors.New(resp.Error))
	}
	return nil
}

type createUser struct {
	Users []createUserItem `json:"users"`
}

type createUserItem struct {
	Username    string      `json:"username,omitempty"`    // 用户名
	Nickname    string      `json:"nickname,omitempty"`    // 昵称
	Mobile      string      `json:"mobile,omitempty"`      //
	Email       string      `json:"email,omitempty"`       // 邮箱
	Password    string      `json:"password"`              // 密码
	Avatar      string      `json:"avatar,omitempty"`      // 头像
	Channel     string      `json:"channel,omitempty"`     // 注册渠道
	ChannelType string      `json:"channelType,omitempty"` // 渠道类型
	Extra       interface{} `json:"extra,omitempty"`       //
	Source      string      `json:"source,omitempty"`      // 用户来源
	SourceType  string      `json:"sourceType,omitempty"`  // 来源类型
	Tag         string      `json:"tag,omitempty"`         // 标签
	UserDetail  interface{} `json:"userDetail,omitempty"`  // 用户详情
}

func convertCreateUserItem(item *apistructs.UserCreateItem) createUserItem {
	return createUserItem{
		Username: item.Name,
		Nickname: item.Nick,
		Mobile:   item.Phone,
		Email:    item.Email,
		Password: item.Password,
	}
}
