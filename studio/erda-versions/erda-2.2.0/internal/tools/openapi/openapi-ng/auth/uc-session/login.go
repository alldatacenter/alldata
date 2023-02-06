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
	"fmt"
	"net"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/go-redis/redis"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/common"
)

func (p *provider) LoginURL(rw http.ResponseWriter, r *http.Request) {
	referer := r.Header.Get("Referer")
	if len(referer) <= 0 {
		referer = p.Cfg.RedirectAfterLogin
	}
	common.ResponseJSON(rw, &struct {
		URL string `json:"url"`
	}{
		URL: p.getAuthorizeURL(p.getScheme(r), r.URL.Host, referer),
	})
}

func (p *provider) getAuthorizeURL(scheme, host, referer string) string {
	params := url.Values{}
	params.Set("response_type", "code")
	params.Set("scope", "public_profile")
	params.Set("client_id", p.Cfg.ClientID)
	params.Set("redirect_uri",
		fmt.Sprintf("%s://%s/logincb?referer=%s", scheme, p.getUCRedirectHost(referer, host), url.QueryEscape(referer)))
	return fmt.Sprintf("%s://%s/oauth/authorize?%s", scheme, p.Cfg.UCAddr, params.Encode())
}

func (p *provider) LoginCallback(rw http.ResponseWriter, r *http.Request) {
	code := r.URL.Query().Get("code")
	referer := r.URL.Query().Get("referer")

	scheme := p.getScheme(r)
	redirectURI := fmt.Sprintf("%s://%s/logincb?referer=%s", scheme, p.getUCRedirectHost(referer, r.URL.Host), url.QueryEscape(referer))

	user := auth.NewUser(p.Redis)
	sessionID, _, err := user.Login(code, redirectURI)
	if err != nil {
		p.Log.Errorf("failed to login: %v", err)
		http.Error(rw, err.Error(), http.StatusUnauthorized)
		return
	}

	http.SetCookie(rw, &http.Cookie{
		Name:     p.Cfg.SessionCookieName,
		Value:    sessionID,
		Domain:   p.getSessionDomain(r.Host),
		HttpOnly: true,
		Secure:   scheme == "https",
	})
	http.Redirect(rw, r, referer, http.StatusFound)
}

func (p *provider) Logout(rw http.ResponseWriter, r *http.Request) {
	referer := r.Header.Get("Referer")
	if len(referer) <= 0 {
		referer = p.Cfg.RedirectAfterLogin
	}

	session := p.getSession(r)
	if len(session) > 0 {
		r = r.WithContext(context.WithValue(r.Context(), "session", session))
		if err := auth.NewUser(p.Redis).Logout(r); err != nil {
			err := fmt.Errorf("logout: %v", err)
			p.Log.Error(err)
			http.Error(rw, err.Error(), http.StatusBadGateway)
			return
		}
	}

	scheme := p.getScheme(r)
	http.SetCookie(rw, &http.Cookie{
		Name:     p.Cfg.SessionCookieName,
		Value:    "",
		Path:     "/",
		Expires:  time.Unix(0, 0),
		MaxAge:   -1,
		Domain:   p.getSessionDomain(r.Host),
		HttpOnly: true,
		Secure:   scheme == "https",
	})

	redirectURL := p.getAuthorizeURL(scheme, r.URL.Host, referer)
	common.ResponseJSON(rw, &struct {
		URL string `json:"url"`
	}{
		URL: fmt.Sprintf("%s://%s/logout?redirectUrl=%s", scheme, p.Cfg.UCAddr, url.QueryEscape(redirectURL)),
	})
}

func (p *provider) getScheme(r *http.Request) string {
	// get from standard header first
	proto := firstNonEmpty(r.Header.Get("X-Forwarded-Proto"), r.Header.Get("X-Forwarded-Protocol"), r.URL.Scheme)
	if len(proto) > 0 {
		return proto
	}
	return "https"
}

func firstNonEmpty(ss ...string) string {
	for _, s := range ss {
		if len(s) > 0 {
			list := strings.Split(s, ",")
			for _, item := range list {
				v := strings.ToLower(strings.TrimSpace(item))
				if len(v) > 0 {
					return v
				}
			}
		}
	}
	return ""
}

func (p *provider) getSessionDomain(host string) string {
	if h, _, err := net.SplitHostPort(host); err == nil {
		host = h
	}
	domains := strings.SplitN(host, ".", -1)
	l := len(domains)
	if l < 2 {
		return ""
	}
	rootDomain := "." + domains[l-2] + "." + domains[l-1]
	for _, domain := range p.Cfg.SessionCookieDomains {
		if strings.Contains(domain, rootDomain) {
			return domain
		}
	}
	return ""
}

func (p *provider) getUCRedirectHost(referer, host string) string {
	for _, addr := range p.Cfg.UCRedirectAddrs {
		domain := addr
		if h, _, err := net.SplitHostPort(host); err == nil {
			domain = h
		}
		domains := strings.SplitN(domain, ".", -1)
		l := len(domains)
		if l < 2 {
			continue
		}
		rootDomain := domains[l-2] + "." + domains[l-1]
		if strings.Contains(referer, rootDomain) {
			return addr
		}
	}
	return host
}

func (p *provider) getSession(r *http.Request) string {
	cookies := r.Cookies()
	var sessions []*http.Cookie
	for _, c := range cookies {
		if c.Name == p.Cfg.SessionCookieName {
			sessions = append(sessions, c)
		}
	}
	for _, session := range sessions {
		if _, err := p.Redis.Get(makeSessionKey(session.Value)).Result(); err == redis.Nil {
			continue
		} else if err != nil {
			continue
		}
		return session.Value
	}
	return ""
}

func makeSessionKey(sessionID string) string {
	return "openapi:sessionid:" + sessionID
}
