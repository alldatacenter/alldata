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

package source

import (
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/source/db"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
}

// +provider
type Provider struct {
	Cfg            *config
	MySQL          mysqlxorm.Interface `autowired:"mysql-xorm"`
	Register       transport.Register  `autowired:"service-register" required:"true"`
	pipelineSource *pipelineSource
}

func (p *Provider) Init(ctx servicehub.Context) error {
	p.pipelineSource = &pipelineSource{
		dbClient: &db.Client{Interface: p.MySQL},
	}
	if p.Register != nil {
		pb.RegisterSourceServiceImp(p.Register, p.pipelineSource, apis.Options())
	}
	return nil
}

func (p *Provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.pipeline.source" || ctx.Type() == pb.SourceServiceServerType() || ctx.Type() == pb.SourceServiceHandlerType():
		return p.pipelineSource
	}
	return p
}

func init() {
	servicehub.Register("erda.core.pipeline.source", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		Dependencies:         []string{"mysql-xorm-client", "service-register"},
		OptionalDependencies: []string{},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &Provider{}
		},
	})
}
