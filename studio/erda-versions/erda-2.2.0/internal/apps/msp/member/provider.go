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

package member

import (
	"context"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-proto-go/msp/member/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
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
	Cfg           *config
	Register      transport.Register
	memberService *memberService
	bdl           *bundle.Bundle
	ProjectServer projectpb.ProjectServiceServer
	DB            *gorm.DB `autowired:"mysql-client"`
	instanceDB    *instancedb.InstanceTenantDB
	mspTenantDB   *db.MSPTenantDB
	monitorDB     *db2.MonitorDb
	audit         audit.Auditor
	Tenant        tenantpb.TenantServiceServer `autowired:"erda.msp.tenant.TenantService"`
	I18n          i18n.Translator              `autowired:"i18n"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.audit = audit.GetAuditor(ctx)
	p.memberService = &memberService{p}
	p.bdl = bundle.New(bundle.WithScheduler(), bundle.WithCoreServices())
	if p.Register != nil {
		type MemberService = pb.MemberServiceServer
		pb.RegisterMemberServiceImp(p.Register, p.memberService, apis.Options(),
			p.audit.Audit(
				audit.Method(MemberService.CreateOrUpdateMember, audit.ProjectScope, string(apistructs.AddServiceMember),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*pb.CreateOrUpdateMemberResponse)
						return r.Data, map[string]interface{}{}, nil
					},
				),
				audit.Method(MemberService.DeleteMember, audit.ProjectScope, string(apistructs.DeleteServiceMember),
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*pb.DeleteMemberResponse)
						return r.Data, map[string]interface{}{}, nil
					},
				),
			),
		)
	}
	p.instanceDB = &instancedb.InstanceTenantDB{DB: p.DB}
	p.mspTenantDB = &db.MSPTenantDB{DB: p.DB}
	p.monitorDB = &db2.MonitorDb{DB: p.DB}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.member.MemberService" || ctx.Type() == pb.MemberServiceServerType() || ctx.Type() == pb.MemberServiceHandlerType():
		return p.memberService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.member", &servicehub.Spec{
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
