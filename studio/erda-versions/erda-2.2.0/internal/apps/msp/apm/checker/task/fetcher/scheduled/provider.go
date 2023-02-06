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

package scheduled

import (
	"context"
	"reflect"
	"time"

	"github.com/go-redis/redis"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage/cache"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/task/fetcher"
)

type config struct {
	CacheKey string `file:"cache_key" default:"checkers"`

	LoadCheckersInterval time.Duration `file:"load_checkers_interval" default:"1m"`
	MaxScheduleInterval  time.Duration `file:"max_schedule_interval" default:"3m"`
}

// +provider
type provider struct {
	Cfg      *config
	Log      logs.Logger
	Redis    *redis.Client      `autowired:"redis-client"`
	Election election.Interface `autowired:"etcd-election"`

	storage    storage.Interface
	dispatcher *Dispatcher
	scheduler  *Scheduler
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.storage = &cache.CacheStorage{Cache: cache.New(p.Cfg.CacheKey, p.Redis)}
	scheduleStorage := &RedisScheduleStorage{
		Root:  p.Cfg.CacheKey,
		Redis: p.Redis,
		NodesFunc: func() (list []*Node, err error) {
			nodes, err := p.Election.Nodes()
			if err != nil {
				return nil, err
			}
			for _, n := range nodes {
				list = append(list, &Node{ID: n.ID})
			}
			return
		},
	}
	p.scheduler = NewScheduler(p.storage, scheduleStorage, p.Cfg.MaxScheduleInterval, p.Log)
	p.Election.OnLeader(p.reschedule)
	p.Election.OnLeader(p.scheduler.Run)

	// for worker
	p.dispatcher = NewDispatcher(p.scheduledCheckers, p.Cfg.LoadCheckersInterval, p.Log)
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	return p.dispatcher.Run(ctx)
}

func (p *provider) scheduledCheckers() (map[int64]*pb.Checker, error) {
	ids, err := p.scheduler.ListIDs(p.Election.Node().ID)
	if err != nil {
		return nil, err
	}
	return p.storage.ListByIDs(ids)
}

func (p *provider) reschedule(ctx context.Context) {
	watch := p.Election.Watch(ctx)
	for {
		select {
		case event, ok := <-watch:
			if !ok {
				return
			}
			if event.Action == election.ActionDelete {
				p.scheduler.RemoveNode(event.Node.ID)
			}
			p.scheduler.Reschedule()
		case <-ctx.Done():
			return
		}
	}
}

func (p *provider) Watch() <-chan *fetcher.Event {
	return p.dispatcher.Watch()
}

func init() {
	servicehub.Register("erda.msp.apm.checker.task.fetcher.scheduled", &servicehub.Spec{
		Services:   []string{"erda.msp.apm.checker.task.fetcher"},
		Types:      []reflect.Type{reflect.TypeOf((*fetcher.Interface)(nil)).Elem()},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
