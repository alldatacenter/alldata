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
	"sync"
	"sync/atomic"
	"time"

	"github.com/go-redis/redis"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/clickhouse"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
)

type LoadMode string

const (
	LoadFromCacheOnly      LoadMode = "LoadFromCacheOnly"
	LoadFromClickhouseOnly LoadMode = "LoadFromClickhouseOnly"
	LoadWithCache          LoadMode = "LoadWithCache"
)

const (
	MapStringString ColumnType = "Map(String,String)"
	String          ColumnType = "String"
	UInt64          ColumnType = "UInt64"
	DateTime64      ColumnType = "DateTime64(9,'Asia/Shanghai')"
)

type setTablesRequest struct {
	Tables map[string]*TableMeta
	Done   chan struct{}
}

type TableMeta struct {
	Engine       string
	Columns      map[string]*TableColumn
	TTLDays      int64
	TTLBaseField string
}

type ColumnType string

type TableColumn struct {
	Type ColumnType
}

type config struct {
	LoadMode           string        `file:"load_mode" default:"LoadWithCache"`
	TablePrefix        string        `file:"table_prefix"`
	DefaultSearchTable string        `file:"default_search_table"`
	Database           string        `file:"database" default:"monitor"`
	ReloadInterval     time.Duration `file:"reload_interval" default:"2m"`
	CacheKeyPrefix     string        `file:"cache_key_prefix" default:"clickhouse-table"`
}

type provider struct {
	Cfg        *config
	Log        logs.Logger
	Clickhouse clickhouse.Interface `autowired:"clickhouse" inherit-label:"preferred"`
	Redis      *redis.Client        `autowired:"redis-client"`
	Election   election.Interface   `autowired:"etcd-election@table-loader"`

	tables      atomic.Value
	listeners   []func(map[string]*TableMeta)
	setTablesCh chan *setTablesRequest
	reloadCh    chan chan error

	loadLock              sync.Mutex
	suppressCacheLoader   bool
	needSyncTablesToCache bool
}

func (p *provider) Init(ctx servicehub.Context) error {
	if len(p.Cfg.TablePrefix) == 0 {
		return fmt.Errorf("table_prefix is required")
	}
	if len(p.Cfg.DefaultSearchTable) == 0 {
		return fmt.Errorf("default_search_table is required")
	}

	switch LoadMode(p.Cfg.LoadMode) {
	case LoadFromClickhouseOnly:
		ctx.AddTask(p.runClickhouseTablesLoader, servicehub.WithTaskName("clickhouse tables loader"))
	case LoadFromCacheOnly:
		ctx.AddTask(p.runCacheTablesLoader, servicehub.WithTaskName("cache tables loader"))
	case LoadWithCache:
		p.Election.OnLeader(func(ctx context.Context) {
			p.needSyncTablesToCache = true
			_ = p.runClickhouseTablesLoader(ctx)
		})
		ctx.AddTask(p.runCacheTablesLoader, servicehub.WithTaskName("cache tables loader"))
	default:
		return fmt.Errorf("invalid load_mode")
	}
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	for {
		select {
		case <-ctx.Done():
			return nil
		case req := <-p.setTablesCh:
			p.tables.Store(req.Tables)
			if req.Done != nil {
				req.Done <- struct{}{}
				close(req.Done)
			}
			for _, listener := range p.listeners {
				listener(req.Tables)
			}
			p.Log.Infof("load clickhouse tables, num: %d", len(req.Tables))
		}
	}
}

func (p *provider) updateTables(tables map[string]*TableMeta) chan struct{} {
	ch := make(chan struct{}, 1)
	req := setTablesRequest{
		Tables: tables,
		Done:   ch,
	}
	p.setTablesCh <- &req
	return ch
}

func init() {
	servicehub.Register("clickhouse.table.loader", &servicehub.Spec{
		Services: []string{"clickhouse.table.loader"},
		Types: []reflect.Type{
			reflect.TypeOf((*Interface)(nil)).Elem(),
		},
		Dependencies:         []string{"clickhouse"},
		OptionalDependencies: []string{"etcd-election"},
		ConfigFunc:           func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			p := &provider{
				setTablesCh: make(chan *setTablesRequest, 1),
				reloadCh:    make(chan chan error),
			}
			return p
		},
	})
}
