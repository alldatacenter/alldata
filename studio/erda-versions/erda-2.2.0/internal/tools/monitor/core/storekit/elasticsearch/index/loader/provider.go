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

package loader

import (
	"context"
	"fmt"
	"reflect"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/go-redis/redis"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/elasticsearch"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda-infra/providers/httpserver/interceptors"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index"
)

type (
	config struct {
		Match []struct {
			Prefix   string   `file:"prefix"`
			Patterns []string `file:"patterns"`
		} `file:"match"`
		LoadMode            string        `file:"load_mode" default:"LoadFromElasticSearchOnly"`
		RequestTimeout      time.Duration `file:"request_timeout" default:"1m"`
		QueryIndexTimeRange bool          `file:"query_index_time_range"`
		TimestampKey        string        `file:"timestamp_key" default:"timestamp"`
		TimestampUnit       string        `file:"timestamp_unit" default:"ns"` // ms、ns
		IndexReloadInterval time.Duration `file:"index_reload_interval" default:"2m"`
		CacheKeyPrefix      string        `file:"cache_key_prefix" default:"es-index"`
		DefaultIndex        string        `file:"default_index" default:"index__for__not__exist"`
	}
	provider struct {
		Cfg      *config
		Log      logs.Logger
		Redis    *redis.Client `autowired:"redis-client" optional:"true"`
		election election.Interface
		es       elasticsearch.Interface

		matchers     []*matcher
		indices      atomic.Value          // *IndexGroup, loaded index
		setIndicesCh chan *indicesBundle   //
		reloadCh     chan chan error       // trigger to load index
		timeRanges   map[string]*timeRange // cache the maximum and minimum time of index
		listeners    []func(*IndexGroup)

		syncLock    sync.Mutex
		cond        sync.Cond
		inSync      bool
		startSyncCh chan struct{}
	}
	matcher struct {
		prefix   string
		patterns []*index.Pattern
	}
	indicesBundle struct {
		indices *IndexGroup
		doneCh  chan struct{}
	}
)

var _ Interface = (*provider)(nil)

func (p *provider) Init(ctx servicehub.Context) error {
	// build matchers
	p.matchers = make([]*matcher, len(p.Cfg.Match), len(p.Cfg.Match))
	for i, item := range p.Cfg.Match {
		if strings.Contains(item.Prefix, "*") {
			return fmt.Errorf("index prefix %q not allowed to contains *", item.Prefix)
		}
		m := &matcher{
			prefix:   item.Prefix,
			patterns: make([]*index.Pattern, len(item.Patterns), len(item.Patterns)),
		}
		for i, item := range item.Patterns {
			ptn, err := index.BuildPattern(item)
			if err != nil {
				return err
			}
			err = ptn.CheckVars()
			if err != nil {
				return err
			}
			m.patterns[i] = ptn
		}
		p.matchers[i] = m
	}

	if es, err := index.FindElasticSearch(ctx, true); err != nil {
		return err
	} else {
		p.es = es
	}

	if err := p.initElection(ctx); err != nil {
		return err
	}

	// setup load task
	switch LoadMode(p.Cfg.LoadMode) {
	case LoadFromElasticSearchOnly:
		if p.es == nil {
			return fmt.Errorf("elasticsearch-client is required")
		}
		ctx.AddTask(p.runElasticSearchIndexLoader, servicehub.WithTaskName("elasticsearch index loader"))
	case LoadFromCacheOnly:
		if p.Redis == nil {
			return fmt.Errorf("redis-client is required")
		}
		ctx.AddTask(p.runCacheLoader, servicehub.WithTaskName("cache index loader"))
	case LoadWithCache:
		if p.es == nil || p.Redis == nil || p.election == nil {
			return fmt.Errorf("elasticsearch-client、etcd-election、redis-client are required")
		}
		p.election.OnLeader(p.syncIndicesToCache)
		ctx.AddTask(p.runCacheLoader, servicehub.WithTaskName("cached index loader"))
	default:
		return fmt.Errorf("invalid load_mode, only support: %v", []LoadMode{LoadFromElasticSearchOnly, LoadFromCacheOnly, LoadWithCache})
	}

	// init manager routes
	routeRrefix := "/api/elasticsearch/index"
	if len(ctx.Label()) > 0 {
		routeRrefix = routeRrefix + "/" + ctx.Label()
	} else {
		routeRrefix = routeRrefix + "/-"
	}
	routes := ctx.Service("http-router", interceptors.CORS()).(httpserver.Router)
	err := p.intRoutes(routes, routeRrefix)
	if err != nil {
		return fmt.Errorf("failed to init routes: %s", err)
	}
	return nil
}

func (p *provider) initElection(ctx servicehub.Context) error {
	const service = "etcd-election"
	var obj interface{}
	var name string
	if len(ctx.Label()) > 0 {
		name = service + "@" + ctx.Label()
		obj = ctx.Service(name)
	}
	if obj == nil {
		name = service + "@index"
		obj = ctx.Service(name)
	}
	if obj == nil {
		name = service
		obj = ctx.Service(name)
	}
	if obj != nil {
		election, ok := obj.(election.Interface)
		if !ok {
			return fmt.Errorf("%q is not election.Interface", name)
		}
		p.election = election
		p.Log.Debugf("use Election(%q) for index clean", name)
	}
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	for {
		select {
		case <-ctx.Done():
			return nil
		case bdl := <-p.setIndicesCh:
			p.indices.Store(bdl.indices)
			if bdl.doneCh != nil {
				bdl.doneCh <- struct{}{}
			}
			for _, fn := range p.listeners {
				fn(bdl.indices)
			}
			p.Log.Infof("load indices %d", GetIndicesNum(bdl.indices))
		}
	}
}

func (p *provider) updateIndices(bdl *indicesBundle) {
	p.setIndicesCh <- bdl
}

func init() {
	servicehub.Register("elasticsearch.index.loader", &servicehub.Spec{
		Services: []string{"elasticsearch.index.loader"},
		Types: []reflect.Type{
			reflect.TypeOf((*Interface)(nil)).Elem(),
		},
		Dependencies:         []string{"elasticsearch"},
		OptionalDependencies: []string{"etcd-election"},
		ConfigFunc:           func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			p := &provider{
				setIndicesCh: make(chan *indicesBundle, 1),
				reloadCh:     make(chan chan error),
				startSyncCh:  make(chan struct{}),
				timeRanges:   make(map[string]*timeRange),
			}
			p.cond.L = &p.syncLock
			return p
		},
	})
}
