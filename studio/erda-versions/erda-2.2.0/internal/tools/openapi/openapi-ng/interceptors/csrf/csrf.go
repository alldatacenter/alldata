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

package csrf

import (
	"context"
	"crypto/subtle"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"

	"github.com/labstack/echo"
	"github.com/labstack/gommon/random"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/common"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors"
)

type config struct {
	Order             int
	AllowValidReferer bool          `file:"allow_valid_referer" default:"false"`
	TokenGenerator    string        `file:"token_generator" default:"random"` // random、random:
	TokenLookup       string        `file:"token_lookup"`                     // "header:<name>"、"form:<name>"、 "query:<name>"
	CookieName        string        `file:"cookie_name" default:"csrf" desc:"name of the CSRF cookie. This cookie will store CSRF token. optional."`
	CookieDomain      string        `file:"cookie_domain" desc:"domain of the CSRF cookie. optional."`
	CookiePath        string        `file:"cookie_path" default:"/" desc:"path of the CSRF cookie. optional."`
	CookieMaxAge      time.Duration `file:"cookie_max_age" default:"24h" desc:"max age of the CSRF cookie. optional."`
	CookieHTTPOnly    bool          `file:"cookie_http_only" default:"false" desc:"indicates if CSRF cookie is HTTP only. optional."`
}

type (
	csrfTokenExtractor func(r *http.Request) (string, error)
	tokenGenerator     struct {
		gen   func(r *http.Request) string
		valid func(cookieToken, clientToken string, r *http.Request) bool
	}
	contextKey struct{}
)

// GetToken .
func GetToken(ctx context.Context) string {
	token, _ := ctx.Value(contextKey{}).(string)
	return token
}

// +provider
type provider struct {
	Log       logs.Logger
	Cfg       *config
	generator *tokenGenerator
	extractor csrfTokenExtractor
}

func (p *provider) Init(ctx servicehub.Context) error {
	// TokenLookup
	if len(p.Cfg.TokenLookup) <= 0 {
		p.Cfg.TokenLookup = "header:X-CSRF-Token"
	}
	parts := strings.Split(p.Cfg.TokenLookup, ":")
	tokenName := parts[1]
	if len(tokenName) <= 0 {
		return errors.New("token name must not be empty")
	}
	switch parts[0] {
	case "form":
		p.extractor = csrfTokenFromForm(tokenName)
	case "query":
		p.extractor = csrfTokenFromQuery(tokenName)
	case "header":
		p.extractor = csrfTokenFromHeader(tokenName)
	default:
		return fmt.Errorf("invalid token_lookup %q", p.Cfg.TokenLookup)
	}

	// TokenGenerator
	parts = strings.Split(p.Cfg.TokenGenerator, ":")
	gen, ok := tokenGenerators[parts[0]]
	if !ok {
		return fmt.Errorf("invalid token_generator %q", p.Cfg.TokenGenerator)
	}
	var args []string
	if len(parts) > 1 && len(parts[1]) > 0 {
		args = strings.Split(parts[1], ",")
	}
	generator, err := gen(p.Cfg, args)
	if err != nil {
		return fmt.Errorf("fail to create token generator: %s", err)
	}
	p.generator = generator
	return nil
}

var _ interceptors.Interface = (*provider)(nil)

func (p *provider) List() []*interceptors.Interceptor {
	return []*interceptors.Interceptor{
		{Order: p.Cfg.Order, Wrapper: p.Interceptor},
	}
}

// Interceptor .
func (p *provider) Interceptor(h http.HandlerFunc) http.HandlerFunc {
	return func(rw http.ResponseWriter, r *http.Request) {
		k, err := r.Cookie(p.Cfg.CookieName)
		var token string
		if err == nil {
			// Reuse token
			token = k.Value
		}
		switch r.Method {
		case http.MethodGet, http.MethodHead, http.MethodOptions, http.MethodTrace:
		default:
			validateToken := true
			if p.Cfg.AllowValidReferer {
				referer := r.Header.Get("Referer")
				if referer == "" {
					validateToken = false
				}
				if ref, err := url.Parse(referer); err == nil {
					if ref.Host == r.Host {
						validateToken = false
					}
				}
			}
			if validateToken {
				// Validate token only for requests which are not defined as 'safe' by RFC7231
				clientToken, err := p.extractor(r)
				if err != nil {
					// Browsers block frontend JavaScript code from accessing the Set Cookie header,
					// as required by the Fetch spec, which defines Set-Cookie as a forbidden response-header name
					// that must be filtered out from any response exposed to frontend code.
					// see: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie
					// use "invalid csrf token" prefix to let frontend known need retry
					err = fmt.Errorf("invalid csrf token: failed to extract token, err: %v", err)
					p.Log.Warn(err)
					p.setCSRFCookie(rw, r, "")
					rw.WriteHeader(http.StatusBadRequest)
					common.WriteError(err, rw)
					return
				}
				if !p.generator.valid(token, clientToken, r) {
					err := fmt.Errorf("invalid csrf token: %s", clientToken)
					p.Log.Warn(err)
					p.setCSRFCookie(rw, r, "")
					rw.WriteHeader(http.StatusForbidden)
					common.WriteError(err, rw)
					return
				}
			}
		}

		if len(token) <= 0 {
			// Set CSRF cookie
			token = p.setCSRFCookie(rw, r, token)
		}

		// Store token in the context
		r = r.WithContext(context.WithValue(r.Context(), contextKey{}, token))

		h(rw, r)
	}
}

var tokenGenerators = map[string]func(*config, []string) (*tokenGenerator, error){
	"random": func(cfg *config, args []string) (*tokenGenerator, error) {
		var length uint8 = 32
		if len(args) > 0 {
			v, err := strconv.ParseInt(args[0], 10, 64)
			if err != nil {
				return nil, err
			}
			length = uint8(v)
		}
		return &tokenGenerator{
			gen: func(r *http.Request) string {
				return random.String(length)
			},
			valid: func(cookieToken, clientToken string, r *http.Request) bool {
				return subtle.ConstantTimeCompare([]byte(cookieToken), []byte(clientToken)) == 1
			},
		}, nil
	},
}

// setCSRFCookie set CSRF cookie
func (p *provider) setCSRFCookie(rw http.ResponseWriter, r *http.Request, token string) string {
	// Protect clients from caching the response
	rw.Header().Add(echo.HeaderVary, echo.HeaderCookie)

	if token == "" {
		// Generate token
		token = p.generator.gen(r)
	}

	cookie := new(http.Cookie)
	cookie.Name = p.Cfg.CookieName
	cookie.Value = token
	if p.Cfg.CookiePath != "" {
		cookie.Path = p.Cfg.CookiePath
	}
	if p.Cfg.CookieDomain != "" {
		cookie.Domain = p.Cfg.CookieDomain
	}
	cookie.Expires = time.Now().Add(p.Cfg.CookieMaxAge)
	cookie.Secure = p.getScheme(r) == "https"
	cookie.HttpOnly = p.Cfg.CookieHTTPOnly
	http.SetCookie(rw, cookie)
	return token
}

// csrfTokenFromForm returns a `csrfTokenExtractor` that extracts token from the
// provided request header.
func csrfTokenFromHeader(header string) csrfTokenExtractor {
	return func(r *http.Request) (string, error) {
		return r.Header.Get(header), nil
	}
}

// csrfTokenFromForm returns a `csrfTokenExtractor` that extracts token from the
// provided form parameter.
func csrfTokenFromForm(param string) csrfTokenExtractor {
	return func(r *http.Request) (string, error) {
		token := r.FormValue(param)
		if token == "" {
			return "", errors.New("missing csrf token in the form parameter")
		}
		return token, nil
	}
}

// csrfTokenFromQuery returns a `csrfTokenExtractor` that extracts token from the
// provided query parameter.
func csrfTokenFromQuery(param string) csrfTokenExtractor {
	return func(r *http.Request) (string, error) {
		token := r.URL.Query().Get(param)
		if token == "" {
			return "", errors.New("missing csrf token in the query string")
		}
		return token, nil
	}
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

func init() {
	servicehub.Register("openapi-interceptor-csrf", &servicehub.Spec{
		Services:   []string{"openapi-interceptor-csrf"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
