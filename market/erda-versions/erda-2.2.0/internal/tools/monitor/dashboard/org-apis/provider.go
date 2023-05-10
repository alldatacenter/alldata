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

package orgapis

import (
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda-infra/providers/i18n"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/bundle-ex/cmdb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricq"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type config struct {
	OfflineTimeout time.Duration `file:"offline_timeout"`
	OfflineSleep   time.Duration `file:"offline_sleep"`
}

type provider struct {
	C          *config
	L          logs.Logger
	bundle     *bundle.Bundle
	cmdb       *cmdb.Cmdb
	metricq    metricq.Queryer
	service    queryServiceImpl
	t          i18n.Translator
	ClusterSvc clusterpb.ClusterServiceServer `autowired:"erda.core.clustermanager.cluster.ClusterService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.t = ctx.Service("i18n").(i18n.I18n).Translator("org-resource")
	hc := httpclient.New(httpclient.WithTimeout(time.Second, time.Second*60))
	p.bundle = bundle.New(
		bundle.WithHTTPClient(hc),
		bundle.WithCoreServices(),
		bundle.WithClusterManager(),
	)
	p.cmdb = cmdb.New(cmdb.WithHTTPClient(hc))
	p.metricq = ctx.Service("metrics-query").(metricq.Queryer)
	p.service = &queryService{metricQ: p.metricq}
	routes := ctx.Service("http-server", interceptors.Recover(p.L)).(httpserver.Router)
	return p.intRoutes(routes)
}

func init() {
	servicehub.Register("org-apis", &servicehub.Spec{
		Services:     []string{"org-apis"},
		Dependencies: []string{"http-server", "metrics-query", "i18n"},
		Description:  "org apis",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
