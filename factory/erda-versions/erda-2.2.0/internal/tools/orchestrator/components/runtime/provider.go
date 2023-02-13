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

package runtime

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda-proto-go/orchestrator/runtime/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

// +provider
type provider struct {
	Cfg          *config
	Logger       logs.Logger
	Register     transport.Register
	DB           *gorm.DB                       `autowired:"mysql-client"`
	EventManager *events.EventManager           `autowired:"erda.orchestrator.events.event-manager"`
	ClusterSvc   clusterpb.ClusterServiceServer `autowired:"erda.core.clustermanager.cluster.ClusterService"`

	runtimeService pb.RuntimeServiceServer
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.runtimeService = NewRuntimeService(
		WithBundleService(NewBundleService()),
		WithDBService(NewDBService(p.DB)),
		WithEventManagerService(p.EventManager),
		WithServiceGroupImpl(servicegroup.NewServiceGroupImplInit()),
		WithClusterSvc(p.ClusterSvc),
	)

	if p.Register != nil {
		pb.RegisterRuntimeServiceImp(p.Register, p.runtimeService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.orchestrator.runtime.RuntimeService" || ctx.Type() == pb.RuntimeServiceServerType() || ctx.Type() == pb.RuntimeServiceHandlerType():
		return p.runtimeService
	}

	return p
}

func init() {
	servicehub.Register("erda.orchestrator.runtime", &servicehub.Spec{
		Services: append(pb.ServiceNames()),
		Types:    pb.Types(),
		OptionalDependencies: []string{
			"erda.orchestrator.events",
			"service-register",
			"mysql",
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
