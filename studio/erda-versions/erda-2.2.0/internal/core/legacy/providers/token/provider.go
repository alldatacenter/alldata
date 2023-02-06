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

package token

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

type provider struct {
	Cfg          *config
	Log          logs.Logger
	Register     transport.Register `autowired:"service-register" required:"true"`
	tokenService *TokenService
	DB           *gorm.DB `autowired:"mysql-client"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.tokenService = &TokenService{
		logger: p.Log,
		dao:    &dao{db: p.DB},
	}
	if p.Register != nil {
		pb.RegisterTokenServiceImp(p.Register, p.tokenService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.token.TokenService" || ctx.Type() == pb.TokenServiceServerType() || ctx.Type() == pb.TokenServiceHandlerType():
		return p.tokenService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.token", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
