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

// Package orchestrator 编排器
package orchestrator

import (
	"context"
	"strconv"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	infrahttpserver "github.com/erda-project/erda-infra/providers/httpserver"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/components/addon/mysql"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	ecpednpoints "github.com/erda-project/erda/internal/tools/orchestrator/ecp/endpoints"
	"github.com/erda-project/erda/internal/tools/orchestrator/endpoints"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/instanceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/addon"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/deployment"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/deployment_order"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/domain"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/environment"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/instance"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/migration"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/resource"
	"github.com/erda-project/erda/internal/tools/orchestrator/services/runtime"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/goroutinepool"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/loop"
)

// Initialize 初始化应用启动服务.
func (p *provider) Initialize(ctx servicehub.Context) error {
	conf.Load()
	if conf.Debug() {
		logrus.SetLevel(logrus.DebugLevel)
	}

	db := &dbclient.DBClient{
		DBEngine: &dbengine.DBEngine{
			DB: p.Orm,
		},
	}

	// init endpoints
	ep, err := p.initEndpoints(db)
	if err != nil {
		return err
	}

	bdl := bundle.New()
	// This server will never be started. Only the routes and locale loader are used by new http server
	server := httpserver.New(":0")
	server.WithLocaleLoader(bdl.GetLocaleLoader())
	// server.Router().Path("/metrics").Methods(http.MethodGet).Handler(promxp.Handler("orchestrator"))
	server.RegisterEndpoint(ep.Routes())

	// register ecp router
	server.RegisterEndpoint(registerEcpRouter(bdl, db.DBEngine, p.ClusterSvc))

	ctx.Service("http-server").(infrahttpserver.Router).Any("/**", server.Router())

	// Limit only one instance of scheduler to do the cron jobs
	p.Election.OnLeader(func(ctx context.Context) {
		logrus.Infof("i'm the leader now")
		go func() {
			if err = cleanLeaderRemainingAddon(ep); err != nil {
				logrus.Errorf("failed to cleanLeaderRemainingAddon,err: %s", err.Error())
			}
		}()
		_ = initLeaderCron(ep, ctx)
		logrus.Infof("i resign the leader now")
	})

	go scheduler.GetDCOSTokenAuthPeriodically()

	// start cron jobs to sync addon & project infos
	go initCron(ep, ctx)

	i18n.InitI18N()
	// TODO: split common trans
	i18n.SetSingle(p.Trans)

	return nil
}

// 初始化 Endpoints
func (p *provider) initEndpoints(db *dbclient.DBClient) (*endpoints.Endpoints, error) {
	// init pool
	pool := goroutinepool.New(conf.PoolSize())
	pool.Start()

	// init Bundle
	bundleOpts := []bundle.Option{
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second, time.Second*60),
			)),
		bundle.WithCoreServices(),
		bundle.WithDiceHub(),
		bundle.WithScheduler(),
		bundle.WithCollector(),
		bundle.WithMonitor(),
		bundle.WithHepa(),
		bundle.WithCMP(),
		bundle.WithKMS(),
		bundle.WithOpenapi(),
		bundle.WithPipeline(),
		bundle.WithGittar(),
		bundle.WithMSP(),
		bundle.WithDOP(),
		bundle.WithClusterManager(),
	}
	bdl := bundle.New(bundleOpts...)

	encrypt := encryption.New(
		encryption.WithRSAScrypt(encryption.NewRSAScrypt(encryption.RSASecret{
			PublicKey:          conf.PublicKey(),
			PublicKeyDataType:  encryption.Base64,
			PrivateKey:         conf.PrivateKey(),
			PrivateKeyType:     encryption.PKCS1,
			PrivateKeyDataType: encryption.Base64,
		})))

	// init scheduler
	instanceinfoImpl := instanceinfo.NewInstanceInfoImpl()
	scheduler := scheduler.NewScheduler(instanceinfoImpl, p.ClusterSvc)

	migration := migration.New(
		migration.WithBundle(bdl),
		migration.WithDBClient(db),
		migration.WithJob(scheduler.Httpendpoints.Job))

	resource := resource.New(
		resource.WithDBClient(db),
		resource.WithBundle(bdl),
	)

	// init addon service
	a := addon.New(
		addon.WithDBClient(db),
		addon.WithBundle(bdl),
		addon.WithResource(resource),
		addon.WithEnvEncrypt(encrypt),
		addon.WithKMSWrapper(mysql.NewKMSWrapper(bdl)),
		addon.WithHTTPClient(httpclient.New(
			httpclient.WithTimeout(time.Second, time.Second*60),
		)),
		addon.WithCap(scheduler.Httpendpoints.Cap),
		addon.WithServiceGroup(scheduler.Httpendpoints.ServiceGroupImpl),
		addon.WithInstanceinfoImpl(instanceinfoImpl),
		addon.WithClusterInfoImpl(scheduler.Httpendpoints.ClusterinfoImpl),
		addon.WithClusterSvc(p.ClusterSvc),
		addon.WithTenantSvc(p.TenantSvc),
	)

	// init runtime service
	rt := runtime.New(
		runtime.WithDBClient(db),
		runtime.WithEventManager(p.EventManager),
		runtime.WithBundle(bdl),
		runtime.WithAddon(a),
		runtime.WithReleaseSvc(p.DicehubReleaseSvc),
		runtime.WithServiceGroup(scheduler.Httpendpoints.ServiceGroupImpl),
		runtime.WithClusterInfo(scheduler.Httpendpoints.ClusterinfoImpl),
		runtime.WithClusterSvc(p.ClusterSvc),
	)
	envConfig := environment.New(
		environment.WithDBClient(db),
	)

	// init deployment service
	d := deployment.New(
		deployment.WithDBClient(db),
		deployment.WithEventManager(p.EventManager),
		deployment.WithBundle(bdl),
		deployment.WithAddon(a),
		deployment.WithMigration(migration),
		deployment.WithEncrypt(encrypt),
		deployment.WithResource(resource),
		deployment.WithReleaseSvc(p.DicehubReleaseSvc),
		deployment.WithServiceGroup(scheduler.Httpendpoints.ServiceGroupImpl),
		deployment.WithScheduler(scheduler),
		deployment.WithEnvConfig(envConfig),
		deployment.WithClusterSvc(p.ClusterSvc),
	)

	// init domain service
	dom := domain.New(
		domain.WithDBClient(db),
		domain.WithEventManager(p.EventManager),
		domain.WithBundle(bdl),
		domain.WithClusterSvc(p.ClusterSvc))

	ins := instance.New(
		instance.WithBundle(bdl),
		instance.WithInstanceInfo(instanceinfoImpl),
	)

	//init deployment order service
	do := deployment_order.New(
		deployment_order.WithDBClient(db),
		deployment_order.WithBundle(bdl),
		deployment_order.WithRuntime(rt),
		deployment_order.WithDeployment(d),
		deployment_order.WithQueue(p.PusherQueue),
		deployment_order.WithReleaseSvc(p.DicehubReleaseSvc),
		deployment_order.WithEnvConfig(envConfig),
	)

	// compose endpoints
	ep := endpoints.New(
		endpoints.WithDBClient(db),
		endpoints.WithPool(pool),
		endpoints.WithQueue(p.PusherQueue),
		endpoints.WithEventManager(p.EventManager),
		endpoints.WithBundle(bdl),
		endpoints.WithRuntime(rt),
		endpoints.WithDeployment(d),
		endpoints.WithDeploymentOrder(do),
		endpoints.WithDomain(dom),
		endpoints.WithAddon(a),
		endpoints.WithInstance(ins),
		endpoints.WithEnvEncrypt(encrypt),
		endpoints.WithResource(resource),
		endpoints.WithMigration(migration),
		endpoints.WithReleaseSvc(p.DicehubReleaseSvc),
		endpoints.WithScheduler(scheduler),
		endpoints.WithInstanceinfoImpl(instanceinfoImpl),
	)

	return ep, nil
}

// 初始化 leader 定时任务, 单实例执行
func initLeaderCron(ep *endpoints.Endpoints, ctx context.Context) error {
	// cron for pushOn deployment
	go loop.New(loop.WithContext(ctx), loop.WithInterval(10*time.Second)).Do(ep.PushOnDeploymentPolling)
	go loop.New(loop.WithContext(ctx), loop.WithDeclineRatio(1.2), loop.WithInterval(50*time.Millisecond),
		loop.WithDeclineLimit(3*time.Second)).Do(ep.PushOnDeployment)
	go loop.New(loop.WithContext(ctx), loop.WithInterval(10*time.Second)).Do(ep.PushOnDeletingRuntimesPolling)
	go loop.New(loop.WithContext(ctx), loop.WithInterval(2*time.Second)).Do(ep.PushOnDeletingRuntimes)

	// con for push on deployment order batches
	go loop.New(loop.WithContext(ctx), loop.WithInterval(10*time.Second)).Do(ep.PushOnDeploymentOrderPolling)
	go loop.New(loop.WithContext(ctx), loop.WithDeclineRatio(1.2), loop.WithInterval(50*time.Millisecond),
		loop.WithDeclineLimit(3*time.Second)).Do(ep.PushOnDeploymentOrder)

	go loop.New(loop.WithContext(ctx), loop.WithInterval(10*time.Minute)).Do(ep.SyncAddonReferenceNum)

	go ep.CleanUnusedMigrationNs()
	go loop.New(loop.WithContext(ctx), loop.WithInterval(24*time.Hour)).Do(ep.CleanUnusedMigrationNs)

	go ep.SyncDeployAddon()

	go loop.New(loop.WithContext(ctx), loop.WithInterval(5*time.Minute)).Do(ep.SyncAddonResources)

	go loop.New(loop.WithContext(ctx), loop.WithInterval(1*time.Hour)).Do(ep.CleanRemainingAddonAttachment)

	ep.FullGCLoop(ctx)

	return nil
}

type SharedCronjobRunner interface {
	SyncAddons() (bool, error)
	SyncProjects() (bool, error)
}

// 初始化定时任务
func initCron(ep SharedCronjobRunner, ctx context.Context) {
	go ep.SyncAddons()
	go loop.New(loop.WithContext(ctx), loop.WithInterval(10*time.Minute)).Do(ep.SyncAddons)
	go ep.SyncProjects()
	go loop.New(loop.WithContext(ctx), loop.WithInterval(5*time.Minute)).Do(ep.SyncProjects)
	<-ctx.Done()
}

// cleanLeaderRemainingAddon clean the remain addon
func cleanLeaderRemainingAddon(ep *endpoints.Endpoints) error {
	// find all addon
	addons, err := ep.DBClient().ListAddonInstancesForClean()
	if err != nil {
		return err
	}
	// find the addons which project is deleted
	existProjectMap := make(map[uint64]struct{})
	notExistProjectMap := make(map[uint64]struct{})
	newAddons := addonsFilterIn(addons, func(addon *dbclient.AddonInstance) bool {
		if addon.ProjectID == "" {
			return false
		}
		proID, err := strconv.ParseUint(addon.ProjectID, 10, 64)
		if err != nil {
			logrus.Errorf("[cleanLeaderRemainingAddon] failed to ParseUint, prjectID: %s", addon.ProjectID)
			return false
		}
		if _, ok := existProjectMap[proID]; ok {
			return false
		}
		if _, ok := notExistProjectMap[proID]; ok {
			return true
		}
		_, err = ep.Bdl().GetProject(proID)
		if err == nil {
			existProjectMap[proID] = struct{}{}
			return false
		}

		if !errorresp.IsNotFound(err) {
			logrus.Errorf("[cleanLeaderRemainingAddon] failed to GetProject, prjectID: %s, err: %s", addon.ProjectID, err.Error())
			return false
		}
		// the project is deleted, and should clean the addon
		notExistProjectMap[proID] = struct{}{}
		return true
	})
	logrus.Infof("[cleanLeaderRemainingAddon] begin clean %d addons", len(newAddons))
	for _, v := range newAddons {
		logrus.Infof("[cleanLeaderRemainingAddon] begin clean addon, instanceID: %s", v.ID)
		routings, err := ep.DBClient().GetInstanceRoutingByRealInstance(v.ID)
		if err != nil {
			logrus.Errorf("[cleanLeaderRemainingAddon] failed to GetInstanceRoutingByRealInstance, instanceID: %s", v.ID)
			continue
		}
		if routings == nil {
			continue
		}
		if len(*routings) != 1 {
			logrus.Infof("[cleanLeaderRemainingAddon] the len of routings is not 1, instanceID: %s", v.ID)
			continue
		}
		// all (1000,5000) users is reserved as internal service account
		if err = ep.Addon().Delete("2000", (*routings)[0].ID); err != nil {
			logrus.Errorf("[cleanLeaderRemainingAddon] failed to delete addon, instanceID: %s", v.ID)
		}
	}
	return nil
}

func addonsFilterIn(addons []dbclient.AddonInstance, fn func(addon *dbclient.AddonInstance) bool) (newAddons []dbclient.AddonInstance) {
	for _, v := range addons {
		if fn(&v) {
			newAddons = append(newAddons, v)
		}
	}
	return
}

func registerEcpRouter(bdl *bundle.Bundle, dbEngine *dbengine.DBEngine, clusterSvc clusterpb.ClusterServiceServer) []httpserver.Endpoint {
	return ecpednpoints.New(
		ecpednpoints.WithDBEngine(dbEngine),
		ecpednpoints.WithBundle(bdl),
		ecpednpoints.WithClusterSvc(clusterSvc)).Routes()
}
