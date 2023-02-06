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

package reconciler

import (
	"context"
	"reflect"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cache"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/compensator"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgereporter"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler/taskpolicy"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/resourcegc"
	"github.com/erda-project/erda/internal/tools/pipeline/services/actionagentsvc"
)

type provider struct {
	Log logs.Logger
	Cfg *config

	MySQL           mysqlxorm.Interface
	LW              leaderworker.Interface
	Cache           cache.Interface
	TaskPolicy      taskpolicy.Interface
	ClusterInfo     clusterinfo.Interface
	EdgeRegister    edgepipeline_register.Interface
	ResourceGC      resourcegc.Interface
	CronCompensator compensator.Interface
	EdgeReporter    edgereporter.Interface
	ActionMgr       actionmgr.Interface

	dbClient *dbclient.Client
	bdl      *bundle.Bundle

	// legacy fields
	pipelineSvcFuncs *PipelineSvcFuncs
	actionAgentSvc   *actionagentsvc.ActionAgentSvc
}

type config struct {
	RetryInterval time.Duration `file:"retry_interval" default:"5s"`
}

func (r *provider) Init(ctx servicehub.Context) error {
	r.dbClient = &dbclient.Client{Engine: r.MySQL.DB()}
	r.bdl = bundle.New(bundle.WithAllAvailableClients())
	return nil
}

func (r *provider) Run(ctx context.Context) error {
	return nil
}

func init() {
	interfaceType := reflect.TypeOf((*Interface)(nil)).Elem()
	servicehub.Register("reconciler", &servicehub.Spec{
		Services:     []string{"reconciler"},
		Types:        []reflect.Type{interfaceType},
		Dependencies: nil,
		Description:  "pipeline reconciler",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
