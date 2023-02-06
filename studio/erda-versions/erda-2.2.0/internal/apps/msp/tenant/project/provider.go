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

package project

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/msp/instance/db/monitor"
	"github.com/erda-project/erda/internal/apps/msp/tenant/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

// +provider
type provider struct {
	Cfg            *config
	Log            logs.Logger
	Register       transport.Register
	projectService *projectService
	bdl            *bundle.Bundle
	I18n           i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	DB             *gorm.DB                     `autowired:"mysql-client"`
	TenantServer   tenantpb.TenantServiceServer `autowired:"erda.msp.tenant.TenantService"`
	Metric         metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithMSP())
	p.projectService = &projectService{
		p:            p,
		MSPProjectDB: &db.MSPProjectDB{DB: p.DB},
		MSPTenantDB:  &db.MSPTenantDB{DB: p.DB},
		MonitorDB:    &monitor.MonitorDB{DB: p.DB},
		metricq:      p.Metric,
	}
	if p.Register != nil {
		pb.RegisterProjectServiceImp(p.Register, p.projectService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.tenant.project.ProjectService" || ctx.Type() == pb.ProjectServiceServerType() || ctx.Type() == pb.ProjectServiceHandlerType():
		return p.projectService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.tenant.project", &servicehub.Spec{
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
