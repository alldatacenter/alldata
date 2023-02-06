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

package domain

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/hepa/domain/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/domain/impl"
	epI "github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api/impl"
	ruleI "github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_rule/impl"
	zoneI "github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/zone/impl"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
}

// +provider
type provider struct {
	Cfg           *config
	Log           logs.Logger
	Register      transport.Register
	domainService *domainService
	Perm          perm.Interface `autowired:"permission"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.domainService = &domainService{p}
	err := impl.NewGatewayDomainServiceImpl()
	if err != nil {
		return err
	}
	err = ruleI.NewGatewayOpenapiRuleServiceImpl()
	if err != nil {
		return err
	}
	err = zoneI.NewGatewayZoneServiceImpl()
	if err != nil {
		return err
	}
	err = epI.NewGatewayOpenapiServiceImpl()
	if err != nil {
		return err
	}
	if p.Register != nil {
		type domainService = pb.DomainServiceServer
		pb.RegisterDomainServiceImp(p.Register, p.domainService, apis.Options(), p.Perm.Check(
			perm.Method(domainService.GetOrgDomains, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(domainService.ChangeRuntimeDomains, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(domainService.GetRuntimeDomains, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(domainService.GetTenantDomains, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.NoPermMethod(domainService.ChangeInnerIngress),
		), common.AccessLogWrap(common.AccessLog))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.hepa.domain.DomainService" || ctx.Type() == pb.DomainServiceServerType() || ctx.Type() == pb.DomainServiceHandlerType():
		return p.domainService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.hepa.domain", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Dependencies: []string{
			"hepa",
			"erda.core.hepa.global.GlobalService",
		},
		Description: "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
