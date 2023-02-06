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

package legacy_consumer

import (
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/hepa/consumer/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/services/legacy_consumer/impl"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
}

// +provider
type provider struct {
	Cfg                   *config
	Register              transport.Register
	legacyConsumerService *legacyConsumerService
	Perm                  perm.Interface `autowired:"permission"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	err := impl.NewGatewayConsumerServiceImpl()
	if err != nil {
		return err
	}
	p.legacyConsumerService = &legacyConsumerService{p}
	if p.Register != nil {
		type legacyConsumerService = pb.LegacyConsumerServiceServer
		pb.RegisterLegacyConsumerServiceImp(p.Register, p.legacyConsumerService, apis.Options(), p.Perm.Check(
			perm.Method(legacyConsumerService.GetConsumer, perm.ScopeProject, "project", perm.ActionGet, perm.FieldValue("ProjectId")),
		), common.AccessLogWrap(common.AccessLog))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.hepa.consumer.LegacyConsumerService" || ctx.Type() == pb.LegacyConsumerServiceServerType() || ctx.Type() == pb.LegacyConsumerServiceHandlerType():
		return p.legacyConsumerService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.hepa.consumer", &servicehub.Spec{
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
