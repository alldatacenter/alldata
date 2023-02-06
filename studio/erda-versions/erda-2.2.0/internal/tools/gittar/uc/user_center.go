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
	"net/url"
	"strings"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/patrickmn/go-cache"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/conf"
	"github.com/erda-project/erda/pkg/desensitize"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/ucauth"
)

type Token struct {
	AccessToken string `json:"access_token"`
	ExpiresIn   int64  `json:"expires_in"`
	ExpiresAt   int64  `json:"expire_at"`
}

var tokenValue *Token

var userCache = cache.New(12*time.Hour, 1*time.Hour)

var uc *ucauth.UCClient

func InitializeUcClient(db *gorm.DB) {
	uc = ucauth.NewUCClient(conf.OryKratosPrivateAddr(), conf.OryCompatibleClientID(), conf.OryCompatibleClientSecret())
	uc.SetDBClient(db)
	logrus.Infof("gittar uc client set up")
}

func GetToken(forceRefresh bool) (string, error) {
	if tokenValue == nil || tokenValue.ExpiresAt < time.Now().Unix() || forceRefresh {
		formBody := make(url.Values)
		formBody.Set("grant_type", "client_credentials")
		var token Token
		r, err := httpclient.New(httpclient.WithCompleteRedirect()).
			BasicAuth(conf.UCClientID(), conf.UCClientSecret()).
			Post(discover.UC()).
			Path("/oauth/token").
			FormBody(formBody).
			Do().JSON(&token)
		if err != nil {
			return "", err
		}
		if !r.IsOK() {
			return "", errors.Errorf("apply token for uc failed, statusCode: %d", r.StatusCode())
		}
		token.ExpiresAt = time.Now().Unix() + token.ExpiresIn
		tokenValue = &token
	}
	logrus.Debug("uc token = ", tokenValue.AccessToken)
	return tokenValue.AccessToken, nil
}

func FindUserById(id string) (*apistructs.UserInfoDto, error) {
	cacheUser, exist := userCache.Get(id)
	if exist {
		return cacheUser.(*apistructs.UserInfoDto), nil
	}
	if id == "0" {
		return nil, nil
	}
	if conf.OryEnabled() {
		user, err := uc.GetUser(id)
		if err != nil {
			return nil, err
		}
		userInfo := &apistructs.UserInfoDto{
			AvatarURL: user.AvatarURL,
			Email:     user.Email,
			UserID:    user.ID,
			NickName:  user.Nick,
			Phone:     user.Phone,
			RealName:  user.Name,
			Username:  user.Name,
		}
		userCache.Set(id, userInfo, cache.DefaultExpiration)
		return userInfo, nil
	}
	token, err := GetToken(false)
	if err != nil {
		return nil, err
	}
	var userInfo apistructs.UserInfoDto
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).Get(discover.UC()).
		Path("/api/open/v2/users/"+id).Header("Authorization", "Bearer "+token).Do().JSON(&userInfo)
	if err != nil || !r.IsOK() {
		token, err = GetToken(true)
		if err != nil {
			return nil, err
		}
	}
	r, err = httpclient.New(httpclient.WithCompleteRedirect()).Get(discover.UC()).
		Path("/api/open/v2/users/"+id).Header("Authorization", "Bearer "+token).Do().JSON(&userInfo)
	if err != nil {
		return nil, errors.Wrapf(err, "uc: get user by id: %s failed", id)
	}
	if !r.IsOK() {
		return nil, errors.Errorf("uc: get user by id: %s failed, statusCode: %d", id, r.StatusCode())
	}
	userCache.Set(id, &userInfo, cache.DefaultExpiration)
	return &userInfo, nil
}

func FindUserByIdWithDesensitize(id string) (*apistructs.UserInfoDto, error) {
	userInfo, err := FindUserById(id)
	if err != nil {
		return nil, err
	}
	if userInfo != nil {
		// 不是用指针类型 防止更新缓存里的原始值
		result := *userInfo
		result.Email = desensitize.Email(userInfo.Email)
		result.Phone = desensitize.Mobile(userInfo.Phone)
		userInfo = &result
	}
	return userInfo, nil
}

// userIds like user_id:12345, ...
// TODO 等 uc 2.0 支持。 目前未支持
func ListUserByIds(userIds []string) ([]UserInfo, error) {
	ids := strings.Join(userIds, " OR ")
	token, err := GetToken(false)
	if err != nil {
		return nil, errors.Wrapf(err, "get uc token failed")
	}
	var userInfos []UserInfo
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).Get(discover.UC()).Path("/v1/users").
		Header("Authorization", "Bearer "+token).Param("query", ids).Do().JSON(&userInfos)
	if err != nil || !r.IsOK() {
		token, err = GetToken(true)
		if err != nil {
			return nil, errors.Wrapf(err, "get uc token failed")
		}
	}
	r, err = httpclient.New(httpclient.WithCompleteRedirect()).Get(discover.UC()).Path("/v1/users").
		Header("Authorization", "Bearer "+token).Param("query", ids).Do().JSON(&userInfos)
	if err != nil {
		return nil, errors.Wrapf(err, "uc: list user by ids: %s failed", ids)
	}
	if !r.IsOK() {
		return nil, errors.Errorf("uc: list user by ids: %s failed, statusCode: %d", ids, r.StatusCode())
	}
	return userInfos, nil
}
