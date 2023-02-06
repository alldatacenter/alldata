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

package browser

import (
	"fmt"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	writer "github.com/erda-project/erda-infra/pkg/parallel-writer"
	"github.com/erda-project/erda-infra/providers/kafka"
	"github.com/erda-project/erda/internal/apps/msp/apm/browser/ipdb"
)

type config struct {
	Ipdb   string               `file:"ipdb"`
	Output kafka.ProducerConfig `file:"output"`
	Input  kafka.ConsumerConfig `file:"input"`
}

type provider struct {
	Cfg    *config
	Log    logs.Logger
	ipdb   *ipdb.Locator
	output writer.Writer
	kafka  kafka.Interface
}

func (p *provider) Init(ctx servicehub.Context) error {
	ipdb, err := ipdb.NewLocator(p.Cfg.Ipdb)
	if err != nil {
		return fmt.Errorf("fail to load ipdb: %s", err)
	}
	p.ipdb = ipdb
	p.Log.Infof("load ipdb from %s", p.Cfg.Ipdb)

	p.kafka = ctx.Service("kafka").(kafka.Interface)
	w, err := p.kafka.NewProducer(&p.Cfg.Output)
	if err != nil {
		return fmt.Errorf("fail to create kafka producer: %s", err)
	}
	p.output = w
	return nil
}

// Start .
func (p *provider) Start() error {
	return p.kafka.NewConsumer(&p.Cfg.Input, p.invoke)
}

// Close .
func (p *provider) Close() error {
	p.Log.Debug("not support close kafka consumer")
	return nil
}

func init() {
	servicehub.Register("browser-analytics", &servicehub.Spec{
		Services:     []string{"browser-analytics"},
		Dependencies: []string{"kafka", "kafka.topic.initializer"},
		Description:  "browser-analytics",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
