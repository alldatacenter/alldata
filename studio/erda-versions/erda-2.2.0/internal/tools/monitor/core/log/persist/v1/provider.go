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

package storage

import (
	"context"
	"fmt"
	"time"

	"github.com/bluele/gcache"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/cassandra"
	mutex "github.com/erda-project/erda-infra/providers/etcd-mutex"
	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/persist/v1/schema"
	"github.com/erda-project/erda/internal/tools/monitor/core/settings/retention-strategy"
)

type config struct {
	Input  kafka.ConsumerConfig `file:"input"`
	Output struct {
		LogSchema struct {
			OrgRefreshInterval time.Duration `file:"org_refresh_interval" default:"2m" env:"LOG_SCHEMA_ORG_REFRESH_INTERVAL"`
		} `file:"log_schema"`
		Cassandra struct {
			cassandra.WriterConfig  `file:"writer_config"`
			cassandra.SessionConfig `file:"session_config"`
			GCGraceSeconds          int           `file:"gc_grace_seconds" default:"86400"`
			CacheStoreInterval      time.Duration `file:"cache_store_interval" default:"3m"`
		} `file:"cassandra"`
		IDKeys []string `file:"id_keys"`
	} `file:"output"`
}

type provider struct {
	Cfg       *config
	Log       logs.Logger
	Kafka     kafka.Interface     `autowired:"kafka"`
	Cassandra cassandra.Interface `autowired:"cassandra"`
	Retention retention.Interface `autowired:"storage-retention-strategy@log"`
	Mutex     mutex.Interface     `autowired:"etcd-mutex"`

	output writer.Writer
	schema schema.LogSchema
	cache  gcache.Cache
}

func (p *provider) Init(ctx servicehub.Context) error {
	session, err := p.Cassandra.NewSession(&p.Cfg.Output.Cassandra.SessionConfig)
	if err != nil {
		return fmt.Errorf("failed to create cassandra session: %s", err)
	}
	p.output = p.Cassandra.NewBatchWriter(session, &p.Cfg.Output.Cassandra.WriterConfig, p.createLogStatementBuilder)

	p.schema, err = schema.NewCassandraSchema(p.Cassandra, p.Log.Sub("logSchema"))
	if err != nil {
		return err
	}
	p.cache = gcache.New(128).LRU().Build()
	ctx.AddTask(func(c context.Context) error {
		p.Retention.Loading(ctx)
		return nil
	})
	return nil
}

func (p *provider) Run(ctx context.Context) error {
	if err := p.schema.CreateDefault(); err != nil {
		return fmt.Errorf("create default error: %w", err)
	}
	go p.schema.RunDaemon(ctx, p.Cfg.Output.LogSchema.OrgRefreshInterval, p.Mutex)

	go p.startStoreMetaCache(ctx)
	if err := p.Kafka.NewConsumer(&p.Cfg.Input, p.invoke); err != nil {
		return err
	}

	<-ctx.Done()
	return nil
}

func init() {
	servicehub.Register("log-persist-v1", &servicehub.Spec{
		Dependencies: []string{"kafka.topic.initializer"},
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
