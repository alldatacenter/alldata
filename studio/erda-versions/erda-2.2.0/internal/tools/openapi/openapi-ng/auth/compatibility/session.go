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

package compatibility

import (
	"net/http"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors"
)

type config struct {
	Order             int
	OldCookieDomain   string `file:"old_cookie_domain"`
	SessionCookieName string `file:"session_cookie_name"`
}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger
}

func (p *provider) List() []*interceptors.Interceptor {
	return []*interceptors.Interceptor{{Order: p.Cfg.Order, Wrapper: p.Interceptor}}
}

func (p *provider) Interceptor(h http.HandlerFunc) http.HandlerFunc {
	if len(p.Cfg.OldCookieDomain) <= 0 {
		return h
	}
	return func(rw http.ResponseWriter, r *http.Request) {
		cs := r.Cookies()
		count := 0
		for _, c := range cs {
			if c.Name == p.Cfg.SessionCookieName {
				count++
			}
		}
		if count > 1 {
			p.Log.Debugf("remove old session cookie")
			http.SetCookie(rw, &http.Cookie{
				Name:     p.Cfg.SessionCookieName,
				Value:    "",
				Path:     "/",
				Expires:  time.Unix(0, 0),
				MaxAge:   -1,
				Domain:   p.Cfg.OldCookieDomain,
				HttpOnly: true,
			})
		}
		h(rw, r)
	}
}

func init() {
	servicehub.Register("openapi-interceptor-auth-session-compatibility", &servicehub.Spec{
		Services:   []string{"openapi-interceptor-auth-session-compatibility"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
