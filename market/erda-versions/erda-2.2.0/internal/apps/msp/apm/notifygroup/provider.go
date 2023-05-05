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

package notifygroup

import (
	"context"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	notifygroup "github.com/erda-project/erda-proto-go/core/messenger/notifygroup/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/notifygroup/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	instancedb "github.com/erda-project/erda/internal/apps/msp/instance/db"
	"github.com/erda-project/erda/internal/apps/msp/tenant/db"
	"github.com/erda-project/erda/internal/pkg/audit"
	db2 "github.com/erda-project/erda/internal/tools/monitor/common/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

type provider struct {
	Cfg                *config
	Register           transport.Register
	notifyGroupService *notifyGroupService
	bdl                *bundle.Bundle
	DB                 *gorm.DB `autowired:"mysql-client"`
	instanceDB         *instancedb.InstanceTenantDB
	mspTenantDB        *db.MSPTenantDB
	monitorDB          *db2.MonitorDb
	audit              audit.Auditor
	Tenant             tenantpb.TenantServiceServer         `autowired:"erda.msp.tenant.TenantService"`
	NotifyGroup        notifygroup.NotifyGroupServiceServer `autowired:"erda.core.messenger.notifygroup.NotifyGroupService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.audit = audit.GetAuditor(ctx)
	p.notifyGroupService = &notifyGroupService{p}
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())
	p.instanceDB = &instancedb.InstanceTenantDB{DB: p.DB}
	p.mspTenantDB = &db.MSPTenantDB{DB: p.DB}
	p.monitorDB = &db2.MonitorDb{DB: p.DB}
	if p.Register != nil {
		type NotifyService = pb.NotifyGroupServiceServer
		pb.RegisterNotifyGroupServiceImp(p.Register, p.notifyGroupService, apis.Options(),
			p.audit.Audit(
				audit.Method(NotifyService.CreateNotifyGroup, audit.ProjectScope, string(apistructs.CreateServiceNotifyGroup),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*pb.CreateNotifyGroupResponse)
						return r.Data.ProjectId, map[string]interface{}{}, nil
					},
				),
				audit.Method(NotifyService.UpdateNotifyGroup, audit.ProjectScope, string(apistructs.UpdateServiceNotifyGroup),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*pb.UpdateNotifyGroupResponse)
						return r.Data.ProjectId, map[string]interface{}{}, nil
					},
				),
				audit.Method(NotifyService.DeleteNotifyGroup, audit.ProjectScope, string(apistructs.DeleteServiceNotifyGroup),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*pb.DeleteNotifyGroupResponse)
						return r.Data.ProjectId, map[string]interface{}{}, nil
					},
				),
			),
		)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.apm.notifygroup.NotifyGroupService" || ctx.Type() == pb.NotifyGroupServiceServerType() || ctx.Type() == pb.NotifyGroupServiceHandlerType():
		return p.notifyGroupService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.apm.notifygroup", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Types: pb.Types(),
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
