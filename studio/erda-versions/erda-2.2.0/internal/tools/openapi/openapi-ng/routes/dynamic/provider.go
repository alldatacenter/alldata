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

package dynamic

import (
	"context"
	"embed"
	"net/http"
	"path/filepath"
	"time"

	"github.com/coreos/etcd/clientv3"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	"github.com/erda-project/erda-infra/providers/httpserver"
	fserver "github.com/erda-project/erda-infra/providers/remote-forward/server"
	discover "github.com/erda-project/erda/internal/pkg/service-discover"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/proxy"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/routes/proto"
)

//go:embed static
var webfs embed.FS

type (
	config struct {
		Prefix              string        `file:"prefix" default:"/openapi/apis"`
		EtcdRequestTimeout  time.Duration `file:"etcd_request_timeout" default:"2m"`
		UseEmbedStaticFiles bool          `file:"use_embed_static_files" default:"true" env:"EMBED_STATIC_FILES"`
		KeepAliveTTL        time.Duration `file:"keepalive_ttl" default:"1m"`
		Host                string        `file:"host" env:"POD_IP"`
		NetInterfaceName    string        `file:"net_interface_name"`
	}
	provider struct {
		Cfg           *config
		Log           logs.Logger
		Router        httpserver.Router  `autowired:"http-router@admin"`
		Etcd          *clientv3.Client   `autowired:"etcd-client"`
		Discover      discover.Interface `autowired:"discover"`
		ForwardServer fserver.Interface  `autowired:"remote-forward-server"`
		proxy         proxy.Proxy
		ctx           servicehub.Context
	}
)

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.ctx = ctx
	p.proxy.Log = p.Log
	p.proxy.Discover = p.Discover
	p.Cfg.Prefix = filepath.Clean("/" + p.Cfg.Prefix)

	p.Router.GET("/openapi/apis", p.listAPIProxies)
	p.Router.PUT("/openapi/apis", p.setAPIProxy)
	p.Router.PUT("/openapi/apis-batch-keepalive", p.setAPIProxyWithKeepAlive)
	p.Router.DELETE("/openapi/apis", p.removeAPIProxy)
	p.Router.GET("/openapi/services", p.listServices)
	if p.Cfg.UseEmbedStaticFiles {
		p.Router.Static("/openapi/static", "/static", httpserver.WithFileSystem(http.FS(webfs)))
	} else {
		p.Router.Static("/openapi/static", "internal/tools/openapi/openapi-ng/routes/dynamic/static")
	}

	return p.initRemoteForward(ctx)
}

var _ openapi.RouteSourceWatcher = (*provider)(nil)

func (p *provider) Watch(ctx context.Context) <-chan openapi.RouteSource {
	auth, _ := p.ctx.Service("openapi-auth").(auth.Interface)
	ch := make(chan openapi.RouteSource, 1)
	go func() {
		apis, err := p.getAPIProxies()
		if err != nil {
			p.Log.Errorf("failed to load api proxy: %s", err)
		} else {
			ch <- &routeSource{
				apis:  apis,
				proxy: p.proxy,
				auth:  auth,
			}
		}
		for func() bool {
			wctx, wcancel := context.WithCancel(ctx)
			defer wcancel()
			wch := p.Etcd.Watch(wctx, p.Cfg.Prefix, clientv3.WithPrefix())
			for {
				select {
				case wr, ok := <-wch:
					if !ok {
						return false
					}
					if wr.Err() != nil {
						p.Log.Error(wr)
						return true
					}
					apis, err := p.getAPIProxies()
					if err != nil {
						p.Log.Errorf("failed to load api proxy: %s", err)
						return true
					}
					ch <- &routeSource{
						apis:  apis,
						proxy: p.proxy,
						auth:  auth,
					}
				case <-ctx.Done():
					return false
				}
			}
		}() {
		}
	}()
	return ch
}

type routeSource struct {
	apis  []*APIProxy
	proxy proxy.Proxy
	auth  auth.Interface
}

func (rs *routeSource) Name() string { return "route-dynamic" }
func (rs *routeSource) RegisterTo(router transhttp.Router) error {
	for _, api := range rs.apis {
		handler, err := rs.proxy.WrapWithServiceURL(api.Method, api.Path, api.BackendPath, api.ServiceURL)
		if err != nil {
			return err
		}
		if rs.auth != nil && api.Auth != nil {
			handler = rs.auth.Interceptor(handler, proto.GetAuthOption(api.Auth, func(opts map[string]interface{}) map[string]interface{} {
				opts["path"] = api.Path
				opts["method"] = api.Method
				return opts
			}))
		}
		router.Add(api.Method, api.Path, transhttp.HandlerFunc(handler))
	}
	return nil
}

func init() {
	servicehub.Register("openapi-dynamic-routes", &servicehub.Spec{
		Services:   []string{"openapi-route-watcher-dynamic"},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
