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

package endpoint_api

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/hepa/endpoint_api/pb"
	_ "github.com/erda-project/erda-proto-go/core/project/client"
	projPb "github.com/erda-project/erda-proto-go/core/project/pb"
	_ "github.com/erda-project/erda-proto-go/orchestrator/runtime/client"
	runtimePb "github.com/erda-project/erda-proto-go/orchestrator/runtime/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	repositoryService "github.com/erda-project/erda/internal/tools/orchestrator/hepa/repository/service"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api/impl"
	zoneI "github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/zone/impl"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
}

// +provider
type provider struct {
	Cfg                *config
	Log                logs.Logger
	Register           transport.Register
	endpointApiService *endpointApiService
	Perm               perm.Interface                 `autowired:"permission"`
	ProjCli            projPb.ProjectServer           `autowired:"erda.core.project.Project"`
	RuntimeCli         runtimePb.RuntimeServiceServer `autowired:"erda.orchestrator.runtime.RuntimeService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	gatewayApiService, err := repositoryService.NewGatewayApiServiceImpl()
	if err != nil {
		return errors.Wrap(err, "failed to NewGatewayApiServiceImpl")
	}
	upstreamApiService, err := repositoryService.NewGatewayUpstreamApiServiceImpl()
	if err != nil {
		return errors.Wrap(err, "failed to NewGatewayUpstreamApiServiceImpl")
	}
	upstreamService, err := repositoryService.NewGatewayUpstreamServiceImpl()
	if err != nil {
		return errors.Wrap(err, "failed to NewGatewayUpstreamServiceImpl")
	}
	if p.ProjCli == nil {
		p.Log.Fatal("projCli is nil")
	}
	if p.RuntimeCli == nil {
		p.Log.Fatal("runtimeCli is nil")
	}
	p.endpointApiService = &endpointApiService{
		projCli:            p.ProjCli,
		runtimeCli:         p.RuntimeCli,
		gatewayApiService:  gatewayApiService,
		upstreamApiService: upstreamApiService,
		upstreamService:    upstreamService,
	}
	err = zoneI.NewGatewayZoneServiceImpl()
	if err != nil {
		return err
	}
	err = impl.NewGatewayOpenapiServiceImpl()
	if err != nil {
		return err
	}
	if p.Register != nil {
		type apiService = pb.EndpointApiServiceServer
		pb.RegisterEndpointApiServiceImp(p.Register, p.endpointApiService, apis.Options(), p.Perm.Check(
			perm.Method(apiService.ChangeEndpointRoot, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(apiService.CreateEndpoint, perm.ScopeProject, "project", perm.ActionGet, perm.FieldValue("ProjectId")),
			perm.Method(apiService.CreateEndpointApi, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(apiService.DeleteEndpoint, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(apiService.DeleteEndpointApi, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(apiService.GetEndpoint, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(apiService.GetEndpointApis, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(apiService.GetEndpoints, perm.ScopeProject, "project", perm.ActionGet, perm.FieldValue("ProjectId")),
			perm.Method(apiService.GetEndpointsName, perm.ScopeProject, "project", perm.ActionGet, perm.FieldValue("ProjectId")),
			perm.Method(apiService.UpdateEndpoint, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(apiService.UpdateEndpointApi, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(apiService.ListInvalidEndpointApi, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
		), common.AccessLogWrap(common.AccessLog))
	}
	return nil
}

//func (p *provider) Run(ctx context.Context) error {
//	go ticker.New(time.Hour*24, func() (bool, error) {
//		_, err := p.endpointApiService.ClearInvalidEndpointApi(ctx, new(commonPb.VoidRequest))
//		return false, err
//	}).Run()
//	return nil
//}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.hepa.endpoint_api.EndpointApiService" || ctx.Type() == pb.EndpointApiServiceServerType() || ctx.Type() == pb.EndpointApiServiceHandlerType():
		return p.endpointApiService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.hepa.endpoint_api", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Dependencies: []string{
			"hepa",
			"erda.core.hepa.api.ApiService",
			"erda.core.hepa.openapi_rule.OpenapiRuleService",
			"erda.core.hepa.openapi_consumer.OpenapiConsumerService",
			"erda.core.hepa.api_policy.ApiPolicyService",
			"erda.core.hepa.domain.DomainService",
			"erda.core.hepa.global.GlobalService",
			"erda.core.project.Project",
			"erda.orchestrator.runtime.RuntimeService",
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
