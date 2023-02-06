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
	"context"
	"fmt"
	"net/http"

	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/providers/httpserver"
)

var _ Interface = (*service)(nil)

type service struct {
	p    *provider
	name string
}

var allMethods = []string{
	http.MethodConnect,
	http.MethodDelete,
	http.MethodGet,
	http.MethodHead,
	http.MethodOptions,
	http.MethodPatch,
	http.MethodPost,
	http.MethodPut,
	http.MethodTrace,
}

func (s *service) Add(method, path string, handler transhttp.HandlerFunc) {
	if len(method) <= 0 {
		for _, method := range allMethods {
			s.addRoute(method, path, handler)
		}
		return
	}
	s.addRoute(method, path, handler)
}

func (s *service) addRoute(method, path string, handler transhttp.HandlerFunc) {
	r := route{
		method:  method,
		path:    path,
		handler: s.p.WrapHandler(handler),
	}
	s.p.routes = append(s.p.routes, r)
	if !s.p.RouterManager.Reloadable() {
		s.p.router.Add(r.method, r.path, r.handler, httpserver.WithPathFormat(httpserver.PathFormatGoogleAPIs))
	}
}

func (s *service) WrapHandler(handler transhttp.HandlerFunc) transhttp.HandlerFunc {
	return s.p.WrapHandler(handler)
}

func (p *provider) WrapHandler(handler transhttp.HandlerFunc) transhttp.HandlerFunc {
	for i := len(p.interceptors) - 1; i >= 0; i-- {
		handler = transhttp.HandlerFunc(p.interceptors[i].Wrapper(http.HandlerFunc(handler)))
	}
	return handler
}

func (p *provider) Run(ctx context.Context) error {
	if p.RouterManager.Reloadable() {
		router := p.RouterManager.NewRouter()
		p.registerFixedRoutes(router, nil)
		err := router.Commit()
		if err != nil {
			return err
		}
	}
	return p.doWatch(ctx)
}

func (p *provider) registerFixedRoutes(router httpserver.Router, exclude map[routeKey]bool) {
	opts := []interface{}{httpserver.WithPathFormat(httpserver.PathFormatGoogleAPIs)}
	for _, route := range p.routes {
		if exclude[routeKey{
			method: route.method, path: route.path,
		}] {
			continue
		}
		router.Add(route.method, route.path, route.handler, opts...)
	}
}

func (p *provider) doWatch(ctx context.Context) error {
	if len(p.watchers) > 0 && !p.RouterManager.Reloadable() {
		return fmt.Errorf("http router must be reloadable for route watchers")
	}
	for _, w := range p.watchers {
		go func(w RouteSourceWatcher) {
			ch := w.Watch(ctx)
			for {
				select {
				case source := <-ch:
					p.Log.Info("routes reloading ...")
					tx := p.RouterManager.NewRouter()
					router := &routerTx{
						tx:     tx,
						routes: make(map[routeKey]bool),
						p:      p,
					}
					err := source.RegisterTo(router)
					if err != nil {
						p.Log.Errorf("failed to register routes from source(%s): %s", source.Name(), err)
						continue
					}
					p.registerFixedRoutes(tx, router.routes)
					err = tx.Commit()
					if err != nil {
						p.Log.Errorf("failed to commit routes from source(%s): %s", source.Name(), err)
					}
					p.Log.Info("routes reload ok")
				case <-ctx.Done():
					return
				}
			}
		}(w)
	}
	return nil
}

type routerTx struct {
	tx     httpserver.RouterTx
	routes map[routeKey]bool
	p      *provider
}

func (rt *routerTx) Add(method, path string, handler transhttp.HandlerFunc) {
	if len(method) <= 0 {
		for _, method := range allMethods {
			rt.addRoute(method, path, handler)
		}
		return
	}
	rt.addRoute(method, path, handler)
}

func (rt *routerTx) addRoute(method, path string, handler transhttp.HandlerFunc) {
	rt.routes[routeKey{
		method: method,
		path:   path,
	}] = true
	rt.tx.Add(method, path, rt.p.WrapHandler(handler), httpserver.WithPathFormat(httpserver.PathFormatGoogleAPIs))
}
