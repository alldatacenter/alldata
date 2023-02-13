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

package proto

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	transhttp "github.com/erda-project/erda-infra/pkg/transport/http"
	common "github.com/erda-project/erda-proto-go/common/pb"
	discover "github.com/erda-project/erda/internal/pkg/service-discover"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/auth"
	"github.com/erda-project/erda/internal/tools/openapi/openapi-ng/proxy"
)

// +provider
type provider struct {
	Log      logs.Logger
	Discover discover.Interface `autowired:"discover"`
	Auth     auth.Interface     `autowired:"openapi-auth"`
	Router   openapi.Interface  `autowired:"openapi-router"`

	proxy proxy.Proxy
}

func (p *provider) Init(ctx servicehub.Context) (err error) {
	p.proxy = proxy.Proxy{
		Log:      p.Log,
		Discover: p.Discover,
	}
	return p.RegisterTo(p.Router)
}

func (p *provider) RegisterTo(router transhttp.Router) (err error) {
	var oneOpenAPIProxyHandler OneOpenAPIProxyHandler = func(method, publishPath, backendPath, serviceName string, opt *common.OpenAPIOption) error {
		handler, err := p.proxy.Wrap(method, publishPath, backendPath, serviceName)
		if err != nil {
			return err
		}
		handler = p.Auth.Interceptor(handler, GetAuthOption(opt.Auth, func(opts map[string]interface{}) map[string]interface{} {
			opts["path"] = publishPath
			opts["method"] = method
			return opts
		}))
		router.Add(method, publishPath, transhttp.HandlerFunc(handler))
		return nil
	}
	return RangeOpenAPIsProxy(oneOpenAPIProxyHandler)
}

func init() {
	servicehub.Register("openapi-protobuf-routes", &servicehub.Spec{
		Creator: func() servicehub.Provider { return &provider{} },
	})
}
