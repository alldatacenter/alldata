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

package definition

import (
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/definition/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

// +provider
type provider struct {
	Cfg      *config
	MySQL    mysqlxorm.Interface `autowired:"mysql-xorm"`
	Register transport.Register  `autowired:"service-register" required:"true"`
	// implements
	pipelineDefinition *pipelineDefinition
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.pipelineDefinition = &pipelineDefinition{
		dbClient: &db.Client{Interface: p.MySQL},
	}
	if p.Register != nil {
		pb.RegisterDefinitionServiceImp(p.Register, p.pipelineDefinition, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.pipeline.definition" || ctx.Type() == pb.DefinitionServiceServerType() || ctx.Type() == pb.DefinitionServiceHandlerType():
		return p.pipelineDefinition
	}
	return p
}

func init() {
	servicehub.Register("erda.core.pipeline.definition", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		Dependencies:         []string{"mysql-xorm-client", "service-register"},
		OptionalDependencies: []string{},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
