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

package global

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda-proto-go/core/hepa/global/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	apiI "github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/endpoint_api/impl"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/global/impl"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

// +provider
type provider struct {
	Cfg           *config
	Log           logs.Logger
	Register      transport.Register
	ClusterSvc    clusterpb.ClusterServiceServer `autowired:"erda.core.clustermanager.cluster.ClusterService"`
	TenantSvc     tenantpb.TenantServiceServer   `autowired:"erda.msp.tenant.TenantService"`
	globalService *globalService
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.globalService = &globalService{p}
	err := apiI.NewGatewayOpenapiServiceImpl()
	if err != nil {
		return err
	}
	err = impl.NewGatewayGlobalServiceImpl(p.ClusterSvc, p.TenantSvc)
	if err != nil {
		return err
	}
	if p.Register != nil {
		pb.RegisterGlobalServiceImp(p.Register, p.globalService, apis.Options(), common.AccessLogWrap(common.AccessLog))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.hepa.global.GlobalService" || ctx.Type() == pb.GlobalServiceServerType() || ctx.Type() == pb.GlobalServiceHandlerType():
		return p.globalService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.hepa.global", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		Dependencies: []string{
			"hepa",
		},
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
