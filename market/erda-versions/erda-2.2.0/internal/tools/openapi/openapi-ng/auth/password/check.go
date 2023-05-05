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

package password

import (
	"encoding/base64"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	openapiauth "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
)

func (p *provider) Weight() int64 { return p.Cfg.Weight }

func (p *provider) Match(r *http.Request, opts openapiauth.Options) (bool, interface{}) {
	check, _ := opts.Get("CheckBasicAuth").(bool)
	if check {
		if authorization := r.Header.Get("Authorization"); strings.HasPrefix(authorization, "Basic ") {
			return true, strings.TrimSpace(authorization[len("Basic "):])
		}
	}
	return false, nil
}

func (p *provider) Check(r *http.Request, data interface{}, opts openapiauth.Options) (bool, *http.Request, error) {
	authorization := data.(string)
	userpwd, err := base64.StdEncoding.DecodeString(authorization)
	if err != nil {
		return false, r, fmt.Errorf("failed to decode base64: %v", err)
	}
	parts := strings.SplitN(string(userpwd), ":", 2)
	if len(parts) != 2 || len(parts[0]) <= 0 || len(parts[1]) <= 0 {
		return false, r, fmt.Errorf("miss username or password")
	}
	user := auth.NewUser(p.Redis)
	_, err = user.PwdLogin(parts[0], parts[1])
	if err != nil {
		return false, r, nil
	}
	result := setUserInfoHeaders(r, user)
	if result.Code != auth.AuthSucc {
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
