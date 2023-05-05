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

// Package cmp Core components of multi-cloud management platform
package cmp

import (
	"context"
	"flag"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/go-redis/redis"
	"github.com/sirupsen/logrus"
	"k8s.io/klog/v2"

	"github.com/erda-project/erda-infra/base/version"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/conf"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/internal/apps/cmp/endpoints"
	"github.com/erda-project/erda/internal/apps/cmp/i18n"
	aliyun_resources "github.com/erda-project/erda/internal/apps/cmp/impl/aliyun-resources"
	org_resource "github.com/erda-project/erda/internal/apps/cmp/impl/org-resource"
	"github.com/erda-project/erda/internal/apps/cmp/resource"
	"github.com/erda-project/erda/internal/apps/cmp/steve/middleware"
	"github.com/erda-project/erda/internal/apps/cmp/tasks"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/dumpstack"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/loop"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/time/ticker"
)

func (p *provider) initialize(ctx context.Context) error {
	conf.Load()

	initKlog()

	// set log formatter
	logrus.SetFormatter(&logrus.TextFormatter{
		ForceColors:     true,
		FullTimestamp:   true,
		TimestampFormat: "2006-01-02 15:04:05.000000000",
	})
	logrus.SetOutput(os.Stdout)

	// set the debug level of log
	debugLevel := logrus.InfoLevel
	if conf.Debug() {
		debugLevel = logrus.DebugLevel
	}
	logrus.SetLevel(debugLevel)

	dumpstack.Open()
	logrus.Infoln(version.String())

	server, err := p.do(ctx)
	if err != nil {
		return err
	}

	return server.ListenAndServe()
}

func initKlog() {
	if !conf.Debug() {
		return
	}

	klog.InitFlags(nil)
	os.Args = append(os.Args, "-v=8")
	flag.Parse()
}

func (p *provider) do(ctx context.Context) (*httpserver.Server, error) {
	var redisCli *redis.Client

	db := dbclient.Open(dbengine.MustOpen())
	i18n.InitI18N()

	// cache etcd
	option := jsonstore.UseCacheEtcdStore(context.Background(), aliyun_resources.CloudResourcePrefix, 100)
	cachedJs, err := jsonstore.New(option)

	// etcd
	js, err := jsonstore.New()
	if err != nil {
		return nil, err
	}

	if conf.LocalMode() {
		redisCli = redis.NewClient(&redis.Options{
			Addr:     conf.RedisAddr(),
			Password: conf.RedisPwd(),
		})
	} else {
		redisCli = redis.NewFailoverClient(&redis.FailoverOptions{
			MasterName:    conf.RedisMasterName(),
			SentinelAddrs: strings.Split(conf.RedisSentinelAddrs(), ","),
			Password:      conf.RedisPwd(),
		})
	}
	if _, err := redisCli.Ping().Result(); err != nil {
		return nil, err
	}

	// init Bundle
	bundleOpts := []bundle.Option{
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second*60, time.Second*60),
			)),
		bundle.WithPipeline(),
		bundle.WithScheduler(),
		bundle.WithMonitor(),
		bundle.WithCoreServices(),
		bundle.WithOrchestrator(),
		bundle.WithDiceHub(),
		bundle.WithClusterManager(),
	}
	bdl := bundle.New(bundleOpts...)

	o := org_resource.New(
		org_resource.WithDBClient(db),
		org_resource.WithBundle(bdl),
		org_resource.WithRedisClient(redisCli),
	)
	r := ctx.Value("resource").(*resource.Resource)
	r.DB = db
	r.Bdl = bdl
	ctx = context.WithValue(ctx, "resource", r)
	resourceTable := resource.NewReportTable(
		resource.ReportTableWithBundle(bdl),
		resource.ReportTableWithCMP(p),
		resource.ReportTableWithTrans(p.Tran),
	)

	ep, err := p.initEndpoints(ctx, db, js, cachedJs, bdl, o, p.Credential, resourceTable)
	if err != nil {
		return nil, err
	}

	p.SteveAggregator = ep.SteveAggregator

	// daily collector project quota and cluster resource request
	dailyCollector := tasks.NewDailyQuotaCollector(
		tasks.DailyQuotaCollectorWithDBClient(db),
		tasks.DailyQuotaCollectorWithBundle(bdl),
		tasks.DailyQuotaCollectorWithCMPAPI(p),
	)
	tic := ticker.New(time.Hour, dailyCollector.Task)
	go tic.Run()

	if conf.EnableEss() {
		initServices(ep)
	}

	server := httpserver.New(conf.ListenAddr())
	server.RegisterEndpoint(append(ep.Routes()))

	authenticator := middleware.NewAuthenticator(bdl, p.ClusterSvc)
	shellHandler := middleware.NewShellHandler(ctx)
	auditor := middleware.NewAuditor(bdl)

	middlewares := middleware.Chain{
		authenticator.AuthMiddleware,
		shellHandler.HandleShell,
		auditor.AuditMiddleWare,
	}
	server.Router().PathPrefix("/api/k8s/clusters/{clusterName}").Handler(middlewares.Handler(ep.SteveAggregator))
	server.Router().PathPrefix("/api/apim/metrics").Handler(endpoints.InternalReverseHandler(endpoints.ProxyMetrics))

	logrus.Infof("start the service and listen on address: %s", conf.ListenAddr())
	logrus.Info("starting cmp instance")

	// init cron job
	initCron(ep)

	return server, nil
}

func (p *provider) initEndpoints(ctx context.Context, db *dbclient.DBClient, js, cachedJS jsonstore.JsonStore, bdl *bundle.Bundle,
	o *org_resource.OrgResource, c tokenpb.TokenServiceServer, rt *resource.ReportTable) (*endpoints.Endpoints, error) {

	// compose endpoints
	ep := endpoints.New(
		ctx,
		db,
		js,
		cachedJS,
		endpoints.WithBundle(bdl),
		endpoints.WithOrgResource(o),
		endpoints.WithCredential(c),
		endpoints.WithResourceTable(rt),
		endpoints.WithCronServiceServer(p.CronService),
		endpoints.WithClusterServiceServer(p.ClusterSvc),
	)

	// Sync org resource task status
	go func() {
		ep.SyncTaskStatus(conf.TaskSyncDuration())
	}()

	// Clean job/deployment sync
	go func() {
		ep.TaskClean(conf.TaskCleanDuration())
	}()

	registerWebHook(bdl)
	registerWebHook(bdl)

	return ep, nil
}

func initServices(ep *endpoints.Endpoints) {
	// run mns service, monitor mns messages & consume them
	ep.Mns.Run()
	ep.Ess.AutoScale()
}

// 初始化定时任务
func initCron(ep *endpoints.Endpoints) {
	// cron job to monitor pipeline created edge clusters
	go loop.New(loop.WithInterval(10 * time.Second)).Do(ep.GetCluster().MonitorCloudCluster)
}

func registerWebHook(bdl *bundle.Bundle) {
	// register pipeline tasks by webhook
	ev := apistructs.CreateHookRequest{
		Name:   "cmdb_pipeline_tasks",
		Events: []string{"pipeline_task", "pipeline_task_runtime"},
		URL:    strutil.Concat("http://", discover.CMP(), "/api/tasks"),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}
	if err := bdl.CreateWebhook(ev); err != nil {
		logrus.Warnf("failed to register pipeline tasks event, (%v)", err)
	}
}

// registerClusterHook register cluster webhook in eventBox
func registerClusterHook() error {
	bdl := bundle.New(bundle.WithCoreServices())

	ev := apistructs.CreateHookRequest{
		Name:   "cmp-clusterhook",
		Events: []string{"cluster"},
		URL:    fmt.Sprintf("http://%s/clusterhook", discover.CMP()),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}

	if err := bdl.CreateWebhook(ev); err != nil {
		logrus.Warnf("failed to register cluster event, (%v)", err)
		return err
	}

	logrus.Infof("register cluster event success")
	return nil
}
