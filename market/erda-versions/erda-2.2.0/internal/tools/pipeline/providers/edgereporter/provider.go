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

package edgereporter

import (
	"context"
	"fmt"
	"reflect"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/safe"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgereporter/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker"
)

type config struct {
	Compensator compensatorConfig `file:"compensator"`
	Target      targetConfig      `file:"target"`
}

type targetConfig struct {
	URL       string `file:"url" env:"EDGE_REPORTER_TARGET_URL"`
	AuthToken string `file:"auth_token" env:"EDGE_REPORTER_TARGET_AUTH_TOKEN"`
}

type compensatorConfig struct {
	Interval time.Duration `file:"interval" env:"EDGE_REPORTER_COMPENSATOR_INTERVAL" default:"2h"`
}

type provider struct {
	Cfg *config
	Log logs.Logger

	LW           leaderworker.Interface
	MySQL        mysqlxorm.Interface
	EdgeRegister edgepipeline_register.Interface
	Cron         cronpb.CronServiceServer

	bdl      *bundle.Bundle
	dbClient *db.Client
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithAllAvailableClients())
	p.dbClient = &db.Client{Client: &dbclient.Client{Engine: p.MySQL.DB()}}

	// target config
	if p.EdgeRegister.IsEdge() {
		// url
		if len(p.Cfg.Target.URL) == 0 {
			return fmt.Errorf("missing target url")
		}
		p.Log.Infof("target url: %s", p.Cfg.Target.URL)

		// token
		if len(p.Cfg.Target.AuthToken) > 0 {
			p.Log.Infof("target auth token: %s", p.Cfg.Target.AuthToken)
		} else {
			p.Log.Infof("target auth token not set, get from edge register when used")
		}
	}
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	if p.EdgeRegister.IsEdge() {
		safe.Go(func() { p.pipelineReporter(ctx) })
		safe.Go(func() { p.taskReporter(ctx) })
		safe.Go(func() { p.cronReporter(ctx) })
		p.LW.OnLeader(p.compensatorPipelineReporter)
	}
	return nil
}

func init() {
	servicehub.Register("edge-reporter", &servicehub.Spec{
		Services:     []string{"edge-reporter"},
		Types:        []reflect.Type{reflect.TypeOf((*Interface)(nil)).Elem()},
		Dependencies: nil,
		Description:  "pipeline edge reporter",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
