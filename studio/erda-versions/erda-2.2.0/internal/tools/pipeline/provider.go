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

package pipeline

import (
	"context"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	_ "github.com/erda-project/erda-infra/providers/etcd"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda/internal/pkg/metrics/report"
	_ "github.com/erda-project/erda/internal/tools/pipeline/aop/plugins"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cache"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cancel"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/compensator"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/daemon"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/dbgc"
	_ "github.com/erda-project/erda/internal/tools/pipeline/providers/dispatcher"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgereporter"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/engine"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/queuemanager"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/reconciler"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/resourcegc"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/run"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/secret"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/user"
)

type provider struct {
	CmsService     pb.CmsServiceServer      `autowired:"erda.core.pipeline.cms.CmsService"`
	MetricReport   report.MetricReport      `autowired:"metric-report-client" optional:"true"`
	Router         httpserver.Router        `autowired:"http-router"`
	CronService    cronpb.CronServiceServer `autowired:"erda.core.pipeline.cron.CronService" required:"true"`
	CronDaemon     daemon.Interface
	CronCompensate compensator.Interface
	MySQL          mysqlxorm.Interface `autowired:"mysql-xorm"`

	Engine       engine.Interface
	QueueManager queuemanager.Interface
	Reconciler   reconciler.Interface
	EdgePipeline edgepipeline.Interface
	EdgeRegister edgepipeline_register.Interface
	EdgeReporter edgereporter.Interface
	LeaderWorker leaderworker.Interface
	ClusterInfo  clusterinfo.Interface
	DBGC         dbgc.Interface
	ResourceGC   resourcegc.Interface
	Cache        cache.Interface
	PipelineRun  run.Interface
	Cancel       cancel.Interface
	User         user.Interface
	Secret       secret.Interface
	ActionMgr    actionmgr.Interface
}

func (p *provider) Run(ctx context.Context) error {
	logrus.Infof("[alert] starting pipeline instance")
	var err error

	select {
	case <-ctx.Done():
	}
	return err
}

func init() {
	servicehub.Register("pipeline", &servicehub.Spec{
		Services:     []string{"pipeline"},
		Dependencies: []string{"etcd"},
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
