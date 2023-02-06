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

package dialer

import (
	"context"

	"github.com/coreos/etcd/clientv3"
	"github.com/rancher/remotedialer"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/httpserver"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/cluster-manager/dialer/config"
	"github.com/erda-project/erda/internal/core/cluster-manager/dialer/server"
)

// +provider
type provider struct {
	Bdl        *bundle.Bundle
	Cfg        *config.Config
	Router     httpserver.Router              `autowired:"http-server@cluster-dialer"`
	Credential tokenpb.TokenServiceServer     `autowired:"erda.core.token.TokenService" optional:"true"`
	Etcd       *clientv3.Client               `autowired:"etcd"`
	Cluster    clusterpb.ClusterServiceServer `autowired:"erda.core.clustermanager.cluster.ClusterService" required:"true"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	if p.Cfg.Debug {
		logrus.SetLevel(logrus.DebugLevel)
		remotedialer.PrintTunnelData = true
	}
	p.Bdl = bundle.New(bundle.WithCoreServices())
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	p.Router.Any("/**", server.NewDialerRouter(ctx, p.Cluster, p.Credential, p.Cfg, p.Etcd, p.Bdl).ServeHTTP)
	return nil
}

func init() {
	servicehub.Register("cluster-dialer", &servicehub.Spec{
		Services: []string{
			"cluster-dialer-service",
		},
		ConfigFunc: func() interface{} {
			return &config.Config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
