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

package api_policy

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/hepa/api_policy/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/api_policy/impl"
	apiI "github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api/impl"
	zoneI "github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/zone/impl"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
}

// +provider
type provider struct {
	Cfg              *config
	Log              logs.Logger
	Register         transport.Register
	apiPolicyService *apiPolicyService
	Perm             perm.Interface `autowired:"permission"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	err := zoneI.NewGatewayZoneServiceImpl()
	if err != nil {
		return err
	}
	err = apiI.NewGatewayOpenapiServiceImpl()
	if err != nil {
		return err
	}
	err = impl.NewGatewayApiPolicyServiceImpl()
	if err != nil {
		return err
	}
	p.apiPolicyService = &apiPolicyService{p}
	if p.Register != nil {
		type policyService = pb.ApiPolicyServiceServer
		pb.RegisterApiPolicyServiceImp(p.Register, p.apiPolicyService, apis.Options(), p.Perm.Check(
			perm.Method(policyService.GetPolicy, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(policyService.SetPolicy, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
		), common.AccessLogWrap(common.AccessLog))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.hepa.api_policy.ApiPolicyService" || ctx.Type() == pb.ApiPolicyServiceServerType() || ctx.Type() == pb.ApiPolicyServiceHandlerType():
		return p.apiPolicyService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.hepa.api_policy", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Dependencies: []string{
			"hepa",
			"erda.core.hepa.global.GlobalService",
			"erda.core.hepa.domain.DomainService",
			"erda.core.hepa.openapi_rule.OpenapiRuleService",
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
