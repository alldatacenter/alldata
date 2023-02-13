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

package events

import (
	"context"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/internal/tools/orchestrator/queue"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type provider struct {
	Orm *gorm.DB

	em *EventManager
	pq *queue.PusherQueue
}

func newBundleService() *bundle.Bundle {
	bundleOpts := []bundle.Option{
		bundle.WithHTTPClient(
			httpclient.New(
				httpclient.WithTimeout(time.Second, time.Second*60),
			)),
		bundle.WithCoreServices(),
		bundle.WithCollector(),
	}
	return bundle.New(bundleOpts...)
}

func (p *provider) Init(ctx servicehub.Context) error {
	// TODO: use smaller scope config
	conf.Load()
	p.pq = queue.NewPusherQueue()
	db := &dbclient.DBClient{DBEngine: &dbengine.DBEngine{DB: p.Orm}}
	bdl := newBundleService()

	p.em = NewEventManager(1000, p.pq, db, bdl)
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	p.em.Start()
	<-ctx.Done()
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	if ctx.Service() == "erda.orchestrator.events.pusher-queue" {
		return p.pq
	}
	return p.em
}

func init() {
	servicehub.Register("erda.orchestrator.events", &servicehub.Spec{
		Services: []string{
			"erda.orchestrator.events",
			"erda.orchestrator.events.event-manager",
			"erda.orchestrator.events.pusher-queue",
		},
		Dependencies: []string{
			"mysql",
		},
		Description: "",
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
