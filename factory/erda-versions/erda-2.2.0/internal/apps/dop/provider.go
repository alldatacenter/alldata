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

package dop

import (
	"embed"
	"os"
	"time"

	"github.com/coreos/etcd/clientv3"
	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/base/version"
	componentprotocol "github.com/erda-project/erda-infra/providers/component-protocol"
	"github.com/erda-project/erda-infra/providers/component-protocol/protocol"
	"github.com/erda-project/erda-infra/providers/etcd"
	"github.com/erda-project/erda-infra/providers/i18n"
	dashboardPb "github.com/erda-project/erda-proto-go/cmp/dashboard/pb"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	dicehubpb "github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	cmspb "github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	definitionpb "github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	sourcepb "github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	errboxpb "github.com/erda-project/erda-proto-go/core/services/errorbox/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	addonmysqlpb "github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/devflow/flow"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/metrics"
	"github.com/erda-project/erda/internal/apps/dop/providers/autotest/testplan"
	"github.com/erda-project/erda/internal/apps/dop/providers/cms"
	"github.com/erda-project/erda/internal/apps/dop/providers/devflowrule"
	"github.com/erda-project/erda/internal/apps/dop/providers/guide"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/core"
	issuequery "github.com/erda-project/erda/internal/apps/dop/providers/issue/core/query"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/stream"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/sync"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline"
	"github.com/erda-project/erda/internal/apps/dop/providers/qa/unittest"
	"github.com/erda-project/erda/internal/apps/dop/providers/taskerror"
	"github.com/erda-project/erda/internal/pkg/metrics/query"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/dumpstack"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

//go:embed component-protocol/scenarios
var scenarioFS embed.FS

type provider struct {
	Log logs.Logger

	PipelineCms           cmspb.CmsServiceServer                  `autowired:"erda.core.pipeline.cms.CmsService" optional:"true"`
	PipelineSource        sourcepb.SourceServiceServer            `autowired:"erda.core.pipeline.source.SourceService" required:"true"`
	PipelineDefinition    definitionpb.DefinitionServiceServer    `autowired:"erda.core.pipeline.definition.DefinitionService" required:"true"`
	TestPlanSvc           *testplan.TestPlanService               `autowired:"erda.core.dop.autotest.testplan.TestPlanService"`
	Cmp                   dashboardPb.ClusterResourceServer       `autowired:"erda.cmp.dashboard.resource.ClusterResource"`
	TaskErrorSvc          *taskerror.TaskErrorService             `autowired:"erda.core.dop.taskerror.TaskErrorService"`
	ErrorBoxSvc           errboxpb.ErrorBoxServiceServer          `autowired:"erda.core.services.errorbox.ErrorBoxService" optional:"true"`
	ProjectPipelineSvc    *projectpipeline.ProjectPipelineService `autowired:"erda.dop.projectpipeline.ProjectPipelineService"`
	PipelineCron          cronpb.CronServiceServer                `autowired:"erda.core.pipeline.cron.CronService" required:"true"`
	QueryClient           query.MetricQuery                       `autowired:"metricq-client"`
	CommentIssueStreamSvc *stream.CommentIssueStreamService       `autowired:"erda.dop.issue.stream.CommentIssueStreamService"`
	IssueSyncSvc          *sync.IssueSyncService                  `autowired:"erda.dop.issue.sync.IssueSyncService"`
	GuideSvc              *guide.GuideService                     `autowired:"erda.dop.guide.GuideService"`
	AddonMySQLSvc         addonmysqlpb.AddonMySQLServiceServer    `autowired:"erda.orchestrator.addon.mysql.AddonMySQLService"`
	DicehubReleaseSvc     dicehubpb.ReleaseServiceServer          `autowired:"erda.core.dicehub.release.ReleaseService"`
	CICDCmsSvc            *cms.CICDCmsService                     `autowired:"erda.dop.cms.CICDCmsService"`
	UnitTestService       *unittest.UnitTestService               `autowired:"erda.dop.qa.unittest.UnitTestService"`
	DevFlowRule           devflowrule.Interface
	TokenService          tokenpb.TokenServiceServer     `autowired:"erda.core.token.TokenService"`
	ClusterSvc            clusterpb.ClusterServiceServer `autowired:"erda.core.clustermanager.cluster.ClusterService"`
	DevFlowSvc            *flow.Service                  `autowired:"erda.apps.devflow.flow.FlowService"`
	IssueCoreSvc          *core.IssueService             `autowired:"erda.dop.issue.core.IssueCoreService"`
	Query                 issuequery.Interface

	Protocol      componentprotocol.Interface
	CPTran        i18n.I18n        `autowired:"i18n@cp"`
	ResourceTrans i18n.Translator  `translator:"resource-trans"`
	APIMTrans     i18n.Translator  `translator:"api-management-trans"`
	DB            *gorm.DB         `autowired:"mysql-client"`
	ETCD          etcd.Interface   // autowired
	EtcdClient    *clientv3.Client // autowired
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.Log.Info("init dop")

	// component-protocol
	p.Log.Info("init component-protocol")
	p.Protocol.SetI18nTran(p.CPTran) // use custom i18n translator
	// compatible for legacy protocol context bundle

	metrics.Client = p.QueryClient

	bdl.Init(
		// bundle.WithDOP(), // TODO change to internal method invoke in component-protocol
		bundle.WithHepa(),
		bundle.WithOrchestrator(),
		bundle.WithGittar(),
		bundle.WithPipeline(),
		bundle.WithMonitor(),
		bundle.WithCollector(),
		bundle.WithKMS(),
		bundle.WithCoreServices(),
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second, time.Duration(conf.BundleTimeoutSecond())*time.Second),
				httpclient.WithEnableAutoRetry(false),
			)),
		// TODO remove it after internal bundle invoke inside cp issue-manage adjusted
		bundle.WithCustom(discover.EnvDOP, "localhost:9527"),
	)
	p.Protocol.WithContextValue(types.GlobalCtxKeyBundle, bdl.Bdl)
	protocol.MustRegisterProtocolsFromFS(scenarioFS)
	p.Log.Info("init component-protocol done")

	p.Protocol.WithContextValue(types.AddonMySQLService, p.AddonMySQLSvc)
	p.Protocol.WithContextValue(types.DicehubReleaseService, p.DicehubReleaseSvc)

	logrus.SetFormatter(&logrus.TextFormatter{
		ForceColors:     true,
		FullTimestamp:   true,
		TimestampFormat: "2006-01-02 15:04:05.000000000",
	})
	logrus.SetOutput(os.Stdout)

	dumpstack.Open()
	logrus.Infoln(version.String())

	return p.Initialize(ctx)
}

func init() {
	servicehub.Register("dop", &servicehub.Spec{
		Services:     []string{"dop"},
		Dependencies: []string{"etcd"},
		Creator:      func() servicehub.Provider { return &provider{} },
	})
}
