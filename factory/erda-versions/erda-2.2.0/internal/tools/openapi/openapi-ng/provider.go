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

package openapi

import (
	"fmt"
	"reflect"
	"sort"
	"strings"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/interceptors"
)

type (
	routeKey struct {
		method string
		path   string
	}
	route struct {
		method  string
		path    string
		handler transhttp.HandlerFunc
	}
)

type (
	config   struct{}
	provider struct {
		Cfg           *config
		Log           logs.Logger
		RouterManager httpserver.RouterManager `autowired:"http-router-manager"`
		interceptors  interceptors.Interceptors
		routes        []route
		sources       []RouteSource
		watchers      []RouteSourceWatcher
		router        httpserver.Router
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	if !p.RouterManager.Reloadable() {
		p.router = p.RouterManager.NewRouter()
	}
	ctx.Hub().ForeachServices(func(service string) bool {
		for i := len(dependKeys) - 1; i >= 0; i-- {
			if strings.HasPrefix(service, dependKeys[i]) {
				h := dependencies[dependKeys[i]]
				err = h.handler(p, service, ctx.Service(service))
				if err != nil {
					return false
				}
			}
		}
		return true
	})
	if err != nil {
		return err
	}
	for _, h := range dependencies {
		if h.done != nil {
			err = h.done(p)
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return &service{
		p:    p,
		name: ctx.Caller(),
	}
}

type dependencyHandler struct {
	handler func(p *provider, service string, d interface{}) error
	done    func(p *provider) error
}

var (
	dependencies = map[string]dependencyHandler{
		"openapi-interceptor-": {
			handler: func(p *provider, service string, d interface{}) error {
				inter, ok := d.(interceptors.Interface)
				if !ok {
					return fmt.Errorf("service %s is not interceptor", service)
				}
				p.interceptors = append(p.interceptors, inter.List()...)
				return nil
			},
			done: func(p *provider) error {
				sort.Sort(p.interceptors)
				return nil
			},
		},
		"openapi-route-": {
			handler: func(p *provider, service string, d interface{}) error {
				s, ok := d.(RouteSource)
				if ok {
					p.sources = append(p.sources, s)
				}
				return nil
			},
			done: func(p *provider) error {
				for _, source := range p.sources {
					err := source.RegisterTo(&service{
						p:    p,
						name: source.Name(),
					})
					if err != nil {
						return err
					}
				}
				return nil
			},
		},
		"openapi-route-watcher-": {
			handler: func(p *provider, service string, d interface{}) error {
				w, ok := d.(RouteSourceWatcher)
				if ok {
					p.watchers = append(p.watchers, w)
				}
				return nil
			},
		},
	}
	dependKeys []string
)

func init() {
	dependKeys = nil
	for key := range dependencies {
		dependKeys = append(dependKeys, key)
	}
	sort.Strings(dependKeys)
	servicehub.Register("openapi-ng", &servicehub.Spec{
		Services: []string{"openapi-router"},
		DependenciesFunc: func(hub *servicehub.Hub) (list []string) {
			hub.ForeachServices(func(service string) bool {
				for i := len(dependKeys) - 1; i >= 0; i-- {
					if strings.HasPrefix(service, dependKeys[i]) {
						list = append(list, service)
						break
					}
				}
				return true
			})
			return list
		},
		Types:      []reflect.Type{reflect.TypeOf((*transhttp.Router)(nil)).Elem()},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
