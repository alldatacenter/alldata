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

package cluster

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/core/cluster-manager/cluster/db"
)

type provider struct {
	Register       transport.Register `autowired:"service-register" required:"true"`
	DB             *gorm.DB           `autowired:"mysql-client"`
	clusterService *ClusterService
	bdl            *bundle.Bundle
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.bdl = bundle.New(bundle.WithCoreServices())
	p.clusterService = NewClusterService(WithDB(&db.ClusterDB{DB: p.DB}), WithBundle(p.bdl))
	if p.Register != nil {
		pb.RegisterClusterServiceImp(p.Register, p.clusterService)
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.core.clustermanager.ClusterService" || ctx.Type() == pb.ClusterServiceServerType() ||
		ctx.Type() == pb.ClusterServiceHandlerType():
		return p.clusterService
	}
	return p
}

func init() {
	servicehub.Register("erda.core.clustermanager.cluster", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
