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

package ucauth

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/jsonstore"
)

type TokenClient struct {
	ID         int    `json:"id"`
	ClientID   string `json:"clientId"`
	ClientName string `json:"clientName"`
}

type UCTokenAuth struct {
	UCHost       string
	ClientID     string // server端的clientID
	ClientSecret string // server端的client secret

	// server token
	serverToken           *OAuthToken
	serverTokenExpireTime time.Time

	// client token cache
	clientTokenCache jsonstore.JsonStore
}

/*
假设 openapi 要使用第三方client token验证
这里 openapi 是 server
第三方程序是 client

1. openapi先获取token (servertoken)
2. 创建client (NewClient)
3. 根据创建的client生成 clienttoken

*/
func NewUCTokenAuth(UCHost, ClientID, ClientSecret string) (*UCTokenAuth, error) {
	clientTokenCache, err := jsonstore.New(jsonstore.UseMemStore(), jsonstore.UseTimeoutStore(60))
	if err != nil {
		return nil, err
	}
	return &UCTokenAuth{
		UCHost:           UCHost,
		ClientID:         ClientID,
		ClientSecret:     ClientSecret,
		clientTokenCache: clientTokenCache,
	}, nil
}

// ExpireServerToken 使 serverToken 过期
func (a *UCTokenAuth) ExpireServerToken() {
	_ = a.serverToken
	a.serverToken = nil
}

func (a *UCTokenAuth) GetServerToken(refresh bool) (OAuthToken, error) {
	if a.serverToken != nil && a.serverTokenExpireTime.After(time.Now().Add(60*time.Second)) && !refresh {
		return *a.serverToken, nil
	}
	basic := "Basic " + base64.StdEncoding.EncodeToString([]byte(a.ClientID+":"+a.ClientSecret))
	oauthToken, err := GenClientToken(a.UCHost, basic)
	if err != nil {
		return OAuthToken{}, err
	}
	a.serverToken = &oauthToken
	a.serverTokenExpireTime = time.Now().Add(time.Duration(oauthToken.ExpiresIn) * time.Second)
	return oauthToken, nil
}

// @return example:
// {"id":7,"userId":null,"clientId":"dice-test","clientName":"dice测试应用","clientLogoUrl":null,"clientSecret":null,"autoApprove":false,"scope":["public_profile","email"],"resourceIds":["shinda-maru"],"authorizedGrantTypes":["client_credentials"],"registeredRedirectUris":[],"autoApproveScopes":[],"authorities":["ROLE_CLIENT"],"accessTokenValiditySeconds":433200,"refreshTokenValiditySeconds":433200,"additionalInformation":{}}
func (a *UCTokenAuth) Auth(token string) (TokenClient, error) {
	if !strings.HasPrefix(token, "Bearer ") {
		token = "Bearer " + token
	}
	var result TokenClient
	if err := a.clientTokenCache.Get(context.Background(), token, &result); err != nil {
		var body bytes.Buffer
		r, err := httpclient.New(httpclient.WithCompleteRedirect()).
			Get(a.UCHost).
			Path("/api/open-client/authorization").
			Header("Authorization", token).Do().Body(&body)
		if err != nil {
			return TokenClient{}, err
		}
		if !r.IsOK() {
			return TokenClient{}, fmt.Errorf("auth token: statuscode: %d, body: %v", r.StatusCode(), body.String())
		}
		d := json.NewDecoder(&body)
		if err := d.Decode(&result); err != nil {
			err := fmt.Errorf("Auth: %v, buffered: %v", err, d.Buffered())
			return TokenClient{}, err
		}
		if err := a.clientTokenCache.Put(context.Background(), token, result); err != nil {
			return TokenClient{}, err
		}
		return result, nil
	}
	return result, nil
}

func GenClientToken(uchost, basic string) (OAuthToken, error) {
	formBody := make(url.Values)
	formBody.Set("grant_type", "client_credentials")
	var body bytes.Buffer
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Post(uchost).
		Path("/oauth/token").
		Header("Authorization", basic).
		FormBody(formBody).Do().Body(&body)
	if err != nil {
		err = errors.Wrap(err, "GenClientToken")
		return OAuthToken{}, err
	}
	if !r.IsOK() {
		err := fmt.Errorf("GenClientToken: statuscode: %d, body: %v", r.StatusCode(), body.String())
		return OAuthToken{}, err
	}

	var oauthToken OAuthToken
	d := json.NewDecoder(&body)
	if err := d.Decode(&oauthToken); err != nil {
		err := fmt.Errorf("GenClientToken: %v, buffered: %+v", err, d.Buffered())
		return OAuthToken{}, err
	}
	return oauthToken, nil
}

/*
{
"accessTokenValiditySeconds": 433200, "autoApprove": false,
"clientId": "testId",
"clientLogoUrl": "http://123.com ", "clientName": "测试应用", "clientSecret": "secret", "refreshTokenValiditySeconds": 433200, "userId": 1
}
*/
type NewClientRequest struct {
	AccessTokenValiditySeconds  int64           `json:"accessTokenValiditySeconds"`
	AutoApprove                 bool            `json:"autoApprove"`
	ClientID                    string          `json:"clientId"`
	ClientLogoUrl               string          `json:"clientLogoUrl"`
	ClientName                  string          `json:"clientName"`
	ClientSecret                string          `json:"clientSecret"`
	RefreshTokenValiditySeconds int64           `json:"refreshTokenValiditySeconds"`
	UserID                      json.RawMessage `json:"userId"`
}

// {"access_token":"xxx","token_type":"bearer","refresh_token":"","expires_in":433199,"scope":"public_profile email","jti":"xxx"}
type NewClientResponse struct {
	AccessToken  string `json:"access_token"`
	TokenType    string `json:"token_type"`
	RefreshToken string `json:"refresh_token"`
	ExpiresIn    int64  `json:"expires_in"`
	Scope        string `json:"scope"`
	Jti          string `json:"jti"`
}

func (a *UCTokenAuth) NewClient(req *NewClientRequest) (*NewClientResponse, error) {
	serverToken, err := a.GetServerToken(false)
	if err != nil {
		return nil, err
	}
	var body bytes.Buffer
	r, err := httpclient.New(httpclient.WithCompleteRedirect()).
		Post(a.UCHost).
		Path("/api/open-client/manager/client").
		Header("Authorization", "Bearer "+serverToken.AccessToken).
		Header("Content-Type", "application/json").
		JSONBody(req).Do().Body(&body)
	if err != nil {
		return nil, err
	}
	if !r.IsOK() {
		// 401 时将 serverToken 置空(防止因 uc 升级 serverToken 格式不兼容, 导致无法失效)
		if r.StatusCode() == http.StatusUnauthorized {
			_ = a.serverToken
			a.serverToken = nil
		}
		err := fmt.Errorf("new client fail, statuscode: %d, body: %v", r.StatusCode(), body.String())
		return nil, err
	}
	var res NewClientResponse
	d := json.NewDecoder(&body)
	if err := d.Decode(&res); err != nil {
		err := fmt.Errorf("new client decode fail: %v, buffered: %v", err, d.Buffered())
		return nil, err
	}
	return &res, nil
}
