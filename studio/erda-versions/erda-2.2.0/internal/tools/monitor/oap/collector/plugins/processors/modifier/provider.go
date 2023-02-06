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

package modifier

import (
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins/processors/modifier/operator"
)

var providerName = plugins.WithPrefixProcessor("modifier")

type config struct {
	Rules []operator.ModifierCfg `file:"rules"`

	Keypass map[string][]string `file:"keypass"`
}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger

	operators []*operator.Operator
}

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func (p *provider) ProcessMetric(item *metric.Metric) (*metric.Metric, error) {
	for _, op := range p.operators {
		if !op.Condition.Match(item) {
			continue
		}
		item = op.Modifier.Modify(item).(*metric.Metric)
	}
	return item, nil
}

func (p *provider) ProcessLog(item *log.Log) (*log.Log, error) {
	for _, op := range p.operators {
		if !op.Condition.Match(item) {
			continue
		}
		item = op.Modifier.Modify(item).(*log.Log)
	}
	return item, nil
}

func (p *provider) ProcessSpan(item *trace.Span) (*trace.Span, error) {
	for _, op := range p.operators {
		if !op.Condition.Match(item) {
			continue
		}
		item = op.Modifier.Modify(item).(*trace.Span)
	}
	return item, nil
}

func (p *provider) ProcessRaw(item *odata.Raw) (*odata.Raw, error) { return item, nil }

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	ops := make([]*operator.Operator, len(p.Cfg.Rules))
	for idx, cfg := range p.Cfg.Rules {
		op, err := operator.NewOperator(cfg)
		if err != nil {
			return fmt.Errorf("NewOperator: %w", err)
		}
		ops[idx] = op
	}
	p.operators = ops
	return nil
}

func init() {
	servicehub.Register(providerName, &servicehub.Spec{
		Services: []string{
			providerName,
		},

		Description: "here is description of erda.oap.collector.processor.modifier",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
