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

package legacy

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/generate/auto_register"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/hooks"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/hooks/prehandle"
	"github.com/erda-project/erda/pkg/oauth2"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

type LoginServer struct {
	r    http.Handler
	auth *auth.Auth

	oauth2server *oauth2.OAuth2Server
}

func NewLoginServer(token tokenpb.TokenServiceServer) (*LoginServer, error) {
	oauth2server := oauth2.NewOAuth2Server()
	auth, err := auth.NewAuth(oauth2server, token)
	if err != nil {
		return nil, err
	}
	bdl := bundle.New(
		bundle.WithCoreServices(),
		bundle.WithDOP(),
		bundle.WithPipeline(),
		bundle.WithDiceHub(),
		bundle.WithMonitor(),
		bundle.WithTMC(),
	)
	auto_register.RegisterAll()
	h, err := NewReverseProxyWithAuth(auth, bdl)
	if err != nil {
		return nil, err
	}
	return &LoginServer{r: h, auth: auth, oauth2server: oauth2server}, nil
}

func (s *LoginServer) ServeHTTP(rw http.ResponseWriter, req *http.Request) {
	if hooks.Enable {
		prehandle.FilterHeader(context.Background(), rw, req)
		prehandle.ReplaceOldCookie(context.Background(), rw, req)
		if err := prehandle.CSRFToken(context.Background(), rw, req); err != nil {
			logrus.Errorf("CSRFToken: %v", err)
			return
		}
	}
	prehandle.FilterCookie(context.Background(), rw, req) // for auth

	// TODO: move there to api/apis/
	if req.Method == "GET" && req.URL.Path == "/api/openapi/login" {
		s.Login(rw, req)
	} else if req.URL.Path == "/api/openapi/logincb" || req.URL.Path == "/logincb" {
		s.LoginCB(rw, req)
	} else if req.Method == "POST" && req.URL.Path == "/login" {
		s.PwdLogin(rw, req)
	} else if req.Method == "POST" && (req.URL.Path == "/api/openapi/logout" || req.URL.Path == "/logout") {
		s.Logout(rw, req)
	} else if req.Method == "GET" && (req.URL.Path == "/api/users/me" || req.URL.Path == "/me") {
		s.UserInfo(rw, req)
	} else if strutil.HasPrefixes(req.URL.Path, "/oauth2") {
		switch req.URL.Path {
		case "/oauth2/token":
			s.oauth2server.Token(rw, req)
		case "/oauth2/invalidate_token":
			s.oauth2server.InvalidateToken(rw, req)
		case "/oauth2/validate_token":
			s.oauth2server.ValidateToken(rw, req)
		default:
			errStr := fmt.Sprintf("not found path: %v", req.URL)
			logrus.Error(errStr)
			http.Error(rw, errStr, 404)
			return
		}
	} else {
		s.r.ServeHTTP(rw, req)
	}
}

// http://uc.app.terminus.io/oauth/authorize?response_type=code&client_id=dice&redirect_uri=http%3A%2F%2Fopenapi.test.terminus.io%2Flogincb%26other_params=xxx&scope=public_profile&
func (s *LoginServer) Login(rw http.ResponseWriter, req *http.Request) {
	rw.Header().Set("Content-Type", "application/json")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	referer := req.Header.Get("Referer")
	if referer == "" {
		redirectURL := conf.RedirectAfterLogin()
		if !strutil.HasPrefixes(redirectURL, "//") {
			redirectURL = "//" + redirectURL
		}
		referer = redirectURL
	}
	rw.WriteHeader(http.StatusOK)
	var u struct {
		URL string `json:"url"`
	}
	u.URL = fmt.Sprintf("https://%s/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=public_profile",
		conf.UCAddrFront(),
		conf.UCClientID(),
		url.QueryEscape("https://"+conf.GetUCRedirectHost(referer)+"/logincb?referer="+url.QueryEscape(referer)))
	if conf.OryEnabled() {
		// TODO return-back page in context (mostly the referer)
		u.URL = conf.OryLoginURL()
	}
	// replace HTTP(s)
	isHTTPS, err := IsHTTPS(req)
	if err != nil {
		logrus.Errorf("Login: no Referer Header in request")
	}
	u.URL = replaceProto(isHTTPS, u.URL)
	var content bytes.Buffer
	d := json.NewEncoder(&content)
	if err := d.Encode(u); err != nil {
		logrus.Error(err)
		http.Error(rw, err.Error(), http.StatusBadGateway)
		return
	}
	rw.Write([]byte(content.String()))
}

func (s *LoginServer) LoginCB(rw http.ResponseWriter, req *http.Request) {
	code := req.URL.Query().Get("code")
	referer := req.URL.Query().Get("referer")
	user := auth.NewUser(s.auth.RedisCli)
	isHTTPS, err := IsHTTPS(req)
	if err != nil {
		logrus.Errorf("LoginCB: no Referer Header in request")
		isHTTPS = true
	}
	redirectURI := "https://" + conf.GetUCRedirectHost(referer) + "/logincb?referer=" + referer
	redirectURI = replaceProto(isHTTPS, redirectURI)
	sessionID, _, err := user.Login(code, redirectURI)
	if err != nil {
		logrus.Errorf("login fail: %v", err)
		http.Error(rw, err.Error(), http.StatusUnauthorized)
		return
	}
	fmt.Printf("%+v\n", conf.CookieDomain()) // debug print
	reqDomain, err := conf.GetDomain(req.Host, conf.CookieDomain())
	if err != nil {
		http.Error(rw, err.Error(), http.StatusUnauthorized)
		return
	}
	http.SetCookie(rw, &http.Cookie{
		Name:     conf.SessionCookieName(),
		Value:    sessionID,
		Domain:   reqDomain,
		HttpOnly: true,
		Secure:   strutil.Contains(conf.DiceProtocol(), "https"),
	})
	refererUnescaped, err := url.QueryUnescape(referer)
	if err != nil {
		http.Error(rw, err.Error(), http.StatusInternalServerError)
	}
	http.Redirect(rw, req, refererUnescaped, http.StatusFound)
}

type PwdLoginRequest struct {
	Username string `json:"username"`
	Password string `json:"password"`
}

type PwdLoginResponse struct {
	SessionID string `json:"sessionid"`
	ucauth.UserInfo
}

func (s *LoginServer) PwdLogin(rw http.ResponseWriter, req *http.Request) {
	var request PwdLoginRequest
	switch contentType := req.Header.Get("content-type"); strings.ToLower(contentType) {
	case "application/json":
		if err := json.NewDecoder(req.Body).Decode(&request); err != nil {
			logrus.Errorf("failed to Decode request body, err: %v", err)
			http.Error(rw, "invalid request body", http.StatusUnauthorized)
			return
		}
	default:
		request.Username = req.FormValue("username")
		request.Password = req.FormValue("password")
	}

	if request.Username == "" || request.Password == "" {
		errStr := "empty username or passwd"
		logrus.Error(errStr)
		http.Error(rw, errStr, http.StatusUnauthorized)
		return
	}
	user := auth.NewUser(s.auth.RedisCli)
	sessionID, err := user.PwdLogin(request.Username, request.Password)
	if err != nil {
		errStr := fmt.Sprintf("pwdlogin fail: %v", err)
		logrus.Error(errStr)
		http.Error(rw, errStr, http.StatusUnauthorized)
		return
	}
	info, authr := user.GetInfo(nil)
	if authr.Code != auth.AuthSucc {
		errStr := fmt.Sprintf("pwdlogin getInfo fail: %v", authr.Detail)
		logrus.Error(errStr)
		http.Error(rw, errStr, authr.Code)
		return
	}
	rw.WriteHeader(http.StatusOK)
	res := PwdLoginResponse{
		SessionID: sessionID,
		UserInfo:  info,
	}
	resJson, err := json.Marshal(res)
	if err != nil {
		errStr := fmt.Sprintf("marshal PwdLoginResponse fail: %v", err)
		logrus.Error(errStr)
		http.Error(rw, errStr, http.StatusUnauthorized)
		return
	}
	rw.Write(resJson)
}

func (s *LoginServer) Logout(rw http.ResponseWriter, req *http.Request) {
	referer := req.Header.Get("Referer")
	if referer == "" {
		redirectURL := conf.RedirectAfterLogin()
		if !strutil.HasPrefixes(redirectURL, "//") {
			redirectURL = "//" + redirectURL
		}
		referer = redirectURL
	}

	if conf.OryEnabled() {
		// no need to delete cookie
	} else {
		user := auth.NewUser(s.auth.RedisCli)
		if err := user.Logout(req); err != nil {
			errStr := fmt.Sprintf("logout: %v", err)
			logrus.Error(errStr)
			http.Error(rw, errStr, http.StatusBadGateway)
			return
		}
		reqDomain, err := conf.GetDomain(req.Host, conf.CookieDomain())
		if err != nil {
			http.Error(rw, err.Error(), http.StatusUnauthorized)
			return
		}
		http.SetCookie(rw, &http.Cookie{Name: conf.SessionCookieName(), Value: "", Path: "/", Expires: time.Unix(0, 0), MaxAge: -1, Domain: reqDomain, HttpOnly: true, Secure: strutil.Contains(conf.DiceProtocol(), "https")})
	}

	var v struct {
		URL string `json:"url"`
	}
	v.URL = "https://" + conf.UCAddrFront() + "/logout?redirectUrl=" + url.QueryEscape(fmt.Sprintf("https://%s/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=public_profile", conf.UCAddrFront(), conf.UCClientID(), url.QueryEscape("https://"+conf.GetUCRedirectHost(referer)+"/logincb?referer="+url.QueryEscape(referer))))
	if conf.OryEnabled() {
		v.URL = conf.OryLogoutURL()
	}

	isHTTPS, err := IsHTTPS(req)
	if err != nil {
		logrus.Errorf("Logout: no Referer Header in request")
	}
	v.URL = replaceProto(isHTTPS, v.URL)
	var content bytes.Buffer
	if err := json.NewEncoder(&content).Encode(v); err != nil {
		http.Error(rw, err.Error(), http.StatusBadGateway)
		return
	}
	rw.Header().Set("Content-Type", "application/json")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	rw.WriteHeader(http.StatusOK)
	rw.Write([]byte(content.String()))
}

type ApiData struct {
	Success bool        `json:"success"`
	Err     interface{} `json:"err"`
	Data    interface{} `json:"data"`
}

func (s *LoginServer) UserInfo(rw http.ResponseWriter, req *http.Request) {
	user := auth.NewUser(s.auth.RedisCli)
	logrus.Debugf("userinfo: %v", req.Cookies())
	info, authr := user.GetInfo(req)
	if authr.Code != auth.AuthSucc {
		http.Error(rw, authr.Detail, authr.Code)
		return
	}
	result := ApiData{
		Success: true,
		Data: map[string]interface{}{
			"id":     info.ID,
			"name":   info.UserName,
			"nick":   info.NickName,
			"avatar": info.AvatarUrl,
			"phone":  info.Phone,
			"email":  info.Email,
			"token":  info.Token,
		},
	}
	content, err := json.Marshal(result)
	if err != nil {
		errStr := "marshal user info fail"
		logrus.Error(errStr)
		http.Error(rw, errStr, http.StatusBadGateway)
		return
	}
	rw.Header().Set("Content-Type", "application/json")
	rw.Header().Set("Access-Control-Allow-Origin", "*")
	rw.WriteHeader(http.StatusOK)
	rw.Write(content)
}
