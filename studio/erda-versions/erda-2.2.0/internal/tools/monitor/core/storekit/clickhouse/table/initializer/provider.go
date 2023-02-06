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

package initializer

import (
	"context"
	"sync"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/clickhouse"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda/internal/tools/monitor/core/settings/retention-strategy"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

type ddlFile struct {
	Path      string `file:"path"`
	IgnoreErr bool   `file:"ignore_err"`
}

type config struct {
	DefaultDDLs     []ddlFile     `file:"default_ddl_files"`
	TenantDDLs      []ddlFile     `file:"tenant_ddl_files"`
	Database        string        `file:"database" default:"monitor"`
	TablePrefix     string        `file:"table_prefix"`
	TTLSyncInterval time.Duration `file:"ttl_sync_interval" default:"24h"`
}

type provider struct {
	Cfg        *config
	Log        logs.Logger
	Clickhouse clickhouse.Interface `autowired:"clickhouse" inherit-label:"preferred"`
	Retention  retention.Interface  `autowired:"storage-retention-strategy" inherit-label:"preferred"`
	Loader     loader.Interface     `autowired:"clickhouse.table.loader" inherit-label:"true"`
	Election   election.Interface   `autowired:"etcd-election@table-initializer"`

	once sync.Once
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.Election.OnLeader(func(ctx context.Context) {
		p.once.Do(func() {
			_ = p.initDefaultDDLs()
			p.initTenantDDLs()
		})
		go p.syncTTL(ctx)
	})
	return nil
}

func init() {
	servicehub.Register("clickhouse.table.initializer", &servicehub.Spec{
		Services:     []string{"clickhouse.table.initializer"},
		Dependencies: []string{"clickhouse"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
