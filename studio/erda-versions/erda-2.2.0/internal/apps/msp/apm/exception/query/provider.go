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
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/cassandra"
	eventpb "github.com/erda-project/erda-proto-go/core/monitor/event/pb"
	metricpb "github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda-proto-go/msp/apm/exception/pb"
	entitypb "github.com/erda-project/erda-proto-go/oap/entity/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/query/source"
	"github.com/erda-project/erda/pkg/common/apis"
)

type config struct {
	Cassandra   cassandra.SessionConfig `file:"cassandra"`
	QuerySource querySource             `file:"query_source"`
}

type querySource struct {
	ElasticSearch bool `file:"elasticsearch"`
	Cassandra     bool `file:"cassandra"`
}

// +provider
type provider struct {
	Cfg              *config
	Log              logs.Logger
	Register         transport.Register
	Source           source.ExceptionSource
	exceptionService *exceptionService
	Cassandra        cassandra.Interface             `autowired:"cassandra" optional:"true"`
	Metric           metricpb.MetricServiceServer    `autowired:"erda.core.monitor.metric.MetricService"`
	Entity           entitypb.EntityServiceServer    `autowired:"erda.oap.entity.EntityService"`
	Event            eventpb.EventQueryServiceServer `autowired:"erda.core.monitor.event.EventQueryService"`
}

func (p *provider) Init(ctx servicehub.Context) error {

	if p.Cfg.QuerySource.Cassandra {
		if p.Cassandra == nil {
			panic("cassandra provider autowired failed.")
		}

		if p.Cassandra != nil {
			session, err := p.Cassandra.NewSession(&p.Cfg.Cassandra)
			if err != nil {
				return fmt.Errorf("fail to create cassandra session: %s", err)
			}
			p.Source = &source.CassandraSource{CassandraSession: session}
		}
	}
	if p.Cfg.QuerySource.ElasticSearch {
		p.Source = &source.ElasticsearchSource{Metric: p.Metric, Event: p.Event, Entity: p.Entity}
	}

	p.exceptionService = &exceptionService{
		p:      p,
		source: p.Source,
	}
	if p.Register != nil {
		pb.RegisterExceptionServiceImp(p.Register, p.exceptionService, apis.Options())
	}
	return nil
}

func (p *provider) Provide(ctx servicehub.DependencyContext, args ...interface{}) interface{} {
	switch {
	case ctx.Service() == "erda.msp.apm.exception.ExceptionService" || ctx.Type() == pb.ExceptionServiceServerType() || ctx.Type() == pb.ExceptionServiceHandlerType():
		return p.exceptionService
	}
	return p
}

func init() {
	servicehub.Register("erda.msp.apm.exception.query", &servicehub.Spec{
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
