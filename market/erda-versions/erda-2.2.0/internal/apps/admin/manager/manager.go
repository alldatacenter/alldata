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

package manager

import (
	"context"
	"net/http"
	"time"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/admin/dao"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/jsonstore/etcd"
)

type AdminManager struct {
	db         *dao.DBClient
	endpoints  []httpserver.Endpoint
	bundle     *bundle.Bundle
	etcdStore  *etcd.Store
	clusterSvc clusterpb.ClusterServiceServer
}

type Option func(am *AdminManager)

func NewAdminManager(options ...Option) *AdminManager {
	admin := &AdminManager{}
	for _, op := range options {
		op(admin)
	}
	return admin
}

func WithDB(db *dao.DBClient) Option {
	return func(am *AdminManager) {
		am.db = db
	}
}

func WithBundle(bundle *bundle.Bundle) Option {
	return func(am *AdminManager) {
		am.bundle = bundle
	}
}

func WithETCDStore(etcdStore *etcd.Store) Option {
	return func(am *AdminManager) {
		am.etcdStore = etcdStore
	}
}

func WithClusterSvc(clusterSvc clusterpb.ClusterServiceServer) Option {
	return func(am *AdminManager) {
		am.clusterSvc = clusterSvc
	}
}

func (am *AdminManager) Routers() []httpserver.Endpoint {
	am.AppendApproveEndpoint()
	am.AppendAuditEndpoint()
	am.AppendNoticeEndpoint()
	am.AppendClusterEndpoint()
	am.AppendHostEndpoint()
	am.AppendAdminEndpoint()
	am.AppendUserEndpoint()
	return am.endpoints
}

func NewBundle() *bundle.Bundle {
	bundleOpts := []bundle.Option{
		bundle.WithCoreServices(),
		bundle.WithClusterManager(),
		bundle.WithHTTPClient(httpclient.New(
			httpclient.WithTimeout(time.Second, time.Second*30),
		)),
	}
	bdl := bundle.New(bundleOpts...)
	return bdl
}

func (am *AdminManager) AppendAdminEndpoint() {
	am.endpoints = append(am.endpoints, []httpserver.Endpoint{
		{Path: "/api/healthy", Method: http.MethodGet, Handler: am.HealthyCheck},
	}...)
}

func (am *AdminManager) HealthyCheck(
	ctx context.Context, r *http.Request,
	vars map[string]string) (httpserver.Responser, error) {
	return httpserver.OkResp("ok")
}
