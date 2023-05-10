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

package query

import (
	"embed"
	"fmt"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/cassandra"
	"github.com/erda-project/erda-infra/providers/clickhouse"
	componentprotocol "github.com/erda-project/erda-infra/providers/component-protocol"
	"github.com/erda-project/erda-infra/providers/component-protocol/protocol"
	"github.com/erda-project/erda-infra/providers/i18n"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/db"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/query/source"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace/storage"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
	Cassandra        cassandra.SessionConfig `file:"cassandra"`
	QuerySource      querySource             `file:"query_source"`
	DebugSQL         bool                    `file:"debug_sql"`
	CompatibleEnable bool                    `file:"compatible_enable"`
}

type querySource struct {
	ElasticSearch bool `file:"elasticsearch"`
	Cassandra     bool `file:"cassandra"`
	ClickHouse    bool `file:"clickhouse"`
}

//go:embed scenarios
var scenarioFS embed.FS

// +provider
type provider struct {
	Cfg              *config
	Log              logs.Logger
	traceService     *TraceService
	I18n             i18n.Translator              `autowired:"i18n" translator:"msp-i18n"`
	Register         transport.Register           `autowired:"service-register"`
	Metric           metricpb.MetricServiceServer `autowired:"erda.core.monitor.metric.MetricService"`
	DB               *gorm.DB                     `autowired:"mysql-client"`
	Cassandra        cassandra.Interface          `autowired:"cassandra" optional:"true"`
	Clickhouse       clickhouse.Interface         `autowired:"clickhouse" optional:"true"`
	StorageReader    storage.Storage              `autowired:"span-storage-elasticsearch-reader" optional:"true"`
	Source           source.TraceSource
	CompatibleSource source.TraceSource // version 2.x
	Protocol         componentprotocol.Interface
	CPTran           i18n.I18n `autowired:"i18n"`
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.Protocol.SetI18nTran(p.CPTran)
	protocol.MustRegisterProtocolsFromFS(scenarioFS)
	if p.Cfg.QuerySource.ElasticSearch {
		if p.StorageReader == nil {
			panic("elasticsearch provider autowired failed.")
		}
		p.Source = &source.ElasticsearchSource{StorageReader: p.StorageReader, Metric: p.Metric, Log: p.Log}
	}
	if p.Cfg.QuerySource.Cassandra {
		if p.StorageReader == nil {
			panic("elasticsearch provider autowired failed.")
		}
		p.CompatibleSource = &source.ElasticsearchSource{StorageReader: p.StorageReader, Metric: p.Metric, Log: p.Log}
		if p.Cassandra == nil {
			panic("cassandra provider autowired failed.")
		}
		if p.Cassandra != nil {
			session, err := p.Cassandra.NewSession(&p.Cfg.Cassandra)
			if err != nil {
				return fmt.Errorf("fail to create cassandra session: %s", err)
			}
			p.Source = &source.CassandraSource{CassandraSession: session, Log: p.Log, CompatibleSource: p.CompatibleSource}
		}
	}
	if p.Cfg.QuerySource.ClickHouse {
		if p.Cfg.CompatibleEnable {
			if p.StorageReader == nil {
				panic("elasticsearch provider autowired failed.")
			}
			p.CompatibleSource = &source.ElasticsearchSource{StorageReader: p.StorageReader, Metric: p.Metric, Log: p.Log}
		}

		if p.Clickhouse == nil {
			panic("clickhouse provider autowired failed.")
		}
		p.Source = &source.ClickhouseSource{Clickhouse: p.Clickhouse, Log: p.Log, CompatibleSource: p.CompatibleSource, DebugSQL: p.Cfg.DebugSQL}
	}

	p.traceService = &TraceService{
		p:                     p,
		i18n:                  p.I18n,
		traceRequestHistoryDB: &db.TraceRequestHistoryDB{DB: p.DB},
		Source:                p.Source,
		CompatibleSource:      p.CompatibleSource,
	}
	if p.Register != nil {
		pb.RegisterTraceServiceImp(p.Register, p.traceService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.apm.trace.TraceService" || ctx.Type() == pb.TraceServiceServerType() || ctx.Type() == pb.TraceServiceHandlerType():
		return p.traceService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.apm.trace.query", &servicehub.Spec{
		Services:             pb.ServiceNames(),
		Types:                pb.Types(),
		OptionalDependencies: []string{"service-register"},
		Description:          "",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
