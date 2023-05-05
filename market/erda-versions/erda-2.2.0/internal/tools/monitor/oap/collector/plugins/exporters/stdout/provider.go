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

package stdout

import (
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/core/log"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/core/model/odata"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/plugins"
)

var providerName = plugins.WithPrefixExporter("stdout")

type config struct {
}

// +provider
type provider struct {
	Cfg *config
	Log logs.Logger
}

func (p *provider) ComponentConfig() interface{} {
	return p.Cfg
}

func (p *provider) Connect() error {
	return nil
}

func (p *provider) Close() error {
	return nil
}

func (p *provider) ExportMetric(items ...*metric.Metric) error {
	for _, item := range items {
		buf, err := json.Marshal(item)
		if err != nil {
			return err
		}
		fmt.Printf("%s\n", string(buf))
	}
	return nil
}

func (p *provider) ExportLog(items ...*log.Log) error {
	for _, item := range items {
		buf, err := json.Marshal(item)
		if err != nil {
			return err
		}
		fmt.Printf("%s\n", string(buf))
	}
	return nil
}

func (p *provider) ExportSpan(items ...*trace.Span) error {
	for _, item := range items {
		buf, err := json.Marshal(item)
		if err != nil {
			return err
		}
		fmt.Printf("%s\n", string(buf))
	}
	return nil
}

func (p *provider) ExportRaw(items ...*odata.Raw) error { return nil }

// Run this is optional
func (p *provider) Init(ctx servicehub.Context) error {
	return nil
}

func init() {
	servicehub.Register(providerName, &servicehub.Spec{
		Services: []string{
			providerName,
		},
		Description: "here is description of erda.oap.collector.exporter.stdout",
		ConfigFunc: func() interface{} {
			return &config{}
		},
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
