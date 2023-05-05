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
	"sort"
	"strings"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type (
	// Interface .
	Interface interface {
		Interceptor(h http.HandlerFunc, opts func(r *http.Request) Options) http.HandlerFunc
	}
	// Auther .
	Auther interface {
		Weight() int64
		Match(r *http.Request, opts Options) (bool, interface{})
		Check(r *http.Request, data interface{}, opts Options) (bool, *http.Request, error)
	}
	// Options .
	Options interface {
		Get(key string) interface{}
		Set(key string, val interface{})
	}
	// AutherLister .
	AutherLister interface {
		Authers() []Auther
	}
)

type provider struct {
	authers []Auther
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	ctx.Hub().ForeachServices(func(service string) bool {
		if strings.HasPrefix(service, "openapi-auth-") {
			authers, ok := ctx.Service(service).(AutherLister)
			if !ok {
				err = fmt.Errorf("%q not implements AutherLister", service)
				return false
			}
			p.authers = append(p.authers, authers.Authers()...)
		}
		return true
	})
	if err != nil {
		return err
	}
	sort.Slice(p.authers, func(i, j int) bool {
		return p.authers[i].Weight() >= p.authers[j].Weight()
	})
	return nil
}

var _ Interface = (*provider)(nil)

func (p *provider) Interceptor(h http.HandlerFunc, opts func(r *http.Request) Options) http.HandlerFunc {
	if len(p.authers) <= 0 {
		return func(rw http.ResponseWriter, r *http.Request) {
			r.Header.Del(httputil.UserHeader)
			r.Header.Del(httputil.OrgHeader)
			h(rw, r)
		}
	}
	return func(rw http.ResponseWriter, r *http.Request) {
		r.Header.Del(httputil.UserHeader)
		r.Header.Del(httputil.OrgHeader)
		opts := opts(r)
		if disable, _ := opts.Get("NoCheck").(bool); disable {
			h(rw, r)
			return
		}
		for _, auther := range p.authers {
			if ok, data := auther.Match(r, opts); ok {
				ok, req, err := auther.Check(r, data, opts)
				if err != nil {
					http.Error(rw, err.Error(), http.StatusUnauthorized)
					return
				}
				if ok {
					h(rw, req)
					return
				}
				break
			}
		}
		http.Error(rw, http.StatusText(http.StatusUnauthorized), http.StatusUnauthorized)
	}
}

func init() {
	servicehub.Register("openapi-auth", &servicehub.Spec{
		Services: []string{"openapi-auth"},
		DependenciesFunc: func(hub *servicehub.Hub) (list []string) {
			hub.ForeachServices(func(service string) bool {
				if strings.HasPrefix(service, "openapi-auth-") {
					list = append(list, service)
				}
				return true
			})
			return list
		},
		Creator: func() servicehub.Provider { return &provider{} },
	})
}
