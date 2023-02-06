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

package action_runner_scheduler

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/mysql"
	"github.com/erda-project/erda-proto-go/core/pipeline/action_runner_scheduler/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/action_runner_scheduler/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/database/dbengine"
)

var (
	collectorLogPathPrefix = "/api/runner/collect/logs/"
)

type config struct {
	ClientID     string `env:"CLIENT_ID" default:"action-runner"`
	ClientSecret string `env:"CLIENT_SECRET" default:"devops/action-runner"`
	RunnerUserID string `env:"RUNNER_USER_ID" default:"1111"`
}

// +provider
type provider struct {
	Cfg               *config
	Log               logs.Logger
	Register          transport.Register
	MySQL             mysql.Interface
	bdl               *bundle.Bundle
	runnerTaskService *runnerTaskService
}

func (p *provider) Init(ctx servicehub.Context) error {
	bdl := bundle.New(bundle.WithCollector(), bundle.WithCoreServices(), bundle.WithOpenapi())
	p.bdl = bdl
	p.runnerTaskService = &runnerTaskService{
		p:        p,
		dbClient: &db.DBClient{&dbengine.DBEngine{p.MySQL.DB()}},
		bdl:      bdl,
	}
	if p.Register != nil {
		pb.RegisterRunnerTaskServiceImp(p.Register, p.runnerTaskService, apis.Options())
	}

	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.pipeline.action_runner_scheduler.RunnerTaskService" || ctx.Type() == pb.RunnerTaskServiceServerType() || ctx.Type() == pb.RunnerTaskServiceHandlerType():
		return p.runnerTaskService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.pipeline.action_runner_scheduler", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
