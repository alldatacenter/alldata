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

package registercenter

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/msp/registercenter/pb"
	"github.com/erda-project/erda/bundle"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct{}

// +provider
type provider struct {
	Cfg                   *config
	Log                   logs.Logger
	Register              transport.Register `autowired:"service-register" optional:"true"`
	DB                    *gorm.DB           `autowired:"mysql-client"`
	Perm                  perm.Interface     `autowired:"permission"`
	registerCenterService *registerCenterService
	bdl                   *bundle.Bundle
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithScheduler())
	p.registerCenterService = &registerCenterService{
		p:                p,
		bdl:              p.bdl,
		instanceTenantDB: &instancedb.InstanceTenantDB{DB: p.DB},
		instanceDB:       &instancedb.InstanceDB{DB: p.DB},
	}
	if p.Register != nil {
		type RegisterCenterService pb.RegisterCenterServiceServer
		pb.RegisterRegisterCenterServiceImp(p.Register, p.registerCenterService, apis.Options(), p.Perm.Check(
			perm.Method(RegisterCenterService.ListInterface, perm.ScopeProject, "registercenter", perm.ActionGet, perm.FieldValue("ProjectID")),
			perm.Method(RegisterCenterService.GetHTTPServices, perm.ScopeProject, "registercenter", perm.ActionGet, perm.FieldValue("ProjectID")),
			perm.Method(RegisterCenterService.EnableHTTPService, perm.ScopeProject, "registercenter", perm.ActionUpdate, perm.FieldValue("ProjectID")),
			perm.Method(RegisterCenterService.GetServiceIpInfo, perm.ScopeProject, "registercenter", perm.ActionUpdate, perm.FieldValue("ProjectID")),
		))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.registercenter.RegisterCenterService" || ctx.Type() == pb.RegisterCenterServiceServerType() || ctx.Type() == pb.RegisterCenterServiceHandlerType():
		return p.registerCenterService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.registercenter", &servicehub.Spec{
		Services: pb.ServiceNames(),
		Types:    pb.Types(),
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
