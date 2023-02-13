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

package cron

import (
	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/daemon"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker"
)

type config struct {
}

// +provider
type provider struct {
	Cfg      *config
	Log      logs.Logger
	Register transport.Register

	MySQL        mysqlxorm.Interface    `autowired:"mysql-xorm"`
	LeaderWorker leaderworker.Interface `autowired:"leader-worker"`

	Daemon               daemon.Interface
	dbClient             *db.Client
	EdgePipelineRegister edgepipeline_register.Interface
}

func (s *provider) Init(ctx servicehub.Context) error {
	s.dbClient = &db.Client{Interface: s.MySQL}
	pb.RegisterCronServiceImp(s.Register, s)
	return nil
}

func (s *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	return s
}

func init() {
	servicehub.Register("erda.core.pipeline.cron", &servicehub.Spec{
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
