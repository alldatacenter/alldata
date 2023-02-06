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
	"time"

	"github.com/bluele/gcache"
	"github.com/gocql/gocql"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/logs/logrusx"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/cassandra"
	mutex "github.com/erda-project/erda-infra/providers/etcd-mutex"
	"github.com/erda-project/erda-infra/providers/kafka"
)

type mockContext struct {
	servicehub.Context
	l logs.Logger
}

func (m *mockContext) Key() string {
	return ""
}

func (m *mockContext) Provider() servicehub.Provider {
	return ""
}

func (m *mockContext) Label() string {
	return ""
}

func (m *mockContext) AddTask(task func(context.Context) error, options ...servicehub.TaskOption) {
	return
}

func (m *mockContext) Hub() *servicehub.Hub {
	return &servicehub.Hub{}
}

func (m *mockContext) Config() interface{} {
	return &config{
		Input: kafka.ConsumerConfig{},
		Output: struct {
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
		}{},
	}
}

func (m *mockContext) Logger() logs.Logger {
	return m.l
}

func (m *mockContext) Service(name string, options ...interface{}) interface{} {
	switch name {
	case "cassandra":
		return &mockCassandraInf{}
	default:
		return nil
	}
}

type mockCassandraInf struct {
}

func (m *mockCassandraInf) NewSession(cfg *cassandra.SessionConfig) (*cassandra.Session, error) {
	return nil, nil
}

func (m *mockCassandraInf) NewBatchWriter(session *cassandra.Session, c *cassandra.WriterConfig, builderCreator func() cassandra.StatementBuilder) writer.Writer {
	return nil
}

func (m *mockCassandraInf) CreateKeyspaces(ksc ...*cassandra.KeyspaceConfig) error {
	return nil
}

func (m *mockCassandraInf) Session(cfg *cassandra.SessionConfig) (*gocql.Session, error) {
	return nil, nil
}

func mockProvider() *provider {
	p := &provider{}
	p.Cfg = &config{
		Input: kafka.ConsumerConfig{},
		Output: struct {
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
		}{},
	}
	p.cache = gcache.New(128).LRU().Build()
	p.Log = logrusx.New()
	p.schema = &mockLogSchema{}
	p.Retention = &mockRetention{}
	return p
}

type mockLogSchema struct {
	validateOrgTriggered bool
}

func (m *mockLogSchema) Name() string {
	return ""
}

func (m *mockLogSchema) RunDaemon(ctx context.Context, interval time.Duration, muInf mutex.Interface) {
	return
}

func (m *mockLogSchema) CreateDefault() error {
	return nil
}

func (m *mockLogSchema) ValidateOrg(orgName string) bool {
	return !m.validateOrgTriggered
}

type mockRetention struct{}

const mockTTL = 7 * 24 * time.Hour

func (r *mockRetention) GetTTL(key string) time.Duration                         { return mockTTL }
func (r *mockRetention) DefaultTTL() time.Duration                               { return mockTTL }
func (r *mockRetention) GetConfigKey(name string, tags map[string]string) string { return "" }
func (r *mockRetention) GetTTLByTags(name string, tags map[string]string) time.Duration {
	return mockTTL
}
func (r *mockRetention) Loading(ctx context.Context) {
	<-ctx.Done()
}
