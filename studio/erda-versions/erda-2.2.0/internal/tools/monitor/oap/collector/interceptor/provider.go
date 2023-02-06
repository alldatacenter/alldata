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

package interceptor

import (
	"reflect"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/authentication"
)

var InterceptorType = reflect.TypeOf((*Interceptors)(nil)).Elem()

type config struct {
}

// +provider
type provider struct {
	Cfg          *config
	Log          logs.Logger
	interceptors Interceptors
	Validator    authentication.Validator `autowired:"erda.oap.collector.authentication.Validator"`
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	p.interceptors = &interceptorImpl{validator: p.Validator}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.oap.collector.interceptor.Interceptor" || ctx.Type() == InterceptorType:
		return p.interceptors
	}
	return p
}

func init() {
	servicehub.Register("erda.oap.collector.interceptor", &servicehub.Spec{
		Services: []string{
			"erda.oap.collector.interceptor.Interceptor",
		},
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
