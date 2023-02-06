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

package pipelinesvc

import (
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda-proto-go/core/pipeline/cms/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/websocket"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cache"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/clusterinfo"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/daemon"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgereporter"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/engine"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/run"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/secret"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/user"
	"github.com/erda-project/erda/internal/tools/pipeline/services/actionagentsvc"
	"github.com/erda-project/erda/internal/tools/pipeline/services/appsvc"
	"github.com/erda-project/erda/internal/tools/pipeline/services/permissionsvc"
	"github.com/erda-project/erda/internal/tools/pipeline/services/queuemanage"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
)

type PipelineSvc struct {
	appSvc          *appsvc.AppSvc
	crondSvc        daemon.Interface
	actionAgentSvc  *actionagentsvc.ActionAgentSvc
	pipelineCronSvc cronpb.CronServiceServer
	permissionSvc   *permissionsvc.PermissionSvc
	queueManage     *queuemanage.QueueManage
	cache           cache.Interface

	dbClient  *dbclient.Client
	bdl       *bundle.Bundle
	publisher *websocket.Publisher

	engine engine.Interface

	js      jsonstore.JsonStore
	etcdctl *etcd.Store

	// providers
	cmsService   pb.CmsServiceServer
	clusterInfo  clusterinfo.Interface
	edgeRegister edgepipeline_register.Interface
	edgeReporter edgereporter.Interface
	secret       secret.Interface
	user         user.Interface
	run          run.Interface
	actionMgr    actionmgr.Interface
	mysql        mysqlxorm.Interface
}

func New(appSvc *appsvc.AppSvc, crondSvc daemon.Interface,
	actionAgentSvc *actionagentsvc.ActionAgentSvc,
	pipelineCronSvc cronpb.CronServiceServer, permissionSvc *permissionsvc.PermissionSvc,
	queueManage *queuemanage.QueueManage,
	dbClient *dbclient.Client, bdl *bundle.Bundle, publisher *websocket.Publisher,
	engine engine.Interface, js jsonstore.JsonStore, etcd *etcd.Store, clusterInfo clusterinfo.Interface, edgeRegister edgepipeline_register.Interface, cache cache.Interface) *PipelineSvc {

	s := PipelineSvc{}
	s.appSvc = appSvc
	s.crondSvc = crondSvc
	s.actionAgentSvc = actionAgentSvc
	s.pipelineCronSvc = pipelineCronSvc
	s.permissionSvc = permissionSvc
	s.queueManage = queueManage
	s.dbClient = dbClient
	s.bdl = bdl
	s.publisher = publisher
	s.engine = engine
	s.js = js
	s.etcdctl = etcd
	s.clusterInfo = clusterInfo
	s.edgeRegister = edgeRegister
	s.cache = cache
	return &s
}

func (s *PipelineSvc) WithCmsService(cmsService pb.CmsServiceServer) {
	s.cmsService = cmsService
}

func (s *PipelineSvc) WithSecret(secret secret.Interface) {
	s.secret = secret
}

func (s *PipelineSvc) WithUser(user user.Interface) {
	s.user = user
}

func (s *PipelineSvc) WithRun(run run.Interface) {
	s.run = run
}

func (s *PipelineSvc) WithActionMgr(actionMgr actionmgr.Interface) {
	s.actionMgr = actionMgr
}

func (s *PipelineSvc) WithMySQL(mysql mysqlxorm.Interface) {
	s.mysql = mysql
}

func (s *PipelineSvc) WithEdgeReporter(r edgereporter.Interface) {
	s.edgeReporter = r
}
