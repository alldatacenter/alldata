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

package clickhouse

import (
	"context"
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/clickhouse"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/exporters/clickhouse/span"
)

type config struct {
	Database string       `file:"database" default:"monitor"`
	Span     *span.Config `file:"span"`
}

// +provider
type provider struct {
	Cfg        *config
	Log        logs.Logger
	ch         clickhouse.Interface
	ctx        context.Context
	cancelFunc context.CancelFunc

	spanStorage *span.Storage
}

func (p *provider) ExportRaw(items ...*odata.Raw) error        { return nil }
func (p *provider) ExportMetric(items ...*metric.Metric) error { return nil }
func (p *provider) ExportLog(items ...*log.Log) error          { return nil }

func (p *provider) ExportSpan(items ...*trace.Span) error {
	p.spanStorage.WriteBatch(items)
	return nil
}

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func (p *provider) Connect() error {
	return nil
}

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	svc := ctx.Service("clickhouse@span")
	if svc == nil {
		svc = ctx.Service("clickhouse")
	}
	if svc == nil {
		return fmt.Errorf("service clickhouse is required")
	}
	p.ch = svc.(clickhouse.Interface)
	p.ctx, p.cancelFunc = context.WithCancel(context.Background())

	p.spanStorage = span.NewStorage(p.ch.Client(), p.Log.Sub("spanStorage"), p.Cfg.Database, p.Cfg.Span)
	if err := p.spanStorage.Init(ctx); err != nil {
		return fmt.Errorf("init spanStorage: %w", err)
	}
	return nil
}

func (p *provider) Start() error {
	if err := p.spanStorage.Start(p.ctx); err != nil {
		return fmt.Errorf("start span: %w", err)
	}
	return nil
}

func (p *provider) Close() error {
	p.cancelFunc()
	return nil
}

func init() {
	servicehub.Register("erda.oap.collector.exporter.clickhouse", &servicehub.Spec{
		Services: []string{
			"erda.oap.collector.exporter.clickhouse",
		},
		Description:  "here is description of erda.oap.collector.exporter.clickhouse",
		Dependencies: []string{"clickhouse", "clickhouse.table.initializer@span"},
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
