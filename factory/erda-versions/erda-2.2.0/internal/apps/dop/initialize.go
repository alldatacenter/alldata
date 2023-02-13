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
	"context"
	"fmt"
	"net/url"
	"time"

	"github.com/aliyun/aliyun-oss-go-sdk/oss"
	"github.com/google/uuid"
	"github.com/gorilla/schema"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/wrapperspb"

	"github.com/erda-project/erda-infra/base/servicehub"
	infrahttpserver "github.com/erda-project/erda-infra/providers/httpserver"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/bdl"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	"github.com/erda-project/erda/internal/apps/dop/conf"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/endpoints"
	"github.com/erda-project/erda/internal/apps/dop/event"
	"github.com/erda-project/erda/internal/apps/dop/services/apidocsvc"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/internal/apps/dop/services/appcertificate"
	"github.com/erda-project/erda/internal/apps/dop/services/application"
	"github.com/erda-project/erda/internal/apps/dop/services/assetsvc"
	"github.com/erda-project/erda/internal/apps/dop/services/autotest"
	atv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
	"github.com/erda-project/erda/internal/apps/dop/services/branchrule"
	"github.com/erda-project/erda/internal/apps/dop/services/cdp"
	"github.com/erda-project/erda/internal/apps/dop/services/certificate"
	"github.com/erda-project/erda/internal/apps/dop/services/code_coverage"
	"github.com/erda-project/erda/internal/apps/dop/services/comment"
	"github.com/erda-project/erda/internal/apps/dop/services/cq"
	"github.com/erda-project/erda/internal/apps/dop/services/environment"
	"github.com/erda-project/erda/internal/apps/dop/services/filetree"
	"github.com/erda-project/erda/internal/apps/dop/services/issue"
	"github.com/erda-project/erda/internal/apps/dop/services/issuefilterbm"
	"github.com/erda-project/erda/internal/apps/dop/services/issuestate"
	"github.com/erda-project/erda/internal/apps/dop/services/iteration"
	"github.com/erda-project/erda/internal/apps/dop/services/libreference"
	"github.com/erda-project/erda/internal/apps/dop/services/migrate"
	"github.com/erda-project/erda/internal/apps/dop/services/namespace"
	"github.com/erda-project/erda/internal/apps/dop/services/nexussvc"
	"github.com/erda-project/erda/internal/apps/dop/services/org"
	"github.com/erda-project/erda/internal/apps/dop/services/permission"
	"github.com/erda-project/erda/internal/apps/dop/services/pipeline"
	"github.com/erda-project/erda/internal/apps/dop/services/project"
	"github.com/erda-project/erda/internal/apps/dop/services/projectpipelinefiletree"
	"github.com/erda-project/erda/internal/apps/dop/services/publish_item"
	"github.com/erda-project/erda/internal/apps/dop/services/publisher"
	"github.com/erda-project/erda/internal/apps/dop/services/sceneset"
	"github.com/erda-project/erda/internal/apps/dop/services/sonar_metric_rule"
	"github.com/erda-project/erda/internal/apps/dop/services/test_report"
	"github.com/erda-project/erda/internal/apps/dop/services/testcase"
	"github.com/erda-project/erda/internal/apps/dop/services/testplan"
	"github.com/erda-project/erda/internal/apps/dop/services/testset"
	"github.com/erda-project/erda/internal/apps/dop/services/ticket"
	"github.com/erda-project/erda/internal/apps/dop/services/workbench"
	"github.com/erda-project/erda/internal/apps/dop/utils"
	"github.com/erda-project/erda/pkg/cron"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

const (
	EtcdPipelineCmsCompensate = "dop/pipelineCms/compensate"
	EtcdIssueStateCompensate  = "dop/issueState/compensate"
)

// Initialize 初始化应用启动服务.
func (p *provider) Initialize(ctx servicehub.Context) error {
	conf.Load()
	if conf.Debug() {
		logrus.SetLevel(logrus.DebugLevel)
		logrus.Infof("set log level: %s", logrus.DebugLevel)
	}

	// TODO invoke self use service
	//_ = os.Setenv("QA_ADDR", discover.QA())

	db := &dao.DBClient{
		DBEngine: &dbengine.DBEngine{
			DB: p.DB,
		},
	}
	dbclient.Set(db.DBEngine)
	ep, err := p.initEndpoints(db)
	if err != nil {
		return err
	}

	registerWebHook(bdl.Bdl)

	if err = deleteWebhook(bdl.Bdl); err != nil {
		logrus.Errorf("failed to delete webhook, err: %v", err)
	}

	// 注册 hook
	if err := ep.RegisterEvents(); err != nil {
		return err
	}

	p.Protocol.WithContextValue(types.IssueFilterBmService, issuefilterbm.New(
		issuefilterbm.WithDBClient(db),
	))
	p.Protocol.WithContextValue(types.CodeCoverageService, ep.CodeCoverageService())
	p.Protocol.WithContextValue(types.IssueService, p.Query)
	p.Protocol.WithContextValue(types.IterationService, ep.IterationService())
	p.Protocol.WithContextValue(types.ManualTestCaseService, ep.ManualTestCaseService())
	p.Protocol.WithContextValue(types.ManualTestPlanService, ep.ManualTestPlanService())
	p.Protocol.WithContextValue(types.AutoTestPlanService, ep.AutoTestPlanService())
	p.Protocol.WithContextValue(types.DBClient, ep.DBClient())
	p.Protocol.WithContextValue(types.ProjectPipelineService, p.ProjectPipelineSvc)
	p.Protocol.WithContextValue(types.PipelineCronService, p.PipelineCron)
	p.Protocol.WithContextValue(types.GuideService, p.GuideSvc)

	// This server will never be started. Only the routes and locale loader are used by new http server
	server := httpserver.New(":0")
	server.Router().UseEncodedPath()
	server.RegisterEndpoint(ep.Routes())
	// server.Router().Path("/metrics").Methods(http.MethodGet).Handler(promxp.Handler("cmdb"))
	server.WithLocaleLoader(bdl.Bdl.GetLocaleLoader())
	server.Router().PathPrefix("/api/apim/metrics").Handler(endpoints.InternalReverseHandler(endpoints.ProxyMetrics))
	ctx.Service("http-server").(infrahttpserver.Router).Any("/**", server.Router())

	loadMetricKeysFromDb(db)

	logrus.Infof("start the service and listen on address: \"%s\"", conf.ListenAddr())

	interval := time.Duration(conf.TestFileIntervalSec())
	purgeCycle := conf.TestFileRecordPurgeCycleDay()
	if err := ep.TestCaseService().BatchClearProcessingRecords(); err != nil {
		logrus.Error(err)
		return err
	}
	// Scheduled polling export task
	go func() {
		ticker := time.NewTicker(time.Second * interval)
		for {
			select {
			case <-ticker.C:
				p.exportTestFileTask(ep)
			case <-ep.ExportChannel:
				p.exportTestFileTask(ep)
			}
		}
	}()

	// Scheduled polling import task
	go func() {
		ticker := time.NewTicker(time.Second * interval)
		for {
			select {
			case <-ticker.C:
				p.importTestFileTask(ep)
			case <-ep.ImportChannel:
				p.importTestFileTask(ep)
			}
		}
	}()

	// Scheduled polling copy task
	go func() {
		ticker := time.NewTicker(time.Second * interval)
		for {
			select {
			case <-ticker.C:
				copyTestFileTask(ep)
			case <-ep.CopyChannel:
				copyTestFileTask(ep)
			}
		}
	}()

	// Daily clear test file records
	go func() {
		day := time.NewTicker(time.Hour * 24 * time.Duration(purgeCycle))
		for {
			select {
			case <-day.C:
				if err := ep.TestCaseService().DeleteRecordApiFilesByTime(time.Now().AddDate(0, 0, -purgeCycle)); err != nil {
					logrus.Error(err)
				}
			}
		}
	}()

	// compensate pipeline cms according to pipeline cron which enable is true
	go func() {
		// add etcd lock to ensure that it is executed only once
		resp, err := p.EtcdClient.Get(context.Background(), EtcdPipelineCmsCompensate)
		if err != nil {
			logrus.Error(err)
			return
		}
		if len(resp.Kvs) == 0 {
			logrus.Infof("start compensate pipelineCms")
			if err = p.compensatePipelineCms(ep); err != nil {
				logrus.Error(err)
			}
			_, err = p.EtcdClient.Put(context.Background(), EtcdPipelineCmsCompensate, "true")
			if err != nil {
				logrus.Error(err)
			}
		}
	}()

	// compensate issue state transition
	go func() {
		// add etcd lock to ensure that it is executed only once
		resp, err := p.EtcdClient.Get(context.Background(), EtcdIssueStateCompensate)
		if err != nil {
			logrus.Error(err)
			return
		}
		if len(resp.Kvs) == 0 {
			_, err = p.EtcdClient.Put(context.Background(), EtcdIssueStateCompensate, "true")
			if err != nil {
				logrus.Error(err)
			}
			logrus.Infof("start compensate issue state transition")
			if err = compensateIssueStateCirculation(ep); err != nil {
				logrus.Error(err)
				_, err = p.EtcdClient.Delete(context.Background(), EtcdIssueStateCompensate)
				if err != nil {
					logrus.Error(err)
				}
				return
			}
		}
	}()

	// instantly run once
	updateIssueExpiryStatus(ep)

	// daily issue expiry status update cron job
	go func() {
		cron := cron.New()
		err := cron.AddFunc(conf.UpdateIssueExpiryStatusCron(), func() {
			updateIssueExpiryStatus(ep)
		})
		if err != nil {
			panic(err)
		}
		cron.Start()
	}()

	go func() {
		if err := updateMemberContribution(ep.DBClient()); err != nil {
			p.Log.Error(err)
		}
		cron := cron.New()
		err := cron.AddFunc(conf.UpdateMemberActiveRankCron(), func() {
			updateMemberContribution(ep.DBClient())
		})
		if err != nil {
			p.Log.Error(err)
		}
		cron.Start()
	}()

	return nil
}

func updateMemberContribution(db *dao.DBClient) error {
	if err := db.BatchClearScore(); err != nil {
		return err
	}
	if err := db.IssueScore(); err != nil {
		return err
	}
	if err := db.CommitScore(); err != nil {
		return err
	}
	return db.QualityScore()
}

func updateIssueExpiryStatus(ep *endpoints.Endpoints) {
	start := time.Now()
	if err := ep.DBClient().BatchUpdateIssueExpiryStatus(apistructs.StateBelongs); err != nil {
		logrus.Errorf("daily issue expiry status batch update err: %v", err)
		return
	}
	logrus.Infof("daily issue expiry status batch update takes %v", time.Since(start))
}

func (p *provider) initEndpoints(db *dao.DBClient) (*endpoints.Endpoints, error) {
	var (
		etcdStore *etcd.Store
		ossClient *oss.Client
		store     jsonstore.JsonStore
	)

	etcdStore, err := etcd.New()
	if err != nil {
		return nil, err
	}

	if utils.IsOSS(conf.AvatarStorageURL()) {
		url, err := url.Parse(conf.AvatarStorageURL())
		if err != nil {
			return nil, err
		}
		appSecret, _ := url.User.Password()
		ossClient, err = oss.New(url.Host, url.User.Username(), appSecret)
		if err != nil {
			return nil, err
		}
	}

	store, err = jsonstore.New()
	if err != nil {
		return nil, err
	}

	c := cdp.New(cdp.WithBundle(bdl.Bdl), cdp.WithResourceTranslator(p.ResourceTrans))

	// init event
	e := event.New(event.WithBundle(bdl.Bdl))

	// queryStringDecoder
	queryStringDecoder := schema.NewDecoder()
	queryStringDecoder.IgnoreUnknownKeys(true)

	testCaseSvc := testcase.New(
		testcase.WithDBClient(db),
		testcase.WithBundle(bdl.Bdl),
	)
	testSetSvc := testset.New(
		testset.WithDBClient(db),
		testset.WithBundle(bdl.Bdl),
		testset.WithTestCaseService(testCaseSvc),
	)
	testCaseSvc.CreateTestSetFn = testSetSvc.Create

	autotest := autotest.New(autotest.WithDBClient(db),
		autotest.WithBundle(bdl.Bdl),
		autotest.WithPipelineCms(p.PipelineCms),
	)

	sceneset := sceneset.New(
		sceneset.WithDBClient(db),
		sceneset.WithBundle(bdl.Bdl),
	)

	autotestV2 := atv2.New(
		atv2.WithDBClient(db),
		atv2.WithBundle(bdl.Bdl),
		atv2.WithSceneSet(sceneset),
		atv2.WithAutotestSvc(autotest),
		atv2.WithPipelineCms(p.PipelineCms),
	)

	autotestV2.UpdateFileRecord = testCaseSvc.UpdateFileRecord
	autotestV2.CreateFileRecord = testCaseSvc.CreateFileRecord

	p.TestPlanSvc.WithAutoTestSvc(autotestV2)
	p.TaskErrorSvc.WithErrorBoxSvc(p.ErrorBoxSvc)

	sceneset.GetScenes = autotestV2.ListAutotestScene
	sceneset.CopyScene = autotestV2.CopyAutotestScene

	sonarMetricRule := sonar_metric_rule.New(
		sonar_metric_rule.WithDBClient(db),
		sonar_metric_rule.WithBundle(bdl.Bdl),
	)

	migrateSvc := migrate.New(migrate.WithDBClient(db))

	// 初始化UC Client
	uc := ucauth.NewUCClient(discover.UC(), conf.UCClientID(), conf.UCClientSecret())
	if conf.OryEnabled() {
		uc = ucauth.NewUCClient(conf.OryKratosPrivateAddr(), conf.OryCompatibleClientID(), conf.OryCompatibleClientSecret())
		uc.SetDBClient(db.DB)
	}

	p.IssueCoreSvc.WithUc(uc)

	// init ticket service
	t := ticket.New(ticket.WithDBClient(db),
		ticket.WithBundle(bdl.Bdl),
	)

	// init comment service
	com := comment.New(
		comment.WithDBClient(db),
		comment.WithBundle(bdl.Bdl),
	)

	ns := namespace.New(
		namespace.WithDBClient(db),
		namespace.WithBundle(bdl.Bdl),
	)

	branchRule := branchrule.New(
		branchrule.WithDBClient(db),
		branchrule.WithBundle(bdl.Bdl),
		branchrule.WithDevFlowRule(p.DevFlowRule),
	)
	gittarFileTreeSvc := filetree.New(filetree.WithBundle(bdl.Bdl), filetree.WithBranchRule(branchRule))

	// 查询
	pFileTree := projectpipelinefiletree.New(
		projectpipelinefiletree.WithBundle(bdl.Bdl),
		projectpipelinefiletree.WithFileTreeSvc(gittarFileTreeSvc),
		projectpipelinefiletree.WithAutoTestSvc(autotest),
	)

	// init permission
	perm := permission.New(permission.WithBundle(bdl.Bdl), permission.WithBranchRule(branchRule))

	filetreeSvc := apidocsvc.New(
		apidocsvc.WithBranchRuleSvc(branchRule),
		apidocsvc.WithTrans(p.APIMTrans),
	)

	env := environment.New(
		environment.WithDBClient(db),
		environment.WithBundle(bdl.Bdl),
	)

	issue := issue.New(
		issue.WithDBClient(db),
		issue.WithBundle(bdl.Bdl),
	)

	issueState := issuestate.New(
		issuestate.WithDBClient(db),
		issuestate.WithBundle(bdl.Bdl),
	)

	itr := iteration.New(
		iteration.WithDBClient(db),
		iteration.WithIssue(issue),
	)

	testPlan := testplan.New(
		testplan.WithDBClient(db),
		testplan.WithBundle(bdl.Bdl),
		testplan.WithTestCase(testCaseSvc),
		testplan.WithTestSet(testSetSvc),
		testplan.WithAutoTest(autotest),
		testplan.WithIssue(issue),
		testplan.WithIssueState(issueState),
		testplan.WithIterationSvc(itr),
	)

	p.IssueCoreSvc.WithTestplan(testPlan)
	p.IssueCoreSvc.WithTestcase(testCaseSvc)

	workBench := workbench.New(
		workbench.WithBundle(bdl.Bdl),
		workbench.WithIssue(issue),
	)

	rsaCrypt := encryption.NewRSAScrypt(encryption.RSASecret{
		PublicKey:          conf.Base64EncodedRsaPublicKey(),
		PublicKeyDataType:  encryption.Base64,
		PrivateKey:         conf.Base64EncodedRsaPrivateKey(),
		PrivateKeyDataType: encryption.Base64,
		PrivateKeyType:     encryption.PKCS1,
	})

	// init nexus service
	nexusSvc := nexussvc.New(
		nexussvc.WithDBClient(db),
		nexussvc.WithBundle(bdl.Bdl),
		nexussvc.WithRsaCrypt(rsaCrypt),
		nexussvc.WithPipelineCms(p.PipelineCms),
	)

	// init publisher service
	pub := publisher.New(
		publisher.WithDBClient(db),
		publisher.WithUCClient(uc),
		publisher.WithBundle(bdl.Bdl),
		publisher.WithNexusSvc(nexusSvc),
	)

	// init certificate service
	cer := certificate.New(
		certificate.WithDBClient(db),
		certificate.WithBundle(bdl.Bdl),
	)

	// init appcertificate service
	appCer := appcertificate.New(
		appcertificate.WithDBClient(db),
		appcertificate.WithBundle(bdl.Bdl),
		appcertificate.WithCertificate(cer),
		appcertificate.WithPipelineCms(p.PipelineCms),
	)

	libReference := libreference.New(
		libreference.WithDBClient(db),
		libreference.WithBundle(bdl.Bdl),
	)

	// init org service
	o := org.New(
		org.WithDBClient(db),
		org.WithUCClient(uc),
		org.WithBundle(bdl.Bdl),
		org.WithPublisher(pub),
		org.WithNexusSvc(nexusSvc),
		org.WithTrans(p.ResourceTrans),
		org.WithCMP(p.Cmp),
	)

	// init project service
	proj := project.New(
		project.WithBundle(bdl.Bdl),
		project.WithTrans(p.ResourceTrans),
		project.WithCMP(p.Cmp),
		project.WithNamespace(ns),
		project.WithTokenSvc(p.TokenService),
		project.WithClusterSvc(p.ClusterSvc),
	)
	proj.UpdateFileRecord = testCaseSvc.UpdateFileRecord
	proj.CreateFileRecord = testCaseSvc.CreateFileRecord

	app := application.New(
		application.WithBundle(bdl.Bdl),
		application.WithDBClient(db),
		application.WithPipelineCms(p.PipelineCms),
		application.WithTokenSvc(p.TokenService),
	)

	codeCvc := code_coverage.New(
		code_coverage.WithDBClient(db),
		code_coverage.WithBundle(bdl.Bdl),
		code_coverage.WithEnvConfig(env),
	)

	testReportSvc := test_report.New(
		test_report.WithDBClient(db),
		test_report.WithBundle(bdl.Bdl),
	)

	pipelineSvc := pipeline.New(
		pipeline.WithBundle(bdl.Bdl),
		pipeline.WithBranchRuleSvc(branchRule),
		pipeline.WithPublisherSvc(pub),
		pipeline.WithPipelineCms(p.PipelineCms),
		pipeline.WithPipelineSource(p.PipelineSource),
		pipeline.WithPipelineCron(p.PipelineCron),
		pipeline.WithPipelineDefinition(p.PipelineDefinition),
		pipeline.WithAppSvc(app),
	)

	publishItem := publish_item.New()

	p.ProjectPipelineSvc.WithPipelineSvc(pipelineSvc)
	p.ProjectPipelineSvc.WithPermissionSvc(perm)
	p.ProjectPipelineSvc.WithBranchRuleSve(branchRule)

	p.GuideSvc.WithBranchRuleSve(branchRule)

	p.CICDCmsSvc.WithPermission(perm)

	p.DevFlowSvc.WithBranchRule(branchRule)
	p.DevFlowSvc.WithGittarFileTree(gittarFileTreeSvc)
	p.DevFlowSvc.WithPermission(perm)

	// compose endpoints
	ep := endpoints.New(
		endpoints.WithBundle(bdl.Bdl),
		endpoints.WithPipeline(pipelineSvc),
		endpoints.WithPipelineCms(p.PipelineCms),
		endpoints.WithEvent(e),
		endpoints.WithCDP(c),
		endpoints.WithPermission(perm),
		endpoints.WithQueryStringDecoder(queryStringDecoder),
		endpoints.WithGittarFileTree(gittarFileTreeSvc),
		endpoints.WithProjectPipelineFileTree(pFileTree),

		endpoints.WithAssetSvc(assetsvc.New(
			assetsvc.WithBranchRuleSvc(branchRule),
			assetsvc.WithI18n(p.APIMTrans),
			assetsvc.WithBundle(bdl.Bdl),
		)),
		endpoints.WithFileTreeSvc(filetreeSvc),
		endpoints.WithProject(proj),
		endpoints.WithApplication(app),

		endpoints.WithDB(db),
		endpoints.WithTestcase(testCaseSvc),
		endpoints.WithTestSet(testSetSvc),
		endpoints.WithSonarMetricRule(sonarMetricRule),
		endpoints.WithTestplan(testPlan),
		endpoints.WithWorkbench(workBench),
		endpoints.WithCQ(cq.New(cq.WithBundle(bdl.Bdl), cq.WithBranchRule(branchRule))),
		endpoints.WithAutoTest(autotest),
		endpoints.WithAutoTestV2(autotestV2),
		endpoints.WithSceneSet(sceneset),
		endpoints.WithMigrate(migrateSvc),

		endpoints.WithJSONStore(store),
		endpoints.WithEtcdStore(etcdStore),
		endpoints.WithOSSClient(ossClient),
		endpoints.WithTicket(t),
		endpoints.WithComment(com),
		endpoints.WithBranchRule(branchRule),
		endpoints.WithNamespace(ns),
		endpoints.WithEnvConfig(env),
		endpoints.WithIssue(issue),
		endpoints.WithIssueState(issueState),
		endpoints.WithIteration(itr),
		endpoints.WithPublisher(pub),
		endpoints.WithCertificate(cer),
		endpoints.WithAppCertificate(appCer),
		endpoints.WithLibReference(libReference),
		endpoints.WithOrg(o),
		endpoints.WithCodeCoverageExecRecord(codeCvc),
		endpoints.WithTestReportRecord(testReportSvc),
		endpoints.WithPipelineCron(p.PipelineCron),
		endpoints.WithPipelineSource(p.PipelineSource),
		endpoints.WithPipelineDefinition(p.PipelineDefinition),
		endpoints.WithPublishItem(publishItem),
		endpoints.WithDevFlowRule(p.DevFlowRule),
		endpoints.WithTokenSvc(p.TokenService),
	)

	ep.ImportChannel = make(chan uint64)
	ep.ExportChannel = make(chan uint64)
	ep.CopyChannel = make(chan uint64)
	p.IssueCoreSvc.WithChannel(ep.ExportChannel, ep.ImportChannel)
	return ep, nil
}

func loadMetricKeysFromDb(db *dao.DBClient) {
	var list []*apistructs.SonarMetricKey
	if err := db.Table("qa_sonar_metric_keys").Find(&list).Error; err != nil {
		panic(err)
	}

	for _, sonarMetricKey := range list {
		apistructs.SonarMetricKeys[sonarMetricKey.ID] = sonarMetricKey
	}
}

func registerWebHook(bdl *bundle.Bundle) {
	// 注册审批流状态变更监听
	ev := apistructs.CreateHookRequest{
		Name:   "dop_approve_status_changed",
		Events: []string{bundle.ApprovalStatusChangedEvent},
		URL:    strutil.Concat("http://", discover.DOP(), "/api/approvals/actions/watch-status"),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}
	if err := bdl.CreateWebhook(ev); err != nil {
		logrus.Warnf("failed to register approval status changed event, %v", err)
	}

	ev = apistructs.CreateHookRequest{
		Name:   "pipeline_yml_update",
		Events: []string{bundle.GitPushEvent},
		URL:    strutil.Concat("http://", discover.DOP(), "/api/cicd-crons/actions/hook-for-update"),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}
	if err := bdl.CreateWebhook(ev); err != nil {
		logrus.Warnf("failed to register pipeline yml event, %v", err)
	}

	ev = apistructs.CreateHookRequest{
		Name:   "pipeline_definition_update",
		Events: []string{bundle.GitPushEvent},
		URL:    strutil.Concat("http://", discover.DOP(), "/api/cicd-pipelines/actions/hook-for-definition-update"),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}
	if err := bdl.CreateWebhook(ev); err != nil {
		logrus.Warnf("failed to register pipeline_definition_update event, %v", err)
	}

	ev = apistructs.CreateHookRequest{
		Name:   "project_pipeline_create",
		Events: []string{bundle.GitPushEvent},
		URL:    strutil.Concat("http://", discover.DOP(), "/api/project-pipeline/actions/create-by-gittar-push-hook"),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}
	if err := bdl.CreateWebhook(ev); err != nil {
		logrus.Warnf("failed to register project_pipeline_create event, %v", err)
	}
}

func (p *provider) exportTestFileTask(ep *endpoints.Endpoints) {
	svc := ep.TestCaseService()
	ok, record, err := svc.GetFirstFileReady(apistructs.FileActionTypeExport,
		apistructs.FileSpaceActionTypeExport,
		apistructs.FileSceneSetActionTypeExport,
		apistructs.FileProjectTemplateExport,
		apistructs.FileProjectPackageExport,
		apistructs.FileIssueActionTypeExport)
	if err != nil {
		logrus.Error(apierrors.ErrExportTestCases.InternalError(err))
		return
	}
	if !ok {
		return
	}
	switch record.Type {
	case apistructs.FileActionTypeExport:
		svc.ExportFile(record)
	case apistructs.FileSpaceActionTypeExport:
		at2Svc := ep.AutotestV2Service()
		at2Svc.ExportFile(record)
	case apistructs.FileSceneSetActionTypeExport:
		at2Svc := ep.AutotestV2Service()
		at2Svc.ExportSceneSetFile(record)
	case apistructs.FileProjectTemplateExport:
		pro := ep.ProjectService()
		pro.ExportTemplatePackage(record)
	case apistructs.FileProjectPackageExport:
		pro := ep.ProjectService()
		pro.ExportProjectPackage(record)
	case apistructs.FileIssueActionTypeExport:
		p.IssueCoreSvc.ExportExcelAsync(record)
	default:

	}
}

func (p *provider) importTestFileTask(ep *endpoints.Endpoints) {
	svc := ep.TestCaseService()
	ok, record, err := svc.GetFirstFileReady(apistructs.FileActionTypeImport,
		apistructs.FileSpaceActionTypeImport,
		apistructs.FileSceneSetActionTypeImport,
		apistructs.FileProjectTemplateImport,
		apistructs.FileProjectPackageImport,
		apistructs.FileIssueActionTypeImport)
	if err != nil {
		logrus.Error(apierrors.ErrExportTestCases.InternalError(err))
		return
	}
	if !ok {
		return
	}
	switch record.Type {
	case apistructs.FileActionTypeImport:
		svc.ImportFile(record)
	case apistructs.FileSpaceActionTypeImport:
		at2Svc := ep.AutotestV2Service()
		at2Svc.ImportFile(record)
	case apistructs.FileSceneSetActionTypeImport:
		at2Svc := ep.AutotestV2Service()
		at2Svc.ImportSceneSetFile(record)
	case apistructs.FileProjectTemplateImport:
		pro := ep.ProjectService()
		pro.ImportTemplatePackage(record)
	case apistructs.FileProjectPackageImport:
		pro := ep.ProjectService()
		pro.ImportProjectPackage(record)
	case apistructs.FileIssueActionTypeImport:
		p.IssueCoreSvc.ImportExcel(record)
	default:

	}
}

func copyTestFileTask(ep *endpoints.Endpoints) {
	ok, record, err := ep.TestCaseService().GetFirstFileReady(apistructs.FileActionTypeCopy)
	if err != nil {
		logrus.Error(apierrors.ErrExportTestCases.InternalError(err))
		return
	}
	if !ok {
		return
	}
	ep.TestSetService().CopyTestSet(record)
}

// compensatePipelineCms compensate pipeline cms according to pipeline cron which enable is true
// it will be deprecated in the later version
func (p *provider) compensatePipelineCms(ep *endpoints.Endpoints) error {
	// get total
	pageResult, err := p.PipelineCron.CronPaging(context.Background(), &cronpb.CronPagingRequest{
		AllSources: false,
		Sources:    []string{apistructs.PipelineSourceDice.String()},
		YmlNames:   nil,
		PageSize:   1,
		PageNo:     1,
		Enable:     wrapperspb.Bool(true),
	})
	if err != nil {
		logrus.Errorf("failed to PageListPipelineCrons, err: %s", err.Error())
		return err
	}
	total := pageResult.Total
	pageSize := 1000
	crons := make([]*pb.Cron, 0, total)
	for i := 0; i < int(total)/pageSize+1; i++ {
		pageResult, err = p.PipelineCron.CronPaging(context.Background(), &cronpb.CronPagingRequest{
			AllSources: true,
			Sources:    nil,
			YmlNames:   nil,
			PageSize:   int64(pageSize),
			PageNo:     int64(i + 1),
			Enable:     wrapperspb.Bool(true),
		})
		if err != nil {
			logrus.Errorf("failed to PageListPipelineCrons, err: %s", err.Error())
			return err
		}
		crons = append(crons, pageResult.Data...)
	}

	// userOrgMap judge the user ns is compensated or not in the org
	// key: userID-orgID, value: struct{}
	userOrgMap := make(map[string]struct{})
	for _, v := range crons {
		if v.Enable != nil && v.Enable.Value && v.UserID != "" && v.OrgID != 0 {
			ns := utils.MakeUserOrgPipelineCmsNs(v.UserID, v.OrgID)
			if !strutil.InSlice(ns, v.ConfigManageNamespaces) {
				_, err := p.PipelineCron.CronUpdate(context.Background(), &cronpb.CronUpdateRequest{
					CronID:                 v.ID,
					PipelineYml:            v.PipelineYml,
					CronExpr:               v.CronExpr,
					ConfigManageNamespaces: []string{utils.MakeUserOrgPipelineCmsNs(v.UserID, v.OrgID)},
				})
				if err != nil {
					logrus.Errorf("failed to UpdatePipelineCron, err: %s", err.Error())
				}
			}
			if _, ok := userOrgMap[fmt.Sprintf("%s-%d", v.UserID, v.OrgID)]; !ok {
				userOrgMap[fmt.Sprintf("%s-%d", v.UserID, v.OrgID)] = struct{}{}
				// the member may not exist
				err = ep.UpdateCmsNsConfigs(v.UserID, v.OrgID)
				if err != nil {
					logrus.Errorf("failed to UpdateCmsNsConfigs, err: %s", err.Error())
				}
			}
		}
	}
	return nil
}

// compensateIssueStateCirculation compensate issue state transition
// it will be deprecated in the later version
func compensateIssueStateCirculation(ep *endpoints.Endpoints) error {
	// get all issue stream
	issueStreamExtras, err := ep.DBClient().ListIssueStreamExtraForIssueStateTransMigration()
	if err != nil {
		return nil
	}
	proIssueStreamMap := make(map[uint64][]dao.IssueStreamExtra)
	for _, v := range issueStreamExtras {
		proIssueStreamMap[v.ProjectID] = append(proIssueStreamMap[v.ProjectID], v)
	}

	statesTrans := make([]dao.IssueStateTransition, 0)
	for k, streams := range proIssueStreamMap {
		states, err := ep.DBClient().GetIssuesStatesByProjectID(k, "")
		if err != nil {
			return err
		}
		stateMap := make(map[apistructs.IssueType]map[string]uint64)
		for _, v := range states {
			if _, ok := stateMap[v.IssueType]; !ok {
				stateMap[v.IssueType] = make(map[string]uint64)
			}
			stateMap[v.IssueType][v.Name] = v.ID
		}
		for _, v := range streams {
			id, err := uuid.NewRandom()
			if err != nil {
				return err
			}
			statesTrans = append(statesTrans, dao.IssueStateTransition{
				ID:        id.String(),
				CreatedAt: v.CreatedAt,
				UpdatedAt: v.UpdatedAt,
				ProjectID: k,
				IssueID:   uint64(v.IssueID),
				StateFrom: stateMap[v.IssueType][v.StreamParams.CurrentState],
				StateTo:   stateMap[v.IssueType][v.StreamParams.NewState],
				Creator:   v.Operator,
			})
		}
	}
	issues, err := ep.DBClient().ListIssueForIssueStateTransMigration()
	if err != nil {
		return err
	}

	proInitStateMap := make(map[uint64]map[apistructs.IssueType]uint64)
	for _, v := range issues {
		if _, ok := proInitStateMap[v.ProjectID]; !ok {
			proInitStateMap[v.ProjectID] = make(map[apistructs.IssueType]uint64)
		}
	}
	for k := range proInitStateMap {
		for _, v := range apistructs.IssueTypes {
			states, err := ep.DBClient().GetIssuesStatesByProjectID(k, v)
			if err != nil {
				return err
			}
			if len(states) == 0 {
				continue
			}
			proInitStateMap[k][v] = states[0].ID
		}
	}
	for _, v := range issues {
		id, err := uuid.NewRandom()
		if err != nil {
			return err
		}
		statesTrans = append(statesTrans, dao.IssueStateTransition{
			ID:        id.String(),
			CreatedAt: v.CreatedAt,
			UpdatedAt: v.UpdatedAt,
			ProjectID: v.ProjectID,
			IssueID:   v.ID,
			StateFrom: 0,
			StateTo:   proInitStateMap[v.ProjectID][v.Type],
			Creator:   v.Creator,
		})
	}

	return ep.DBClient().BatchCreateIssueTransition(statesTrans)
}

func deleteWebhook(bdl *bundle.Bundle) error {
	const (
		createHookName = "guide_create"
		deleteHookName = "guide_delete"
	)

	hookNames := []string{createHookName, deleteHookName}

	for _, v := range hookNames {
		err := bdl.DeleteWebhook(apistructs.DeleteHookRequest{
			Name: v,
			HookLocation: apistructs.HookLocation{
				Org:         "-1",
				Project:     "-1",
				Application: "-1",
			},
		})
		if err != nil {
			logrus.Errorf("failed to DeleteWebhook, name: %s,err: %v", v, err)
			return err
		}
	}
	return nil
}
