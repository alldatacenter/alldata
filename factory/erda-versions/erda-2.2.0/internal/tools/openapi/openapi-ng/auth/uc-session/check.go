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

package ucoauth

import (
	"context"
	"math"
	"net/http"
	"strconv"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	openapiauth "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
)

type loginChecker struct {
	p *provider
}

func (a *loginChecker) Weight() int64 { return a.p.Cfg.Weight }

func (a *loginChecker) Match(r *http.Request, opts openapiauth.Options) (bool, interface{}) {
	check, _ := opts.Get("CheckLogin").(bool)
	if check {
		session := a.p.getSession(r)
		if len(session) > 0 {
			return true, session
		}
	}
	return false, nil
}

func (a *loginChecker) Check(r *http.Request, data interface{}, opts openapiauth.Options) (bool, *http.Request, error) {
	user := auth.NewUser(a.p.Redis)
	r = r.WithContext(context.WithValue(r.Context(), "session", data.(string)))
	result := user.IsLogin(r)
	if result.Code != auth.AuthSucc {
		a.p.Log.Debugf("failed to auth: %v", result.Detail)
		return false, r, nil
	}
	result = setUserInfoHeaders(r, user)
	if result.Code != auth.AuthSucc {
		a.p.Log.Debugf("failed to auth: %v", result.Detail)
		return false, r, nil
	}
	return true, r, nil
}

func setUserInfoHeaders(req *http.Request, user *auth.User) auth.AuthResult {
	userinfo, r := user.GetInfo(req)
	if r.Code != auth.AuthSucc {
		return r
	}
	// set User-ID
	req.Header.Set("User-ID", string(userinfo.ID))

	var scopeinfo auth.ScopeInfo
	scopeinfo, r = user.GetScopeInfo(req)
	if r.Code != auth.AuthSucc {
		return r
	}
	// set Org-ID
	if scopeinfo.OrgID != 0 {
		req.Header.Set("Org-ID", strconv.FormatUint(scopeinfo.OrgID, 10))
	}
	return auth.AuthResult{Code: auth.AuthSucc}
}

type tryLoginChecker struct {
	p *provider
}

func (a *tryLoginChecker) Weight() int64 { return math.MinInt64 }

func (a *tryLoginChecker) Match(r *http.Request, opts openapiauth.Options) (bool, interface{}) {
	check, _ := opts.Get("TryCheckLogin").(bool)
	if check {
		session := a.p.getSession(r)
		return true, session
	}
	return false, nil
}

func (a *tryLoginChecker) Check(r *http.Request, data interface{}, opts openapiauth.Options) (bool, *http.Request, error) {
	user := auth.NewUser(a.p.Redis)
	r = r.WithContext(context.WithValue(r.Context(), "session", data.(string)))
	result := user.IsLogin(r)
	if result.Code == auth.AuthSucc {
		setUserInfoHeaders(r, user)
	}
	return true, r, nil
}
