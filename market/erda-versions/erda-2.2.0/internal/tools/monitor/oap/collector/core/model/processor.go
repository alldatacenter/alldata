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
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	odata2 "github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
)

type RuntimeProcessor struct {
	Name      string
	Processor Processor
	Filter    *DataFilter
}

type Processor interface {
	Component
	ProcessMetric(item *metric.Metric) (*metric.Metric, error)
	ProcessLog(item *log.Log) (*log.Log, error)
	ProcessSpan(item *trace.Span) (*trace.Span, error)
	ProcessRaw(item *odata2.Raw) (*odata2.Raw, error)
}

type RunningProcessor interface {
	Processor
	StartProcessor(consumer ObservableDataConsumerFunc)
}

type NoopProcessor struct {
}

func (n *NoopProcessor) ComponentConfig() interface{} {
	return nil
}

func (n *NoopProcessor) Process(in odata2.ObservableData) (odata2.ObservableData, error) {
	return in, nil
}
