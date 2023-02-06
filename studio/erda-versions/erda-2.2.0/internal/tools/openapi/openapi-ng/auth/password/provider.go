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
	"encoding/json"
	"fmt"
	"net/http"
	"strings"

	"github.com/go-redis/redis"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng"
	openapiauth "github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/common"
	"github.com/erda-project/erda/pkg/ucauth"
)

type config struct {
	Weight int64 `file:"weight" default:"50"`
}

// +provider
type provider struct {
	Cfg    *config
	Log    logs.Logger
	Router openapi.Interface `autowired:"openapi-router"`
	Redis  *redis.Client     `autowired:"redis-client"`
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.Router.Add(http.MethodPost, "/login", p.Login)
	return nil
}

func (p *provider) Login(rw http.ResponseWriter, r *http.Request) {
	var params struct {
		Username string `json:"username"`
		Password string `json:"password"`
	}

	switch contentType := strings.ToLower(r.Header.Get("content-type")); {
	case strings.HasPrefix(contentType, "application/json"):
		if err := json.NewDecoder(r.Body).Decode(&params); err != nil {
			http.Error(rw, err.Error(), http.StatusUnauthorized)
			return
		}
	default:
		r.ParseForm()
		params.Username = r.FormValue("username")
		params.Password = r.FormValue("password")
	}

	if params.Username == "" || params.Password == "" {
		http.Error(rw, "username or password is required", http.StatusUnauthorized)
		return
	}

	user := auth.NewUser(p.Redis)
	sessionID, err := user.PwdLogin(params.Username, params.Password)
	if err != nil {
		err := fmt.Errorf("failed to PwdLogin: %v", err)
		p.Log.Error(err)
		http.Error(rw, err.Error(), http.StatusUnauthorized)
		return
	}
	info, authr := user.GetInfo(nil)
	if authr.Code != auth.AuthSucc {
		err := fmt.Errorf("failed to GetInfo: %v", authr.Detail)
		p.Log.Error(err)
		http.Error(rw, err.Error(), authr.Code)
		return
	}
	common.ResponseJSON(rw, &struct {
		SessionID string `json:"sessionid"`
		ucauth.UserInfo
	}{
		SessionID: sessionID,
		UserInfo:  info,
	})
}

var _ openapiauth.AutherLister = (*provider)(nil)

func (p *provider) Authers() []openapiauth.Auther {
	return []openapiauth.Auther{p}
}

func init() {
	servicehub.Register("openapi-auth-password", &servicehub.Spec{
		Services:     []string{"openapi-auth-password"},
		Dependencies: []string{"openapi-auth-session"}, // to check session
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
