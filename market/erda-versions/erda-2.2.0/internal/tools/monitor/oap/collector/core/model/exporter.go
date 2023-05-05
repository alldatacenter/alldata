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

package model

import (
	"context"
	"time"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	odata2 "github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib"
)

type RuntimeExporter struct {
	Name     string
	DType    odata2.DataType
	Logger   logs.Logger
	Exporter Exporter
	Filter   *DataFilter

	Buffer           *odata2.Buffer
	Timer            *time.Timer
	Interval, Jitter time.Duration
}

func (re *RuntimeExporter) Add(od odata2.ObservableData) {
	re.Buffer.Push(od)

	if re.Buffer.Full() {
		if err := re.flushOnce(); err != nil {
			re.Logger.Errorf("event limited, but flush err: %s", err)
		}
		if !re.Timer.Stop() {
			<-re.Timer.C
		}
		re.Timer.Reset(lib.RandomDuration(re.Interval, re.Jitter))
	}
}

func (re *RuntimeExporter) Start(ctx context.Context) {
	for {
		select {
		case <-ctx.Done():
			if err := re.flushOnce(); err != nil {
				re.Logger.Errorf("event done, but flush err: %s", err)
			}
			re.Timer.Stop()
			return
		case <-re.Timer.C: // TODO. Timer, can reset
			if !re.Buffer.Empty() {
				if err := re.flushOnce(); err != nil {
					re.Logger.Errorf("event elapsed, but flush err: %s", err)
				}
			}
			re.Timer.Reset(lib.RandomDuration(re.Interval, re.Jitter))
		}
	}
}

func (re *RuntimeExporter) flushOnce() error {
	switch re.DType {
	case odata2.MetricType:
		items := re.Buffer.FlushAllMetrics()
		err := re.Exporter.ExportMetric(items...)
		if err != nil {
			re.Logger.Errorf("Exporter<%s> process data error: %s", re.Name, err)
		}
	case odata2.LogType:
		items := re.Buffer.FlushAllLogs()
		err := re.Exporter.ExportLog(items...)
		if err != nil {
			re.Logger.Errorf("Exporter<%s> process data error: %s", re.Name, err)
		}
	case odata2.SpanType:
		items := re.Buffer.FlushAllSpans()
		err := re.Exporter.ExportSpan(items...)
		if err != nil {
			re.Logger.Errorf("Exporter<%s> process data error: %s", re.Name, err)
		}
	case odata2.RawType:
		items := re.Buffer.FlushAllRaws()
		err := re.Exporter.ExportRaw(items...)
		if err != nil {
			re.Logger.Errorf("Exporter<%s> process data error: %s", re.Name, err)
		}
	}
	return nil
}

type Exporter interface {
	Component
	Connect() error
	ExportMetric(items ...*metric.Metric) error
	ExportLog(items ...*log.Log) error
	ExportSpan(items ...*trace.Span) error
	ExportRaw(items ...*odata2.Raw) error
}

type NoopExporter struct{}

func (n *NoopExporter) ComponentConfig() interface{} {
	return nil
}

func (n *NoopExporter) Connect() error {
	return nil
}

func (n *NoopExporter) Export(ods []odata2.ObservableData) error {
	return nil
}
