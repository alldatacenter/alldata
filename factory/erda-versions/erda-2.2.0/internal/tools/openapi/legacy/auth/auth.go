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

package auth

import (
	"encoding/base64"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/go-redis/redis"
	"github.com/pkg/errors"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/spec"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/monitor"
	"github.com/erda-project/erda/pkg/oauth2"
	"github.com/erda-project/erda/pkg/ucauth"
)

type AuthResult struct {
	Code   int
	Detail string
}

type TokenClient struct {
	ClientID   string
	ClientName string
}

const (
	Unauthed        = http.StatusUnauthorized
	AuthFail        = http.StatusForbidden
	InternalAuthErr = http.StatusInternalServerError
	AuthSucc        = http.StatusOK
)

const (
	HeaderAuthorization             = "Authorization"
	HeaderAuthorizationBearerPrefix = "Bearer "
)

type Auth struct {
	RedisCli     *redis.Client
	OAuth2Server *oauth2.OAuth2Server
	TokenService tokenpb.TokenServiceServer
}

func NewAuth(oauth2server *oauth2.OAuth2Server, token tokenpb.TokenServiceServer) (*Auth, error) {
	sentinelAddrs := strings.Split(conf.RedisSentinelAddrs(), ",")
	RedisCli := redis.NewFailoverClient(&redis.FailoverOptions{
		MasterName:    conf.RedisMasterName(),
		SentinelAddrs: sentinelAddrs,
		Password:      conf.RedisPwd(),
	})
	if _, err := RedisCli.Ping().Result(); err != nil {
		return nil, err
	}
	return &Auth{RedisCli: RedisCli, OAuth2Server: oauth2server, TokenService: token}, nil
}

func (a *Auth) Auth(spec *spec.Spec, req *http.Request) AuthResult {
	r := AuthResult{AuthSucc, ""}
	defer func() {
		if r.Code != AuthSucc {
			monitor.Notify(monitor.Info{
				Tp: monitor.AuthFail, Detail: r.Detail,
			})
		}
		monitor.Notify(monitor.Info{
			Tp: monitor.AuthSucc, Detail: "",
		})
	}()
	var t checkType
	t, err := whichCheck(req, spec)
	if err != nil {
		return AuthResult{Unauthed, err.Error()}
	}
	switch t {
	case NONE:
		break
	case LOGIN:
		user := NewUser(a.RedisCli)
		if r = a.checkLogin(req, user, spec); r.Code != AuthSucc {
			return r
		}
		if r = setUserInfoHeaders(req, user); r.Code != AuthSucc {
			return r
		}
	case TRY_LOGIN:
		user := NewUser(a.RedisCli)
		if r := a.checkLogin(req, user, spec); r.Code != AuthSucc {
			break
		}
		if r := setUserInfoHeaders(req, user); r.Code != AuthSucc {
			break
		}
	case BASICAUTH:
		user := NewUser(a.RedisCli)
		if r = a.checkBasicAuth(req, user); r.Code != AuthSucc {
			return r
		}
		if r = setUserInfoHeaders(req, user); r.Code != AuthSucc {
			return r
		}

	case TOKEN:
		var client TokenClient
		client, r = a.checkToken(spec, req)
		if r.Code != AuthSucc {
			return r
		}
		req.Header.Set("Client-ID", client.ClientID)
		req.Header.Set("Client-Name", client.ClientName)
	}
	return r
}

func setUserInfoHeaders(req *http.Request, user *User) AuthResult {
	var userinfo ucauth.UserInfo
	r := AuthResult{AuthSucc, ""}
	userinfo, r = user.GetInfo(req)
	if r.Code != AuthSucc {
		return r
	}
	// set User-ID
	req.Header.Set("User-ID", string(userinfo.ID))
	if _, err := req.Cookie(conf.SessionCookieName()); err != nil {
		req.AddCookie(&http.Cookie{Name: conf.SessionCookieName(), Value: user.sessionID})
	}

	var scopeinfo ScopeInfo
	scopeinfo, r = user.GetScopeInfo(req)
	if r.Code != AuthSucc {
		return r
	}
	// set Org-ID
	if scopeinfo.OrgID != 0 {
		req.Header.Set("Org-ID", strconv.FormatUint(scopeinfo.OrgID, 10))
	}
	return r
}

type checkType int

const (
	LOGIN checkType = iota
	TRY_LOGIN
	BASICAUTH
	TOKEN
	NONE
)

func whichCheck(req *http.Request, spec *spec.Spec) (checkType, error) {
	session := req.Context().Value("session")
	if spec.CheckLogin && session != nil {
		return LOGIN, nil
	}
	auth := req.Header.Get("Authorization")
	if spec.CheckBasicAuth && strings.HasPrefix(auth, "Basic ") {
		return BASICAUTH, nil
	}
	if spec.CheckToken && auth != "" {
		return TOKEN, nil
	}
	if spec.TryCheckLogin {
		return TRY_LOGIN, nil
	}
	if !spec.CheckToken && !spec.CheckLogin {
		return NONE, nil
	}
	return NONE, errors.New("lack of required auth header")
}

func (a *Auth) checkLogin(req *http.Request, user *User, spec *spec.Spec) AuthResult {
	return user.IsLogin(req)
}

// checkToken try:
// 1. uc token
// 2. openapi oauth2 token
// 3. access key
func (a *Auth) checkToken(spec *spec.Spec, req *http.Request) (TokenClient, AuthResult) {
	// 1. uc token
	ucTC, err := VerifyUCClientToken(req.Header.Get(HeaderAuthorization))
	if err == nil {
		return TokenClient{
			ClientID:   ucTC.ClientID,
			ClientName: ucTC.ClientName,
		}, AuthResult{AuthSucc, ""}
	}
	// 2. openapi oauth2 token
	oauth2TC, err := VerifyOpenapiOAuth2Token(a.OAuth2Server, &OpenapiSpec{Spec: spec}, req)
	if err == nil {
		return oauth2TC, AuthResult{AuthSucc, ""}
	}
	// 3. access key
	accesskey, err := VerifyAccessKey(a.TokenService, req)
	if err != nil {
		return TokenClient{}, AuthResult{AuthFail, err.Error()}
	}
	return accesskey, AuthResult{AuthSucc, ""}
}

func (a *Auth) checkBasicAuth(req *http.Request, user *User) AuthResult {
	auth := req.Header.Get("Authorization")
	auth = strings.TrimPrefix(auth, "Basic ")
	userNameAndPwd, err := base64.StdEncoding.DecodeString(auth)
	if err != nil {
		return AuthResult{AuthFail,
			fmt.Sprintf("checkBasicAuth: decode base64 fail: %v", err)}
	}
	splitted := strings.SplitN(string(userNameAndPwd), ":", 2)
	if len(splitted) != 2 {
		return AuthResult{AuthFail,
			fmt.Sprintf("checkBasicAuth: split username and password fail: %v", userNameAndPwd)}
	}
	_, err = user.PwdLogin(splitted[0], splitted[1])
	if err != nil {
		return AuthResult{Unauthed, err.Error()}
	}
	return AuthResult{AuthFail, err.Error()}
}
