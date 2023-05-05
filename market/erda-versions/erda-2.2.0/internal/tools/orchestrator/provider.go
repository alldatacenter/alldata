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

package orchestrator

import (
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda-infra/providers/i18n"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	dicehubpb "github.com/erda-project/erda-proto-go/core/dicehub/release/pb"
	tenantpb "github.com/erda-project/erda-proto-go/msp/tenant/pb"
	"github.com/erda-project/erda/internal/tools/orchestrator/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/queue"
)

type provider struct {
	Election          election.Interface             `autowired:"etcd-election"`
	Orm               *gorm.DB                       `autowired:"mysql-client"`
	EventManager      *events.EventManager           `autowired:"erda.orchestrator.events.event-manager"`
	PusherQueue       *queue.PusherQueue             `autowired:"erda.orchestrator.events.pusher-queue"`
	Trans             i18n.Translator                `translator:"common"`
	DicehubReleaseSvc dicehubpb.ReleaseServiceServer `autowired:"erda.core.dicehub.release.ReleaseService"`
	ClusterSvc        clusterpb.ClusterServiceServer `autowired:"erda.core.clustermanager.cluster.ClusterService"`
	TenantSvc         tenantpb.TenantServiceServer   `autowired:"erda.msp.tenant.TenantService"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	return p.Initialize(ctx)
}

func init() {
	servicehub.Register("orchestrator", &servicehub.Spec{
		Services: []string{"orchestrator"},
		Dependencies: []string{
			"etcd-election",
			"http-server",
			"mysql",
			"erda.orchestrator.events",
		},
		Creator: func() servicehub.Provider { return &provider{} },
	})
}
