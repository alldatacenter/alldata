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

package resource

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	"github.com/erda-project/erda-proto-go/msp/resource/pb"
	monitordb "github.com/erda-project/erda/internal/apps/msp/instance/db/monitor"
	"github.com/erda-project/erda/internal/apps/msp/resource/deploy/coordinator"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

// +provider
type provider struct {
	Cfg               *config
	Log               logs.Logger
	Register          transport.Register `autowired:"service-register" optional:"true"`
	resourceService   *resourceService
	DeployCoordinator coordinator.Interface   `autowired:"erda.msp.resource.deploy.coordinator"`
	ES                elasticsearch.Interface `autowired:"elasticsearch"`
	DB                *gorm.DB                `autowired:"mysql-client"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.resourceService = &resourceService{p: p, coordinator: p.DeployCoordinator, es: p.ES.Client(), monitorDb: &monitordb.MonitorDB{DB: p.DB}}
	if p.Register != nil {
		pb.RegisterResourceServiceImp(p.Register, p.resourceService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.resource.ResourceService" || ctx.Type() == pb.ResourceServiceServerType() || ctx.Type() == pb.ResourceServiceHandlerType():
		return p.resourceService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.resource", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Dependencies:         []string{"erda.msp.resource.deploy.coordinator", "elasticsearch"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
