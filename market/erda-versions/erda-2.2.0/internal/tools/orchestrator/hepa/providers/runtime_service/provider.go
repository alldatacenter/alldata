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

package runtime_service

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/hepa/runtime_service/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/runtime_service/impl"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
}

// +provider
type provider struct {
	Cfg            *config
	Log            logs.Logger
	Register       transport.Register
	runtimeService *runtimeService
	Perm           perm.Interface `autowired:"permission"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.runtimeService = &runtimeService{p}
	err := impl.NewGatewayRuntimeServiceServiceImpl()
	if err != nil {
		return err
	}
	if p.Register != nil {
		type runtimeService = pb.RuntimeServiceServer
		pb.RegisterRuntimeServiceImp(p.Register, p.runtimeService, apis.Options(), p.Perm.Check(
			perm.NoPermMethod(runtimeService.ChangeRuntime),
			perm.NoPermMethod(runtimeService.DeleteRuntime),
			perm.Method(runtimeService.GetApps, perm.ScopeOrg, "org", perm.ActionGet, perm.OrgIDValue()),
			perm.Method(runtimeService.GetServiceRuntimes, perm.ScopeProject, "project", perm.ActionGet, perm.FieldValue("ProjectId")),
			perm.Method(runtimeService.GetServiceApiPrefix, perm.ScopeProject, "project", perm.ActionGet, perm.FieldValue("ProjectId")),
		), common.AccessLogWrap(common.AccessLog))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.hepa.runtime_service.RuntimeService" || ctx.Type() == pb.RuntimeServiceServerType() || ctx.Type() == pb.RuntimeServiceHandlerType():
		return p.runtimeService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.hepa.runtime_service", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Dependencies: []string{
			"hepa",
			"erda.core.hepa.api.ApiService",
			"erda.core.hepa.domain.DomainService",
			"erda.core.hepa.endpoint_api.EndpointApiService",
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
