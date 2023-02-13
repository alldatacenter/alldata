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

package exporter

import (
	"fmt"

	"github.com/recallsong/go-utils/encoding/md5x"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/kafka"
)

type config struct {
	Input        kafka.ConsumerConfig `file:"input"`
	Output       string               `file:"output" env:"MONITOR_LOG_OUTPUT"`
	OutputConfig string               `file:"output_config" env:"MONITOR_LOG_OUTPUT_CONFIG"`
	Filters      map[string]string    `file:"filters"`
}

type provider struct {
	C     *config
	L     logs.Logger
	kafka kafka.Interface
}

func (p *provider) Init(ctx servicehub.Context) error {
	if len(p.C.Input.Group) <= 0 {
		p.C.Input.Group = fmt.Sprintf("%s-%s", p.C.Output, md5x.SumString(p.C.OutputConfig).String16())
	}
	p.kafka = ctx.Service("kafka").(kafka.Interface)
	return nil
}

// NewConsumer
func (p *provider) NewConsumer(fn OutputFactory) error {
	return p.kafka.NewConsumerWitchCreator(&p.C.Input, func(i int) (kafka.ConsumerFunc, error) {
		output, err := fn(i)
		if err != nil {
			return nil, fmt.Errorf("fail to create output %s", err)
		}
		c := &consumer{
			filters: p.C.Filters,
			log:     p.L,
			output:  output,
		}
		return c.Invoke, nil
	})
}

func init() {
	servicehub.Register("logs-exporter-base", &servicehub.Spec{
		Services:     []string{"logs-exporter-base"},
		Dependencies: []string{"kafka"},
		Description:  "logs exporter base",
		ConfigFunc:   func() interface{} { return &config{} },
		Creator: func() servicehub.Provider {
			return &provider{}
		},
	})
}
