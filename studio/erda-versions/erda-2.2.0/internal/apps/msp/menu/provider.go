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

package menu

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/msp/menu/pb"
	"github.com/erda-project/erda/bundle"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	mperm "github.com/erda-project/erda/internal/apps/msp/instance/permission"
	"github.com/erda-project/erda/internal/apps/msp/menu/db"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
}

// +provider
type provider struct {
	Cfg         *config
	Log         logs.Logger
	Register    transport.Register `autowired:"service-register" optional:"true"`
	DB          *gorm.DB           `autowired:"mysql-client"`
	Perm        perm.Interface     `autowired:"permission"`
	MPerm       mperm.Interface    `autowired:"msp.permission"`
	menuService *menuService
	bdl         *bundle.Bundle
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithScheduler())

	p.menuService = &menuService{
		p:                p,
		db:               &db.MenuConfigDB{DB: p.DB},
		instanceTenantDB: &instancedb.InstanceTenantDB{DB: p.DB},
		instanceDB:       &instancedb.InstanceDB{DB: p.DB},
		bdl:              p.bdl,
	}
	if p.Register != nil {
		type MenuService = pb.MenuServiceServer
		pb.RegisterMenuServiceImp(p.Register, p.menuService, apis.Options(), p.Perm.Check(
			perm.Method(MenuService.GetMenu, perm.ScopeProject, "menu", perm.ActionGet, p.MPerm.TenantToProjectID("TenantGroup", "TenantId")),
			perm.Method(MenuService.GetSetting, perm.ScopeProject, "settings", perm.ActionGet, p.MPerm.TenantToProjectID("TenantGroup", "TenantId")),
		))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.menu.MenuService" || ctx.Type() == pb.MenuServiceServerType() || ctx.Type() == pb.MenuServiceHandlerType():
		return p.menuService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.menu", &servicehub.Spec{
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
