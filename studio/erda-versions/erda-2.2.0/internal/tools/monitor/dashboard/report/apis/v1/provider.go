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

package reportapisv1

import (
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	"github.com/erda-project/erda-infra/providers/mysql"
	notifyGrouppb "github.com/erda-project/erda-proto-go/core/messenger/notifygroup/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/tools/monitor/dashboard/report/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/bundle-ex/cmdb"
	"github.com/erda-project/erda/pkg/common/apis"
	perm "github.com/erda-project/erda/pkg/common/permission"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type config struct {
	Pipeline struct {
		Version       string `file:"version" default:"1.1"`
		ActionType    string `file:"action_type" default:"reportengine"`
		ActionVersion string `file:"action_version" default:"1.0"`
	} `file:"pipeline"`
	ReportCron struct {
		DailyCron   string `file:"daily_cron"`
		WeeklyCron  string `file:"weekly_cron"`
		MonthlyCron string `file:"monthly_cron"`
	} `file:"report_cron"`
	ClusterName   string `env:"DICE_CLUSTER_NAME" default:""`
	DiceProtocol  string `env:"DICE_PROTOCOL" default:"http"`
	DiceNameSpace string `file:"namespace" env:"DICE_NAMESPACE" default:"default"`
}

// +provider
type provider struct {
	Cfg                *config
	Log                logs.Logger
	Register           transport.Register `autowired:"service-register"`
	Perm               perm.Interface     `autowired:"permission"`
	bdl                *bundle.Bundle
	cmdb               *cmdb.Cmdb
	t                  i18n.Translator
	db                 *DB
	CronService        cronpb.CronServiceServer               `autowired:"erda.core.pipeline.cron.CronService" required:"true"`
	NotifyGroupService notifyGrouppb.NotifyGroupServiceServer `autowired:"erda.core.messenger.notifygroup.NotifyGroupService" required:"true"`
	reportService      *reportService
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	p.t = ctx.Service("i18n").(i18n.I18n).Translator("alert")
	p.db = newDB(ctx.Service("mysql").(mysql.Interface).DB())
	hc := httpclient.New(httpclient.WithTimeout(time.Second, time.Second*60))
	p.cmdb = cmdb.New(cmdb.WithHTTPClient(hc))
	bundleOpts := []bundle.Option{
		bundle.WithHTTPClient(hc),
		bundle.WithPipeline(),
		bundle.WithCoreServices(),
	}

	p.bdl = bundle.New(bundleOpts...)
	p.reportService = &reportService{
		p: p,
	}
	if p.Register != nil {
		pb.RegisterReportServiceImp(p.Register, p.reportService, apis.Options(), p.Perm.Check(
			perm.NoPermMethod(pb.ReportServiceServer.ListTasks),
			perm.NoPermMethod(pb.ReportServiceServer.CreateTask),
			perm.NoPermMethod(pb.ReportServiceServer.UpdateTask),
			perm.NoPermMethod(pb.ReportServiceServer.SwitchTask),
			perm.NoPermMethod(pb.ReportServiceServer.GetTask),
			perm.NoPermMethod(pb.ReportServiceServer.DeleteTask),

			perm.NoPermMethod(pb.ReportServiceServer.ListTypes),
			perm.NoPermMethod(pb.ReportServiceServer.ListHistories),
			perm.NoPermMethod(pb.ReportServiceServer.CreateHistory),
			perm.NoPermMethod(pb.ReportServiceServer.GetHistory),
			perm.NoPermMethod(pb.ReportServiceServer.DeleteHistory),
		))
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "monitor.dashboard.report.apis.v1.ReportService" || ctx.Type() == pb.ReportServiceClientType() || ctx.Type() == pb.ReportServiceHandlerType():
		return p.reportService
	}
	return p
}

func init() {
	servicehub.Register("monitor.dashboard.report.apis.v1", &servicehub.Spec{
		Services: []string{
			"monitor.dashboard.report.apis.v1-service",
		},
		Description: "here is description of monitor.dashboard.report.apis.v1",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
