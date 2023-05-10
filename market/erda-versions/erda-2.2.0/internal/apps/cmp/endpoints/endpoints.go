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

package endpoints

import (
	"context"
	"net/http"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/internal/apps/cmp/impl/addons"
	cloud_account "github.com/erda-project/erda/internal/apps/cmp/impl/cloud-account"
	"github.com/erda-project/erda/internal/apps/cmp/impl/clusters"
	"github.com/erda-project/erda/internal/apps/cmp/impl/ess"
	"github.com/erda-project/erda/internal/apps/cmp/impl/labels"
	"github.com/erda-project/erda/internal/apps/cmp/impl/mns"
	"github.com/erda-project/erda/internal/apps/cmp/impl/nodes"
	org_resource "github.com/erda-project/erda/internal/apps/cmp/impl/org-resource"
	"github.com/erda-project/erda/internal/apps/cmp/metrics"
	"github.com/erda-project/erda/internal/apps/cmp/resource"
	"github.com/erda-project/erda/internal/apps/cmp/steve"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/jsonstore"
)

type Endpoints struct {
	bdl      *bundle.Bundle
	dbclient *dbclient.DBClient

	nodes           *nodes.Nodes
	labels          *labels.Labels
	clusters        *clusters.Clusters
	orgResource     *org_resource.OrgResource
	Mns             *mns.Mns
	Ess             *ess.Ess
	metrics         *metrics.Metric
	CloudAccount    *cloud_account.CloudAccount
	Addons          *addons.Addons
	JS              jsonstore.JsonStore
	CachedJS        jsonstore.JsonStore
	SteveAggregator *steve.Aggregator
	Resource        *resource.Resource
	Credential      tokenpb.TokenServiceServer
	ClusterSvc      clusterpb.ClusterServiceServer

	reportTable *resource.ReportTable
	CronService cronpb.CronServiceServer
}

type Option func(*Endpoints)

func New(ctx context.Context, db *dbclient.DBClient, js jsonstore.JsonStore, cachedJS jsonstore.JsonStore, options ...Option) *Endpoints {
	e := &Endpoints{}

	for _, op := range options {
		op(e)
	}
	e.dbclient = db
	e.labels = labels.New(db, e.bdl)
	e.nodes = nodes.New(db, e.bdl, e.ClusterSvc)
	e.clusters = clusters.New(db, e.bdl, e.Credential, e.ClusterSvc)
	e.Mns = mns.New(db, e.bdl, e.nodes, js, e.ClusterSvc)
	e.Ess = ess.New(e.bdl, e.Mns, e.nodes, e.labels, e.ClusterSvc)
	e.CloudAccount = cloud_account.New(db, cachedJS)
	e.Addons = addons.New(db, e.bdl)
	e.JS = js
	e.metrics = ctx.Value("metrics").(*metrics.Metric)
	e.Resource = ctx.Value("resource").(*resource.Resource)
	e.CachedJS = cachedJS
	e.SteveAggregator = steve.NewAggregator(ctx, e.bdl, e.ClusterSvc)
	return e
}

func (e *Endpoints) GetCluster() *clusters.Clusters {
	return e.clusters
}

// WithBundle With bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(e *Endpoints) {
		e.bdl = bdl
	}
}

func WithOrgResource(o *org_resource.OrgResource) Option {
	return func(e *Endpoints) {
		e.orgResource = o
	}
}

// WithCredential with accessKey credential
func WithCredential(c tokenpb.TokenServiceServer) Option {
	return func(e *Endpoints) {
		e.Credential = c
	}
}

func WithResourceTable(reportTable *resource.ReportTable) Option {
	return func(e *Endpoints) {
		e.reportTable = reportTable
	}
}

func WithCronServiceServer(cronService cronpb.CronServiceServer) Option {
	return func(e *Endpoints) {
		e.CronService = cronService
	}
}

func WithClusterServiceServer(clusterSvc clusterpb.ClusterServiceServer) Option {
	return func(e *Endpoints) {
		e.ClusterSvc = clusterSvc
	}
}

// Routes Return routes
func (e *Endpoints) Routes() []httpserver.Endpoint {
	return []httpserver.Endpoint{
		{Path: "/info", Method: http.MethodGet, Handler: e.Info},
		{Path: "/api/node-labels", Method: http.MethodGet, Handler: i18nPrinter(e.ListLabels)},
		{Path: "/api/node-labels", Method: http.MethodPost, Handler: auth(i18nPrinter(e.UpdateLabels))},
		{Path: "/api/nodes", Method: http.MethodPost, Handler: auth(i18nPrinter(e.AddNodes))},
		{Path: "/api/nodes", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.RmNodes))},
		{Path: "/api/records", Method: http.MethodGet, Handler: auth(i18nPrinter(e.Query))},
		{Path: "/api/recordtypes", Method: http.MethodGet, Handler: auth(i18nPrinter(e.RecordTypeList))},
		{Path: "/api/node-logs", Method: http.MethodGet, Handler: auth(i18nPrinter(e.Logs))},
		{Path: "/api/cluster/actions/import", Method: http.MethodPost, Handler: auth(i18nPrinter(e.ImportCluster))},
		{Path: "/api/cluster/actions/init-retry", Method: http.MethodPost, Handler: auth(i18nPrinter(e.InitClusterRetry))},
		{Path: "/api/cluster/actions/upgrade", Method: http.MethodPost, Handler: auth(i18nPrinter(e.UpgradeEdgeCluster))},
		{Path: "/api/cluster/actions/batch-upgrade", Method: http.MethodPost, Handler: auth(i18nPrinter(e.BatchUpgradeEdgeCluster))},
		{Path: "/api/cluster/actions/batch-offline", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.BatchOfflineEdgeCluster))},
		{Path: "/api/cluster", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.OfflineEdgeCluster))},
		{Path: "/api/cluster", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ClusterInfo))},
		{Path: "/api/cluster/init-command", Method: http.MethodGet, WriterHandler: e.InitCluster},
		{Path: "/api/cluster/credential/access-keys", Method: http.MethodGet, Handler: auth(i18nPrinter(e.GetAccessKey))},
		{Path: "/api/cluster/credential/access-keys", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateAccessKey))},
		{Path: "/api/cluster/credential/access-keys/actions/reset", Method: http.MethodPost, Handler: auth(i18nPrinter(e.ResetAccessKey))},
		{Path: "/api/org-cluster-info", Method: http.MethodGet, Handler: auth(i18nPrinter(e.OrgClusterInfo))},

		// officer apis
		{Path: "/api/clusters/{clusterName}/registry/readonly", Method: http.MethodGet, Handler: e.RegistryReadonly},
		{Path: "/api/clusters/{clusterName}/registry/layers", Method: http.MethodDelete, Handler: e.RegistryRemoveLayers},
		{Path: "/api/clusters", Method: http.MethodPut, Handler: auth(i18nPrinter(e.ClusterUpdate))},
		{Path: "/api/clusters/{clusterName}/registry/manifests/actions/remove", Method: http.MethodPost, Handler: e.RegistryRemoveManifests},
		{Path: "/api/script/info", Method: http.MethodGet, Handler: e.GetScriptInfo},
		{Path: "/api/script/{Name}", Method: http.MethodGet, WriterHandler: e.ServeScript},

		// cloud node apis
		{Path: "/api/ops/cloud-nodes", Method: http.MethodPost, Handler: auth(i18nPrinter(e.AddCloudNodes))},

		// cloud cluster apis
		{Path: "/api/cloud-clusters", Method: http.MethodPost, Handler: auth(i18nPrinter(e.AddCloudClusters))},
		{Path: "/api/cluster-preview", Method: http.MethodPost, Handler: auth(i18nPrinter(e.ClusterPreview))},
		{Path: "/api/ops/lock-cluster", Method: http.MethodPost, Handler: auth(i18nPrinter(e.LockCluster))},
		{Path: "/api/ops/unlock-cluster", Method: http.MethodPost, Handler: auth(i18nPrinter(e.UnlockCluster))},

		{Path: "/api/ops/cloud-resource-list", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListAliyunResources))},
		{Path: "/api/ops/cloud-resource", Method: http.MethodGet, Handler: auth(i18nPrinter(e.QueryCloudResourceDetail))},
		{Path: "/api/ops/cloud-resource-tag", Method: http.MethodPost, Handler: auth(i18nPrinter(e.TagResources))},
		{Path: "/api/cloud-resource/set-tag", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CloudResourceSetTag))},

		// ecs
		{Path: "/api/cloud-ecs", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListECS))},
		{Path: "/api/cloud-ecs/actions/stop", Method: http.MethodPost, Handler: auth(i18nPrinter(e.StopECS))},
		{Path: "/api/cloud-ecs/actions/start", Method: http.MethodPost, Handler: auth(i18nPrinter(e.StartECS))},
		{Path: "/api/cloud-ecs/actions/restart", Method: http.MethodPost, Handler: auth(i18nPrinter(e.RestartECS))},
		{Path: "/api/cloud-ecs/actions/config-renew-attribute", Method: http.MethodPost, Handler: auth(i18nPrinter(e.AutoRenewECS))},

		//vpc, vswitch
		{Path: "/api/cloud-vpc", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListVPC))},
		{Path: "/api/cloud-vpc", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateVPC))},
		{Path: "/api/cloud-vpc/actions/tag-cluster", Method: http.MethodPost, Handler: auth(i18nPrinter(e.VPCTagCluster))},
		{Path: "/api/cloud-vsw", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListVSW))},
		{Path: "/api/cloud-vsw", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateVSW))},

		// region, zone list
		{Path: "/api/cloud-region", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListRegion))},
		{Path: "/api/cloud-zone", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListZone))},

		// cloud-account
		{Path: "/api/cloud-account", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListAccount))},
		{Path: "/api/cloud-account", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateAccount))},
		{Path: "/api/cloud-account", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteAccount))},

		// cloud-mysql
		{Path: "/api/cloud-mysql", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListMysql))},
		{Path: "/api/cloud-mysql/{instanceID}", Method: http.MethodGet, Handler: auth(i18nPrinter(e.GetMysqlDetailInfo))},
		{Path: "/api/cloud-mysql", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateMysqlInstance))},
		{Path: "/api/cloud-mysql", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteMysql))},
		{Path: "/api/cloud-mysql/{instanceID}/databases", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListMysqlDatabase))},
		{Path: "/api/cloud-mysql/actions/delete-db", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteMysqlDatabase))},
		{Path: "/api/cloud-mysql/actions/create-db", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateMysqlDatabase))},
		{Path: "/api/cloud-mysql/{instanceID}/accounts", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListMysqlAccount))},
		{Path: "/api/cloud-mysql/actions/create-account", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateMysqlAccount))},
		{Path: "/api/cloud-mysql/actions/reset-password", Method: http.MethodPost, Handler: auth(i18nPrinter(e.ResetMysqlAccountPassword))},
		{Path: "/api/cloud-mysql/actions/grant-privilege", Method: http.MethodPost, Handler: auth(i18nPrinter(e.GrantMysqlAccountPrivilege))},

		// cloud-redis
		{Path: "/api/cloud-redis", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateRedis))},
		{Path: "/api/cloud-redis", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteRedisResource))},
		{Path: "/api/cloud-redis", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListRedis))},
		{Path: "/api/cloud-redis/{instanceID}", Method: http.MethodGet, Handler: auth(i18nPrinter(e.CetRedisDetailInfo))},

		// cloud-oss
		{Path: "/api/cloud-oss", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateOSS))},
		{Path: "/api/cloud-oss", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteOSSResource))},
		{Path: "/api/cloud-oss", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListOSS))},

		// cloud-ons
		{Path: "/api/cloud-ons", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateOnsInstance))},
		{Path: "/api/cloud-ons", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteOns))},
		{Path: "/api/cloud-ons/actions/delete-topic", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteOnsTopic))},
		{Path: "/api/cloud-ons/actions/list-topic", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListOnsTopic))},
		{Path: "/api/cloud-ons/actions/create-topic", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateOnsTopic))},
		{Path: "/api/cloud-ons/actions/list-group", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListOnsGroup))},
		{Path: "/api/cloud-ons/actions/create-group", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateOnsGroup))},
		{Path: "/api/cloud-ons", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListOns))},
		{Path: "/api/cloud-ons/{instanceID}", Method: http.MethodGet, Handler: auth(i18nPrinter(e.CetOnsDetailInfo))},

		// cloud-gateway
		{Path: "/api/cloud-gateway", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateGatewayVpcGrant))},
		{Path: "/api/cloud-gateway", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ListGatewayAndVpc))},
		{Path: "/api/cloud-gateway", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteGateway))},
		{Path: "/api/cloud-gateway/actions/create-vpc-grant", Method: http.MethodPost, Handler: auth(i18nPrinter(e.CreateGatewayVpcGrant))},
		{Path: "/api/cloud-gateway/actions/delete-vpc-grant", Method: http.MethodDelete, Handler: auth(i18nPrinter(e.DeleteGateway))},

		// cloud resource overview
		{Path: "/api/cloud-resource-overview", Method: http.MethodGet, Handler: auth(i18nPrinter(e.CloudResourceOverview))},
		{Path: "/api/ecs-trending", Method: http.MethodGet, Handler: auth(i18nPrinter(e.ECSTrending))},

		// addon management
		{Path: "/api/addons/actions/config", Method: http.MethodGet, Handler: auth(i18nPrinter(e.GetAddonConfig))},
		{Path: "/api/addons/actions/config", Method: http.MethodPost, Handler: auth(i18nPrinter(e.UpdateAddonConfig))},
		{Path: "/api/addons/actions/scale", Method: http.MethodPost, Handler: auth(i18nPrinter(e.AddonScale))},
		{Path: "/api/addons/status", Method: http.MethodGet, Handler: auth(i18nPrinter(e.GetAddonStatus))},

		{Path: "/api/aliyun-client", Method: http.MethodPost, WriterHandler: e.DoRemoteAction},

		{Path: "/api/internal-cloud-account", Method: http.MethodGet, Handler: i18nPrinter(e.GetCloudAccount)},

		// task list
		{Path: "/api/org/actions/list-running-tasks", Method: http.MethodGet, Handler: i18nPrinter(e.ListOrgRunningTasks)},
		{Path: "/api/tasks", Method: http.MethodPost, Handler: i18nPrinter(e.DealTaskEvent)},
		{Path: "/api/metrics", Method: http.MethodPost, Handler: i18nPrinter(e.MetricsQuery)},

		{Path: "/api/resource-overview/gauge", Method: http.MethodGet, Handler: i18nPrinter(e.GetResourceGauge)},
		{Path: "/api/resource-overview/class", Method: http.MethodGet, Handler: i18nPrinter(e.GetResourceClass)},
		{Path: "/api/resource-overview/cluster-trend", Method: http.MethodGet, Handler: i18nPrinter(e.GetResourceClusterTrend)},
		{Path: "/api/resource-overview/project-trend", Method: http.MethodGet, Handler: httpserver.Wrap(e.GetResourceProjectTrend, httpserver.WithI18nCodes)},
		{Path: "/api/resource-overview/report-table", Method: http.MethodGet, Handler: i18nPrinter(e.ResourceOverviewReport)},

		// k8s clusters
		{Path: "/api/k8s/clusters", Method: http.MethodGet, Handler: e.ListK8SClusters},

		// cluster hook
		{Path: "/api/clusterhook", Method: http.MethodPost, Handler: e.ClusterHook},
	}
}
