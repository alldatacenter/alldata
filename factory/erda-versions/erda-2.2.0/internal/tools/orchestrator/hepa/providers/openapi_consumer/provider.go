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

package openapi_consumer

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/hepa/openapi_consumer/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/openapi_consumer/impl"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
}

// +provider
type provider struct {
	Cfg                    *config
	Log                    logs.Logger
	Register               transport.Register
	openapiConsumerService *openapiConsumerService
	Perm                   perm.Interface `autowired:"permission"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.openapiConsumerService = &openapiConsumerService{p}
	err := impl.NewGatewayOpenapiConsumerServiceImpl()
	if err != nil {
		return err
	}
	if p.Register != nil {
		type openapiConsumerService = pb.OpenapiConsumerServiceServer
		pb.RegisterOpenapiConsumerServiceImp(p.Register, p.openapiConsumerService, apis.Options(), p.Perm.Check(
			perm.Method(openapiConsumerService.CreateConsumer, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.DeleteConsumer, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.GetConsumerAcl, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.GetConsumerAuth, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.GetConsumers, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.GetConsumersName, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.GetEndpointAcl, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.GetEndpointApiAcl, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.UpdateConsumer, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.UpdateConsumerAcl, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.UpdateConsumerAuth, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.UpdateEndpointAcl, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(openapiConsumerService.UpdateEndpointApiAcl, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
		), common.AccessLogWrap(common.AccessLog))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.hepa.openapi_consumer.OpenapiConsumerService" || ctx.Type() == pb.OpenapiConsumerServiceServerType() || ctx.Type() == pb.OpenapiConsumerServiceHandlerType():
		return p.openapiConsumerService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.hepa.openapi_consumer", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Dependencies: []string{
			"hepa",
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
