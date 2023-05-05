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

package apis

import (
	"context"

	"github.com/go-redis/redis"
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	projectpb "github.com/erda-project/erda-proto-go/msp/tenant/project/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage/cache"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage/db"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
)

type config struct {
	CacheKey string `file:"cache_key" default:"checkers"`
}

// +provider
type provider struct {
	DB            *gorm.DB
	Cfg           *config
	Log           logs.Logger
	Register      transport.Register             `autowired:"service-register" optional:"true"`
	Metric        metricpb.MetricServiceServer   `autowired:"erda.core.monitor.metric.MetricService"`
	ProjectServer projectpb.ProjectServiceServer `autowired:"erda.msp.tenant.project.ProjectService"`
	Redis         *redis.Client                  `autowired:"redis-client"`
	I18n          i18n.Translator                `autowired:"i18n" translator:"msp-i18n"`
	Perm          perm.Interface                 `autowired:"permission"`
	audit         audit.Auditor

	// implements
	checkerService   *checkerService
	checkerV1Service *checkerV1Service
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.audit = audit.GetAuditor(ctx)
	cache := cache.New(p.Cfg.CacheKey, p.Redis)

	p.checkerService = &checkerService{p}
	p.checkerV1Service = &checkerV1Service{
		p:             p,
		metricq:       p.Metric,
		projectServer: p.ProjectServer,
		projectDB:     &db.ProjectDB{DB: p.DB},
		metricDB:      &db.MetricDB{DB: p.DB},
		cache:         cache,
	}
	if p.Register != nil {
		pb.RegisterCheckerServiceImp(p.Register, p.checkerService, apis.Options())

		type CheckerServiceV1 = pb.CheckerV1ServiceServer
		pb.RegisterCheckerV1ServiceImp(p.Register, p.checkerV1Service, apis.Options(),
			p.Perm.Check(
				perm.Method(CheckerServiceV1.CreateCheckerV1, perm.ScopeProject, "monitor_status", perm.ActionCreate, perm.FieldValue("Data.ProjectID")),
				perm.Method(CheckerServiceV1.UpdateCheckerV1, perm.ScopeProject, "monitor_status", perm.ActionUpdate, p.checkerV1Service.GetProjectFromMetricID()),
				perm.Method(CheckerServiceV1.DeleteCheckerV1, perm.ScopeProject, "monitor_status", perm.ActionDelete, p.checkerV1Service.GetProjectFromMetricID()),
				perm.Method(CheckerServiceV1.DescribeCheckersV1, perm.ScopeProject, "monitor_status", perm.ActionList, perm.FieldValue("ProjectID")),
				perm.Method(CheckerServiceV1.DescribeCheckerV1, perm.ScopeProject, "monitor_status", perm.ActionList, p.checkerV1Service.GetProjectFromMetricID()),
				perm.Method(CheckerServiceV1.GetCheckerStatusV1, perm.ScopeProject, "monitor_status", perm.ActionGet, p.checkerV1Service.GetProjectFromMetricID()),
				perm.Method(CheckerServiceV1.GetCheckerIssuesV1, perm.ScopeProject, "monitor_status", perm.ActionGet, p.checkerV1Service.GetProjectFromMetricID()),
			),
			p.audit.Audit(
				audit.Method(CheckerServiceV1.CreateCheckerV1, audit.ProjectScope, "createInitiativeMonitor",
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := req.(*pb.CreateCheckerV1Request)
						return r.Data.ProjectID, map[string]interface{}{
							"metricName": r.Data.Name,
						}, nil
					},
				),
				audit.Method(CheckerServiceV1.UpdateCheckerV1, audit.ProjectScope, "updateInitiativeMonitor",
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := req.(*pb.UpdateCheckerV1Request)
						return r.Data.ProjectID, map[string]interface{}{
							"metricName": r.Data.Name,
						}, nil
					},
				),
				audit.Method(CheckerServiceV1.DeleteCheckerV1, audit.ProjectScope, "deleteInitiativeMonitor",
					func(ctx context.Context, req, resp interface{}, err error) (interface{}, map[string]interface{}, error) {
						r := resp.(*pb.DeleteCheckerV1Response)
						return r.Data.ProjectID, map[string]interface{}{
							"metricName": r.Data.Name,
						}, nil
					},
				),
			),
		)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.apm.checker.CheckerService" || ctx.Type() == pb.CheckerServiceServerType() || ctx.Type() == pb.CheckerServiceHandlerType():
		return p.checkerService
	case ctx.Service() == "erda.msp.apm.checker.CheckerV1Service" || ctx.Type() == pb.CheckerV1ServiceServerType() || ctx.Type() == pb.CheckerV1ServiceHandlerType():
		return p.checkerV1Service
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.apm.checker", &servicehub.Spec{
		Services: pb.ServiceNames(),
		Types:    pb.Types(),
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
