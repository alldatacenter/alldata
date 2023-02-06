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

package openapiv1

import (
	"net/http"
	"strings"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	discover "github.com/erda-project/erda/internal/pkg/service-discover"
	openapiv1 "github.com/erda-project/erda/internal/tools/openapi/legacy"
	apiv1 "github.com/erda-project/erda/internal/tools/openapi/legacy/api"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/types"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/hooks"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/proxy"
)

type config struct {
	CP types.ComponentProtocolConfigs `file:"component-protocol"`
}

// +provider
type provider struct {
	Cfg          *config
	Log          logs.Logger
	Discover     discover.Interface `autowired:"discover"`
	Router       openapi.Interface  `autowired:"openapi-router"`
	proxy        proxy.Proxy
	handler      http.Handler
	TokenService tokenpb.TokenServiceServer `autowired:"erda.core.token.TokenService"`
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.proxy.Log = p.Log
	p.proxy.Discover = p.Discover
	hooks.Enable = false
	conf.Load()
	srv, err := openapiv1.NewServer(p.TokenService)
	if err != nil {
		return err
	}
	p.handler = srv.Handler
	types.CPConfigs = p.Cfg.CP
	return p.RegisterTo(p.Router)
}

func (p *provider) RegisterTo(router transhttp.Router) (err error) {
	for _, api := range apiv1.API {
		newPath := replaceOpenapiV1Path(api.Path.String())
		router.Add(api.Method, newPath, p.handler.ServeHTTP)
	}
	// v1 router add backport methods routes
	router.Add("", "/**", p.handler.ServeHTTP)
	return nil
}

func replaceOpenapiV1Path(path string) string {
	path = strings.ReplaceAll(path, "<*>", "**")
	newPath := strings.NewReplacer("<", "{", ">", "}", " ", "_").Replace(path)
	return newPath
}

func init() {
	servicehub.Register("openapi-v1-routes", &servicehub.Spec{
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
