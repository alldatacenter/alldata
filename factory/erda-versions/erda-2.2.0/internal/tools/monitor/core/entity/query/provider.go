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

package entity

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/oap/entity/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/entity/storage"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct{}

// +provider
type provider struct {
	Cfg           *config
	Log           logs.Logger
	Register      transport.Register `autowired:"service-register" optional:"true"`
	Storage       storage.Storage    `autowired:"entity-storage"`
	entityService *entityService
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.entityService = &entityService{
		p:       p,
		storage: p.Storage,
	}
	if p.Register != nil {
		pb.RegisterEntityServiceImp(p.Register, p.entityService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.oap.entity.EntityService" || ctx.Type() == pb.EntityServiceServerType() || ctx.Type() == pb.EntityServiceHandlerType():
		return p.entityService
	}
	return p
}

func init() {
	servicehub.Register("erda.oap.entity", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		ConfigFunc:           func() interface{} { return &config{} },
		Creator:              func() servicehub.Provider { return &provider{} },
	})
}
