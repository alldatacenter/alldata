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
	"fmt"
	"net/http"
	"net/textproto"
	"strconv"
	"time"

	"github.com/go-redis/redis"
	"github.com/pkg/errors"
	uuid "github.com/satori/go.uuid"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

type GetUserState int
type SetUserState int

const (
	GetInit GetUserState = iota
	GotSessionID
	GotToken
	GotInfo
	GotScopeInfo
)

const (
	SetInit SetUserState = iota
	SetSessionID
)

const (
	SessionExpireDays = 5
)

var (
	ErrNotExist = errors.New("session not exist")
)

type ScopeInfo struct {
	OrgID uint64 `json:"orgId"`
	// dont care other fields
}

type User struct {
	sessionID  string
	token      ucauth.OAuthToken
	info       ucauth.UserInfo
	scopeInfo  ScopeInfo
	state      GetUserState
	redisCli   *redis.Client
	ucUserAuth *ucauth.UCUserAuth

	bundle *bundle.Bundle
}

var client = bundle.New(bundle.WithCoreServices(), bundle.WithDOP())

func NewUser(redisCli *redis.Client) *User {
	ucUserAuth := ucauth.NewUCUserAuth(conf.UCAddrFront(), discover.UC(), "http://"+conf.UCRedirectHost()+"/logincb", conf.UCClientID(), conf.UCClientSecret())
	if conf.OryEnabled() {
		ucUserAuth.ClientID = conf.OryCompatibleClientID()
		ucUserAuth.UCHost = conf.OryKratosAddr()
	}
	return &User{state: GetInit, redisCli: redisCli, ucUserAuth: ucUserAuth, bundle: client}
}

func (u *User) get(req *http.Request, state GetUserState) (interface{}, AuthResult) {
	switch u.state {
	case GetInit:
		session := req.Context().Value("session")
		if session == nil {
			return nil, AuthResult{Unauthed, "User:GetInit"}
		}
		u.sessionID = session.(string)
		u.state = GotSessionID
		fallthrough
	case GotSessionID:
		token, err := u.redisCli.Get(MkSessionKey(u.sessionID)).Result()
		if conf.OryEnabled() {
			// TODO: remove useless `token`
			token = u.sessionID
			err = nil
		}
		if err == redis.Nil {
			return nil, AuthResult{AuthFail,
				errors.Wrap(ErrNotExist, "User:GetInfo:GotSessionID:not exist: "+u.sessionID).Error()}
		} else if err != nil {
			return nil, AuthResult{InternalAuthErr,
				errors.Wrap(err, "User:GetInfo:GotSessionID").Error()}
		}
		u.token = ucauth.OAuthToken{AccessToken: token}
		u.state = GotToken
		fallthrough
	case GotToken:
		if state == GotToken {
			return nil, AuthResult{AuthSucc, ""}
		}
		cookieExtract := req.Header[textproto.CanonicalMIMEHeaderKey("cookie")]
		var info ucauth.UserInfo
		var err error
		//useToken, _ := strconv.ParseBool(req.Header.Get("USE-TOKEN"))
		// if len(cookieExtract) > 0 && !useToken {
		cookie := map[string][]string{"cookie": cookieExtract}
		user, err := u.ucUserAuth.GetCurrentUser(cookie)
		// } else {
		info, err = u.ucUserAuth.GetUserInfo(u.token)
		info.LastLoginAt = user.LastLoginAt
		// }
		if err != nil {
			return nil, AuthResult{Unauthed, err.Error()}
		}
		info.Token = u.token.AccessToken
		u.info = info
		u.state = GotInfo
		fallthrough
	case GotInfo:
		if state == GotInfo {
			return u.info, AuthResult{AuthSucc, ""}
		}
		// 1. 如果 request.Header 中存在 'ORG', 直接使用它作为 OrgID
		// 2. 否则 使用 request.Host 来查询 OrgID
		orgHeader := req.Header.Get("ORG")
		var orgID uint64
		var noOrgID bool
		if orgHeader != "" && orgHeader != "-" {
			org, err := u.bundle.GetOrg(orgHeader)
			if err != nil {
				return nil, AuthResult{InternalAuthErr, err.Error()}
			}
			orgID = org.ID
		} else {
			domain := strutil.Split(req.Host, ":")[0]
			org, err := u.bundle.GetDopOrgByDomain(domain, string(u.info.ID))
			if err != nil {
				return nil, AuthResult{InternalAuthErr, err.Error()}
			} else if org == nil {
				noOrgID = true
			} else {
				orgID = org.ID
			}
		}
		if !noOrgID {
			role, err := u.bundle.ScopeRoleAccess(string(u.info.ID), &apistructs.ScopeRoleAccessRequest{
				Scope: apistructs.Scope{
					Type: apistructs.OrgScope,
					ID:   strconv.FormatUint(orgID, 10),
				},
			})
			if err != nil {
				return nil, AuthResult{InternalAuthErr, err.Error()}
			}
			if !role.Access {
				return nil, AuthResult{AuthFail, fmt.Sprintf("access denied: userID: %v, orgID: %v", u.info.ID, orgID)}
			}
			var scopeinfo ScopeInfo
			scopeinfo.OrgID = orgID
			u.scopeInfo = scopeinfo
		}
		u.state = GotScopeInfo
		fallthrough
	case GotScopeInfo:
		if state == GotScopeInfo {
			return u.scopeInfo, AuthResult{AuthSucc, ""}
		}
	}
	panic("unreachable")
}

func (u *User) IsLogin(req *http.Request) AuthResult {
	_, authr := u.get(req, GotToken)
	return authr
}

// 获取用户信息
func (u *User) GetInfo(req *http.Request) (ucauth.UserInfo, AuthResult) {
	info, authr := u.get(req, GotInfo)
	if authr.Code != AuthSucc {
		return ucauth.UserInfo{}, authr
	}
	return info.(ucauth.UserInfo), authr
}

// 获取用户orgID
func (u *User) GetScopeInfo(req *http.Request) (ScopeInfo, AuthResult) {
	scopeinfo, authr := u.get(req, GotScopeInfo)
	if authr.Code != AuthSucc {
		return ScopeInfo{}, authr
	}
	return scopeinfo.(ScopeInfo), authr
}

// return (token, expiredays, err)
func (u *User) Login(uccode string, redirectURI string) (string, int, error) {
	u.ucUserAuth.RedirectURI = redirectURI
	otoken, err := u.ucUserAuth.Login(uccode)
	if err != nil {
		logrus.Error(err)
		return "", SessionExpireDays, err
	}
	u.token = otoken
	userInfo, err := u.ucUserAuth.GetUserInfo(u.token)
	if err != nil {
		return "", SessionExpireDays, err
	}
	u.info = userInfo
	u.state = GotInfo
	if err := u.storeSession(otoken.AccessToken); err != nil {
		err_ := errors.Wrap(err, "login: storeSession fail")
		logrus.Error(err_)
		return "", SessionExpireDays, err_
	}
	return u.sessionID, SessionExpireDays, nil
}

func (u *User) PwdLogin(username, password string) (string, error) {
	otoken, err := u.ucUserAuth.PwdAuth(username, password)
	if err != nil {
		logrus.Error(err)
		return "", err
	}
	u.token = otoken
	userInfo, err := u.ucUserAuth.GetUserInfo(u.token)
	if err != nil {
		return "", err
	}
	u.info = userInfo
	u.state = GotInfo
	if err := u.storeSession(otoken.AccessToken); err != nil {
		err_ := errors.Wrap(err, "pwdlogin: storeSession fail")
		logrus.Error(err_)
		return "", err_
	}
	return u.sessionID, nil
}

func (u *User) storeSession(token string) error {
	u.sessionID = genSessionID()
	_, err := u.redisCli.Set(MkSessionKey(u.sessionID), token, SessionExpireDays*24*time.Hour).Result()
	if err != nil {
		err_ := errors.Wrap(err, "storeSession: store redis fail")
		return err_
	}
	return nil
}

func (u *User) Logout(req *http.Request) error {
	c := req.Context().Value("session")
	if c == nil {
		return fmt.Errorf("not provide session")
	}
	if _, err := u.redisCli.Del(MkSessionKey(c.(string))).Result(); err != nil {
		return err
	}
	return nil
}

func MkSessionKey(sessionID string) string {
	return "openapi:sessionid:" + sessionID
}

func genSessionID() string {
	return uuid.NewV4().String()
}
