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

package creator

import (
	"context"
	"fmt"
	"sync"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/clickhouse"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/clickhouse/table/loader"
)

type Interface interface {
	Ensure(ctx context.Context, tenant, key string, ttlDays int64) (<-chan error, string)
}

type config struct {
	DDLTemplate       string `file:"ddl_template"`
	DefaultWriteTable string `file:"default_write_table"`
	Database          string `file:"database" default:"monitor"`
	TablePrefix       string `file:"table_prefix"`
}

type request struct {
	Ctx       context.Context
	TableName string
	AliasName string
	TTLDays   int64
	Wait      chan error
}

type provider struct {
	Cfg        *config
	Log        logs.Logger
	Clickhouse clickhouse.Interface `autowired:"clickhouse" inherit-label:"preferred"`
	Loader     loader.Interface     `autowired:"clickhouse.table.loader" inherit-label:"true"`

	createCh   chan request
	createLock sync.Mutex
	created    sync.Map
}

func (p *provider) Init(ctx servicehub.Context) error {
	if len(p.Cfg.DefaultWriteTable) == 0 {
		return fmt.Errorf("default_write_table is required")
	}
	if len(p.Cfg.TablePrefix) == 0 {
		return fmt.Errorf("table_prefix is required")
	}
	return nil
}

func init() {
	servicehub.Register("clickhouse.table.creator", &servicehub.Spec{
		Services:             []string{"clickhouse.table.creator"},
		Dependencies:         []string{"clickhouse.table.loader"},
		OptionalDependencies: []string{"clickhouse.table.initializer"},
		ConfigFunc:           func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{
				createCh: make(chan request),
			}
		},
	})
}
